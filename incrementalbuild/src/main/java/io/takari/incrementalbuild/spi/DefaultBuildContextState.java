package io.takari.incrementalbuild.spi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultBuildContextState implements Serializable {

  private static final transient Logger log = LoggerFactory
      .getLogger(DefaultBuildContextState.class);

  private static final long serialVersionUID = 6195150574931820441L;

  final Map<String, Serializable> configuration;

  final Set<File> outputs;

  final Map<Object, ResourceHolder<?>> resources;

  final Map<Object, Collection<File>> resourceOutputs;

  private final Map<File, Collection<Object>> outputInputs;

  final Map<Object, Map<String, Serializable>> resourceAttributes;

  final Map<Object, Collection<Message>> resourceMessages;

  private DefaultBuildContextState(Map<String, Serializable> configuration //
      , Map<Object, ResourceHolder<?>> inputs //
      , Set<File> outputs //
      , Map<Object, Collection<File>> resourceOutputs //
      , Map<File, Collection<Object>> outputInputs //
      , Map<Object, Map<String, Serializable>> resourceAttributes //
      , Map<Object, Collection<Message>> resourceMessages) {
    this.configuration = configuration;
    this.resources = inputs;
    this.outputs = outputs;
    this.resourceOutputs = resourceOutputs;
    this.outputInputs = outputInputs;
    this.resourceAttributes = resourceAttributes;
    this.resourceMessages = resourceMessages;
  }

  public static DefaultBuildContextState withConfiguration(Map<String, Serializable> configuration) {
    HashMap<String, Serializable> copy = new HashMap<String, Serializable>(configuration);
    // configuration marker used to distinguish between empty and new state
    copy.put("incremental", Boolean.TRUE);
    return new DefaultBuildContextState(Collections.<String, Serializable>unmodifiableMap(copy) // configuration
        , new HashMap<Object, ResourceHolder<?>>() // inputs
        , new HashSet<File>() // outputs
        , new HashMap<Object, Collection<File>>() // inputOutputs
        , new HashMap<File, Collection<Object>>() // outputInputs
        , new HashMap<Object, Map<String, Serializable>>() // resourceAttributes
        , new HashMap<Object, Collection<Message>>() // messages
    );
  }

  public static DefaultBuildContextState emptyState() {
    return new DefaultBuildContextState(Collections.<String, Serializable>emptyMap() // configuration
        , Collections.<Object, ResourceHolder<?>>emptyMap() // inputs //
        , Collections.<File>emptySet() // outputs //
        , Collections.<Object, Collection<File>>emptyMap() // inputOutputs //
        , Collections.<File, Collection<Object>>emptyMap() // outputInputs //
        , Collections.<Object, Map<String, Serializable>>emptyMap() // resourceAttributes //
        , Collections.<Object, Collection<Message>>emptyMap() // messages
    );
  }

  public String getStats() {
    StringBuilder sb = new StringBuilder();

    sb.append(configuration.size()).append(' ');
    sb.append(resources.size()).append(' ');
    sb.append(outputs.size()).append(' ');
    sb.append(resourceOutputs.size()).append(' ');
    sb.append(outputInputs.size()).append(' ');
    sb.append(resourceAttributes.size()).append(' ');
    sb.append(resourceMessages.size()).append(' ');

    return sb.toString();
  }

  public void storeTo(OutputStream os) throws IOException {
    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os));
    try {
      writeMap(oos, this.configuration);
      writeCollection(oos, this.outputs);
      writeMap(oos, this.resources);

      writeMultimap(oos, resourceOutputs);
      writeDoublemap(oos, resourceAttributes);
      writeMultimap(oos, resourceMessages);

    } finally {
      oos.flush();
    }
  }

  private static void writeMap(ObjectOutputStream oos, Map<?, ?> map) throws IOException {
    oos.writeInt(map.size());
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      oos.writeObject(entry.getKey());
      oos.writeObject(entry.getValue());
    }
  }

  private static void writeMultimap(ObjectOutputStream oos, Map<?, ? extends Collection<?>> mmap)
      throws IOException {
    oos.writeInt(mmap.size());
    for (Map.Entry<?, ? extends Collection<?>> entry : mmap.entrySet()) {
      oos.writeObject(entry.getKey());
      writeCollection(oos, entry.getValue());
    }
  }

  private static void writeCollection(ObjectOutputStream oos, Collection<?> collection)
      throws IOException {
    if (collection == null || collection.isEmpty()) {
      oos.writeInt(0);
    } else {
      oos.writeInt(collection.size());
      for (Object element : collection) {
        oos.writeObject(element);
      }
    }
  }

  private static void writeDoublemap(ObjectOutputStream oos, Map<?, ? extends Map<?, ?>> dmap)
      throws IOException {
    oos.writeInt(dmap.size());
    for (Map.Entry<?, ? extends Map<?, ?>> entry : dmap.entrySet()) {
      oos.writeObject(entry.getKey());
      writeMap(oos, entry.getValue());
    }
  }

  public static DefaultBuildContextState loadFrom(File stateFile) {
    // TODO verify stateFile location has not changed since last build
    // TODO wrap collections in corresponding immutable collections

    if (stateFile == null) {
      // transient build context
      return DefaultBuildContextState.emptyState();
    }

    try {
      ObjectInputStream is =
          new ObjectInputStream(new BufferedInputStream(new FileInputStream(stateFile))) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException,
                ClassNotFoundException {
              // TODO does it matter if TCCL or super is called first?
              try {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                Class<?> clazz = tccl.loadClass(desc.getName());
                return clazz;
              } catch (ClassNotFoundException e) {
                return super.resolveClass(desc);
              }
            }
          };
      try {
        final long start = System.currentTimeMillis();

        Map<String, Serializable> configuration = readMap(is);
        Set<File> outputs = new HashSet<>(DefaultBuildContextState.<File>readCollection(is));
        Map<Object, ResourceHolder<?>> resources = readMap(is);

        Map<Object, Collection<File>> resourceOutputs = readMultimap(is);
        Map<File, Collection<Object>> outputInputs = invertMultimap(resourceOutputs);
        Map<Object, Map<String, Serializable>> resourceAttributes = readDoublemap(is);
        Map<Object, Collection<Message>> messages = readMultimap(is);

        DefaultBuildContextState state = new DefaultBuildContextState(configuration //
            , resources //
            , outputs //
            , resourceOutputs //
            , outputInputs //
            , resourceAttributes //
            , messages //
            );
        log.debug("Loaded incremental build state {} ({} ms)", stateFile,
            System.currentTimeMillis() - start);
        return state;
      } finally {
        try {
          is.close();
        } catch (IOException e) {
          // ignore secondary exceptions
        }
      }
    } catch (FileNotFoundException e) {
      // this is expected, silently ignore
    } catch (RuntimeException e) {
      // this is a bug in our code, let it bubble up as build failure
      throw e;
    } catch (Exception e) {
      // this is almost certainly caused by incompatible state file, log and continue
      log.debug("Could not load incremental build state {}", stateFile, e);
    }
    return DefaultBuildContextState.emptyState();
  }

  @SuppressWarnings("unchecked")
  private static <K, V> Map<K, V> readMap(ObjectInputStream ois) throws IOException,
      ClassNotFoundException {
    Map<K, V> map = new HashMap<K, V>();
    int size = ois.readInt();
    for (int i = 0; i < size; i++) {
      K key = (K) ois.readObject();
      V value = (V) ois.readObject();
      map.put(key, value);
    }
    return Collections.unmodifiableMap(map);
  }

  @SuppressWarnings("unchecked")
  private static <K, V> Map<K, Collection<V>> readMultimap(ObjectInputStream ois)
      throws IOException, ClassNotFoundException {
    Map<K, Collection<V>> mmap = new HashMap<K, Collection<V>>();
    int size = ois.readInt();
    for (int i = 0; i < size; i++) {
      K key = (K) ois.readObject();
      Collection<V> value = readCollection(ois);
      mmap.put(key, value);
    }
    return Collections.unmodifiableMap(mmap);
  }

  @SuppressWarnings("unchecked")
  private static <V> Collection<V> readCollection(ObjectInputStream ois) throws IOException,
      ClassNotFoundException {
    int size = ois.readInt();
    if (size == 0) {
      return null;
    }
    Collection<V> collection = new ArrayList<V>();
    for (int i = 0; i < size; i++) {
      collection.add((V) ois.readObject());
    }
    return Collections.unmodifiableCollection(collection);
  }

  @SuppressWarnings("unchecked")
  private static <K, VK, VV> Map<K, Map<VK, VV>> readDoublemap(ObjectInputStream ois)
      throws IOException, ClassNotFoundException {
    int size = ois.readInt();
    Map<K, Map<VK, VV>> dmap = new HashMap<K, Map<VK, VV>>();
    for (int i = 0; i < size; i++) {
      K key = (K) ois.readObject();
      Map<VK, VV> value = readMap(ois);
      dmap.put(key, value);
    }
    return Collections.unmodifiableMap(dmap);
  }

  private static <K, V> Map<V, Collection<K>> invertMultimap(Map<K, Collection<V>> mmap) {
    Map<V, Collection<K>> inverted = new HashMap<V, Collection<K>>();
    for (Map.Entry<K, Collection<V>> entry : mmap.entrySet()) {
      for (V value : entry.getValue()) {
        Collection<K> keys = inverted.get(value);
        if (keys == null) {
          keys = new ArrayList<K>();
          inverted.put(value, keys);
        }
        keys.add(entry.getKey());
      }
    }
    return Collections.unmodifiableMap(inverted);
  }

  // getters and settings

  public Collection<Object> getOutputInputs(File outputFile) {
    return outputInputs.get(outputFile);
  }

  public boolean putOutputInput(File outputFile, Object input) {
    return MMaps.put(outputInputs, outputFile, input);
  }

  public Collection<File> getOutputs() {
    return Collections.unmodifiableCollection(outputs);
  }

  public boolean isOutput(File outputFile) {
    return outputs.contains(outputFile);
  }
}
