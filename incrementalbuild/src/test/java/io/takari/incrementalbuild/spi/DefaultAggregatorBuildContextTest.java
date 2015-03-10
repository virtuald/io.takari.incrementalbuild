package io.takari.incrementalbuild.spi;

import io.takari.incrementalbuild.MessageSeverity;
import io.takari.incrementalbuild.Output;
import io.takari.incrementalbuild.Resource;
import io.takari.incrementalbuild.aggregator.AggregateCreator;
import io.takari.incrementalbuild.aggregator.AggregateInput;
import io.takari.incrementalbuild.aggregator.InputProcessor;
import io.takari.incrementalbuild.aggregator.internal.DefaultAggregateMetadata;
import io.takari.incrementalbuild.aggregator.internal.DefaultAggregatorBuildContext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;


public class DefaultAggregatorBuildContextTest extends AbstractBuildContextTest {

  private class FileIndexer implements AggregateCreator {
    public final Map<File, File> inputs = new LinkedHashMap<>();
    public final List<File> outputs = new ArrayList<>();

    @Override
    public void create(Output<File> output, Iterable<AggregateInput> inputs) throws IOException {
      outputs.add(output.getResource());
      try (BufferedWriter w =
          new BufferedWriter(new OutputStreamWriter(output.newOutputStream(), "UTF-8"))) {
        for (AggregateInput input : inputs) {
          this.inputs.put(input.getResource(), input.getBasedir());
          w.write(input.getResource().getAbsolutePath());
          w.newLine();
        }
      }
    }
  };

  @Test
  public void testBasic() throws Exception {
    File outputFile = new File(temp.getRoot(), "output");

    File basedir = temp.newFolder();
    File a = new File(basedir, "a").getCanonicalFile();
    a.createNewFile();

    // initial build
    FileIndexer indexer = new FileIndexer();
    DefaultAggregatorBuildContext actx = newContext();
    DefaultAggregateMetadata output = actx.registerOutput(outputFile);
    output.addInputs(basedir, null, null);
    output.createIfNecessary(indexer);
    actx.commit(null);
    Assert.assertTrue(outputFile.canRead());
    Assert.assertEquals(1, indexer.outputs.size());
    Assert.assertEquals(1, indexer.inputs.size());
    Assert.assertEquals(basedir.getCanonicalFile(), indexer.inputs.get(a));

    // no-change rebuild
    indexer = new FileIndexer();
    actx = newContext();
    output = actx.registerOutput(outputFile);
    output.addInputs(basedir, null, null);
    output.createIfNecessary(indexer);
    actx.commit(null);
    Assert.assertTrue(outputFile.canRead());
    Assert.assertEquals(0, indexer.outputs.size());

    // no-change rebuild
    indexer = new FileIndexer();
    actx = newContext();

    output = actx.registerOutput(outputFile);
    output.addInputs(basedir, null, null);
    output.createIfNecessary(indexer);
    actx.commit(null);
    Assert.assertTrue(outputFile.canRead());
    Assert.assertEquals(0, indexer.outputs.size());

    // new input
    File b = new File(basedir, "b").getCanonicalFile();
    b.createNewFile();
    indexer = new FileIndexer();
    actx = newContext();
    output = actx.registerOutput(outputFile);
    output.addInputs(basedir, null, null);
    output.createIfNecessary(indexer);
    actx.commit(null);
    Assert.assertTrue(outputFile.canRead());
    Assert.assertEquals(1, indexer.outputs.size());

    // removed output
    a.delete();
    indexer = new FileIndexer();
    actx = newContext();
    output = actx.registerOutput(outputFile);
    output.addInputs(basedir, null, null);
    output.createIfNecessary(indexer);
    actx.commit(null);
    Assert.assertTrue(outputFile.canRead());
    Assert.assertEquals(1, indexer.outputs.size());

    // no-change rebuild
    indexer = new FileIndexer();
    actx = newContext();
    output = actx.registerOutput(outputFile);
    output.addInputs(basedir, null, null);
    output.createIfNecessary(indexer);
    actx.commit(null);
    Assert.assertTrue(outputFile.canRead());
    Assert.assertEquals(0, indexer.outputs.size());
  }

  private DefaultAggregatorBuildContext newContext() {
    File stateFile = new File(temp.getRoot(), "buildstate.ctx");
    return new DefaultAggregatorBuildContext(new FilesystemWorkspace(), stateFile,
        new HashMap<String, Serializable>(), null);
  }

  @Test
  public void testEmpty() throws Exception {
    File outputFile = new File(temp.getRoot(), "output");
    File basedir = temp.newFolder();

    FileIndexer indexer = new FileIndexer();
    DefaultAggregatorBuildContext actx = newContext();
    DefaultAggregateMetadata output = actx.registerOutput(outputFile);
    output.addInputs(basedir, null, null);
    output.createIfNecessary(indexer);
    actx.commit(null);
    Assert.assertTrue(outputFile.canRead());
    Assert.assertEquals(1, indexer.outputs.size());
  }

  @Test
  public void testInputStateCarryOver() throws Exception {
    File outputFile = new File(temp.getRoot(), "output");

    File basedir = temp.newFolder();
    File a = new File(basedir, "a").getCanonicalFile();
    a.createNewFile();

    // initial build
    DefaultAggregatorBuildContext actx = newContext();
    DefaultAggregateMetadata output = actx.registerOutput(outputFile);
    output.addInputs(basedir, null, null, new InputProcessor() {
      @Override
      public Map<String, Serializable> process(Resource<File> input) throws IOException {
        input.setAttribute("key", "value");
        input.addMessage(0, 0, "message", MessageSeverity.INFO, null);
        return null;
      }
    });
    output.createIfNecessary(new FileIndexer());
    actx.commit(null);

    // new input
    File b = new File(basedir, "b").getCanonicalFile();
    b.createNewFile();
    actx = newContext();
    output = actx.registerOutput(outputFile);
    output.addInputs(basedir, null, null, new InputProcessor() {
      @Override
      public Map<String, Serializable> process(Resource<File> input) throws IOException {
        input.setAttribute("key", "value");
        input.addMessage(0, 0, "message", MessageSeverity.INFO, null);
        return null;
      }
    });
    output.createIfNecessary(new AggregateCreator() {
      @Override
      public void create(Output<File> output, Iterable<AggregateInput> inputs) throws IOException {
        for (AggregateInput input : inputs) {
          Assert.assertEquals("value", input.getAttribute("key", String.class));
        }
      }
    });
    actx.commit(null);
  }


}
