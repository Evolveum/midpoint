/*
 * Copyright (c) 2010-2014 Evolveum
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

import static org.testng.AssertJUnit.assertFalse;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.FilterInvocation;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SpecialObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurity extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/security");
	
	protected static final File USER_LECHUCK_FILE = new File(TEST_DIR, "user-lechuck.xml");
	protected static final String USER_LECHUCK_OID = "c0c010c0-d34d-b33f-f00d-1c1c11cc11c2";
	
	protected static final File ROLE_READONLY_FILE = new File(TEST_DIR, "role-readonly.xml");
	protected static final String ROLE_READONLY_OID = "00000000-0000-0000-0000-00000000aa01";
	
	protected static final File ROLE_READONLY_DEEP_FILE = new File(TEST_DIR, "role-readonly-deep.xml");
	protected static final String ROLE_READONLY_DEEP_OID = "00000000-0000-0000-0000-00000000aa02";
	
	protected static final File ROLE_SELF_FILE = new File(TEST_DIR, "role-self.xml");
	protected static final String ROLE_SELF_OID = "00000000-0000-0000-0000-00000000aa03";
	
	protected static final File ROLE_OBJECT_FILTER_CARIBBEAN_FILE = new File(TEST_DIR, "role-filter-object-caribbean.xml");
	protected static final String ROLE_OBJECT_FILTER_CARIBBEAN_OID = "00000000-0000-0000-0000-00000000aa04";

	@Autowired(required=true)
	private UserProfileService userDetailsService;
	
	@Autowired(required=true)
	private SecurityEnforcer securityEnforcer;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		repoAddObjectFromFile(ROLE_READONLY_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_READONLY_DEEP_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_SELF_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_OBJECT_FILTER_CARIBBEAN_FILE, RoleType.class, initResult);
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);

        // WHEN
        PrismObject<RoleType> roleSelf = getRole(ROLE_SELF_OID);
        
        // THEN
        display("Role self", roleSelf);
        List<AuthorizationType> authorizations = roleSelf.asObjectable().getAuthorization();
        assertEquals("Wrong number of authorizations", 2, authorizations.size());
        AuthorizationType authRead = findAutz(authorizations, ModelService.AUTZ_READ_URL);
        assertEquals("Wrong action in authorization", ModelService.AUTZ_READ_URL, authRead.getAction().get(0));
        List<ObjectSpecificationType> objectSpecs = authRead.getObject();
        assertEquals("Wrong number of object specs in authorization", 1, objectSpecs.size());
        ObjectSpecificationType objectSpec = objectSpecs.get(0);
        List<SpecialObjectSpecificationType> specials = objectSpec.getSpecial();
        assertEquals("Wrong number of specials in object specs in authorization", 1, specials.size());
        SpecialObjectSpecificationType special = specials.get(0);
        assertEquals("Wrong special in object specs in authorization", SpecialObjectSpecificationType.SELF, special);
    }
	
	private AuthorizationType findAutz(List<AuthorizationType> authorizations, String actionUrl) {
		for (AuthorizationType authorization: authorizations) {
			if (authorization.getAction().contains(actionUrl)) {
				return authorization;
			}
		}
		return null;
	}

	@Test
    public void test010GetUserAdministrator() throws Exception {
		final String TEST_NAME = "test010GetUserAdministrator";
        TestUtil.displayTestTile(this, TEST_NAME);
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);

        // WHEN
        MidPointPrincipal principal = userDetailsService.getPrincipal(USER_ADMINISTRATOR_USERNAME);
        
        // THEN
        display("Administrator principal", principal);
        assertEquals("Wrong number of authorizations", 1, principal.getAuthorities().size());
        assertHasAuthotizationAllow(principal.getAuthorities().iterator().next(), AuthorizationConstants.AUTZ_ALL_URL);

        assertAuthorized(principal, AUTZ_LOOT_URL);
        assertAuthorized(principal, AUTZ_COMMAND_URL);
	}
		
	@Test
    public void test050GetUserJack() throws Exception {
		final String TEST_NAME = "test050GetUserJack";
        TestUtil.displayTestTile(this, TEST_NAME);
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);

        // WHEN
        MidPointPrincipal principal = userDetailsService.getPrincipal(USER_JACK_USERNAME);
        
        // THEN
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);
        assertJack(principal);
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());

        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);
	}
	
	@Test
    public void test051GetUserBarbossa() throws Exception {
		final String TEST_NAME = "test051GetUserBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);

        // WHEN
        MidPointPrincipal principal = userDetailsService.getPrincipal(USER_BARBOSSA_USERNAME);
        
        // THEN
        display("Principal barbossa", principal);
        assertNotNull("No principal for username "+USER_BARBOSSA_USERNAME, principal);
        assertEquals("wrong username", USER_BARBOSSA_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_BARBOSSA_OID, principal.getOid());
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());
        display("User in principal barbossa", principal.getUser().asPrismObject());
        
        principal.getUser().asPrismObject().checkConsistence(true, true);
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	@Test
    public void test052GetUserGuybrush() throws Exception {
		final String TEST_NAME = "test052GetUserGuybrush";
        TestUtil.displayTestTile(this, TEST_NAME);
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);

        // WHEN
        MidPointPrincipal principal = userDetailsService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
        // THEN
        display("Principal guybrush", principal);
        assertEquals("wrong username", USER_GUYBRUSH_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_GUYBRUSH_OID, principal.getOid());
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());
        display("User in principal guybrush", principal.getUser().asPrismObject());
        
        principal.getUser().asPrismObject().checkConsistence(true, true);
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	
	@Test
    public void test100JackRolePirate() throws Exception {
		final String TEST_NAME = "test100JackRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // WHEN
        MidPointPrincipal principal = userDetailsService.getPrincipal(USER_JACK_USERNAME);
        
        // THEN
        assertJack(principal);
        
        assertEquals("Wrong number of authorizations", 1, principal.getAuthorities().size());
        assertHasAuthotizationAllow(principal.getAuthorities().iterator().next(), AUTZ_LOOT_URL);
        
        assertAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	@Test
    public void test109JackUnassignRolePirate() throws Exception {
		final String TEST_NAME = "test100JackRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // WHEN
        MidPointPrincipal principal = userDetailsService.getPrincipal(USER_JACK_USERNAME);
        
        // THEN
        assertJack(principal);
        
        assertEquals("Wrong number of authorizations", 0, principal.getAuthorities().size());
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	@Test
    public void test110GuybrushRoleNicePirate() throws Exception {
		final String TEST_NAME = "test110GuybrushRoleNicePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assignRole(USER_GUYBRUSH_OID, ROLE_NICE_PIRATE_OID, task, result);
        
        // WHEN
        MidPointPrincipal principal = userDetailsService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
        // THEN
        display("Principal guybrush", principal);
        assertEquals("Wrong number of authorizations", 2, principal.getAuthorities().size());
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	@Test
    public void test111GuybrushRoleCaptain() throws Exception {
		final String TEST_NAME = "test111GuybrushRoleCaptain";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assignRole(USER_GUYBRUSH_OID, ROLE_CAPTAIN_OID, task, result);
        
        // WHEN
        MidPointPrincipal principal = userDetailsService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
        // THEN
        display("Principal guybrush", principal);
        assertEquals("Wrong number of authorizations", 3, principal.getAuthorities().size());
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	// Authorization tests: logged-in user jack
	
	@Test
    public void test200AutzJackNoRole() throws Exception {
		final String TEST_NAME = "test200AutzJackNoRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
	}
	
	@Test
    public void test201AutzJackSuperuserRole() throws Exception {
		final String TEST_NAME = "test201AutzJackSuperuserRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SUPERUSER_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadAllow();
        assertAddAllow();
        assertModifyAllow();
        assertDeleteAllow();        
	}
	
	@Test
    public void test202AutzJackReadonlyRole() throws Exception {
		final String TEST_NAME = "test202AutzJackReadonlyRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
	}
	
	@Test
    public void test203AutzJackReadonlyDeepRole() throws Exception {
		final String TEST_NAME = "test203AutzJackReadonlyDeepRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_DEEP_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
	}
	
	@Test
    public void test204AutzJackSelfRole() throws Exception {
		final String TEST_NAME = "test204AutzJackSelfRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        
        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);
	}
	
	@Test
    public void test205AutzJackObjectFilterCaribbeanfRole() throws Exception {
		final String TEST_NAME = "test205AutzJackObjectFilterCaribbeanfRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_OBJECT_FILTER_CARIBBEAN_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadAllow();

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
	}
	
	private void cleanupAutzTest(String userOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, IOException {
		login(userAdministrator);
        unassignAllRoles(userOid);
        
        Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".cleanupAutzTest");
        OperationResult result = task.getResult();
        
        cleanupDelete(UserType.class, USER_HERMAN_OID, task, result);
        cleanupDelete(UserType.class, USER_DRAKE_OID, task, result);
        cleanupDelete(UserType.class, USER_RAPP_OID, task, result);
        cleanupAdd(USER_LARGO_FILE, task, result);
        cleanupAdd(USER_LECHUCK_FILE, task, result);
        
        modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, task, result);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, task, result, PrismTestUtil.createPolyString("Wannabe"));
	}
	
	private void cleanupAdd(File userLargoFile, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		try {
			addObject(userLargoFile, task, result);
		} catch (ObjectAlreadyExistsException e) {
			// this is OK
			result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
		}
	}

	private <O extends ObjectType> void cleanupDelete(Class<O> type, String oid, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, ObjectAlreadyExistsException {
		try {
			deleteObject(type, oid, task, result);
		} catch (ObjectNotFoundException e) {
			// this is OK
			result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
		}
	}

	private void assertReadDeny() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
	}

	private void assertReadAllow() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
	}
	
	private void assertAddDeny() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
		assertAddDeny(USER_HERMAN_FILE);
		assertAddDeny(USER_DRAKE_FILE, ModelExecuteOptions.createRaw());
		assertImportStreamDeny(USER_RAPP_FILE);
	}

	private void assertAddAllow() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		assertAddAllow(USER_HERMAN_FILE);
		assertAddAllow(USER_DRAKE_FILE, ModelExecuteOptions.createRaw());
		assertImportStreamAllow(USER_RAPP_FILE);
	}

	private void assertModifyDeny() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		// self-modify, common property
		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
		assertModifyDenyOptions(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, ModelExecuteOptions.createRaw(), PrismTestUtil.createPolyString("CSc"));
		// TODO: self-modify password
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
		// TODO: modify other objects
	}

	private void assertModifyAllow() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		// self-modify, common property
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
		assertModifyAllowOptions(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, ModelExecuteOptions.createRaw(), PrismTestUtil.createPolyString("CSc"));
		// TODO: self-modify password
		assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
		// TODO: modify other objects
	}

	private void assertDeleteDeny() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertDeleteDeny(UserType.class, USER_LARGO_OID);
		assertDeleteDeny(UserType.class, USER_LECHUCK_OID, ModelExecuteOptions.createRaw());
	}

	private void assertDeleteAllow() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertDeleteAllow(UserType.class, USER_LARGO_OID);
		assertDeleteAllow(UserType.class, USER_LECHUCK_OID, ModelExecuteOptions.createRaw());
	}
	
	private <O extends ObjectType> void assertGetDeny(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		assertGetDeny(type, oid, null);
	}
	
	private <O extends ObjectType> void assertGetDeny(Class<O> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertGetDeny");
        OperationResult result = task.getResult();
		try {
			PrismObject<O> object = modelService.getObject(type, oid, options, task, result);
			AssertJUnit.fail("Expected get of "+object+" to fail. But it was successful");
		} catch (SecurityViolationException e) {
			// this is expected
			result.computeStatus();
			TestUtil.assertFailure(result);
		}
	}
	
	private <O extends ObjectType> void assertGetAllow(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		assertGetAllow(type, oid, null);
	}
	
	private <O extends ObjectType> void assertGetAllow(Class<O> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertGetAllow");
        OperationResult result = task.getResult();
		PrismObject<O> object = modelService.getObject(type, oid, options, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		// TODO: check audit
	}
	
	private void assertAddDeny(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
		assertAddDeny(file, null);
	}
	
	private void assertAddDeny(File file, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertAddDeny");
        OperationResult result = task.getResult();
        PrismObject<?> object = PrismTestUtil.parseObject(file);
    	ObjectDelta<?> addDelta = object.createAddDelta();
        try {
            modelService.executeChanges(MiscSchemaUtil.createCollection(addDelta), options, task, result);
        	AssertJUnit.fail("Expected add of object from file "+file+" to fail. But it was successful");
        } catch (SecurityViolationException e) {
			// this is expected
			result.computeStatus();
			TestUtil.assertFailure(result);
		}
	}

	private void assertAddAllow(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		assertAddAllow(file, null);
	}
	
	private void assertAddAllow(File file, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertAddAllow");
        OperationResult result = task.getResult();
        PrismObject<?> object = PrismTestUtil.parseObject(file);
    	ObjectDelta<?> addDelta = object.createAddDelta();
    	modelService.executeChanges(MiscSchemaUtil.createCollection(addDelta), options, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}
	
	private <O extends ObjectType> void assertModifyDeny(Class<O> type, String oid, QName propertyName, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyDenyOptions(type, oid, propertyName, null, newRealValue);
	}
	
	private <O extends ObjectType> void assertModifyDenyOptions(Class<O> type, String oid, QName propertyName, ModelExecuteOptions options, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertModifyDeny");
        OperationResult result = task.getResult();
        ObjectDelta<O> objectDelta = ObjectDelta.createModificationReplaceProperty(type, oid, new ItemPath(propertyName), prismContext, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        try {
        	modelService.executeChanges(deltas, options, task, result);
        } catch (SecurityViolationException e) {
			// this is expected
			result.computeStatus();
			TestUtil.assertFailure(result);
		}
	}
	
	private <O extends ObjectType> void assertModifyAllow(Class<O> type, String oid, QName propertyName, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyAllowOptions(type, oid, propertyName, null, newRealValue);
	}
	
	private <O extends ObjectType> void assertModifyAllowOptions(Class<O> type, String oid, QName propertyName, ModelExecuteOptions options, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertModifyAllow");
        OperationResult result = task.getResult();
        ObjectDelta<O> objectDelta = ObjectDelta.createModificationReplaceProperty(type, oid, new ItemPath(propertyName), prismContext, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, options, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}

	private <O extends ObjectType> void assertDeleteDeny(Class<O> type, String oid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertDeleteDeny(type, oid, null);
	}
	
	private <O extends ObjectType> void assertDeleteDeny(Class<O> type, String oid, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertDeleteDeny");
        OperationResult result = task.getResult();
        ObjectDelta<O> delta = ObjectDelta.createDeleteDelta(type, oid, prismContext);
        try {
    		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), options, task, result);
		} catch (SecurityViolationException e) {
			// this is expected
			result.computeStatus();
			TestUtil.assertFailure(result);
		}
	}
	
	private <O extends ObjectType> void assertDeleteAllow(Class<O> type, String oid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertDeleteAllow(type, oid, null);
	}
	
	private <O extends ObjectType> void assertDeleteAllow(Class<O> type, String oid, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertDeleteAllow");
        OperationResult result = task.getResult();
        ObjectDelta<O> delta = ObjectDelta.createDeleteDelta(type, oid, prismContext);
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), options, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}
	
	private void assertImportDeny(File file) {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertImportDeny");
        OperationResult result = task.getResult();
        // This does not throw exception, failure is indicated in the result
        modelService.importObjectsFromFile(file, null, task, result);
		result.computeStatus();
		TestUtil.assertFailure(result);        	
	}

	private void assertImportAllow(File file) {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertImportAllow");
        OperationResult result = task.getResult();
        modelService.importObjectsFromFile(file, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}
	
	private void assertImportStreamDeny(File file) throws FileNotFoundException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertImportStreamDeny");
        OperationResult result = task.getResult();
        InputStream stream = new FileInputStream(file);
		// This does not throw exception, failure is indicated in the result
        modelService.importObjectsFromStream(stream, null, task, result);
		result.computeStatus();
		TestUtil.assertFailure(result);        	
	}

	private void assertImportStreamAllow(File file) throws FileNotFoundException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertImportStreamAllow");
        OperationResult result = task.getResult();
        InputStream stream = new FileInputStream(file);
        modelService.importObjectsFromStream(stream, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}

	private void assertJack(MidPointPrincipal principal) {
		display("Principal jack", principal);
        assertEquals("wrong username", USER_JACK_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_JACK_OID, principal.getOid());
		assertJack(principal.getUser());		
	}
	
	private void assertJack(UserType userType) {
        display("User in principal jack", userType.asPrismObject());
        assertUserJack(userType.asPrismObject());
        
        userType.asPrismObject().checkConsistence(true, true);		
	}
	
	private void assertHasAuthotizationAllow(Authorization authorization, String... action) {
		assertNotNull("Null authorization", authorization);
		assertEquals("Wrong decision in "+authorization, AuthorizationDecisionType.ALLOW, authorization.getDecision());
		TestUtil.assertSetEquals("Wrong action in "+authorization, authorization.getAction(), action);
	}

	private void assertAuthorized(MidPointPrincipal principal, String action) throws SchemaException {
		SecurityContext origContext = SecurityContextHolder.getContext();
		createSecurityContext(principal);
		try {
			assertTrue("AuthorizationEvaluator.isAuthorized: Principal "+principal+" NOT authorized for action "+action, 
					securityEnforcer.isAuthorized(action, null, null, null));
			securityEnforcer.decide(SecurityContextHolder.getContext().getAuthentication(), createSecureObject(), 
					createConfigAttributes(action));
		} finally {
			SecurityContextHolder.setContext(origContext);
		}
	}
	
	private void assertNotAuthorized(MidPointPrincipal principal, String action) throws SchemaException {
		SecurityContext origContext = SecurityContextHolder.getContext();
		createSecurityContext(principal);
		boolean isAuthorized = securityEnforcer.isAuthorized(action, null, null, null);
		SecurityContextHolder.setContext(origContext);
		assertFalse("AuthorizationEvaluator.isAuthorized: Principal "+principal+" IS authorized for action "+action+" but he should not be", isAuthorized);
	}

	private void createSecurityContext(MidPointPrincipal principal) {
		SecurityContext context = new SecurityContextImpl();
		Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null);
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}
	
	private Object createSecureObject() {
		return new FilterInvocation("/midpoint", "whateverServlet", "doSomething");
	}

	private Collection<ConfigAttribute> createConfigAttributes(String action) {
		Collection<ConfigAttribute> attrs = new ArrayList<ConfigAttribute>();
		attrs.add(new SecurityConfig(action));
		return attrs;
	}
}
