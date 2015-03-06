package io.takari.incrementalbuild.aggregator.internal;


import static io.takari.incrementalbuild.spi.MMaps.put;
import io.takari.incrementalbuild.Resource;
import io.takari.incrementalbuild.ResourceMetadata;
import io.takari.incrementalbuild.ResourceStatus;
import io.takari.incrementalbuild.aggregator.AggregateCreator;
import io.takari.incrementalbuild.aggregator.AggregateInput;
import io.takari.incrementalbuild.aggregator.AggregatorBuildContext;
import io.takari.incrementalbuild.aggregator.InputProcessor;
import io.takari.incrementalbuild.spi.AbstractBuildContext;
import io.takari.incrementalbuild.spi.DefaultOutput;
import io.takari.incrementalbuild.workspace.Workspace;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultAggregatorBuildContext extends AbstractBuildContext
    implements
      AggregatorBuildContext {

  private final Map<File, File> inputBasedir = new HashMap<>();

  private final Map<File, Collection<File>> outputInputs = new HashMap<>();

  public DefaultAggregatorBuildContext(Workspace workspace, File stateFile,
      Map<String, Serializable> configuration) {
    super(workspace, stateFile, configuration);
  }

  @Override
  public DefaultAggregateMetadata registerOutput(File outputFile) {
    outputFile = normalize(outputFile);
    // throws IllegaleStateException if the output has already been generated
    registerNormalizedResource(outputFile, outputFile.lastModified(), outputFile.length(), false);
    return new DefaultAggregateMetadata(this, oldState, outputFile);
  }

  @Override
  protected boolean shouldCarryOverOutput(File resource) {
    return false; // delete outputs that were not recreated during this build
  }

  public void associatedInputs(DefaultAggregateMetadata output, File basedir,
      Collection<String> includes, Collection<String> excludes, InputProcessor... processors)
      throws IOException {
    basedir = normalize(basedir);
    for (ResourceMetadata<File> inputMetadata : registerInputs(basedir, includes, excludes)) {
      if (inputMetadata.getStatus() != ResourceStatus.UNMODIFIED) {
        Resource<File> input = inputMetadata.process();
        if (processors != null) {
          for (InputProcessor processor : processors) {
            processor.process(input);
          }
        }
      }
      put(outputInputs, output.getResource(), inputMetadata.getResource());
    }
  }

  public boolean createIfNecessary(DefaultAggregateMetadata outputMetadata, AggregateCreator creator)
      throws IOException {
    File outputFile = outputMetadata.getResource();
    boolean processingRequired = getResourceStatus(outputFile) != ResourceStatus.UNMODIFIED;
    Collection<File> inputFiles = outputInputs.get(outputMetadata.getResource());
    if (!processingRequired) {
      for (File input : inputFiles) {
        if (getResourceStatus(input) != ResourceStatus.UNMODIFIED) {
          processingRequired = true;
          break;
        }
      }
    }
    if (processingRequired) {
      DefaultOutput output = processOutput(outputFile);
      List<AggregateInput> inputs = new ArrayList<>();
      for (File inputFile : inputFiles) {
        if (!isProcessedResource(inputFile)) {
          processResource(inputFile);
        }
        state.putOutputInput(outputFile, inputFile);
        inputs.add(new DefaultAggregateInput(this, state, inputBasedir.get(inputFile), inputFile));
      }
      creator.create(output, inputs);
    }
    return processingRequired;
  }

}
