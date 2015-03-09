package io.takari.incrementalbuild.aggregator;

import io.takari.incrementalbuild.Output;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public interface MetadataAggregateCreator {
  public void create(Output<File> output, Map<String, Serializable> metadata) throws IOException;

}
