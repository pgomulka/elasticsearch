/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.shard;

import org.elasticsearch.action.bulk.TxID;
import org.elasticsearch.test.ESTestCase;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.equalTo;

public class ShardTransactionRegistryTests extends ESTestCase {
    private final ShardTransactionRegistry registry = new ShardTransactionRegistry();

    public void testRegisterAndUnregister() {
        Set<String> ids1 = ids(0, 100);
        Set<String> ids2 = ids(between(0, 199), 200);

        TxID txID1 = TxID.create();
        registry.registerTransaction(txID1, ids1);
        TxID txID2 = TxID.create();
        registry.registerTransaction(txID2, ids2);

        assertThat(registry.keys(txID1), equalTo(ids1));
        assertThat(registry.keys(txID2), equalTo(ids2));
        assertThat(registry.size(), equalTo(2));
        // invariant assertions are key part of the test.

        registry.releaseTransaction(txID1);
        assertThat(registry.keys(txID2), equalTo(ids2));
        assertThat(registry.size(), equalTo(1));

        registry.releaseTransaction(txID2);
        assertThat(registry.size(), equalTo(0));
    }

    private Set<String> ids(int startInclusive, int endExclusive) {
        return IntStream.range(startInclusive, endExclusive).mapToObj(String::valueOf).collect(Collectors.toSet());
    }
}
