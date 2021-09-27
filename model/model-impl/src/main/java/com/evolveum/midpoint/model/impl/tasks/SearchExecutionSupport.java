/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.task.SearchSpecification;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;

import org.jetbrains.annotations.NotNull;

interface SearchExecutionSupport {

    Integer countObjects(@NotNull SearchSpecification<?> searchSpecification, @NotNull RunningTask task,
            @NotNull OperationResult result) throws CommonException;

    <C extends Containerable> void searchIterative(@NotNull SearchSpecification<C> searchSpecification,
            @NotNull ObjectResultHandler handler, @NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException;
}
