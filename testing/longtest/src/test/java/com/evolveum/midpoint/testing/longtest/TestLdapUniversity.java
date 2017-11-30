/*
 * Copyright (c) 2010-2017 Evolveum
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


import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.common.ProfilingConfigurationManager;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.io.IOUtils;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.util.LDIFException;
import org.opends.server.util.LDIFReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

/**
 *
 * @author Pavol Mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-longtest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapUniversity extends AbstractModelIntegrationTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ldap-university");

	public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();

    protected static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	protected static final String USER_ADMINISTRATOR_USERNAME = "administrator";

	protected static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

	protected static final File RESOURCE_OPENDJ_FILE = new File(COMMON_DIR, "resource-opendj-university.xml");
    protected static final String RESOURCE_OPENDJ_NAME = "Localhost OpenDJ";
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;

	// Make it at least 1501 so it will go over the 3000 entries size limit
	private static final int NUM_LDAP_ENTRIES = 20000;

	private static final String LDAP_GROUP_PIRATES_DN = "cn=Pirates,ou=groups,dc=example,dc=com";

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
		modelService.postInit(initResult);

		// System Configuration
        PrismObject<SystemConfigurationType> config;
		try {
			config = repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}

        LoggingConfigurationManager.configure(
                ProfilingConfigurationManager.checkSystemProfilingConfiguration(config),
                config.asObjectable().getVersion(), initResult);

		// administrator
		PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
		repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
		login(userAdministrator);

		// Resources
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

		display("initial LDAP content", openDJController.dumpEntries());
	}

	@Test
    public void test100BigImportWithLinking() throws Exception {
		final String TEST_NAME = "test100BigImportWithLinking";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN

        InternalsConfig.turnOffAllChecks();

        Task task = taskManager.createTaskInstance(TestLdapUniversity.class.getName() + "." + TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        loadEntries("u");
        createUsers("u", new OperationResult("createUsers"));       // we do not want to have all this in the task's result

        display("e0", findUserByUsername("e0"));

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        //task.setExtensionPropertyValue(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS, 5);
        modelService.importFromResource(RESOURCE_OPENDJ_OID,
        		new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, true, 20000 + NUM_LDAP_ENTRIES*2000, 10000L);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        display("Users", userCount);
        assertEquals("Unexpected number of users", NUM_LDAP_ENTRIES+1, userCount);

        display("e0(u0)", findUserByUsername("e0(u0)"));
        display("e1(u1)", findUserByUsername("e1(u1)"));

        assertUser("e0(u0)", task, result);
        assertUser("e1(u1)", task, result);
	}

    private void createUsers(String prefix, OperationResult result) throws ObjectAlreadyExistsException, SchemaException {
        final int TICK=100;
        long start = System.currentTimeMillis();
        for(int i=0; i < NUM_LDAP_ENTRIES; i++) {
            UserType userType = (UserType) prismContext.getSchemaRegistry().findObjectDefinitionByType(UserType.COMPLEX_TYPE).instantiate().asObjectable();
            if (i%2 == 0) {
                userType.setName(createPolyStringType("e" + i));
            } else {
                userType.setName(createPolyStringType("e" + i + "(u" + i + ")"));
            }
            userType.setEmployeeNumber("e"+i);
            repositoryService.addObject(userType.asPrismObject(), null, result);

            if ((i+1)%TICK == 0 && (i+1)<NUM_LDAP_ENTRIES) {
                display("Created "+(i+1)+" users in "+((System.currentTimeMillis()-start))+" milliseconds, continuing...");
            }
        }
        display("Created "+NUM_LDAP_ENTRIES+" users in "+((System.currentTimeMillis()-start))+" milliseconds.");

    }

    private void assertUser(String name, Task task, OperationResult result) throws com.evolveum.midpoint.util.exception.ObjectNotFoundException, com.evolveum.midpoint.util.exception.SchemaException, com.evolveum.midpoint.util.exception.SecurityViolationException, com.evolveum.midpoint.util.exception.CommunicationException, com.evolveum.midpoint.util.exception.ConfigurationException, ExpressionEvaluationException {
        UserType user = findUserByUsername(name).asObjectable();
        display("user " + name, user.asPrismObject());

        //assertEquals("Wrong number of assignments", 4, user.getAssignmentNew().size());
    }

    @Test
    public void test120BigReconciliation() throws Exception {
        final String TEST_NAME = "test120BigReconciliation";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN

        Task task = taskManager.createTaskInstance(TestLdapUniversity.class.getName() + "." + TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        //task.setExtensionPropertyValue(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS, 2);

        ResourceType resource = modelService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result).asObjectable();
        reconciliationTaskHandler.launch(resource,
                new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        // TODO
//        OperationResult subresult = result.getLastSubresult();
//        TestUtil.assertInProgress("reconciliation launch result", subresult);

        waitForTaskFinish(task, true, 20000 + NUM_LDAP_ENTRIES*2000, 10000L);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        display("Users", userCount);
        assertEquals("Unexpected number of users", NUM_LDAP_ENTRIES+1, userCount);

        display("e0(u0)", findUserByUsername("e0(u0)"));
        display("e1(u1)", findUserByUsername("e1(u1)"));

        assertUser("e0(u0)", task, result);
        assertUser("e1(u1)", task, result);
    }

    private void loadEntries(String prefix) throws LDIFException, IOException {
        long ldapPopStart = System.currentTimeMillis();

        final int BATCH = 200;
        final int TICK = 1000;
//        List<String> namesToAdd = new ArrayList<>(BATCH);
        for(int i=0; i < NUM_LDAP_ENTRIES; i++) {
        	String name = "user"+i;
        	Entry entry = createEntry(prefix+i, "e"+i, name);
        	openDJController.addEntry(entry);
//            namesToAdd.add(entry.getDN().toNormalizedString());
//            if (namesToAdd.size() == BATCH) {
//                addToGroups(namesToAdd);
//                namesToAdd.clear();
//            }
            if ((i+1)%TICK == 0 && (i+1)<NUM_LDAP_ENTRIES) {
                display("Loaded "+(i+1)+" LDAP entries in "+((System.currentTimeMillis()-ldapPopStart)/1000)+" seconds, continuing...");
            }
        }
//        if (!namesToAdd.isEmpty()) {
//            addToGroups(namesToAdd);
//        }

        long ldapPopEnd = System.currentTimeMillis();

        display("Loaded "+NUM_LDAP_ENTRIES+" LDAP entries in "+((ldapPopEnd-ldapPopStart)/1000)+" seconds");
	}

    private void addToGroups(List<String> namesToAdd) throws IOException, LDIFException {
        for (int groupIndex = 1; groupIndex <= 10; groupIndex++) {
            openDJController.addGroupUniqueMembers(groupDn(groupIndex), namesToAdd);
        }
    }

    private Entry createEntry(String uid, String empno, String name) throws IOException, LDIFException {
		StringBuilder sb = new StringBuilder();
		String dn = "uid="+uid+","+openDJController.getSuffixPeople();
		sb.append("dn: ").append(dn).append("\n");
		sb.append("objectClass: inetOrgPerson\n");
		sb.append("uid: ").append(uid).append("\n");
        sb.append("employeenumber: ").append(empno).append("\n");
		sb.append("cn: ").append(name).append("\n");
		sb.append("sn: ").append(name).append("\n");
		LDIFImportConfig importConfig = new LDIFImportConfig(IOUtils.toInputStream(sb.toString(), "utf-8"));
        LDIFReader ldifReader = new LDIFReader(importConfig);
        Entry ldifEntry = ldifReader.readEntry();
		return ldifEntry;
	}

	private String toDn(String username) {
		return "uid="+username+","+OPENDJ_PEOPLE_SUFFIX;
	}

    private String groupDn(int groupIndex) {
        return "cn="+groupCn(groupIndex)+","+OPENDJ_GROUPS_SUFFIX;
    }

    private String groupCn(int groupIndex) {
        return String.format("g%02d", groupIndex);
    }
}
