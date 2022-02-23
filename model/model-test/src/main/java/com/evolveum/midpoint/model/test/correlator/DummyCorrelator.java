/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test.correlator;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectOwnerOptionsType;

import org.jetbrains.annotations.NotNull;

/**
 * A dummy correlator implementation.
 *
 * Not used yet.
 */
class DummyCorrelator implements Correlator {

    DummyCorrelator(AbstractCorrelatorType ignored) {
    }

    @Override
    public CorrelationResult correlate(
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result) {
        // TODO
        return CorrelationResult.uncertain(
                new ResourceObjectOwnerOptionsType(PrismContext.get()));
    }

    @Override
    public void resolve(
            @NotNull CaseType aCase,
            @NotNull String outcomeUri,
            @NotNull Task task,
            @NotNull OperationResult result) {
        throw new UnsupportedOperationException();
    }
}
