package io.takari.incrementalbuild.aggregator.internal;

import io.takari.incrementalbuild.Output;
import io.takari.incrementalbuild.aggregator.AggregatorBuildContext;
import io.takari.incrementalbuild.spi.AbstractBuildContext;
import io.takari.incrementalbuild.spi.DefaultBuildContext;
import io.takari.incrementalbuild.spi.DefaultOutput;

import java.io.File;

import javax.inject.Inject;

public class DefaultAggregatorBuildContext extends AbstractBuildContext
    implements
      AggregatorBuildContext {

  @Inject
  public DefaultAggregatorBuildContext(DefaultBuildContext<?> context) {
    this.context = context;
  }

  @Override
  public DefaultAggregateOutput registerOutput(File outputFile) {
    DefaultOutput output = super.registerInput(outputFile)
    if (output instanceof Output<?>) {
      throw new IllegalStateException();
    }
    return new DefaultAggregateOutput(output);
  }

  @Override
  protected boolean shouldCarryOverOutput(File resource) {
    return false; // delete outputs that were not recreated during this build
  }

}
