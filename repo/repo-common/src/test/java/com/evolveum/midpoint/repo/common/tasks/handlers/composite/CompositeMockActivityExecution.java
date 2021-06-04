/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.composite;

import com.evolveum.midpoint.repo.common.activity.execution.AbstractCompositeActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

/**
 * Mock activity is a semi-composite one. It contains two tailorable activities - opening and closing.
 * These activities are separable: they can run as part of mock activity or separately under pure composite activity.
 */
class CompositeMockActivityExecution
        extends AbstractCompositeActivityExecution<CompositeMockWorkDefinition, CompositeMockActivityHandler, AbstractActivityWorkStateType> {

    CompositeMockActivityExecution(ExecutionInstantiationContext<CompositeMockWorkDefinition, CompositeMockActivityHandler> context) {
        super(context);
    }

    private MockRecorder getRecorder() {
        return activity.getHandler().getRecorder();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder(super.debugDump(indent));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "current recorder state", getRecorder(), indent+1);
        return sb.toString();
    }
}
