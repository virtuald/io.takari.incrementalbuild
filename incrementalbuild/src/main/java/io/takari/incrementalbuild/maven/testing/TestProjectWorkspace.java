package io.takari.incrementalbuild.maven.testing;

import io.takari.incrementalbuild.maven.internal.FilesystemWorkspace;
import io.takari.incrementalbuild.maven.internal.ProjectWorkspace;
import io.takari.incrementalbuild.workspace.Workspace;
import io.takari.incrementalbuild.workspace.Workspace2;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;

import org.apache.maven.project.MavenProject;

//this is explicitly bound in IncrementalBuildRuntime.addGuiceModules
class TestProjectWorkspace extends ProjectWorkspace implements Workspace2 {

  private IncrementalBuildLog log;

  private class ForwardingWorkspace2 implements Workspace2 {

    private final Workspace workspace;

    public ForwardingWorkspace2(Workspace workspace) {
      this.workspace = workspace;
    }

    @Override
    public Mode getMode() {
      return workspace.getMode();
    }

    @Override
    public Workspace escalate() {
      throw new UnsupportedOperationException(); // already escalated
    }

    @Override
    public boolean isPresent(File file) {
      return workspace.isPresent(file);
    }

    @Override
    public void deleteFile(File file) throws IOException {
      log.addDeletedOutput(file);

      workspace.deleteFile(file);
    }

    @Override
    public void processOutput(File file) {
      log.addRegisterOutput(file);

      workspace.processOutput(file);
    }

    @Override
    public OutputStream newOutputStream(File file) throws IOException {
      return workspace.newOutputStream(file);
    }

    @Override
    public ResourceStatus getResourceStatus(File file, long lastModified, long length) {
      return workspace.getResourceStatus(file, lastModified, length);
    }

    @Override
    public void walk(File basedir, FileVisitor visitor) throws IOException {
      workspace.walk(basedir, visitor);
    }

    @Override
    public void carryOverOutput(File file) {
      log.addCarryoverOutput(file);

      if (workspace instanceof Workspace2) {
        ((Workspace2) workspace).carryOverOutput(file);
      }
    }
  }


  @Inject
  public TestProjectWorkspace(MavenProject project, Workspace workspace,
      FilesystemWorkspace filesystem, IncrementalBuildLog log) {
    super(project, workspace, filesystem);
    this.log = log;
  }

  @Override
  public void processOutput(File file) {
    log.addRegisterOutput(file);

    super.processOutput(file);
  }


  @Override
  public void deleteFile(File file) throws IOException {
    log.addDeletedOutput(file);

    super.deleteFile(file);
  }

  @Override
  public void carryOverOutput(File file) {
    log.addCarryoverOutput(file);
  }

  @Override
  public Workspace escalate() {
    return new ForwardingWorkspace2(super.escalate());
  }

  @Override
  protected Workspace getWorkspace(File file) {
    return new ForwardingWorkspace2(super.getWorkspace(file));
  }
}
