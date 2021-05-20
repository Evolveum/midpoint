/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.composite;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.repo.common.task.execution.ActivityInstantiationContext;
import com.evolveum.midpoint.repo.common.task.execution.ActivityInstantiationContext.ComponentActivityInstantiationContext;

import com.evolveum.midpoint.repo.common.task.execution.AbstractCompositeActivityExecution;
import com.evolveum.midpoint.repo.common.task.execution.ActivityExecution;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Mock activity is a semi-composite one. It contains two tailorable activities - opening and closing.
 * These activities are separable: they can run as part of mock activity or separately under pure composite activity.
 */
class CompositeMockActivityExecution
        extends AbstractCompositeActivityExecution<CompositeMockWorkDefinition, CompositeMockActivityHandler> {

    CompositeMockActivityExecution(ActivityInstantiationContext<CompositeMockWorkDefinition> context,
            @NotNull CompositeMockActivityHandler handler) {
        super(context, handler);
    }

    @Override
    @NotNull
    protected List<ActivityExecution> createChildren(OperationResult result) {
        List<ActivityExecution> executions = new ArrayList<>();
        ComponentActivityInstantiationContext<CompositeMockWorkDefinition> context =
                new ComponentActivityInstantiationContext<>(activityDefinition, this);
        if (isOpeningEnabled()) {
            executions.add(new MockOpeningActivityExecution(context, activityHandler));
        }
        if (isClosingEnabled()) {
            executions.add(new MockClosingActivityExecution(context, activityHandler));
        }
        return executions;
    }

    private boolean isOpeningEnabled() {
        return !Boolean.FALSE.equals(activityDefinition.getWorkDefinition().isOpening());
    }

    private boolean isClosingEnabled() {
        return !Boolean.FALSE.equals(activityDefinition.getWorkDefinition().isClosing());
    }

    void recordExecution(String value) {
        getRecorder().recordExecution(value);
    }

    MockRecorder getRecorder() {
        return activityHandler.getRecorder();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder(super.debugDump(indent));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "current recorder state", getRecorder(), indent+1);
        return sb.toString();
    }
}
