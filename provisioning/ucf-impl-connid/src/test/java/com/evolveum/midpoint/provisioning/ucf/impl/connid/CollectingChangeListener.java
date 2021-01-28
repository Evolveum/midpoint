/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.util.ArrayList;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.ucf.api.UcfLiveSyncChange;
import com.evolveum.midpoint.provisioning.ucf.api.UcfLiveSyncChangeListener;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * A change listener that simply collects changes that arrive.
 */
@VisibleForTesting
class CollectingChangeListener implements UcfLiveSyncChangeListener {

    private final List<UcfLiveSyncChange> changes = new ArrayList<>();

    @Override
    public boolean onChange(UcfLiveSyncChange change, OperationResult result) {
        changes.add(change);
        return true;
    }

    @Override
    public boolean onError(int localSequentialNumber, @NotNull Object primaryIdentifierRealValue,
            @NotNull PrismProperty<?> token, @NotNull Throwable exception, @NotNull OperationResult result) {
        // Should we test also this path?
        return false;
    }

    public List<UcfLiveSyncChange> getChanges() {
        return changes;
    }
}
