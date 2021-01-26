/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ilm.action;

import org.elasticsearch.Version;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.core.ilm.action.PutLifecycleAction;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.PUT;

public class RestPutLifecycleActionV7 extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return List.of(new Route(PUT, "/_ilm/policy/{name}"));
    }

    @Override
    public String getName() {
        return "ilm_put_action_v7";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String lifecycleName = restRequest.param("name");
        try (XContentParser parser = restRequest.contentParser(true)) {
            PutLifecycleAction.Request putLifecycleRequest = PutLifecycleAction.Request.parseRequest(lifecycleName, parser);
            putLifecycleRequest.timeout(restRequest.paramAsTime("timeout", putLifecycleRequest.timeout()));
            putLifecycleRequest.masterNodeTimeout(restRequest.paramAsTime("master_timeout", putLifecycleRequest.masterNodeTimeout()));

            return channel -> client.execute(PutLifecycleAction.INSTANCE, putLifecycleRequest, new RestToXContentListener<>(channel));
        }
    }

    @Override
    public Version compatibleWithVersion() {
        return Version.CURRENT.minimumRestCompatibilityVersion();
    }
}
