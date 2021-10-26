/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
