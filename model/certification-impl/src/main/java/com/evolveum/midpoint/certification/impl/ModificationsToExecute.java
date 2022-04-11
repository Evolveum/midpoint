/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.prism.delta.ItemDelta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Modifications to execute on an object (mostly on a campaign).
 *
 * Because there can be lots of modifications that could take literally hours to execute (sometimes blocking DB as described
 * e.g. in MID-4611), they are divided into smaller batches.
 */
public class ModificationsToExecute {

    private static final int BATCH_SIZE = 50;

    final List<List<ItemDelta<?, ?>>> batches = new ArrayList<>();

    private List<ItemDelta<?, ?>> getLastBatch() {
        return batches.get(batches.size() - 1);
    }

    public boolean isEmpty() {
        return batches.isEmpty();
    }

    public void add(ItemDelta<?, ?>... deltas) {
        add(Arrays.asList(deltas));
    }

    public void add(Collection<ItemDelta<?, ?>> deltas) {
        if (deltas.isEmpty()) {
            return;
        }
        if (isEmpty() || getLastBatch().size() + deltas.size() > BATCH_SIZE) {
            createNewBatch();
        }
        getLastBatch().addAll(deltas);
    }

    void createNewBatch() {
        batches.add(new ArrayList<>());
    }

    int getTotalDeltasCount() {
        int rv = 0;
        for (List<ItemDelta<?, ?>> batch : batches) {
            rv += batch.size();
        }
        return rv;
    }

    List<ItemDelta<?, ?>> getAllDeltas() {
        return batches.stream().flatMap(Collection::stream).collect(Collectors.toList());
    }
}
