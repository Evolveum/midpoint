/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ChangeHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
class CollectingChangeHandler implements ChangeHandler {
    private List<Change> changes = new ArrayList<>();

    @Override
    public boolean handleChange(Change change, OperationResult result) {
        changes.add(change);
        return true;
    }

    @Override
    public boolean handleError(PrismProperty<?> token, @Nullable Change change,
            @NotNull Throwable exception, @NotNull OperationResult result) {
        return false;
    }

    public List<Change> getChanges() {
        return changes;
    }
}
