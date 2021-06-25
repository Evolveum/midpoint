/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ExecutionSupplier<WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>> {

    AbstractActivityExecution<WD, AH, ?> createExecution(@NotNull ExecutionInstantiationContext<WD, AH> context,
            @NotNull OperationResult result);
}
