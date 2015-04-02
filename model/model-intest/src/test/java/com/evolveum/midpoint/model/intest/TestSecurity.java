/*
 * Copyright (c) 2010-2015 Evolveum
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

import static org.testng.AssertJUnit.assertNull;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OwnedObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SpecialObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

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

	private static final File USER_MANCOMB_FILE = new File(TEST_DIR, "user-mancomb.xml");
	private static final String USER_MANCOMB_OID = "00000000-0000-0000-0000-110000000011";
	
	private static final File USER_ESTEVAN_FILE = new File(TEST_DIR, "user-estevan.xml");
	private static final String USER_ESTEVAN_OID = "00000000-0000-0000-0000-110000000012";
	
	private static final File USER_ANGELICA_FILE = new File(TEST_DIR, "user-angelica.xml");
	private static final String USER_ANGELICA_NAME = "angelika";

	private static final String USER_RUM_ROGERS_NAME = "rum";
	
	protected static final File ROLE_READONLY_FILE = new File(TEST_DIR, "role-readonly.xml");
	protected static final String ROLE_READONLY_OID = "00000000-0000-0000-0000-00000000aa01";
	protected static final File ROLE_READONLY_REQ_FILE = new File(TEST_DIR, "role-readonly-req.xml");
	protected static final String ROLE_READONLY_REQ_OID = "00000000-0000-0000-0000-00000000ab01";
	protected static final File ROLE_READONLY_EXEC_FILE = new File(TEST_DIR, "role-readonly-exec.xml");
	protected static final String ROLE_READONLY_EXEC_OID = "00000000-0000-0000-0000-00000000ae01";
	protected static final File ROLE_READONLY_REQ_EXEC_FILE = new File(TEST_DIR, "role-readonly-req-exec.xml");
	protected static final String ROLE_READONLY_REQ_EXEC_OID = "00000000-0000-0000-0000-00000000ab01";
	
	protected static final File ROLE_READONLY_DEEP_FILE = new File(TEST_DIR, "role-readonly-deep.xml");
	protected static final String ROLE_READONLY_DEEP_OID = "00000000-0000-0000-0000-00000000aa02";
	protected static final File ROLE_READONLY_DEEP_EXEC_FILE = new File(TEST_DIR, "role-readonly-deep-exec.xml");
	protected static final String ROLE_READONLY_DEEP_EXEC_OID = "00000000-0000-0000-0000-00000000ae02";
	
	protected static final File ROLE_SELF_FILE = new File(TEST_DIR, "role-self.xml");
	protected static final String ROLE_SELF_OID = "00000000-0000-0000-0000-00000000aa03";
	
	protected static final File ROLE_OBJECT_FILTER_MODIFY_CARIBBEAN_FILE = new File(TEST_DIR, "role-filter-object-modify-caribbean.xml");
	protected static final String ROLE_OBJECT_FILTER_MODIFY_CARIBBEAN_OID = "00000000-0000-0000-0000-00000000aa04";
	
	protected static final File ROLE_PROP_READ_ALL_MODIFY_SOME_FILE = new File(TEST_DIR, "role-prop-read-all-modify-some.xml");
	protected static final String ROLE_PROP_READ_ALL_MODIFY_SOME_OID = "00000000-0000-0000-0000-00000000aa05";
	
	protected static final File ROLE_MASTER_MINISTRY_OF_RUM_FILE = new File(TEST_DIR, "role-org-master-ministry-of-rum.xml");
	protected static final String ROLE_MASTER_MINISTRY_OF_RUM_OID = "00000000-0000-0000-0000-00000000aa06";
	
	protected static final File ROLE_OBJECT_FILTER_CARIBBEAN_FILE = new File(TEST_DIR, "role-filter-object-caribbean.xml");
	protected static final String ROLE_OBJECT_FILTER_CARIBBEAN_OID = "00000000-0000-0000-0000-00000000aa07";
	
	protected static final File ROLE_PROP_READ_SOME_MODIFY_SOME_FILE = new File(TEST_DIR, "role-prop-read-some-modify-some.xml");
	protected static final String ROLE_PROP_READ_SOME_MODIFY_SOME_OID = "00000000-0000-0000-0000-00000000aa08";
	protected static final File ROLE_PROP_READ_SOME_MODIFY_SOME_REQ_EXEC_FILE = new File(TEST_DIR, "role-prop-read-some-modify-some-req-exec.xml");
	protected static final String ROLE_PROP_READ_SOME_MODIFY_SOME_REQ_EXEC_OID = "00000000-0000-0000-0000-00000000ac08";

	protected static final File ROLE_SELF_ACCOUNTS_READ_FILE = new File(TEST_DIR, "role-self-accounts-read.xml");
	protected static final String ROLE_SELF_ACCOUNTS_READ_OID = "00000000-0000-0000-0000-00000000aa09";
	
	protected static final File ROLE_SELF_ACCOUNTS_READ_WRITE_FILE = new File(TEST_DIR, "role-self-accounts-read-write.xml");
	protected static final String ROLE_SELF_ACCOUNTS_READ_WRITE_OID = "00000000-0000-0000-0000-00000000aa0a";
	
	protected static final File ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_FILE = new File(TEST_DIR, "role-self-accounts-partial-control.xml");
	protected static final String ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_OID = "00000000-0000-0000-0000-00000000aa0b";

	protected static final File ROLE_ASSIGN_APPLICATION_ROLES_FILE = new File(TEST_DIR, "role-assign-application-roles.xml");
	protected static final String ROLE_ASSIGN_APPLICATION_ROLES_OID = "00000000-0000-0000-0000-00000000aa0c";
	
	protected static final File ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM_FILE = new File(TEST_DIR, "role-org-read-orgs-ministry-of-rum.xml");
	protected static final String ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM_OID = "00000000-0000-0000-0000-00000000aa0d";

	protected static final File ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS_FILE = new File(TEST_DIR, "role-filter-object-user-location-shadows.xml");
	protected static final String ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS_OID = "00000000-0000-0000-0000-00000000aa0e";
	
	protected static final File ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS_FILE = new File(TEST_DIR, "role-filter-object-user-type-shadow.xml");
	protected static final String ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS_OID = "00000000-0000-0000-0000-00000000aa0h";
	
	protected static final File ROLE_END_USER_FILE = new File(TEST_DIR, "role-end-user.xml");
	protected static final String ROLE_END_USER_OID = "00000000-0000-0000-0000-00000000aa0f";
	
	protected static final File ROLE_MODIFY_USER_FILE = new File(TEST_DIR, "role-modify-user.xml");
	protected static final String ROLE_MODIFY_USER_OID = "00000000-0000-0000-0000-00000000aa0g";

	protected static final File ROLE_APPLICATION_1_FILE = new File(TEST_DIR, "role-application-1.xml");
	protected static final String ROLE_APPLICATION_1_OID = "00000000-0000-0000-0000-00000000aaa1";

	protected static final File ROLE_APPLICATION_2_FILE = new File(TEST_DIR, "role-application-2.xml");
	protected static final String ROLE_APPLICATION_2_OID = "00000000-0000-0000-0000-00000000aaa2";

	protected static final File ROLE_BUSINESS_1_FILE = new File(TEST_DIR, "role-business-1.xml");
	protected static final String ROLE_BUSINESS_1_OID = "00000000-0000-0000-0000-00000000aab1";
	
	protected static final File ROLE_CONDITIONAL_FILE = new File(TEST_DIR, "role-conditional.xml");
	protected static final String ROLE_CONDITIONAL_OID = "00000000-0000-0000-0000-00000000aac1";
	
	protected static final File ROLE_BASIC_FILE = new File(TEST_DIR, "role-basic.xml");
	protected static final String ROLE_BASIC_OID = "00000000-0000-0000-0000-00000000aad1";

	private static final String LOG_PREFIX_FAIL = "SSSSS=X ";
	private static final String LOG_PREFIX_ATTEMPT = "SSSSS=> ";
	private static final String LOG_PREFIX_DENY = "SSSSS=- ";
	private static final String LOG_PREFIX_ALLOW = "SSSSS=+ ";
	
	String userRumRogersOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		repoAddObjectFromFile(ROLE_READONLY_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_READONLY_REQ_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_READONLY_EXEC_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_READONLY_REQ_EXEC_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_READONLY_DEEP_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_READONLY_DEEP_EXEC_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_SELF_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_OBJECT_FILTER_MODIFY_CARIBBEAN_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_PROP_READ_ALL_MODIFY_SOME_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_MASTER_MINISTRY_OF_RUM_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_OBJECT_FILTER_CARIBBEAN_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_PROP_READ_SOME_MODIFY_SOME_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_PROP_READ_SOME_MODIFY_SOME_REQ_EXEC_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_SELF_ACCOUNTS_READ_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_SELF_ACCOUNTS_READ_WRITE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_ASSIGN_APPLICATION_ROLES_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS_FILE, RoleType.class, initResult);
		
		repoAddObjectFromFile(ROLE_APPLICATION_1_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_APPLICATION_2_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_BUSINESS_1_FILE, RoleType.class, initResult);
		
		repoAddObjectFromFile(ROLE_CONDITIONAL_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_BASIC_FILE, RoleType.class, initResult);
		
		repoAddObjectFromFile(ROLE_END_USER_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_MODIFY_USER_FILE, RoleType.class, initResult);
		
		assignOrg(USER_GUYBRUSH_OID, ORG_SWASHBUCKLER_SECTION_OID, initTask, initResult);
		
		PrismObject<UserType> userRum = createUser(USER_RUM_ROGERS_NAME, "Rum Rogers");
		addObject(userRum, initTask, initResult);
		userRumRogersOid = userRum.getOid();
		assignOrg(userRumRogersOid, ORG_MINISTRY_OF_RUM_OID, initTask, initResult);
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
        AuthorizationType authRead = findAutz(authorizations, ModelAuthorizationAction.READ.getUrl());
        assertEquals("Wrong action in authorization", ModelAuthorizationAction.READ.getUrl(), authRead.getAction().get(0));
        List<OwnedObjectSpecificationType> objectSpecs = authRead.getObject();
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
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_ADMINISTRATOR_USERNAME);
        
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
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_JACK_USERNAME);
        
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
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_BARBOSSA_USERNAME);
        
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
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
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
    public void test060GuybrushConditionalRoleFalse() throws Exception {
		final String TEST_NAME = "test060GuybrushConditionalRoleFalse";
        TestUtil.displayTestTile(this, TEST_NAME);
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);
        
        assignRole(USER_GUYBRUSH_OID, ROLE_CONDITIONAL_OID);

        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
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
    public void test061GuybrushConditionalRoleTrue() throws Exception {
		final String TEST_NAME = "test061GuybrushConditionalRoleTrue";
        TestUtil.displayTestTile(this, TEST_NAME);
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);
        
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_EMPLOYEE_TYPE, task, result, "looser");

        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
        // THEN
        display("Principal guybrush", principal);
        assertEquals("wrong username", USER_GUYBRUSH_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_GUYBRUSH_OID, principal.getOid());
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());
        display("User in principal guybrush", principal.getUser().asPrismObject());
        
        principal.getUser().asPrismObject().checkConsistence(true, true);
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
        assertNotAuthorized(principal, AUTZ_CAPSIZE_URL);
	}
	
	@Test
    public void test062GuybrushConditionalRoleUnassign() throws Exception {
		final String TEST_NAME = "test062GuybrushConditionalRoleUnassign";
        TestUtil.displayTestTile(this, TEST_NAME);
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);
        
        unassignRole(USER_GUYBRUSH_OID, ROLE_CONDITIONAL_OID);

        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
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
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_JACK_USERNAME);
        
        // THEN
        assertJack(principal);
        
        assertEquals("Wrong number of authorizations", 1, principal.getAuthorities().size());
        assertHasAuthotizationAllow(principal.getAuthorities().iterator().next(), AUTZ_LOOT_URL);
        
        assertAuthorized(principal, AUTZ_LOOT_URL, AuthorizationPhaseType.EXECUTION);
        assertNotAuthorized(principal, AUTZ_LOOT_URL, AuthorizationPhaseType.REQUEST);
        assertNotAuthorized(principal, AUTZ_LOOT_URL, null);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	@Test
    public void test109JackUnassignRolePirate() throws Exception {
		final String TEST_NAME = "test109JackUnassignRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_JACK_USERNAME);
        
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
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
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
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
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
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        RoleSelectionSpecification roleSpec = getAssignableRoleSpecification(userJack);
        assertNotNull("Null role spec "+roleSpec, roleSpec);
        assertRoleTypes(roleSpec);
        assertFilter(roleSpec.getFilter(), NoneFilter.class);
        
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
        
        RoleSelectionSpecification roleSpec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertNotNull("Null role spec "+roleSpec, roleSpec);
        assertNull("Non-null role types in spec "+roleSpec, roleSpec.getRoleTypes());
        assertFilter(roleSpec.getFilter(), null);
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

	/**
	 * Authorized only for request but not execution. Everything should be denied.
	 */
	@Test
    public void test202rAutzJackReadonlyReqRole() throws Exception {
		final String TEST_NAME = "test202rAutzJackReadonlyReqRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_REQ_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
	}
	
	/**
	 * Authorized only for execution but not request. Everything should be denied.
	 */
	@Test
    public void test202eAutzJackReadonlyExecRole() throws Exception {
		final String TEST_NAME = "test202eAutzJackReadonlyExecRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_EXEC_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
	}
	
	@Test
    public void test202reAutzJackReadonlyReqExecRole() throws Exception {
		final String TEST_NAME = "test202reAutzJackReadonlyReqExecRole";
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
    public void test203eAutzJackReadonlyDeepExecRole() throws Exception {
		final String TEST_NAME = "test203eAutzJackReadonlyDeepExecRole";
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
        
        assertVisibleUsers(1);
        // The search wit ObjectClass is important. It is a very different case
        // than searching just for UserType
        assertSearch(ObjectType.class, null, 1);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        
        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);
	}
	
	@Test
    public void test205AutzJackObjectFilterModifyCaribbeanfRole() throws Exception {
		final String TEST_NAME = "test205AutzJackObjectFilterModifyCaribbeanfRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_OBJECT_FILTER_MODIFY_CARIBBEAN_OID);
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
	
	@Test
    public void test207AutzJackObjectFilterCaribbeanRole() throws Exception {
		final String TEST_NAME = "test207AutzJackObjectFilterCaribbeanfRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_OBJECT_FILTER_CARIBBEAN_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, 2);
        assertSearch(ObjectType.class, null, 2);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(ObjectType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        assertSearch(ObjectType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
	}
	
	@Test
    public void test210AutzJackPropReadAllModifySome() throws Exception {
		final String TEST_NAME = "test210AutzJackPropReadAllModifySome";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_ALL_MODIFY_SOME_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadAllow();

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
	}
	
	@Test
    public void test215AutzJackPropReadSomeModifySome() throws Exception {
		final String TEST_NAME = "test215AutzJackPropReadSomeModifySome";
		testAutzJackPropReadSomeModifySome(TEST_NAME, ROLE_PROP_READ_SOME_MODIFY_SOME_OID);
	}

	@Test
    public void test215reAutzJackPropReadSomeModifySomeReqExec() throws Exception {
		final String TEST_NAME = "test215reAutzJackPropReadSomeModifySomeReqExec";
		testAutzJackPropReadSomeModifySome(TEST_NAME, ROLE_PROP_READ_SOME_MODIFY_SOME_REQ_EXEC_OID);
	}

    public void testAutzJackPropReadSomeModifySome(final String TEST_NAME, String roleOid) throws Exception {
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, roleOid);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadAllow();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ADDITIONAL_NAME, PrismTestUtil.createPolyString("Captain"));
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_USERNAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
        PrismAsserts.assertPropertyValue(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
        	ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userJack, UserType.F_GIVEN_NAME);
        PrismAsserts.assertNoItem(userJack, UserType.F_FAMILY_NAME);
        PrismAsserts.assertNoItem(userJack, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
        PrismAsserts.assertNoItem(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
        assertAssignmentsWithTargets(userJack, 1);
        
        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        display("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, false, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, false, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_METADATA, false, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), true, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), false, false, false);
        
        PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
        display("Guybrush", userGuybrush);
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_USERNAME));
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_FULL_NAME));
        PrismAsserts.assertPropertyValue(userGuybrush, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
            	ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_GIVEN_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_FAMILY_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_DESCRIPTION);
        PrismAsserts.assertNoItem(userGuybrush, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
        assertAssignmentsWithTargets(userGuybrush, 3);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PrismTestUtil.createPolyString("Brethren of the Coast"));
        
        assertDeleteDeny();
	}

	@Test
    public void test230AutzJackMasterMinistryOfRum() throws Exception {
		final String TEST_NAME = "test230AutzJackMasterMinistryOfRum";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_MASTER_MINISTRY_OF_RUM_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadDeny(2);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGetAllow(UserType.class, userRumRogersOid);
        assertModifyAllow(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertAddAllow(USER_MANCOMB_FILE);
        
        assertVisibleUsers(3);
        
        assertDeleteAllow(UserType.class, USER_ESTEVAN_OID);
        
        assertVisibleUsers(2);
	}

	@Test
    public void test232AutzJackReadOrgMinistryOfRum() throws Exception {
		final String TEST_NAME = "test232AutzJackReadOrgMinistryOfRum";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadDeny(0);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertSearch(OrgType.class, null, 1);
        // The search wit ObjectClass is important. It is a very different case
        // than searching just for UserType or OrgType
        assertSearch(ObjectType.class, null, 1);
        
        assertGetDeny(UserType.class, userRumRogersOid);
        assertModifyDeny(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertAddDeny(USER_MANCOMB_FILE);
	}

	@Test
    public void test250AutzJackSelfAccountsRead() throws Exception {
		final String TEST_NAME = "test250AutzJackSelfAccountsRead";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        
        // precondition
        PrismObject<ShadowType> elaineShadow = getObject(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        assertNotNull(elaineShadow);
        display("Elaine's shadow", elaineShadow);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_READ_OID);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
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
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        
        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED_FILE);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY_FILE);
        
        // Linked to jack
        assertDeny("add jack's account to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result);
			}
		});
        
        // Linked to other user
        assertDeny("add jack's account to gyubrush", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				modifyUserAddAccount(USER_GUYBRUSH_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result);
			}
		});
        
        assertDeleteDeny(ShadowType.class, accountOid);
        assertDeleteDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        PrismObjectDefinition<UserType> userEditSchema = getEditObjectDefinition(user);
        // TODO: assert items
        
        PrismObjectDefinition<ShadowType> shadowEditSchema = getEditObjectDefinition(shadow);
        // TODO: assert items
	}

	@Test
    public void test255AutzJackSelfAccountsReadWrite() throws Exception {
		final String TEST_NAME = "test255AutzJackSelfAccountsReadWrite";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_READ_WRITE_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
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
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        RefinedObjectClassDefinition rOcDef = modelInteractionService.getEditObjectClassDefinition(shadow, resourceDummy, null);
        display("Refined objectclass def", rOcDef);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, true, true);
        
        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED_FILE);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY_FILE);
        
        // Linked to jack
        assertAllow("add jack's account to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result);
			}
		});
        user = getUser(USER_JACK_OID);
        display("Jack after red account link", user);
        String accountRedOid = getLinkRefOid(user, RESOURCE_DUMMY_RED_OID);
        assertNotNull("Strange, red account not linked to jack", accountRedOid);
        
        // Linked to other user
        assertDeny("add gyubrush's account", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				modifyUserAddAccount(USER_LARGO_OID, ACCOUNT_HERMAN_DUMMY_FILE, task, result);
			}
		});
        
        assertDeleteAllow(ShadowType.class, accountRedOid);
        assertDeleteDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
	}

    @Test
    public void test256AutzJackSelfAccountsPartialControl() throws Exception {
        final String TEST_NAME = "test256AutzJackSelfAccountsPartialControl";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_OID);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        login(USER_JACK_USERNAME);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_NICK_NAME, PrismTestUtil.createPolyString("jackie"));
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));

        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        RefinedObjectClassDefinition rOcDef = modelInteractionService.getEditObjectClassDefinition(shadow, resourceDummy, null);
        display("Refined objectclass def", rOcDef);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, false, false);
        assertAttributeFlags(rOcDef, new QName("location"), true, true, true);
        assertAttributeFlags(rOcDef, new QName("weapon"), true, false, false);

        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED_FILE);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY_FILE);

//        // Linked to jack
//        assertAllow("add jack's account to jack", new Attempt() {
//            @Override
//            public void run(Task task, OperationResult result) throws Exception {
//                modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result);
//            }
//        });
//        user = getUser(USER_JACK_OID);
//        display("Jack after red account link", user);
//        String accountRedOid = getLinkRefOid(user, RESOURCE_DUMMY_RED_OID);
//        assertNotNull("Strange, red account not linked to jack", accountRedOid);
//
//        // Linked to other user
//        assertDeny("add gyubrush's account", new Attempt() {
//            @Override
//            public void run(Task task, OperationResult result) throws Exception {
//                modifyUserAddAccount(USER_LARGO_OID, ACCOUNT_HERMAN_DUMMY_FILE, task, result);
//            }
//        });
//
//        assertDeleteAllow(ShadowType.class, accountRedOid);
//        assertDeleteDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
    }

    @Test
    public void test260AutzJackObjectFilterLocationShadowRole() throws Exception {
		final String TEST_NAME = "test260AutzJackObjectFilterLocationShadowRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, 2);
        assertSearch(ObjectType.class, null, 8);
        assertSearch(OrgType.class, null, 6);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(ObjectType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        assertSearch(ObjectType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
        
        // Linked to jack
        assertAllow("add jack's account to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result);
			}
		});
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("Jack after red account link", user);
        String accountRedOid = getLinkRefOid(user, RESOURCE_DUMMY_RED_OID);
        assertNotNull("Strange, red account not linked to jack", accountRedOid);
        assertGetAllow(ShadowType.class, accountRedOid);
	}


    /**
     * creates user and assigns role at the same time
     * @throws Exception
     */
    @Test
    public void test261AutzAngelicaObjectFilterLocationCreateUserShadowRole() throws Exception {
		final String TEST_NAME = "test261AutzJackObjectFilterLocationCreateUserShadowRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS_OID);
        login(USER_JACK_USERNAME);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);

		assertAllow("add user angelica", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				addObject(USER_ANGELICA_FILE, task, result);
			}
		});

        // THEN
		TestUtil.displayThen(TEST_NAME);

		login(USER_ADMINISTRATOR_USERNAME);                 // user jack seemingly has no rights to search for angelika

		PrismObject<UserType> angelica = findUserByUsername(USER_ANGELICA_NAME);
		display("angelica", angelica);
		assertUser(angelica, null, USER_ANGELICA_NAME, "angelika", "angelika", "angelika");
		assertAssignedRole(angelica, ROLE_BASIC_OID);
		assertAccount(angelica, RESOURCE_DUMMY_OID);
	}
    
	@Test
    public void test270AutzJackAssignApplicationRoles() throws Exception {
		final String TEST_NAME = "test270AutzJackAssignApplicationRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_APPLICATION_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(10);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_APPLICATION_ROLES_OID);
        
        assertAllow("assign application role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_APPLICATION_1_OID);

        assertDeny("assign business role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result);
			}
		});

        assertAllow("unassign application role from jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				unassignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result);
			}
		});

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec, "application");
        assertFilter(spec.getFilter(), TypeFilter.class);
	}
	
	@Test
    public void test280AutzJackEndUserAndModify() throws Exception {
		final String TEST_NAME = "test280AutzJackEndUserAndModify";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        
        assignRole(USER_JACK_OID, ROLE_END_USER_OID);
        assignRole(USER_JACK_OID, ROLE_MODIFY_USER_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(10);
        assertAddDeny();
        assertModifyAllow();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        
        assertAllow("modify jack's familyName", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				modifyObjectReplaceProperty(UserType.class, USER_JACK_OID, new ItemPath(UserType.F_FAMILY_NAME), task, result, PrismTestUtil.createPolyString("changed"));
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertUser(user, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, "Jack", "changed");
       
	}


	@Test
    public void test281AutzJackModifyAndEndUser() throws Exception {
		final String TEST_NAME = "test270AutzJackAssignApplicationRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        
        assignRole(USER_JACK_OID, ROLE_MODIFY_USER_OID);
        assignRole(USER_JACK_OID, ROLE_END_USER_OID);
        
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(10);
        assertAddDeny();
        assertModifyAllow();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        
        assertAllow("modify jack's familyName", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				modifyObjectReplaceProperty(UserType.class, USER_JACK_OID, new ItemPath(UserType.F_FAMILY_NAME), task, result, PrismTestUtil.createPolyString("changed"));
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertUser(user, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, "Jack", "changed");

	}
	
	private void assertItemFlags(PrismObjectDefinition<UserType> editSchema, QName itemName, boolean expectedRead, boolean expectedAdd, boolean expectedModify) {
		assertItemFlags(editSchema, new ItemPath(itemName), expectedRead, expectedAdd, expectedModify);
	}
	
	private void assertItemFlags(PrismObjectDefinition<UserType> editSchema, ItemPath itemPath, boolean expectedRead, boolean expectedAdd, boolean expectedModify) {
		ItemDefinition itemDefinition = editSchema.findItemDefinition(itemPath);
		assertEquals("Wrong readability flag for "+itemPath, expectedRead, itemDefinition.canRead());
		assertEquals("Wrong addition flag for "+itemPath, expectedAdd, itemDefinition.canAdd());
		assertEquals("Wrong modification flag for "+itemPath, expectedModify, itemDefinition.canModify());
	}

	private void assertAssignmentsWithTargets(PrismObject<UserType> user, int expectedNumber) {
		PrismContainer<AssignmentType> assignmentContainer = user.findContainer(UserType.F_ASSIGNMENT);
        assertEquals("Unexpected number of assignments in "+user, expectedNumber, assignmentContainer.size());
        for (PrismContainerValue<AssignmentType> cval: assignmentContainer.getValues()) {
        	assertNotNull("No targetRef in assignment in "+user, cval.asContainerable().getTargetRef());
        }
	}
	
	private void assertAttributeFlags(RefinedObjectClassDefinition rOcDef, QName attrName, boolean expectedRead, boolean expectedAdd, boolean expectedModify) {
		RefinedAttributeDefinition rAttrDef = rOcDef.findAttributeDefinition(attrName);
		assertEquals("Wrong readability flag for "+attrName, expectedRead, rAttrDef.canRead());
		assertEquals("Wrong addition flag for "+attrName, expectedAdd, rAttrDef.canAdd());
		assertEquals("Wrong modification flag for "+attrName, expectedModify, rAttrDef.canModify());
	}

	
	private void cleanupAutzTest(String userOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, IOException {
		login(userAdministrator);
        unassignAllRoles(userOid);
        
        Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".cleanupAutzTest");
        OperationResult result = task.getResult();
        
        cleanupDelete(UserType.class, USER_HERMAN_OID, task, result);
        cleanupDelete(UserType.class, USER_DRAKE_OID, task, result);
        cleanupDelete(UserType.class, USER_RAPP_OID, task, result);
        cleanupDelete(UserType.class, USER_MANCOMB_OID, task, result);
        cleanupAdd(USER_LARGO_FILE, task, result);
        cleanupAdd(USER_LECHUCK_FILE, task, result);
        cleanupAdd(USER_ESTEVAN_FILE, task, result);
        
        modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, task, result);
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
        modifyUserReplace(userRumRogersOid, UserType.F_TITLE, task, result);
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
	
	private void assertVisibleUsers(int expectedNumAllUsers) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		assertSearch(UserType.class, null, expectedNumAllUsers);

	}
	
	private void assertReadDeny() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		assertReadDeny(0);
	}

	private void assertReadDeny(int expectedNumAllUsers) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, expectedNumAllUsers);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
	}

	private void assertReadAllow() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		assertReadAllow(9);
	}
	
	private void assertReadAllow(int expectedNumAllUsers) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, expectedNumAllUsers);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
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
			logAttempt("get", type, oid, null);
			PrismObject<O> object = modelService.getObject(type, oid, options, task, result);
			failDeny("get", type, oid, null);
		} catch (SecurityViolationException e) {
			// this is expected
			logDeny("get", type, oid, null);
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
        logAttempt("get", type, oid, null);
		PrismObject<O> object = modelService.getObject(type, oid, options, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		logAllow("get", type, oid, null);
		// TODO: check audit
	}
	
	private <O extends ObjectType> void assertSearch(Class<O> type, ObjectQuery query, int expectedResults) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		assertSearch(type, query, null, expectedResults);
	}
	
	private <O extends ObjectType> void assertSearch(Class<O> type, ObjectQuery query, 
			Collection<SelectorOptions<GetOperationOptions>> options, int expectedResults) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertSearchObjects");
        OperationResult result = task.getResult();
		try {
			logAttempt("search", type, query);
			List<PrismObject<O>> objects = modelService.searchObjects(type, query, options, task, result);
			display("Search returned", objects.toString());
			if (objects.size() > expectedResults) {
				failDeny("search", type, query, expectedResults, objects.size());
			} else if (objects.size() < expectedResults) {
				failAllow("search", type, query, expectedResults, objects.size());
			}
			result.computeStatus();
			TestUtil.assertSuccess(result);
		} catch (SecurityViolationException e) {
			// this should not happen
			result.computeStatus();
			TestUtil.assertFailure(result);
			failAllow("search", type, query, e);
		}

		task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertSearchObjectsIterative");
        result = task.getResult();
		try {
			logAttempt("searchIterative", type, query);
			final List<PrismObject<O>> objects = new ArrayList<>();
			ResultHandler<O> handler = new ResultHandler<O>() {
				@Override
				public boolean handle(PrismObject<O> object, OperationResult parentResult) {
					objects.add(object);
					return true;
				}
			};
			modelService.searchObjectsIterative(type, query, handler, options, task, result);
			display("Search iterative returned", objects.toString());
			if (objects.size() > expectedResults) {
				failDeny("searchIterative", type, query, expectedResults, objects.size());
			} else if (objects.size() < expectedResults) {
				failAllow("searchIterative", type, query, expectedResults, objects.size());
			}
			result.computeStatus();
			TestUtil.assertSuccess(result);
		} catch (SecurityViolationException e) {
			// this should not happen
			result.computeStatus();
			TestUtil.assertFailure(result);
			failAllow("searchIterative", type, query, e);
		}
		
		task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertSearchObjects.count");
        result = task.getResult();
		try {
			logAttempt("count", type, query);
			int numObjects = modelService.countObjects(type, query, options, task, result);
			display("Count returned", numObjects);
			if (numObjects > expectedResults) {
				failDeny("count", type, query, expectedResults, numObjects);
			} else if (numObjects < expectedResults) {
				failAllow("count", type, query, expectedResults, numObjects);
			}
			result.computeStatus();
			TestUtil.assertSuccess(result);
		} catch (SecurityViolationException e) {
			// this should not happen
			result.computeStatus();
			TestUtil.assertFailure(result);
			failAllow("search", type, query, e);
		}
	}
	
	private void assertAddDeny(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
		assertAddDeny(file, null);
	}
	
	private <O extends ObjectType> void assertAddDeny(File file, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertAddDeny");
        OperationResult result = task.getResult();
        PrismObject<O> object = PrismTestUtil.parseObject(file);
    	ObjectDelta<O> addDelta = object.createAddDelta();
        try {
        	logAttempt("add", object.getCompileTimeClass(), object.getOid(), null);
            modelService.executeChanges(MiscSchemaUtil.createCollection(addDelta), options, task, result);
            failDeny("add", object.getCompileTimeClass(), object.getOid(), null);
        } catch (SecurityViolationException e) {
			// this is expected
        	logDeny("add", object.getCompileTimeClass(), object.getOid(), null);
			result.computeStatus();
			TestUtil.assertFailure(result);
		}
	}

	private void assertAddAllow(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		assertAddAllow(file, null);
	}
	
	private <O extends ObjectType> void assertAddAllow(File file, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertAddAllow");
        OperationResult result = task.getResult();
        PrismObject<O> object = PrismTestUtil.parseObject(file);
    	ObjectDelta<O> addDelta = object.createAddDelta();
    	logAttempt("add", object.getCompileTimeClass(), object.getOid(), null);
    	try {
    		modelService.executeChanges(MiscSchemaUtil.createCollection(addDelta), options, task, result);
    	} catch (SecurityViolationException e) {
			failAllow("add", object.getCompileTimeClass(), object.getOid(), null, e);
		}
		result.computeStatus();
		TestUtil.assertSuccess(result);
		logAllow("add", object.getCompileTimeClass(), object.getOid(), null);
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
        	logAttempt("modify", type, oid, propertyName);
        	modelService.executeChanges(deltas, options, task, result);
        	failDeny("modify", type, oid, propertyName);
        } catch (SecurityViolationException e) {
			// this is expected
        	logDeny("modify", type, oid, propertyName);
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
		try {
			logAttempt("modify", type, oid, propertyName);
			modelService.executeChanges(deltas, options, task, result);
		} catch (SecurityViolationException e) {
			failAllow("modify", type, oid, propertyName, e);
		}
		result.computeStatus();
		TestUtil.assertSuccess(result);
		logAllow("modify", type, oid, propertyName);
	}

	private <O extends ObjectType> void assertDeleteDeny(Class<O> type, String oid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertDeleteDeny(type, oid, null);
	}
	
	private <O extends ObjectType> void assertDeleteDeny(Class<O> type, String oid, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertDeleteDeny");
        OperationResult result = task.getResult();
        ObjectDelta<O> delta = ObjectDelta.createDeleteDelta(type, oid, prismContext);
        try {
        	logAttempt("delete", type, oid, null);
    		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), options, task, result);
    		failDeny("delete", type, oid, null);
		} catch (SecurityViolationException e) {
			// this is expected
			logDeny("delete", type, oid, null);
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
        logAttempt("delete", type, oid, null);
        try {
        	modelService.executeChanges(MiscSchemaUtil.createCollection(delta), options, task, result);
        } catch (SecurityViolationException e) {
			failAllow("delete", type, oid, null, e);
		}
		result.computeStatus();
		TestUtil.assertSuccess(result);
		logAllow("delete", type, oid, null);
	}
	
	private void assertImportDeny(File file) throws FileNotFoundException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertImportDeny");
        OperationResult result = task.getResult();
        // This does not throw exception, failure is indicated in the result
        modelService.importObjectsFromFile(file, null, task, result);
		result.computeStatus();
		TestUtil.assertFailure(result);
	}

	private void assertImportAllow(File file) throws FileNotFoundException {
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
	
	private <O extends ObjectType> void failDeny(String action, Class<O> type, ObjectQuery query, int expected, int actual) {
		failDeny(action, type, (query==null?"null":query.toString())+", expected "+expected+", actual "+actual);
	}
	
	private <O extends ObjectType> void failDeny(String action, Class<O> type, String oid, QName propertyName) {
		failDeny(action, type, oid+" prop "+propertyName);
	}
	
	private <O extends ObjectType> void failDeny(String action, Class<O> type, String desc) {
		String msg = "Failed to deny "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(LOG_PREFIX_FAIL+msg);
		LOGGER.error(LOG_PREFIX_FAIL+msg);
		AssertJUnit.fail(msg);
	}

	private <O extends ObjectType> void failDeny(String action) {
		String msg = "Failed to deny "+action;
		System.out.println(LOG_PREFIX_FAIL+msg);
		LOGGER.error(LOG_PREFIX_FAIL+msg);
		AssertJUnit.fail(msg);
	}

	private <O extends ObjectType> void failAllow(String action, Class<O> type, ObjectQuery query, SecurityViolationException e) throws SecurityViolationException {
		failAllow(action, type, query==null?"null":query.toString(), e);
	}

	private <O extends ObjectType> void failAllow(String action, Class<O> type, ObjectQuery query, int expected, int actual) throws SecurityViolationException {
		failAllow(action, type, (query==null?"null":query.toString())+", expected "+expected+", actual "+actual, null);
	}

	private <O extends ObjectType> void failAllow(String action, Class<O> type, String oid, QName propertyName, SecurityViolationException e) throws SecurityViolationException {
		failAllow(action, type, oid+" prop "+propertyName, e);
	}
	
	private <O extends ObjectType> void failAllow(String action, Class<O> type, String desc, SecurityViolationException e) throws SecurityViolationException {
		String msg = "Failed to allow "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(LOG_PREFIX_FAIL+msg);
		LOGGER.error(LOG_PREFIX_FAIL+msg);
		if (e != null) {
			throw new SecurityViolationException(msg+": "+e.getMessage(), e);
		} else {
			AssertJUnit.fail(msg);
		}
	}

	private <O extends ObjectType> void failAllow(String action, SecurityViolationException e) throws SecurityViolationException {
		String msg = "Failed to allow "+action;
		System.out.println(LOG_PREFIX_FAIL+msg);
		LOGGER.error(LOG_PREFIX_FAIL+msg);
		if (e != null) {
			throw new SecurityViolationException(msg+": "+e.getMessage(), e);
		} else {
			AssertJUnit.fail(msg);
		}
	}

	private <O extends ObjectType> void logAttempt(String action, Class<O> type, ObjectQuery query) {
		logAttempt(action, type, query==null?"null":query.toString());
	}
	
	private <O extends ObjectType> void logAttempt(String action, Class<O> type, String oid, QName propertyName) {
		logAttempt(action, type, oid+" prop "+propertyName);
	}
	
	private <O extends ObjectType> void logAttempt(String action, Class<O> type, String desc) {
		String msg = LOG_PREFIX_ATTEMPT+"Trying "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(msg);
		LOGGER.info(msg);
	}
	
	private <O extends ObjectType> void logAttempt(String action) {
		String msg = LOG_PREFIX_ATTEMPT+"Trying "+action;
		System.out.println(msg);
		LOGGER.info(msg);
	}
	
	private <O extends ObjectType> void logDeny(String action, Class<O> type, ObjectQuery query) {
		logDeny(action, type, query==null?"null":query.toString());
	}
	
	private <O extends ObjectType> void logDeny(String action, Class<O> type, String oid, QName propertyName) {
		logDeny(action, type, oid+" prop "+propertyName);
	}
	
	private <O extends ObjectType> void logDeny(String action, Class<O> type, String desc) {
		String msg = LOG_PREFIX_DENY+"Denied "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(msg);
		LOGGER.info(msg);
	}
	
	private <O extends ObjectType> void logDeny(String action) {
		String msg = LOG_PREFIX_DENY+"Denied "+action;
		System.out.println(msg);
		LOGGER.info(msg);
	}
	
	private <O extends ObjectType> void logAllow(String action, Class<O> type, ObjectQuery query) {
		logAllow(action, type, query==null?"null":query.toString());
	}
	
	private <O extends ObjectType> void logAllow(String action, Class<O> type, String oid, QName propertyName) {
		logAllow(action, type, oid+" prop "+propertyName);
	}
	
	private <O extends ObjectType> void logAllow(String action, Class<O> type, String desc) {
		String msg = LOG_PREFIX_ALLOW+"Allowed "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(msg);
		LOGGER.info(msg);
	}
	
	private <O extends ObjectType> void logAllow(String action) {
		String msg = LOG_PREFIX_ALLOW+"Allowed "+action;
		System.out.println(msg);
		LOGGER.info(msg);
	}
	
	private <O extends ObjectType> void assertDeny(String opname, Attempt attempt) throws Exception {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertDeny."+opname);
        OperationResult result = task.getResult();
        try {
        	logAttempt(opname);
        	attempt.run(task, result);
            failDeny(opname);
        } catch (SecurityViolationException e) {
			// this is expected
        	logDeny(opname);
			result.computeStatus();
			TestUtil.assertFailure(result);
		}
	}
	
	private <O extends ObjectType> void assertAllow(String opname, Attempt attempt) throws Exception {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertAllow."+opname);
        OperationResult result = task.getResult();
        try {
        	logAttempt(opname);
        	attempt.run(task, result);
        } catch (SecurityViolationException e) {
			failAllow(opname, e);
		}
		result.computeStatus();
		TestUtil.assertSuccess(result);
		logAllow(opname);
	}
	
	interface Attempt {
		void run(Task task, OperationResult result) throws Exception;
	}
	
}
