/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.logging;

public class LogManager {

    public static Logger getLogger(final String name) {
        return null;
    }

    public static Logger getLogger(final Class<?> clazz) {
        return null;
    }

    private LogManager() {}

    public static Logger getLogger() {
        return null;
    }

    public static Logger getRootLogger() {
        return getLogger("");
    }

    //getRootLogger do we want it?
}
