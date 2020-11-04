/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.LiveSyncChangeListener;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A change listener that simply collects changes that arrive.
 */
@VisibleForTesting
class CollectingChangeListener implements LiveSyncChangeListener {
    private final List<Change> changes = new ArrayList<>();

    @Override
    public boolean onChange(Change change, OperationResult result) {
        changes.add(change);
        return true;
    }

    @Override
    public boolean onChangePreparationError(PrismProperty<?> token, @Nullable Change change,
            @NotNull Throwable exception, @NotNull OperationResult result) {
        return false;
    }

    @Override
    public void onAllChangesFetched(PrismProperty<?> finalToken, OperationResult result) {
    }

    public List<Change> getChanges() {
        return changes;
    }
}
