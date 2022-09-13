/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.analysis.common;

import org.elasticsearch.common.io.Streams;
import org.elasticsearch.plugin.analysis.api.CharFilterFactory;
import org.elasticsearch.plugin.api.NamedComponent;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

@NamedComponent(name = "xxx")
public class XX implements CharFilterFactory {
    @Override
    public Reader create(Reader reader) {
        try {
            return new StringReader(Streams.copyToString(reader) + "_stable");
        } catch (IOException e) {}
        return reader;
    }
}
