package io.takari.incrementalbuild.maven.internal;

import io.takari.incrementalbuild.maven.internal.digest.MojoConfigurationDigester;
import io.takari.incrementalbuild.spi.BuildContextConfiguration;
import io.takari.incrementalbuild.workspace.Workspace;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.scope.MojoExecutionScoped;

// TODO merge with MavenIncrementalConventions, not sure we need both

@Named
@MojoExecutionScoped
public class MavenBuildContextConfiguration implements BuildContextConfiguration {

  private final ProjectWorkspace workspace;
  private final File stateFile;
  private Map<String, Serializable> parameters;

  @Inject
  public MavenBuildContextConfiguration(ProjectWorkspace workspace,
      MavenIncrementalConventions conventions, MojoConfigurationDigester digester)
      throws IOException {
    this.workspace = workspace;
    this.stateFile = conventions.getExecutionStateLocation();
    this.parameters = digester.digest();
  }

  @Override
  public File getStateFile() {
    return stateFile;
  }

  @Override
  public Workspace getWorkspace() {
    return workspace;
  }

  @Override
  public Map<String, Serializable> getParameters() {
    return parameters;
  }

}
