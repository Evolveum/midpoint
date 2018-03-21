/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.tasks.WfTask;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This handler takes all changes from child tasks and puts them into model context of this task.
 *
 * @author mederly
 */
@Component
public class WfPrepareRootOperationTaskHandler implements TaskHandler {

	public static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/workflow/prepare-root-operation/handler-3";

    private static final Trace LOGGER = TraceManager.getTrace(WfPrepareRootOperationTaskHandler.class);

    //region Spring dependencies and initialization
    @Autowired private TaskManager taskManager;
    @Autowired private WfTaskController wfTaskController;
    @Autowired private ApprovalMetadataHelper metadataHelper;

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

            WfTask rootWfTask = wfTaskController.recreateRootWfTask(task);
            List<WfTask> children = rootWfTask.listChildren(result);

            LensContext rootContext = (LensContext) rootWfTask.retrieveModelContext(result);

            List<ObjectTreeDeltas> deltasToMerge = new ArrayList<>();

            boolean changed = false;
            for (WfTask child : children) {

                if (child.getTaskExecutionStatus() != TaskExecutionStatus.CLOSED) {
                    throw new IllegalStateException("Child task " + child + " is not in CLOSED state; its state is " + child.getTaskExecutionStatus());
                }

                if (child.hasModelContext()) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Child job {} has model context present - skipping fetching deltas from it.", child);
                    }
                } else {
                    ObjectTreeDeltas deltas = child.retrieveResultingDeltas();
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Child job {} returned {} deltas", child, deltas != null ? deltas.getDeltaList().size() : 0);
                    }
                    if (deltas != null) {
                        ObjectDelta focusChange = deltas.getFocusChange();
                        if (focusChange != null) {
                            metadataHelper.addAssignmentApprovalMetadata(focusChange, child.getTask(), result);
                        }
                        if (focusChange != null && focusChange.isAdd()) {
                            deltasToMerge.add(0, deltas);   // "add" must go first
                        } else {
                            deltasToMerge.add(deltas);
                        }
                    }
                }
            }

            for (ObjectTreeDeltas deltaToMerge : deltasToMerge) {
                LensFocusContext focusContext = rootContext.getFocusContext();
                ObjectDelta focusDelta = deltaToMerge.getFocusChange();
                if (focusDelta != null) {
                    LOGGER.trace("Adding delta to root model context; delta = {}", focusDelta.debugDumpLazily());
                    if (focusContext.getPrimaryDelta() != null && !focusContext.getPrimaryDelta().isEmpty()) {
                        focusContext.addPrimaryDelta(focusDelta);
                    } else {
                        focusContext.setPrimaryDelta(focusDelta);
                    }
                    changed = true;
                }
                Set<Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>>> entries = deltaToMerge.getProjectionChangeMapEntries();
                for (Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> entry : entries) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Adding projection delta to root model context; rsd = {}, delta = {}", entry.getKey(),
                                entry.getValue().debugDump());
                    }
                    ModelProjectionContext projectionContext = rootContext.findProjectionContext(entry.getKey());
                    if (projectionContext == null) {
                        // TODO more liberal treatment?
                        throw new IllegalStateException("No projection context for " + entry.getKey());
                    }
                    if (projectionContext.getPrimaryDelta() != null && !projectionContext.getPrimaryDelta().isEmpty()) {
                        projectionContext.addPrimaryDelta(entry.getValue());
                    } else {
                        projectionContext.setPrimaryDelta(entry.getValue());
                    }
                    changed = true;
                }
            }


            if (!rootContext.hasAnyPrimaryChange()) {
                rootContext = null; // deletes the model context
                changed = true;     // regardless of whether rootContext was changed or not
            }

            if (changed) {
                rootWfTask.storeModelContext(rootContext, true);
                rootWfTask.commitChanges(result);
            }

        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | ConfigurationException | ExpressionEvaluationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't aggregate resulting deltas from child workflow-monitoring tasks due to schema exception", e);
            status = TaskRunResultStatus.PERMANENT_ERROR;
        } catch (CommunicationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't aggregate resulting deltas from child workflow-monitoring tasks", e);
            status = TaskRunResultStatus.TEMPORARY_ERROR;
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
    //endregion

}
