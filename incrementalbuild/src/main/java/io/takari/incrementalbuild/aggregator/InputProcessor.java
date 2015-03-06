package io.takari.incrementalbuild.aggregator;

import io.takari.incrementalbuild.Resource;

import java.io.File;
import java.io.IOException;

/**
 * Aggregate input processor. Useful to glean information from input resource and store it in
 * Input attributes.
 */
public interface InputProcessor {
  public void process(Resource<File> input) throws IOException;
}