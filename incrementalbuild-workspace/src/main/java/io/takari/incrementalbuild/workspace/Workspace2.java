package io.takari.incrementalbuild.workspace;

import java.io.File;

public interface Workspace2 extends Workspace {

  public void carryOverOutput(File file);

}
