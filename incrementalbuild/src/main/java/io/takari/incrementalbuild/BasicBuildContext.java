package io.takari.incrementalbuild;

import java.io.File;

/**
 * Build context that tracks inputs and outputs but not associations among them.
 */
public interface BasicBuildContext {
  public ResourceMetadata<File> registerInput(File inputFile);

  public boolean isProcessingRequired();

  public Output<File> processOutput(File outputFile);
}
