/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import com.evolveum.midpoint.repo.common.activity.execution.AbstractCompositeActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import org.jetbrains.annotations.NotNull;

/**
 * Nothing special here. This activity execution is just a shell for executing the children.
 */
class ReconciliationActivityExecution
        extends AbstractCompositeActivityExecution<
            ReconciliationWorkDefinition,
            ReconciliationActivityHandler,
            AbstractActivityWorkStateType> {

    ReconciliationActivityExecution(
            @NotNull ExecutionInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> context) {
        super(context);
    }
}
