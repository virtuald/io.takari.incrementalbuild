package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.MessageSeverity;
import io.takari.incrementalbuild.Output;
import io.takari.incrementalbuild.Resource;

import java.io.File;
import java.io.Serializable;

/**
 * @noinstantiate clients are not expected to instantiate this class
 */
public class DefaultResource<T> extends DefaultResourceMetadata<T> implements Resource<T> {

  DefaultResource(AbstractBuildContext context, DefaultBuildContextState state, T resource) {
    super(context, state, resource);
  }

  @Override
  public DefaultOutput associateOutput(Output<File> output) {
    if (!(output instanceof DefaultOutput)) {
      throw new IllegalArgumentException();
    }
    return context.associate(this, (DefaultOutput) output);
  }

  @Override
  public DefaultOutput associateOutput(File outputFile) {
    return context.associate(this, outputFile);
  }

  @Override
  public <V extends Serializable> Serializable setAttribute(String key, V value) {
    return context.setResourceAttribute(resource, key, value);
  }

  @Override
  public void addMessage(int line, int column, String message, MessageSeverity severity,
      Throwable cause) {
    context.addMessage(getResource(), line, column, message, severity, cause);
  }

}
