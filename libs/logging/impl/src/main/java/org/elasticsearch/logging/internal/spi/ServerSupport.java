/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.logging.internal.spi;


public interface ServerSupport {

    /**
     * Return a tuple, where the first element is the node name, and second is the cluster Id (in string form).
     */
    Tuple<String, String> nodeAndClusterId();

    // Header Warning support
    void addHeaderWarning(String message, Object... params);

    // TODO: warning header from where, context? improve docs
    String getXOpaqueIdHeader();

    String getProductOriginHeader();

    String getTraceIdHeader();

    // settings

    String getClusterNameSettingValue();

    String getNodeNameSettingValue();

     class Tuple<V1, V2> {
        private final V1 v1;
        private final V2 v2;

        public Tuple(V1 v1, V2 v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
    }
}
