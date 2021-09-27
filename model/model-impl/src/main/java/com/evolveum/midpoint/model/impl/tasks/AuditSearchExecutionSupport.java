/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks;

import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.model.api.ModelAuditService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.task.SearchSpecification;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.assertCheck;

class AuditSearchExecutionSupport implements SearchExecutionSupport {

    private final ModelAuditService modelAuditService;

    AuditSearchExecutionSupport(ModelAuditService modelAuditService) {
        this.modelAuditService = modelAuditService;
    }

    @Override
    public Integer countObjects(@NotNull SearchSpecification<?> searchSpecification, @NotNull RunningTask task,
            @NotNull OperationResult result) throws CommonException {
        return modelAuditService.countObjects(
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                task, result);
    }

    @Override
    public <C extends Containerable> void searchIterative(@NotNull SearchSpecification<C> searchSpecification,
            @NotNull ObjectResultHandler handler, @NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException {
        assertCheck(handler instanceof AuditResultHandler,
                "Unsupported type of result handler for object type " + searchSpecification.getContainerType());
        modelAuditService.searchObjectsIterative(
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                (AuditResultHandler) handler, task, result);
    }

}
