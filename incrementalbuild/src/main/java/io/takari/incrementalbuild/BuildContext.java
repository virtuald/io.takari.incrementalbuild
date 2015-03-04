package io.takari.incrementalbuild;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;



/**
 * 
 * XXX decide if implementations of this interface should be thread-safe or not.
 * 
 * @author igor
 */
public interface BuildContext {

  public static enum Severity {
    ERROR, WARNING, INFO
  }

  public static enum ResourceStatus {

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

  public static interface ResourceMetadata<T> {

    public T getResource();

    public ResourceStatus getStatus();

    /**
     * Returns current attribute value.
     * <p>
     * For registered (but not processed) resources and carried over resources, returns value
     * associated with the key during previous build. For processed resources, returns value
     * associated with the key during this build.
     */
    public <V extends Serializable> V getAttribute(String key, Class<V> clazz);

    /**
     * Returns outputs associated with this resource.
     * <p>
     * For registered (but not processed) resources and carried over resources returns outputs
     * associated with the resource during previous build. For processed resources, returns outputs
     * associated with the resource during this build.
     */
    public Iterable<? extends ResourceMetadata<File>> getAssociatedOutputs();

    public Resource<T> process();

  }

  public static interface Resource<T> extends ResourceMetadata<T> {

    /**
     * Returns attribute value associated with the key during previous build.
     */
    public <V extends Serializable> Serializable setAttribute(String key, V value);

    public void addMessage(int line, int column, String message, Severity severity, Throwable cause);

    public Output<File> associateOutput(Output<File> output);

    public Output<File> associateOutput(File outputFile);
  }

  public static interface Output<T> extends Resource<T> {

    public OutputStream newOutputStream() throws IOException;

  }

  /**
   * Registers specified input {@code File} with this build context.
   * <p>
   * <strong>WARNING</strong> this method is not fully compatible with m2e build workspace. It
   * should only be used for static project resources, like Maven pom.xml. {@link #registerInputs}
   * (or {@link #registerAndProcessInputs}) should be used in all other cases.
   * 
   * @TODO this method behaves differently before and after input was processed. Once input is
   *       processed, the returned InputMetadata represent input's new state, which maybe confusing.
   *       There is also no way to access input's old state.
   * 
   * @return {@link InputMetadata} representing the input file, never {@code null}.
   * @throws IllegalArgumentException if inputFile is not a file or cannot be read
   */
  // public InputMetadata<File> registerInput(File inputFile);

  /**
   * Registers inputs identified by {@code basedir} and {@code includes}/{@code excludes} ant
   * patterns.
   * <p>
   * When a file is found under {@code basedir}, it will be registered if it does not match
   * {@code excludes} patterns and matches {@code includes} patterns. {@code null} or empty includes
   * parameter will match all files. {@code excludes} match takes precedence over {@code includes},
   * if a file matches one of excludes patterns it will not be registered regardless of includes
   * patterns match.
   * <p>
   * Implementation is not expected to handle changes {@code basedir}, {@code includes} or
   * {@code excludes} incrementally.
   * 
   * @param basedir is the base directory to look for inputs, must not be {@code null}
   * @param includes patterns of the files to register, can be {@code null}
   * @param excludes patterns of the files to ignore, can be {@code null}
   * @see http://ant.apache.org/manual/dirtasks.html#patterns
   */
  public Iterable<? extends ResourceMetadata<File>> registerInputs(File basedir,
      Collection<String> includes, Collection<String> excludes) throws IOException;

  /**
   * Registers inputs identified by {@code basedir} and {@code includes}/{@code excludes} ant
   * patterns. Processes inputs that are new or modified since previous build.
   * 
   * @returns processed inputs
   */
  public Iterable<? extends Resource<File>> registerAndProcessInputs(File basedir,
      Collection<String> includes, Collection<String> excludes) throws IOException;

  public Output<File> processOutput(File outputFile);

  /**
   * Returns all inputs registered with this {@link BuildContext} during current and previous
   * builds.
   */
  // public Iterable<? extends InputMetadata<File>> getRegisteredInputs();

  /**
   * Returns all outputs processed by this {@link BuildContext} during current build or carried over
   * from previous build.
   */
  // public Iterable<? extends OutputMetadata<File>> getProcessedOutputs();
}
