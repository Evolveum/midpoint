/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.wf.impl.policy.assignments.plain;

import com.evolveum.midpoint.wf.impl.policy.assignments.AbstractTestAssignmentApproval;

/**
 * Tests assigning of roles 1..3 with implicitly defined approvers (i.e. via org:approver assignment).
 * As for policy rules, the default ones are used.
 *
 * @author mederly
 */
@SuppressWarnings("Duplicates")
public class TestAssignmentApprovalPlainImplicit extends AbstractTestAssignmentApproval {

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
}
