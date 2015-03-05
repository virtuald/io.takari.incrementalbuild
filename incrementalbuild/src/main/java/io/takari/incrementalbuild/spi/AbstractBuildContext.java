package io.takari.incrementalbuild.spi;

import static io.takari.incrementalbuild.spi.MMaps.put;
import io.takari.incrementalbuild.MessageSeverity;
import io.takari.incrementalbuild.ResourceMetadata;
import io.takari.incrementalbuild.ResourceStatus;
import io.takari.incrementalbuild.workspace.Workspace;
import io.takari.incrementalbuild.workspace.Workspace.FileVisitor;
import io.takari.incrementalbuild.workspace.Workspace.Mode;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks build input and output resources and associations among them.
 */
public class AbstractBuildContext {
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Workspace workspace;

  private final File stateFile;

  private final DefaultBuildContextState state;

  private final DefaultBuildContextState oldState;

  /**
   * Previous build state does not exist, cannot be read or configuration has changed. When
   * escalated, all input files are considered require processing.
   */
  private final boolean escalated;

  /**
   * Resources known to be deleted since previous build.
   */
  private final Set<File> deletedResources = new HashSet<>();

  /**
   * Resources selected for processing during this build.
   */
  private final Set<Object> processedResources = new HashSet<>();

  public AbstractBuildContext(Workspace workspace, File stateFile,
      Map<String, Serializable> configuration) {

    // preconditions
    if (workspace == null) {
      throw new NullPointerException();
    }
    if (configuration == null) {
      throw new NullPointerException();
    }

    this.stateFile = stateFile;
    this.state = DefaultBuildContextState.withConfiguration(configuration);
    this.oldState = DefaultBuildContextState.loadFrom(stateFile);

    final boolean configurationChanged = getConfigurationChanged();
    if (workspace.getMode() == Mode.ESCALATED) {
      this.escalated = true;
      this.workspace = workspace;
    } else if (workspace.getMode() == Mode.SUPPRESSED) {
      this.escalated = false;
      this.workspace = workspace;
    } else if (configurationChanged) {
      this.escalated = true;
      this.workspace = workspace.escalate();
    } else {
      this.escalated = false;
      this.workspace = workspace;
    }

    if (escalated && stateFile != null) {
      if (!stateFile.canRead()) {
        log.info("Previous incremental build state does not exist, performing full build");
      } else {
        log.info("Incremental build configuration change detected, performing full build");
      }
    } else {
      log.info("Performing incremental build");
    }
  }

  private boolean getConfigurationChanged() {
    Map<String, Serializable> configuration = state.configuration;
    Map<String, Serializable> oldConfiguration = oldState.configuration;

    if (oldConfiguration.isEmpty()) {
      return true; // no previous state
    }

    Set<String> keys = new TreeSet<String>();
    keys.addAll(configuration.keySet());
    keys.addAll(oldConfiguration.keySet());

    boolean result = false;
    StringBuilder msg = new StringBuilder();

    for (String key : keys) {
      Serializable value = configuration.get(key);
      Serializable oldValue = oldConfiguration.get(key);
      if (!Objects.equals(oldValue, value)) {
        result = true;
        msg.append("\n   ");
        if (value == null) {
          msg.append("REMOVED");
        } else if (oldValue == null) {
          msg.append("ADDED");
        } else {
          msg.append("CHANGED");
        }
        msg.append(' ').append(key);
      }
    }

    if (result) {
      log.debug("Incremental build configuration key changes:{}", msg.toString());
    }

    return result;
  }


  /**
   * Registers matching resources as this build's input set.
   */
  public Collection<DefaultResourceMetadata<File>> registerInputs(File basedir,
      Collection<String> includes, Collection<String> excludes) throws IOException {
    basedir = normalize(basedir);
    final List<DefaultResourceMetadata<File>> result = new ArrayList<>();
    final FileMatcher matcher = FileMatcher.matcher(basedir, includes, excludes);
    workspace.walk(basedir, new FileVisitor() {
      @Override
      public void visit(File file, long lastModified, long length, Workspace.ResourceStatus status) {
        if (matcher.matches(file)) {
          switch (status) {
            case MODIFIED:
            case NEW:
              result.add(registerNormalizedResource(file, lastModified, length, false));
              break;
            case REMOVED:
              deletedResources.add(file);
              break;
            default:
              throw new IllegalArgumentException();
          }
        }
      }
    });
    if (workspace.getMode() == Mode.DELTA) {
      // only NEW, MODIFIED and REMOVED resources are reported in DELTA mode
      // need to find any UNMODIFIED
      final FileMatcher absoluteMatcher = FileMatcher.matcher(basedir, includes, excludes);
      for (ResourceHolder<?> holder : oldState.resources.values()) {
        if (holder instanceof FileState) {
          FileState fileState = (FileState) holder;
          if (!state.resources.containsKey(fileState.file)
              && !deletedResources.contains(fileState.file)
              && absoluteMatcher.matches(fileState.file)) {
            result.add(registerNormalizedResource(fileState.file, fileState.lastModified,
                fileState.length, false));
          }
        }
      }
    }
    return result;
  }

  private static File normalize(File file) {
    if (file == null) {
      throw new IllegalArgumentException();
    }
    try {
      return file.getCanonicalFile();
    } catch (IOException e) {
      return file.getAbsoluteFile();
    }
  }

  private DefaultResourceMetadata<File> registerNormalizedResource(File resourceFile,
      long lastModified, long length, boolean replace) {
    if (!state.resources.containsKey(resourceFile)) {
      registerResource(newFileState(resourceFile, lastModified, length), replace);
    }
    return new DefaultResourceMetadata<File>(this, oldState, resourceFile);
  }

  private FileState newFileState(File file, long lastModified, long length) {
    if (!workspace.isPresent(file)) {
      throw new IllegalArgumentException("File does not exist or cannot be read " + file);
    }
    return new FileState(file, lastModified, length);
  }

  /**
   * Adds the resource to this build's resource set. The resource must exist, i.e. it's status must
   * not be REMOVED.
   */
  private <T extends Serializable> T registerResource(ResourceHolder<T> holder, boolean replace) {
    T resource = holder.getResource();
    ResourceHolder<?> other = state.resources.get(resource);
    if (other == null) {
      if (getResourceStatus(holder) == ResourceStatus.REMOVED) {
        throw new IllegalArgumentException("Resource does not exist " + resource);
      }
      state.resources.put(resource, holder);
    } else {
      if (!holder.equals(other)) {
        if (!replace) {
          throw new IllegalArgumentException("Inconsistent resource state " + resource);
        }
        state.resources.put(resource, holder);
      }
    }
    return resource;
  }

  /**
   * Returns resource status compared to the previous build.
   */
  public ResourceStatus getResourceStatus(Object resource) {
    if (deletedResources.contains(resource)) {
      return ResourceStatus.REMOVED;
    }

    ResourceHolder<?> oldResourceState = oldState.resources.get(resource);
    if (oldResourceState == null) {
      return ResourceStatus.NEW;
    }

    if (escalated) {
      return ResourceStatus.MODIFIED;
    }

    return getResourceStatus(oldResourceState);
  }

  private ResourceStatus getResourceStatus(ResourceHolder<?> holder) {
    if (holder instanceof FileState) {
      FileState fileState = (FileState) holder;
      switch (workspace.getResourceStatus(fileState.file, fileState.lastModified, fileState.length)) {
        case NEW:
          return ResourceStatus.NEW;
        case MODIFIED:
          return ResourceStatus.MODIFIED;
        case REMOVED:
          return ResourceStatus.REMOVED;
        case UNMODIFIED:
          return ResourceStatus.UNMODIFIED;
      }
      throw new IllegalArgumentException();
    }
    return holder.getStatus();
  }

  public <T> DefaultResource<T> processResource(DefaultResourceMetadata<T> metadata) {
    final T resource = metadata.getResource();

    if (metadata.context != this || !state.resources.containsKey(resource)) {
      throw new IllegalArgumentException();
    }

    if (metadata instanceof DefaultResource) {
      return (DefaultResource<T>) metadata;
    }

    processResource(resource);

    return new DefaultResource<T>(this, state, resource);
  }

  private <T> void processResource(final T resource) {
    processedResources.add(resource);
    // reset all metadata associated with the resource during this build
    state.resourceAttributes.remove(resource);
    state.resourceMessages.remove(resource);
    state.resourceOutputs.remove(resource);
  }

  // simple key/value pairs

  public <T extends Serializable> Serializable setResourceAttribute(Object resource, String key,
      T value) {
    Map<String, Serializable> attributes = state.resourceAttributes.get(resource);
    if (attributes == null) {
      attributes = new LinkedHashMap<String, Serializable>();
      state.resourceAttributes.put(resource, attributes);
    }
    attributes.put(key, value);
    Map<String, Serializable> oldAttributes = oldState.resourceAttributes.get(resource);
    return oldAttributes != null ? (Serializable) oldAttributes.get(key) : null;
  }

  public <T extends Serializable> T getResourceAttribute(DefaultBuildContextState state,
      Object resource, String key, Class<T> clazz) {
    Map<String, Serializable> attributes = state.resourceAttributes.get(resource);
    return attributes != null ? clazz.cast(attributes.get(key)) : null;
  }

  // persisted messages

  public void addMessage(Object resource, int line, int column, String message, MessageSeverity severity,
      Throwable cause) {
    // this is likely called as part of builder error handling logic.
    // to make IAE easier to troubleshoot, link cause to the exception thrown
    if (resource == null) {
      throw new IllegalArgumentException(cause);
    }
    if (severity == null) {
      throw new IllegalArgumentException(cause);
    }
    put(state.resourceMessages, resource, new Message(line, column, message, severity, cause));
  }

  public DefaultOutput processOutput(File outputFile) {
    outputFile = normalize(outputFile);

    registerNormalizedResource(outputFile, outputFile.lastModified(), outputFile.length(), true);
    processResource(outputFile);
    state.outputs.add(outputFile);

    return new DefaultOutput(this, state, outputFile);
  }

  public OutputStream newOutputStream(DefaultOutput output) throws IOException {
    return workspace.newOutputStream(output.getResource());
  }

  public <T> void associate(DefaultResource<T> resource, DefaultOutput output) {
    if (resource.context != this) {
      throw new IllegalArgumentException();
    }
    if (output.context != this) {
      throw new IllegalArgumentException();
    }

    put(state.resourceOutputs, resource.getResource(), output.getResource());
  }

  public Collection<? extends ResourceMetadata<File>> getAssociatedOutputs(
      DefaultBuildContextState state, Object resource) {
    Collection<File> outputFiles = state.resourceOutputs.get(resource);
    if (outputFiles == null || outputFiles.isEmpty()) {
      return Collections.emptyList();
    }
    List<ResourceMetadata<File>> outputs = new ArrayList<>();
    for (File outputFile : outputFiles) {
      outputs.add(new DefaultResourceMetadata<File>(this, state, outputFile));
    }
    return outputs;
  }

  public void persist() throws IOException {
    // need to decide what to do with up-to-date resources from previous build

    // inputs: carry-over metadata. no choice here. the user explicitly registered the inputs.

    // outputs: can be either carried over (including metadata) or deleted
    // generally, output is carried over if its inputs are up-to-date
    // which can be tricky to determine in case of many-inputs-to-one-output

    Collection<Object> resources = oldState.resources.keySet();
    while (!resources.isEmpty()) {
      resources = finalizeResources(resources);
    }

    if (stateFile != null) {
      final long start = System.currentTimeMillis();
      try (OutputStream os = workspace.newOutputStream(stateFile)) {
        state.storeTo(os);
      }
      log.debug("Stored incremental build state {} ({} ms)", stateFile, System.currentTimeMillis()
          - start);
    }
  }

  private Collection<Object> finalizeResources(Collection<Object> resources) {
    Set<Object> consider = new HashSet<>();

    for (Object resource : resources) {
      if (processedResources.contains(resource) || deletedResources.contains(resource)) {
        // known deleted or processed resource, nothing to carry over
        continue;
      }

      ResourceHolder<?> holder = state.resources.get(resource);

      if (holder == null) {
        if (oldState.outputs.contains(resource)) {
          workspace.deleteFile((File) resource);
        }

        continue;
      }

      state.resources.put(resource, holder);

      // carry-over messages
      Collection<Message> messages = oldState.resourceMessages.get(resource);
      if (messages != null && !messages.isEmpty()) {
        state.resourceMessages.put(resource, messages);
      }

      // carry-over attributes
      Map<String, Serializable> attributes = oldState.resourceAttributes.get(resource);
      if (attributes != null && !attributes.isEmpty()) {
        state.resourceAttributes.put(resource, attributes);
      }

      Collection<File> outputFiles = oldState.resourceOutputs.get(resource);
      if (outputFiles != null && !outputFiles.isEmpty()) {
        state.resourceOutputs.put(resource, outputFiles);
        // associated up-to-date outputs need to be carried over
        for (File outputFile : outputFiles) {
          if (!state.resources.containsKey(outputFile)) {
            state.resources.put(outputFile, oldState.resources.get(outputFile));
            state.outputs.add(outputFile);
            consider.add(outputFile);
          }
        }
      }
    }

    return consider;
  }
}
