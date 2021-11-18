/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.shard;

import org.elasticsearch.action.bulk.TxID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ShardTransactionRegistry {
    private final Map<String, Set<TxID>> byKey = new HashMap<>();
    private final Map<TxID, Set<String>> byTxID = new HashMap<>();
    private final Map<TxID, Set<String>> conflictingKeys = new HashMap<>();

    // todo: less locking and perhaps totally different content...
    public synchronized void registerTransaction(TxID txID, Set<String> ids) {
        Set<String> previous = byTxID.put(txID, ids);
        assert previous == null;
        for (String id : ids) {
            Set<TxID> txIDSet = byKey.computeIfAbsent(id, k -> new HashSet<>());
            txIDSet.add(txID);
            if (txIDSet.size() > 1) {
                txIDSet.forEach(conflict -> conflictingKeys.computeIfAbsent(conflict, k-> new HashSet<>()).add(id));
            }
        }
    }

    public synchronized void releaseTransaction(TxID txID) {
        byTxID.get(txID).forEach(id -> cleanByKey(id, txID));
    }

    public synchronized boolean prepare(TxID txID) {
        // todo: detect conflicts.
        assert byTxID.containsKey(txID);
        return true;
    }

    private void cleanByKey(String id, TxID txID) {
        assert Thread.holdsLock(this);
        Set<TxID> txIDs = byKey.get(id);
        txIDs.remove(txID);
        if (txIDs.isEmpty()) {
            byKey.remove(id);
        } else if (txIDs.size() == 1) {
            conflictingKeys.remove(id);
        }
    }

}
