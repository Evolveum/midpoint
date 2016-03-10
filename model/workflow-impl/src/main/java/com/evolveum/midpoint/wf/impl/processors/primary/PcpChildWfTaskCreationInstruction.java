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
import com.evolveum.midpoint.wf.impl.jobs.ProcessInstruction;
import com.evolveum.midpoint.wf.impl.jobs.WfTask;
import com.evolveum.midpoint.wf.impl.jobs.WfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalInstruction;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author mederly
 */
public class PcpChildWfTaskCreationInstruction<PI extends ProcessInstruction> extends WfTaskCreationInstruction<PrimaryChangeProcessorInstruction, PI> {

    protected PcpChildWfTaskCreationInstruction(ChangeProcessor changeProcessor, PI processInstruction) {
        super(changeProcessor, new PrimaryChangeProcessorInstruction(changeProcessor.getPrismContext()), processInstruction);
    }

	// useful shortcut
	public static PcpChildWfTaskCreationInstruction createItemApprovalInstruction(ChangeProcessor changeProcessor, String approvalTaskName,
			ApprovalRequest<?> approvalRequest) {
		ItemApprovalInstruction itemApprovalInstruction = new ItemApprovalInstruction();
		itemApprovalInstruction.setTaskName(approvalTaskName);
		itemApprovalInstruction.setApprovalSchema(approvalRequest.getApprovalSchema());
		PcpChildWfTaskCreationInstruction pcpjci = new PcpChildWfTaskCreationInstruction(changeProcessor, itemApprovalInstruction);
		return pcpjci;
	}

    public boolean isExecuteApprovedChangeImmediately() {
        return processorInstruction.isExecuteApprovedChangeImmediately();
    }

    public void prepareCommonAttributes(PrimaryChangeAspect aspect, ModelContext<?> modelContext, String objectOid, PrismObject<UserType> requester) throws SchemaException {

        setRequesterRef(requester);

		processorInstruction.setExecuteApprovedChangeImmediately(ModelExecuteOptions.isExecuteImmediatelyAfterApproval(((LensContext) modelContext).getOptions()));

		processorInstruction.createProcessorSpecificState().setChangeAspect(aspect.getClass().getName());

        if (isExecuteApprovedChangeImmediately()) {
            // actually, context should be emptied anyway; but to be sure, let's do it here as well
            setTaskModelContext(((PrimaryChangeProcessor) getChangeProcessor()).contextCopyWithNoDelta((LensContext) modelContext));
            setExecuteModelOperationHandler(true);
        }
    }

    @Deprecated
    public void setDeltasToProcess(ObjectDelta delta) {
        setDeltasToProcesses(new ObjectTreeDeltas(delta, getChangeProcessor().getPrismContext()));
    }

    public void setDeltasToProcesses(ObjectTreeDeltas objectTreeDeltas) {
        try {
            processorInstruction.createProcessorSpecificState().setDeltasToProcess(ObjectTreeDeltas.toObjectTreeDeltasType(objectTreeDeltas));
        } catch (SchemaException e) {
            throw new SystemException("Couldn't store primary delta(s) into the task variable due to schema exception", e);
        }
    }

	public String getAspectClassName() {
        return processorInstruction.createProcessorSpecificState().getChangeAspect();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.indentDebugDump(sb, indent);
        sb.append("PrimaryChangeProcessor ChildJobCreationInstruction: (execute approved change immediately = ")
                .append(isExecuteApprovedChangeImmediately())
                .append(")\n");
        sb.append(super.debugDump(indent+1));
        return sb.toString();
    }

}
