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

package org.elasticsearch.rest.action.admin.indices;

import org.apache.logging.log4j.LogManager;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.CompatibleHandlers;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.elasticsearch.client.Requests.putMappingRequest;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

public class RestPutMappingAction extends BaseRestHandler {
    private static final DeprecationLogger deprecationLogger = new DeprecationLogger(
        LogManager.getLogger(RestPutMappingAction.class));
    public static final String TYPES_DEPRECATION_MESSAGE = "[types removal] Using include_type_name in put "
        + "mapping requests is deprecated. The parameter will be removed in the next major version.";

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(
            new Route(POST, "/{index}/_mapping/"),
            new Route(PUT, "/{index}/_mapping/"),
            new Route(POST, "/{index}/_mappings/"),
            new Route(PUT, "/{index}/_mappings/")));
    }

    @Override
    public String getName() {
        return "put_mapping_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        PutMappingRequest putMappingRequest = putMappingRequest(Strings.splitStringByCommaToArray(request.param("index")));

        Map<String, Object> sourceAsMap = XContentHelper.convertToMap(request.requiredContent(), false,
            request.getXContentType()).v2();
        if(CompatibleHandlers.isV7Compatible(request)) {
            final boolean includeTypeName = request.paramAsBoolean(INCLUDE_TYPE_NAME_PARAMETER,
                DEFAULT_INCLUDE_TYPE_NAME_POLICY);
            if (request.hasParam(INCLUDE_TYPE_NAME_PARAMETER)) {
                deprecationLogger.deprecatedAndMaybeLog("put_mapping_with_types", TYPES_DEPRECATION_MESSAGE);
            }
            final String type = request.param("type");
//            putMappingRequest.type(includeTypeName ? type : MapperService.SINGLE_MAPPING_NAME);
            if(includeTypeName && isMappingSourceTyped(type,sourceAsMap)){
                sourceAsMap = CompatibleHandlers.replaceTypeWithDoc(sourceAsMap);
            }
            if (includeTypeName == false &&
                (type != null || isMappingSourceTyped(MapperService.SINGLE_MAPPING_NAME, sourceAsMap))) {
                throw new IllegalArgumentException("Types cannot be provided in put mapping requests, unless " +
                    "the include_type_name parameter is set to true.");
            }


        }else{
            if (MapperService.isMappingSourceTyped(MapperService.SINGLE_MAPPING_NAME, sourceAsMap)) {
                throw new IllegalArgumentException("Types cannot be provided in put mapping requests");
            }
        }

        putMappingRequest.source(sourceAsMap);
        putMappingRequest.timeout(request.paramAsTime("timeout", putMappingRequest.timeout()));
        putMappingRequest.masterNodeTimeout(request.paramAsTime("master_timeout", putMappingRequest.masterNodeTimeout()));
        putMappingRequest.indicesOptions(IndicesOptions.fromRequest(request, putMappingRequest.indicesOptions()));
        return channel -> client.admin().indices().putMapping(putMappingRequest, new RestToXContentListener<>(channel));
    }

    public static boolean isMappingSourceTyped(String type, Map<String, Object> mapping) {
        return mapping.size() == 1 && mapping.keySet().iterator().next().equals(type);
    }


    public static class CompatibleRestPutMappingAction extends RestPutMappingAction {
        @Override
        public List<Route> routes() {
            return unmodifiableList(asList(
                new Route(PUT, "/{index}/{type}/_mapping/"),
                new Route(PUT, "/{index}/_mapping/{type}"),
                new Route(PUT, "/_mapping/{type}"),

                new Route(POST, "/{index}/{type}/_mapping/"),
                new Route(POST, "/{index}/_mapping/{type}"),
                new Route(POST, "/_mapping/{type}"),

                new Route(PUT, "/{index}/{type}/_mappings/"),
                new Route(PUT, "/{index}/_mappings/{type}"),
                new Route(PUT, "/_mappings/{type}"),

                new Route(POST, "/{index}/{type}/_mappings/"),
                new Route(POST, "/{index}/_mappings/{type}"),
                new Route(POST, "/_mappings/{type}")
            ));
        }

        @Override
        public boolean compatibilityRequired() {
            return true;
        }
    }
}
