/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

class ObjectSearchActivityExecutionSupport<O extends ObjectType>
        implements SearchActivityExecutionSupport<O, PrismObject<O>, ResultHandler<O>> {

    @Override
    public int countObjectsInRepository(Class<O> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull IterativeActivityExecution activityExecution,
            OperationResult result) throws SchemaException {
            return activityExecution.beans.repositoryService.countObjects(
                    type,
                    query,
                    options,
                    result);
    }

    @Override
    public OperationResultType getFetchResult(PrismObject<O> object) {
        return object.asObjectable().getFetchResult();
    }

    @Override
    public void searchIterativeInRepository(Class<O> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            AtomicInteger sequentialNumberCounter, @NotNull IterativeActivityExecution activityExecution,
            OperationResult result) throws SchemaException {
            activityExecution.beans.repositoryService.searchObjectsIterative(
                    type,
                    query,
                    createSearchResultHandler(sequentialNumberCounter, activityExecution.coordinator, activityExecution),
                    options,
                    true,
                    result);
    }

    @Override
    public ResultHandler<O> createSearchResultHandler(AtomicInteger sequentialNumberCounter, ProcessingCoordinator coordinator,
            @NotNull IterativeActivityExecution activityExecution) {
        return (object, parentResult) -> {
            ObjectProcessingRequest<O> request =
                        new ObjectProcessingRequest(sequentialNumberCounter.getAndIncrement(), object, activityExecution);
            return coordinator.submit(request, parentResult);
        };
    }
}
