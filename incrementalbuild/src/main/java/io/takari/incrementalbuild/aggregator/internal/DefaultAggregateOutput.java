package io.takari.incrementalbuild.aggregator.internal;

import io.takari.incrementalbuild.Resource;
import io.takari.incrementalbuild.ResourceMetadata;
import io.takari.incrementalbuild.ResourceStatus;
import io.takari.incrementalbuild.aggregator.AggregateCreator;
import io.takari.incrementalbuild.aggregator.AggregateInput;
import io.takari.incrementalbuild.aggregator.AggregateOutput;
import io.takari.incrementalbuild.aggregator.InputProcessor;
import io.takari.incrementalbuild.spi.DefaultOutput;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class DefaultAggregateOutput implements AggregateOutput {

  private final ResourceMetadata<File> outputMetadata;

  private Collection<AggregateInput> inputs = new ArrayList<>();

  private boolean processingRequired;

  public DefaultAggregateOutput(ResourceMetadata<File> output) {
    this.outputMetadata = output;
    this.processingRequired = output.getStatus() != ResourceStatus.UNMODIFIED;
  }

  @Override
  public File getResource() {
    return outputMetadata.getResource();
  }

  @Override
  public void addInputs(File basedir, Collection<String> includes, Collection<String> excludes,
      InputProcessor... processors) throws IOException {
    basedir = basedir.getCanonicalFile(); // TODO move to DefaultBuildContext
    for (ResourceMetadata<File> inputMetadata : context.registerInputs(basedir, includes, excludes)) {
      if (context.getResourceStatus(inputMetadata.getResource(), false) != ResourceStatus.UNMODIFIED) {
        processingRequired = true;
        Resource<File> input = inputMetadata.process();
        if (processors != null) {
          for (InputProcessor processor : processors) {
            processor.process(input);
          }
        }
        inputs.add(new DefaultAggregateInput(basedir, input));
      } else {
        inputs.add(new DefaultAggregateInput(basedir, inputMetadata));
      }
    }
  }

  @Override
  public boolean createIfNecessary(AggregateCreator creator) throws IOException {
    if (!processingRequired) {
      for (ResourceMetadata<File> input : outputMetadata.getAssociatedInputs(File.class)) {
        if (input.getStatus() != ResourceStatus.UNMODIFIED) {
          processingRequired = true;
          break;
        }
      }
    }
    File outputFile = outputMetadata.getResource();
    if (processingRequired) {
      DefaultOutput output = context.processOutput(outputFile);
      for (AggregateInput aggregateInput : inputs) {
        output.associateInput(((DefaultAggregateInput) aggregateInput).input);
      }
      creator.create(output, inputs);
    } else {
      context.markOutputAsUptodate(outputFile);
    }

    return processingRequired;
  }
}
