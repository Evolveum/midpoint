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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.wf.impl.policy.assignments.AbstractTestAssignmentApproval;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Shouldn't be used, as global policy rules for assignments are not implemented yet.
 *
 * @author mederly
 */
public class TestAssignmentApprovalGlobal extends AbstractTestAssignmentApproval {

	private static final File SYSTEM_CONFIGURATION_GLOBAL_FILE = new File(TEST_RESOURCE_DIR, "system-configuration-global.xml");

	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_GLOBAL_FILE;
	}

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

	/**
	 * MID-3836
	 */
	@Test
	public void test300ApprovalAndEnforce() throws Exception {
		final String TEST_NAME = "test300ApprovalAndEnforce";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);
		Task task = createTask(TEST_NAME);
		task.setOwner(userAdministrator);
		OperationResult result = task.getResult();

		try {
			assignRole(userJackOid, roleRole15Oid, task, result);
		} catch (PolicyViolationException e) {
			// ok
			System.out.println("Got expected exception: " + e);
		}
		List<WorkItemType> currentWorkItems = modelService.searchContainers(WorkItemType.class, null, null, task, result);
		display("current work items", currentWorkItems);
		assertEquals("Wrong # of current work items", 0, currentWorkItems.size());
	}

}
