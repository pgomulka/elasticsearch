/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.settings.annotations;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Settings;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

public class SettingsProxy {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T create(Settings settings, Class<T> parameterType) {
//        AnalysisSettings annotation = (AnalysisSettings) parameterType.getAnnotations()[0];
//        String prefix = annotation.prefix();
        return (T) Proxy.newProxyInstance(
            parameterType.getClassLoader(),
            new Class[]{parameterType},
            new DynamicInvocationHandler(settings));
    }

}

