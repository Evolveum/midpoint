/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

class AuditSearchActivityExecutionSupport<C extends Containerable>
        implements SearchActivityExecutionSupport<C, PrismContainer<C>, AuditResultHandler> {

    @Override
    public int countObjectsInRepository(Class<C> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull IterativeActivityExecution activityExecution, OperationResult result) throws SchemaException {
        return activityExecution.beans.auditService.countObjects(
                query,
                options,
                result);
    }

    public OperationResultType getFetchResult(PrismContainer<C> object) {
        return null;
    }

    @Override
    public void searchIterativeInRepository(Class<C> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            AtomicInteger sequentialNumberCounter, @NotNull IterativeActivityExecution activityExecution, OperationResult result) throws SchemaException {
        activityExecution.beans.auditService.searchObjectsIterative(
                query,
                createSearchResultHandler(sequentialNumberCounter, activityExecution.coordinator, activityExecution),
                options,
                result);
    }

    @Override
    public AuditResultHandler createSearchResultHandler(AtomicInteger sequentialNumberCounter, ProcessingCoordinator coordinator,
            @NotNull IterativeActivityExecution activityExecution) {
        return (object, parentResult) -> {
            ItemProcessingRequest request =
                    new ContainerableProcessingRequest(sequentialNumberCounter.getAndIncrement(), object, activityExecution);
            return coordinator.submit(request, parentResult);
        };
    }
}
