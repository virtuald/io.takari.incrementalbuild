package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.BasicBuildContext;
import io.takari.incrementalbuild.ResourceStatus;
import io.takari.incrementalbuild.workspace.Workspace;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

public class DefaultBasicBuildContextImpl extends AbstractBuildContext implements BasicBuildContext {

  public DefaultBasicBuildContextImpl(Workspace workspace, File stateFile,
      Map<String, Serializable> configuration) {
    super(workspace, stateFile, configuration);
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

}
