/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.smart.api.synchronization;

public class SourceSynchronizationAnswers {

    private final UnmatchedSourceChoice unmatched;
    private final DeletedSourceChoice deleted;

    public SourceSynchronizationAnswers(UnmatchedSourceChoice unmatched, DeletedSourceChoice deleted) {
        this.unmatched = unmatched;
        this.deleted = deleted;
    }

    public static SourceSynchronizationAnswers of(UnmatchedSourceChoice unmatched, DeletedSourceChoice deleted) {
        return new SourceSynchronizationAnswers(unmatched, deleted);
    }

    public UnmatchedSourceChoice getUnmatched() {
        return unmatched;
    }

    public DeletedSourceChoice getDeleted() {
        return deleted;
    }
}
