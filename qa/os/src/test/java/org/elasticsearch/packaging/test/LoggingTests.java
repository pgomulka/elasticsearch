/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.packaging.test;

import org.apache.http.client.fluent.Request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.elasticsearch.packaging.util.FileUtils.fileWithGlobExist;
import static org.elasticsearch.packaging.util.ServerUtils.makeRequest;

public class LoggingTests extends PackagingTestCase {

    public void test10ESLogsExist() throws Exception {
        install();
        startElasticsearch();
        String clusterName = clusterName();

        assertThat(installation.logs, fileWithGlobExist(clusterName + "_server.log"));
        assertThat(installation.logs, fileWithGlobExist(clusterName + "_server.json"));
        assertThat(installation.logs, fileWithGlobExist(clusterName + "_deprecation.json"));
        assertThat(installation.logs, fileWithGlobExist(clusterName + "_index_indexing_slow_log.json"));
        assertThat(installation.logs, fileWithGlobExist(clusterName + "_index_search_slow_log.json"));
        stopElasticsearch();
    }

    public void test11DeprecationLogs() throws Exception {
        install();
        startElasticsearch();
        String clusterName = clusterName();

        assertThat(installation.logs, fileWithGlobExist(clusterName + "_server.log"));
        assertThat(installation.logs, fileWithGlobExist(clusterName + "_server.json"));
        assertThat(installation.logs, fileWithGlobExist(clusterName + "_deprecation.json"));
        assertThat(installation.logs, fileWithGlobExist(clusterName + "_index_indexing_slow_log.json"));
        assertThat(installation.logs, fileWithGlobExist(clusterName + "_index_search_slow_log.json"));
        stopElasticsearch();
    }

    private String clusterName() throws Exception {
        String clusterInfo = makeRequest(Request.Get("http://localhost:9200/"));
        Pattern p = Pattern.compile("\"cluster_name\": \"(.*)\"");
        Matcher matcher = p.matcher(clusterInfo);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("unable to parse cluster name");
    }
}
