/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.model.intest.security;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyExceptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Security tests for multitenant environment.
 * 
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityMultitenant extends AbstractSecurityTest {
	
	public static final File TEST_DIR = new File("src/test/resources/security/multitenant");
	
	protected static final File ORG_MULTITENANT_FILE = new File(TEST_DIR, "org-multitenant.xml");
	
	protected static final String ORG_ROOT_OID = "00000000-8888-6666-1111-000000000000";
	
	protected static final String ORG_CORRINO_OID = "00000000-8888-6666-1111-000000001000";
	protected static final String ROLE_CORRINO_ADMIN_OID = "00000000-8888-6666-1111-100000001000";
	
	protected static final String ORG_ATREIDES_OID = "00000000-8888-6666-1111-000000002000";
	protected static final String ROLE_ATREIDES_ADMIN_OID = "00000000-8888-6666-1111-100000002000";
	
	protected static final String ORG_HARKONNEN_OID = "00000000-8888-6666-1111-000000003000";
	protected static final String ROLE_HARKONNEN_ADMIN_OID = "00000000-8888-6666-1111-100000003000";
	
//	protected static final File ROLE_X_FILE = new File(TEST_DIR, "role-vault-dweller.xml");
//	protected static final String ROLE_X_OID = "8d8471f4-2906-11e8-9078-4f2b205aa01d";
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
//		repoAddObjectFromFile(ROLE_VAULT_DWELLER_FILE, initResult);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
	}
	
	@Override
	protected boolean doAddOrgstruct() {
		return false;
	}
	
	@Override
	protected String getTopOrgOid() {
		return ORG_ROOT_OID;
	}
	
	protected static final int NUMBER_OF_IMPORTED_ROLES = 0;
	
	protected int getNumberOfRoles() {
		return super.getNumberOfRoles() + NUMBER_OF_IMPORTED_ROLES;
	}
	
	/**
	 * Stay logged in as administrator. Make sure that our assumptions about
	 * the users and roles are correct.
	 */
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);

        // WHEN
        displayWhen(TEST_NAME);
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS);
        assertSearch(RoleType.class, null, getNumberOfRoles());
        
        assertReadAllow(NUMBER_OF_ALL_USERS);
		assertReadAllowRaw(NUMBER_OF_ALL_USERS);
        assertAddAllow();
        assertAddAllowRaw();
        assertModifyAllow();
        assertDeleteAllow();

        assertGlobalStateUntouched();
	}
	
	/**
	 * Stay logged in as administrator. 
	 * Import orgstruct with tenant and roles and everything.
	 */
	@Test
    public void test010ImportOrgstruct() throws Exception {
		final String TEST_NAME = "test010ImportOrgstruct";
        displayTestTitle(TEST_NAME);
        // GIVEN
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ImportOptionsType options = MiscSchemaUtil.getDefaultImportOptions();
        ModelExecuteOptionsType modelOptions = new ModelExecuteOptionsType();
        modelOptions.setRaw(false);
		options.setModelExecutionOptions(modelOptions);

        // WHEN
        displayWhen(TEST_NAME);
        importObjectFromFile(ORG_MULTITENANT_FILE, options, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        dumpOrgTree();
        
        assertOrgAfter(ORG_ATREIDES_OID)
        	.assertIsTenant()
        	.assignments()
        		.single()
        			.assertTargetOid(ORG_ROOT_OID)
        			.end()
        		.end()
        	.assertLinks(0)
        	.assertParentOrgRefs(ORG_ROOT_OID);
        
        assertRoleAfter(ROLE_CORRINO_ADMIN_OID)
//        	.assertTenantRef(ORG_ATREIDES_OID)
        	.assertParentOrgRefs(ORG_ATREIDES_OID);
        
        assertGlobalStateUntouched();
	}

	/**
	 */
	@Test(enabled=false) // work in progress
    public void test080AutzJackEndUserPassword() throws Exception {
		final String TEST_NAME = "test080AutzJackEndUserPassword";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);

//        assignRole(USER_JACK_OID, ROLE_END_USER_OID);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        displayWhen(TEST_NAME);
        
//        assertAllow("set jack's password",
//        		(task, result) -> modifyUserSetPassword(USER_JACK_OID, "nbusr123", task, result) );
        
        // THEN
        displayThen(TEST_NAME);
        
//        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
//        
//        user = getUser(USER_JACK_OID);
//        display("user after password change", user);
//        PasswordType passwordType = assertUserPassword(user, "nbusr123");
//        MetadataType metadata = passwordType.getMetadata();
//        assertNotNull("No password metadata", metadata);
//        assertMetadata("password metadata", metadata, true, false, startTs, endTs, USER_JACK_OID, SchemaConstants.CHANNEL_GUI_USER_URI);

        assertGlobalStateUntouched();
	}	
	
}
