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
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.processors.StartInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * @author mederly
 */
public class PcpStartInstruction extends StartInstruction {

	@SuppressWarnings("unused")
	private static final Trace LOGGER = TraceManager.getTrace(PcpStartInstruction.class);

	private ObjectTreeDeltas<?> deltasToProcess;
	private boolean executeApprovedChangeImmediately;     // should the child job execute approved change immediately (i.e. executeModelOperationHandler must be set as well!)


	protected PcpStartInstruction(@NotNull ChangeProcessor changeProcessor, @NotNull String archetypeOid) {
        super(changeProcessor, archetypeOid);
		WfPrimaryChangeProcessorStateType state = new WfPrimaryChangeProcessorStateType(getPrismContext());
		state.setProcessor(changeProcessor.getClass().getName());
		getWfContext().setProcessorSpecificState(state);
    }

	public static PcpStartInstruction createItemApprovalInstruction(
			ChangeProcessor changeProcessor, String approvalTaskName,
			@NotNull ApprovalSchemaType approvalSchemaType, SchemaAttachedPolicyRulesType attachedPolicyRules) {
		PrismContext prismContext = changeProcessor.getPrismContext();
		ItemApprovalProcessStateType processState = new ItemApprovalProcessStateType(prismContext)
				.approvalSchema(approvalSchemaType)
				.policyRules(attachedPolicyRules);
		PcpStartInstruction instruction = new PcpStartInstruction(changeProcessor,
				SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value());
		instruction.setProcessState(processState);
		return instruction;
	}

	public static PcpStartInstruction createEmpty(ChangeProcessor changeProcessor, @NotNull String archetypeOid) {
		return new PcpStartInstruction(changeProcessor, archetypeOid);
	}


	public boolean isExecuteApprovedChangeImmediately() {
        return executeApprovedChangeImmediately;
    }

    public void prepareCommonAttributes(PrimaryChangeAspect aspect, ModelContext<?> modelContext, PrismObject<UserType> requester) {

		if (requester != null) {
			setRequesterRef(requester);
		}

		executeApprovedChangeImmediately = ModelExecuteOptions.isExecuteImmediatelyAfterApproval(modelContext.getOptions());

		getProcessorSpecificState().setChangeAspect(aspect.getClass().getName());

		CaseCreationEventType event = new CaseCreationEventType(getPrismContext());
        event.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        if (requester != null) {
			event.setInitiatorRef(ObjectTypeUtil.createObjectRef(requester, getPrismContext()));
			// attorney does not need to be set here (for now)
		}
		event.setBusinessContext(((LensContext) modelContext).getRequestBusinessContext());
        aCase.getEvent().add(event);
    }

    @NotNull
	private WfPrimaryChangeProcessorStateType getProcessorSpecificState() {
		return (WfPrimaryChangeProcessorStateType) getWfContext().getProcessorSpecificState();
	}

	public void setDeltasToProcess(ObjectDelta<? extends ObjectType> delta) throws SchemaException {
        setDeltasToProcess(new ObjectTreeDeltas<>(delta, getChangeProcessor().getPrismContext()));
    }

    public void setDeltasToProcess(ObjectTreeDeltas objectTreeDeltas) throws SchemaException {
		deltasToProcess = objectTreeDeltas;
        getProcessorSpecificState().setDeltasToProcess(ObjectTreeDeltas.toObjectTreeDeltasType(objectTreeDeltas));
    }

    public void setResultingDeltas(ObjectTreeDeltas objectTreeDeltas) throws SchemaException {
        getProcessorSpecificState().setResultingDeltas(ObjectTreeDeltas.toObjectTreeDeltasType(objectTreeDeltas));
    }

	@Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.indentDebugDump(sb, indent);
        sb.append("PrimaryChangeProcessor StartInstruction: (execute approved change immediately = ")
                .append(isExecuteApprovedChangeImmediately())
                .append(")\n");
        sb.append(super.debugDump(indent+1));
//		if (getProcessContent() instanceof ItemApprovalSpecificContent) {
//			ItemApprovalSpecificContent iasc = (ItemApprovalSpecificContent) getProcessContent();
//			PrismContext prismContext = changeProcessor.getPrismContext();
//			DebugUtil.debugDumpWithLabel(sb, "Approval schema", iasc.approvalSchemaType.asPrismContainerValue(), indent+1);
//			DebugUtil.debugDumpWithLabel(sb, "Attached rules", PrismUtil.serializeQuietly(prismContext, iasc.policyRules), indent+1);
//		}
		return sb.toString();
    }

	public boolean isObjectCreationInstruction() {
		return deltasToProcess != null && deltasToProcess.getFocusChange() != null && deltasToProcess.getFocusChange().isAdd();
	}

	public ItemApprovalProcessStateType getItemApprovalProcessState() {
		WfProcessSpecificStateType state = getWfContext().getProcessSpecificState();
		if (state == null || state instanceof ItemApprovalProcessStateType) {
			return (ItemApprovalProcessStateType) state;
		} else {
			throw new IllegalStateException("Expected " + ItemApprovalProcessStateType.class + " but got " + state.getClass());
		}
	}
}
