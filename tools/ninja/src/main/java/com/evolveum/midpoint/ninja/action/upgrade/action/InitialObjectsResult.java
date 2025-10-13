/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.action.upgrade.action;

public class InitialObjectsResult {

    private int total;

    private int added;

    private int merged;

    private int unchanged;

    private int error;

    void incrementUnchanged() {
        unchanged++;
    }

    void incrementTotal() {
        total++;
    }

    void incrementAdded() {
        added++;
    }

    void incrementMerged() {
        merged++;
    }

    void incrementError() {
        error++;
    }

    public int getTotal() {
        return total;
    }

    public int getAdded() {
        return added;
    }

    public int getMerged() {
        return merged;
    }

    public int getError() {
        return error;
    }

    public int getUnchanged() {
        return unchanged;
    }
}
