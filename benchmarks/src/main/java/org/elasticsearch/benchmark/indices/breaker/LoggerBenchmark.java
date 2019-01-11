/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.benchmark.indices.breaker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.elasticsearch.Version;
import org.elasticsearch.cli.UserException;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.logging.LogConfigurator;
import org.elasticsearch.common.logging.NodeAndClusterIdConverter;
import org.elasticsearch.common.logging.NodeAndClusterIdConverter2;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.env.Environment;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Fork(3)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@SuppressWarnings("unused") //invoked by benchmarking framework
public class LoggerBenchmark {



    NodeAndClusterIdConverter nodeAndClusterIdConverter1 = new NodeAndClusterIdConverter();
    NodeAndClusterIdConverter2 nodeAndClusterIdConverter2 = new NodeAndClusterIdConverter2();

    @Setup(Level.Trial)
    public void init() throws IOException, UserException {
        LogConfigurator.setNodeName("sample-name");
        setupLogging("log4j2.properties");
    }

    @Setup(Level.Iteration)
    public void perTest() {
        nodeAndClusterIdConverter1.clusterChanged(event());
        nodeAndClusterIdConverter1.format(new Log4jLogEvent(), new StringBuilder());
        nodeAndClusterIdConverter2.clusterChanged(event());
        nodeAndClusterIdConverter2.format(new Log4jLogEvent(), new StringBuilder());
    }

    @Benchmark
    @Threads(1)
    public StringBuilder readThreadLocal() {
        StringBuilder stringBuilder = new StringBuilder();
        nodeAndClusterIdConverter1.format(new Log4jLogEvent(), stringBuilder);
        return stringBuilder;
    }

    @Benchmark
    @Threads(1)
    public StringBuilder readAtomicRef() {
        StringBuilder stringBuilder = new StringBuilder();
        nodeAndClusterIdConverter2.format(new Log4jLogEvent(), stringBuilder);
        return stringBuilder;

    }

    private ClusterChangedEvent event() {
        ClusterState clusterState = ClusterState.builder(new ClusterName("name"))
                                                .metaData(MetaData.builder()
                                                                  .clusterUUID("sampleClusterId")
                                                                  .build())
                                                .nodes(DiscoveryNodes.builder()
                                                                     .localNodeId("localNodeId")
                                                                     .add(new DiscoveryNode("localNodeId", buildNewFakeTransportAddress(), Collections.emptyMap(), Collections.emptySet(),
                                                                         Version.CURRENT))
                                                                     .build()
                                                )
                                                .build();
        return new ClusterChangedEvent("EventSource", clusterState, clusterState);
    }

    private void setupLogging(final String config) throws IOException, UserException {
        setupLogging(config, Settings.EMPTY);
    }

    public static TransportAddress buildNewFakeTransportAddress() {
        return new TransportAddress(TransportAddress.META_ADDRESS, 1);
    }

    private void setupLogging(final String config, final Settings settings) throws IOException, UserException {
        assert !Environment.PATH_HOME_SETTING.exists(settings);
        final Path configDir = getDataPath(config);
        final Settings mergedSettings = Settings.builder()
                                                .put(settings)
                                                .put(Environment.PATH_HOME_SETTING.getKey(), createTempDir("tempDir").toString())
                                                .build();
        // need to use custom config path so we can use a custom log4j2.properties file for the test
        final Environment environment = new Environment(mergedSettings, configDir);
        LogConfigurator.configure(environment);
    }

    public Path getDataPath(String relativePath) {
        // we override LTC behavior here: wrap even resources with mockfilesystems,
        // because some code is buggy when it comes to multiple nio.2 filesystems
        // (e.g. FileSystemUtils, and likely some tests)
        try {
            URL resource1 = getClass().getClassLoader().getResource(relativePath);
            return PathUtils.get(resource1.toURI());
        } catch (Exception e) {
            throw new RuntimeException("resource not found: " + relativePath, e);
        }
    }


    public static Path createTempDir(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }


}

