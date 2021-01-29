/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Request to process an object.
 */
public class ObjectProcessingRequest<O extends ObjectType> extends ItemProcessingRequest<PrismObject<O>> {

    ObjectProcessingRequest(PrismObject<O> item, AbstractIterativeItemProcessor<PrismObject<O>, ?, ?, ?, ?> itemProcessor) {
        super(item, itemProcessor);
    }

    @Override
    public @NotNull IterationItemInformation getIterationItemInformation() {
        return new IterationItemInformation(item);
    }

    @Override
    public ObjectType getObjectToReportOperation() {
        return getItem().asObjectable(); // TODO
    }

    @Override
    public void acknowledge(boolean release, OperationResult result) {
        // Nothing to acknowledge here.
    }
}
