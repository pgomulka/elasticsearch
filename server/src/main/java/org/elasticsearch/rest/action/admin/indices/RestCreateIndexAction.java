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

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.xcontent.LoggingDeprecationHandler;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.CompatibleHandlers;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestCreateIndexAction extends BaseRestHandler {

    public RestCreateIndexAction(RestController controller) {
        controller.registerHandler(RestRequest.Method.PUT, "/{index}", this, List.of(CompatibleHandlers.consumeParameterIncludeType()));
    }

    @Override
    public String getName() {
        return "create_index_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {

        CreateIndexRequest createIndexRequest = new CreateIndexRequest(request.param("index"));

        if (request.hasContent()) {
            Map<String, Object> sourceAsMap = XContentHelper.convertToMap(request.requiredContent(), false,
                request.getXContentType()).v2();
            if(CompatibleHandlers.isRequestCompatible(request)){
                sourceAsMap = prepareMappingsV7(sourceAsMap, request);
            }else {
                sourceAsMap = prepareMappings(sourceAsMap);
            }

            createIndexRequest.source(sourceAsMap, LoggingDeprecationHandler.INSTANCE);
        }

        createIndexRequest.timeout(request.paramAsTime("timeout", createIndexRequest.timeout()));
        createIndexRequest.masterNodeTimeout(request.paramAsTime("master_timeout", createIndexRequest.masterNodeTimeout()));
        createIndexRequest.waitForActiveShards(ActiveShardCount.parseString(request.param("wait_for_active_shards")));
        return channel -> client.admin().indices().create(createIndexRequest, new RestToXContentListener<>(channel));
    }

    private void updateType(Map<String, Object> sourceAsMap) {

    }


    static Map<String, Object> prepareMappings(Map<String, Object> source) {

        if (source.containsKey("mappings") == false
            || (source.get("mappings") instanceof Map) == false) {
            return source;
        }

        Map<String, Object> newSource = new HashMap<>(source);

        @SuppressWarnings("unchecked")
        Map<String, Object> mappings = (Map<String, Object>) source.get("mappings");
        if (MapperService.isMappingSourceTyped(MapperService.SINGLE_MAPPING_NAME, mappings)) {
            throw new IllegalArgumentException("The mapping definition cannot be nested under a type");
        }

        newSource.put("mappings", Collections.singletonMap(MapperService.SINGLE_MAPPING_NAME, mappings));
        return newSource;
    }

    static Map<String, Object> prepareMappingsV7(Map<String, Object> source, RestRequest request) {
        final String INCLUDE_TYPE_NAME_PARAMETER = "include_type_name";
        final boolean DEFAULT_INCLUDE_TYPE_NAME_POLICY = false;
        final boolean includeTypeName = request.paramAsBoolean(INCLUDE_TYPE_NAME_PARAMETER,
            DEFAULT_INCLUDE_TYPE_NAME_POLICY);

        Map<String, Object> newSource = new HashMap<>(source);
        @SuppressWarnings("unchecked")
        Map<String, Object> mappings = (Map<String, Object>) source.get("mappings");

        if (includeTypeName && mappings.size() == 1){
            //no matter what the type was, replace it with _doc
            String key = mappings.keySet().iterator().next();
            @SuppressWarnings("unchecked")
            Map<String, Object>  typedMappings = (Map<String, Object>) mappings.get(key);

            newSource.put("mappings", Collections.singletonMap(MapperService.SINGLE_MAPPING_NAME, typedMappings));
            return newSource;
        }else if (source.containsKey("mappings") == false
            || (source.get("mappings") instanceof Map) == false) {
            return source;
        }

        if (MapperService.isMappingSourceTyped(MapperService.SINGLE_MAPPING_NAME, mappings)) {
            throw new IllegalArgumentException("The mapping definition cannot be nested under a type " +
                "[" + MapperService.SINGLE_MAPPING_NAME + "] unless include_type_name is set to true.");
        }

        newSource.put("mappings", Collections.singletonMap(MapperService.SINGLE_MAPPING_NAME, mappings));
        return newSource;
    }
}
