/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action.bulk;

import org.elasticsearch.action.ActionType;

public class ShardPrepareCommitAction extends ActionType<ShardPrepareCommitResponse> {

    public static final ShardPrepareCommitAction INSTANCE = new ShardPrepareCommitAction();
    // todo: should the name be bulk[prepare] style?
    public static final String NAME = "indices:data/write/prepare";

    private ShardPrepareCommitAction() {
        super(NAME, ShardPrepareCommitResponse::new);
    }

    // todo: transport options?
}
