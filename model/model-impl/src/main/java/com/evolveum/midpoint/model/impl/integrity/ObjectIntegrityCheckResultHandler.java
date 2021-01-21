/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity;

import java.util.Map;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 *
 */
public class ObjectIntegrityCheckResultHandler
        extends AbstractSearchIterativeResultHandler
        <ObjectType,
                ObjectIntegrityCheckTaskHandler,
                ObjectIntegrityCheckTaskHandler.TaskExecution,
                ObjectIntegrityCheckTaskPartExecution, ObjectIntegrityCheckResultHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectIntegrityCheckResultHandler.class);

    private static final String CLASS_DOT = ObjectIntegrityCheckResultHandler.class.getName() + ".";
    private static final int HISTOGRAM_COLUMNS = 80;

    private final ObjectStatistics statistics = new ObjectStatistics();

    ObjectIntegrityCheckResultHandler(ObjectIntegrityCheckTaskPartExecution taskExecution) {
        super(taskExecution);

        ensureNoWorkerThreads();

        LOGGER.info("Object integrity check is starting");
    }

    @Override
    protected boolean handleObject(PrismObject<ObjectType> object, RunningTask workerTask, OperationResult parentResult) throws CommonException {
        OperationResult result = parentResult.createMinorSubresult(CLASS_DOT + "handleObject");
        try {
            statistics.record(object);
        } catch (RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unexpected error while checking object {} integrity", e, ObjectTypeUtil.toShortString(object));
            result.recordPartialError("Unexpected error while checking object integrity", e);
            statistics.incrementObjectsWithErrors();
        } finally {
            workerTask.markObjectActionExecutedBoundary();
        }

        result.computeStatusIfUnknown();
        return true;
    }

    public ObjectStatistics getStatistics() {
        return statistics;
    }

    @Override
    public void completeProcessing(Task task, OperationResult result) {
        super.completeProcessing(task, result);
        LOGGER.info("Object integrity check finished.");
        dumpStatistics();
    }

    private void dumpStatistics() {
        Map<String, ObjectTypeStatistics> map = statistics.getStatisticsMap();
        if (map.isEmpty()) {
            LOGGER.info("(no objects were found)");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, ObjectTypeStatistics> entry : map.entrySet()) {
                sb.append("\n\n**************************************** Statistics for ").append(entry.getKey()).append(" ****************************************\n\n");
                sb.append(entry.getValue().dump(HISTOGRAM_COLUMNS));
            }
            LOGGER.info("{}", sb.toString());
        }
        LOGGER.info("Objects processed with errors: {}", statistics.getErrors());
    }
}
