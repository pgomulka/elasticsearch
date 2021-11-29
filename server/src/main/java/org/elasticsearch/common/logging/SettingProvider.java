/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.logging;

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.plugins.Plugin;

import java.util.List;

public class SettingProvider  extends Plugin {
    public static final Setting<Boolean> ES_PRODUCT_ENABLED = Setting.boolSetting(
        "cluster.deprecation_indexing.es_product.enabled",
        false,
        Setting.Property.Final
    );

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(ES_PRODUCT_ENABLED);
    }
}
