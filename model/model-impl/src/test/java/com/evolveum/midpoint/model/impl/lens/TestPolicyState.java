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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.Collections;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Tests recording of policy situations into objects and assignments.
 *
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPolicyState extends AbstractLensTest {

	private static final String ROLE_JUDGE_POLICY_RULE_EXCLUSION_NAME = "criminal exclusion";

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		setDefaultUserTemplate(USER_TEMPLATE_OID);

		addObject(ROLE_PIRATE_SITUATION_ONLY_FILE);
		addObject(ROLE_MUTINIER_FILE);
		addObject(ROLE_JUDGE_SITUATION_ONLY_FILE);
		addObject(ROLE_CONSTABLE_FILE);
		addObject(ROLE_THIEF_FILE);

		addObjects(ROLE_CORP_FILES);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		InternalMonitor.reset();

		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

	@Test
	public void test100JackAssignRoleJudge() throws Exception {
		final String TEST_NAME = "test100JackAssignRoleJudge";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyState.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		UserType jack = getUser(USER_JACK_OID).asObjectable();
		display("jack", jack);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
		assertEquals("Wrong # of assignments", 1, jack.getAssignment().size());
		assertEquals("Wrong policy situations",
				Collections.emptyList(),
				jack.getAssignment().get(0).getPolicySituation());
	}

	@Test
	public void test110JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test110JackAssignRolePirate";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyState.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		UserType jack = getUser(USER_JACK_OID).asObjectable();
		display("jack", jack);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
		assertEquals("Wrong # of assignments", 2, jack.getAssignment().size());
		for (AssignmentType assignment : jack.getAssignment()) {
			assertEquals("Wrong policy situations",
					Collections.singletonList(SchemaConstants.MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION),
					assignment.getPolicySituation());
		}
	}

	// should keep the situation for both assignments
	@Test
	public void test120RecomputeJack() throws Exception {
		final String TEST_NAME = "test120RecomputeJack";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyState.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		recomputeUser(USER_JACK_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		UserType jack = getUser(USER_JACK_OID).asObjectable();
		display("jack", jack);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertEquals("Wrong # of assignments", 2, jack.getAssignment().size());
		for (AssignmentType assignment : jack.getAssignment()) {
			assertEquals("Wrong policy situations",
					Collections.singletonList(SchemaConstants.MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION),
					assignment.getPolicySituation());
		}
	}




}
