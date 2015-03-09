package io.takari.incrementalbuild.aggregator.internal;


import io.takari.incrementalbuild.Resource;
import io.takari.incrementalbuild.ResourceMetadata;
import io.takari.incrementalbuild.ResourceStatus;
import io.takari.incrementalbuild.aggregator.AggregateCreator;
import io.takari.incrementalbuild.aggregator.AggregateInput;
import io.takari.incrementalbuild.aggregator.AggregatorBuildContext;
import io.takari.incrementalbuild.aggregator.InputProcessor;
import io.takari.incrementalbuild.aggregator.MetadataAggregateCreator;
import io.takari.incrementalbuild.spi.AbstractBuildContext;
import io.takari.incrementalbuild.spi.BuildContextEnvironment;
import io.takari.incrementalbuild.spi.DefaultBuildContextState;
import io.takari.incrementalbuild.spi.DefaultOutput;
import io.takari.incrementalbuild.spi.DefaultResource;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DefaultAggregatorBuildContext extends AbstractBuildContext
    implements
      AggregatorBuildContext {

  private static final String KEY_METADATA = "aggregate.metadata";

  private final Map<File, File> inputBasedir = new HashMap<>();

  private final Map<File, Collection<Object>> outputInputs = new HashMap<>();

  private final Map<File, Map<String, Serializable>> outputMetadata = new HashMap<>();

  public DefaultAggregatorBuildContext(BuildContextEnvironment configuration) {
    super(configuration);
  }

  @Override
  public DefaultAggregateMetadata registerOutput(File outputFile) {
    outputFile = normalize(outputFile);
    if (isRegisteredResource(outputFile)) {
      // only allow single registrator of the same output. not sure why/if multuple will be needed
      throw new IllegalStateException("Output already registrered " + outputFile);
    }
    registerNormalizedOutput(outputFile);
    return new DefaultAggregateMetadata(this, oldState, outputFile);
  }

  @Override
  protected boolean shouldCarryOverOutput(File resource) {
    return false; // delete outputs that were not recreated during this build
  }

  public void associateInputs(DefaultAggregateMetadata output, File basedir,
      Collection<String> includes, Collection<String> excludes, InputProcessor... processors)
      throws IOException {
    basedir = normalize(basedir);

    File outputFile = output.getResource();

    Collection<Object> inputs = outputInputs.get(outputFile);
    if (inputs == null) {
      inputs = new ArrayList<>();
      outputInputs.put(outputFile, inputs);
    }
    Map<String, Serializable> metadata = outputMetadata.get(outputFile);
    if (metadata == null) {
      metadata = new HashMap<>();
      outputMetadata.put(outputFile, metadata);
    }

    for (ResourceMetadata<File> inputMetadata : registerInputs(basedir, includes, excludes)) {
      inputBasedir.put(inputMetadata.getResource(), basedir); // TODO move to FileState
      inputs.add(inputMetadata.getResource());
      if (inputMetadata.getStatus() != ResourceStatus.UNMODIFIED) {
        if (isProcessedResource(inputMetadata.getResource())) {
          // don't know all implications, will deal when/if anyone asks for it
          throw new IllegalStateException("Input already processed " + inputMetadata.getResource());
        }
        Resource<File> input = inputMetadata.process();
        if (processors != null) {
          for (InputProcessor processor : processors) {
            Map<String, Serializable> processed = processor.process(input);
            if (processed != null) {
              input.setAttribute(KEY_METADATA, new HashMap<String, Serializable>(processed));
              metadata.putAll(processed);
            }
          }
        }
      } else {
        @SuppressWarnings("unchecked")
        HashMap<String, Serializable> persisted =
            inputMetadata.getAttribute(KEY_METADATA, HashMap.class);
        if (persisted != null) {
          metadata.putAll(persisted);
        }
      }
    }
  }

  public boolean createIfNecessary(DefaultAggregateMetadata outputMetadata, AggregateCreator creator)
      throws IOException {
    File outputFile = outputMetadata.getResource();
    boolean processingRequired = isEscalated();
    if (!processingRequired) {
      processingRequired = isProcessingRequired(outputFile);
    }
    if (processingRequired) {
      Collection<Object> inputFiles = outputInputs.get(outputFile);
      DefaultOutput output = processOutput(outputFile);
      List<AggregateInput> inputs = new ArrayList<>();
      if (inputFiles != null) {
        for (Object inputFile : inputFiles) {
          if (!isProcessedResource(inputFile)) {
            processResource(inputFile);
          }
          state.putResourceOutput(inputFile, outputFile);
          inputs.add(newAggregateInput((File) inputFile, true /* processed */));
        }
      }
      creator.create(output, inputs);
    } else {
      markUptodateOutput(outputFile);
    }
    return processingRequired;
  }

  private DefaultAggregateInput newAggregateInput(File inputFile, boolean processed) {
    DefaultBuildContextState state;
    if (processed) {
      state = isProcessedResource(inputFile) ? this.state : this.oldState;
    } else {
      state = this.oldState;
    }
    return new DefaultAggregateInput(this, state, inputBasedir.get(inputFile), inputFile);
  }

  // re-create output if any its inputs were added, changed or deleted since previous build
  private boolean isProcessingRequired(File outputFile) {
    Collection<Object> inputs = outputInputs.get(outputFile);
    if (inputs != null) {
      for (Object input : inputs) {
        if (getResourceStatus(input) != ResourceStatus.UNMODIFIED) {
          return true;
        }
      }
    }

    Collection<Object> oldInputs = oldState.getOutputInputs(outputFile);
    if (oldInputs != null) {
      for (Object oldInput : oldInputs) {
        if (!inputs.contains(oldInput)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  protected void assertAssociation(DefaultResource<?> resource, DefaultOutput output) {
    // aggregating context supports any association between inputs and outputs
    // or, rather, there is no obviously wrong combination
  }

  public boolean createIfNecessary(DefaultAggregateMetadata outputMetadata,
      MetadataAggregateCreator creator) throws IOException {
    File outputFile = outputMetadata.getResource();
    Map<String, Serializable> metadata = this.outputMetadata.get(outputFile);
    boolean processingRequired = isEscalated();
    if (!processingRequired) {
      HashMap<String, Serializable> oldMetadata = new HashMap<>();
      Collection<Object> oldInputs = oldState.getOutputInputs(outputFile);
      if (oldInputs != null) {
        for (Object input : oldInputs) {
          putAll(oldMetadata, oldState.getResourceAttribute(input, KEY_METADATA));
        }
      }
      processingRequired = !Objects.equals(metadata, oldMetadata);
    }
    if (processingRequired) {
      DefaultOutput output = processOutput(outputFile);
      Collection<Object> inputs = outputInputs.get(outputFile);
      if (inputs != null) {
        for (Object input : inputs) {
          state.putResourceOutput(input, outputFile);
        }
      }
      creator.create(output, metadata);
    } else {
      markUptodateOutput(outputFile);
    }
    return processingRequired;
  }

  @SuppressWarnings("unchecked")
  private <K, V> void putAll(Map<K, V> target, Serializable source) {
    if (source != null) {
      target.putAll((Map<K, V>) source);
    }
  }
}
