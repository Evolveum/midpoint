/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test.correlator;

import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
    public CorrelationResult correlate(@NotNull ShadowType resourceObject, @NotNull Task task, @NotNull OperationResult result) {
        // TODO
        return CorrelationResult.uncertain();
    }
}
