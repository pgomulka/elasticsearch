package org.elasticsearch.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.Version;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.index.mapper.MapperService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class CompatibleHandlers {
    private static final Logger logger = LogManager.getLogger(CompatibleHandlers.class);

    public static Consumer<RestRequest> consumeParameterIncludeType(DeprecationLogger deprecationLogger) {
        final String TYPES_DEPRECATION_MESSAGE = "[types removal] Using include_type_name in create " +
            "index requests is deprecated. The parameter will be removed in the next major version.";

        return r -> {
            if(r.hasParam(BaseRestHandler.INCLUDE_TYPE_NAME_PARAMETER)){
                deprecationLogger.deprecatedAndMaybeLog("create_index_with_types", TYPES_DEPRECATION_MESSAGE);
                r.param(BaseRestHandler.INCLUDE_TYPE_NAME_PARAMETER);
            }
        };
    }

    public static Consumer<RestRequest> consumeParameterType(DeprecationLogger deprecationLogger) {
        String TYPES_DEPRECATION_MESSAGE = "[types removal] Using type as a path parameter is deprecated.";

        return r -> {
            deprecationLogger.deprecatedAndMaybeLog("create_index_with_types", TYPES_DEPRECATION_MESSAGE);
            r.param("type");
        };
    }

    public static boolean isCompatible(ToXContent.Params params) {
        String param = params.param(Version.COMPATIBLE_PARAMS_KEY);
        return Version.COMPATIBLE_VERSION.equals(param);
    }

    public static Map<String,Object> replaceTypeWithDoc(Map<String,Object> mappings){
        Map<String, Object> newSource = new HashMap<>();

        String typeName = mappings.keySet().iterator().next();
        @SuppressWarnings("unchecked")
        Map<String, Object> typedMappings = (Map<String, Object>) mappings.get(typeName);

        newSource.put("mappings", Collections.singletonMap(MapperService.SINGLE_MAPPING_NAME, typedMappings));
        return typedMappings;
    }

}
