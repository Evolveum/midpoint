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

package com.evolveum.midpoint.wf.impl.policy.plain;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.impl.policy.AbstractTestAssignmentApproval;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Tests assigning of roles 1..3 with explicitly defined approvers (i.e. via approverRef item).
 * As for policy rules, the default ones are used.
 *
 * @author mederly
 */
@SuppressWarnings("Duplicates")
public class TestAssignmentApprovalPlainExplicit extends AbstractTestAssignmentApproval {

	@Override
	protected String getRoleOid(int number) {
		switch (number) {
			case 1: return ROLE_ROLE1A_OID;
			case 2: return ROLE_ROLE2A_OID;
			case 3: return ROLE_ROLE3A_OID;
			case 4: return ROLE_ROLE4A_OID;
			case 10: return ROLE_ROLE10A_OID;
			default: throw new IllegalArgumentException("Wrong role number: " + number);
		}
	}

	@Override
	protected String getRoleName(int number) {
		switch (number) {
			case 1: return "Role1a";
			case 2: return "Role2a";
			case 3: return "Role3a";
			case 4: return "Role4a";
			case 10: return "Role10a";
			default: throw new IllegalArgumentException("Wrong role number: " + number);
		}
	}

	@Override
	protected void importLead10(Task task, OperationResult result) throws Exception {
		super.importLead10(task, result);
		executeChangesAssertSuccess((ObjectDelta<RoleType>) DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(RoleType.F_APPROVER_REF)
						.add(ObjectTypeUtil.createObjectRef(USER_LEAD10_OID, ObjectTypes.USER).asReferenceValue())
				.asObjectDelta(getRoleOid(10)), null, task, result);
	}
}
