package io.takari.incrementalbuild;

import java.io.File;
import java.io.Serializable;

public interface Resource<T> extends ResourceMetadata<T> {

  public <V extends Serializable> Serializable setAttribute(String key, V value);

  public void addMessage(int line, int column, String message, MessageSeverity severity,
      Throwable cause);

  public Output<File> associateOutput(Output<File> output);

  public Output<File> associateOutput(File outputFile);
}
