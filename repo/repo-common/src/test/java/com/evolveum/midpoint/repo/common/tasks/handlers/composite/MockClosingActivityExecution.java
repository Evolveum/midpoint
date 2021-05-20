/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.composite;

import com.evolveum.midpoint.repo.common.task.execution.ActivityInstantiationContext;

import org.jetbrains.annotations.NotNull;

class MockClosingActivityExecution extends MockComponentActivityExecution {

    MockClosingActivityExecution(@NotNull ActivityInstantiationContext<CompositeMockWorkDefinition> context,
            @NotNull CompositeMockActivityHandler activityHandler) {
        super(context, activityHandler);
    }

    @Override
    String getSubActivity() {
        return "closing";
    }

    @Override
    public String toString() {
        return "CompositeMockClosingActivityExecution{" +
                "activityDefinition=" + activityDefinition +
                '}';
    }
}
