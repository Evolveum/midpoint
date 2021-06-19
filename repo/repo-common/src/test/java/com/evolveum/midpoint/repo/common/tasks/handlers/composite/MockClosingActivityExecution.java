/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.composite;

import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStatePersistenceType;

import org.jetbrains.annotations.NotNull;

class MockClosingActivityExecution extends MockComponentActivityExecution {

    MockClosingActivityExecution(
            @NotNull ExecutionInstantiationContext<CompositeMockWorkDefinition, CompositeMockActivityHandler> context) {
        super(context);
    }

    @Override
    public @NotNull ActivityStatePersistenceType getPersistenceType() {
        return ActivityStatePersistenceType.PERPETUAL;
    }

    @Override
    String getSubActivity() {
        return "closing";
    }
}
