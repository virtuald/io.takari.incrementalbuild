package io.takari.incrementalbuild;

import java.io.File;
import java.io.Serializable;

public interface ResourceMetadata<T> {

  public T getResource();

  public ResourceStatus getStatus();

  /**
   * Returns current attribute value.
   * <p>
   * For registered (but not processed) resources and carried over resources, returns value
   * associated with the key during previous build. For processed resources, returns value
   * associated with the key during this build.
   */
  public <V extends Serializable> V getAttribute(String key, Class<V> clazz);

  /**
   * Returns outputs associated with this resource.
   * <p>
   * For registered (but not processed) resources and carried over resources returns outputs
   * associated with the resource during previous build. For processed resources, returns outputs
   * associated with the resource during this build.
   */
  public Iterable<? extends ResourceMetadata<File>> getAssociatedOutputs();

  public Resource<T> process();

}
