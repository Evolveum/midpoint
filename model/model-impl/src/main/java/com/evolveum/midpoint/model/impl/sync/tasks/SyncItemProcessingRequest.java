/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.SynchronizationEvent;
import com.evolveum.midpoint.repo.common.task.AbstractIterativeItemProcessor;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * TODO
 */
public class SyncItemProcessingRequest<SE extends SynchronizationEvent> extends ItemProcessingRequest<SE> {

    SyncItemProcessingRequest(SE item, AbstractIterativeItemProcessor<SE, ?, ?, ?, ?> itemProcessor) {
        super(item, itemProcessor);
    }

    @Override
    public Object getCorrelationValue() {
        return getItem().getCorrelationValue();
    }

    @Override
    public ShadowType getObjectToReportOperation() {
        ResourceObjectShadowChangeDescription changeDescription = getItem().getChangeDescription();
        if (changeDescription != null && changeDescription.getCurrentShadow() != null) {
            return changeDescription.getCurrentShadow().asObjectable(); // TODO
        } else {
            return null;
        }
    }
}
