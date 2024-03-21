/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.benchmark.metering;

import co.elastic.elasticsearch.metering.ingested_size.MeteringConstantUnsupportedOperationParser;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentParserConfiguration;
import org.elasticsearch.xcontent.XContentType;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class MeteringParsingBenchmark {

    private String path = "/Users/przemyslawgomulka/scratch/rally/geonames";
    @Param({"JSON"/*, "YAML", "CBOR", "SMILE"*/})
    private String xContentTypeName;


    @Setup
    public void setup() throws IOException {
    }

    @Benchmark
    public AtomicLong noopParser(Blackhole blackhole) throws IOException {
        AtomicLong counter = new AtomicLong();
        parse((XContentParser p, AtomicLong l) -> p, blackhole);
        return counter;
    }

    @Benchmark
    public AtomicLong constantValuePerCharacter(Blackhole blackhole) throws IOException {
        AtomicLong counter = new AtomicLong();
        parse((XContentParser p, AtomicLong l) -> new MeteringLengthParser(p, l), blackhole);
        return counter;
    }

    @Benchmark
    public AtomicLong constantValuePerCharacterUOE(Blackhole blackhole) throws IOException {
        AtomicLong counter = new AtomicLong();
        parse((XContentParser p, AtomicLong l) -> new MeteringConstantUnsupportedOperationParser(p, l), blackhole);
        return counter;
    }

    @Benchmark
    public AtomicLong variableValuePerCharacter(Blackhole blackhole) throws IOException {
        return parse((XContentParser p, AtomicLong l) -> new MeteringLengthParser(p, l),blackhole);
    }

    private AtomicLong parse(BiFunction<XContentParser, AtomicLong, XContentParser> wrapper, Blackhole blackhole) {
        AtomicLong counter = new AtomicLong();
        try {
            XContentType xContentType = XContentType.valueOf(xContentTypeName);
            Files.lines(Path.of(path+"."+xContentTypeName)).forEach(line -> {
                BytesArray bytesArray = new BytesArray(line);
                try (XContentParser parser = XContentHelper.createParser(XContentParserConfiguration.EMPTY, bytesArray, xContentType)) {
                    XContentParser wrapped = wrapper.apply(parser, counter);
                    Map<String, Object> map = wrapped.map();
                    blackhole.consume(map);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return counter;
    }

}
