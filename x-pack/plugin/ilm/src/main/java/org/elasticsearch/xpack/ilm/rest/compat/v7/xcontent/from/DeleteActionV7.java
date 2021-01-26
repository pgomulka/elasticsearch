/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ilm.rest.compat.v7.xcontent.from;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.core.ilm.DeleteAction;

public class DeleteActionV7 {
    public static final ParseField DELETE_SEARCHABLE_SNAPSHOT_FIELD = new ParseField("delete_searchable_snapshot");

    private static final ConstructingObjectParser<DeleteAction, Void> PARSER = new ConstructingObjectParser<>("v7DeleteAction",
        a -> new DeleteAction(a[0] == null ? true : (boolean) a[0]));

    static {
        PARSER.declareBoolean(ConstructingObjectParser.optionalConstructorArg(), DELETE_SEARCHABLE_SNAPSHOT_FIELD);
    }

    public static DeleteAction parse(XContentParser parser) {
        return PARSER.apply(parser, null);
    }
}
