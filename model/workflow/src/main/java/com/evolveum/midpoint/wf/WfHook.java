/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf;

import java.util.*;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.wf.activiti.Idm2Activiti;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.messages.StartProcessCommand;
import com.evolveum.midpoint.wf.wrappers.ProcessWrapper;
import com.evolveum.midpoint.wf.wrappers.StartProcessInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.springframework.stereotype.Component;

/**
 * Provides an interface between the model and the workflow engine.
 * (1) catches hook calls and determines which ones require workflow invocation
 * (2) (asynchronously) receives results from the workflow process and passes them back to model;
 *     resulting e.g. in continuing model operation that has been approved by workflow process
 *
 * @author mederly
 */
@Component
public class WfHook implements ChangeHook {

    public static final String WORKFLOW_HOOK_URI = "http://midpoint.evolveum.com/model/workflow-hook-1";

    private static final Trace LOGGER = TraceManager.getTrace(WfHook.class);

    @Autowired(required = true)
    private TaskManager taskManager;

    @Autowired(required = true)
    private WfTaskUtil taskUtil;

    @Autowired(required = true)
    private Idm2Activiti idm2Activiti;

    private List<ProcessWrapper> wrappers = new ArrayList<ProcessWrapper>();

    @Override
    public HookOperationMode invoke(ModelContext context, Task task, OperationResult result) {

        LensContext<UserType,AccountShadowType> lensContext = (LensContext<UserType,AccountShadowType>) context;

        LOGGER.info("=====================================================================");
        LOGGER.info("WfHook invoked in state " + context.getState() + " (wave " + lensContext.getWave() + ", max " + lensContext.getMaxWave() + "):");

        ModelContext<UserType, AccountShadowType> usContext = (ModelContext<UserType, AccountShadowType>) context;

        ObjectDelta pdelta = usContext.getFocusContext().getPrimaryDelta();
        ObjectDelta sdelta = usContext.getFocusContext().getSecondaryDelta();
        LOGGER.info("Primary delta: " + (pdelta == null ? "(null)" : pdelta.debugDump()));
        LOGGER.info("Secondary delta: " + (sdelta == null ? "(null)" : sdelta.debugDump()));

        LOGGER.info("Projection contexts: " + usContext.getProjectionContexts().size());
        for (ModelProjectionContext mpc : usContext.getProjectionContexts()) {
            ObjectDelta ppdelta = mpc.getPrimaryDelta();
            ObjectDelta psdelta = mpc.getSecondaryDelta();
            LOGGER.info(" - Primary delta: " + (ppdelta == null ? "(null)" : ppdelta.debugDump()));
            LOGGER.info(" - Secondary delta: " + (psdelta == null ? "(null)" : psdelta.debugDump()));
            LOGGER.info(" - Sync delta:" + (mpc.getSyncDelta() == null ? "(null)" : mpc.getSyncDelta().debugDump()));
        }

        return HookOperationMode.FOREGROUND;
    }

//    @Override
//    public HookOperationMode preChangePrimary(Collection<ObjectDelta<? extends ObjectType>> changes, Task task,
//                                              OperationResult result) {
//
//        Validate.notNull(changes);
//        Validate.notNull(task);
//        Validate.notNull(result);
//
//
//
//        if (changes.size() != 1) {
//            return reportHookError(result, "Invalid number of objectDeltas in preChangePrimary (" + changes.size() + "), expected 1");
//        }
//
//        return executeProcessStart(ModelState.PRIMARY, changes, task, result);
//    }

    public void registerWfProcessWrapper(ProcessWrapper starter) {
        LOGGER.trace("Registering process wrapper: " + starter.getClass().getName());
        wrappers.add(starter);
    }

    @Override
    public void postChange(Collection<ObjectDelta<? extends ObjectType>> changes, Task task, OperationResult result) {
        LOGGER.info("=====================================================================");
        LOGGER.info("postChange:\n" + dump(changes));
        for (ObjectDelta<?> change : changes)
            LOGGER.trace(change.debugDump());
    }


    private HookOperationMode executeProcessStart(ModelContext context, Task task, OperationResult result) {

        for (ProcessWrapper wrapper : wrappers) {
            LOGGER.debug("Trying wrapper: " + wrapper.getClass().getName());
            StartProcessInstruction startCommand = wrapper.startProcessIfNeeded(context, task, result);
            if (startCommand != null) {
                LOGGER.debug("Wrapper " + wrapper.getClass().getName() + " prepared the following wf process start command: " + startCommand);
                try {
                    startProcessInstance(startCommand, wrapper, task, result);
                } catch(Exception e) { // TODO better error handling here
                    LoggingUtils.logException(LOGGER, "Workflow process instance couldn't be started", e);
                    return HookOperationMode.ERROR;
                }
                return HookOperationMode.BACKGROUND;
            }
        }

        LOGGER.debug("No wrapper served this request, returning the FOREGROUND flag.");
        return HookOperationMode.FOREGROUND;
    }

    private String dump(Collection<ObjectDelta<? extends ObjectType>> changes) {
        StringBuffer sb = new StringBuffer();
        for (ObjectDelta<?> change : changes) {
            sb.append(change.debugDump());
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * A convenience method for reporting errors in wf hooks.
     *
     * @param result
     * @param message
     * @return
     */
    private HookOperationMode reportHookError(OperationResult result, String message) {
        LOGGER.error(message);
        result.recordFatalError(message);
        return HookOperationMode.ERROR;
    }

    /**
     * Starts a process instance in WfMS.
     */
    private void startProcessInstance(StartProcessInstruction startInstruction, ProcessWrapper wrapper, Task task,
            OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        LOGGER.trace(" === startProcessInstance starting ===");

        if (!task.isTransient()) {
            throw new IllegalStateException("Workflow-related task should be transient but it is persistent; task = " + task);
        }

        // first, create in-memory task instance
        if (startInstruction.isSimple()) {
            taskUtil.prepareActiveTask(task, startInstruction.getTaskName(), parentResult);
        } else {
            taskUtil.preparePassiveTask(task, startInstruction.getTaskName(), parentResult);
        }

        taskUtil.setProcessWrapper(task, wrapper);

        taskManager.switchToBackground(task, parentResult);

        // taskUtil.setInvocationType(task, invtype, parentResult);

        // perhaps more useful would be state 'workflow process instance creation HAS BEEN requested';
        // however, if we record process state AFTER the request is sent, it is possible
        // that the response would come even before we log the request
        taskUtil.recordProcessState(task, "Workflow process instance creation is being requested.", "", null, parentResult);

        // prepare and send the start process instance command
        StartProcessCommand spc = new StartProcessCommand();
        spc.setTaskOid(task.getOid());
        spc.setProcessName(startInstruction.getProcessName());
        spc.setSendStartConfirmation(startInstruction.isSimple());	// for simple processes we should get wrapper-generated start events
        spc.setVariablesFrom(startInstruction.getProcessVariables());

        try {
            idm2Activiti.idm2activiti(spc);
        } catch (RuntimeException e) {
            LoggingUtils.logException(LOGGER,
                    "Couldn't send a request to start a process instance to workflow management system", e);
            taskUtil.recordProcessState(task, "Workflow process instance creation could not be requested: " + e, "", null, parentResult);
            parentResult.recordPartialError("Couldn't send a request to start a process instance to workflow management system", e);
        }

        // final
        parentResult.recordSuccessIfUnknown();

        LOGGER.trace(" === startProcessInstance ending ===");
    }

    /*
      * Post-processing
      *
      * For active tasks:
      *  - we throw away this wrapper, so the ModelOperationTaskHandler (next one on wrapper stack) will be invoked
      * For passive tasks:
      *  - we change task status to RUNNING, RELEASED, so that TaskManager will invoke WorkflowHookTaskHandler
      *
      * FIXME: correctly deal with the situation when more than one message with 'answer' present arrives
      * (probably do not invoke post-processing when task executionStatus == CLOSED)
      */

    void finishProcessing(ProcessEvent event, Task task, OperationResult result)
    {
        if (task == null) {
            try {
                task = taskManager.getTask(event.getTaskOid(), result);
            } catch (ObjectNotFoundException e) {   // todo: fixme - temporary "solution"
                throw new SystemException(e);
            } catch (SchemaException e) {
                throw new SystemException(e);
            }
        }

        ProcessWrapper wrapper = taskUtil.getProcessWrapper(task, wrappers);
        wrapper.finishProcess(event, task, result);


//		if (task.getExecutionStatus() != TaskExecutionStatus.RUNNING)

//		// let us mark the task result as SUCCESS
//		OperationResult or = task.getResult();		// 'or' should really be non-null here
//		if (or != null) {
//			or.recordSuccess();
//			taskUtil.setTaskResult(task.getOid(), or);
//		}
    }

}
