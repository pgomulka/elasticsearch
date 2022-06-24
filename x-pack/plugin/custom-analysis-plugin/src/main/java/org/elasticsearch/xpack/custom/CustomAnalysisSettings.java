/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.custom;

import org.elasticsearch.common.settings.annotations.AnalysisSettings;
import org.elasticsearch.common.settings.annotations.LongSetting;

@AnalysisSettings(prefix="myplugin")//prefix not working yet
public interface CustomAnalysisSettings {


    @LongSetting(path = "myplugin.number_increase", defaultValue = 1L, max = 5678L)
    long getNumberIncrease();

}
