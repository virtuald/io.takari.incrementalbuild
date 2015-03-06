package io.takari.incrementalbuild.aggregator;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Represents aggregate output being created.
 */
public interface AggregateOutput {

  public File getResource();

  /**
   * Creates the aggregate if there are new, changed or removed inputs.
   * 
   * @returns {@code true} if the new output was created, {@code false} if the output was
   *          up-to-date
   */
  public boolean createIfNecessary(AggregateCreator creator) throws IOException;

  /**
   * Adds inputs to the aggregate
   */
  public void addInputs(File basedir, Collection<String> includes, Collection<String> excludes,
      InputProcessor... processors) throws IOException;
}