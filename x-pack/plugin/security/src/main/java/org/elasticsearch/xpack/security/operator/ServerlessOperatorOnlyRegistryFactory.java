/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.operator;

import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.index.IndexSettings;

public class ServerlessOperatorOnlyRegistryFactory implements OperatorOnlyRegistryFactory{
    @Override
    public OperatorOnlyRegistry create(IndexScopedSettings indexSettings, SettingsFilter settingsFilter) {
        return new ServerlessOperatorOnlyRegistry(indexSettings,settingsFilter);
    }
}
