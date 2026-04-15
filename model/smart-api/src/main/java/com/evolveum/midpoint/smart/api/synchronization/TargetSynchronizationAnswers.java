/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.smart.api.synchronization;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class TargetSynchronizationAnswers implements Serializable {

    private UnmatchedTargetChoice unmatched;
    private DeletedTargetChoice deleted;
    private DisputedTargetChoice disputed;

    public TargetSynchronizationAnswers(UnmatchedTargetChoice unmatched, DeletedTargetChoice deleted, DisputedTargetChoice disputed) {
        this.unmatched = unmatched;
        this.deleted = deleted;
        this.disputed = disputed;
    }

    public static @NotNull TargetSynchronizationAnswers of(UnmatchedTargetChoice unmatched, DeletedTargetChoice deleted, DisputedTargetChoice disputed) {
        return new TargetSynchronizationAnswers(unmatched, deleted, disputed);
    }

    public UnmatchedTargetChoice getUnmatched() {
        return unmatched;
    }

    public DeletedTargetChoice getDeleted() {
        return deleted;
    }

    public DisputedTargetChoice getDisputed() {
        return disputed;
    }

    public void setUnmatched(UnmatchedTargetChoice unmatched) {
        this.unmatched = unmatched;
    }

    public void setDeleted(DeletedTargetChoice deleted) {
        this.deleted = deleted;
    }

    public void setDisputed(DisputedTargetChoice disputed) {
        this.disputed = disputed;
    }
}
