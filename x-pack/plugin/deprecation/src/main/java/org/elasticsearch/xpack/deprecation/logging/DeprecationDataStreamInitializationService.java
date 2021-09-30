/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.deprecation.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.xpack.core.action.CreateDataStreamAction;

import java.util.concurrent.atomic.AtomicBoolean;

class DeprecationDataStreamInitializationService implements ClusterStateListener {
    private static final Logger logger = LogManager.getLogger(DeprecationIndexingComponent.class);

    private final Client client;
    private boolean isMaster = false;
    private final AtomicBoolean isIndexCreationInProgress = new AtomicBoolean(false);

    public DeprecationDataStreamInitializationService(Client client) {
        this.client = client;
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        this.isMaster = event.localNodeMaster();
        if (this.isMaster && isIndexCreationInProgress.compareAndSet(false, true)) {

            CreateDataStreamAction.Request putDataStreamRequest =
                new CreateDataStreamAction.Request(DeprecationIndexingAppender.DEPRECATION_MESSAGES_DATA_STREAM);
            client.execute(CreateDataStreamAction.INSTANCE, putDataStreamRequest, new ActionListener<AcknowledgedResponse>() {
                @Override
                public void onResponse(AcknowledgedResponse acknowledgedResponse) {
                    logger.info("create ds success ");
                    isIndexCreationInProgress.set(false);
                }

                @Override
                public void onFailure(Exception e) {
                    logger.info("ds failure ", e);
                    isIndexCreationInProgress.set(false);
                }
            });
        }
    }
}
