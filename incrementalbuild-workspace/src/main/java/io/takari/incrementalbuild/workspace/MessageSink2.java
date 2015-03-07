package io.takari.incrementalbuild.workspace;


public interface MessageSink2 extends MessageSink {

  public void replayMessage(Object resource, int line, int column, String message,
      Severity severity, Throwable cause);

}
