/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.indices.analysis.wrappers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

import java.lang.reflect.Proxy;

public class SettingsProxy {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T create(Settings settings, Class<T> parameterType, Environment environment) {
        return (T) Proxy.newProxyInstance(
            parameterType.getClassLoader(),
            new Class[] { parameterType },
            new SettingsInvocationHandler(settings, environment)
        );
    }
}
