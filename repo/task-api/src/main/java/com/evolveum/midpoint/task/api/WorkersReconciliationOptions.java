/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

/**
 * @author mederly
 */
public class WorkersReconciliationOptions {
    private boolean dontCloseWorkersWhenWorkDone;

    public boolean isDontCloseWorkersWhenWorkDone() {
        return dontCloseWorkersWhenWorkDone;
    }

    public void setDontCloseWorkersWhenWorkDone(boolean dontCloseWorkersWhenWorkDone) {
        this.dontCloseWorkersWhenWorkDone = dontCloseWorkersWhenWorkDone;
    }
}
