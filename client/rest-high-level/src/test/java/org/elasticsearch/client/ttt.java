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

package org.elasticsearch.client;

import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.tasks.TaskSubmissionResponse;
import org.elasticsearch.common.CheckedRunnable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;


public class ttt {


    public static void main(String[] args) throws IOException, InterruptedException {

        final String sourceIndex = "source3422315";
        final String destinationIndex = "dest3224153";
        // Prepare
        Settings settings = Settings.builder()
                                    .put("number_of_shards", 1)
                                    .put("number_of_replicas", 0)
                                    .build();

        CreateIndexRequest createIndexRequest = new CreateIndexRequest(sourceIndex);
        Alias alias = new Alias("alias_name");
        createIndexRequest.alias(alias);
        createIndexRequest.settings(settings);
        final RestHighLevelClient restHighLevelClient = highLevelClient();
        CreateIndexResponse createIndexResponse = restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);


        CreateIndexRequest createIndexRequest2 = new CreateIndexRequest(destinationIndex);
        createIndexRequest.settings(settings);
        CreateIndexResponse createIndexResponse2 = restHighLevelClient.indices().create(createIndexRequest2, RequestOptions.DEFAULT);


        BulkRequest bulkRequest = new BulkRequest()
            .add(new IndexRequest(sourceIndex, "type", "1").source(Collections.singletonMap("foo", "bar"), XContentType.JSON))
            .add(new IndexRequest(sourceIndex, "type", "2").source(Collections.singletonMap("foo2", "bar2"), XContentType.JSON))
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        restHighLevelClient.bulk(bulkRequest,RequestOptions.DEFAULT);

        ReindexRequest reindexRequest = new ReindexRequest();
        reindexRequest.setSourceIndices(sourceIndex);
        reindexRequest.setDestIndex(destinationIndex);
        CountDownLatch latch = new CountDownLatch(1);
        restHighLevelClient.reindexAsync(reindexRequest, RequestOptions.DEFAULT, new ActionListener<BulkByScrollResponse>() {
            @Override
            public void onResponse(BulkByScrollResponse bulkByScrollResponse) {
                GetAliasesRequest request = new GetAliasesRequest("alias_name");
                try {
                    GetAliasesResponse alias = restHighLevelClient.indices().getAlias(request, RequestOptions.DEFAULT);
                    System.out.println(alias.getAliases());
                    latch.countDown();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                throw new RuntimeException(e);
            }
        });
        latch.await();


    }

    private static RestHighLevelClient highLevelClient() {
        RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(
                new HttpHost("localhost", 9200, "http"),
                new HttpHost("localhost", 9201, "http")));
        return client;
    }

}
