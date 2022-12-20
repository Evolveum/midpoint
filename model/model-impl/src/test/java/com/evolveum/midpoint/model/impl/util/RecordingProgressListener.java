/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.util;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.api.context.ModelContext;

public class RecordingProgressListener implements ProgressListener {
    private ModelContext<?> modelContext;

    @Override
    public void onProgressAchieved(ModelContext<?> modelContext, ProgressInformation progressInformation) {
        this.modelContext = modelContext;
    }

    @Override
    public boolean isAbortRequested() {
        return false;
    }

    public ModelContext<?> getModelContext() {
        return modelContext;
    }
}
