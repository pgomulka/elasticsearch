/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action.bulk;

import org.elasticsearch.action.support.WriteResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Map;

public class ShardPrepareCommitResponse extends ReplicationResponse implements WriteResponse {

    // initally, we could make do with just a boolean here, but in further iterations, some extra info could be useful.
    private final Map<TxID, Boolean> conflicts;

    public ShardPrepareCommitResponse(Map<TxID, Boolean> conflicts) {
        this.conflicts = conflicts;
    }

    public ShardPrepareCommitResponse(StreamInput in) throws IOException {
        super(in);
        this.conflicts = in.readMap(TxID::new, StreamInput::readBoolean);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeMap(conflicts, (o, k) -> k.writeTo(o), StreamOutput::writeBoolean);
    }

    /**
     * the conflict map, the boolean indicates true == this tx won it, false indicates that it lost, i.e., the tx in the map won.
     */
    public Map<TxID, Boolean> conflicts() {
        return conflicts;
    }

    @Override
    public void setForcedRefresh(boolean forcedRefresh) {
        // this does not refresh currently.
        throw new UnsupportedOperationException();
    }
}
