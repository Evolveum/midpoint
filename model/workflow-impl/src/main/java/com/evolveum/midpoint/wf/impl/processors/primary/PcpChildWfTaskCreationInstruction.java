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
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalSchema;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalSpecificContent;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.tasks.ProcessSpecificContent;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskCreationInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * @author mederly
 */
public class PcpChildWfTaskCreationInstruction<PI extends ProcessSpecificContent> extends WfTaskCreationInstruction<PrimaryChangeProcessorSpecificContent, PI> {

	private static final Trace LOGGER = TraceManager.getTrace(PcpChildWfTaskCreationInstruction.class);

	protected PcpChildWfTaskCreationInstruction(ChangeProcessor changeProcessor, PI processInstruction) {
        super(changeProcessor, new PrimaryChangeProcessorSpecificContent(changeProcessor.getPrismContext()), processInstruction);
    }

	public static PcpChildWfTaskCreationInstruction<ItemApprovalSpecificContent> createItemApprovalInstruction(
			ChangeProcessor changeProcessor, String approvalTaskName, @NotNull ApprovalSchema approvalSchema,
			@NotNull ApprovalSchemaType approvalSchemaType, SchemaAttachedPolicyRulesType attachedPolicyRules) {
		ItemApprovalSpecificContent itemApprovalInstruction = new ItemApprovalSpecificContent(
				changeProcessor.getPrismContext(), approvalTaskName, approvalSchema, approvalSchemaType, attachedPolicyRules);
		return new PcpChildWfTaskCreationInstruction<>(changeProcessor, itemApprovalInstruction);
	}

    public boolean isExecuteApprovedChangeImmediately() {
        return processorContent.isExecuteApprovedChangeImmediately();
    }

    public void prepareCommonAttributes(PrimaryChangeAspect aspect, ModelContext<?> modelContext, PrismObject<UserType> requester) throws SchemaException {

		if (requester != null) {
			setRequesterRef(requester);
		}

		processorContent.setExecuteApprovedChangeImmediately(ModelExecuteOptions.isExecuteImmediatelyAfterApproval(((LensContext) modelContext).getOptions()));

		processorContent.createProcessorSpecificState().setChangeAspect(aspect.getClass().getName());

        if (isExecuteApprovedChangeImmediately()) {
            // actually, context should be emptied anyway; but to be sure, let's do it here as well
            setTaskModelContext(((PrimaryChangeProcessor) getChangeProcessor()).contextCopyWithNoDelta((LensContext) modelContext));
            setExecuteModelOperationHandler(true);
        }

		WfProcessCreationEventType event = new WfProcessCreationEventType();
        event.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        if (requester != null) {
			event.setInitiatorRef(ObjectTypeUtil.createObjectRef(requester));
		}
		event.setBusinessContext(((LensContext) modelContext).getRequestBusinessContext());
        wfContext.getEvent().add(event);
    }

    public <F extends FocusType> void setDeltasToProcess(ObjectDelta<F> delta) {
        setDeltasToProcesses(new ObjectTreeDeltas<>(delta, getChangeProcessor().getPrismContext()));
    }

    public void setDeltasToProcesses(ObjectTreeDeltas objectTreeDeltas) {
        try {
            processorContent.createProcessorSpecificState().setDeltasToProcess(ObjectTreeDeltas.toObjectTreeDeltasType(objectTreeDeltas));
        } catch (SchemaException e) {
            throw new SystemException("Couldn't store primary delta(s) into the task variable due to schema exception", e);
        }
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
