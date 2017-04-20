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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.wf.impl.processes.BaseProcessMidPointInterface;
import com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpWfTask;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskCreationInstruction;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames.*;

/**
 * @author mederly
 */
@Component
public class ItemApprovalProcessInterface extends BaseProcessMidPointInterface {

	private static final Trace LOGGER = TraceManager.getTrace(ItemApprovalProcessInterface.class);

	public static final String PROCESS_DEFINITION_KEY = "ItemApproval";

    public void prepareStartInstruction(WfTaskCreationInstruction instruction) {
        instruction.setProcessName(PROCESS_DEFINITION_KEY);
        instruction.setSimple(false);
        instruction.setSendStartConfirmation(true);
        instruction.setProcessInterfaceBean(this);

        if (LOGGER.isDebugEnabled() && instruction instanceof PcpChildWfTaskCreationInstruction) {
			PcpChildWfTaskCreationInstruction instr = (PcpChildWfTaskCreationInstruction) instruction;
			LOGGER.debug("About to start approval process instance '{}'", instr.getProcessInstanceName());
			if (instr.getProcessContent() instanceof ItemApprovalSpecificContent) {
				ItemApprovalSpecificContent iasc = (ItemApprovalSpecificContent) instr.getProcessContent();
				LOGGER.debug("Approval schema XML:\n{}", PrismUtil.serializeQuietlyLazily(prismContext, iasc.approvalSchemaType));
				LOGGER.debug("Attached rules:\n{}", PrismUtil.serializeQuietlyLazily(prismContext, iasc.policyRules));
			}
		}
    }

    @Override
	public WorkItemResultType extractWorkItemResult(Map<String, Object> variables) {
	    Boolean wasCompleted = ActivitiUtil.getVariable(variables, VARIABLE_WORK_ITEM_WAS_COMPLETED, Boolean.class, prismContext);
	    if (BooleanUtils.isNotTrue(wasCompleted)) {
		    return null;
	    }
		WorkItemResultType result = new WorkItemResultType();
		result.setOutcome(ActivitiUtil.getVariable(variables, FORM_FIELD_OUTCOME, String.class, prismContext));
		result.setComment(ActivitiUtil.getVariable(variables, FORM_FIELD_COMMENT, String.class, prismContext));
		String additionalDeltaString = ActivitiUtil.getVariable(variables, FORM_FIELD_ADDITIONAL_DELTA, String.class, prismContext);
		boolean isApproved = ApprovalUtils.isApproved(result);
		if (isApproved && StringUtils.isNotEmpty(additionalDeltaString)) {
			try {
				ObjectDeltaType additionalDelta = prismContext.parserFor(additionalDeltaString).parseRealValue(ObjectDeltaType.class);
				ObjectTreeDeltasType treeDeltas = new ObjectTreeDeltasType();
				treeDeltas.setFocusPrimaryDelta(additionalDelta);
				result.setAdditionalDeltas(treeDeltas);
			} catch (SchemaException e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse delta received from the activiti form:\n{}", e, additionalDeltaString);
				throw new SystemException("Couldn't parse delta received from the activiti form: " + e.getMessage(), e);
			}
		}
		return result;
    }

	@Override
	public WfProcessSpecificWorkItemPartType extractProcessSpecificWorkItemPart(Map<String, Object> variables) {
		// nothing to do here for now
		return null;
	}

	@Override
    public List<ObjectReferenceType> prepareApprovedBy(ProcessEvent event, PcpWfTask job, OperationResult result) {
    	WfContextType wfc = job.getTask().getWorkflowContext();
		List<ObjectReferenceType> rv = new ArrayList<>();
    	if (!ApprovalUtils.isApprovedFromUri(event.getOutcome())) {		// wfc.approved is not filled in yet
    		return rv;
		}
		for (WorkItemCompletionEventType completionEvent : WfContextUtil.getEvents(wfc, WorkItemCompletionEventType.class)) {
			if (ApprovalUtils.isApproved(completionEvent.getOutput()) && completionEvent.getInitiatorRef() != null) {
				rv.add(completionEvent.getInitiatorRef().clone());
			}
		}
		return rv;
    }


}
