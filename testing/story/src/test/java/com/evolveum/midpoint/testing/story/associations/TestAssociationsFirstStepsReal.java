/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.associations;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.MARK_MANAGED;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.LIFECYCLE_PROPOSED;
import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.opends.server.types.DirectoryException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectSet;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.CsvTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * This class is more directly derived from the real "associations first steps" scenario - more than
 * {@link TestAssociationsFirstSteps} and {@link TestAssociationsFirstStepsActiveDirectory}.
 *
 * Currently, it is here to diagnose the disappearing role membership (MID-10094).
 *
 * It is not included in the standard test suite - for now.
 *
 * What it does:
 *
 * . Import LDIF
 * . Import from HR -> will link LDAP accounts
 * . Import groups from LDAP (AD groups reconciliation tasks) -> roles will appear in midPoint
 * . Import associations from LDAP (AD accounts reconciliation)
 * . Migration:
 * .. cn=all-users will get MANAGED/apply/proposed mark
 * .. some simulations are run
 * .. after some time, we switch its LC to active, while running HR reconciliation at the same time
 * .. wrong behavior: role and/or association of a user will disappear
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssociationsFirstStepsReal extends AbstractStoryTest {

    public static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "associations-real");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ObjectTemplateType> TEMPLATE_PERSON = TestObject.file(
            TEST_DIR, "object-template-person.xml", "00000000-0000-0000-0000-000000000380");

    private static final TestObject<ArchetypeType> ARCHETYPE_PERSON = TestObject.file(
            TEST_DIR, "archetype-person.xml", "00000000-0000-0000-0000-000000000702");
    private static final TestObject<ArchetypeType> ARCHETYPE_AD_GROUP = TestObject.file(
            TEST_DIR, "archetype-ad-group.xml", "8053bbaa-6578-442d-a7a6-9c5b39b8a389");
    private static final TestObject<ArchetypeType> ARCHETYPE_APP_GROUP = TestObject.file(
            TEST_DIR, "archetype-app-group.xml", "c43e59cf-c2ff-4dc8-93c9-bc32c6ce308a");

    private static final CsvTestResource RESOURCE_HR = new CsvTestResource(
            TEST_DIR, "resource-hr.xml", "07b20755-5b61-4fd9-afed-a819eb2ac686", "hr.csv");

    private static final File LDIF_100_BASE_ENTRY_FILE = new File(TEST_DIR, "100-baseEntry.ldif");
    private static final File LDIF_105_LDAP_PEOPLE_FILE = new File(TEST_DIR, "105-ldap-people.ldif");
    private static final File LDIF_110_LDAP_PEOPLE_SYSTEM_FILE = new File(TEST_DIR, "110-ldap-people-system.ldif");
    private static final File LDIF_200_GROUPS_FILE = new File(TEST_DIR, "200-groups.ldif");

    private static final TestResource RESOURCE_LDAP = TestResource.file(
            TEST_DIR, "resource-ldap.xml", "052a29b1-cde5-4546-9787-916e4a846f2e");

    private static final TestTask TASK_HR_RECON = new TestTask(
            TEST_DIR, "task-hr-recon.xml", "0aebc929-52f6-4aeb-9bcc-335aa8e5951f");
    private static final TestTask TASK_HR_RECON_LOOPING = new TestTask(
            TEST_DIR, "task-hr-recon-looping.xml", "98bac8cc-29f2-4eee-96be-bf5e89770b6d");
    private static final TestTask TASK_AD_ACCOUNT_RECON = new TestTask(
            TEST_DIR, "task-ad-account-recon.xml", "d3fb9178-5c37-4b28-b733-f58c5af0a813");
    private static final TestTask TASK_AD_GROUP_RECON = new TestTask(
            TEST_DIR, "task-ad-group-recon.xml", "a6368718-66eb-450d-b194-d49f22d22d0d");
    private static final TestTask TASK_APP_GROUP_RECON = new TestTask(
            TEST_DIR, "task-app-group-recon.xml", "02ad695f-500b-4a6b-b400-f33754fababf");

    private static final String ALL_USERS_DN = "cn=all-users,ou=groups,dc=example,dc=com";
    private static final String ALL_USERS_DN_TEMPLATE = "cn=all-users-%d,ou=groups,dc=example,dc=com";
    private static final String ALL_USERS_ROLE_NAME = "ad:all-users";
    private static final String ALL_USERS_ROLE_NAME_TEMPLATE = "ad:all-users-%d";
    private static final String GREEN_USER_NAME = "ggreen";

    @Override
    protected boolean isAvoidLoggingChange() {
        return false; // to avoid all the detailed TRACE logging
    }

    @Override
    protected boolean requiresNativeRepository() {
        return true;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                CommonInitialObjects.ARCHETYPE_RECONCILIATION_TASK,
                CommonInitialObjects.ARCHETYPE_APPLICATION_ROLE);

        initTestObjects(initTask, initResult,
                TEMPLATE_PERSON, ARCHETYPE_PERSON, ARCHETYPE_AD_GROUP, ARCHETYPE_APP_GROUP,
                TASK_HR_RECON, TASK_HR_RECON_LOOPING, TASK_AD_ACCOUNT_RECON, TASK_AD_GROUP_RECON, TASK_APP_GROUP_RECON);

        RESOURCE_HR.initAndTest(this, initTask, initResult);

        openDJController.startCleanServer();
        openDJController.addEntriesFromLdifFile(LDIF_100_BASE_ENTRY_FILE);
        openDJController.addEntriesFromLdifFile(LDIF_105_LDAP_PEOPLE_FILE);
        openDJController.addEntriesFromLdifFile(LDIF_110_LDAP_PEOPLE_SYSTEM_FILE);
        openDJController.addEntriesFromLdifFile(LDIF_200_GROUPS_FILE);

        RESOURCE_LDAP.initAndTest(this, initTask, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test100ReconcilingWithHr() throws Exception {
        var result = getTestOperationResult();

        TASK_HR_RECON.rerun(result);

        var users = repositoryService.searchObjects(UserType.class, null, null, result);
        displayValue("Users", users.size());

        assertUserAfterByUsername(GREEN_USER_NAME)
                .withObjectResolver(createSimpleModelObjectResolver())
                .links()
                .by().resourceOid(RESOURCE_HR.oid).find()
                .resolveTarget()
                .display()
                .end()
                .end()
                .by().resourceOid(RESOURCE_LDAP.oid).find()
                .resolveTarget()
                .display()
                .end()
                .end();
    }

    @Test
    public void test110ReconcilingWithAd() throws Exception {
        OperationResult result = getTestOperationResult();

        TASK_AD_GROUP_RECON.rerun(result);
        TASK_APP_GROUP_RECON.rerun(result);
        TASK_AD_ACCOUNT_RECON.rerun(result);

        assertRoleAfterByName(ALL_USERS_ROLE_NAME);
    }

    @Test(enabled = false)
    public void test120MarkAllUsersManagedForManyGroups() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        int max = 39;

        TASK_HR_RECON_LOOPING.resume(result);

        MiscUtil.sleepCatchingInterruptedException(1000); // for the task to start running

        for (int i = 100; i <= 100 + max; i++) {
            var roleName = ALL_USERS_ROLE_NAME_TEMPLATE.formatted(i);
            var groupDn = ALL_USERS_DN_TEMPLATE.formatted(i);
            runTestForRole(roleName, groupDn, task, result);
        }

        TASK_HR_RECON_LOOPING.assertAfter()
                .display();

        TASK_HR_RECON_LOOPING.suspend();
    }

    /** MID-10094 */
    @Test
    public void test120MarkAllUsersManaged() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TASK_HR_RECON_LOOPING.resume(result);

        runTestForRole(ALL_USERS_ROLE_NAME, ALL_USERS_DN, task, result);

        TASK_HR_RECON_LOOPING.assertAfter()
                .display();

        TASK_HR_RECON_LOOPING.suspend();
    }

    private void runTestForRole(String roleName, String groupDn, Task task, OperationResult result)
            throws CommonException, IOException {

        var asserter = assertRoleAfterByName(roleName);

        var roleOid = asserter.getOid();
        var groupShadowOid = asserter.singleLink().getOid();

        markShadow(groupShadowOid, PolicyStatementTypeType.APPLY, MARK_MANAGED.oid, LIFECYCLE_PROPOSED, task, result);
        var statementId = getPolicyStatementId(groupShadowOid, result);

        reconcileAccountsRequest()
                .withResourceOid(RESOURCE_LDAP.oid)
                .withTypeIdentification(ACCOUNT_DEFAULT)
                .withProcessingAllAccounts()
                .simulatedDevelopment()
                .execute(result);

        int roleMembersBefore = getRoleMembers(roleOid).size();
        displayValue("Role members before (" + roleName + ")", roleMembersBefore);

        Collection<String> groupMembers = getGroupMembers(groupDn);
        displayValue("members", groupMembers);
        int groupMembersBefore = groupMembers.size();
        displayValue("Group members before (" + groupDn + ")", groupMembersBefore);

        executeChanges(
                deltaFor(ShadowType.class)
                        .item(ShadowType.F_POLICY_STATEMENT, statementId, PolicyStatementType.F_LIFECYCLE_STATE)
                        .replace(SchemaConstants.LIFECYCLE_ACTIVE)
                        .asObjectDelta(groupShadowOid),
                null, task, result);

        int membersAfter = getRoleMembers(roleOid).size();
        displayValue("Members after (" + roleName + ")", membersAfter);

        int groupMembersAfter = getGroupMembers(groupDn).size();
        displayValue("Group members after (" + groupDn + ")", groupMembersAfter);

        assertThat(membersAfter).as("members after").isEqualTo(roleMembersBefore);
        assertThat(groupMembersAfter).as("group members after").isEqualTo(groupMembersBefore);
    }

    private long getPolicyStatementId(String groupShadowOid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        var s = repositoryService.getObject(ShadowType.class, groupShadowOid, null, result);
        return MiscUtil.extractSingletonRequired(s.asObjectable().getPolicyStatement())
                .getId();
    }

    private ObjectSet<UserType> getRoleMembers(String roleOid) throws SchemaException {
        return ObjectSet.ofPrismObjects(
                repositoryService.searchObjects(
                        UserType.class,
                        queryFor(UserType.class)
                                .item(UserType.F_ROLE_MEMBERSHIP_REF).ref(roleOid)
                                .build(),
                        null, getTestOperationResult()));
    }

    private Collection<String> getGroupMembers(String groupDn) {
        try {
            return openDJController.getGroupMembers(groupDn);
        } catch (DirectoryException e) {
            throw new AssertionError(e);
        }
    }

    @AfterClass
    public static void stopResources() {
        if (openDJController.isRunning()) {
            openDJController.stop();
        }
    }
}
