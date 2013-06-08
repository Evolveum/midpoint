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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfTaskUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.List;

/**
 * This handler propagates object OID of newly added object to dependent tasks.
 * (Quite a hack for now.)
 *
 * @author mederly
 */

@Component
public class WfPropagateTaskObjectReferenceTaskHandler implements TaskHandler {

    public static final String HANDLER_URI = "http://midpoint.evolveum.com/wf-propagate-task-object-reference";

    private static final Trace LOGGER = TraceManager.getTrace(WfPropagateTaskObjectReferenceTaskHandler.class);

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @PostConstruct
    public void init() {
        LOGGER.trace("Registering with taskManager as a handler for " + HANDLER_URI);
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public TaskRunResult run(Task task) {

        TaskRunResult.TaskRunResultStatus status = TaskRunResult.TaskRunResultStatus.FINISHED;

        LOGGER.trace("WfPropagateTaskObjectReferenceTaskHandler starting... task = {}", task);

        OperationResult result = task.getResult().createSubresult(WfPropagateTaskObjectReferenceTaskHandler.class + ".run");

        ModelContext modelContext;
        try {
            modelContext = wfTaskUtil.retrieveModelContext(task, result);
            if (modelContext == null) {
                throw new IllegalStateException("There's no model context in the task; task = " + task);
            }
        } catch (SchemaException e) {
            return reportException("Couldn't retrieve model context from task " + task, task, result, e);
        } catch (ObjectNotFoundException e) {
            return reportException("Couldn't retrieve model context from task " + task, task, result, e);
        } catch (CommunicationException e) {
            return reportException("Couldn't retrieve model context from task " + task, task, result, TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR, e);
        } catch (ConfigurationException e) {
            return reportException("Couldn't retrieve model context from task " + task, task, result, e);
        }

        String oid = ((LensContext) modelContext).getFocusContext().getOid();
        if (oid == null) {
            LOGGER.warn("No object OID in task " + task);
        } else {

            Class typeClass = ((LensContext) modelContext).getFocusContext().getObjectTypeClass();
            QName type = typeClass != null ? JAXBUtil.getTypeQName(typeClass) : null;
            if (type == null) {
                LOGGER.warn("Unknown type of object " + oid + " in task " + task);
            } else {

                ObjectReferenceType objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setType(type);
                objectReferenceType.setOid(oid);

                if (task.getObjectRef() == null) {
                    task.setObjectRef(objectReferenceType);
                } else {
                    LOGGER.warn("object reference in task " + task + " is already set, although it shouldn't be");
                }

                List<Task> dependents = null;
                try {
                    dependents = task.listDependents(result);
                    dependents.add(task.getParentTask(result));
                } catch (SchemaException e) {
                    return reportException("Couldn't get task dependents from task " + task, task, result, e);
                } catch (ObjectNotFoundException e) {
                    return reportException("Couldn't get task dependents from task " + task, task, result, e);
                }

                for (Task dependent : dependents) {
                    if (dependent.getObjectRef() == null) {
                        if (dependent.getObjectRef() == null) {
                            try {
                                dependent.setObjectRefImmediate(objectReferenceType, result);
                            } catch (ObjectNotFoundException e) {
                                // note we DO NOT return, because we want to set all references we can
                                reportException("Couldn't set object reference on task " + dependent, task, result, e);
                            } catch (SchemaException e) {
                                reportException("Couldn't set object reference on task " + dependent, task, result, e);
                            } catch (ObjectAlreadyExistsException e) {
                                reportException("Couldn't set object reference on task " + dependent, task, result, e);
                            }
                        } else {
                            LOGGER.warn("object reference in task " + task + " is already set, although it shouldn't be");
                        }
                    }
                }
            }
        }

        TaskRunResult runResult = new TaskRunResult();
        runResult.setRunResultStatus(status);
        runResult.setOperationResult(task.getResult());
        return runResult;
    }

    private TaskRunResult reportException(String message, Task task, OperationResult result, Throwable cause) {
        return reportException(message, task, result, TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR, cause);
    }

    private TaskRunResult reportException(String message, Task task, OperationResult result, TaskRunResult.TaskRunResultStatus status, Throwable cause) {

        LoggingUtils.logException(LOGGER, message, cause);
        result.recordFatalError(message, cause);

        TaskRunResult runResult = new TaskRunResult();
        runResult.setRunResultStatus(status);
        runResult.setOperationResult(task.getResult());
        return runResult;
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
