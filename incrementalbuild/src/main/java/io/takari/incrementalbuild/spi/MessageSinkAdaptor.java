package io.takari.incrementalbuild.spi;

import java.util.Collection;
import java.util.Map;


public interface MessageSinkAdaptor {

  public static final MessageSinkAdaptor DEFAULT = new MessageSinkAdaptor() {
    @Override
    public void record(Map<Object, Collection<Message>> allMessages,
        Map<Object, Collection<Message>> newMessages) {}

    @Override
    public void clear(Object resource) {}
  };

  void clear(Object resource);

  void record(Map<Object, Collection<Message>> allMessages,
      Map<Object, Collection<Message>> newMessages);

  // private MessageSink.Severity toMessageSinkSeverity(MessageSeverity severity) {
  // switch (severity) {
  // case ERROR:
  // return MessageSink.Severity.ERROR;
  // case WARNING:
  // return MessageSink.Severity.WARNING;
  // case INFO:
  // return MessageSink.Severity.INFO;
  // default:
  // throw new IllegalArgumentException();
  // }
  // }
}
