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
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.node.NodeClient;
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
    public static final String NAME = "DeprecationIndexer";
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
            return new DeprecationAppender(getName(),getFilter(),getOrCreateLayout());
        }
    }

    @PluginBuilderFactory
    public static <B extends DeprecationAppender.Builder<B>> B newBuilder() {
        return new DeprecationAppender.Builder<B>().asBuilder();
    }

    @Override
    public void append(LogEvent event) {
        byte[] payload = getLayout().toByteArray(event);
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
