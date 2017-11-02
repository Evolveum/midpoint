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
package com.evolveum.midpoint.model.intest.rbac;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleManagementConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAutoassign extends AbstractRbacTest {

	protected static final File AUTOASSIGN_DIR = new File(TEST_DIR, "autoassign");
	
	protected static final File ROLE_UNIT_WORKER_FILE = new File(AUTOASSIGN_DIR, "role-unit-worker.xml");
	protected static final String ROLE_UNIT_WORKER_OID = "e83969fa-bfda-11e7-8b5b-ab60e0279f06";
	protected static final String ROLE_UNIT_WORKER_TITLE = "Worker";
	
	protected static final File ROLE_UNIT_SLEEPER_FILE = new File(AUTOASSIGN_DIR, "role-unit-sleeper.xml");
	protected static final String ROLE_UNIT_SLEEPER_OID = "660f0fb8-bfec-11e7-bb16-07154f1e53a6";
	protected static final String ROLE_UNIT_SLEEPER_TITLE = "Sleeper";

	protected static final File ROLE_UNIT_WALKER_FILE = new File(AUTOASSIGN_DIR, "role-unit-walker.xml");
	protected static final String ROLE_UNIT_WALKER_OID = "a2bc45fc-bfec-11e7-bdfd-af4b3e689502";
	protected static final String ROLE_UNIT_WALKER_TITLE = "Walker";

	protected static final String UNIT_WORKER = "worker";
	protected static final String UNIT_SLEEPER = "sleeper";
	protected static final String UNIT_WALKER = "walker";

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		
		repoAddObjectFromFile(ROLE_UNIT_WORKER_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_UNIT_SLEEPER_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_UNIT_WALKER_FILE, RoleType.class, initResult);
		
		modifyObjectReplaceProperty(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
				new ItemPath(SystemConfigurationType.F_ROLE_MANAGEMENT, RoleManagementConfigurationType.F_AUTOASSIGN_ENABLED),
				initTask, initResult, Boolean.TRUE);
	}

	/**
	 * MID-2840
	 */
	@Test
    public void test100ModifyUnitWorker() throws Exception {
		final String TEST_NAME = "test100ModifyUnitWorker";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
        		createPolyString(UNIT_WORKER));

		// THEN
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATIONAL_UNIT, 
				createPolyString(UNIT_WORKER));
        assertAssignedRole(userAfter, ROLE_UNIT_WORKER_OID);
        assertAssignments(userAfter, 1);
        getSingleLinkRef(userAfter);

        assertDummyAccount(null, USER_JACK_USERNAME);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_UNIT_WORKER_TITLE);
	}
	
	/**
	 * MID-2840
	 */
	@Test
    public void test109ModifyUniNull() throws Exception {
		final String TEST_NAME = "test109ModifyUniNull";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result /* no value */);

		// THEN
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);

		PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATIONAL_UNIT);
        assertAssignments(userAfter, 0);
        assertLinks(userAfter, 0);

        assertNoDummyAccount(null, USER_JACK_USERNAME);
	}
	
	/**
	 * MID-2840
	 */
	@Test
    public void test110ModifyUnitSleepwalker() throws Exception {
		final String TEST_NAME = "test110ModifyUnitSleepwalker";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
        		createPolyString(UNIT_SLEEPER), createPolyString(UNIT_WALKER));

		// THEN
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATIONAL_UNIT, 
				createPolyString(UNIT_SLEEPER), createPolyString(UNIT_WALKER));
        assertAssignedRole(userAfter, ROLE_UNIT_SLEEPER_OID);
        assertAssignedRole(userAfter, ROLE_UNIT_WALKER_OID);
        assertAssignments(userAfter, 2);
        getSingleLinkRef(userAfter);

        assertDummyAccount(null, USER_JACK_USERNAME);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, 
        		ROLE_UNIT_SLEEPER_TITLE, ROLE_UNIT_WALKER_TITLE);
	}
	
	/**
	 * MID-2840
	 */
	@Test
    public void test112ModifyUnitSleeperToWorker() throws Exception {
		final String TEST_NAME = "test112ModifyUnitSleeperToWorker";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		ObjectDelta<UserType> objectDelta = ObjectDelta.createModificationAddProperty(UserType.class, 
				USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, prismContext, createPolyString(UNIT_WORKER));
		objectDelta.addModificationDeleteProperty(UserType.F_ORGANIZATIONAL_UNIT, createPolyString(UNIT_SLEEPER));
		
		// WHEN
		modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

		// THEN
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATIONAL_UNIT, 
				createPolyString(UNIT_WORKER), createPolyString(UNIT_WALKER));
        assertAssignedRole(userAfter, ROLE_UNIT_WALKER_OID);
        assertAssignedRole(userAfter, ROLE_UNIT_WORKER_OID);
        assertAssignments(userAfter, 2);
        getSingleLinkRef(userAfter);

        assertDummyAccount(null, USER_JACK_USERNAME);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, 
        		ROLE_UNIT_WORKER_TITLE, ROLE_UNIT_WALKER_TITLE);
	}

	/**
	 * MID-2840
	 */
	@Test
    public void test114ModifyUnitAddSleeper() throws Exception {
		final String TEST_NAME = "test114ModifyUnitAddSleeper";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
		modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
        		createPolyString(UNIT_SLEEPER));

		// THEN
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATIONAL_UNIT, 
				createPolyString(UNIT_WORKER), createPolyString(UNIT_WALKER), createPolyString(UNIT_SLEEPER));
        assertAssignedRole(userAfter, ROLE_UNIT_WALKER_OID);
        assertAssignedRole(userAfter, ROLE_UNIT_WORKER_OID);
        assertAssignedRole(userAfter, ROLE_UNIT_SLEEPER_OID);
        assertAssignments(userAfter, 3);
        getSingleLinkRef(userAfter);

        assertDummyAccount(null, USER_JACK_USERNAME);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, 
        		ROLE_UNIT_WORKER_TITLE, ROLE_UNIT_WALKER_TITLE, ROLE_UNIT_SLEEPER_TITLE);
	}

	
}
