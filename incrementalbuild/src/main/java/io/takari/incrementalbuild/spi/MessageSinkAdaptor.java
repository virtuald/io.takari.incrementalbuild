package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.MessageSeverity;
import io.takari.incrementalbuild.workspace.MessageSink;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
class MessageSinkAdaptor {

  private final MessageSink messageSink;

  @Inject
  public MessageSinkAdaptor(@Nullable MessageSink messageSink) {
    this.messageSink = messageSink;
  }

  public void record(Map<Object, Collection<Message>> messages) {
    if (messageSink != null) {
      for (Map.Entry<Object, Collection<Message>> entry : messages.entrySet()) {
        Object resource = entry.getKey();
        for (Message message : entry.getValue()) {
          messageSink.message(resource, message.line, message.column, message.message,
              toMessageSinkSeverity(message.severity), message.cause);
        }
      }
    }
  }

  public void clear(Object resource) {
    if (messageSink != null) {
      messageSink.clearMessages(resource);
    }
  }

  private MessageSink.Severity toMessageSinkSeverity(MessageSeverity severity) {
    switch (severity) {
      case ERROR:
        return MessageSink.Severity.ERROR;
      case WARNING:
        return MessageSink.Severity.WARNING;
      case INFO:
        return MessageSink.Severity.INFO;
      default:
        throw new IllegalArgumentException();
    }
  }

}
