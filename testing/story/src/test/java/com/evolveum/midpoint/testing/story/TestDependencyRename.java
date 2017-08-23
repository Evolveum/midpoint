package com.evolveum.midpoint.testing.story;
/*
 * Copyright (c) 2017 Evolveum
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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.model.impl.util.DebugReconciliationTaskResultListener;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestDependencyRename extends AbstractStoryTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "dependency-rename");

	public static final File OBJECT_TEMPLATE_USER_FILE = new File(TEST_DIR, "object-template-user.xml");
	public static final String OBJECT_TEMPLATE_USER_OID = "10000000-0000-0000-0000-000000000222";

	protected static final File RESOURCE_DUMMY_PHONEBOOK_FILE = new File(TEST_DIR, "resource-dummy-phonebook.xml");
	protected static final String RESOURCE_DUMMY_PHONEBOOK_ID = "phonebook";
	protected static final String RESOURCE_DUMMY_PHONEBOOK_OID = "10000000-0000-0000-0000-000000000001";
	protected static final String RESOURCE_DUMMY_PHONEBOOK_NAMESPACE = MidPointConstants.NS_RI;

	protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;

	public static final File ROLE_BASIC_FILE = new File(TEST_DIR, "role-basic.xml");
	public static final String ROLE_BASIC_OID = "10000000-0000-0000-0000-000000000601";

	private static final String USER_HERMAN_USERNAME = "ht";
	private static final String USER_HERMAN_USERNAME_FINAL = "htoothrot";
	private static final String USER_HERMAN_GIVEN_NAME = "Herman";
	private static final String USER_HERMAN_FAMILY_NAME = "Toothrot";
	private static final String USER_HERMAN_USERNAME_MARLEY = "hmarley";
	private static final String USER_HERMAN_FAMILY_NAME_MARLEY = "Marley";

	private static final String USER_CAPSIZE_USERNAME = "kc";
	private static final String USER_CAPSIZE_USERNAME_FINAL = "kcapsize";
	private static final String USER_CAPSIZE_GIVEN_NAME = "Kate";
	private static final String USER_CAPSIZE_FAMILY_NAME = "Capsize";
	
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;
	
	private String userHermanOid;
	private String accountHermanPhonebookOid;
	private String accountHermanOpenDjOid;

	@Override
	protected void startResources() throws Exception {
		openDJController.startCleanServer();
	}

	@AfterClass
	public static void stopResources() throws Exception {
		openDJController.stop();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		// Resources
		initDummyResource(RESOURCE_DUMMY_PHONEBOOK_ID, RESOURCE_DUMMY_PHONEBOOK_FILE, RESOURCE_DUMMY_PHONEBOOK_OID, initTask, initResult);

		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE,
				RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);

		// Object Templates
		importObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);
		setDefaultUserTemplate(OBJECT_TEMPLATE_USER_OID);

		// Role
		importObjectFromFile(ROLE_BASIC_FILE, initResult);
	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		OperationResult testResultHr = modelService.testResource(RESOURCE_DUMMY_PHONEBOOK_OID, task);
		TestUtil.assertSuccess(testResultHr);

		OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
		TestUtil.assertSuccess(testResultOpenDj);
	}

	
	
	@Test
	public void test100AddUserHerman() throws Exception {
		final String TEST_NAME = "test100AddUserHerman";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> userBefore = getUserDefinition().instantiate();
		UserType userType = userBefore.asObjectable();
		userType.setName(createPolyStringType(USER_HERMAN_USERNAME));
		userType.setGivenName(createPolyStringType(USER_HERMAN_GIVEN_NAME));
		userType.setFamilyName(createPolyStringType(USER_HERMAN_FAMILY_NAME));
		
		display("User before", userBefore);
		
		// WHEN
		addObject(userBefore, task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismObject<UserType> userAfter = findUserByUsername(USER_HERMAN_USERNAME);
		assertNotNull("No herman user", userAfter);
		userHermanOid = userAfter.getOid();
		display("User after", userAfter);
		assertUserHerman(userAfter, USER_HERMAN_USERNAME);
		assertNoAssignments(userAfter);
	}
	
	
	@Test
	public void test110HermanAssignRoleBasic() throws Exception {
		final String TEST_NAME = "test110HermanAssignRoleBasic";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
		assignRole(userHermanOid, ROLE_BASIC_OID, task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismObject<UserType> userAfter = getUser(userHermanOid);
		assertNotNull("No herman user", userAfter);
		userHermanOid = userAfter.getOid();
		display("User after", userAfter);
		assertUserHerman(userAfter, USER_HERMAN_USERNAME_FINAL);
		assertAssignedRole(userAfter, ROLE_BASIC_OID);
		assertAssignments(userAfter, 1);
		
		assertLinks(userAfter, 2);
		accountHermanPhonebookOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_PHONEBOOK_OID);
		PrismObject<ShadowType> shadowPhonebook = getShadowModel(accountHermanPhonebookOid);
		display("Shadow phonebook after", shadowPhonebook);
		assertShadowSecondaryIdentifier(shadowPhonebook, USER_HERMAN_USERNAME_FINAL, getDummyResourceType(RESOURCE_DUMMY_PHONEBOOK_ID), null);
		
		accountHermanOpenDjOid = getLinkRefOid(userAfter, RESOURCE_OPENDJ_OID);
		PrismObject<ShadowType> shadowOpenDj = getShadowModel(accountHermanOpenDjOid);
		display("Shadow opendj after", shadowOpenDj);
		assertShadowSecondaryIdentifier(shadowOpenDj, openDJController.getAccountDn(USER_HERMAN_USERNAME_FINAL), resourceOpenDjType, caseIgnoreMatchingRule);
		
	}
	
	@Test
	public void test112HermanRename() throws Exception {
		final String TEST_NAME = "test112HermanRename";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
		modifyUserReplace(userHermanOid, UserType.F_FAMILY_NAME, task, result, createPolyString(USER_HERMAN_FAMILY_NAME_MARLEY));

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismObject<UserType> userAfter = getUser(userHermanOid);
		assertNotNull("No herman user", userAfter);
		userHermanOid = userAfter.getOid();
		display("User after", userAfter);
		assertUser(userAfter, USER_HERMAN_USERNAME_MARLEY, USER_HERMAN_GIVEN_NAME, USER_HERMAN_FAMILY_NAME_MARLEY);
		assertAssignedRole(userAfter, ROLE_BASIC_OID);
		assertAssignments(userAfter, 1);
		
		assertLinks(userAfter, 2);
		accountHermanPhonebookOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_PHONEBOOK_OID);
		PrismObject<ShadowType> shadowPhonebook = getShadowModel(accountHermanPhonebookOid);
		display("Shadow phonebook after", shadowPhonebook);
		assertShadowSecondaryIdentifier(shadowPhonebook, USER_HERMAN_USERNAME_MARLEY, getDummyResourceType(RESOURCE_DUMMY_PHONEBOOK_ID), null);
		
		accountHermanOpenDjOid = getLinkRefOid(userAfter, RESOURCE_OPENDJ_OID);
		PrismObject<ShadowType> shadowOpenDj = getShadowModel(accountHermanOpenDjOid);
		display("Shadow opendj after", shadowOpenDj);
		assertShadowSecondaryIdentifier(shadowOpenDj, openDJController.getAccountDn(USER_HERMAN_USERNAME_MARLEY), resourceOpenDjType, caseIgnoreMatchingRule);
		
	}
	
	@Test
	public void test120AddUserCapsizeWithBasicRole() throws Exception {
		final String TEST_NAME = "test120AddUserCapsizeWithBasicRole";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> userBefore = getUserDefinition().instantiate();
		UserType userType = userBefore.asObjectable();
		userType.setName(createPolyStringType(USER_CAPSIZE_USERNAME));
		userType.setGivenName(createPolyStringType(USER_CAPSIZE_GIVEN_NAME));
		userType.setFamilyName(createPolyStringType(USER_CAPSIZE_FAMILY_NAME));
		userType.beginAssignment().targetRef(ROLE_BASIC_OID, RoleType.COMPLEX_TYPE).end();
		
		display("User before", userBefore);
		
		// WHEN
		addObject(userBefore, task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		String userCapsizeOid = userBefore.getOid();
		PrismObject<UserType> userAfter = getUser(userCapsizeOid);
		assertNotNull("No user", userAfter);
		display("User after", userAfter);
		assertUser(userAfter, USER_CAPSIZE_USERNAME_FINAL, USER_CAPSIZE_GIVEN_NAME, USER_CAPSIZE_FAMILY_NAME);
		
		assertLinks(userAfter, 2);
		String accountPhonebookOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_PHONEBOOK_OID);
		PrismObject<ShadowType> shadowPhonebook = getShadowModel(accountPhonebookOid);
		display("Shadow phonebook after", shadowPhonebook);
		assertShadowSecondaryIdentifier(shadowPhonebook, USER_CAPSIZE_USERNAME_FINAL, getDummyResourceType(RESOURCE_DUMMY_PHONEBOOK_ID), null);
		
		String accountOpenDjOid = getLinkRefOid(userAfter, RESOURCE_OPENDJ_OID);
		PrismObject<ShadowType> shadowOpenDj = getShadowModel(accountOpenDjOid);
		display("Shadow opendj after", shadowOpenDj);
		assertShadowSecondaryIdentifier(shadowOpenDj, openDJController.getAccountDn(USER_CAPSIZE_USERNAME_FINAL), resourceOpenDjType, caseIgnoreMatchingRule);
	}
	
	
	protected void assertUserHerman(PrismObject<UserType> user, String username) {
		assertUser(user, username, USER_HERMAN_GIVEN_NAME, USER_HERMAN_FAMILY_NAME);
	}

	protected void assertUser(PrismObject<UserType> user, String username, String firstName,
			String lastName) {
		assertUser(user, user.getOid(), username, firstName + " " + lastName, firstName, lastName);
	}

}
