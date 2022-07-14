/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugin.analysis.nori;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.sp.api.analysis.settings.AnalysisSettings;
import org.elasticsearch.sp.api.analysis.settings.BooleanSetting;
import org.elasticsearch.sp.api.analysis.settings.ListSetting;
import org.elasticsearch.sp.api.analysis.settings.PathSetting;
import org.elasticsearch.sp.api.analysis.settings.StringSetting;

import java.nio.file.Path;
import java.util.List;

@AnalysisSettings(prefix = "")
public interface NoriAnalysisSettings {

    @ListSetting(path = "stoptags")
    List<String> getStopTags();

    @PathSetting(path = "stoptags_path", configRelative = true)
    Path getStopTagsPath();

    @StringSetting(path = "decompound_mode")
    String getDecompoundMode();

    @PathSetting(path = "user_dictionary")
    Path getUserDictionaryPath();

    @ListSetting(path = "user_dictionary_rules")
    List<String> getUserDictionaryRulesOption();

    @BooleanSetting(path = "discard_punctuation", defaultValue = true)
    Boolean isDiscardPunctuation();
}
