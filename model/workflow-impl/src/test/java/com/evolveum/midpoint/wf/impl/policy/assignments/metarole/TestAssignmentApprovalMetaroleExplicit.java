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

package com.evolveum.midpoint.wf.impl.policy.assignments.metarole;

import com.evolveum.midpoint.wf.impl.policy.assignments.AbstractTestAssignmentApproval;

/**
 * Tests assigning of roles 1..3 with explicitly assigned metaroles (with policy rules).
 *
 * @author mederly
 */
@SuppressWarnings("Duplicates")
public class TestAssignmentApprovalMetaroleExplicit extends AbstractTestAssignmentApproval {

	@Override
	protected String getRoleOid(int number) {
		switch (number) {
			case 1: return roleRole1bOid;
			case 2: return roleRole2bOid;
			case 3: return roleRole3bOid;
			case 4: return roleRole4bOid;
			case 10: return roleRole10bOid;
			default: throw new IllegalArgumentException("Wrong role number: " + number);
		}
	}

	@Override
	protected String getRoleName(int number) {
		switch (number) {
			case 1: return "Role1b";
			case 2: return "Role2b";
			case 3: return "Role3b";
			case 4: return "Role4b";
			case 10: return "Role10b";
			default: throw new IllegalArgumentException("Wrong role number: " + number);
		}
	}
}
