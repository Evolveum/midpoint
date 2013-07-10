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

package com.evolveum.midpoint.wf.taskHandlers;

/**
 * @author mederly
 */

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.WfTaskUtil;
import com.evolveum.midpoint.wf.processors.primary.PrimaryChangeProcessor;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * This handler prepares model operation to be executed within the context of child task:
 * - prepares model operation context - currently, adds OID if necessary (if delta0 was 'add object' delta)
 *
 * @author mederly
 */

@Component
public class WfPrepareChildOperationTaskHandler implements TaskHandler {

    public static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/workflow/prepare-child-operation/handler-2";

    private static final Trace LOGGER = TraceManager.getTrace(WfPrepareChildOperationTaskHandler.class);

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    private WfConfiguration wfConfiguration;

    @PostConstruct
    public void init() {
        LOGGER.trace("Registering with taskManager as a handler for " + HANDLER_URI);
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public TaskRunResult run(Task task) {

        TaskRunResult.TaskRunResultStatus status = TaskRunResult.TaskRunResultStatus.FINISHED;

        LOGGER.trace("WfPrepareChildOperationTaskHandler starting... task = {}", task);

        try {

            OperationResult result = task.getResult();

            ModelContext modelContext = wfTaskUtil.retrieveModelContext(task, result);
            if (modelContext == null) {
                throw new IllegalStateException("There's no model context in child task; task = " + task);
            }

            // prepare deltaOut to be used

            List<ObjectDelta<Objectable>> deltasOut = wfTaskUtil.retrieveResultingDeltas(task);
            if (LOGGER.isTraceEnabled()) { dumpDeltaOut(deltasOut); }
            ObjectDelta deltaOut = ObjectDelta.summarize(deltasOut);

            if (deltaOut == null || deltaOut.isEmpty()) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("There's no primary delta in focus context; task = " + task + ", model context = " + modelContext.debugDump());
                    LOGGER.trace("We'll set skip model context processing property.");
                }

                wfTaskUtil.setSkipModelContextProcessingProperty(task, true, result);

            } else {

                setOidIfNeeded(deltaOut, task, result);         // fixes OID in deltaOut, if necessary

                if (deltaOut.getOid() == null || deltaOut.getOid().equals(PrimaryChangeProcessor.UNKNOWN_OID)) {
                    throw new IllegalStateException("Null or unknown OID in deltaOut: " + deltaOut.getOid());
                }

                // place deltaOut into model context

                ObjectDelta primaryDelta = modelContext.getFocusContext().getPrimaryDelta();
                if (primaryDelta == null || !primaryDelta.isModify()) {
                    throw new IllegalStateException("Object delta in model context in task " + task + " should have been empty or of MODIFY type, but it isn't; it is " + primaryDelta.debugDump());
                }

                modelContext.getFocusContext().setPrimaryDelta(deltaOut);

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Resulting model context to be stored into task {}:\n{}", task, modelContext.debugDump(0));
                }
                wfTaskUtil.storeModelContext(task, modelContext);
            }

            task.savePendingModifications(result);

        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't prepare child model context due to schema exception", e);
            status = TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't prepare child model context", e);
            status = TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
        } catch (ObjectAlreadyExistsException e) {
            LoggingUtils.logException(LOGGER, "Couldn't prepare child model context", e);
            status = TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
        } catch (CommunicationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't prepare child model context", e);
            status = TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR;
        } catch (ConfigurationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't prepare child model context", e);
            status = TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
        }

        TaskRunResult runResult = new TaskRunResult();
        runResult.setRunResultStatus(status);
        return runResult;
    }

    private void setOidIfNeeded(ObjectDelta deltaOut, Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {

        List<Task> prerequisities = task.listPrerequisiteTasks(result);

        if (prerequisities.isEmpty()) {
            return;         // this should not happen; however, if it happens, it means we have no source of OID available
        }

        if (prerequisities.size() > 1) {
            throw new IllegalStateException("Child task should have at most one prerequisite (task0); this one has " + prerequisities.size() + "; task = " + task);
        }

        Task task0 = prerequisities.get(0);
        Validate.isTrue(task0.isClosed(), "Task0 should be already closed; it is " + task0.getExecutionStatus());

        LensContext context0 = (LensContext) wfTaskUtil.retrieveModelContext(task0, result);
        if (context0 == null) {
            throw new IllegalStateException("There's no model context in task0; task0 = " + task);
        }

        String oidInTask0 = context0.getFocusContext().getOid();
        if (oidInTask0 == null) {
            throw new IllegalStateException("There's no focus OID in model context in task0; task0 = " + task + "; context = " + context0.debugDump());
        }

        String currentOid = deltaOut.getOid();
        LOGGER.trace("Object OID in task0 = " + oidInTask0 + ", current OID in this task = " + currentOid);

        if (PrimaryChangeProcessor.UNKNOWN_OID.equals(currentOid)) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Replaced delta OID with " + oidInTask0 + " in task " + task);
            }
            deltaOut.setOid(oidInTask0);
        } else {
            if (!oidInTask0.equals(currentOid)) {
                throw new IllegalStateException("Object OID in partial child task (" + currentOid + ") differs from OID in task0 (" + oidInTask0 + ")");
            }
            LOGGER.trace("Delta OID is current, we will not change it.");
        }
    }

    private void dumpDeltaOut(List<ObjectDelta<Objectable>> deltaOut) {
        LOGGER.trace("deltaOut has " + deltaOut.size() + " modifications:");
        for (ObjectDelta<Objectable> delta : deltaOut) {
            LOGGER.trace(delta.debugDump());
        }
    }

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

}
