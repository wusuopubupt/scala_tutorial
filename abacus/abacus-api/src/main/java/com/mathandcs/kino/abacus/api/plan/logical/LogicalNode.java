package com.mathandcs.kino.abacus.api.plan.logical;

import akka.actor.ActorRef;
import com.mathandcs.kino.abacus.api.datastream.DataStreamId;
import com.mathandcs.kino.abacus.api.operators.Operator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LogicalNode implements Serializable {

    private final DataStreamId id;
    private final Operator operator;
    private final int parallelism;

    private List<LogicalEdge> inputEdges = new ArrayList<>();
    private List<LogicalEdge> outputEdges = new ArrayList<>();

    private ActorRef actor;

    public LogicalNode(DataStreamId id,
                       Operator operator,
                       int parallelism) {

        this.id = id;
        this.operator = operator;
        this.parallelism = parallelism;
    }

    public DataStreamId getId() {
        return id;
    }

    public void addInputEdge(LogicalEdge inputEdge) {
        if (inputEdges.contains(inputEdge)) {
            return;
        }
        inputEdges.add(inputEdge);
    }

    public void addOutputEdge(LogicalEdge outputEdge) {
        if (outputEdges.contains(outputEdge)) {
            return;
        }
        outputEdges.add(outputEdge);
    }

    public List<LogicalEdge> getInputEdges() {
        return inputEdges;
    }

    public List<LogicalEdge> getOutputEdges() {
        return outputEdges;
    }

    public Operator getOperator() {
        return operator;
    }

    public int getParallelism() {
        return parallelism;
    }

    public boolean isSource() {
        return inputEdges.isEmpty();
    }

    public boolean isSink() {
        return outputEdges.isEmpty();
    }

    public ActorRef getActor() {
        return actor;
    }

    public void setActor(ActorRef actor) {
        this.actor = actor;
    }
}
