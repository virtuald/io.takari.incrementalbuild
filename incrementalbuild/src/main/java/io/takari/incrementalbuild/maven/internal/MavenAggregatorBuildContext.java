package io.takari.incrementalbuild.maven.internal;

import io.takari.incrementalbuild.aggregator.AggregateMetadata;
import io.takari.incrementalbuild.aggregator.AggregatorBuildContext;
import io.takari.incrementalbuild.aggregator.internal.DefaultAggregatorBuildContext;
import io.takari.incrementalbuild.spi.BuildContextConfiguration;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.execution.scope.MojoExecutionScoped;

@Named(MavenAggregatorBuildContext.HINT)
@Singleton
public class MavenAggregatorBuildContext implements AggregatorBuildContext {
  public static final String HINT = "singleton";

  @Named
  @MojoExecutionScoped
  public static class MojoExecutionScopedAggregatorBuildContext
      extends DefaultAggregatorBuildContext {

    @Inject
    public MojoExecutionScopedAggregatorBuildContext(BuildContextConfiguration configuration) {
      super(configuration);
    }
  }

  private final Provider<MojoExecutionScopedAggregatorBuildContext> provider;

  @Inject
  public MavenAggregatorBuildContext(Provider<MojoExecutionScopedAggregatorBuildContext> provider) {
    this.provider = provider;
  }

  @Override
  public AggregateMetadata registerOutput(File output) {
    return provider.get().registerOutput(output);
  }

}
