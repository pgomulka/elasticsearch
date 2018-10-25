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

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.protocol.xpack.migration.IndexUpgradeInfoRequest;
import org.elasticsearch.protocol.xpack.migration.IndexUpgradeInfoResponse;
import org.elasticsearch.protocol.xpack.migration.IndexUpgradeRequest;
import org.elasticsearch.protocol.xpack.watcher.PutWatchRequest;
import org.elasticsearch.protocol.xpack.watcher.PutWatchResponse;

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class MigrationIT extends ESRestHighLevelClientTestCase {

    public void testGetAssistance() throws IOException {
        RestHighLevelClient client = highLevelClient();
        {
            IndexUpgradeInfoResponse response = client.migration().getAssistance(new IndexUpgradeInfoRequest(), RequestOptions.DEFAULT);
            assertEquals(0, response.getActions().size());
        }
        {
            client.indices().create(new CreateIndexRequest("test"), RequestOptions.DEFAULT);
            IndexUpgradeInfoResponse response = client.migration().getAssistance(
                new IndexUpgradeInfoRequest("test"), RequestOptions.DEFAULT);
            assertEquals(0, response.getActions().size());
        }
    }

    public void testGetAssistanceFor() throws IOException {
            createWatch();
            IndexUpgradeInfoResponse response = highLevelClient().migration().getAssistance(new IndexUpgradeInfoRequest(), RequestOptions.DEFAULT);
            assertEquals(1, response.getActions().size());
    }


    public void testUpgrade() throws IOException {
        createWatch();

        BulkByScrollResponse resposne = highLevelClient().migration().upgrade(
            new IndexUpgradeRequest(".watches"), RequestOptions.DEFAULT);

        assertThat(resposne.getCreated(), equalTo(1L));
    }


    private PutWatchResponse createWatch() throws IOException {
        String json = "{ \n" +
            "  \"trigger\": { \"schedule\": { \"interval\": \"10h\" } },\n" +
            "  \"input\": { \"none\": {} },\n" +
            "  \"actions\": { \"logme\": { \"logging\": { \"text\": \"{{ctx.payload}}\" } } }\n" +
            "}";
        BytesReference bytesReference = new BytesArray(json);
        String watchId = randomAlphaOfLength(10);

        PutWatchRequest putWatchRequest = new PutWatchRequest(watchId, bytesReference, XContentType.JSON);
        PutWatchResponse putWatchResponse = highLevelClient().watcher().putWatch(putWatchRequest, RequestOptions.DEFAULT);
        assertThat(putWatchResponse.isCreated(), is(true));
        return putWatchResponse;
    }

}
