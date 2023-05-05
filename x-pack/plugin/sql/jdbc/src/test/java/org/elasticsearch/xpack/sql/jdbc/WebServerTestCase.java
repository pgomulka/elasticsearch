/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.sql.jdbc;

import org.elasticsearch.Build;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.http.MockWebServer;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.ToXContentObject;
import org.junit.After;
import org.junit.Before;

import java.util.Date;

/**
 * Base class for unit tests that need a web server for basic tests.
 */
public abstract class WebServerTestCase extends ESTestCase {

    private MockWebServer webServer = new MockWebServer();

    @Before
    public void init() throws Exception {
        webServer.start();
    }

    @After
    public void cleanup() {
        webServer.close();
    }

    public MockWebServer webServer() {
        return webServer;
    }

    ToXContent createCurrentVersionMainResponse() {
        return createMainResponse(Version.CURRENT);
    }

    ToXContentObject createMainResponse(Version version) {
        String clusterUuid = randomAlphaOfLength(10);
        ClusterName clusterName = new ClusterName(randomAlphaOfLength(10));
        String nodeName = randomAlphaOfLength(10);
        final String date = new Date(randomNonNegativeLong()).toString();
        Build build = new Build(Build.Type.UNKNOWN, randomAlphaOfLength(8), date, randomBoolean(), version.toString());

        return (builder, p) -> {
            builder.startObject();
            builder.field("name", nodeName);
            builder.field("cluster_name", clusterName.value());
            builder.field("cluster_uuid", clusterUuid);
            builder.startObject("version")
                .field("number", build.qualifiedVersion())
                .field("build_flavor", "default")
                .field("build_type", build.type().displayName())
                .field("build_hash", build.hash())
                .field("build_date", build.date())
                .field("build_snapshot", build.isSnapshot())
                .field("lucene_version", version.luceneVersion.toString())
                .field("minimum_wire_compatibility_version", version.minimumCompatibilityVersion().toString())
                .field("minimum_index_compatibility_version", version.minimumIndexCompatibilityVersion().toString())
                .endObject();
            builder.field("tagline", "You Know, for Search");
            builder.endObject();
            return builder;
        };
    }

    String webServerAddress() {
        return webServer.getHostName() + ":" + webServer.getPort();
    }
}
