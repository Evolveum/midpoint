/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.policy.assignments.global;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.impl.policy.assignments.AbstractTestAssignmentApproval;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Shouldn't be used, as global policy rules for assignments are not implemented yet.
 *
 * @author mederly
 */
public class TestAssignmentApprovalGlobal extends AbstractTestAssignmentApproval {

	@SuppressWarnings("Duplicates")
	@Override
	protected String getRoleOid(int number) {
		switch (number) {
			case 1: return roleRole1Oid;
			case 2: return roleRole2Oid;
			case 3: return roleRole3Oid;
			case 4: return roleRole4Oid;
			case 10: return roleRole10Oid;
			default: throw new IllegalArgumentException("Wrong role number: " + number);
		}
	}

	@SuppressWarnings("Duplicates")
	@Override
	protected String getRoleName(int number) {
		switch (number) {
			case 1: return "Role1";
			case 2: return "Role2";
			case 3: return "Role3";
			case 4: return "Role4";
			case 10: return "Role10";
			default: throw new IllegalArgumentException("Wrong role number: " + number);
		}
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		/*
			<globalPolicyRule>
				<policyConstraints>
					<assignment>
						<operation>add</operation>
					</assignment>
				</policyConstraints>
				<policyActions>
					<approval>
						<approverRelation>approver</approverRelation>
					</approval>
				</policyActions>
				<focusSelector>
					<type>UserType</type>
				</focusSelector>
				<targetSelector>
					<type>RoleType</type>
					<!-- ...and not Role4 -->
				</targetSelector>
			</globalPolicyRule>
		 */

		/*
		 * Role4 has no approvers. By default, no workflow process(es) are created for roles that have no approvers.
		 * But if we would include Role4 in the global policy rule, a workflow process would be created (even if it
		 * would be automatically approved/rejected, based on setting). But the tests expect there's no process for this role.
		 * So we have to exclude it from the global policy rule.
		 */


		GlobalPolicyRuleType rule = new GlobalPolicyRuleType(prismContext);
		PolicyConstraintsType constraints = new PolicyConstraintsType(prismContext);
		AssignmentPolicyConstraintType assignmentConstraint = new AssignmentPolicyConstraintType(prismContext);
		assignmentConstraint.getOperation().add(ModificationTypeType.ADD);
		constraints.getAssignment().add(assignmentConstraint);
		rule.setPolicyConstraints(constraints);
		PolicyActionsType actions = new PolicyActionsType(prismContext);
		ApprovalPolicyActionType approvalAction = new ApprovalPolicyActionType(prismContext);
		approvalAction.getApproverRelation().add(new QName("approver"));
		actions.setApproval(approvalAction);
		ObjectSelectorType users = new ObjectSelectorType(prismContext);
		users.setType(UserType.COMPLEX_TYPE);
		rule.setFocusSelector(users);
		ObjectSelectorType roles = new ObjectSelectorType(prismContext);
		roles.setType(RoleType.COMPLEX_TYPE);
		roles.setFilter(
				QueryConvertor.createSearchFilterType(
						QueryBuilder.queryFor(RoleType.class, prismContext)
								.not().item(RoleType.F_NAME).eqPoly("Role4")
								.buildFilter(),
						prismContext)
		);
		rule.setTargetSelector(roles);
		rule.setPolicyActions(actions);

		List<ItemDelta<?, ?>> deltas =
				DeltaBuilder.deltaFor(SystemConfigurationType.class, prismContext)
						.item(SystemConfigurationType.F_GLOBAL_POLICY_RULE)
						.replace(rule)
						.asItemDeltas();
		repositoryService.modifyObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), deltas, initResult);

	}
}
