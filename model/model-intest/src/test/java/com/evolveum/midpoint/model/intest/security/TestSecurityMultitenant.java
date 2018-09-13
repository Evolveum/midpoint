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

import java.io.File;
import java.io.IOException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
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
	
	protected static final String ORG_ROOT_OID = "00000000-8888-6666-a000-000000000000";
	protected static final String ROLE_TENANT_ADMIN_OID = "00000000-8888-6666-a000-100000000000";
	
	
	// ===[ Spacing Guild ]===
	
	protected static final String ORG_GUILD_OID = "00000000-8888-6666-a001-000000000000";
	
	protected static final String ROLE_GUILD_BROKEN_ADMIN_OID = "00000000-8888-6666-a001-100000000001";
	
	protected static final String USER_EDRIC_OID = "00000000-8888-6666-a001-200000000000";
	protected static final String USER_EDRIC_NAME = "edric";
	protected static final String USER_EDRIC_FULL_NAME = "Navigator Edric";
	
	protected static final File USER_DMURR_FILE = new File(TEST_DIR, "user-dmurr.xml");
	protected static final String USER_DMURR_OID = "00000000-8888-6666-a001-200000000001";
	protected static final String USER_DMURR_NAME = "dmurr";
	protected static final String USER_DMURR_FULL_NAME = "D'murr Pilru";
	
	private static final File RESOURCE_DUMMY_JUNCTION_FILE = new File(TEST_DIR, "resource-dummy-junction.xml");
	private static final String RESOURCE_DUMMY_JUNCTION_OID = "00000000-8888-6666-a001-300000000000";
	
	
	// ===[ House Corrino ]===
	
	protected static final String ORG_CORRINO_OID = "00000000-8888-6666-a100-000000000000";
	
	protected static final String ROLE_CORRINO_ADMIN_OID = "00000000-8888-6666-a100-100000000000";
	
	protected static final String USER_SHADDAM_CORRINO_OID = "00000000-8888-6666-a100-200000000000";
	protected static final String USER_SHADDAM_CORRINO_NAME = "shaddam";
	protected static final String USER_SHADDAM_CORRINO_FULL_NAME = "Padishah Emperor Shaddam IV";
	
	
	// ===[ House Atreides ]===
	
	protected static final String ORG_ATREIDES_OID = "00000000-8888-6666-a200-000000000000";
	
	protected static final String ROLE_ATREIDES_ADMIN_OID = "00000000-8888-6666-a200-100000000000";
	
	protected static final String USER_LETO_ATREIDES_OID = "00000000-8888-6666-a200-200000000000";
	protected static final String USER_LETO_ATREIDES_NAME = "leto";
	protected static final String USER_LETO_ATREIDES_FULL_NAME = "Duke Leto Atreides";
	
	protected static final String USER_PAUL_ATREIDES_OID = "00000000-8888-6666-a200-200000000001";
	protected static final String USER_PAUL_ATREIDES_NAME = "paul";
	protected static final String USER_PAUL_ATREIDES_FULL_NAME = "Paul Atreides";
	
	protected static final File USER_DUNCAN_FILE = new File(TEST_DIR, "user-duncan.xml");
	protected static final String USER_DUNCAN_OID = "00000000-8888-6666-a200-200000000002";
	protected static final String USER_DUNCAN_NAME = "duncan";
	protected static final String USER_DUNCAN_FULL_NAME = "Duncan Idaho";
	
	private static final File RESOURCE_DUMMY_CASTLE_CALADAN_FILE = new File(TEST_DIR, "resource-dummy-castle-caladan.xml");
	private static final String RESOURCE_DUMMY_CASTLE_CALADAN_OID = "00000000-8888-6666-a200-300000000000";
	
	// ===[ House Harkonnen ]===
	
	protected static final String ORG_HARKONNEN_OID = "00000000-8888-6666-a300-000000000000";
	
	protected static final String ROLE_HARKONNEN_ADMIN_OID = "00000000-8888-6666-a300-100000000000";
	
	protected static final String USER_VLADIMIR_HARKONNEN_OID = "00000000-8888-6666-a300-200000000000";
	
	protected static final File USER_PITER_FILE = new File(TEST_DIR, "user-piter.xml");
	protected static final String USER_PITER_OID = "00000000-8888-6666-a300-200000000001";
	protected static final String USER_PITER_NAME = "piter";
	protected static final String USER_PITER_FULL_NAME = "Piter De Vries";
	
	private static final File RESOURCE_DUMMY_BARONY_FILE = new File(TEST_DIR, "resource-dummy-barony.xml");
	private static final String RESOURCE_DUMMY_BARONY_OID = "00000000-8888-6666-a300-300000000000";
	
	protected PrismObject<ConnectorType> dummyConnector;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		dummyConnector = findConnectorByTypeAndVersion(CONNECTOR_DUMMY_TYPE, CONNECTOR_DUMMY_VERSION, initResult);
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
	 * Make sure that tenantRefs are properly set (they are NOT part of imported file)
	 * 
	 * MID-4882
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
        	.assertTenantRef(ORG_ATREIDES_OID)
        	.assignments()
        		.single()
        			.assertTargetOid(ORG_ROOT_OID)
        			.end()
        		.end()
        	.assertLinks(0)
        	.assertParentOrgRefs(ORG_ROOT_OID);
        
        assertRoleAfter(ROLE_ATREIDES_ADMIN_OID)
        	.assertTenantRef(ORG_ATREIDES_OID)
        	.assertParentOrgRefs(ORG_ATREIDES_OID);
        
        assertUserAfter(USER_LETO_ATREIDES_OID)
        	.assertName(USER_LETO_ATREIDES_NAME)
        	.assertFullName(USER_LETO_ATREIDES_FULL_NAME)
        	.assignments()
        		.assertOrg(ORG_ATREIDES_OID)
        		.assertRole(ROLE_ATREIDES_ADMIN_OID)
        		.end()
    		.assertTenantRef(ORG_ATREIDES_OID)
        	.assertParentOrgRefs(ORG_ATREIDES_OID)
        	.assertLinks(0);

        assertUserAfter(USER_PAUL_ATREIDES_OID)
	    	.assertName(USER_PAUL_ATREIDES_NAME)
	    	.assertFullName(USER_PAUL_ATREIDES_FULL_NAME)
	    	.assignments()
	    		.assertOrg(ORG_ATREIDES_OID)
	    		.assertNoRole()
	    		.end()
			.assertTenantRef(ORG_ATREIDES_OID)
	    	.assertParentOrgRefs(ORG_ATREIDES_OID)
	    	.assertLinks(0);
        
        assertOrgAfter(ORG_GUILD_OID)
        	.assertTenant(null)
        	.assertTenantRef(null)
        	.assignments()
	    		.single()
	    			.assertTargetOid(ORG_ROOT_OID)
	    			.end()
	    		.end()
	    	.assertLinks(0)
	    	.assertParentOrgRefs(ORG_ROOT_OID);
        
        assertUserAfter(USER_EDRIC_OID)
	    	.assertName(USER_EDRIC_NAME)
	    	.assertFullName(USER_EDRIC_FULL_NAME)
	    	.assignments()
	    		.assertOrg(ORG_GUILD_OID)
	    		.assertRole(ROLE_GUILD_BROKEN_ADMIN_OID)
	    		.end()
			.assertTenantRef(null)
	    	.assertParentOrgRefs(ORG_GUILD_OID)
	    	.assertLinks(0);

        assertGlobalStateUntouched();
	}

	/**
	 * Leto is Atreides admin. He can see all of House Atreides.
	 * But nothing else.
	 * 
	 * MID-4882
	 */
	@Test
    public void test100AutzLetoRead() throws Exception {
		final String TEST_NAME = "test100AutzLetoRead";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);
        
        // WHEN
        displayWhen(TEST_NAME);

        // Matching tenant
        assertGetAllow(UserType.class, USER_LETO_ATREIDES_OID);
        assertGetAllow(UserType.class, USER_PAUL_ATREIDES_OID);
        assertGetAllow(OrgType.class, ORG_ATREIDES_OID);
        assertGetAllow(RoleType.class, ROLE_ATREIDES_ADMIN_OID);
        
        // Wrong tenant
        assertGetDeny(UserType.class, USER_VLADIMIR_HARKONNEN_OID);
        assertGetDeny(OrgType.class, ORG_HARKONNEN_OID);
        assertGetDeny(RoleType.class, ROLE_HARKONNEN_ADMIN_OID);
        
        // No tenant
        assertGetDeny(OrgType.class, ORG_GUILD_OID);
        assertGetDeny(RoleType.class, ROLE_TENANT_ADMIN_OID);
        assertGetDeny(UserType.class, USER_EDRIC_OID);
        
        assertSearch(UserType.class, null, USER_LETO_ATREIDES_OID, USER_PAUL_ATREIDES_OID);
        assertSearch(RoleType.class, null, ROLE_ATREIDES_ADMIN_OID);
        assertSearch(OrgType.class, null, ORG_ATREIDES_OID);
        
        // THEN
        displayThen(TEST_NAME);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-4882
	 */
	@Test
    public void test102AutzLetoAdd() throws Exception {
		final String TEST_NAME = "test102AutzLetoAdd";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);
        
        // WHEN
        displayWhen(TEST_NAME);

        // Matching tenant
        assertAddAllow(USER_DUNCAN_FILE);
        
        // Wrong tenant
        assertAddDeny(USER_PITER_FILE);
        
        // No tenant
        assertAddDeny(USER_DMURR_FILE);
        
        // THEN
        displayThen(TEST_NAME);
        
        login(USER_ADMINISTRATOR_USERNAME);
        
        assertUserAfter(USER_DUNCAN_OID)
        	.assertName(USER_DUNCAN_NAME)
        	.assertFullName(USER_DUNCAN_FULL_NAME)
        	.assignments()
	    		.assertOrg(ORG_ATREIDES_OID)
	    		.assertNoRole()
	    		.end()
			.assertTenantRef(ORG_ATREIDES_OID)
	    	.assertParentOrgRefs(ORG_ATREIDES_OID)
	    	.assertLinks(0);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-4882
	 */
	@Test
    public void test104AutzLetoModify() throws Exception {
		final String TEST_NAME = "test104AutzLetoModify";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);
        
        // WHEN
        displayWhen(TEST_NAME);

        // Matching tenant
        assertModifyAllow(UserType.class, USER_PAUL_ATREIDES_OID, UserType.F_LOCALITY, createPolyString("Arrakis"));
        
        // Wrong tenant
        assertModifyDeny(UserType.class, USER_VLADIMIR_HARKONNEN_OID, UserType.F_LOCALITY, createPolyString("Deepest hell"));
        
        // No tenant
        assertModifyDeny(UserType.class, USER_EDRIC_OID, UserType.F_LOCALITY, createPolyString("Whatever"));
        
        // THEN
        displayThen(TEST_NAME);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-4882
	 */
	@Test
    public void test106AutzLetoAddResourceTask() throws Exception {
		final String TEST_NAME = "test106AutzLetoAddResourceTask";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);
        
        // WHEN
        displayWhen(TEST_NAME);

        // Matching tenant
        assertAddDummyResourceAllow(RESOURCE_DUMMY_CASTLE_CALADAN_FILE);
        
        // Wrong tenant
        assertAddDummyResourceDeny(RESOURCE_DUMMY_BARONY_FILE);
        
        // No tenant
        assertAddDummyResourceDeny(RESOURCE_DUMMY_JUNCTION_FILE);
        
        // THEN
        displayThen(TEST_NAME);
        
        login(USER_ADMINISTRATOR_USERNAME);
                
        assertGlobalStateUntouched();
	}
	
	private void assertAddDummyResourceAllow(File file) throws SchemaException, IOException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(file);
		resource.asObjectable()
			.connectorRef(dummyConnector.getOid(), ConnectorType.COMPLEX_TYPE);
        assertAddAllow(resource, null);
	}
	
	private void assertAddDummyResourceDeny(File file) throws SchemaException, IOException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(file);
		resource.asObjectable()
			.connectorRef(dummyConnector.getOid(), ConnectorType.COMPLEX_TYPE);
        assertAddDeny(resource, null);
	}

	/**
	 * MID-4882
	 */
	@Test
    public void test109AutzLetoDelete() throws Exception {
		final String TEST_NAME = "test109AutzLetoDelete";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(null);

        login(USER_LETO_ATREIDES_NAME);
        
        // WHEN
        displayWhen(TEST_NAME);

        // Matching tenant
        assertDeleteAllow(UserType.class, USER_DUNCAN_OID);
        
        // Wrong tenant
        assertDeleteDeny(UserType.class, USER_PITER_OID);
        
        // No tenant
        assertDeleteDeny(UserType.class, USER_DMURR_OID);
        
        // THEN
        displayThen(TEST_NAME);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * Edric is part of Spacing Guld. But the Guild is not tenant.
	 * Edric has a broken role that should work only for tenants.
	 * Therefore the role should not work. Edric should not be
	 * able to access anything.
	 * 
	 * MID-4882
	 */
	@Test
    public void test120AutzEdricRead() throws Exception {
		final String TEST_NAME = "test120AutzEdricRead";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(null);

        login(USER_EDRIC_NAME);
        
        // WHEN
        displayWhen(TEST_NAME);

        // Wrong tenant
        assertGetDeny(UserType.class, USER_LETO_ATREIDES_OID);
        assertGetDeny(UserType.class, USER_PAUL_ATREIDES_OID);
        assertGetDeny(OrgType.class, ORG_ATREIDES_OID);
        assertGetDeny(RoleType.class, ROLE_ATREIDES_ADMIN_OID);
        
        // No tenant
        assertGetDeny(OrgType.class, ORG_GUILD_OID);
        assertGetDeny(RoleType.class, ROLE_TENANT_ADMIN_OID);
        assertGetDeny(UserType.class, USER_EDRIC_OID);
        
        assertSearch(UserType.class, null, 0);
        assertSearch(RoleType.class, null, 0);
        assertSearch(OrgType.class, null, 0);
        
        // THEN
        displayThen(TEST_NAME);
        
        assertGlobalStateUntouched();
	}	
	

	
}
