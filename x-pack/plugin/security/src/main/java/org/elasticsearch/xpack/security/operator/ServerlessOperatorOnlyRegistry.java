/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.operator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexAction;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexAction;
import org.elasticsearch.action.admin.indices.template.put.PutComposableIndexTemplateAction;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.Scope;
import org.elasticsearch.rest.action.admin.indices.RestGetComposableIndexTemplateAction;
import org.elasticsearch.rest.action.admin.indices.RestGetIndicesAction;
import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.xcontent.XContentBuilder;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class ServerlessOperatorOnlyRegistry implements OperatorOnlyRegistry{

    private final IndexScopedSettings indexScopedSettings;
    private static final Logger logger = LogManager.getLogger(ServerlessOperatorOnlyRegistry.class);
    private static final Set<String> PARTIALLY_RESTRICTED_PATHS = Set.of("/");

    private final Set<String> partiallyRestrictedPaths;
    private final SettingsFilter settingsFilter;

    public ServerlessOperatorOnlyRegistry(IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter) {
        this.indexScopedSettings = indexScopedSettings;
        this.settingsFilter = settingsFilter;
        this.partiallyRestrictedPaths = PARTIALLY_RESTRICTED_PATHS;
    }

    @Override
    public OperatorPrivilegesViolation checkRest(RestHandler restHandler, RestRequest restRequest, RestChannel restChannel) {
        try {
            markRestRequestsToFilterSettings(restHandler, restRequest);
            Scope scope = restHandler.getServerlessScope();
            Objects.requireNonNull(
                scope,
                "Serverless scope must not be null. " + "Please report this as a bug. Request URI: [" + restRequest.uri() + "]"
            ); // upstream guarantees this is never null
            if (Scope.INTERNAL.equals(scope)) {
                String errorMessage = String.format(
                    Locale.ROOT,
                    "Request for uri [%s] with method [%s] exists but is not available when running in serverless mode",
                    restRequest.uri(),
                    restRequest.method()
                );
                try (XContentBuilder builder = restChannel.newErrorBuilder()) {
                    builder.startObject();
                    builder.field("error", errorMessage);
                    builder.endObject();
                    restChannel.sendResponse(new RestResponse(RestStatus.NOT_FOUND, builder));
                }
                return () -> errorMessage;
            } else if (restHandler.routes().stream().map(RestHandler.Route::getPath).anyMatch(partiallyRestrictedPaths::contains)) {
                assert Scope.PUBLIC.equals(scope);
                restRequest.markResponseRestricted("serverless");
                logger.trace("Marked request for uri [{}] as restricted for serverless", restRequest.uri());
            }
        } catch (Exception e) {
            throw new ElasticsearchException(e);
        }
        return null;
    }

    Set<Class<? extends RestHandler>> settingsReturningHandlers = Set.of(RestGetIndicesAction.class, RestGetComposableIndexTemplateAction.class);
    private void markRestRequestsToFilterSettings(RestHandler restHandler, RestRequest restRequest) {
        if(settingsReturningHandlers.stream().anyMatch(c -> c.isAssignableFrom(restHandler.getClass()))) { // we could have an annotation etc
            settingsFilter.addFilterSettingParams(restRequest);
        }
    }

    @Override
    public OperatorPrivilegesViolation check(String action, TransportRequest request) {
        if (CreateIndexAction.NAME.equals(action)) {
            assert request instanceof CreateIndexRequest;
            return checkIndexSettings(((CreateIndexRequest) request).settings());
        } else if (PutComposableIndexTemplateAction.NAME.equals(action)) {
            return checkIndexSettings(((PutComposableIndexTemplateAction.Request) request).indexTemplate().template().settings());
        }//more ifs/map/hooks
        return null;  // do nothing
    }


    private OperatorPrivilegesViolation checkIndexSettings(Settings settings) {
        List<String> list = settings
            .keySet()
            .stream()
            .filter(settingName -> indexScopedSettings.get(settingName).isServerlessPublic())
            .toList();
        if (false == list.isEmpty()) {
            return () -> (list.size() == 1 ? "setting" : "settings") + " [" + Strings.collectionToDelimitedString(list, ",") + "]";
        } else {
            return null;
        }
    }
}
