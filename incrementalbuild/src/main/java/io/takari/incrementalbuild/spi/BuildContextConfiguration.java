package io.takari.incrementalbuild.spi;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

public interface BuildContextConfiguration {

  File getStateFile();

  Map<String, Serializable> getConfiguration();

}
