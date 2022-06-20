/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.custom;

import org.elasticsearch.common.settings.LongSetting;
import org.elasticsearch.common.settings.NodeSettings;

public class CustomSettings implements NodeSettings {

    @LongSetting(path = "decompound_max_cache_size", defaultValue = 123L, max = 5678L)
    long decompoundMaxCacheSize;


    public long getDecompoundMaxCacheSize() {
        return decompoundMaxCacheSize;
    }
}
