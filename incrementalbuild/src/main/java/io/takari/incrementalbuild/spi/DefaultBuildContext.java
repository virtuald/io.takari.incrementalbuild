package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.BuildContext;
import io.takari.incrementalbuild.workspace.MessageSink;
import io.takari.incrementalbuild.workspace.Workspace;
import io.takari.incrementalbuild.workspace.Workspace.FileVisitor;
import io.takari.incrementalbuild.workspace.Workspace.Mode;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// XXX normalize all File parameters. maybe easier to use URI internally.
// XXX maybe use relative URIs to save heap

public abstract class DefaultBuildContext<BuildFailureException extends Exception>
    implements
      BuildContext {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final DefaultResourceTracker tracker;

  private final MessageSink messageSink;

  /**
   * Indicates that no further modifications to this build context are allowed. When context is
   * closed, all register* and process* methods throw {@link IllegalStateException} and
   * {@link #commit()} method does nothing.
   */
  private boolean closed;


  public DefaultBuildContext(Workspace workspace, MessageSink messageSink, File stateFile,
      Map<String, Serializable> configuration) {

    this.tracker = new DefaultResourceTracker(workspace, stateFile, configuration);

    this.messageSink = messageSink;
  }

  public boolean isEscalated() {
    return escalated;
  }

  private void assertOpen() {
    if (closed) {
      throw new IllegalStateException();
    }
  }

  public <T> DefaultResource<T> processInput(DefaultResourceMetadata<T> inputMetadata, boolean force) {
    if (inputMetadata.context != this) {
      throw new IllegalArgumentException();
    }

    if (inputMetadata instanceof DefaultResource) {
      return (DefaultResource<T>) inputMetadata;
    }

    T inputResource = inputMetadata.getResource();

    @SuppressWarnings("unchecked")
    DefaultResource<T> input = (DefaultResource<T>) processedInputs.get(inputResource);
    if (force || input == null) {
      input = new DefaultResource<T>(this, state, inputResource);
      processedInputs.put(inputResource, input);

      clearMessages(inputResource);

      // clean this build's metadata to accommodate reprocessing
      state.inputIncludedInputs.remove(inputResource);
      Collection<File> outputs = state.resourceOutputs.remove(inputResource);
      if (outputs != null) {
        for (File outputFile : outputs) {
          Collection<Object> others = state.outputInputs.get(outputFile);
          if (others != null) {
            others.remove(inputResource);
            if (others.isEmpty()) {
              state.outputInputs.remove(outputFile);
            }
          }
        }
      }
      state.resourceAttributes.remove(inputResource);
      state.resourceMessages.remove(inputResource);
    }

    return input;
  }

  @Override
  public Iterable<DefaultResource<File>> registerAndProcessInputs(File basedir,
      Collection<String> includes, Collection<String> excludes) throws IOException {
    basedir = normalize(basedir);
    final List<DefaultResource<File>> inputs = new ArrayList<DefaultResource<File>>();
    final FileMatcher matcher = FileMatcher.matcher(basedir, includes, excludes);
    workspace.walk(basedir, new FileVisitor() {
      @Override
      public void visit(File file, long lastModified, long length, Workspace.ResourceStatus status) {
        if (matcher.matches(file)) {
          switch (status) {
            case MODIFIED:
            case NEW: {
              DefaultResource<File> input = getProcessedInput(file);
              if (input == null) {
                DefaultResourceMetadata<File> metadata =
                    registerNormalizedInput(file, lastModified, length);
                if (workspace.getMode() == Mode.DELTA
                    || getResourceStatus(file, true) != ResourceStatus.UNMODIFIED) {
                  input = metadata.process();
                }
              }
              if (input != null) {
                inputs.add(input);
              }
              break;
            }
            case REMOVED:
              deletedInputs.add(file);
              break;
            default:
              throw new IllegalArgumentException();
          }
        }
      }
    });
    return inputs;
  }

  // low-level methods

  /**
   * Deletes outputs that were registered during the previous build but not the current build.
   * Usually not called directly, since it is automatically invoked during {@link #commit()}.
   * <p>
   * Result includes DefaultOutput instances removed from the state even if underlying file did not
   * exist.
   * <p>
   * If {@code eager == false}, preserves outputs associated with existing inputs during the
   * previous build and outputs that do not have associated inputs. This is useful if generator
   * needs access to old output files during multi-round build. For example, java incremental
   * compiler needs to compare old and new version of class files to determine if changes need to be
   * propagated.
   * 
   * @return deleted outputs
   * 
   * @throws IOException if an orphaned output file cannot be deleted.
   */
  public Iterable<DefaultOutputMetadata> deleteStaleOutputs(boolean eager) throws IOException {
    List<DefaultOutputMetadata> deleted = new ArrayList<DefaultOutputMetadata>();

    oldOutputs: for (File outputFile : oldState.outputs.keySet()) {
      // keep if output file was processed or marked as up-to-date during this build
      if (processedOutputs.containsKey(outputFile) || uptodateOutputs.contains(outputFile)) {
        continue oldOutputs;
      }

      Collection<Object> associatedInputs = oldState.outputInputs.get(outputFile);
      if (associatedInputs != null) {
        for (Object inputResource : associatedInputs) {

          // input is registered and not processed, not orphaned
          if (isRegistered(inputResource) && !processedInputs.containsKey(inputResource)) {
            continue oldOutputs;
          }

          final DefaultResource<?> input = processedInputs.get(inputResource);
          // if not eager, let the caller deal with the outputs
          if (input != null && (!eager || isAssociatedOutput(input, outputFile))) {
            // the oldOutput is associated with an input, not orphaned
            continue oldOutputs;
          }
        }
      } else if (!eager) {
        // outputs without inputs maybe recreated, retained during non-eager delete
        continue oldOutputs;
      }

      // don't double-delete already deleted outputs
      if (!deletedOutputs.add(outputFile)) {
        continue oldOutputs;
      }

      deleteStaleOutput(outputFile);

      deleted.add(new DefaultOutputMetadata(this, oldState, outputFile));
    }
    return deleted;
  }

  public Iterable<DefaultOutputMetadata> deleteStaleOutputs(DefaultResourceMetadata<?> input)
      throws IOException {
    List<DefaultOutputMetadata> deleted = new ArrayList<DefaultOutputMetadata>();

    Object inputResource = input.getResource();
    boolean registered = isRegistered(inputResource);

    if (registered && !processedInputs.containsKey(inputResource)) {
      // input is registered and not processed, all associated outputs should be carried over
      return deleted;
    }

    Collection<File> oldAssociatedOutputs = oldState.resourceOutputs.get(inputResource);

    if (oldAssociatedOutputs == null || oldAssociatedOutputs.isEmpty()) {
      // input didn't have associated outputs in the previous build, nothing to delete
      return deleted;
    }

    Collection<File> associatedOutputs = state.resourceOutputs.get(inputResource);
    for (File oldOutputFile : oldAssociatedOutputs) {
      if (associatedOutputs == null || !associatedOutputs.contains(oldOutputFile)) {
        deleteStaleOutput(oldOutputFile);
        deleted.add(new DefaultOutputMetadata(this, oldState, oldOutputFile));
      }
    }

    return deleted;
  }

  protected void deleteStaleOutput(File outputFile) throws IOException {
    workspace.deleteFile(outputFile);
  }

  public void deleteOutput(File outputFile) throws IOException {
    // XXX totally bogus. need to maintain state, things like deleted outputs and such
    workspace.deleteFile(outputFile);
  }

  @Override
  public DefaultOutput processOutput(File outputFile) {
    assertOpen();

    return tracker.processOutput(outputFile);
  }

  /**
   * Marks skipped build execution. All inputs, outputs and their associated metadata are carried
   * over to the next build as-is. No context modification operations (register* or process) are
   * permitted after this call.
   */
  public void markSkipExecution() {
    if (isModified()) {
      throw new IllegalStateException();
    }

    closed = true;
  }

  @Override
  public DefaultResourceMetadata<File> registerInput(File inputFile) {
    inputFile = normalize(inputFile);
    return registerNormalizedInput(inputFile, inputFile.lastModified(), inputFile.length());
  }

  public <T extends Serializable> DefaultResourceMetadata<T> registerInput(ResourceHolder<T> holder) {
    T resource = registerInput(state.resources, holder);

    // this returns different instance each invocation. This should not be a problem because
    // each instance is a stateless flyweight.

    return new DefaultResourceMetadata<T>(this, oldState, resource);
  }

  @Override
  public Collection<DefaultResourceMetadata<File>> registerInputs(File basedir,
      Collection<String> includes, Collection<String> excludes) throws IOException {
    assertOpen();

    return tracker.registerInputs(basedir, includes, excludes);
  }

  @Override
  public Iterable<DefaultOutputMetadata> getProcessedOutputs() {
    Set<DefaultOutputMetadata> result = new LinkedHashSet<DefaultOutputMetadata>();
    for (DefaultOutput output : processedOutputs.values()) {
      result.add(output);
    }
    for (File outputFile : oldState.outputs.keySet()) {
      if (!processedOutputs.containsKey(outputFile)) {
        Collection<Object> associatedInputs = oldState.outputInputs.get(outputFile);
        if (associatedInputs != null) {
          for (Object inputResource : associatedInputs) {
            if (isRegistered(inputResource) && !processedInputs.containsKey(inputResource)) {
              result.add(new DefaultOutputMetadata(this, oldState, outputFile));
              break;
            }
          }
        } else {
          result.add(new DefaultOutputMetadata(this, oldState, outputFile));
        }
      }
    }
    return result;
  }

  public DefaultOutputMetadata registerOutput(File outputFile) {
    outputFile = normalize(outputFile);
    DefaultOutputMetadata output = processedOutputs.get(outputFile);
    if (output == null) {
      output = new DefaultOutputMetadata(this, oldState, outputFile);
    }
    return output;
  }

  // association management

  public void associate(DefaultResource<?> resource, DefaultOutput output) {
    tracker.associate(resource, output);
  }

  // messages

  protected void log(Object resource, int line, int column, String message, Severity severity,
      Throwable cause) {
    switch (severity) {
      case ERROR:
        log.error("{}:[{}:{}] {}", resource.toString(), line, column, message, cause);
        break;
      case WARNING:
        log.warn("{}:[{}:{}] {}", resource.toString(), line, column, message, cause);
        break;
      default:
        log.info("{}:[{}:{}] {}", resource.toString(), line, column, message, cause);
        break;
    }
  }


  Collection<Message> getMessages(Object resource) {
    if (processedInputs.containsKey(resource) || processedOutputs.containsKey(resource)) {
      return state.resourceMessages.get(resource);
    }
    return oldState.resourceMessages.get(resource);
  }

  public void commit() throws BuildFailureException, IOException {
    if (closed) {
      return;
    }
    this.closed = true;

    deleteStaleOutputs(true);

    // carry over relevant parts of the old state

    Map<Object, Collection<Message>> newMessages = new HashMap<>(state.resourceMessages);
    Map<Object, Collection<Message>> recordedMessages = new HashMap<>();

    for (Object resource : oldState.resources.keySet()) {
      if (!isRegistered(resource)) {
        clearMessages(resource);
        continue;
      }
      if (!state.resources.containsKey(resource)) {
        // this is possible with delta workspaces
        state.resources.put(resource, oldState.resources.get(resource));
      }
      if (!processedInputs.containsKey(resource)) {
        // copy associated outputs
        Collection<File> associatedOutputs = oldState.resourceOutputs.get(resource);
        if (associatedOutputs != null) {
          for (File outputFile : associatedOutputs) {
            if (!processedOutputs.containsKey(outputFile)) {
              carryOverOutput(resource, outputFile);
              carryOverMessages(outputFile, recordedMessages);
            }
          }
        }

        // copy associated included inputs
        Collection<Object> includedInputs = oldState.inputIncludedInputs.get(resource);
        if (includedInputs != null) {
          state.inputIncludedInputs.put(resource, new LinkedHashSet<Object>(includedInputs));

          for (Object includedInput : includedInputs) {
            ResourceHolder<?> oldHolder = oldState.includedInputs.get(includedInput);
            state.includedInputs.put(oldHolder.getResource(), oldHolder);
          }
        }

        // copy messages
        carryOverMessages(resource, recordedMessages);

        // copy attributes
        Map<String, Serializable> attributes = oldState.resourceAttributes.get(resource);
        if (attributes != null) {
          state.resourceAttributes.put(resource, attributes);
        }
      }
    }

    for (ResourceHolder<?> resource : state.resources.values()) {
      if (getResourceStatus(resource) != ResourceStatus.UNMODIFIED) {
        throw new IllegalStateException("Unexpected input change " + resource.getResource());
      }
    }

    // carry over up-to-date output files
    for (File outputFile : oldState.outputs.keySet()) {
      if (uptodateOutputs.contains(outputFile)) {
        carryOverOutput(outputFile);
        carryOverMessages(outputFile, recordedMessages);
      }
    }

    // timestamp processed output files
    for (File outputFile : processedOutputs.keySet()) {
      if (state.outputs.get(outputFile) == null) {
        state.outputs.put(outputFile, newFileState(outputFile));
      }
    }

    if (stateFile != null) {
      final long start = System.currentTimeMillis();
      try (OutputStream os = workspace.newOutputStream(stateFile)) {
        state.storeTo(os);
      }
      log.debug("Stored incremental build state {} ({} ms)", stateFile, System.currentTimeMillis()
          - start);
    }

    if (!recordedMessages.isEmpty()) {
      log.info("Replaying recorded messages...");
      for (Map.Entry<Object, Collection<Message>> entry : state.resourceMessages.entrySet()) {
        Object resource = entry.getKey();
        for (Message message : entry.getValue()) {
          log(resource, message.line, message.column, message.message, message.severity,
              message.cause);
        }
      }
    }

    if (messageSink != null) {
      // let message sink record all new messages
      for (Map.Entry<Object, Collection<Message>> entry : newMessages.entrySet()) {
        Object resource = entry.getKey();
        for (Message message : entry.getValue()) {
          messageSink.message(resource, message.line, message.column, message.message,
              toMessageSinkSeverity(message.severity), message.cause);
        }
      }
    } else {
      // without messageSink, have to raise exception if there were errors
      int errorCount = 0;
      StringBuilder errors = new StringBuilder();
      for (Map.Entry<Object, Collection<Message>> entry : state.resourceMessages.entrySet()) {
        Object resource = entry.getKey();
        for (Message message : entry.getValue()) {
          if (message.severity == Severity.ERROR) {
            errorCount++;
            errors.append(String.format("%s:[%d:%d] %s\n", resource.toString(), message.line,
                message.column, message.message));
          }
        }
      }
      if (errorCount > 0) {
        throw newBuildFailureException(errorCount + " error(s) encountered:\n" + errors.toString());
      }
    }
  }

  private MessageSink.Severity toMessageSinkSeverity(Severity severity) {
    switch (severity) {
      case ERROR:
        return MessageSink.Severity.ERROR;
      case WARNING:
        return MessageSink.Severity.WARNING;
      case INFO:
        return MessageSink.Severity.INFO;
      default:
        throw new IllegalArgumentException();
    }
  }

  private void carryOverMessages(Object resource, Map<Object, Collection<Message>> recordedMessages) {
    Collection<Message> messages = oldState.resourceMessages.get(resource);
    if (messages != null && !messages.isEmpty()) {
      state.resourceMessages.put(resource, new ArrayList<Message>(messages));

      putAll(recordedMessages, resource, messages);
    }
  }

  protected void carryOverOutput(Object inputResource, File outputFile) {
    carryOverOutput(outputFile);

    put(state.resourceOutputs, inputResource, outputFile);
    put(state.outputInputs, outputFile, inputResource);
  }

  protected void carryOverOutput(File outputFile) {
    state.outputs.put(outputFile, oldState.outputs.get(outputFile));

    Map<String, Serializable> attributes = oldState.resourceAttributes.get(outputFile);
    if (attributes != null) {
      state.resourceAttributes.put(outputFile, attributes);
    }
  }

  public boolean isProcessingRequired() {
    if (escalated || isModified()) {
      return true;
    }
    for (Object inputResource : state.resources.keySet()) {
      ResourceHolder<?> oldInputState = oldState.resources.get(inputResource);
      if (oldInputState == null || getResourceStatus(oldInputState) != ResourceStatus.UNMODIFIED) {
        return true;
      }
    }
    for (Object oldInputResource : oldState.resources.keySet()) {
      if (!isRegistered(oldInputResource)) {
        return true;
      }
    }
    for (ResourceHolder<?> oldIncludedInputState : oldState.includedInputs.values()) {
      if (getResourceStatus(oldIncludedInputState) != ResourceStatus.UNMODIFIED) {
        return true;
      }
    }
    for (ResourceHolder<File> oldOutputState : oldState.outputs.values()) {
      if (getResourceStatus(oldOutputState) != ResourceStatus.UNMODIFIED) {
        return true;
      }
    }
    return false;
  }

  private boolean isModified() {
    return !processedInputs.isEmpty() || !processedOutputs.isEmpty() || !deletedOutputs.isEmpty()
        || !deletedInputs.isEmpty();
  }

  protected abstract BuildFailureException newBuildFailureException(String message);

  public Resource<File> getResource(File resourceFile) {
    resourceFile = normalize(resourceFile);
    @SuppressWarnings("unchecked")
    Resource<File> resource = (Resource<File>) processedInputs.get(resourceFile);
    if (resource == null) {
      resource = processedOutputs.get(resourceFile);
    }
    return resource;
  }
}
