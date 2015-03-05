package io.takari.incrementalbuild.aggregator.internal;

import io.takari.incrementalbuild.Output;
import io.takari.incrementalbuild.Resource;
import io.takari.incrementalbuild.ResourceMetadata;
import io.takari.incrementalbuild.ResourceStatus;
import io.takari.incrementalbuild.aggregator.AggregatorBuildContext;
import io.takari.incrementalbuild.spi.DefaultBuildContext;
import io.takari.incrementalbuild.spi.DefaultOutput;
import io.takari.incrementalbuild.spi.DefaultOutputMetadata;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DefaultAggregatorBuildContext implements AggregatorBuildContext {

  public class DefaultAggregateInput implements AggregateInput {
    private final File basedir;

    private final ResourceMetadata<File> input;

    public DefaultAggregateInput(File basedir, ResourceMetadata<File> input) {
      this.basedir = basedir;
      this.input = input;
    }

    @Override
    public File getResource() {
      return input.getResource();
    }

    @Override
    public File getBasedir() {
      return basedir;
    }

    @Override
    public ResourceStatus getStatus() {
      return input.getStatus();
    }

    @Override
    public Iterable<? extends ResourceMetadata<File>> getAssociatedOutputs() {
      return input.getAssociatedOutputs();
    }

    @Override
    public Resource<File> process() {
      return input.process();
    }

    @Override
    public <V extends Serializable> V getAttribute(String key, Class<V> clazz) {
      return input.getAttribute(key, clazz);
    }
  }

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
      for (ResourceMetadata<File> inputMetadata : context.registerInputs(basedir, includes,
          excludes)) {
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

  private final DefaultBuildContext<?> context;

  @Inject
  public DefaultAggregatorBuildContext(DefaultBuildContext<?> context) {
    this.context = context;
  }

  public DefaultAggregateOutput registerOutput(File outputFile) {
    DefaultOutputMetadata output = context.registerOutput(outputFile);
    if (output instanceof Output<?>) {
      throw new IllegalStateException();
    }
    return new DefaultAggregateOutput(output);
  }

}
