package com.thinkaurelius.faunus.mapreduce.filter;

import com.thinkaurelius.faunus.FaunusEdge;
import com.thinkaurelius.faunus.FaunusVertex;
import com.thinkaurelius.faunus.Tokens;
import com.thinkaurelius.faunus.util.MicroElement;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class DuplicateFilterMap {

    public static final String CLASS = Tokens.makeNamespace(DuplicateFilterMap.class) + ".class";

    public enum Counters {
        VERTICES_DEDUPED,
        EDGES_DEDUPED
    }

    public static class Map extends Mapper<NullWritable, FaunusVertex, NullWritable, FaunusVertex> {

        private boolean isVertex;

        @Override
        public void setup(final Mapper.Context context) throws IOException, InterruptedException {
            this.isVertex = context.getConfiguration().getClass(CLASS, Element.class, Element.class).equals(Vertex.class);
        }

        @Override
        public void map(final NullWritable key, final FaunusVertex value, final Mapper<NullWritable, FaunusVertex, NullWritable, FaunusVertex>.Context context) throws IOException, InterruptedException {

            if (this.isVertex) {
                if (value.hasPaths()) {
                    final List<MicroElement> path = value.getPaths().get(0);
                    value.clearPaths();
                    value.addPath(path, false);
                    context.getCounter(Counters.VERTICES_DEDUPED).increment(1l);
                }
            } else {
                long counter = 0;
                for (final Edge e : value.getEdges(Direction.BOTH)) {
                    final FaunusEdge edge = (FaunusEdge) e;
                    if (edge.hasPaths()) {
                        final List<MicroElement> path = edge.getPaths().get(0);
                        edge.clearPaths();
                        edge.addPath(path, false);
                        counter++;
                    }
                }
                context.getCounter(Counters.EDGES_DEDUPED).increment(counter);
            }

            context.write(NullWritable.get(), value);
        }
    }
}
