package io.takari.incrementalbuild.aggregator.internal;

import io.takari.incrementalbuild.Resource;
import io.takari.incrementalbuild.ResourceMetadata;
import io.takari.incrementalbuild.ResourceStatus;
import io.takari.incrementalbuild.aggregator.AggregateInput;

import java.io.File;
import java.io.Serializable;

public class DefaultAggregateInput implements AggregateInput {
  private final File basedir;

  final ResourceMetadata<File> input;

  public DefaultAggregateInput(File basedir, ResourceMetadata<File> input) {
    this.basedir = basedir;
    this.input = input;
  }

  @Override
  public File getResource() {
    return input.getResource();
  }

  @Override
  public File getBasedir() {
    return basedir;
  }

  @Override
  public ResourceStatus getStatus() {
    return input.getStatus();
  }

  @Override
  public Iterable<? extends ResourceMetadata<File>> getAssociatedOutputs() {
    return input.getAssociatedOutputs();
  }

  @Override
  public Resource<File> process() {
    return input.process();
  }

  @Override
  public <V extends Serializable> V getAttribute(String key, Class<V> clazz) {
    return input.getAttribute(key, clazz);
  }
}