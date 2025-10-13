/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.distribution;

public class WorkersReconciliationOptions {

    private boolean dontCloseWorkersWhenWorkDone;
    private boolean createSuspended;

    private boolean isDontCloseWorkersWhenWorkDone() {
        return dontCloseWorkersWhenWorkDone;
    }

    static boolean shouldCloseWorkersOnWorkDone(WorkersReconciliationOptions options) {
        return options == null || !options.isDontCloseWorkersWhenWorkDone();
    }

    public void setDontCloseWorkersWhenWorkDone(boolean dontCloseWorkersWhenWorkDone) {
        this.dontCloseWorkersWhenWorkDone = dontCloseWorkersWhenWorkDone;
    }

    public boolean isCreateSuspended() {
        return createSuspended;
    }

    static boolean shouldCreateSuspended(WorkersReconciliationOptions options) {
        return options != null && options.createSuspended;
    }

    public void setCreateSuspended(boolean createSuspended) {
        this.createSuspended = createSuspended;
    }
}
