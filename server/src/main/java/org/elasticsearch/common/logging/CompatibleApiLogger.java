/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A logger that logs deprecation notices.
 */
public class CompatibleApiLogger {

    private final ThrottlingLogger throttlingLogger;
    private final HeaderWarningLogger headerWarningLogger = new HeaderWarningLogger();

    /**
     * Creates a new deprecation logger based on the parent logger. Automatically
     * prefixes the logger name with "deprecation", if it starts with "org.elasticsearch.",
     * it replaces "org.elasticsearch" with "org.elasticsearch.deprecation" to maintain
     * the "org.elasticsearch" namespace.
     */
    public CompatibleApiLogger(Logger parentLogger) {
        String name = parentLogger.getName();
        if (name.startsWith("org.elasticsearch")) {
            name = name.replace("org.elasticsearch.", "org.elasticsearch.compatible.");
        } else {
            name = "compatible." + name;
        }
        this.throttlingLogger = new ThrottlingLogger(LogManager.getLogger(name));
    }

    /**
     * Logs a deprecation message, adding a formatted warning message as a response header on the thread context.
     */
    public void deprecated(String msg, Object... params) {
        headerWarningLogger.log(msg, params);
        throttlingLogger.log(msg, params);
    }

    /**
     * Adds a formatted warning message as a response header on the thread context, and logs a deprecation message if the associated key has
     * not recently been seen.
     *
     * @param key    the key used to determine if this deprecation should be logged
     * @param msg    the message to log
     * @param params parameters to the message
     */
    public void deprecatedAndMaybeLog(final String key, final String msg, final Object... params) {
        headerWarningLogger.log(msg, params);
        throttlingLogger.throttleLog(key, msg, params);
    }
}
