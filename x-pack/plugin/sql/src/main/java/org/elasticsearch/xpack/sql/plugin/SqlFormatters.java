/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.sql.plugin;

import org.elasticsearch.common.xcontent.MediaType;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestStatus;

import java.util.HashMap;
import java.util.Map;

public class SqlFormatters  {
    Map<MediaType, SqlResponseFormatter> formattersMap = new HashMap<>();

    public SqlFormatters() {
        for (XContentType value : XContentType.values()) {
            formattersMap.put(value, xcontentTypeFormatter());
        }
        formattersMap.put(TextMediaTypes.CSV, TextFormat.CSV);
        formattersMap.put(TextMediaTypes.VND_CSV, TextFormat.CSV);
        formattersMap.put(TextMediaTypes.PLAIN_TEXT, TextFormat.PLAIN_TEXT);
        formattersMap.put(TextMediaTypes.VND_PLAIN_TEXT, TextFormat.PLAIN_TEXT);
        formattersMap.put(TextMediaTypes.TSV, TextFormat.TSV);
        formattersMap.put(TextMediaTypes.VND_TSV, TextFormat.TSV);
    }

    public SqlResponseFormatter getFormatter(MediaType responseMediaType) {
        if(responseMediaType instanceof XContentType) {
            return xcontentTypeFormatter();
        }
        return formattersMap.get(responseMediaType);
    }

    private SqlResponseFormatter xcontentTypeFormatter() {
        return (response, request, channel, responseMediaType) -> {
            XContentType type = (XContentType) responseMediaType;
            XContentBuilder builder = channel.newBuilder(request.getXContentType(), type, true);
            response.toXContent(builder, request);
            return new BytesRestResponse(RestStatus.OK, builder);
        };
    }
}
