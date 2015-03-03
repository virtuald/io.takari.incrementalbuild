package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.BuildContext.ResourceMetadata;
import io.takari.incrementalbuild.BuildContext.ResourceStatus;

import java.io.File;
import java.io.Serializable;

/**
 * @noinstantiate clients are not expected to instantiate this class
 */
public class DefaultResourceMetadata<T> implements ResourceMetadata<T> {

  final DefaultBuildContext<?> context;

  final DefaultBuildContextState state;

  final T resource;

  DefaultResourceMetadata(DefaultBuildContext<?> context, DefaultBuildContextState state, T resource) {
    this.context = context;
    this.state = state;
    this.resource = resource;
  }

  @Override
  public T getResource() {
    return resource;
  }

  @Override
  public Iterable<? extends ResourceMetadata<File>> getAssociatedOutputs() {
    return context.getAssociatedOutputs(state, resource);
  }

  @Override
  public ResourceStatus getStatus() {
    return context.getResourceStatus(resource, true /* associated */);
  }

  @Override
  public <V extends Serializable> V getAttribute(String key, Class<V> clazz) {
    return context.getResourceAttribute(resource, key, true /* previous */, clazz);
  }

  @Override
  public DefaultResource<T> process() {
    return context.processResource(this);
  }

  @Override
  public int hashCode() {
    return resource.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }

    DefaultResourceMetadata<?> other = (DefaultResourceMetadata<?>) obj;

    // must be from the same context to be equal
    return context == other.context && state == other.state && resource.equals(other.resource);
  }

}
