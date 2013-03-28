package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.processes.ProcessWrapper;
import com.evolveum.midpoint.wf.processes.UserChangeStartProcessInstruction;
import com.evolveum.midpoint.wf.processes.addroles.AddRoleAssignmentWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Deals with approving primary changes related to a user. Typical such changes are adding of a role or an account.
 *
 * @author mederly
 */
public class PrimaryUserChangeProcessor implements ChangeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(PrimaryUserChangeProcessor.class);

    private List<ProcessWrapper> processWrappers = new ArrayList<ProcessWrapper>();

    private WorkflowManager workflowManager;
    private WfCore wfCore;
    private WfTaskUtil wfTaskUtil;

    public PrimaryUserChangeProcessor(WorkflowManager workflowManager) {

        this.workflowManager = workflowManager;
        this.wfCore = workflowManager.getWfCore();
        this.wfTaskUtil = workflowManager.getWfCore().getWfTaskUtil();

        processWrappers.add(new AddRoleAssignmentWrapper(workflowManager));
    }

    @Override
    public HookOperationMode startProcessesIfNeeded(ModelContext context, Task task, OperationResult result) throws SchemaException {

        if (context.getState() != ModelState.PRIMARY || context.getFocusContext() == null) {
            return null;
        }

        ObjectDelta<? extends ObjectType> change = context.getFocusContext().getPrimaryDelta();
        if (change == null) {
            return null;
        }

        // let's check whether we deal with a user

        if (!UserType.class.isAssignableFrom(change.getObjectTypeClass())) {        // todo works with ADD as well?
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("change object type class is not a UserType (it is " + change.getObjectTypeClass() + "), exiting " + this.getClass().getSimpleName());
            }
            return null;
        }

        // examine the request using process wrappers

        ObjectDelta<UserType> changeBeingDecomposed = (ObjectDelta<UserType>) change.clone();
        List<UserChangeStartProcessInstruction> startProcessInstructions =
                gatherStartProcessInstructions(context, changeBeingDecomposed, task, result);

        // start the process(es)

        ((LensContext) context).replacePrimaryFocusDelta(changeBeingDecomposed);

        return startProcesses(startProcessInstructions, context, task, result);
    }

    private List<UserChangeStartProcessInstruction> gatherStartProcessInstructions(ModelContext context, ObjectDelta<UserType> changeBeingDecomposed, Task task, OperationResult result) {
        List<UserChangeStartProcessInstruction> startProcessInstructions = new ArrayList<UserChangeStartProcessInstruction>();

        for (ProcessWrapper wrapper : processWrappers) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Calling wrapper " + wrapper.getClass() + "...");
            }
            List<UserChangeStartProcessInstruction> processes = wrapper.prepareProcessesToStart(context, changeBeingDecomposed, task, result);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Wrapper " + wrapper.getClass() + " returned the following process start instructions (count: " + processes.size() + "):");
                for (UserChangeStartProcessInstruction startProcessInstruction : processes) {
                    LOGGER.trace(startProcessInstruction.debugDump(0));
                }
            }
            if (processes != null) {
                startProcessInstructions.addAll(processes);
            }
        }
        return startProcessInstructions;
    }

    private HookOperationMode startProcesses(List<UserChangeStartProcessInstruction> startProcessInstructions, ModelContext context, Task task, OperationResult result) {

        if (startProcessInstructions.isEmpty()) {
            LOGGER.trace("There are no workflow processes to be started, exiting.");
            return null;
        }

        Throwable failReason;

        try {

            wfCore.prepareRootTask(context, task, result);

            for (UserChangeStartProcessInstruction instruction : startProcessInstructions) {

                Task childTask = task.createSubtask();
                wfCore.prepareChildTaskNoSave(instruction, childTask, result);

                if (!instruction.isNoProcess()) {
                    wfTaskUtil.storeDeltasToProcess(instruction.getDeltas(), childTask);        // will be processed by wrapper on wf process termination
                } else {
                    // we have to put deltas into model context, as it will be processed directly by ModelOperationTaskHandler
                    LensContext contextCopy = ((LensContext) context).clone();
                    contextCopy.replacePrimaryFocusDeltas(instruction.getDeltas());
                    wfTaskUtil.storeModelContext(childTask, contextCopy, result);
                }
                wfCore.saveChildTask(childTask, result);

                if (!instruction.isNoProcess()) {
                    wfCore.startProcessInstance(instruction, task, result);
                }
            }

            return HookOperationMode.BACKGROUND;

        } catch (SchemaException e) {
            failReason = e;
        } catch (ObjectNotFoundException e) {
            failReason = e;
        } catch (RuntimeException e) {
            failReason = e;
        }

        LoggingUtils.logException(LOGGER, "Workflow process(es) could not be started", failReason);
        result.recordFatalError("Workflow process(es) could not be started: " + failReason, failReason);
        return HookOperationMode.ERROR;

        // todo rollback - at least close open tasks, maybe stop workflow process instances
    }

}
