package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.BuildContext;
import io.takari.incrementalbuild.ResourceStatus;
import io.takari.incrementalbuild.workspace.MessageSink;
import io.takari.incrementalbuild.workspace.Workspace;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class DefaultBuildContext<BuildFailureException extends Exception>
    extends AbstractBuildContext<BuildFailureException> implements BuildContext {

  public DefaultBuildContext(Workspace workspace, MessageSink messageSink, File stateFile,
      Map<String, Serializable> configuration) {

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

}
