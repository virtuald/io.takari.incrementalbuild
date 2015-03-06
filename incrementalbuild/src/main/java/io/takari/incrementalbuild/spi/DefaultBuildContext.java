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
  protected boolean isOutputUptodate(File resource) {


  }

}
