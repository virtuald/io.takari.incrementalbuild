package io.takari.incrementalbuild.maven.internal;

import io.takari.incrementalbuild.spi.BuildContextConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

@Named
@MojoExecutionScoped
public class MavenBuildContextConfiguration implements BuildContextConfiguration {

  private final File stateFile;
  private final Map<String, Serializable> configuration;

  @Inject
  public MavenBuildContextConfiguration(MojoConfigurationDigester digester,
      MavenIncrementalConventions conventions, MavenProject project, MojoExecution execution)
      throws IOException {
    stateFile = conventions.getExecutionStateLocation(project, execution);
    configuration = digester.digest();
  }

  @Override
  public File getStateFile() {
    return stateFile;
  }

  @Override
  public Map<String, Serializable> getConfiguration() {
    return configuration;
  }
}
