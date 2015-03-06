package io.takari.incrementalbuild.maven.internal;

import io.takari.incrementalbuild.workspace.MessageSink;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.execution.scope.WeakMojoExecutionListener;
import org.apache.maven.plugin.MojoExecutionException;

@Named
@MojoExecutionScoped
public class MavenBuildContext implements WeakMojoExecutionListener {

  private final MessageSink messageSink;

  @Inject
  public MavenBuildContext(@Nullable MessageSink messageSink) throws IOException {
    this.messageSink = messageSink;
  }

  @Override
  public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
    try {
      commit();
    } catch (IOException e) {
      throw new MojoExecutionException("Could not maintain incremental build state", e);
    }
  }

  @Override
  public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {}

  @Override
  public void afterExecutionFailure(MojoExecutionEvent event) {}

}
