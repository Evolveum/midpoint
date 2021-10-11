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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class CheckingProgressListener implements ProgressListener {

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.model.api.ProgressListener#onProgressAchieved(com.evolveum.midpoint.model.api.context.ModelContext, com.evolveum.midpoint.model.api.ProgressInformation)
     */
    @Override
    public void onProgressAchieved(ModelContext modelContext, ProgressInformation progressInformation) {
        LensContext<ObjectType> lensContext = (LensContext<ObjectType>)modelContext;
        lensContext.checkConsistence();
        for (LensProjectionContext projectionContext: lensContext.getProjectionContexts()) {
            // MID-3213
            assert projectionContext.getResourceShadowDiscriminator().getResourceOid() != null;
        }
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.model.api.ProgressListener#isAbortRequested()
     */
    @Override
    public boolean isAbortRequested() {
        return false;
    }

}
