/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.settings;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.xcontent.ToXContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.elasticsearch.common.settings.DefaultSettingsFilter.SETTINGS_FILTER_PARAM;

public interface SettingsFilter {
    /**
     * Returns <code>true</code> iff the given string is either a valid settings key pattern or a simple regular expression
     *
     * @see Regex
     * @see AbstractScopedSettings#isValidKey(String)
     */
    static boolean isValidPattern(String pattern) {
        return AbstractScopedSettings.isValidKey(pattern) || Regex.isSimpleMatchPattern(pattern);
    }

    static Settings filterSettings(ToXContent.Params params, Settings settings) {
        String patterns = params.param(SETTINGS_FILTER_PARAM);
        final Settings filteredSettings;
        if (patterns != null && patterns.isEmpty() == false) {
            filteredSettings = filterSettings(Strings.commaDelimitedListToSet(patterns), settings);
        } else {
            filteredSettings = settings;
        }
        return filteredSettings;
    }

    static Settings filterSettings(Iterable<String> patterns, Settings settings) {
        Settings.Builder builder = Settings.builder().put(settings);
        List<String> simpleMatchPatternList = new ArrayList<>();
        for (String pattern : patterns) {
            if (Regex.isSimpleMatchPattern(pattern)) {
                simpleMatchPatternList.add(pattern);
            } else {
                builder.remove(pattern);
            }
        }
        if (simpleMatchPatternList.isEmpty() == false) {
            String[] simpleMatchPatterns = simpleMatchPatternList.toArray(String[]::new);
            builder.keys().removeIf(key -> Regex.simpleMatch(simpleMatchPatterns, key));
        }
        return builder.build();
    }

    default void addFilterSettingParams(RestRequest request) {
        if (getPatterns().isEmpty() == false) {
            request.params().put(SETTINGS_FILTER_PARAM, getPatternString());
        }
    }

    default String getPatternString() {
       return Strings.collectionToDelimitedString(getPatterns(), ",");
    }

    Set<String> getPatterns();

    Settings filter(Settings settings);

    void validateSettings(Settings settings);
}
