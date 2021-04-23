/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartDefinitionType;

@Component
@Experimental
@FunctionalInterface
public interface TaskPartExecutionFactory {

    @NotNull TaskPartExecution createPartExecution(@NotNull TaskPartDefinitionType partDef, @NotNull TaskExecution taskExecution,
            @NotNull OperationResult result);

}
