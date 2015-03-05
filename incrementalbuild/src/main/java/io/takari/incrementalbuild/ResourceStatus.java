package io.takari.incrementalbuild;

public enum ResourceStatus {

  /**
   * Input is new in this build, i.e. it was not present in the previous build.
   */
  NEW,

  /**
   * Input itself changed or any of its included inputs changed or was removed since last build.
   */
  MODIFIED,

  /**
   * Input itself and all includes inputs, if any, did not change since last build.
   */
  UNMODIFIED,

  /**
   * Input was removed since last build.
   */
  REMOVED;
}