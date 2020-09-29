/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.benchmark;

import org.elasticsearch.common.xcontent.MediaType;
import org.elasticsearch.common.xcontent.MediaTypeParserRegex;
import org.elasticsearch.common.xcontent.MediaTypeParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.elasticsearch.common.xcontent.XContentType.CBOR;
import static org.elasticsearch.common.xcontent.XContentType.COMPATIBLE_WITH_PARAMETER_NAME;
import static org.elasticsearch.common.xcontent.XContentType.JSON;
import static org.elasticsearch.common.xcontent.XContentType.SMILE;
import static org.elasticsearch.common.xcontent.XContentType.VERSION_PATTERN;
import static org.elasticsearch.common.xcontent.XContentType.YAML;


@Fork(3)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@SuppressWarnings("unused") //invoked by benchmarking framework
public class MediaTypeParsingBenchmark {

    MediaTypeParser<XContentType> parser = XContentType.mediaTypeParser;


   MediaTypeParserRegex<XContentType> parser2 = new MediaTypeParserRegex.Builder<XContentType>()
        .withMediaTypeAndParams("application/smile",SMILE, Collections.emptyMap())
        .withMediaTypeAndParams("application/cbor", CBOR, Collections.emptyMap())
        .withMediaTypeAndParams("application/json",JSON, Map.of("charset", Pattern.compile("UTF-8")))
        .withMediaTypeAndParams("application/yaml", YAML, Map.of("charset", Pattern.compile("UTF-8")))
        .withMediaTypeAndParams("application/*", JSON, Map.of("charset", Pattern.compile("UTF-8")))
        .withMediaTypeAndParams("application/x-ndjson", JSON, Map.of("charset", Pattern.compile("UTF-8")))
        .withMediaTypeAndParams("application/vnd.elasticsearch+json", JSON,
                                Map.of(COMPATIBLE_WITH_PARAMETER_NAME, VERSION_PATTERN,"charset", Pattern.compile("UTF-8")))
        .withMediaTypeAndParams("application/vnd.elasticsearch+smile", SMILE,
                                Map.of(COMPATIBLE_WITH_PARAMETER_NAME, VERSION_PATTERN,"charset", Pattern.compile("UTF-8")))
        .withMediaTypeAndParams("application/vnd.elasticsearch+yaml", YAML,
                                Map.of(COMPATIBLE_WITH_PARAMETER_NAME, VERSION_PATTERN,"charset", Pattern.compile("UTF-8")))
        .withMediaTypeAndParams("application/vnd.elasticsearch+cbor", CBOR,
                                Map.of(COMPATIBLE_WITH_PARAMETER_NAME, VERSION_PATTERN,"charset", Pattern.compile("UTF-8")))
        .withMediaTypeAndParams("application/vnd.elasticsearch+x-ndjson", JSON,
                                Map.of(COMPATIBLE_WITH_PARAMETER_NAME, VERSION_PATTERN,"charset", Pattern.compile("UTF-8")))
        .build();
    @Benchmark
    @Threads(1)
    public MediaType parseIter() {
        return parser.fromMediaType("application/vnd.elasticsearch+json;compatible-with=7 ; charset=UTF-8");
    }

    @Benchmark
    @Threads(1)
    public MediaType parseRegex() {
        return parser2.fromMediaType("application/vnd.elasticsearch+json;compatible-with=7 ; charset=UTF-8");
    }
}
