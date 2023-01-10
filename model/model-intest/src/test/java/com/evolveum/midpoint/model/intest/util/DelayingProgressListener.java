/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.util;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.api.context.ModelContext;

public class DelayingProgressListener implements ProgressListener {

    private final long delayMin, delayMax;

    public DelayingProgressListener(long delayMin, long delayMax) {
        this.delayMin = delayMin;
        this.delayMax = delayMax;
    }

    @Override
    public void onProgressAchieved(ModelContext<?> modelContext, ProgressInformation progressInformation) {
        try {
            long delay = (long) (delayMin + Math.random() * (delayMax - delayMin));
            System.out.println("[" + Thread.currentThread().getName() + "] Delaying execution by " + delay + " ms for " + progressInformation);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Override
    public boolean isAbortRequested() {
        return false;
    }
}
