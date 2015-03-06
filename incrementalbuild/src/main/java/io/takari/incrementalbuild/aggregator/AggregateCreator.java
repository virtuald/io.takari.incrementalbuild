package io.takari.incrementalbuild.aggregator;

import io.takari.incrementalbuild.Output;

import java.io.File;
import java.io.IOException;

/**
 * Aggregation function. Only called when new output needs to be generated.
 */
public interface AggregateCreator {

  /**
   * Creates aggregate output given the inputs.
   */
  public void create(Output<File> output, Iterable<AggregateInput> inputs) throws IOException;
}
