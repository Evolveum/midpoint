/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
