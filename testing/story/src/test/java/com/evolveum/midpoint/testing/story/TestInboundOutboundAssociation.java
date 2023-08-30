/*
 * Copyright (C) 2018-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import java.io.File;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.asserter.RoleAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests for bi-directional entitlement association synchronization.
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestInboundOutboundAssociation extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "inbound-outbound-association");

    protected static final File USER_MANCOMB_FILE = new File(TEST_DIR, "user-mancomb.xml");
    protected static final String USER_MANCOMB_OID = "8e3a3770-cc60-11e8-8354-a7bb150473c1";
    protected static final String USER_MANCOMB_USERNAME = "mancomb";

    protected static final File ROLE_META_GROUP_FILE = new File(TEST_DIR, "role-meta-group.xml");
    protected static final String ROLE_META_GROUP_OID = "471a49a2-d8fe-11e8-9b6b-730d02c33833";

    protected static final File RESOURCE_DUMMY_DIR_FILE = new File(TEST_DIR, "resource-dummy-dir.xml");
    protected static final String RESOURCE_DUMMY_DIR_OID = "82230126-d85c-11e8-bc12-537988b7843a";
    protected static final String RESOURCE_DUMMY_DIR_NAME = "dir";

    protected static final File TASK_DUMMY_DIR_LIVESYNC_FILE = new File(TEST_DIR, "task-dumy-dir-livesync.xml");
    protected static final String TASK_DUMMY_DIR_LIVESYNC_OID = "7d79f012-d861-11e8-b788-07bda6c5bb24";

    public static final File OBJECT_TEMPLATE_ROLE_GROUP_FILE = new File(TEST_DIR, "object-template-role-group.xml");
    public static final String OBJECT_TEMPLATE_ROLE_GROUP_OID = "ef638872-cc69-11e8-8ee2-333f3bf7747f";

    public static final String SUBTYPE_GROUP = "group";

    private static final String GROUP_PIRATES_NAME = "pirates";

    private static final QName ASSOCIATION_GROUP_QNAME = new QName(MidPointConstants.NS_RI, "group");

    private String rolePiratesOid;
    private String shadowGroupPiratesOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_DIR_NAME, RESOURCE_DUMMY_DIR_FILE, RESOURCE_DUMMY_DIR_OID, initTask, initResult);
        getDummyResourceDir().setSyncStyle(DummySyncStyle.SMART);

        importObjectFromFile(ROLE_META_GROUP_FILE, initResult);

        // Object Templates
        importObjectFromFile(OBJECT_TEMPLATE_ROLE_GROUP_FILE, initResult);
        // subtype==employee: Make sure that this is not applied to administrator or other non-person accounts.
        setDefaultObjectTemplate(RoleType.COMPLEX_TYPE, SUBTYPE_GROUP, OBJECT_TEMPLATE_ROLE_GROUP_OID, initResult);

        addObject(TASK_DUMMY_DIR_LIVESYNC_FILE);
        // This is to make sure the 1st run (initializing the token) is executed before Pirates group is created.
        waitForTaskFinish(TASK_DUMMY_DIR_LIVESYNC_OID);

        importObjectFromFile(USER_MANCOMB_FILE, initResult);
    }

    @Test
    public void test100ImportGroupPirates() throws Exception {

        DummyGroup group = new DummyGroup(GROUP_PIRATES_NAME);
        getDummyResourceDir().addGroup(group);

        when();
        // TODO fails on JDK 8 long tests in funny way, every second time:
        //  java.lang.AssertionError: Timeout (25000) while waiting for Task(id:7d79f012-d861-11e8-b788-07bda6c5bb24,
        //  name:Live Sync: Dummy Dir Resource, oid:7d79f012-d861-11e8-b788-07bda6c5bb24) to finish. Last result R(run IN_PROGRESS null)
        //   at com.evolveum.midpoint.testing.story.TestInboundOutboundAssociation.liveSyncDir(TestInboundOutboundAssociation.java:514)
        //  This then takes with it tests 130, 140, 149, 150, 152, 153, 154 and 159.
        liveSyncDir();

        then();
        displayDumpable("dir after", getDummyResourceDir());

        RoleAsserter<Void> rolePiratesAsserter = assertRoleAfterByName(groupRoleName(GROUP_PIRATES_NAME));
        rolePiratesAsserter
                .assertSubtype(SUBTYPE_GROUP)
                .assertIdentifier(GROUP_PIRATES_NAME)
                .assignments()
                .assertAssignments(1)
                .assertRole(ROLE_META_GROUP_OID)
                .end();

        rolePiratesOid = rolePiratesAsserter.getOid();

        shadowGroupPiratesOid = rolePiratesAsserter
                .links()
                .singleAny()
                .getOid();
    }

    @Test
    public void test110AssignJackDirAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assignAccount(UserType.class, USER_JACK_OID, RESOURCE_DUMMY_DIR_OID, null, task, result);

        then();
        assertSuccess(result);

        displayDumpable("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
                .assignments()
                .assertAssignments(1);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
                .assertNoMembers();
    }

    /**
     * Make sure situation is stable.
     */
    @Test
    public void test115Stability() throws Exception {
        when();
        liveSyncDir();

        then();
        displayDumpable("dir after", getDummyResourceDir());

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
                .assertNoMembers();

        assertUserAfter(USER_JACK_OID)
                .assignments()
                .assertAssignments(1);
    }

    @Test
    public void test120AddJackToGroupPirates() throws Exception {
        getDummyResourceDir().getGroupByName(GROUP_PIRATES_NAME)
                .addMember(USER_JACK_USERNAME);

        // "fake" modification of jack's account. Just to "motivate" it to be synchronized
        getDummyResourceDir().getAccountByUsername(USER_JACK_USERNAME)
                .replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");

        when();
        liveSyncDir();

        then();
        displayDumpable("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
                .assignments()
                .assertAssignments(2)
                .assertRole(rolePiratesOid);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
                .assertMembers(USER_JACK_USERNAME);
    }

    /**
     * MID-4948
     */
    @Test
    public void test130JackUnassignRolePirates() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignRole(USER_JACK_OID, rolePiratesOid, task, result);

        then();
        assertSuccess(result);

        displayDumpable("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
                .assignments()
                .assertAssignments(1)
                .assertNoRole(rolePiratesOid);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
                .assertNoMembers();
    }

    @Test
    public void test140JackAssignRolePirates() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assignRole(USER_JACK_OID, rolePiratesOid, task, result);

        then();
        assertSuccess(result);

        displayDumpable("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
                .assignments()
                .assertAssignments(2)
                .assertRole(rolePiratesOid);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
                .assertMembers(USER_JACK_USERNAME);
    }

    /**
     * Unassign dir account. But there is still pirates group assignment,
     * therefore the account should be kept.
     */
    @Test
    public void test142JackUnAssignDirAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignAccount(UserType.class, USER_JACK_OID, RESOURCE_DUMMY_DIR_OID, null, task, result);

        then();
        assertSuccess(result);

        displayDumpable("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
                .assignments()
                .assertAssignments(1)
                .assertRole(rolePiratesOid);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
                .assertMembers(USER_JACK_USERNAME);
    }

    /**
     * MID-4948
     */
    @Test
    public void test149JackUnassignRolePirates() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignRole(USER_JACK_OID, rolePiratesOid, task, result);

        then();
        assertSuccess(result);

        displayDumpable("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
                .assignments()
                .assertAssignments(0);

        assertNoDummyAccount(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
                .assertNoMembers();
    }

    @Test
    public void test150AssignJackDirAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assignAccount(UserType.class, USER_JACK_OID, RESOURCE_DUMMY_DIR_OID, null, task, result);

        then();
        assertSuccess(result);

        displayDumpable("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
                .assignments()
                .assertAssignments(1);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
                .assertNoMembers();
    }

    @Test
    public void test152JackAssignRolePirates() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assignRole(USER_JACK_OID, rolePiratesOid, task, result);

        then();
        assertSuccess(result);

        displayDumpable("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
                .assignments()
                .assertAssignments(2)
                .assertRole(rolePiratesOid);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
                .assertMembers(USER_JACK_USERNAME);
    }

    /**
     * MID-4948
     */
    @Test
    public void test153JackUnassignRolePiratesPreview() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> focusDelta = createAssignmentFocusDelta(
                UserType.class, USER_JACK_OID,
                FocusType.F_ASSIGNMENT,
                rolePiratesOid, RoleType.COMPLEX_TYPE,
                null, null, false);

        when();
        ModelContext<UserType> previewContext = previewChanges(focusDelta, null, task, result);

        then();
        assertSuccess(result);

        assertPreviewContext(previewContext)
                .projectionContexts()
                .single()
                .assertNoPrimaryDelta()
                .secondaryDelta()
                .display()
                .assertModify()
                .container(ShadowType.F_ASSOCIATION)
                .assertNoValuesToAdd()
                .assertNoValuesToReplace()
                .valuesToDelete()
                .single()
                .assertPropertyEquals(ShadowAssociationType.F_NAME, ASSOCIATION_GROUP_QNAME)
                .assertRefEquals(ShadowAssociationType.F_SHADOW_REF, shadowGroupPiratesOid)
                .end()
                .end()
                .end()
                .end()
                .objectNew()
                .display()
                .assertNoItem(ShadowType.F_ASSOCIATION);

    }

    /**
     * MID-4948
     */
    @Test
    public void test154JackUnassignRolePirates() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignRole(USER_JACK_OID, rolePiratesOid, task, result);

        then();
        assertSuccess(result);

        displayDumpable("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
                .assignments()
                .assertAssignments(1)
                .assertNoRole(rolePiratesOid);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);

        assertDummyAccountByUsername(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
                .assertNoMembers();
    }

    @Test
    public void test159JackUnassignDirAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignAccount(UserType.class, USER_JACK_OID, RESOURCE_DUMMY_DIR_OID, null, task, result);

        then();
        assertSuccess(result);

        displayDumpable("dir after", getDummyResourceDir());

        assertUserAfter(USER_JACK_OID)
                .assignments()
                .assertAssignments(0);

        assertNoDummyAccount(RESOURCE_DUMMY_DIR_NAME, USER_JACK_USERNAME);

        assertDummyGroupByName(RESOURCE_DUMMY_DIR_NAME, GROUP_PIRATES_NAME)
                .assertNoMembers();
    }

    /**
     * MID-5635
     */
    @Test
    public void test200MancombAssignAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assignAccountToUser(USER_MANCOMB_OID, RESOURCE_DUMMY_DIR_OID, "default", task, result);

        then();
        assertSuccess(result);

        displayDumpable("dir after", getDummyResourceDir());
        assertUserAfter(USER_MANCOMB_OID)
                .assignments()
                .assertAssignments(1);

        assertDummyAccount(RESOURCE_DUMMY_DIR_NAME, USER_MANCOMB_USERNAME);

    }

    private String groupRoleName(String groupName) {
        return "group:" + groupName;
    }

    private void liveSyncDir() throws CommonException {
        rerunTask(TASK_DUMMY_DIR_LIVESYNC_OID);
    }

    private DummyResource getDummyResourceDir() {
        return getDummyResource(RESOURCE_DUMMY_DIR_NAME);
    }
}
