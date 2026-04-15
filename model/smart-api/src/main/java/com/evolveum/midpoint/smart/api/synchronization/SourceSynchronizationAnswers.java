/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.smart.api.synchronization;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class SourceSynchronizationAnswers implements Serializable {

    private UnmatchedSourceChoice unmatched;
    private DeletedSourceChoice deleted;

    public SourceSynchronizationAnswers(UnmatchedSourceChoice unmatched, DeletedSourceChoice deleted) {
        this.unmatched = unmatched;
        this.deleted = deleted;
    }

    public static @NotNull SourceSynchronizationAnswers of(UnmatchedSourceChoice unmatched, DeletedSourceChoice deleted) {
        return new SourceSynchronizationAnswers(unmatched, deleted);
    }

    public UnmatchedSourceChoice getUnmatched() {
        return unmatched;
    }

    public DeletedSourceChoice getDeleted() {
        return deleted;
    }

    public void setDeleted(DeletedSourceChoice deleted) {
        this.deleted = deleted;
    }

    public void setUnmatched(UnmatchedSourceChoice unmatched) {
        this.unmatched = unmatched;
    }

}
