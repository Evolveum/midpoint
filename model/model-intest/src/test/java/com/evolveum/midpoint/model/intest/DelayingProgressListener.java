/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest;

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
