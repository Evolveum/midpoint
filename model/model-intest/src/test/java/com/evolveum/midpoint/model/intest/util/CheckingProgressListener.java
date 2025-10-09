/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.util;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;

/**
 * @author semancik
 *
 */
public class CheckingProgressListener implements ProgressListener {

    @Override
    public void onProgressAchieved(ModelContext<?> modelContext, ProgressInformation progressInformation) {
        LensContext<?> lensContext = (LensContext<?>)modelContext;
        lensContext.checkConsistence();
        for (LensProjectionContext projectionContext: lensContext.getProjectionContexts()) {
            // MID-3213
            assert projectionContext.getKey().getResourceOid() != null;
        }
    }

    @Override
    public boolean isAbortRequested() {
        return false;
    }
}
