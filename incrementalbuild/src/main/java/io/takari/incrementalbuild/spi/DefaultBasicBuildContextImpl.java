package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.BasicBuildContext;
import io.takari.incrementalbuild.ResourceStatus;

import java.io.File;

public class DefaultBasicBuildContextImpl extends AbstractBuildContext implements BasicBuildContext {

  public DefaultBasicBuildContextImpl(BuildContextConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected boolean shouldCarryOverOutput(File resource) {
    return false; // delete all outputs that were not processed during this build
  }

  @Override
  public boolean isProcessingRequired() {
    for (Object resource : state.resourcesMap().keySet()) {
      if (!state.isOutput(resource) && getResourceStatus(resource) != ResourceStatus.UNMODIFIED) {
        return true;
      }
    }
    for (Object oldResource : oldState.resourcesMap().keySet()) {
      if (!oldState.isOutput(oldResource) && !state.isResource(oldResource)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public DefaultOutput processOutput(File outputFile) {
    return super.processOutput(outputFile);
  }

  @Override
  public DefaultResourceMetadata<File> registerInput(File inputFile) {
    return super.registerInput(inputFile);
  }

  @Override
  protected void assertAssociation(DefaultResource<?> resource, DefaultOutput output) {
    // this context does not track input/output association, so lets make it clear to the users
    throw new UnsupportedOperationException();
  }
}
