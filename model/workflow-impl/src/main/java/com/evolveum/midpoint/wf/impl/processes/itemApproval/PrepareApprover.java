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

import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.*;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import static com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil.getRequiredVariable;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getTaskManager;

public class PrepareApprover implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(PrepareApprover.class);

    public void execute(DelegateExecution execution) {

        LightweightObjectRef approverRef = getRequiredVariable(execution, ProcessVariableNames.APPROVER_REF, LightweightObjectRef.class);
		ApprovalLevelImpl level = ActivitiUtil.getRequiredVariable(execution, ProcessVariableNames.LEVEL, ApprovalLevelImpl.class);

        String assignee = null;
        String candidateGroups = null;
        if (approverRef.getType() == null || UserType.COMPLEX_TYPE.equals(approverRef.getType())) {
            assignee = approverRef.getOid();
        } else if (RoleType.COMPLEX_TYPE.equals(approverRef.getType())) {
            candidateGroups = MiscDataUtil.ROLE_PREFIX + ":" + approverRef.getOid();
        } else if (OrgType.COMPLEX_TYPE.equals(approverRef.getType())) {
            candidateGroups = MiscDataUtil.ORG_PREFIX + ":" + approverRef.getOid();
        } else {
            throw new IllegalStateException("Unsupported type of the approver: " + approverRef.getType());
        }

		execution.setVariableLocal(ProcessVariableNames.ASSIGNEE, assignee);
		execution.setVariableLocal(ProcessVariableNames.CANDIDATE_GROUPS, candidateGroups);

		String instruction = null;
        if (level.getApproverInstruction() != null) {
			try {
				WfExpressionEvaluationHelper evaluator = SpringApplicationContextHolder.getExpressionEvaluationHelper();
				OperationResult result = new OperationResult(PrepareApprover.class.getName() + ".prepareApproverInstruction");
				Task wfTask = ActivitiUtil.getTask(execution, result);
				Task opTask = getTaskManager().createTaskInstance();
				ExpressionVariables variables = evaluator.getDefaultVariables(execution, wfTask, result);
				instruction = evaluator.getSingleValue(
						evaluator.evaluateExpression(level.getApproverInstruction(), variables, execution,
								"approver instruction expression", String.class, DOMUtil.XSD_STRING, opTask, result),
						"", "approver instruction expression");
			} catch (Throwable t) {
        		throw new SystemException("Couldn't evaluate approver instruction expression in " + execution, t);
			}
			execution.setVariableLocal(CommonProcessVariableNames.APPROVER_INSTRUCTION, instruction);
		}

        LOGGER.debug("Creating work item for assignee={}, candidateGroups={}, approverInstruction='{}'",
				assignee, candidateGroups, instruction);
    }
}
