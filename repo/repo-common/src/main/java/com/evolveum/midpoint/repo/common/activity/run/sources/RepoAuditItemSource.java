/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.sources;

import com.evolveum.midpoint.repo.common.activity.run.SearchSpecification;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.ContainerableResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Provides access to audit events at the repository ({@link AuditService}) level.
 */
@Component
public class RepoAuditItemSource implements SearchableItemSource {

    @Autowired private AuditService auditService;

    @Override
    public Integer count(@NotNull SearchSpecification<?> searchSpecification, @NotNull RunningTask task,
            @NotNull OperationResult result) throws CommonException {
        return auditService.countObjects(
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                result);
    }

    @Override
    public <C extends Containerable> void searchIterative(@NotNull SearchSpecification<C> searchSpecification,
            @NotNull ContainerableResultHandler<C> handler, @NotNull RunningTask task,
            @NotNull OperationResult result)
            throws CommonException {
        auditService.searchObjectsIterative(
                searchSpecification.getQuery(),
                toAuditResultHandler(handler),
                searchSpecification.getSearchOptions(),
                result);
    }

    /**
     * Adapts arbitrary {@link ContainerableResultHandler} (of AuditResultType) to {@link AuditResultHandler}.
     */
    public static <C extends Containerable> AuditResultHandler toAuditResultHandler(
            @NotNull ContainerableResultHandler<C> handler) {
        if (handler instanceof AuditResultHandler) {
            return (AuditResultHandler) handler;
        } else {
            //noinspection unchecked
            return (value, parentResult) ->
                    handler.handle((C) value, parentResult);
        }
    }
}
