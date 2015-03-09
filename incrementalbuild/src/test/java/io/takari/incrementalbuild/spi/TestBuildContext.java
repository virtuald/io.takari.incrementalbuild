package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.ResourceStatus;
import io.takari.incrementalbuild.workspace.Workspace;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

class TestBuildContext extends DefaultBuildContext {

  public TestBuildContext(File stateFile, Map<String, Serializable> configuration) {
    this(new FilesystemWorkspace(), stateFile, configuration);
  }

  public TestBuildContext(Workspace workspace, File stateFile,
      Map<String, Serializable> configuration) {
    super(workspace, stateFile, configuration, null);
  }

  public void commit() throws IOException {
    super.commit(null);
  }

  public Collection<Object> getRegisteredInputs() {

  }

  public ResourceStatus getResourceStatus(Object resource) {
    return super.getResourceStatus(resource);
  }
}
