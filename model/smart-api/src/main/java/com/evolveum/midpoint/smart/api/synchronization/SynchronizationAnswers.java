/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */
package com.evolveum.midpoint.smart.api.synchronization;

/** Answers from the GUI wizard for building synchronization reactions. */
public class SynchronizationAnswers {

    private final UnmatchedChoice unmatched;
    private final DeletedChoice deleted;
    private final DisputedChoice disputed;

    public SynchronizationAnswers(UnmatchedChoice unmatched, DeletedChoice deleted, DisputedChoice disputed) {
        this.unmatched = unmatched;
        this.deleted = deleted;
        this.disputed = disputed;
    }

    public static SynchronizationAnswers of(UnmatchedChoice unmatched, DeletedChoice deleted, DisputedChoice disputed) {
        return new SynchronizationAnswers(unmatched, deleted, disputed);
    }

    public UnmatchedChoice getUnmatched() {
        return unmatched;
    }

    public DeletedChoice getDeleted() {
        return deleted;
    }

    public DisputedChoice getDisputed() {
        return disputed;
    }
}

