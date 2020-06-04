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

package org.elasticsearch.common.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.common.Strings.isNullOrEmpty;

@Plugin(name = DeprecationAppender.NAME, category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class DeprecationAppender extends AbstractAppender{
    public static final String NAME = "deprecation_indexer";
    private static final Logger logger = LogManager.getLogger(DeprecationAppender.class);

    private static final String TEMPLATE_NAME = ".deprecation_logs";

//    private final ClusterService clusterService;
//    private final NodeClient nodeClient;

    private boolean isTemplateCreated = false;
    private NodeClient nodeClient;

    public DeprecationAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout);
    }

    @Override
    public void start() {
        this.setStarting();
        if (getFilter() != null) {
            getFilter().start();
        }
//        this.setStarted();
    }

    public void start(NodeClient client) {
        this.nodeClient = client;
        setStarted();//sets volatile variable
    }

    public static class Builder<B extends DeprecationAppender.Builder<B>> extends AbstractAppender.Builder<B>
        implements org.apache.logging.log4j.core.util.Builder<DeprecationAppender> {

        @Override
        public DeprecationAppender build() {
            return new DeprecationAppender(getName(),getFilter(),getLayout());
        }
    }

//    public DeprecationAppender(/*ClusterService clusterService, NodeClient nodeClient*/) {
//        this.clusterService = clusterService;
//        this.nodeClient = nodeClient;
//
//    }


    /**
     * Records a deprecation message to the `.deprecations` index.
     *
     * @param key       the key that was used to determine if this deprecation should have been be logged.
     *                  This is potentially useful when aggregating the recorded messages.
     * @param message   the message to log
     * @param xOpaqueId the associated "X-Opaque-ID" header value, if any
     * @param params    parameters to the message, if any
     */
    public void indexDeprecationMessage(String key, String message, String xOpaqueId, Object[] params) {
//        if (isTemplateCreated == false) {
//            return;
//        }

        Map<String, Object> payload = new HashMap<>();

        // ECS fields
        payload.put("@timestamp", Instant.now().toString());
        payload.put("message", message);
        if (isNullOrEmpty(key) == false) {
            payload.put("tags", key);
        }

        // Other fields
        if (isNullOrEmpty(xOpaqueId) == false) {
            // I considered putting this under labels.x-opaque-id, per ECS,
            // but wondered if that was a stretch? Also it may have high
            // cardinality, meaning that describing it as a label might
            // be a stretch.
            payload.put("x-opaque-id", xOpaqueId);
        }
        if (params != null && params.length > 0) {
            payload.put("params", params);
        }

        final String indexName = TEMPLATE_NAME + "." + DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now());

        new IndexRequestBuilder(nodeClient, IndexAction.INSTANCE).setIndex(indexName)
            .setOpType(DocWriteRequest.OpType.CREATE)
            .setSource(payload)
            .execute(new ActionListener<>() {
                @Override
                public void onResponse(IndexResponse indexResponse) {
                    // Nothing to do
                }

                @Override
                public void onFailure(Exception e) {
                    logger.error("Failed to index deprecation message", e);
                }
            });
    }

    @Override
    public void append(LogEvent event) {
        String payload = event.getMessage().getFormattedMessage();
        final String indexName = TEMPLATE_NAME + "." + DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now());

        new IndexRequestBuilder(nodeClient, IndexAction.INSTANCE).setIndex(indexName)
            .setOpType(DocWriteRequest.OpType.CREATE)
            .setSource(payload, XContentType.JSON)
            .execute(new ActionListener<>() {
                @Override
                public void onResponse(IndexResponse indexResponse) {
                    // Nothing to do
                }

                @Override
                public void onFailure(Exception e) {
                    logger.error("Failed to index deprecation message", e);
                }
            });
    }
}
