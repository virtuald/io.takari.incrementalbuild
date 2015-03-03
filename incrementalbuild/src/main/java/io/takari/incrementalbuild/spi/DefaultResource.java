package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.BuildContext.Output;
import io.takari.incrementalbuild.BuildContext.Resource;
import io.takari.incrementalbuild.BuildContext.Severity;

import java.io.File;
import java.io.Serializable;

/**
 * @noinstantiate clients are not expected to instantiate this class
 */
public class DefaultResource<T> extends DefaultResourceMetadata<T> implements Resource<T> {

  DefaultResource(DefaultBuildContext<?> context, DefaultBuildContextState state, T resource) {
    super(context, state, resource);
  }

  @Override
  public DefaultOutput associateOutput(Output<File> output) {
    if (!(output instanceof DefaultOutput)) {
      throw new IllegalArgumentException();
    }
    context.associate(this, (DefaultOutput) output);
    return (DefaultOutput) output;
  }

  @Override
  public DefaultOutput associateOutput(File outputFile) {
    DefaultOutput output = context.processOutput(outputFile);
    context.associate(this, output);
    return output;
  }

  @Override
  public <V extends Serializable> Serializable setAttribute(String key, V value) {
    return context.setResourceAttribute(resource, key, value);
  }

  @Override
  public void addMessage(int line, int column, String message, Severity severity, Throwable cause) {
    context.addMessage(getResource(), line, column, message, severity, cause);
  }

}
