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
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Processes individual objects found by the iterative search.
 *
 * Actually, this class only provides backwards-compatible {@link #processObject(PrismObject, RunningTask, OperationResult)}
 * to be used instead of more generic {@link #process(ItemProcessingRequest, RunningTask, OperationResult)} method.
 *
 * *TODO consider removing this class altogether (requires migration from processObject to process).*
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
        return processObject(request.getItem(), workerTask, parentResult);
    }

    protected abstract boolean processObject(PrismObject<O> object, RunningTask workerTask, OperationResult result)
            throws CommonException, PreconditionViolationException;
}
