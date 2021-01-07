/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.rest.action.document;

import org.elasticsearch.Version;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.rest.RestRequest;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.HEAD;

public class RestGetActionV7 extends RestGetAction {

    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(RestGetActionV7.class);
    static final String TYPES_DEPRECATION_MESSAGE = "[types removal] Specifying types in "
        + "document get requests is deprecated, use the /{index}/_doc/{id} endpoint instead.";

    @Override
    public String getName() {
        return super.getName() + "_v7";
    }

    @Override
    public List<Route> routes() {
        return List.of(new Route(GET, "/{index}/{type}/{id}"), new Route(HEAD, "/{index}/{type}/{id}"));
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, final NodeClient client) throws IOException {
//        deprecationLogger.deprecate("get_with_types", TYPES_DEPRECATION_MESSAGE);
        request.param("type");
        return super.prepareRequest(request, client);
    }

    @Override
    public Version compatibleWithVersion() {
        return Version.V_7_0_0;
    }
}
