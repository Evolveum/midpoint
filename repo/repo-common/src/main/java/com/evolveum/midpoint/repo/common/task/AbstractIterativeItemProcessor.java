/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.util.logging.Trace;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Processes a single item, typically a prism object or a synchronization event.
 *
 * Does *not* care about auxiliary actions, like statistics keeping, error handling, logging, profiling, and so on.
 * All of this is handled by {@link ItemProcessingGatekeeper} class.
 *
 * Also, should not keep much state related to the processing like the overall statistics.
 * This state should be maintained by related part execution object.
 */
@SuppressWarnings("JavadocReference")
public abstract class AbstractIterativeItemProcessor<I,
        TH extends AbstractTaskHandler<TH, TE>,
        TE extends AbstractTaskExecution<TH, TE>,
        PE extends AbstractIterativeTaskPartExecution<I, TH, TE, PE, IP>,
        IP extends AbstractIterativeItemProcessor<I, TH, TE, PE, IP>> {

    /**
     * Execution of the containing task part.
     */
    @NotNull protected final PE partExecution;

    /**
     * Execution of the containing task.
     */
    @NotNull protected final TE taskExecution;

    /**
     * Handler of the containing task.
     */
    @NotNull protected final TH taskHandler;

    /**
     * See {@link AbstractTaskHandler#logger}.
     */
    @NotNull protected final Trace logger;

    protected AbstractIterativeItemProcessor(@NotNull PE partExecution) {
        this.partExecution = partExecution;
        this.taskExecution = partExecution.taskExecution;
        this.taskHandler = partExecution.taskHandler;
        this.logger = taskHandler.getLogger();
    }

    /**
     * Does the "pure" processing, free of any reporting, error handling, tracing, and similar issues.
     */
    public abstract boolean process(ItemProcessingRequest<I> request, RunningTask workerTask, OperationResult parentResult)
            throws CommonException, PreconditionViolationException;
}
