/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.custom;

import org.elasticsearch.common.settings.annotations.BooleanSetting;
import org.elasticsearch.common.settings.annotations.ClusterSettings;
import org.elasticsearch.common.settings.annotations.LongSetting;

@ClusterSettings
public interface CustomClusterSettings {
//registration of new settings
    @BooleanSetting(path = "cluster.deprecation_indexing.enabled", defaultValue = false)
    boolean getSuffix();

}
