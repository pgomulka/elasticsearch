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

package org.elasticsearch.compat;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.index.reindex.RestDeleteByQueryActionV7;
import org.elasticsearch.index.reindex.RestUpdateByQueryActionV7;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.action.admin.indices.RestCreateIndexActionV7;
import org.elasticsearch.rest.action.document.RestGetActionV7;
import org.elasticsearch.rest.action.document.RestIndexActionV7;
import org.elasticsearch.rest.action.document.RestMultiTermVectorsActionV7;
import org.elasticsearch.rest.action.document.RestTermVectorsActionV7;
import org.elasticsearch.rest.action.search.RestMultiSearchActionV7;
import org.elasticsearch.rest.action.search.RestSearchActionV7;
import org.elasticsearch.script.mustache.RestMultiSearchTemplateActionV7;
import org.elasticsearch.script.mustache.RestSearchTemplateActionV7;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class RestCompatPlugin extends Plugin implements ActionPlugin {

    @Override
    public List<RestHandler> getRestHandlers(
        Settings settings,
        RestController restController,
        ClusterSettings clusterSettings,
        IndexScopedSettings indexScopedSettings,
        SettingsFilter settingsFilter,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<DiscoveryNodes> nodesInCluster
    ) {
        if (Version.CURRENT.major == 8) {
            return validateCompatibleHandlers(
                7,
                new RestDeleteByQueryActionV7(),
                new RestUpdateByQueryActionV7(),
                new RestCreateIndexActionV7(),
                new RestGetActionV7(),
                new RestIndexActionV7.CompatibleRestIndexAction(),
                new RestIndexActionV7.CompatibleCreateHandler(),
                new RestIndexActionV7.CompatibleAutoIdHandler(nodesInCluster),
                new RestTermVectorsActionV7(),
                new RestMultiTermVectorsActionV7(),
                new RestSearchActionV7(),
                new RestMultiSearchActionV7(settings),
                new RestSearchTemplateActionV7(),
                new RestMultiSearchTemplateActionV7(settings)
            );
        }
        return Collections.emptyList();
    }

    // default scope for testing
    List<RestHandler> validateCompatibleHandlers(int expectedVersion, RestHandler... handlers) {
        for (RestHandler handler : handlers) {
            if (handler.compatibleWithVersion().major != expectedVersion) {
                String msg = String.format(
                    Locale.ROOT,
                    "Handler %s is of incorrect version %s.",
                    handler.getClass().getSimpleName(),
                    handler.compatibleWithVersion()
                );
                throw new IllegalStateException(msg);
            }
        }
        return List.of(handlers);
    }
}
