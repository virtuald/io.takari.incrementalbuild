package io.takari.incrementalbuild.aggregator.internal;

import io.takari.incrementalbuild.aggregator.AggregateInput;
import io.takari.incrementalbuild.spi.DefaultBuildContextState;
import io.takari.incrementalbuild.spi.DefaultResource;

import java.io.File;

public class DefaultAggregateInput extends DefaultResource<File> implements AggregateInput {
  private final File basedir;

  public DefaultAggregateInput(DefaultAggregatorBuildContext context,
      DefaultBuildContextState state, File basedir, File file) {
    super(context, state, file);
    this.basedir = basedir;
  }

  @Override
  public File getBasedir() {
    return basedir;
  }

}
