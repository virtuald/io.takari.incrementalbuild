package io.takari.incrementalbuild.maven.testing;

import io.takari.incrementalbuild.MessageSeverity;
import io.takari.incrementalbuild.maven.internal.MavenBuildContext;
import io.takari.incrementalbuild.maven.internal.MavenIncrementalConventions;
import io.takari.incrementalbuild.maven.internal.MojoConfigurationDigester;
import io.takari.incrementalbuild.spi.DefaultOutput;
import io.takari.incrementalbuild.workspace.Workspace;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

// this is explicitly bound in IncrementalBuildRuntime.addGuiceModules
class TestBuildContext extends MavenBuildContext.MojoExecutionScopedBuildContext {

  private final IncrementalBuildLog logger;

  @Inject
  public TestBuildContext(Workspace workspace, MavenIncrementalConventions convensions,
      MojoConfigurationDigester digester, IncrementalBuildLog logger) throws IOException {
    super(workspace, convensions, digester);
    this.logger = logger;
  }

  @Override
  public DefaultOutput processOutput(File outputFile) {
    DefaultOutput output = super.processOutput(outputFile);
    logger.addRegisterOutput(output.getResource());
    return output;
  }

  @Override
  protected boolean shouldCarryOverOutput(File resource) {
    boolean carryOver = super.shouldCarryOverOutput(resource);
    if (carryOver) {
      logger.addCarryoverOutput(resource);
    } else {
      logger.addDeletedOutput(resource);
    }
    return carryOver;
  }

  @Override
  protected void log(Object resource, int line, int column, String message,
      MessageSeverity severity, Throwable cause) {
    logger.message(resource, line, column, message, severity, cause);
  }
}
