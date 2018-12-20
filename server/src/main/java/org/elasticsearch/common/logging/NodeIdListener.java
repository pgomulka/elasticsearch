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

package org.elasticsearch.common.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.cluster.node.DiscoveryNode;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class NodeIdListener implements ClusterStateListener {

    public static final String UNKOWN_NODE_ID = "";//formatIds("unkown_id","unkown_id");
    private static final Logger LOGGER = LogManager.getLogger(NodeIdListener.class);
    private AtomicReference<String> nodeId = new AtomicReference<>(UNKOWN_NODE_ID);


    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        DiscoveryNode localNode = event.state().getNodes().getLocalNode();
        String clusterUUID = event.state().getMetaData().clusterUUID();
        String nodeId = localNode.getId();
        boolean wasSet = this.nodeId.compareAndSet(UNKOWN_NODE_ID, formatIds(clusterUUID,nodeId));
        if (wasSet) {
            LOGGER.info("received first cluster state update. Setting nodeId={}", nodeId);
            final LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration config = context.getConfiguration();



            PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("nodeId: "+nodeId+"message\": \"%.-10000m")
                .build();
            Appender appender = ConsoleAppender.createAppender(layout, null, ConsoleAppender.Target.SYSTEM_OUT, "console",
                false, false, false);


            appender.start();
            config.getAppenders().put(appender.getName(),appender);

            AppenderRef ref = AppenderRef.createAppenderRef("console", null, null);
            AppenderRef[] refs = new AppenderRef[] {ref};
            LoggerConfig rootConfig = LoggerConfig.
                createLogger(false, Level.INFO, "", "true", refs, null, config, null);
//            rootConfig.getAppenders().put("")
//            LoggerConfig loggerConfig = LoggerConfig.createLogger("false", "info", "org.apache.logging.log4j",
//                "true", refs, null, config, null );

//            rootConfig.removeAppender("console");
            rootConfig.addAppender(appender, null, null);


            config.removeLogger("");
            config.addLogger("", rootConfig);
            synchronized (config){
                context.updateLoggers();

            }
        }

    }

    private static String formatIds(String clusterUUID, String nodeId) {
        return String.format(Locale.ROOT,"\"cluster_uuid\": \"%s\", \"node_id2\": \"%s\", ",clusterUUID,nodeId);
    }

    public AtomicReference<String> getNodeId() {
        return nodeId;
    }
}
