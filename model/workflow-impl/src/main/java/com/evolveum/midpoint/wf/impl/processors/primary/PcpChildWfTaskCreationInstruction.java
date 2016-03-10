/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.wf.impl.jobs.WfTask;
import com.evolveum.midpoint.wf.impl.jobs.WfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRefImpl;
import com.evolveum.midpoint.wf.impl.processes.common.StringHolder;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_PROCESSOR_SPECIFIC_STATE;

/**
 * @author mederly
 */
public class PcpChildWfTaskCreationInstruction extends WfTaskCreationInstruction {

    private String aspectClassName;
    private ObjectTreeDeltasType deltasToProcess;
    private boolean executeApprovedChangeImmediately;     // should the child job execute approved change immediately (i.e. executeModelOperationHandler must be set as well!)

    protected PcpChildWfTaskCreationInstruction(ChangeProcessor changeProcessor) {
        super(changeProcessor);
    }

    protected PcpChildWfTaskCreationInstruction(WfTask parentWfTask) {
        super(parentWfTask);
    }

    public static PcpChildWfTaskCreationInstruction createInstruction(ChangeProcessor changeProcessor) {
        PcpChildWfTaskCreationInstruction pcpjci = new PcpChildWfTaskCreationInstruction(changeProcessor);
        prepareWfProcessChildJobInternal(pcpjci);
        return pcpjci;
    }

    public static PcpChildWfTaskCreationInstruction createInstruction(WfTask parentWfTask) {
        PcpChildWfTaskCreationInstruction pcpjci = new PcpChildWfTaskCreationInstruction(parentWfTask);
        prepareWfProcessChildJobInternal(pcpjci);
        return pcpjci;
    }

    public boolean isExecuteApprovedChangeImmediately() {
        return executeApprovedChangeImmediately;
    }

    public void setExecuteApprovedChangeImmediately(boolean executeApprovedChangeImmediately) {
        this.executeApprovedChangeImmediately = executeApprovedChangeImmediately;
    }

    public void prepareCommonAttributes(PrimaryChangeAspect aspect, ModelContext<?> modelContext, String objectOid, PrismObject<UserType> requester) throws SchemaException {

        setRequesterOidAndRefInProcess(requester);

        setExecuteApprovedChangeImmediately(ModelExecuteOptions.isExecuteImmediatelyAfterApproval(((LensContext) modelContext).getOptions()));

        addProcessVariable(PcpProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_ASPECT, aspect.getClass().getName());
        aspectClassName = aspect.getClass().getName();

        if (isExecuteApprovedChangeImmediately()) {
            // actually, context should be emptied anyway; but to be sure, let's do it here as well
            addTaskModelContext(((PrimaryChangeProcessor) getChangeProcessor()).contextCopyWithNoDelta((LensContext) modelContext));
            setExecuteModelOperationHandler(true);
        }
    }

    @Deprecated
    public void setDeltaProcessAndTaskVariables(ObjectDelta delta) {
        setObjectTreeDeltasProcessAndTaskVariables(new ObjectTreeDeltas(delta, getChangeProcessor().getPrismContext()));
    }

    public void setObjectTreeDeltasProcessAndTaskVariables(ObjectTreeDeltas objectTreeDeltas) {
        try {
            addProcessVariable(PcpProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TREE_DELTAS,
                    new StringHolder(ObjectTreeDeltas.toObjectTreeDeltasTypeXml(objectTreeDeltas)));
        } catch (SchemaException e) {
            throw new SystemException("Couldn't store primary delta(s) into the process variable due to schema exception", e);
        }

        try {
            deltasToProcess = ObjectTreeDeltas.toObjectTreeDeltasType(objectTreeDeltas);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't store primary delta(s) into the task variable due to schema exception", e);
        }
    }

    public void setObjectRefVariable(ObjectReferenceType ref, OperationResult result) {
        if (ref != null) {
            ref = getChangeProcessor().getWorkflowManager().getMiscDataUtil().resolveObjectReferenceName(ref, result);
            addProcessVariable(CommonProcessVariableNames.VARIABLE_OBJECT_REF, new LightweightObjectRefImpl(ref));
        } else {
            removeProcessVariable(CommonProcessVariableNames.VARIABLE_OBJECT_REF);
        }
    }

    public void setObjectRefVariable(ModelContext<?> modelContext, OperationResult result) {
        ObjectType focus = MiscDataUtil.getFocusObjectNewOrOld(modelContext);
        setObjectRefVariable(ObjectTypeUtil.createObjectRef(focus), result);
    }

    public void setTargetRefVariable(ObjectReferenceType ref, OperationResult result) {
        if (ref != null) {
            ref = getChangeProcessor().getWorkflowManager().getMiscDataUtil().resolveObjectReferenceName(ref, result);
            addProcessVariable(CommonProcessVariableNames.VARIABLE_TARGET_REF, new LightweightObjectRefImpl(ref));
        } else {
            removeProcessVariable(CommonProcessVariableNames.VARIABLE_TARGET_REF);
        }
    }

    public String getAspectClassName() {
        return aspectClassName;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.indentDebugDump(sb, indent);
        sb.append("PrimaryChangeProcessor ChildJobCreationInstruction: (execute approved change immediately = ")
                .append(executeApprovedChangeImmediately)
                .append(")\n");
        sb.append(super.debugDump(indent+1));
        return sb.toString();
    }

    @Override
    public void tailorTask(Task task) throws SchemaException {
        super.tailorTask(task);

        PrismContext prismContext = getChangeProcessor().getPrismContext();
        WfPrimaryChangeProcessorStateType state = new WfPrimaryChangeProcessorStateType();
        state.setChangeAspect(aspectClassName);
        state.setDeltasToProcess(deltasToProcess);
        state.asPrismContainerValue().setConcreteType(WfPrimaryChangeProcessorStateType.COMPLEX_TYPE);
        task.addModification(
                DeltaBuilder.deltaFor(TaskType.class, prismContext)
                        .item(F_WORKFLOW_CONTEXT, F_PROCESSOR_SPECIFIC_STATE).replace(state)
                        .asItemDelta());
    }
}
