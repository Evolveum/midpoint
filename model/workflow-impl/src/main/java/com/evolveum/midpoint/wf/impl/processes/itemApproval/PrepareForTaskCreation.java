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

import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.*;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.wf.impl.util.SingleItemSerializationSafeContainerImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil.getRequiredVariable;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getPrismContext;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getTaskManager;

public class PrepareForTaskCreation implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(PrepareForTaskCreation.class);

    public void execute(DelegateExecution execution) {

    	PrismContext prismContext = getPrismContext();
		OperationResult result = new OperationResult(PrepareForTaskCreation.class.getName() + ".execute");
		Task wfTask = ActivitiUtil.getTask(execution, result);
		Task opTask = getTaskManager().createTaskInstance();
		ApprovalStageDefinitionType stageDef = ActivitiUtil.getAndVerifyCurrentStage(execution, wfTask, true, prismContext);

		LightweightObjectRef approverRef = getRequiredVariable(execution, ProcessVariableNames.APPROVER_REF, LightweightObjectRef.class,
				prismContext);

        String assignee = null;
        String candidateGroups = null;
        if (approverRef.getType() == null || QNameUtil.match(UserType.COMPLEX_TYPE, approverRef.getType())) {
            assignee = MiscDataUtil.refToString(new ObjectReferenceType().oid(approverRef.getOid()).type(UserType.COMPLEX_TYPE));
        } else if (QNameUtil.match(RoleType.COMPLEX_TYPE, approverRef.getType()) ||
				QNameUtil.match(OrgType.COMPLEX_TYPE, approverRef.getType()) ||
				QNameUtil.match(ServiceType.COMPLEX_TYPE, approverRef.getType())) {
            candidateGroups = MiscDataUtil.refToString(approverRef.toObjectReferenceType());
        } else {
            throw new IllegalStateException("Unsupported type of the approver: " + approverRef.getType());
        }

        // TODO optimize by using setVariablesLocal
		execution.setVariableLocal(ProcessVariableNames.ASSIGNEE, assignee);
		execution.setVariableLocal(ProcessVariableNames.CANDIDATE_GROUPS, candidateGroups);

		List<?> additionalInformation;
        if (stageDef.getAdditionalInformation() != null) {
			try {
				WfExpressionEvaluationHelper evaluator = SpringApplicationContextHolder.getExpressionEvaluationHelper();
				WfStageComputeHelper stageComputer = SpringApplicationContextHolder.getStageComputeHelper();
				ExpressionVariables variables = stageComputer.getDefaultVariables(execution, wfTask, result);
				additionalInformation = evaluator.evaluateExpression(stageDef.getAdditionalInformation(), variables,
						"additional information expression", Object.class, DOMUtil.XSD_STRING, null, opTask, result);
			} catch (Throwable t) {
        		throw new SystemException("Couldn't evaluate additional information expression in " + execution, t);
			}
		} else {
        	additionalInformation = new AdditionalInformationGenerator().getDefaultAdditionalInformation(wfTask, stageDef.getNumber());
		}
		if (!additionalInformation.isEmpty()) {
			execution.setVariableLocal(CommonProcessVariableNames.ADDITIONAL_INFORMATION,
					new SingleItemSerializationSafeContainerImpl<>(
							wrapAdditionalInformationIfNeeded(additionalInformation), prismContext));
		}

        LOGGER.debug("Creating work item for assignee={}, candidateGroups={}, additionalInformation='{}'",
				assignee, candidateGroups, additionalInformation);
    }

	// TODO localizable
	@SuppressWarnings("unchecked")
	private List<InformationType> wrapAdditionalInformationIfNeeded(List<?> data) {		// data is not empty
		if (data.stream().allMatch(o -> o instanceof String)) {
			InformationType info = new InformationType();
			for (Object o : data) {
				InformationPartType part = new InformationPartType();
				part.setText(LocalizationUtil.forFallbackMessage((String) o));
				info.getPart().add(part);
			}
			return Collections.singletonList(info);
		} else if (data.stream().allMatch(o -> o instanceof InformationType)) {
			return (List<InformationType>) data;
		} else {
			throw new SystemException("Couldn't create 'additional information' structure from list of "
				+ data.stream().map(o -> o != null ? o.getClass().getSimpleName() : null).collect(Collectors.joining(", ", "[", "]")));
		}
	}
}
