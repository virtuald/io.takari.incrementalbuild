package io.takari.incrementalbuild.maven.testing;

import io.takari.incrementalbuild.maven.internal.MavenBuildContextFinalizer;
import io.takari.incrementalbuild.spi.AbstractBuildContext;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.plugin.MojoExecutionException;


@Named
@MojoExecutionScoped
class TestBuildContextFinalizer extends MavenBuildContextFinalizer {

  private final IncrementalBuildLog log;

  @Inject
  public TestBuildContextFinalizer(IncrementalBuildLog log) {
    this.log = log;
  }

  @Override
  public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
    try {
      super.afterMojoExecutionSuccess(event);
    } finally {
      for (AbstractBuildContext context : getRegisteredContexts()) {
        // carried over outputs
        for (File output : context.getState().getOutputs()) {
          // if not processed during this build it must have been carried over
          if (!log.getRegisteredOutputs().contains(output)) {
            log.addCarryoverOutput(output);
          }
        }

        // carried over messages

      }
    }
  }
}
