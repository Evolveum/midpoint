/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.composite;

import com.evolveum.midpoint.repo.common.task.execution.ActivityContext;

import org.jetbrains.annotations.NotNull;

class CompositeMockClosingActivityExecution extends CompositeMockSubActivityExecution {

    CompositeMockClosingActivityExecution(@NotNull ActivityContext<CompositeMockWorkDefinition> context) {
        super(context);
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
