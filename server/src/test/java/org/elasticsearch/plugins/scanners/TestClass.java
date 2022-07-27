/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugins.scanners;

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.sp.api.analysis.TokenFilterFactory;
import org.elasticsearch.sp.api.analysis.annotations.Factory;

@Factory(name = "TestClass")
public class TestClass implements TokenFilterFactory {

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return null;
    }
}
