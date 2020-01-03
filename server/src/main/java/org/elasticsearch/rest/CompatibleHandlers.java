package org.elasticsearch.rest;

import org.apache.logging.log4j.LogManager;
import org.elasticsearch.Version;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.xcontent.LoggingDeprecationHandler;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.rest.action.admin.indices.RestCreateIndexAction;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class CompatibleHandlers {
    public static UnaryOperator<RestHandler> compatibleHandlerWrapper(List<Consumer<RestRequest>> parameterConsumers){
        UnaryOperator<RestHandler> COMPATIBLE_HANDLER_WRAPPER = handler ->
            new RestHandler() {
                @Override
                public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
                    if (isRequestCompatible(request)) {
                        request.params().put(Version.COMPATIBLE_HEADER, request.header(Version.COMPATIBLE_HEADER));
                        request.param(Version.COMPATIBLE_HEADER);

                        //consume type field even though not used
                        parameterConsumers.forEach(c -> c.accept(request));
                        request.param("type");
                    }
                    handler.handleRequest(request, channel, client);

                }

                @Override
                public boolean isCompatible() {
                    return true;
                }
            };

        return COMPATIBLE_HANDLER_WRAPPER;
    }

    public static UnaryOperator<RestHandler> compatibleParameterConsumingHandler(List<Function<RestRequest,RestRequest>> parameterConsumers){
        UnaryOperator<RestHandler> COMPATIBLE_HANDLER_WRAPPER = handler ->
            new RestHandler() {
                @Override
                public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
                    if (isRequestCompatible(request)) {

                        //consume path a params
                        parameterConsumers.forEach(c -> c.apply(request));
                    }

                    handler.handleRequest(request, channel, client);
                }

                @Override
                public boolean isCompatible() {
                    return false;
                }
            };

        return COMPATIBLE_HANDLER_WRAPPER;
    }

    public static boolean isRequestCompatible(RestRequest request) {
        return Version.COMPATIBLE_VERSION.equals(request.header(Version.COMPATIBLE_HEADER));
    }

    public static Function<RestRequest,RestRequest> consumeParameterIncludeType() {
        return r -> {
            r.param("include_type_name");
            return r;
        };
    }

}
