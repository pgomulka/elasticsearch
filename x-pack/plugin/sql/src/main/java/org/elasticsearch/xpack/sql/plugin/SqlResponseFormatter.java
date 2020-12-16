/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.sql.plugin;

import org.elasticsearch.common.xcontent.MediaType;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.xpack.sql.action.SqlQueryResponse;

import java.io.IOException;

public interface SqlResponseFormatter {
    RestResponse getRestResponse(SqlQueryResponse response, RestRequest request, RestChannel channel, MediaType responseMediaType)
        throws IOException;
}
