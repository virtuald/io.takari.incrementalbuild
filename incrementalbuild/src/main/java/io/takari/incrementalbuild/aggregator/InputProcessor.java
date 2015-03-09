package io.takari.incrementalbuild.aggregator;

import io.takari.incrementalbuild.Resource;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * Aggregate input processor. Useful to glean information from input resource.
 */
public interface InputProcessor {
  public Map<String, Serializable> process(Resource<File> input) throws IOException;
}
