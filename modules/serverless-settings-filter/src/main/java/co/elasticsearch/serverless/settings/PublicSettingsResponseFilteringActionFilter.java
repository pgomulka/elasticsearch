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
import org.elasticsearch.action.admin.indices.get.GetIndexAction;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.tasks.Task;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PublicSettingsResponseFilteringActionFilter implements ActionFilter {
    private final PublicSettingsFilter publicSettingsFilter;
    private final Map<String, Function<? extends ActionResponse, ? extends ActionResponse>> mappingFunctions = Map.ofEntries(
        getIndexAction()
    );

    public PublicSettingsResponseFilteringActionFilter(ThreadContext threadContext, IndexScopedSettings indexScopedSettings) {
        this.publicSettingsFilter = new PublicSettingsFilter(threadContext, indexScopedSettings);
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
            // casting?? and no assert about action matching the response type
            Function<Response, Response> function = (Function<Response, Response>) mappingFunctions.get(action);

            ActionListener<Response> map = listener.map(response -> function.apply(response));
            chain.proceed(task, action, request, map);
        } else {
            chain.proceed(task, action, request, listener);
        }

    }

    private Map.Entry<String, Function<GetIndexResponse, GetIndexResponse>> getIndexAction() {
        return Map.entry(GetIndexAction.NAME, (GetIndexResponse r) -> {
            var settings = filter(r.getSettings());
            var defSettings = filter(r.getSettings());
            return new GetIndexResponse(r.indices(), r.mappings(), r.aliases(), settings, defSettings, r.dataStreams());
        });
    }

    private Map<String, Settings> filter(Map<String, Settings> settings) {
        return settings.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> publicSettingsFilter.filterPublic(e.getValue())));
    }

}
