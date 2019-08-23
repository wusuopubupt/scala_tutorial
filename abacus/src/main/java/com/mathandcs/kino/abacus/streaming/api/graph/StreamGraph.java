package com.mathandcs.kino.abacus.streaming.api.graph;

import com.mathandcs.kino.abacus.streaming.api.common.ExecutionConfig;
import com.mathandcs.kino.abacus.streaming.api.common.JobID;
import com.mathandcs.kino.abacus.streaming.api.graph.tasks.AbstractInvokable;
import com.mathandcs.kino.abacus.streaming.api.graph.tasks.OneInputStreamTask;
import com.mathandcs.kino.abacus.streaming.api.graph.tasks.SourceStreamTask;
import com.mathandcs.kino.abacus.streaming.api.operators.Operator;
import com.mathandcs.kino.abacus.streaming.api.operators.SourceOperator;
import com.mathandcs.kino.abacus.streaming.api.optimizer.StreamPlan;
import com.mathandcs.kino.abacus.streaming.runtime.io.partition.ForwardPartitioner;
import com.mathandcs.kino.abacus.streaming.runtime.io.partition.RebalancePartitioner;
import com.mathandcs.kino.abacus.streaming.runtime.io.partition.StreamPartitioner;
import com.mathandcs.kino.abacus.streaming.runtime.jobgraph.JobGraph;
import com.mathandcs.kino.abacus.streaming.runtime.utils.AbstractID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Class representing the streaming topology. It contains all the information
 * necessary to build the jobgraph for the execution.
 */
public class StreamGraph extends StreamPlan {

    private static final Logger LOG = LoggerFactory.getLogger(StreamGraph.class);

    private final ExecutionConfig executionConfig;

    // NodeID -> Node
    private Map<AbstractID, StreamNode> streamNodes;
    private Set<AbstractID> sources;
    private Set<AbstractID> sinks;

    public StreamGraph(ExecutionConfig executionConfig) {
        this.executionConfig = checkNotNull(executionConfig);
        clear();
    }

    public void clear() {
        streamNodes = new HashMap();
        sources = new HashSet();
        sinks = new HashSet();
    }

    public <OUT> void addSource(AbstractID vertexID,
                                Operator<OUT> operator) {
        addOperator(vertexID, operator);
        sources.add(vertexID);
    }

    public <OUT> void addSink(AbstractID vertexID,
                              Operator<OUT> operator) {
        addOperator(vertexID, operator);
        sinks.add(vertexID);
    }

    public <IN, OUT> void addOperator(
            AbstractID vertexID,
            Operator<OUT> operator) {

        if (operator instanceof SourceOperator) {
            addNode(vertexID, SourceStreamTask.class, operator);
        } else {
            addNode(vertexID, OneInputStreamTask.class, operator);
        }
    }

    protected StreamNode addNode(AbstractID vertexID,
                                 Class<? extends AbstractInvokable> vertexClass,
                                 Operator<?> operator) {

        if (streamNodes.containsKey(vertexID)) {
            throw new RuntimeException("Duplicate vertexID " + vertexID);
        }

        StreamNode vertex = new StreamNode(
                vertexID,
                operator,
                vertexClass);

        streamNodes.put(vertexID, vertex);

        return vertex;
    }

    // TODO: add partitioner
    protected void addEdge(AbstractID sourceId, AbstractID targetId, StreamPartitioner partitioner) {
        StreamNode sourceNode = getStreamNode(sourceId);
        StreamNode targetNode = getStreamNode(targetId);

        // If no partitioner was specified and the parallelism of upstream and downstream
        // operator matches use forward partitioning, use rebalance otherwise.
        if (partitioner == null && sourceNode.getParallelism() == targetNode.getParallelism()) {
            partitioner = new ForwardPartitioner<Object>();
        } else if (partitioner == null) {
            partitioner = new RebalancePartitioner<Object>();
        }

        if (partitioner instanceof ForwardPartitioner) {
            if (sourceNode.getParallelism() != targetNode.getParallelism()) {
                throw new UnsupportedOperationException("Forward partitioning does not allow " +
                        "change of parallelism. Upstream operation: " + sourceNode + " parallelism: " + sourceNode.getParallelism() +
                        ", downstream operation: " + targetNode + " parallelism: " + targetNode.getParallelism() +
                        " You must use another partitioning strategy, such as broadcast, rebalance, shuffle or global.");
            }
        }

        StreamEdge edge = new StreamEdge(sourceNode, targetNode, partitioner);

        getStreamNode(edge.getSourceId()).addOutEdge(edge);
        getStreamNode(edge.getTargetId()).addInEdge(edge);
    }

    private StreamNode getStreamNode(AbstractID id) {
        return streamNodes.get(id);
    }

    @Override
    public JobGraph getJobGraph(@Nullable JobID jobID) {
        // TODO
        //return StreamingJobGraphGenerator.createJobGraph(this, jobID);
        return null;
    }

    @Override
    public String toJson() {
        try {
            // TODO
            //return new JSONGenerator(this).getJSON();
            return this.toString();
        } catch (Exception e) {
            throw new RuntimeException("JSON plan creation failed", e);
        }
    }

}
