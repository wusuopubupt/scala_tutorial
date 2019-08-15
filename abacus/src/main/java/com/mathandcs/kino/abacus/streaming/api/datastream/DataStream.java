package com.mathandcs.kino.abacus.streaming.api.datastream;

import com.mathandcs.kino.abacus.streaming.api.environment.ExecutionEnvironment;
import com.mathandcs.kino.abacus.streaming.api.functions.FilterFunction;
import com.mathandcs.kino.abacus.streaming.api.functions.FlatMapFunction;
import com.mathandcs.kino.abacus.streaming.api.functions.MapFunction;
import com.mathandcs.kino.abacus.streaming.api.functions.sink.PrintSinkFunction;
import com.mathandcs.kino.abacus.streaming.api.operators.*;
import com.mathandcs.kino.abacus.streaming.runtime.utils.AbstractID;
import lombok.Getter;

/**
 * A DataStream represents a stream of elements of the same type.A DataStream
 *  * can be transformed into another DataStream by applying a transformation as
 *  * for example:
 *  * <ul>
 *  * <li>{@link DataStream#map}
 *  * </ul>
 *  *
 * @param <IN> The type of the elements in this stream
 */
public class DataStream<IN> implements Transformable {

    protected AbstractID id;
    protected ExecutionEnvironment env;
    protected DataStream input;
    protected Operator   operator;

    public DataStream(ExecutionEnvironment env, DataStream input, Operator operator) {
        this.id = new AbstractID();
        this.env = env;
        this.input = input;
        this.operator = operator;
    }

    public DataStream(DataStream<IN> input, Operator operator) {
        this.id = new AbstractID();
        this.env = input.env;
        this.input = input;
        this.operator = operator;
    }

    public <OUT> OneInputDataStream<IN, OUT> map(MapFunction<IN, OUT> mapper) {
        return new OneInputDataStream(this, new MapOperator(mapper));
    }

    public <OUT> OneInputDataStream<IN, OUT> filter(FilterFunction<IN> filter) {
        return new OneInputDataStream(this, new FilterOperator(filter));
    }

    public <OUT> OneInputDataStream<IN, OUT> flatMap(FlatMapFunction<IN, OUT> flatMapper) {
        return new OneInputDataStream(this, new FlatMapOperator(flatMapper));
    }

    public DataStreamSink<IN> print() {
        PrintSinkFunction<IN> printFunction = new PrintSinkFunction<>();
        SinkOperator sinkOperator = new SinkOperator(printFunction);
        DataStreamSink sink = new DataStreamSink(this, sinkOperator);
        return sink;
    }

    @Override
    public AbstractID getId() {
        return id;
    }

    @Override
    public Operator getOperator() {
        return operator;
    }

    @Override
    public Transformable getInput() {
        return input;
    }
}
