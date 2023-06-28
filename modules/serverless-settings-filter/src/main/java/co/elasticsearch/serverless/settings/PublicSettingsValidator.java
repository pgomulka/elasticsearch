/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package co.elasticsearch.serverless.settings;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;

import java.util.List;

public class PublicSettingsValidator {

    // serverless dependency on xpack-core
    public static final String PRIVILEGE_CATEGORY_VALUE_OPERATOR = "operator";
    public static final String PRIVILEGE_CATEGORY_KEY = "_security_privilege_category";

    private final ThreadContext threadContext;
    private IndexScopedSettings indexScopedSettings;

    public PublicSettingsValidator(ThreadContext threadContext, IndexScopedSettings indexScopedSettings) {
        this.threadContext = threadContext;
        this.indexScopedSettings = indexScopedSettings;
    }

    public void validateSettings(Settings settings) {
        if (false == PRIVILEGE_CATEGORY_VALUE_OPERATOR.equals(threadContext.getHeader(PRIVILEGE_CATEGORY_KEY))) {
            List<String> list = settings.keySet()
                .stream()
                .filter(settingName -> indexScopedSettings.get(settingName).isPublicServerless() == false)
                .toList();
            if (false == list.isEmpty()) {
                throw new IllegalArgumentException(
                    "unknown "
                        + (list.size() == 1 ? "setting" : "settings")
                        + " ["
                        + Strings.collectionToDelimitedString(list, ",")
                        + "]"
                        + " within non-operator mode"
                );
            }
        }

    }

}
