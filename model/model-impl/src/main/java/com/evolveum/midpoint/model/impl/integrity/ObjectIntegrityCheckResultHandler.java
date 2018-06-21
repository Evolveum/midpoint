/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Map;

/**
 * @author mederly
 */
public class ObjectIntegrityCheckResultHandler extends AbstractSearchIterativeResultHandler<ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectIntegrityCheckResultHandler.class);

    private static final String CLASS_DOT = ObjectIntegrityCheckResultHandler.class.getName() + ".";
    private static final int HISTOGRAM_COLUMNS = 80;

    private PrismContext prismContext;
    private RepositoryService repositoryService;
    private SystemObjectCache systemObjectCache;

    private ObjectStatistics statistics = new ObjectStatistics();

    public ObjectIntegrityCheckResultHandler(Task coordinatorTask, String taskOperationPrefix, String processShortName,
            String contextDesc, TaskManager taskManager, PrismContext prismContext, RepositoryService repositoryService,
            SystemObjectCache systemObjectCache, OperationResult result) {
        super(coordinatorTask, taskOperationPrefix, processShortName, contextDesc, taskManager);
        this.prismContext = prismContext;
        this.repositoryService = repositoryService;
        this.systemObjectCache = systemObjectCache;
        setStopOnError(false);
        setLogErrors(false);            // we do log errors ourselves

        Integer tasks = getWorkerThreadsCount(coordinatorTask);
        if (tasks != null && tasks != 0) {
            throw new UnsupportedOperationException("Unsupported number of worker threads: " + tasks + ". This task cannot be run with worker threads. Please remove workerThreads extension property or set its value to 0.");
        }

        logConfiguration("Object integrity check is starting");
    }

    private void logConfiguration(String state) {
        LOGGER.info("{}", state);
    }

    @Override
    protected boolean handleObject(PrismObject<ObjectType> object, Task workerTask, OperationResult parentResult) throws CommonException {
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
        logConfiguration("Object integrity check finished.");
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
