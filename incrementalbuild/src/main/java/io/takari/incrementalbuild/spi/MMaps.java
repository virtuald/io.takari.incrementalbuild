package io.takari.incrementalbuild.spi;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

public class MMaps {
  public static <K, V> boolean put(Map<K, Collection<V>> multimap, K key, V value) {
    Collection<V> values = multimap.get(key);
    if (values == null) {
      values = new LinkedHashSet<V>();
      multimap.put(key, values);
    }
    return values.add(value);
  }

  public static <K, V> void putAll(Map<K, Collection<V>> multimap, K key, Collection<V> value) {
    Collection<V> values = multimap.get(key);
    if (values == null) {
      values = new LinkedHashSet<V>();
      multimap.put(key, values);
    }
    values.addAll(value);
  }

}
