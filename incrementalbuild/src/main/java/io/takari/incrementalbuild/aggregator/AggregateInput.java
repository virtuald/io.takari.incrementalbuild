package io.takari.incrementalbuild.aggregator;

import io.takari.incrementalbuild.ResourceMetadata;

import java.io.File;

// TODO this will go away once #getRelativePath moves to InputMetadata
public interface AggregateInput extends ResourceMetadata<File> {

  /**
   * When input was registered using glob matching, returns base directory of the match.
   */
  public File getBasedir();
}
