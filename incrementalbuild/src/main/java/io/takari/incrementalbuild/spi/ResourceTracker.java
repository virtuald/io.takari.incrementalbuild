package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.BuildContext.ResourceMetadata;
import io.takari.incrementalbuild.BuildContext.ResourceStatus;
import io.takari.incrementalbuild.BuildContext.Severity;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;

// XXX probably not useful when all said and done
interface ResourceTracker {

  ResourceStatus getResourceStatus(Object resource);

  <T> DefaultResource<T> processResource(DefaultResourceMetadata<T> metadata);

  Collection<? extends ResourceMetadata<File>> getAssociatedOutputs(DefaultBuildContextState state,
      Object resource);

  <T extends Serializable> Serializable setResourceAttribute(Object resource, String key, T value);

  <T extends Serializable> T getResourceAttribute(DefaultBuildContextState state, Object resource,
      String key, Class<T> clazz);

  void addMessage(Object resource, int line, int column, String message, Severity severity,
      Throwable cause);

  DefaultOutput processOutput(File outputFile);

  OutputStream newOutputStream(DefaultOutput output) throws IOException;

  <T> void associate(DefaultResource<T> defaultResource, DefaultOutput output);
}
