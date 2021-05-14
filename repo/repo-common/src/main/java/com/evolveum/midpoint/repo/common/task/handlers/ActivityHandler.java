/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.handlers;

import com.evolveum.midpoint.repo.common.task.execution.ActivityContext;
import com.evolveum.midpoint.repo.common.task.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.task.execution.ActivityExecution;
import com.evolveum.midpoint.task.api.TaskHandler;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Spring component that ensures handling activity invocations.
 *
 * It is really minimalistic: its only responsibility is to instantiate appropriate {@link ActivityExecution} object.
 *
 * The naming is derived from the {@link TaskHandler}, to which it is conceptually somewhat similar.
 */
@Component
@Experimental
@FunctionalInterface
public interface ActivityHandler<WD extends WorkDefinition> {

    @NotNull ActivityExecution createExecution(@NotNull ActivityContext<WD> context, @NotNull OperationResult result);
}
