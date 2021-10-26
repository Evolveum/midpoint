/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.processing;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;

public interface ItemProcessor<I> {

    /**
     * Does the "pure" processing, free of any reporting, error handling, tracing, and similar issues.
     */
    boolean processItem(ItemProcessingRequest<I> request, RunningTask workerTask, OperationResult parentResult)
            throws CommonException, ActivityRunException;

}
