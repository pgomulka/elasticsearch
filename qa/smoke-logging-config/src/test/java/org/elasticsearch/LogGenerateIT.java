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

package org.elasticsearch;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.test.rest.ESRestTestCase;
import org.junit.Assert;

import java.io.IOException;

public class LogGenerateIT extends ESRestTestCase {

    public void testStartUp() throws IOException {
        Assert.fail();
    }

    public void testSlowLogs() throws IOException {
        String indexSettings = "{\n" +
            "  \"settings\": {\n" +
            "    \"index.search.slowlog.level\": \"trace\",\n" +
            "    \"index.search.slowlog.threshold.query.trace\": 0,\n" +
            "    \"index.indexing.slowlog.level\": \"trace\",\n" +
            "    \"index.indexing.slowlog.threshold.index.trace\": 0\n" +
            "  }\n" +
            "}";
        final Request putIndexRequest = new Request("PUT", "test_index");
        putIndexRequest.setJsonEntity(indexSettings);
        assertOK(client().performRequest(putIndexRequest));

        String docBody = "{ \"field\":123 }";
        final Request postDoc = new Request("POST", "test_index/_doc");
        postDoc.setJsonEntity(docBody);
        assertOK(client().performRequest(postDoc));

        final Request searchRequest = new Request("GET", "test_index/_search");
        assertOK(client().performRequest(searchRequest));

        Assert.fail();
    }

    public void testDeprecationLogs() throws IOException {
        String mappingWithDeprecatedPrecision = "{\n" +
            "\t\"mappings\":{\n" +
            "    \"properties\": {\n" +
            "         \"location\": {\n" +
            "             \"type\": \"geo_shape\",\n" +
            "             \"tree\": \"quadtree\",\n" +
            "             \"precision\": \"1m\"\n" +
            "         }\n" +
            "    }\n" +
            "\t}\n" +
            "}";
         Request putIndexRequest = new Request("PUT", "test_index");
        putIndexRequest.setJsonEntity(mappingWithDeprecatedPrecision);
        assertOK(client().performRequest(putIndexRequest));


        putIndexRequest = new Request("PUT", "test_index");
         RequestOptions.Builder options = putIndexRequest.getOptions().toBuilder();
        options.addHeader("X-Opaque-Id", "my-identifier");
        putIndexRequest.setOptions(options);
        putIndexRequest.setJsonEntity(mappingWithDeprecatedPrecision);
        assertOK(client().performRequest(putIndexRequest));
        Assert.fail();
    }


}
