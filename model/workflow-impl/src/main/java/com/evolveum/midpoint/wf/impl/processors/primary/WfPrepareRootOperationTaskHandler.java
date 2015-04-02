/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.jobs.Job;
import com.evolveum.midpoint.wf.impl.jobs.JobController;
import com.evolveum.midpoint.wf.impl.jobs.WfTaskUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.List;

/**
 * This handler takes all changes from child tasks and puts them into model context of this task.
 *
 * @author mederly
 */
@Component
public class WfPrepareRootOperationTaskHandler implements TaskHandler {

    // should be available only within the context of primary change processor
	static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/workflow/prepare-root-operation/handler-3";

    private static final Trace LOGGER = TraceManager.getTrace(WfPrepareRootOperationTaskHandler.class);

    //region Spring dependencies and initialization
    @Autowired
    private TaskManager taskManager;

    @Autowired
    private JobController jobController;

    @PostConstruct
    public void init() {
        LOGGER.trace("Registering with taskManager as a handler for " + HANDLER_URI);
        taskManager.registerHandler(HANDLER_URI, this);
    }
    //endregion

    //region run method
	@Override
	public TaskRunResult run(Task task) {

        TaskRunResultStatus status = TaskRunResultStatus.FINISHED;

        try {

            OperationResult result = task.getResult();

            Job rootJob = jobController.recreateRootJob(task);
            List<Job> children = rootJob.listChildren(result);

            LensContext rootContext = (LensContext) rootJob.retrieveModelContext(result);

            boolean changed = false;
            for (Job child : children) {

                if (child.getTaskExecutionStatus() != TaskExecutionStatus.CLOSED) {
                    throw new IllegalStateException("Child task " + child + " is not in CLOSED state; its state is " + child.getTaskExecutionStatus());
                }

                if (child.hasModelContext()) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Child job {} has model context present - skipping fetching deltas from it.", child);
                    }
                } else {
                    List<ObjectDelta<Objectable>> deltas = child.retrieveResultingDeltas();
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Child job {} returned {} deltas", child, deltas.size());
                    }
                    LensFocusContext focusContext = rootContext.getFocusContext();
                    for (ObjectDelta delta : deltas) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Adding delta from job {} to root model context; delta = {}", child, delta.debugDump(0));
                        }
                        if (focusContext.getPrimaryDelta() != null && !focusContext.getPrimaryDelta().isEmpty()) {
                            focusContext.addPrimaryDelta(delta);
                        } else {
                            focusContext.setPrimaryDelta(delta);
                        }
                        changed = true;
                    }
                }
            }

            if (rootContext.getFocusContext().getPrimaryDelta() == null || rootContext.getFocusContext().getPrimaryDelta().isEmpty()) {
                rootJob.setSkipModelContextProcessingProperty(true, result);
                changed = true;     // regardless of whether rootContext was changed or not
            }

            if (changed) {
                rootJob.storeModelContext(rootContext);
                rootJob.commitChanges(result);
            }

        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't aggregate resulting deltas from child workflow-monitoring tasks due to schema exception", e);
            status = TaskRunResultStatus.PERMANENT_ERROR;
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't aggregate resulting deltas from child workflow-monitoring tasks", e);
            status = TaskRunResultStatus.PERMANENT_ERROR;
        } catch (ObjectAlreadyExistsException e) {
            LoggingUtils.logException(LOGGER, "Couldn't aggregate resulting deltas from child workflow-monitoring tasks", e);
            status = TaskRunResultStatus.PERMANENT_ERROR;
        } catch (CommunicationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't aggregate resulting deltas from child workflow-monitoring tasks", e);
            status = TaskRunResultStatus.TEMPORARY_ERROR;
        } catch (ConfigurationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't aggregate resulting deltas from child workflow-monitoring tasks", e);
            status = TaskRunResultStatus.PERMANENT_ERROR;
        }

        TaskRunResult runResult = new TaskRunResult();
		runResult.setRunResultStatus(status);
		return runResult;
	}
    //endregion

    //region Other task handler stuff
	@Override
	public Long heartbeat(Task task) {
		return null;		// null - as *not* to record progress (which would overwrite operationResult!)
	}

	@Override
	public void refreshStatus(Task task) {
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.WORKFLOW;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
    //endregion

}
