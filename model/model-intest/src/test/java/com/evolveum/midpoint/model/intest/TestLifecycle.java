/*
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLifecycle extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/lifecycle");
	
	public static final File SYSTEM_CONFIGURATION_LIFECYCLE_FILE = new File(TEST_DIR, "system-configuration-lifecycle.xml");
	
	// subtype = dataProcessingBasis
	public static final File ROLE_HEADMASTER_FILE = new File(TEST_DIR, "role-headmaster.xml");
	protected static final String ROLE_HEADMASTER_OID = "b9c885ba-034b-11e8-a708-13836b619045";
	
	// subtype = dataProcessingBasis
	public static final File ROLE_CARETAKER_FILE = new File(TEST_DIR, "role-caretaker.xml");
	protected static final String ROLE_CARETAKER_OID = "9162a952-034b-11e8-afb7-138a763f2350";
	
	// no subtype, this is NOT a dataProcessingBasis
	public static final File ROLE_GAMBLER_FILE = new File(TEST_DIR, "role-gambler.xml");
	protected static final String ROLE_GAMBLER_OID = "2bb2fb86-034e-11e8-9cf3-77abfc7aafec";
	
	public static final String SUBTYPE_EMPLOYEE = "employee";
	private static final Object USER_JACK_TELEPHONE_NUMBER = "12345654321";

	@Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        
		repoAddObjectFromFile(ROLE_HEADMASTER_FILE, initResult);
		repoAddObjectFromFile(ROLE_CARETAKER_FILE, initResult);
		repoAddObjectFromFile(ROLE_GAMBLER_FILE, initResult);
    }
	
	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_LIFECYCLE_FILE;
	}

	/**
	 * Setup jack. Setting subtype to employee will put him under lifecycle
	 * control. But before that we want him to have at least one
	 * processing basis role.
	 */
    @Test
    public void test050SetupJack() throws Exception {
		final String TEST_NAME = "test050SetupJack";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignRole(USER_JACK_OID, ROLE_HEADMASTER_OID, task, result);
        assignRole(USER_JACK_OID, ROLE_GAMBLER_OID, task, result);
        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, task, result, SUBTYPE_EMPLOYEE);
        modifyUserReplace(USER_JACK_OID, UserType.F_TELEPHONE_NUMBER, task, result, USER_JACK_TELEPHONE_NUMBER);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertLifecycleState(userAfter, null);
        assertTelephoneNumber(userAfter, USER_JACK_TELEPHONE_NUMBER);
        assertLinks(userAfter, 0);
    }
    
    private void assertTelephoneNumber(PrismObject<UserType> user, Object expectedTelephoneNumber) {
    	assertEquals("Wrong telephoe number in "+user, expectedTelephoneNumber, user.asObjectable().getTelephoneNumber());
	}

	protected <O extends ObjectType> void assertLifecycleState(PrismObject<O> object, String expectedLifecycleState) {
		assertEquals("Wrong lifecycle state in "+object, expectedLifecycleState, object.asObjectable().getLifecycleState());
	}

	@Test
    public void test100AssignJackCaretaker() throws Exception {
		final String TEST_NAME = "test100AssignJackCaretaker";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignRole(USER_JACK_OID, ROLE_CARETAKER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 3);
        assertLifecycleState(userAfter, null);
        assertTelephoneNumber(userAfter, USER_JACK_TELEPHONE_NUMBER);
    }

	@Test
    public void test102UnassignJackHeadmaster() throws Exception {
		final String TEST_NAME = "test102UnassignJackHeadmaster";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        unassignRole(USER_JACK_OID, ROLE_HEADMASTER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertLifecycleState(userAfter, null);
        assertTelephoneNumber(userAfter, USER_JACK_TELEPHONE_NUMBER);
    }
	
	/**
	 * This is the real test. Now lifecycle transition should take
	 * place because jack has no processing basis role.
	 */
	@Test
    public void test110UnassignJackCaretaker() throws Exception {
		final String TEST_NAME = "test110UnassignJackCaretaker";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        unassignRole(USER_JACK_OID, ROLE_CARETAKER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertLifecycleState(userAfter, SchemaConstants.LIFECYCLE_ARCHIVED);
        assertTelephoneNumber(userAfter, null);
    }

	/**
	 * Jack is now archived. So, even if we assign a new processing basis
	 * role the lifecycle should not change. Archival is a one-way process.
	 */
	@Test
    public void test112UnassignJackCaretaker() throws Exception {
		final String TEST_NAME = "test110UnassignJackCaretaker";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignRole(USER_JACK_OID, ROLE_HEADMASTER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertLifecycleState(userAfter, SchemaConstants.LIFECYCLE_ARCHIVED);
        assertTelephoneNumber(userAfter, null);
    }

}
