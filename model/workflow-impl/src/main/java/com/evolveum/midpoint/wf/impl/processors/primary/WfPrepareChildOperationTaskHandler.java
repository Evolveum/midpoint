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

/**
 * @author mederly
 */

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.tasks.WfTask;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This handler prepares model operation to be executed within the context of child task:
 * - prepares model operation context, filling it up with approved delta(s)
 *
 * @author mederly
 */

@Component
public class WfPrepareChildOperationTaskHandler implements TaskHandler {

    public static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/workflow/prepare-child-operation/handler-3";

    private static final Trace LOGGER = TraceManager.getTrace(WfPrepareChildOperationTaskHandler.class);

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

    //region Body
    @SuppressWarnings("unchecked")
    @Override
    public TaskRunResult run(Task task) {

        TaskRunResult.TaskRunResultStatus status = TaskRunResult.TaskRunResultStatus.FINISHED;

        LOGGER.trace("WfPrepareChildOperationTaskHandler starting... task = {}", task);

        try {

            WfTask wfTask = wfTaskController.recreateWfTask(task);

            OperationResult result = task.getResult();

            ModelContext<?> modelContext = wfTask.retrieveModelContext(result);
            if (modelContext == null) {
                throw new IllegalStateException("There's no model context in child task; task = " + task);
            }

            // prepare deltaOut to be used

            ObjectTreeDeltas deltasOut = wfTask.retrieveResultingDeltas();
            if (LOGGER.isTraceEnabled()) { dumpDeltaOut(deltasOut); }

            if (deltasOut == null || deltasOut.isEmpty()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("There's no primary delta in focus context; we'll delete model operation context. Task = {}, model context:\n{}", task, modelContext.debugDump());
                }
                wfTask.deleteModelOperationContext();
            } else {
                // place deltaOut into model context
                ObjectDelta focusChange = deltasOut.getFocusChange();
                metadataHelper.addAssignmentApprovalMetadata(focusChange, task, result);
                modelContext.getFocusContext().setPrimaryDelta(focusChange);
                Set<Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>>> entries = deltasOut.getProjectionChangeMapEntries();
                for (Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> entry : entries) {
                    // TODO what if projection context does not exist?
                    modelContext.findProjectionContext(entry.getKey()).setPrimaryDelta(entry.getValue());
                }
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Resulting model context to be stored into task {}:\n{}", task, modelContext.debugDump(0));
                }
                wfTask.storeModelContext(modelContext, true);
            }
            task.savePendingModifications(result);
        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | ConfigurationException | ExpressionEvaluationException | RuntimeException | Error e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't prepare child model context", e);
            status = TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
        } catch (CommunicationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't prepare child model context", e);
            status = TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR;
        }
        TaskRunResult runResult = new TaskRunResult();
        runResult.setRunResultStatus(status);
        return runResult;
    }

    // TODO implement correctly
    private void dumpDeltaOut(ObjectTreeDeltas<?> deltasOut) {
        List<ObjectDelta<? extends ObjectType>> deltaOut = deltasOut != null ? deltasOut.getDeltaList() : new ArrayList<>();
        LOGGER.trace("deltaOut has {} modifications:", deltaOut.size());
        for (ObjectDelta<?> delta : deltaOut) {
            LOGGER.trace("{}", delta.debugDump());
        }
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
