package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.processes.ProcessWrapper;
import com.evolveum.midpoint.wf.processes.StartProcessInstruction;
import com.evolveum.midpoint.wf.processes.UserChangeStartProcessInstruction;
import com.evolveum.midpoint.wf.processes.addroles.AddRoleAssignmentWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

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

    public PrimaryUserChangeProcessor(WorkflowManager workflowManager) {

        this.workflowManager = workflowManager;
        this.wfCore = workflowManager.getWfCore();

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

    private HookOperationMode startProcesses(List<UserChangeStartProcessInstruction> startProcessInstructions, ModelContext context, Task task, OperationResult result) throws SchemaException {

        if (startProcessInstructions.isEmpty()) {
            LOGGER.trace("There are no workflow processes to be started, exiting.");
            return null;
        }

        wfCore.prepareRootTask(context, task, result);

        for (UserChangeStartProcessInstruction instruction : startProcessInstructions) {
            startProcessInstance(instruction, task, result);
        }

        return HookOperationMode.BACKGROUND;

        //
//
//        /*
//         *  We must split the original delta to a sequence of deltas (delta0, delta1, ..., deltaN).
//         *  Delta0 will contain changes that can be executed without approval; delta1-N require approvals.
//         */
//
//
    }


    private void startProcessInstance(UserChangeStartProcessInstruction instruction, Task task, OperationResult result) {
        Task child = prepareChildTask(instruction, task, result);

    }

    private Task prepareChildTask(UserChangeStartProcessInstruction instruction, Task task, OperationResult result) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }


}
