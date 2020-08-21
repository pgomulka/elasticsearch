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

package org.elasticsearch.rest;

import org.elasticsearch.Version;
import org.elasticsearch.common.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulate multiple handlers for the same path, allowing different handlers for different HTTP verbs.
 */
final class MethodHandlers {

    private final String path;
    private final Map<RestRequest.Method, Map<Version, RestHandler>> methodHandlers;

    MethodHandlers(String path, RestHandler handler, Version version, RestRequest.Method... methods) {
        this.path = path;
        this.methodHandlers = new HashMap<>(methods.length);
        for (RestRequest.Method method : methods) {
            methodHandlers.computeIfAbsent(method, k -> new HashMap<>())
                .put(version, handler);
        }
    }

    /**
     * Add a handler for an additional array of methods. Note that {@code MethodHandlers}
     * does not allow replacing the handler for an already existing method.
     */
    MethodHandlers addMethods(RestHandler handler, Version version, RestRequest.Method... methods) {
        for (RestRequest.Method method : methods) {
            RestHandler existing = methodHandlers.computeIfAbsent(method, k -> new HashMap<>())
                .putIfAbsent(version, handler);
            if (existing != null) {
                throw new IllegalArgumentException("Cannot replace existing handler for [" + path + "] for method: " + method);
            }
        }
        return this;
    }

    /**
     * Return a handler for a given method and a version
     * If a handler for a given version is not found, the handler for Version.CURRENT is returned.
     * We only expect Version.CURRENT or Version.CURRENT -1. This is validated.
     *
     * Handlers can be registered under the same path and method, but will require to have different versions (CURRENT or CURRENT-1)
     *
     * //todo What if a handler was added in V8 but was not present in V7?
     *
     * @param method a REST method under which a handler was registered
     * @param version a Version under which a handler was registered
     * @return a handler
     */
    @Nullable
    RestHandler getHandler(RestRequest.Method method, Version version) {
        Map<Version, RestHandler> versionToHandlers = methodHandlers.get(method);
        if (versionToHandlers == null) {
            return null; //method not found
        }
        final RestHandler handler = versionToHandlers.get(version);
        return handler != null || version.equals(Version.CURRENT) ? handler : versionToHandlers.get(Version.CURRENT);
    }

    /**
     * Return a set of all valid HTTP methods for the particular path
     */
    Set<RestRequest.Method> getValidMethods() {
        return methodHandlers.keySet();
    }
}
