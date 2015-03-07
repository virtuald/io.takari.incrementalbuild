package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.BuildContext;
import io.takari.incrementalbuild.ResourceStatus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultBuildContext extends AbstractBuildContext implements BuildContext {

  public DefaultBuildContext(BuildContextConfiguration configuration) {
    super(configuration);
  }

  @Override
  public Iterable<DefaultResource<File>> registerAndProcessInputs(File basedir,
      Collection<String> includes, Collection<String> excludes) throws IOException {
    basedir = normalize(basedir);
    final List<DefaultResource<File>> inputs = new ArrayList<DefaultResource<File>>();
    for (DefaultResourceMetadata<File> metadata : registerInputs(basedir, includes, excludes)) {
      if (metadata.getStatus() != ResourceStatus.UNMODIFIED) {
        inputs.add(metadata.process());
      }
    }
    return inputs;
  }

  @Override
  protected boolean shouldCarryOverOutput(File resource) {
    // output should be carried over if it's input is registered but not processed during this build
    Collection<Object> inputs = oldState.getOutputInputs(resource);
    if (inputs == null || inputs.size() != 1) {
      // this implementation only supports 1..* input/output association
      // it is not possible to create output without one and only onle associated input
      throw new IllegalStateException();
    }
    Object input = inputs.iterator().next();

    // input --> output --> output2 is not supported for now
    if (oldState.isOutput(input)) {
      throw new IllegalStateException();
    }

    return isRegisteredResource(input) && !isProcessedResource(input);
  }

  @Override
  public void markSkipExecution() {
    super.markSkipExecution();
  }

  @Override
  public DefaultResourceMetadata<File> registerInput(File inputFile) {
    return super.registerInput(inputFile);
  }

  @Override
  public Collection<DefaultResourceMetadata<File>> registerInputs(File basedir,
      Collection<String> includes, Collection<String> excludes) throws IOException {
    return super.registerInputs(basedir, includes, excludes);
  }

  @Override
  protected void assertAssociation(DefaultResource<?> resource, DefaultOutput output) {
    Object input = resource.getResource();
    File outputFile = output.getResource();

    // input --> output --> output2 is not supported (until somebody provides a usecase)
    if (state.isOutput(input)) {
      throw new UnsupportedOperationException();
    }

    // each output can only be associated with a single input
    Collection<Object> inputs = state.getOutputInputs(outputFile);
    if (inputs != null && !inputs.isEmpty() && !containsOnly(inputs, input)) {
      throw new UnsupportedOperationException();
    }
  }

  private static boolean containsOnly(Collection<Object> collection, Object element) {
    for (Object other : collection) {
      if (!element.equals(other)) {
        return true;
      }
    }
    return true;
  }
}
