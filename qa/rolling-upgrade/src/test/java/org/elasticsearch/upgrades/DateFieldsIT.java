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
package org.elasticsearch.upgrades;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.Version;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.WarningsHandler;
import org.junit.BeforeClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.elasticsearch.rest.action.search.RestSearchAction.TOTAL_HITS_AS_INT_PARAM;

/**
 * This is test is meant to verify that when upgrading from 6.x version to 7.7 or newer it is able to parse date fields with joda pattern.
 *
 * The test is indexing documents and searches with use of joda or java pattern.
 * In order to make sure that serialization logic is used a search call is executed 3 times (using all nodes)
 *
 * A special flag on DocValues is used to indicate that an index was created in 6.x and has a joda pattern.
 * When upgrading from 7.0-7.6 to 7.7 there is no way to tell if a pattern was created in 6.x as this flag cannot be added.
 * Hence a skip assume section in init()
 *
 * @see org.elasticsearch.search.DocValueFormat.DateTime
 */
public class DateFieldsIT extends AbstractRollingTestCase {
    @BeforeClass
    public static void init(){
        assumeTrue("upgrading from 7.0-7.6 will fail parsing joda formats",
            UPGRADE_FROM_VERSION.before(Version.V_7_0_0));
    }

    public void testJodaBackedDocValueAndDateFields() throws Exception {
        switch (CLUSTER_TYPE) {
            case OLD:
                Request createTestIndex = indexWithDateField("joda_time", "YYYY-MM-dd'T'HH:mm:ssZZ");
                createTestIndex.setOptions(ignoreWarnings());

                Response resp = client().performRequest(createTestIndex);
                assertEquals(200, resp.getStatusLine().getStatusCode());

                postNewDoc("joda_time/_doc");

                break;
            case MIXED:
                postNewDoc("joda_time/_doc");

                Request search = dateRangeSearch("joda_time/_search");
                search.setOptions(ignoreWarnings());

                Response searchResp = client().performRequest(search,3);
                assertEquals(200, searchResp.getStatusLine().getStatusCode());
                break;
            case UPGRADED:
                postNewDoc("joda_time/_doc");
                search = searchWithAgg("joda_time/_search");
                search.setOptions(ignoreWarnings());

                searchResp = client().performRequest(search,3);
                assertSearchResponse(searchResp, 4);
                break;
        }
    }

    public void testJavaBackedDocValueAndDateFields() throws Exception {
        switch (CLUSTER_TYPE) {
            case OLD:
                Request createTestIndex = indexWithDateField("java_time", "8yyyy-MM-dd'T'HH:mm:ssXXX");
                Response resp = client().performRequest(createTestIndex);
                assertEquals(200, resp.getStatusLine().getStatusCode());

                postNewDoc("java_time/_doc");

                break;
            case MIXED:
                postNewDoc("java_time/_doc");

                Request search = dateRangeSearch("java_time/_search");
                Response searchResp = client().performRequest(search);
                assertEquals(200, searchResp.getStatusLine().getStatusCode());
                break;
            case UPGRADED:
                postNewDoc("java_time/_doc");

                search = searchWithAgg("java_time/_search");
                searchResp = client().performRequest(search);
                assertSearchResponse(searchResp, 4);
                break;
        }
    }

    private RequestOptions ignoreWarnings() {
        RequestOptions.Builder options = RequestOptions.DEFAULT.toBuilder();
        options.setWarningsHandler(WarningsHandler.PERMISSIVE);
        return options.build();
    }

    private void assertSearchResponse(Response searchResp, int count) throws IOException {
        assertEquals(200, searchResp.getStatusLine().getStatusCode());
        assertEquals("{\"hits\":{\"total\":" + count + "}}",
        EntityUtils.toString(searchResp.getEntity(), StandardCharsets.UTF_8));
    }

    private Request dateRangeSearch(String endpoint) {
        Request search = new Request("GET", endpoint);
        search.addParameter(TOTAL_HITS_AS_INT_PARAM, "true");
        search.addParameter("filter_path", "hits.total");
        search.setJsonEntity("" +
                "{\n" +
                "  \"sort\": \"datetime\",\n" +
                "  \"query\": {\n" +
                "    \"range\": {\n" +
                "      \"datetime\": {\n" +
                "        \"gte\": \"2020-01-01T00:00:00+01:00\",\n" +
                "        \"lte\": \"2020-01-02T00:00:00+01:00\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n"
        );
        return search;
    }

    private Request searchWithAgg(String endpoint) {
        Request search = new Request("GET", endpoint);
        search.addParameter(TOTAL_HITS_AS_INT_PARAM, "true");
        search.addParameter("filter_path", "hits.total");

        search.setJsonEntity("{\n" +
            "  \"sort\": \"datetime\",\n" +
            "  \"query\": {\n" +
            "    \"range\": {\n" +
            "      \"datetime\": {\n" +
            "        \"gte\": \"2020-01-01T00:00:00+01:00\",\n" +
            "        \"lte\": \"2020-01-02T00:00:00+01:00\"\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"aggs\" : {\n" +
            "    \"docs_per_year\" : {\n" +
            "      \"date_histogram\" : {\n" +
            "        \"field\" : \"date\",\n" +
            "        \"calendar_interval\" : \"year\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n"
        );
        return search;
    }
    private Request indexWithDateField(String indexName, String format) {
        Request createTestIndex = new Request("PUT", indexName);
        createTestIndex.addParameter("include_type_name", "false");
        createTestIndex.setJsonEntity("{\n" +
            "  \"settings\": {\n" +
            "    \"index.number_of_shards\": 3\n" +
            "  },\n" +
            "  \"mappings\": {\n" +
            "      \"properties\": {\n" +
            "        \"datetime\": {\n" +
            "          \"type\": \"date\",\n" +
            "          \"format\": \"" + format + "\"\n" +
            "        }\n" +
            "      }\n" +
            "  }\n" +
            "}"
        );
        return createTestIndex;
    }

    private Version getMinVersion() throws IOException {
        Version minNodeVersion = null;
        Map<?, ?> response = entityAsMap(client().performRequest(new Request("GET", "_nodes")));
        Map<?, ?> nodes = (Map<?, ?>) response.get("nodes");
        for (Map.Entry<?, ?> node : nodes.entrySet()) {
            Map<?, ?> nodeInfo = (Map<?, ?>) node.getValue();
            Version nodeVersion = Version.fromString(nodeInfo.get("version").toString());
            if (minNodeVersion == null) {
                minNodeVersion = nodeVersion;
            } else if (nodeVersion.before(minNodeVersion)) {
                minNodeVersion = nodeVersion;
            }
        }
        return minNodeVersion;
    }

    private void postNewDoc(String endpoint) throws IOException {
        Request putDoc = new Request("POST", endpoint);
        putDoc.setJsonEntity("{\n" +
            "  \"datetime\": \"2020-01-01T01:01:01+01:00\"\n" +
            "}"
        );
        Response resp = client().performRequest(putDoc);
        assertEquals(201, resp.getStatusLine().getStatusCode());
        client().performRequest(new Request("POST", "/_flush"));
    }

}
