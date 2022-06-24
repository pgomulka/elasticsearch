/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.custom;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.annotations.AnalysisSettings;
import org.elasticsearch.common.settings.annotations.AnalysisSettingsFactory;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.CharFilterFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;

import java.io.IOException;
import java.io.Reader;


public class MyCustomAnalysisProvider<T> implements AnalysisModule.AnalysisProvider<CharFilterFactory> {



    public CharFilterFactory get(AnalysisSettingsFactory analysisSettingsFactory/*, CustomNodeSettings nodeSettings*/) throws IOException {
        CustomAnalysisSettings analysisSettings = analysisSettingsFactory.create(CustomAnalysisSettings.class);

        return new CharFilterFactory() {
            @Override
            public String name() {
                return "custom_replace";
            }

            @Override
            public Reader create(Reader reader) {


                return new CustomAnalysisCharFilter(analysisSettings, reader);
            }
        };
    }

    @Override
    public CharFilterFactory get(IndexSettings indexSettings, Environment environment, String name, Settings settings) throws IOException {
        return null;
    }
}
