/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.sql.plugin;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.xcontent.MediaType;
import org.elasticsearch.common.xcontent.MediaTypeParser.ParsedMediaType;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestResponseListener;
import org.elasticsearch.xpack.sql.action.SqlQueryAction;
import org.elasticsearch.xpack.sql.action.SqlQueryRequest;
import org.elasticsearch.xpack.sql.action.SqlQueryResponse;
import org.elasticsearch.xpack.sql.proto.Mode;
import org.elasticsearch.xpack.sql.proto.Protocol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.xpack.sql.proto.Protocol.URL_PARAM_DELIMITER;
import static org.elasticsearch.xpack.sql.proto.Protocol.URL_PARAM_FORMAT;

public class RestSqlQueryAction extends BaseRestHandler {

    MediaType responseMediaType;

    @Override
    public List<Route> routes() {
        return List.of(
            new Route(GET, Protocol.SQL_QUERY_REST_ENDPOINT),
            new Route(POST, Protocol.SQL_QUERY_REST_ENDPOINT));
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client)
            throws IOException {
        SqlQueryRequest sqlRequest;
        try (XContentParser parser = request.contentOrSourceParamParser()) {
            sqlRequest = SqlQueryRequest.fromXContent(parser);
        }

        final MediaType localMediaType = getMediaType(request, sqlRequest);
        // this is a hack and we shouldn't rely on the class instance value asynchronously
        this.responseMediaType = localMediaType;

        long startNanos = System.nanoTime();
        return channel -> client.execute(SqlQueryAction.INSTANCE, sqlRequest, new RestResponseListener<SqlQueryResponse>(channel) {
            @Override
            public RestResponse buildResponse(SqlQueryResponse response) throws Exception {
                RestResponse restResponse;

                // XContent branch
                if (localMediaType instanceof XContentType) {
                    XContentType type = (XContentType) localMediaType;
                    XContentBuilder builder = channel.newBuilder(request.getXContentType(), type, true);
                    response.toXContent(builder, request);
                    restResponse = new BytesRestResponse(RestStatus.OK, builder);
                } else { // TextFormat
                    TextFormat type = (TextFormat) localMediaType;
                    final String data = type.format(request, response);

                    restResponse = new BytesRestResponse(RestStatus.OK, type.contentType(request),
                        data.getBytes(StandardCharsets.UTF_8));

                    if (response.hasCursor()) {
                        restResponse.addHeader("Cursor", response.cursor());
                    }
                }

                restResponse.addHeader("Took-nanos", Long.toString(System.nanoTime() - startNanos));
                return restResponse;
            }
        });
    }

    @Override
    protected Set<String> responseParams() {
        return responseMediaType == TextFormat.CSV ? Collections.singleton(URL_PARAM_DELIMITER) : Collections.emptySet();
    }

    /*
     * Since we support {@link TextFormat} <strong>and</strong>
     * {@link XContent} outputs we can't use {@link RestToXContentListener}
     * like everything else. We want to stick as closely as possible to
     * Elasticsearch's defaults though, while still layering in ways to
     * control the output more easily.
     *
     * First we find the string that the user used to specify the response
     * format. If there is a {@code format} parameter we use that. If there
     * isn't but there is a {@code Accept} header then we use that. If there
     * isn't then we use the {@code Content-Type} header which is required.
     */
    public MediaType getMediaType(RestRequest request, SqlQueryRequest sqlRequest) {
        if (Mode.isDedicatedClient(sqlRequest.requestInfo().mode())
            && (sqlRequest.binaryCommunication() == null || sqlRequest.binaryCommunication())) {
            // enforce CBOR response for drivers and CLI (unless instructed differently through the config param)
            return XContentType.CBOR;
        } else if (request.getFormatMediaType() != null) {
            return validateColumnarRequest(sqlRequest.columnar(), request.getFormatMediaType());
        }
        if (request.getAcceptMediaType() != null) {
            return validateColumnarRequest(sqlRequest.columnar(), request.getAcceptMediaType().getMediaType());
        }

        ParsedMediaType contentType = request.getContentType();
        assert contentType != null : "The Content-Type header is required";
        return validateColumnarRequest(sqlRequest.columnar(), contentType.getMediaType());
    }

    private static MediaType validateColumnarRequest(boolean requestIsColumnar, MediaType fromMediaType) {
        if (requestIsColumnar && fromMediaType instanceof TextFormat){
            throw new IllegalArgumentException("Invalid use of [columnar] argument: cannot be used in combination with "
                + "txt, csv or tsv formats");
        }
        return fromMediaType;
    }

    @Override
    public String getName() {
        return "sql_query";
    }
}
