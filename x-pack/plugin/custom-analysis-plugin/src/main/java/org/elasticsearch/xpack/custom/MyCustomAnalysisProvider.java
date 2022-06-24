/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.custom;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.annotations.SettingsFactory;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.CharFilterFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;

import java.io.IOException;
import java.io.Reader;


public class MyCustomAnalysisProvider<T> implements AnalysisModule.AnalysisProvider<CharFilterFactory> {


    @Override
    public CharFilterFactory get(SettingsFactory analysisSettingsFactory, SettingsFactory indexSettingsFactory,  SettingsFactory clusterStateSettings) throws IOException {
        CustomAnalysisSettings analysisSettings = analysisSettingsFactory.create(CustomAnalysisSettings.class);
        CustomIndexSettings customIndexSettings = indexSettingsFactory.create(CustomIndexSettings.class);
        CustomClusterSettings customClusterSettings = clusterStateSettings.create(CustomClusterSettings.class);

        System.out.println("getIndexQueryLenient" +customIndexSettings.getIndexQueryLenient());;
        return new CharFilterFactory() {
            @Override
            public String name() {
                return "custom_replace";
            }

            @Override
            public Reader create(Reader reader) {


                return new CustomAnalysisCharFilter(analysisSettings, customClusterSettings, reader);
            }
        };
    }

    @Override
    public CharFilterFactory get(IndexSettings indexSettings, Environment environment, String name, Settings settings) throws IOException {
        return null;
    }
}
