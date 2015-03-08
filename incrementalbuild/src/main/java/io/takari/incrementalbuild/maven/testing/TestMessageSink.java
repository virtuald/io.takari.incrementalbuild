package io.takari.incrementalbuild.maven.testing;

import io.takari.incrementalbuild.workspace.MessageSink2;

import javax.inject.Inject;

//this is explicitly bound in IncrementalBuildRuntime.addGuiceModules
class TestMessageSink implements MessageSink2 {

  private final IncrementalBuildLog log;

  @Inject
  public TestMessageSink(IncrementalBuildLog log) {
    this.log = log;
  }

  @Override
  public void clearMessages(Object resource) {}

  @Override
  public void message(Object resource, int line, int column, String message, Severity severity,
      Throwable cause) {
    log.message(resource, line, column, message, severity, cause);
  }

  @Override
  public void replayMessage(Object resource, int line, int column, String message,
      Severity severity, Throwable cause) {
    log.message(resource, line, column, message, severity, cause);
  }

}
