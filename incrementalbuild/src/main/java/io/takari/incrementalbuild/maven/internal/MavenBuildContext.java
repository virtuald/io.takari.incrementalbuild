package io.takari.incrementalbuild.maven.internal;

import io.takari.incrementalbuild.BuildContext;
import io.takari.incrementalbuild.Resource;
import io.takari.incrementalbuild.ResourceMetadata;
import io.takari.incrementalbuild.spi.BuildContextConfiguration;
import io.takari.incrementalbuild.spi.DefaultBuildContext;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.execution.scope.MojoExecutionScoped;

/**
 * Maven Guice/JSR330 bindings for {@link DefaultBuildContext}.
 * <p>
 * {@link MojoExecutionScopedBuildContext} is {@link MojoExecutionScoped} and its lifecycle is bound
 * to lifecycle of the corresponding mojo execution, that is, it is created right before the mojo
 * execution starts and discarded immediately after the mojo execution ends. Most Maven plugin
 * components, however, are singletons, which means they are created when plugin class realm is
 * created at the beginning of the build and discarded when plugin realm is discarded at the end of
 * the build. It is therefore not possible to bind MavenBuildContext to singleton components
 * directly.
 */
@Named(MavenBuildContext.HINT)
@Singleton
public class MavenBuildContext implements BuildContext {

  public static final String HINT = "singleton";

  @Named
  @MojoExecutionScoped
  public static class MojoExecutionScopedBuildContext extends DefaultBuildContext {

    @Inject
    public MojoExecutionScopedBuildContext(BuildContextConfiguration configuration) {
      super(configuration);
    }
  }

  private final Provider<MojoExecutionScopedBuildContext> provider;

  public MavenBuildContext(Provider<MojoExecutionScopedBuildContext> delegate) {
    this.provider = delegate;
  }

  @Override
  public ResourceMetadata<File> registerInput(File inputFile) {
    return provider.get().registerInput(inputFile);
  }

  @Override
  public Iterable<? extends ResourceMetadata<File>> registerInputs(File basedir,
      Collection<String> includes, Collection<String> excludes) throws IOException {
    return provider.get().registerInputs(basedir, includes, excludes);
  }

  @Override
  public Iterable<? extends Resource<File>> registerAndProcessInputs(File basedir,
      Collection<String> includes, Collection<String> excludes) throws IOException {
    return provider.get().registerAndProcessInputs(basedir, includes, excludes);
  }

  @Override
  public void markSkipExecution() {
    provider.get().markSkipExecution();
  }
}
