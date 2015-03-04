package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.BuildContext.ResourceMetadata;
import io.takari.incrementalbuild.BuildContext.ResourceStatus;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;

// XXX probably not useful when all said and done
interface ResourceTracker {

  ResourceStatus getResourceStatus(Object resource);

  <T extends Serializable> T getResourceAttribute(DefaultBuildContextState state, Object resource,
      String key, Class<T> clazz);

  <T> DefaultResource<T> processResource(DefaultResourceMetadata<T> metadata);

  Collection<? extends ResourceMetadata<File>> getAssociatedOutputs(DefaultBuildContextState state,
      Object resource);

  <T extends Serializable> Serializable setResourceAttribute(Object resource, String key, T value);

}
