/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.smart.api.synchronization;

public class TargetSynchronizationAnswers {

    private final UnmatchedTargetChoice unmatched;
    private final DeletedTargetChoice deleted;
    private final DisputedTargetChoice disputed;

    public TargetSynchronizationAnswers(UnmatchedTargetChoice unmatched, DeletedTargetChoice deleted, DisputedTargetChoice disputed) {
        this.unmatched = unmatched;
        this.deleted = deleted;
        this.disputed = disputed;
    }

    public static TargetSynchronizationAnswers of(UnmatchedTargetChoice unmatched, DeletedTargetChoice deleted, DisputedTargetChoice disputed) {
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
}
