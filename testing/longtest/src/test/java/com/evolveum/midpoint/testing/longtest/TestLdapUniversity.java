/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.longtest;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.namespace.QName;

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

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.common.ProfilingConfigurationManager;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.impl.sync.tasks.recon.ReconciliationLauncher;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "classpath:ctx-longtest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapUniversity extends AbstractModelIntegrationTest {

    public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");

    protected static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
    protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";

    protected static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");

    protected static final File RESOURCE_OPENDJ_FILE = new File(COMMON_DIR, "resource-opendj-university.xml");
    protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
    protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;

    // Make it at least 1501 so it will go over the 3000 entries size limit
    private static final int NUM_LDAP_ENTRIES = 3100;

    protected ResourceType resourceOpenDjType;
    protected PrismObject<ResourceType> resourceOpenDj;

    @Autowired private ReconciliationLauncher reconciliationLauncher;
    @Autowired private MidpointConfiguration midpointConfiguration;

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopResources() {
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
                config.asObjectable().getVersion(), midpointConfiguration, initResult);

        // administrator
        PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
        login(userAdministrator);

        // Resources
        resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
        resourceOpenDjType = resourceOpenDj.asObjectable();
        openDJController.setResource(resourceOpenDj);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        displayValue("initial LDAP content", openDJController.dumpEntries());
    }

    @Test
    public void test100BigImportWithLinking() throws Exception {
        // GIVEN

        InternalsConfig.turnOffAllChecks();

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        loadEntries("u");
        createUsers(createOperationResult()); // we do not want to have all this in the task's result

        display("e0", findUserByUsername("e0"));

        when();
        modelService.importFromResource(RESOURCE_OPENDJ_OID,
                new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"), task, result);

        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, true, 20000 + NUM_LDAP_ENTRIES * 2000, 10000L);

        then();
        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        displayValue("Users", userCount);
        assertEquals("Unexpected number of users", NUM_LDAP_ENTRIES + 1, userCount);

        display("e0(u0)", findUserByUsername("e0(u0)"));
        display("e1(u1)", findUserByUsername("e1(u1)"));

        assertUser("e0(u0)");
        assertUser("e1(u1)");
    }

    private void createUsers(OperationResult result) throws ObjectAlreadyExistsException, SchemaException {
        final int TICK = 100;
        long start = System.currentTimeMillis();
        for (int i = 0; i < NUM_LDAP_ENTRIES; i++) {
            UserType userType = (UserType) prismContext.getSchemaRegistry().findObjectDefinitionByType(UserType.COMPLEX_TYPE).instantiate().asObjectable();
            if (i % 2 == 0) {
                userType.setName(createPolyStringType("e" + i));
            } else {
                userType.setName(createPolyStringType("e" + i + "(u" + i + ")"));
            }
            userType.setEmployeeNumber("e" + i);
            repositoryService.addObject(userType.asPrismObject(), null, result);

            if ((i + 1) % TICK == 0 && (i + 1) < NUM_LDAP_ENTRIES) {
                display("Created " + (i + 1) + " users in " + ((System.currentTimeMillis() - start)) + " milliseconds, continuing...");
            }
        }
        display("Created " + NUM_LDAP_ENTRIES + " users in " + ((System.currentTimeMillis() - start)) + " milliseconds.");

    }

    private void assertUser(String name) throws com.evolveum.midpoint.util.exception.ObjectNotFoundException, com.evolveum.midpoint.util.exception.SchemaException, com.evolveum.midpoint.util.exception.SecurityViolationException, com.evolveum.midpoint.util.exception.CommunicationException, com.evolveum.midpoint.util.exception.ConfigurationException, ExpressionEvaluationException {
        UserType user = findUserByUsername(name).asObjectable();
        display("user " + name, user.asPrismObject());
    }

    @Test
    public void test120BigReconciliation() throws Exception {
        given();
        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        when();
        ResourceType resource = modelService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result).asObjectable();
        reconciliationLauncher.launch(
                resource, new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"), task, result);

        then();
        waitForTaskFinish(task, true, 20000 + NUM_LDAP_ENTRIES * 2000, 10000L);
        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        displayValue("Users", userCount);
        assertEquals("Unexpected number of users", NUM_LDAP_ENTRIES + 1, userCount);

        display("e0(u0)", findUserByUsername("e0(u0)"));
        display("e1(u1)", findUserByUsername("e1(u1)"));

        assertUser("e0(u0)");
        assertUser("e1(u1)");
    }

    private void loadEntries(String prefix) throws LDIFException, IOException {
        long ldapPopStart = System.currentTimeMillis();

        final int TICK = 1000;
        for (int i = 0; i < NUM_LDAP_ENTRIES; i++) {
            String name = "user" + i;
            Entry entry = createEntry(prefix + i, "e" + i, name);
            openDJController.addEntry(entry);
            if ((i + 1) % TICK == 0 && (i + 1) < NUM_LDAP_ENTRIES) {
                display("Loaded " + (i + 1) + " LDAP entries in " + ((System.currentTimeMillis() - ldapPopStart) / 1000) + " seconds, continuing...");
            }
        }

        long ldapPopEnd = System.currentTimeMillis();

        display("Loaded " + NUM_LDAP_ENTRIES + " LDAP entries in " + ((ldapPopEnd - ldapPopStart) / 1000) + " seconds");
    }

    private Entry createEntry(String uid, String empno, String name) throws IOException, LDIFException {
        StringBuilder sb = new StringBuilder();
        String dn = "uid=" + uid + "," + openDJController.getSuffixPeople();
        sb.append("dn: ").append(dn).append("\n");
        sb.append("objectClass: inetOrgPerson\n");
        sb.append("uid: ").append(uid).append("\n");
        sb.append("employeenumber: ").append(empno).append("\n");
        sb.append("cn: ").append(name).append("\n");
        sb.append("sn: ").append(name).append("\n");
        LDIFImportConfig importConfig = new LDIFImportConfig(IOUtils.toInputStream(sb.toString(), StandardCharsets.UTF_8));
        LDIFReader ldifReader = new LDIFReader(importConfig);
        return ldifReader.readEntry();
    }
}
