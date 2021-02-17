/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.compat;

public class CompatibleApiException extends RuntimeException {

    CompatibleApiException(String cause) {
        super(cause);
    }
}
