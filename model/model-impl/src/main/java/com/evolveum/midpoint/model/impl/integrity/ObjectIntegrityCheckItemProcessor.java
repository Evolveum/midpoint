/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeItemProcessor;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Item processor for object integrity checker.
 */
public class ObjectIntegrityCheckItemProcessor
        extends AbstractSearchIterativeItemProcessor
        <ObjectType,
                ObjectIntegrityCheckTaskHandler,
                ObjectIntegrityCheckTaskHandler.TaskExecution,
                ObjectIntegrityCheckTaskPartExecution, ObjectIntegrityCheckItemProcessor> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectIntegrityCheckItemProcessor.class);

    private static final String CLASS_DOT = ObjectIntegrityCheckItemProcessor.class.getName() + ".";

    ObjectIntegrityCheckItemProcessor(ObjectIntegrityCheckTaskPartExecution taskExecution) {
        super(taskExecution);
    }

    @Override
    protected boolean processObject(PrismObject<ObjectType> object,
            ItemProcessingRequest<PrismObject<ObjectType>> request,
            RunningTask workerTask, OperationResult parentResult) throws CommonException {
        OperationResult result = parentResult.createMinorSubresult(CLASS_DOT + "handleObject");
        try {
            partExecution.objectStatistics.record(object);
        } catch (RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unexpected error while checking object {} integrity", e, ObjectTypeUtil.toShortString(object));
            result.recordPartialError("Unexpected error while checking object integrity", e);
            partExecution.objectStatistics.incrementObjectsWithErrors();
        } finally {
            workerTask.markObjectActionExecutedBoundary();
        }

        result.computeStatusIfUnknown();
        return true;
    }
}
