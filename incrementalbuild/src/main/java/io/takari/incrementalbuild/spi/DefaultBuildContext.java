package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.BuildContext;
import io.takari.incrementalbuild.ResourceStatus;
import io.takari.incrementalbuild.workspace.Workspace;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DefaultBuildContext extends AbstractBuildContext implements BuildContext {

  public DefaultBuildContext(Workspace workspace, File stateFile,
      Map<String, Serializable> configuration) {
    super(workspace, stateFile, configuration);
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
  public <T> DefaultOutput associate(DefaultResource<T> resource, DefaultOutput output) {
    // this can be used to associated multiple inputs with the same output, so not supported
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> DefaultOutput associate(DefaultResource<T> resource, File outputFile) {
    if (state.outputs.contains(resource.getResource())) {
      // input --> output --> output2 is not supported (until somebody provides a usecase)
      throw new UnsupportedOperationException();
    }
    return super.associate(resource, outputFile);
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
    if (oldState.outputs.contains(input)) {
      throw new IllegalStateException();
    }

    return isRegisteredResource(input) && !isProcessedResource(input);
  }

}
