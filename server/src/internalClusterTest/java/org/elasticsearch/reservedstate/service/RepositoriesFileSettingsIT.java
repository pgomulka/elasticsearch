/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.reservedstate.service;

import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesAction;
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesRequest;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import org.elasticsearch.action.admin.cluster.repositories.put.TransportPutRepositoryAction;
import org.elasticsearch.action.admin.cluster.repositories.reservedstate.ReservedRepositoryAction;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.cluster.metadata.ReservedStateErrorMetadata;
import org.elasticsearch.cluster.metadata.ReservedStateHandlerMetadata;
import org.elasticsearch.cluster.metadata.ReservedStateMetadata;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.Strings;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.repositories.RepositoryMissingException;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xcontent.XContentParserConfiguration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.elasticsearch.test.NodeRoles.dataOnlyNode;
import static org.elasticsearch.xcontent.XContentType.JSON;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 0, autoManageMasterNodes = false)
public class RepositoriesFileSettingsIT extends ESIntegTestCase {
    private static AtomicLong versionCounter = new AtomicLong(1);

    private static String testJSON = """
        {
             "metadata": {
                 "version": "%s",
                 "compatibility": "8.4.0"
             },
             "state": {
                 "snapshot_repositories": {
                    "repo": {
                       "type": "fs",
                       "settings": {
                          "location": "my_backup_location"
                       }
                    }
                 }
             }
        }""";
    private static String testJSON2 = """
        {
             "metadata": {
                 "version": "%s",
                 "compatibility": "8.4.0"
             },
             "state": {
                 "snapshot_repositories": {
                    "repo": {
                       "type": "fs",
                       "settings": {
                          "location": "my_backup_location2"
                       }
                    }
                 }
             }
        }""";
    private static String testErrorJSON = """
        {
             "metadata": {
                 "version": "%s",
                 "compatibility": "8.4.0"
             },
             "state": {
                 "snapshot_repositories": {
                    "err-repo": {
                       "type": "interstelar",
                       "settings": {
                          "location": "my_backup_location"
                       }
                    }
                 }
             }
        }""";

    private void assertMasterNode(Client client, String node) throws ExecutionException, InterruptedException {
        assertThat(client.admin().cluster().prepareState().execute().get().getState().nodes().getMasterNode().getName(), equalTo(node));
    }

    private void writeJSONFile(String node, String json) throws Exception {
        long version = versionCounter.incrementAndGet();

        FileSettingsService fileSettingsService = internalCluster().getInstance(FileSettingsService.class, node);

        Files.createDirectories(fileSettingsService.watchedFileDir());
        Path tempFilePath = createTempFile();

        Files.write(tempFilePath, Strings.format(json, version).getBytes(StandardCharsets.UTF_8));
        Files.move(tempFilePath, fileSettingsService.watchedFile(), StandardCopyOption.ATOMIC_MOVE);
    }

    private Tuple<CountDownLatch, AtomicLong> setupClusterStateListener(String node) {
        ClusterService clusterService = internalCluster().clusterService(node);
        CountDownLatch savedClusterState = new CountDownLatch(1);
        AtomicLong metadataVersion = new AtomicLong(-1);
        clusterService.addListener(new ClusterStateListener() {
            @Override
            public void clusterChanged(ClusterChangedEvent event) {
                ReservedStateMetadata reservedState = event.state().metadata().reservedStateMetadata().get(FileSettingsService.NAMESPACE);
                if (reservedState != null) {
                    ReservedStateHandlerMetadata handlerMetadata = reservedState.handlers().get(ReservedRepositoryAction.NAME);
                    if (handlerMetadata != null && handlerMetadata.keys().contains("repo")) {
                        clusterService.removeListener(this);
                        metadataVersion.set(event.state().metadata().version());
                        savedClusterState.countDown();
                    }
                }
            }
        });

        return new Tuple<>(savedClusterState, metadataVersion);
    }

    private void assertClusterStateSaveOK(CountDownLatch savedClusterState, AtomicLong metadataVersion) throws Exception {
        boolean awaitSuccessful = savedClusterState.await(20, TimeUnit.SECONDS);
        assertTrue(awaitSuccessful);

        final var reposResponse = client().execute(
            GetRepositoriesAction.INSTANCE,
            new GetRepositoriesRequest(new String[] { "repo", "repo1" })
        ).get();

        assertThat(
            reposResponse.repositories().stream().map(r -> r.name()).collect(Collectors.toSet()),
            containsInAnyOrder("repo", "repo1")
        );

        assertEquals(
            "Failed to process request "
                + "[org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest/unset] "
                + "with errors: [[repo] set as read-only by [file_settings]]",
            expectThrows(IllegalArgumentException.class, client().execute(TransportPutRepositoryAction.TYPE, sampleRestRequest("repo")))
                .getMessage()
        );
    }

    public void testSettingsApplied() throws Exception {
        final var clusterStateUpdateBarrier = new CyclicBarrier(2);


        final var blockingTask = new ClusterStateUpdateTask() {
            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                clusterStateUpdateBarrier.await(20, TimeUnit.SECONDS);
                clusterStateUpdateBarrier.await(20, TimeUnit.SECONDS);
                return currentState;
            }

            @Override
            public void onFailure(Exception e) {
                fail();
            }
        };
        final var fileSettingBarrier = new CyclicBarrier(2);

        var blockingListener = new FileChangedListener(){
            @Override
            public void watchedFileChanged() {
                try {
                    fileSettingBarrier.await(10, TimeUnit.SECONDS);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
        internalCluster().setBootstrapMasterNodeIndex(0);
        logger.info("--> start data node / non master node");
        var dataNode = internalCluster().startNode(Settings.builder().put(dataOnlyNode()).put("discovery.initial_state_timeout", "1s"));

        var savedClusterState = setupClusterStateListener(dataNode);
        // In internal cluster tests, the nodes share the config directory, so when we write with the data node path
        // the master will pick it up on start



        logger.info("--> start master node");
        final String masterNode = internalCluster().startMasterOnlyNode();
        assertMasterNode(internalCluster().nonMasterClient(), masterNode);
        internalCluster().clusterService(internalCluster().getMasterName()).getMasterService()
            .submitUnbatchedStateUpdateTask("block", blockingTask);
        clusterStateUpdateBarrier.await(10, TimeUnit.SECONDS);

        internalCluster().getInstance(FileSettingsService.class, masterNode)
            .addFileChangedListener(blockingListener);

        writeJSONFile(masterNode, testJSON);
        fileSettingBarrier.await(10, TimeUnit.SECONDS);
        writeJSONFile(masterNode, testJSON2);

        fileSettingBarrier.await(10, TimeUnit.SECONDS);

//        clusterStateUpdateBarrier.await(10, TimeUnit.SECONDS);
        Thread.sleep(10000);
        assertClusterStateSaveOK(savedClusterState.v1(), savedClusterState.v2());
    }

    /*
     internalCluster().getInstance(ReservedClusterStateService.class, masterNode)
            .updateTaskQueue.submitTask("block", new ReservedStateUpdateTask(
                "namespace",
                new ReservedStateChunk(Map.of(), missingVersion),
                List.of(),
                Map.of(),
                List.of(),
                // error state should not be possible since there is no metadata being parsed or processed
                errorState -> { throw new AssertionError(); },
                null
            ){
                @Override
                protected ClusterState execute(ClusterState currentState) {
                    try {
                        fileSettingBarrier.await(10, TimeUnit.SECONDS);
                        //
                        fileSettingBarrier.await(10, TimeUnit.SECONDS);

                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                    return super.execute(currentState);
                }
            },null);
        fileSettingBarrier.await(10, TimeUnit.SECONDS);
     */

    private Tuple<CountDownLatch, AtomicLong> setupClusterStateListenerForError(String node) {
        ClusterService clusterService = internalCluster().clusterService(node);
        CountDownLatch savedClusterState = new CountDownLatch(1);
        AtomicLong metadataVersion = new AtomicLong(-1);
        clusterService.addListener(new ClusterStateListener() {
            @Override
            public void clusterChanged(ClusterChangedEvent event) {
                ReservedStateMetadata reservedState = event.state().metadata().reservedStateMetadata().get(FileSettingsService.NAMESPACE);
                if (reservedState != null && reservedState.errorMetadata() != null) {
                    assertEquals(ReservedStateErrorMetadata.ErrorKind.VALIDATION, reservedState.errorMetadata().errorKind());
                    assertThat(reservedState.errorMetadata().errors(), allOf(notNullValue(), hasSize(1)));
                    assertThat(
                        reservedState.errorMetadata().errors().get(0),
                        containsString("[err-repo] repository type [interstelar] does not exist")
                    );
                    clusterService.removeListener(this);
                    metadataVersion.set(event.state().metadata().version());
                    savedClusterState.countDown();
                }
            }
        });

        return new Tuple<>(savedClusterState, metadataVersion);
    }

    private void assertClusterStateNotSaved(CountDownLatch savedClusterState, AtomicLong metadataVersion) throws Exception {
        boolean awaitSuccessful = savedClusterState.await(20, TimeUnit.SECONDS);
        assertTrue(awaitSuccessful);

        assertEquals(
            "[err-repo] missing",
            expectThrows(
                RepositoryMissingException.class,
                client().execute(GetRepositoriesAction.INSTANCE, new GetRepositoriesRequest(new String[] { "err-repo" }))
            ).getMessage()
        );

        // This should succeed, nothing was reserved
        client().execute(TransportPutRepositoryAction.TYPE, sampleRestRequest("err-repo")).get();
    }

    public void testErrorSaved() throws Exception {
        internalCluster().setBootstrapMasterNodeIndex(0);
        logger.info("--> start data node / non master node");
        internalCluster().startNode(Settings.builder().put(dataOnlyNode()).put("discovery.initial_state_timeout", "1s"));

        logger.info("--> start master node");
        final String masterNode = internalCluster().startMasterOnlyNode();
        assertMasterNode(internalCluster().nonMasterClient(), masterNode);
        var savedClusterState = setupClusterStateListenerForError(masterNode);

        writeJSONFile(masterNode, testErrorJSON);
        assertClusterStateNotSaved(savedClusterState.v1(), savedClusterState.v2());
    }

    private PutRepositoryRequest sampleRestRequest(String name) throws Exception {
        var json = """
            {
              "type": "fs",
              "settings": {
                 "location": "my_backup_location_2"
              }
            }""";

        try (
            var bis = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            var parser = JSON.xContent().createParser(XContentParserConfiguration.EMPTY, bis)
        ) {
            return new PutRepositoryRequest(name).source(parser.map());
        }
    }
}
