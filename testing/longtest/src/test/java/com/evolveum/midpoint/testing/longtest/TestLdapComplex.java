/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.testing.longtest;


import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.opends.server.types.Entry;
import org.opends.server.util.LDIFException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Mix of various tests for issues that are difficult to replicate using dummy resources.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-longtest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapComplex extends AbstractLongTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ldap-complex");

	public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();

    public static final File USER_TEMPLATE_FILE = new File(TEST_DIR, "user-template.xml");

    protected static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	protected static final String USER_ADMINISTRATOR_USERNAME = "administrator";

	protected static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

	protected static final File ROLE_CAPTAIN_FILE = new File(TEST_DIR, "role-captain.xml");

	protected static final File ROLE_JUDGE_FILE = new File(TEST_DIR, "role-judge.xml");
    
	protected static final File ROLE_PIRATE_FILE = new File(TEST_DIR, "role-pirate.xml");
	protected static final String ROLE_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556603";

	protected static final File ROLE_SAILOR_FILE = new File(TEST_DIR, "role-sailor.xml");

	protected static final File ROLE_SECURITY_FILE = new File(TEST_DIR, "role-security.xml");
	protected static final String ROLE_SECURITY_OID = "ab6de882-1e05-11e8-86f0-379d1205707a";
	
    protected static final File ROLES_LDIF_FILE = new File(TEST_DIR, "roles.ldif");

	protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj-complex.xml");
    protected static final String RESOURCE_OPENDJ_NAME = "Localhost OpenDJ";
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;

	private static final int NUM_LDAP_ENTRIES = 50;

	private static final String LDAP_GROUP_PIRATES_DN = "cn=Pirates,ou=groups,dc=example,dc=com";

	private static final String INTENT_SECURITY = "security";
	private static final String OBJECTCLASS_USER_SECURITY_INFORMATION = "userSecurityInformation";

	
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;

    @Autowired
    private ReconciliationTaskHandler reconciliationTaskHandler;

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

		// Roles
		repoAddObjectFromFile(ROLE_CAPTAIN_FILE, initResult);
        repoAddObjectFromFile(ROLE_JUDGE_FILE, initResult);
        repoAddObjectFromFile(ROLE_PIRATE_FILE, initResult);
        repoAddObjectFromFile(ROLE_SAILOR_FILE, initResult);
        repoAddObjectFromFile(ROLE_SECURITY_FILE, initResult);

        // templates
        repoAddObjectFromFile(USER_TEMPLATE_FILE, initResult);

		// Resources
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        openDJController.addEntriesFromLdifFile(ROLES_LDIF_FILE.getPath());

		display("initial LDAP content", openDJController.dumpEntries());
	}

	@Test
    public void test100BigImport() throws Exception {
		final String TEST_NAME = "test100BigImport";
        displayTestTitle(TEST_NAME);

        // GIVEN

        loadLdapEntries("u", NUM_LDAP_ENTRIES);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        //task.setExtensionPropertyValue(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS, 2);
        modelService.importFromResource(RESOURCE_OPENDJ_OID,
        		new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"), task, result);

        // THEN
        displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, true, 20000 + NUM_LDAP_ENTRIES*2000);

        // THEN
        displayThen(TEST_NAME);

        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        display("Users", userCount);
        assertEquals("Unexpected number of users", NUM_LDAP_ENTRIES+4, userCount);

        assertUser("u1", task, result);
	}

    private void assertUser(String name, Task task, OperationResult result) throws com.evolveum.midpoint.util.exception.ObjectNotFoundException, com.evolveum.midpoint.util.exception.SchemaException, com.evolveum.midpoint.util.exception.SecurityViolationException, com.evolveum.midpoint.util.exception.CommunicationException, com.evolveum.midpoint.util.exception.ConfigurationException, ExpressionEvaluationException {
        UserType user = findUserByUsername("u1").asObjectable();
        display("user " + name, user.asPrismObject());

        assertEquals("Wrong number of assignments", 4, user.getAssignment().size());
    }

    @Test(enabled = false)
    public void test120BigReconciliation() throws Exception {
        final String TEST_NAME = "test120BigReconciliation";
        displayTestTitle(TEST_NAME);

        // GIVEN

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        //task.setExtensionPropertyValue(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS, 2);

        ResourceType resource = modelService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result).asObjectable();
        reconciliationTaskHandler.launch(resource,
                new QName(RESOURCE_OPENDJ_NAMESPACE, "AccountObjectClass"), task, result);

        // THEN
        displayThen(TEST_NAME);
        // TODO
//        OperationResult subresult = result.getLastSubresult();
//        TestUtil.assertInProgress("reconciliation launch result", subresult);

        waitForTaskFinish(task, true, 20000 + NUM_LDAP_ENTRIES*2000);

        // THEN
        displayThen(TEST_NAME);

        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        display("Users", userCount);
        assertEquals("Unexpected number of users", NUM_LDAP_ENTRIES+4, userCount);

        assertUser("u1", task, result);
    }
    
    /**
     * MID-4483
     */
    @Test
    public void test500GuybrushAssignSecurity() throws Exception {
        final String TEST_NAME = "test500GuybrushAssignSecurity";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        addObject(USER_GUYBRUSH_FILE);

        // WHEN
        displayWhen(TEST_NAME);
        assignAccount(USER_GUYBRUSH_OID, RESOURCE_OPENDJ_OID, INTENT_SECURITY, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertLinks(userAfter, 1);

        Entry entry = assertOpenDjAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
        display("LDAP account after", entry);
        openDJController.assertHasObjectClass(entry, OBJECTCLASS_USER_SECURITY_INFORMATION);
    }
    
    /**
     * MID-4483
     */
    @Test
    public void test502RuinGuybrushAccountAndReconcile() throws Exception {
        final String TEST_NAME = "test502RuinGuybrushAccountAndReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        Entry entryOrig = openDJController.searchByUid(USER_GUYBRUSH_USERNAME);
        openDJController.modifyDelete(entryOrig.getDN().toString(), "objectClass", OBJECTCLASS_USER_SECURITY_INFORMATION);
        Entry entryBefore = openDJController.fetchEntry(entryOrig.getDN().toString());
        display("LDAP account before", entryBefore);
        openDJController.assertHasNoObjectClass(entryBefore, OBJECTCLASS_USER_SECURITY_INFORMATION);

        // WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_GUYBRUSH_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertLinks(userAfter, 1);

        Entry entry = assertOpenDjAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
        display("LDAP account after", entry);
        openDJController.assertHasObjectClass(entry, OBJECTCLASS_USER_SECURITY_INFORMATION);
    }

	private String toDn(String username) {
		return "uid="+username+","+OPENDJ_PEOPLE_SUFFIX;
	}
}
