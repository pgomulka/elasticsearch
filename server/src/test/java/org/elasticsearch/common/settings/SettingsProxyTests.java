/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.settings;

import org.elasticsearch.common.settings.annotations.AnalysisSettings;
import org.elasticsearch.common.settings.annotations.LongSetting;
import org.elasticsearch.common.settings.annotations.SettingsProxy;
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.equalTo;

public class SettingsProxyTests extends ESTestCase {

    public void testDefaultValue(){
        @AnalysisSettings(prefix = "prefix")
        interface MyCustomSetting {
            @LongSetting(path = "path", defaultValue = 0L, max = 0L)
            Long getSetting();
        }
        MyCustomSetting setting = SettingsProxy.create(Settings.EMPTY, MyCustomSetting.class);

        Long value = setting.getSetting();
        assertThat(value, equalTo(0L));
    }

    public void testBasic(){
        Settings settings = Settings.builder()
            .put("prefix.path", 1L)
            .build();

        @AnalysisSettings(prefix = "prefix")
        interface MyCustomSetting {
            @LongSetting(path = "path", defaultValue = 0L, max = 0L)
            Long getSetting();
        }
        MyCustomSetting setting = SettingsProxy.create(settings, MyCustomSetting.class);

        Long value = setting.getSetting();
        assertThat(value, equalTo(1L));
    }
}
