/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.api.plugin.analysis;

import org.elasticsearch.api.plugin.Nameable;

/**
 * An analysis component used to create Analyzers
 */
public interface Analyzer<T extends org.apache.lucene.analysis.Analyzer> extends Nameable {
    /**
     * Returns a lucene org.apache.lucene.analysis.Analyzer instance
     */
    T create();

}
