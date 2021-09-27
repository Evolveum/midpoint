/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks;

import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.task.SearchSpecification;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.assertCheck;

class ObjectSearchExecutionSupport implements SearchExecutionSupport {

    private final ModelObjectResolver modelObjectResolver;

    ObjectSearchExecutionSupport(ModelObjectResolver modelObjectResolver) {
        this.modelObjectResolver = modelObjectResolver;
    }

    @Override
    public Integer countObjects(@NotNull SearchSpecification<?> searchSpecification, @NotNull RunningTask task,
            @NotNull OperationResult result) throws CommonException {
        return modelObjectResolver.countObjects(
                (Class<? extends ObjectType>) searchSpecification.getContainerType(),
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                task, result);
    }

    @Override
    public <C extends Containerable> void searchIterative(@NotNull SearchSpecification<C> searchSpecification,
            @NotNull ObjectResultHandler handler, @NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException {
        assertCheck(handler instanceof ResultHandler,
                "Unsupported type of result handler for object type " + searchSpecification.getContainerType());
        modelObjectResolver.searchIterative(
                (Class<? extends ObjectType>) searchSpecification.getContainerType(),
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                (ResultHandler) handler, task, result);
    }
}
