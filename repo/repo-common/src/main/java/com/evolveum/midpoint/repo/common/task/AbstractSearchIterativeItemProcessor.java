/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import org.jetbrains.annotations.NotNull;

/**
 * Processes individual objects found by the iterative search.
 *
 * It provides backwards-compatible {@link #processObject(PrismObject, ItemProcessingRequest, RunningTask, OperationResult)}
 * to be used instead of more generic {@link #process(ItemProcessingRequest, RunningTask, OperationResult)} method.
 *
 * But also allows separate processing of errored objects by {@link #processError(PrismObject, OperationResultType, RunningTask, OperationResult)}.
 */
public abstract class AbstractSearchIterativeItemProcessor<
        O extends ObjectType,
        TH extends AbstractTaskHandler<TH, TE>,
        TE extends AbstractTaskExecution<TH, TE>,
        PE extends AbstractSearchIterativeTaskPartExecution<O, TH, TE, PE, IP>,
        IP extends AbstractSearchIterativeItemProcessor<O, TH, TE, PE, IP>>
        extends AbstractIterativeItemProcessor<PrismObject<O>, TH, TE, PE, IP> {

    public AbstractSearchIterativeItemProcessor(PE partExecution) {
        super(partExecution);
    }

    @Override
    public boolean process(ItemProcessingRequest<PrismObject<O>> request, RunningTask workerTask,
            OperationResult parentResult) throws CommonException, PreconditionViolationException {
        PrismObject<O> object = request.getItem();

        if (filteredOutByAdditionalFilter(request)) {
            logger.trace("Request {} filtered out by additional filter", request);
            parentResult.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Filtered out by additional filter");
            return true; // continue working
        }

        OperationResultType errorFetchResult = object.asObjectable().getFetchResult();
        if (errorFetchResult == null) {
            return processObject(object, request, workerTask, parentResult);
        } else {
            return processError(object, errorFetchResult, workerTask, parentResult);
        }
    }

    private boolean filteredOutByAdditionalFilter(ItemProcessingRequest<PrismObject<O>> request)
            throws SchemaException {
        return partExecution.additionalFilter != null &&
                !partExecution.additionalFilter.match(request.getItem().getValue(), taskHandler.matchingRuleRegistry);
    }

    protected abstract boolean processObject(PrismObject<O> object, ItemProcessingRequest<PrismObject<O>> request,
            RunningTask workerTask, OperationResult result)
            throws CommonException, PreconditionViolationException;

    @SuppressWarnings({ "WeakerAccess", "unused" })
    protected boolean processError(PrismObject<O> object, @NotNull OperationResultType errorFetchResult, RunningTask workerTask,
            OperationResult result)
            throws CommonException, PreconditionViolationException {
        result.recordFatalError("Error in preprocessing: " + errorFetchResult.getMessage());
        return true; // "Can continue" flag is updated by item processing gatekeeper (unfortunately, the exception is lost)
    }
}
