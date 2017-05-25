package com.evolveum.midpoint.testing.story;
/*
 * Copyright (c) 2014-2017 Evolveum
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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.opends.server.types.DirectoryException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUuid extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "uuid");
	
	public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");
	
	public static final File OBJECT_TEMPLATE_USER_FILE = new File(TEST_DIR, "object-template-user.xml");
	public static final String OBJECT_TEMPLATE_USER_OID = "10000000-0000-0000-0000-000000000222";
		
	protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;
		
	public static final File ROLE_CLIENT_FILE = new File(TEST_DIR, "role-client.xml");
	public static final String ROLE_CLIENT_OID = "10000000-0000-0000-0000-000000000601";
	public static final String ROLE_CLIENT_NAME = "Client";
	
	private static final File OU_CLIENTS_LDIF_FILE = new File(TEST_DIR, "ou-clients.ldif");

	private static final String USER_RAPP_GIVEN_NAME = "Rapp";
	private static final String USER_RAPP_FAMILY_NAME = "Scallion";

	private static final String USER_MANCOMB_GIVEN_NAME = "Mancomb";
	private static final String USER_MANCOMB_FAMILY_NAME = "Seepgood";

	private static final String USER_KATE_NAME = "c0c010c0-d34d-b33f-f00d-11111aa00001";
	private static final String USER_KATE_GIVEN_NAME = "Kate";
	private static final String USER_KATE_FAMILY_NAME = "Capsize";
	
	private static final String USER_WALLY_OID = "c0c010c0-d34d-b33f-f00d-11111aa00002";
	private static final String USER_WALLY_GIVEN_NAME = "Wally";
	private static final String USER_WALLY_FAMILY_NAME = "Feed";
	
	private static final String USER_ROGERS_OID = "c0c010c0-d34d-b33f-f00d-11111aa00003";
	private static final String USER_ROGERS_GIVEN_NAME = "Rum";
	private static final String USER_ROGERS_FAMILY_NAME = "Rogers";

	private static final String USER_MARTY_OID = "c0c010c0-d34d-b33f-f00d-11111aa00004";
	private static final String USER_MARTY_NAME = "marty";
	private static final String USER_MARTY_GIVEN_NAME = "Mad";
	private static final String USER_MARTY_FAMILY_NAME = "Marty";
	
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;
	
	protected String userRappOid;
	protected String userMancombOid;
	
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
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);
		
		// Object Templates
		importObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);
				
		// Role
		importObjectFromFile(ROLE_CLIENT_FILE, initResult);
				
		// LDAP content
		openDJController.addEntryFromLdifFile(OU_CLIENTS_LDIF_FILE);
		
	}
	
	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);
        
        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, true);
        waitForTaskStart(TASK_VALIDITY_SCANNER_OID, true);
	}
	
	@Test
    public void test100AddUserRapp() throws Exception {
		final String TEST_NAME = "test100AddUserRapp";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = createNoNameUser(USER_RAPP_GIVEN_NAME, USER_RAPP_FAMILY_NAME, true);
        		
        // WHEN
        addObject(user, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        userRappOid = user.getOid();
        
        user = getUser(userRappOid);
        assertUser(user, USER_RAPP_GIVEN_NAME, USER_RAPP_FAMILY_NAME);
	}

	@Test
    public void test101RappAssignRoleClient() throws Exception {
		final String TEST_NAME = "test101RappAssignRoleClient";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignRole(userRappOid, ROLE_CLIENT_OID, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(userRappOid);
        assertUser(userAfter, USER_RAPP_GIVEN_NAME, USER_RAPP_FAMILY_NAME);
        assertLdapClient(userAfter, USER_RAPP_GIVEN_NAME, USER_RAPP_FAMILY_NAME);
	}
	
	// TODO: modify user, account should be modified
	
	@Test
    public void test107RappUnAssignRoleClient() throws Exception {
		final String TEST_NAME = "test107RappUnAssignRoleClient";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        // WHEN
        unassignRole(userRappOid, ROLE_CLIENT_OID);
        
        // THEN
        PrismObject<UserType> userAfter = getUser(userRappOid);
        assertUser(userAfter, USER_RAPP_GIVEN_NAME, USER_RAPP_FAMILY_NAME);
        assertNoLdapClient(userAfter);
	}
	
	@Test
    public void test110AddMancombWithRoleClient() throws Exception {
		final String TEST_NAME = "test110AddMancombWithRoleClient";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = createClientUser(null, null, USER_MANCOMB_GIVEN_NAME, USER_MANCOMB_FAMILY_NAME, true);
        
        // WHEN
        addObject(user, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        userMancombOid = user.getOid();
        
        PrismObject<UserType> userAfter = getUser(userMancombOid);
        assertUser(userAfter, USER_MANCOMB_GIVEN_NAME, USER_MANCOMB_FAMILY_NAME);
        assertLdapClient(userAfter, USER_MANCOMB_GIVEN_NAME, USER_MANCOMB_FAMILY_NAME);
	}

	/**
	 * Test user rename. Name is bound to OID and OID cannot be changed therefore
	 * an attempt to rename should fail.
	 */
	@Test
    public void test112RenameMancomb() throws Exception {
		final String TEST_NAME = "test112RenameMancomb";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        try {
	        // WHEN
	        modifyUserReplace(userMancombOid, UserType.F_NAME, task, result, PrismTestUtil.createPolyString("whatever"));
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// This is expected
        	result.computeStatus();
        	TestUtil.assertFailure(result);
        }
        
        PrismObject<UserType> userAfter = getUser(userMancombOid);
        assertUser(userAfter, USER_MANCOMB_GIVEN_NAME, USER_MANCOMB_FAMILY_NAME);
        assertLdapClient(userAfter, USER_MANCOMB_GIVEN_NAME, USER_MANCOMB_FAMILY_NAME);
	}
	
	@Test
    public void test119MancombDelete() throws Exception {
		final String TEST_NAME = "test119MancombDelete";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        deleteObject(UserType.class, userMancombOid, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoObject(UserType.class, userMancombOid, task, result);
        openDJController.assertNoEntry("uid="+userMancombOid+",ou=clients,dc=example,dc=com");
	}
	
	/**
	 * Kate nas a name. But no OID.
	 */
	@Test
    public void test120AddKateWithRoleClient() throws Exception {
		final String TEST_NAME = "test120AddKateWithRoleClient";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = createClientUser(null, USER_KATE_NAME, USER_KATE_GIVEN_NAME, USER_KATE_FAMILY_NAME, true);
        
        // WHEN
        addObject(user, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_KATE_NAME);
        assertUser(userAfter, USER_WALLY_GIVEN_NAME, USER_WALLY_FAMILY_NAME);
        assertLdapClient(userAfter, USER_WALLY_GIVEN_NAME, USER_WALLY_FAMILY_NAME);
	}

	/**
	 * Wally already has OID. But no name.
	 */
	@Test
    public void test122AddWallyWithRoleClient() throws Exception {
		final String TEST_NAME = "test122AddWallyWithRoleClient";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = createClientUser(USER_WALLY_OID, null, USER_WALLY_GIVEN_NAME, USER_WALLY_FAMILY_NAME, true);
        
        // WHEN
        addObject(user, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_WALLY_OID);
        assertUser(userAfter, USER_WALLY_GIVEN_NAME, USER_WALLY_FAMILY_NAME);
        assertLdapClient(userAfter, USER_WALLY_GIVEN_NAME, USER_WALLY_FAMILY_NAME);
	}
	
	/**
	 * rogers has both OID and name and they do match.
	 */
	@Test
    public void test124AddRogersWithRoleClient() throws Exception {
		final String TEST_NAME = "test124AddMartyWithRoleClient";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = createClientUser(USER_ROGERS_OID, USER_ROGERS_OID, USER_ROGERS_GIVEN_NAME, USER_ROGERS_FAMILY_NAME, true);
        
        // WHEN
        addObject(user, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_ROGERS_OID);
        assertUser(userAfter, USER_ROGERS_GIVEN_NAME, USER_ROGERS_FAMILY_NAME);
        assertLdapClient(userAfter, USER_ROGERS_GIVEN_NAME, USER_ROGERS_FAMILY_NAME);
	}
	
	/**
	 * marty has both OID and name and they do NOT match.
	 */
	@Test
    public void test126AddMartyWithRoleClient() throws Exception {
		final String TEST_NAME = "test124AddMartyWithRoleClient";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = createClientUser(USER_MARTY_OID, USER_MARTY_NAME, USER_MARTY_GIVEN_NAME, USER_MARTY_FAMILY_NAME, true);
        
        try {
	        // WHEN
        	addObject(user, task, result);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// This is expected
        	result.computeStatus();
        	TestUtil.assertFailure(result);
        }
        
        // THEN
        
        assertNoObject(UserType.class, USER_MARTY_OID, task, result);
        openDJController.assertNoEntry("uid="+USER_MARTY_OID+",ou=clients,dc=example,dc=com");
	}
	
	private void assertUser(PrismObject<UserType> user, String firstName, String lastName)  {
		display("User", user);
		String oid = user.getOid();
		PolyStringType name = user.asObjectable().getName();
		assertEquals("User name-OID mismatch", oid, name.getOrig());
		// TODO
	}

	private void assertLdapClient(PrismObject<UserType> user, String firstName, String lastName) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        assertLinks(user, 1);
        assertAssignments(user, RoleType.class, 1);
        assertAssignedRole(user, ROLE_CLIENT_OID);
        
        assertAccount(user, RESOURCE_OPENDJ_OID);
        PrismReferenceValue linkRef = getLinkRef(user, RESOURCE_OPENDJ_OID);
        PrismObject<ShadowType> shadow = getShadowModel(linkRef.getOid());
		display("OpenDJ shadow linked to "+user, shadow);
		IntegrationTestTools.assertSecondaryIdentifier(shadow, "uid="+user.getOid()+",ou=clients,dc=example,dc=com");
		// TODO: cn, sn, givenName
	}
	
	private void assertNoLdapClient(PrismObject<UserType> user) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException {
        assertLinks(user, 0);
        assertAssignments(user, RoleType.class, 0);
        assertNotAssignedRole(user, ROLE_CLIENT_OID);
        
        openDJController.assertNoEntry("uid="+user.getOid()+",ou=clients,dc=example,dc=com");
	}
	
	protected PrismObject<UserType> createNoNameUser(String givenName, String familyName, Boolean enabled) throws SchemaException {
		PrismObject<UserType> user = getUserDefinition().instantiate();
		UserType userType = user.asObjectable();
		userType.setGivenName(PrismTestUtil.createPolyStringType(givenName));
		userType.setFamilyName(PrismTestUtil.createPolyStringType(familyName));
		if (enabled != null) {
			ActivationType activation = new ActivationType();
			userType.setActivation(activation);
			if (enabled) {
				activation.setAdministrativeStatus(ActivationStatusType.ENABLED);
			} else {
				activation.setAdministrativeStatus(ActivationStatusType.DISABLED);
			}
		}
		return user;
	}
	
	private PrismObject<UserType> createClientUser(String oid, String name, String givenName,
			String familyName, boolean enabled) throws SchemaException {
        PrismObject<UserType> user = createNoNameUser(givenName, familyName, enabled);
        AssignmentType roleAssignment = new AssignmentType();
        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid(ROLE_CLIENT_OID);
        targetRef.setType(RoleType.COMPLEX_TYPE);
		roleAssignment.setTargetRef(targetRef);
		user.asObjectable().getAssignment().add(roleAssignment);
		user.setOid(oid);
		if (name != null) {
			user.asObjectable().setName(PrismTestUtil.createPolyStringType(name));
		}
		return user;
	}
	
}
