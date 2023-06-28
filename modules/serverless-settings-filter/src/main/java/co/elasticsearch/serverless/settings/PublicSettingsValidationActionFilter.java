/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package co.elasticsearch.serverless.settings;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexAction;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.tasks.Task;

import java.util.Map;
import java.util.function.Function;

public class PublicSettingsValidationActionFilter implements ActionFilter {
    private final PublicSettingsValidator publicSettingsValidator;

    private final Map<String, Function<? extends ActionRequest, Settings>> mappingFunctions = Map.ofEntries(createIndexAction());

    private Map.Entry<String, Function<CreateIndexRequest, Settings>> createIndexAction() {
        return Map.entry(CreateIndexAction.NAME, (CreateIndexRequest ir) -> ir.settings());
    }

    public PublicSettingsValidationActionFilter(ThreadContext threadContext, IndexScopedSettings indexScopedSettings) {
        this.publicSettingsValidator = new PublicSettingsValidator(threadContext, indexScopedSettings);
    }

    @Override
    public int order() {
        return 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Request extends ActionRequest, Response extends ActionResponse> void apply(
        Task task,
        String action,
        Request request,
        ActionListener<Response> listener,
        ActionFilterChain<Request, Response> chain
    ) {
        if (mappingFunctions.containsKey(action)) {
            // casting??
            Function<Request, Settings> settingsFunction = (Function<Request, Settings>) mappingFunctions.get(CreateIndexAction.NAME);
            Settings apply = settingsFunction.apply(request);
            publicSettingsValidator.validateSettings(apply);
        }

        chain.proceed(task, action, request, listener);
    }

}
