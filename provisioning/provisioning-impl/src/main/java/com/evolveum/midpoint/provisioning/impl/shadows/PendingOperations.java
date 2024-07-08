/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class PendingOperations implements Serializable, Iterable<PendingOperation> {

    @NotNull private final ImmutableList<PendingOperation> pendingOperations;

    private PendingOperations(@NotNull List<PendingOperation> pendingOperations) {
        this.pendingOperations = ImmutableList.copyOf(pendingOperations);
    }

    public static @NotNull PendingOperations of(@NotNull List<PendingOperationType> beans) {
        return new PendingOperations(
                beans.stream()
                        .map(PendingOperation::new)
                        .toList());
    }

    public static @NotNull PendingOperations sorted(@NotNull List<PendingOperationType> beans) {
        return of(ShadowUtil.sortPendingOperations(beans));
    }

    public @NotNull Collection<PendingOperation> getOperations() {
        return pendingOperations;
    }

    public int size() {
        return pendingOperations.size();
    }

    public PendingOperation findPendingAddOperation() {
        return pendingOperations.stream()
                .filter(pendingOperation -> pendingOperation.isAdd() && pendingOperation.isInProgress())
                .findFirst()
                .orElse(null);
    }

    public boolean isEmpty() {
        return pendingOperations.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<PendingOperation> iterator() {
        return pendingOperations.iterator();
    }

    public boolean hasRetryableOperation() {
        return pendingOperations.stream()
                .anyMatch(PendingOperation::canBeRetried);
    }
}
