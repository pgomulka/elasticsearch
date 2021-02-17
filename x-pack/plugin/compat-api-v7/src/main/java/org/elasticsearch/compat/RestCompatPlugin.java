/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.compat;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.action.admin.indices.RestCreateIndexActionV7;
import org.elasticsearch.rest.action.document.RestGetActionV7;
import org.elasticsearch.rest.action.document.RestIndexActionV7;


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
                new RestCreateIndexActionV7(),
                new RestGetActionV7(),
                new RestIndexActionV7.CompatibleRestIndexAction(),
                new RestIndexActionV7.CompatibleCreateHandler(),
                new RestIndexActionV7.CompatibleAutoIdHandler(nodesInCluster)

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
