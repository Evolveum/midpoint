/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.longtest;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import javax.xml.namespace.QName;

import org.opends.server.types.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.sync.tasks.recon.ReconciliationLauncher;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Mix of various tests for issues that are difficult to replicate using dummy resources.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-longtest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapComplex extends AbstractLongTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ldap-complex");

    public static final File USER_TEMPLATE_FILE = new File(TEST_DIR, "user-template.xml");

    protected static final File ROLE_CAPTAIN_FILE = new File(TEST_DIR, "role-captain.xml");

    protected static final File ROLE_JUDGE_FILE = new File(TEST_DIR, "role-judge.xml");

    protected static final File ROLE_PIRATE_FILE = new File(TEST_DIR, "role-pirate.xml");

    protected static final File ROLE_SAILOR_FILE = new File(TEST_DIR, "role-sailor.xml");

    protected static final File ROLE_SECURITY_FILE = new File(TEST_DIR, "role-security.xml");

    protected static final File ROLES_LDIF_FILE = new File(TEST_DIR, "roles.ldif");

    protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj-complex.xml");
    protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";

    private static final int NUM_LDAP_ENTRIES = 50;

    private static final String INTENT_SECURITY = "security";
    private static final String OBJECT_CLASS_USER_SECURITY_INFORMATION = "userSecurityInformation";

    protected ResourceType resourceOpenDjType;
    protected PrismObject<ResourceType> resourceOpenDj;

    @Autowired
    private ReconciliationLauncher reconciliationLauncher;

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

        displayValue("initial LDAP content", openDJController.dumpEntries());
    }

    @Test
    public void test100BigImport() throws Exception {
        given();
        loadLdapEntries("u", NUM_LDAP_ENTRIES);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        //task.setExtensionPropertyValue(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS, 2);
        modelService.importFromResource(RESOURCE_OPENDJ_OID,
                new QName(MidPointConstants.NS_RI, "inetOrgPerson"), task, result);

        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, 20000 + NUM_LDAP_ENTRIES * 2000);

        then();

        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        displayValue("Users", userCount);
        assertEquals("Unexpected number of users", NUM_LDAP_ENTRIES + 4, userCount);

        assertUser("u1");
    }

    private void assertUser(String name)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        UserType user = findUserByUsername(name).asObjectable();
        display("user " + name, user.asPrismObject());

        assertEquals("Wrong number of assignments", 4, user.getAssignment().size());
    }

    @Test(enabled = false)
    public void test120BigReconciliation() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        ResourceType resource = modelService.getObject(
                ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result).asObjectable();
        reconciliationLauncher.launch(resource, RI_ACCOUNT_OBJECT_CLASS, task, result);

        then();
        // TODO
//        OperationResult subresult = result.getLastSubresult();
//        TestUtil.assertInProgress("reconciliation launch result", subresult);

        waitForTaskFinish(task, 20000 + NUM_LDAP_ENTRIES * 2000);

        then(); // use and(description) instead

        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        displayValue("Users", userCount);
        assertEquals("Unexpected number of users", NUM_LDAP_ENTRIES + 4, userCount);

        assertUser("u1");
    }

    /**
     * MID-4483
     */
    @Test
    public void test500GuybrushAssignSecurity() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addObject(USER_GUYBRUSH_FILE);

        when();
        assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_OPENDJ_OID, INTENT_SECURITY, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 1);

        Entry entry = assertOpenDjAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
        display("LDAP account after", entry);
        openDJController.assertHasObjectClass(entry, OBJECT_CLASS_USER_SECURITY_INFORMATION);
    }

    /**
     * MID-4483
     */
    @Test
    public void test502RuinGuybrushAccountAndReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        Entry entryOrig = openDJController.searchByUid(USER_GUYBRUSH_USERNAME);
        openDJController.modifyDelete(entryOrig.getDN().toString(), "objectClass", OBJECT_CLASS_USER_SECURITY_INFORMATION);
        Entry entryBefore = openDJController.fetchEntry(entryOrig.getDN().toString());
        display("LDAP account before", entryBefore);
        openDJController.assertHasNoObjectClass(entryBefore, OBJECT_CLASS_USER_SECURITY_INFORMATION);

        when();
        reconcileUser(USER_GUYBRUSH_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 1);

        Entry entry = assertOpenDjAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
        display("LDAP account after", entry);
        openDJController.assertHasObjectClass(entry, OBJECT_CLASS_USER_SECURITY_INFORMATION);
    }
}
