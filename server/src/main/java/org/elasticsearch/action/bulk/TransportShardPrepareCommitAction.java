/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action.bulk;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.replication.TransportWriteAction;
import org.elasticsearch.cluster.action.shard.ShardStateAction;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexingPressure;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.indices.ExecutorSelector;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.indices.SystemIndices;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;

public class TransportShardPrepareCommitAction extends TransportWriteAction<
    ShardPrepareCommitRequest,
    ShardPrepareCommitRequest,
    ShardPrepareCommitResponse> {

    @Inject
    public TransportShardPrepareCommitAction(
        Settings settings,
        TransportService transportService,
        ClusterService clusterService,
        IndicesService indicesService,
        ThreadPool threadPool,
        ShardStateAction shardStateAction,
        ActionFilters actionFilters,
        IndexingPressure indexingPressure,
        SystemIndices systemIndices
    ) {
        super(
            settings,
            ShardPrepareCommitAction.NAME,
            transportService,
            clusterService,
            indicesService,
            threadPool,
            shardStateAction,
            actionFilters,
            ShardPrepareCommitRequest::new,
            ShardPrepareCommitRequest::new,
            ExecutorSelector::getWriteExecutorForShard,
            false,
            indexingPressure,
            systemIndices
        );
    }

    @Override
    protected ShardPrepareCommitResponse newResponseInstance(StreamInput in) throws IOException {
        return new ShardPrepareCommitResponse(in);
    }

    @Override
    protected void dispatchedShardOperationOnPrimary(
        ShardPrepareCommitRequest request,
        IndexShard primary,
        ActionListener<PrimaryResult<ShardPrepareCommitRequest, ShardPrepareCommitResponse>> listener
    ) {
        listener.onResponse(new PrimaryResult<>(request, new ShardPrepareCommitResponse(primary.prepareCommit(request.txid()))));
    }

    @Override
    protected void dispatchedShardOperationOnReplica(
        ShardPrepareCommitRequest request,
        IndexShard replica,
        ActionListener<ReplicaResult> listener
    ) {
        // todo: need to replicate the decision from primary.
        listener.onResponse(new ReplicaResult());
    }
}
