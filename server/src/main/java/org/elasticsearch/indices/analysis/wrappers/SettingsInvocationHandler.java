/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.indices.analysis.wrappers;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.analysis.Analysis;
import org.elasticsearch.plugin.api.settings.BooleanSetting;
import org.elasticsearch.plugin.api.settings.LongSetting;
import org.elasticsearch.plugin.api.settings.ListSetting;
import org.elasticsearch.plugin.api.settings.Parser;
import org.elasticsearch.plugin.api.settings.StringSetting;
import org.elasticsearch.plugin.api.settings.TypeSetting;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.function.Function;

public class SettingsInvocationHandler implements InvocationHandler {

    private static Logger LOGGER = LogManager.getLogger(SettingsInvocationHandler.class);
    private String prefix = "";
    private Settings settings;
    private Environment environment;

    public SettingsInvocationHandler(Settings settings, Environment environment) {
        this.settings = settings;
        this.environment = environment;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // LongSetting annotation = method.getAnnotation(LongSetting.class);
        // assert method.getAnnotations().length == 1;
        Annotation annotation = method.getAnnotations()[0];
        if (annotation instanceof LongSetting) {
            LongSetting setting = (LongSetting) annotation;
            Long value = getValue(Long::valueOf, setting.path(), setting.defaultValue(), setting.max());
            return value ;
        } else if (annotation instanceof BooleanSetting) {
            BooleanSetting setting = (BooleanSetting) annotation;
            return getValue(Boolean::valueOf, setting.path(), setting.defaultValue());
        } else if (annotation instanceof StringSetting) {
            StringSetting setting = (StringSetting) annotation;
            return getValue(String::valueOf, setting.path(), null);
        } else if (annotation instanceof TypeSetting) {
            TypeSetting setting = (TypeSetting) annotation;
            String value = getValue(String::valueOf, setting.path(), null);
            Constructor<? extends Parser<?>> constructor = setting.parser().getConstructor();
            Parser<?> parser = constructor.newInstance();
            return parser.parse(value);
        } else if (annotation instanceof ListSetting) {
            ListSetting setting = (ListSetting) annotation;
//            if(setting.fallbackToPathOption()!=""){
//                if(setting.exclusiveWithFallbackPath()) {
//                    String optionListSetting = settings.get(setting.path());
//                    String pathSetting = settings.get(setting.fallbackToPathOption());
//
//                    if (setting.exclusiveWithFallbackPath() && optionListSetting != null && pathSetting != "") {
//                        throw new IllegalArgumentException(
//                            "It is not allowed to use [" + setting.path() + "] in conjunction with [" + setting.fallbackToPathOption() + "]"
//                        );
//                    }
//                }
//                return Analysis.getWordList(environment, settings, setting.fallbackToPathOption(), setting.path(),true);
//            }
            return settings.getAsList(setting.path(), Collections.emptyList());
      /*  } else if (annotation instanceof PathSetting) {
            PathSetting setting = (PathSetting) annotation;
            String path = settings.get(setting.path());
            if (path == null) {
                return null;
            }
            if (setting.configRelative()) {
                return environment.configFile().resolve(path);
            }
            return environment.pluginsFile().resolve(path);
        }*/} else {
            throw new IllegalArgumentException();
        }

    }

    private <T extends Comparable<T>> T getValue(Function<String, T> parser, String path, T defaultValue, T max) {
        T value = getValue(parser, path, defaultValue);
        if (value.compareTo(max) > 0) {
            throw new IllegalArgumentException("Setting value " + value + "is greater than max " + max);
        }
        return value;
    }

    private <T> T getValue(Function<String, T> parser, String path, T defaultValue) {
        String key = path;
        if (settings.get(key) != null) {
            return parser.apply(settings.get(key));
        }
        return defaultValue;
    }

}
