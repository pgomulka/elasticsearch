/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.sql.plugin;

import junit.framework.TestCase;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.rest.FakeRestRequest;

import static java.util.Collections.singletonMap;
import static org.elasticsearch.xpack.sql.plugin.TextFormat.CSV;
import static org.elasticsearch.xpack.sql.plugin.TextFormat.TSV;

public class TextMediaTypesTests extends ESTestCase {

    public void testCsvContentType() {
        assertEquals("text/csv; charset=utf-8; header=present", TextMediaTypes.CSV.responseContentType(req()));
    }

    public void testCsvContentTypeWithoutHeader() {
        assertEquals("text/csv; charset=utf-8; header=absent", TextMediaTypes.CSV.responseContentType(reqWithParam("header", "absent")));
    }

    public void testTsvContentType() {
        assertEquals("text/tab-separated-values; charset=utf-8", TextMediaTypes.TSV.responseContentType(req()));
    }
    private static RestRequest req() {
        return new FakeRestRequest();
    }

    private static RestRequest reqWithParam(String paramName, String paramVal) {
        return new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY).withParams(singletonMap(paramName, paramVal)).build();
    }
}
