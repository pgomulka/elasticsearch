package org.elasticsearch.rest;

import org.apache.logging.log4j.LogManager;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.rest.action.document.RestDeleteAction;

public class TypeWarnings {
    public static final String TYPES_DEPRECATION_MESSAGE = "[types removal] Specifying types in document " +
        "index requests is deprecated, use the typeless endpoints instead (/{index}/_doc/{id}, /{index}/_doc, " +
        "or /{index}/_create/{id}).";
    private static final DeprecationLogger deprecationLogger = new DeprecationLogger(
        LogManager.getLogger(RestDeleteAction.class));

    public static Runnable typesRemovalDeprecation() {
        return () -> deprecationLogger.deprecatedAndMaybeLog("index_with_types", TYPES_DEPRECATION_MESSAGE);
    }


}
