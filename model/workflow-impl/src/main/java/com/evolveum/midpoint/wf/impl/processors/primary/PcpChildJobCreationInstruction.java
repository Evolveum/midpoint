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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.wf.impl.jobs.Job;
import com.evolveum.midpoint.wf.impl.jobs.JobCreationInstruction;
import com.evolveum.midpoint.wf.impl.processes.common.StringHolder;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author mederly
 */
public class PcpChildJobCreationInstruction extends JobCreationInstruction {

    private boolean executeApprovedChangeImmediately;     // should the child job execute approved change immediately (i.e. executeModelOperationHandler must be set as well!)

    protected PcpChildJobCreationInstruction(ChangeProcessor changeProcessor) {
        super(changeProcessor);
    }

    protected PcpChildJobCreationInstruction(Job parentJob) {
        super(parentJob);
    }

    public static PcpChildJobCreationInstruction createInstruction(ChangeProcessor changeProcessor) {
        PcpChildJobCreationInstruction pcpjci = new PcpChildJobCreationInstruction(changeProcessor);
        prepareWfProcessChildJobInternal(pcpjci);
        return pcpjci;
    }

    public static PcpChildJobCreationInstruction createInstruction(Job parentJob) {
        PcpChildJobCreationInstruction pcpjci = new PcpChildJobCreationInstruction(parentJob);
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

        setRequesterOidInProcess(requester);
        setObjectOidInProcess(objectOid);

        setExecuteApprovedChangeImmediately(ModelExecuteOptions.isExecuteImmediatelyAfterApproval(((LensContext) modelContext).getOptions()));

        addProcessVariable(PcpProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_ASPECT, aspect.getClass().getName());
        addTaskVariable(getChangeProcessor().getWorkflowManager().getWfTaskUtil().getWfPrimaryChangeAspectPropertyDefinition(), aspect.getClass().getName());

        if (isExecuteApprovedChangeImmediately()) {
            // actually, context should be emptied anyway; but to be sure, let's do it here as well
            addTaskModelContext(((PrimaryChangeProcessor) getChangeProcessor()).contextCopyWithNoDelta((LensContext) modelContext));
            setExecuteModelOperationHandler(true);
        }
    }

    @Deprecated
    public void setDeltaProcessAndTaskVariables(ObjectDelta delta) {
//        try {
//            addProcessVariable(PcpProcessVariableNames.VARIABLE_MIDPOINT_DELTA, new StringHolder(DeltaConvertor.toObjectDeltaTypeXml(delta)));
//        } catch(JAXBException e) {
//            throw new SystemException("Couldn't store primary delta into the process variable due to JAXB exception", e);
//        } catch (SchemaException e) {
//            throw new SystemException("Couldn't store primary delta into the process variable due to schema exception", e);
//        }
//
//        try {
//            addTaskDeltasVariable(getChangeProcessor().getWorkflowManager().getWfTaskUtil().getWfDeltaToProcessPropertyDefinition(), delta);
//        } catch (SchemaException e) {
//            throw new SystemException("Couldn't store primary delta into the task variable due to schema exception", e);
//        }
//
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
            addTaskVariable(getChangeProcessor().getWorkflowManager().getWfTaskUtil().getWfDeltasToProcessPropertyDefinition(),
                    ObjectTreeDeltas.toObjectTreeDeltasType(objectTreeDeltas));
        } catch (SchemaException e) {
            throw new SystemException("Couldn't store primary delta(s) into the task variable due to schema exception", e);
        }
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
}
