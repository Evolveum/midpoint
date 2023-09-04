/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
