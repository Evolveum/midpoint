/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.polystring.PolyString;

import com.evolveum.midpoint.test.TestTask;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests for MID-2436 (volatile attributes).
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestVolatility extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/volatility");

    private static final File RESOURCE_DUMMY_HR_FILE = new File(TEST_DIR, "resource-dummy-hr.xml");
    private static final String RESOURCE_DUMMY_HR_OID = "10000000-0000-0000-0000-00000000f004";
    private static final String RESOURCE_DUMMY_HR_NAME = "hr";

    private static final File RESOURCE_DUMMY_VOLATILE_FILE = new File(TEST_DIR, "resource-dummy-volatile.xml");
    private static final String RESOURCE_DUMMY_VOLATILE_OID = "10000000-0000-0000-0000-00000000f104";
    private static final String RESOURCE_DUMMY_VOLATILE_NAME = "volatile";

    private static final File RESOURCE_DUMMY_MONSTERIZED_FILE = new File(TEST_DIR, "resource-dummy-monsterized.xml");
    private static final String RESOURCE_DUMMY_MONSTERIZED_OID = "67a954d2-f391-11e6-a1d7-078381fe0e6f";
    private static final String RESOURCE_DUMMY_MONSTERIZED_NAME = "monsterized";

    private static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";
    protected static final String ACCOUNT_GUYBRUSH_DUMMY_USERNAME = "guybrush"; //Guybrush Threepwood
    private static final String ACCOUNT_LARGO_DUMMY_USERNAME = "largo";

    private static final TestTask TASK_LIVE_SYNC_DUMMY_HR = new TestTask(
            TEST_DIR, "task-dummy-hr-livesync.xml", "10000000-0000-0000-5555-55550000f004");

    private static final File USER_TEMPLATE_FILE = new File(TEST_DIR, "user-template-import-hr.xml");
    private static final File USER_LARGO_WITH_ASSIGNMENT_FILE = new File(TEST_DIR, "user-largo-with-assignment.xml");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        repoAddObjectFromFile(USER_TEMPLATE_FILE, initResult);

        initDummyResource(RESOURCE_DUMMY_HR_NAME, RESOURCE_DUMMY_HR_FILE, RESOURCE_DUMMY_HR_OID, ctl -> {
            ctl.getDummyResource().setSyncStyle(DummySyncStyle.SMART);
            ctl.getDummyResource().populateWithDefaultSchema();
        }, initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_VOLATILE_NAME, RESOURCE_DUMMY_VOLATILE_FILE, RESOURCE_DUMMY_VOLATILE_OID, initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_MONSTERIZED_NAME, RESOURCE_DUMMY_MONSTERIZED_FILE, RESOURCE_DUMMY_MONSTERIZED_OID, initTask, initResult);

        initTestObjects(initTask, initResult, TASK_LIVE_SYNC_DUMMY_HR);
        TASK_LIVE_SYNC_DUMMY_HR.rerun(initResult);
    }

    @Test
    public void test110AddDummyHrAccountMancomb() throws Exception {
        var result = getTestOperationResult();

        given();

        DummyAccount account = new DummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");

        when();

        getDummyResource(RESOURCE_DUMMY_HR_NAME).addAccount(account);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_HR_OID);

        TASK_LIVE_SYNC_DUMMY_HR.rerun(result);

        then();

        PrismObject<ShadowType> accountMancombHr = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME,
                getDummyResourceObject(RESOURCE_DUMMY_HR_NAME));
        display("Account mancomb on HR", accountMancombHr);
        assertNotNull("No mancomb HR account shadow", accountMancombHr);
        assertEquals("Wrong resourceRef in mancomb HR account", RESOURCE_DUMMY_HR_OID,
                accountMancombHr.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancombHr, SynchronizationSituationType.LINKED, null);

        PrismObject<ShadowType> accountMancombVolatileTarget = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME,
                getDummyResourceObject(RESOURCE_DUMMY_VOLATILE_NAME));
        display("Account mancomb on target", accountMancombVolatileTarget);
        assertNotNull("No mancomb target account shadow", accountMancombVolatileTarget);
        assertEquals("Wrong resourceRef in mancomb target account", RESOURCE_DUMMY_VOLATILE_OID,
                accountMancombVolatileTarget.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancombVolatileTarget, SynchronizationSituationType.LINKED, null);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb was not created", userMancomb);
        assertLiveLinks(userMancomb, 2);

        assertLinked(userMancomb, accountMancombHr);
        assertLinked(userMancomb, accountMancombVolatileTarget);

        String descriptionOnResource = getAttributeValue(accountMancombVolatileTarget.asObjectable(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DESCRIPTION_QNAME);
        String descriptionOfUser = userMancomb.asObjectable().getDescription();
        String expectedDescription = "Description of " + ACCOUNT_MANCOMB_DUMMY_USERNAME;

        assertEquals("Wrong description on resource account", expectedDescription, descriptionOnResource);
        assertEquals("Wrong description in user record", expectedDescription, descriptionOfUser);

        notificationManager.setDisabled(true);
    }

    @Test
    public void test120UpdateDummyHrAccountMancomb() throws Exception {
        var result = getTestOperationResult();

        when();
        DummyAccount account = getDummyResource(RESOURCE_DUMMY_HR_NAME).getAccountByName(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        account.replaceAttributeValue(DummyAccount.ATTR_FULLNAME_NAME, "Sir Mancomb Seepgood");

        displayValue("Dummy HR resource", getDummyResource(RESOURCE_DUMMY_HR_NAME).debugDump());

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_HR_OID);

        // Make sure we have steady state
        TASK_LIVE_SYNC_DUMMY_HR.rerun(result);

        then();
        PrismObject<ShadowType> accountMancombHr = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME,
                getDummyResourceObject(RESOURCE_DUMMY_HR_NAME));
        display("Account mancomb on HR", accountMancombHr);
        assertNotNull("No mancomb HR account shadow", accountMancombHr);
        assertEquals("Wrong resourceRef in mancomb HR account", RESOURCE_DUMMY_HR_OID,
                accountMancombHr.asObjectable().getResourceRef().getOid());
        assertEquals("Wrong name in mancomb HR account", "Sir Mancomb Seepgood",
                getAttributeValue(accountMancombHr.asObjectable(),
                        DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME));
        assertShadowOperationalData(accountMancombHr, SynchronizationSituationType.LINKED, null);

        PrismObject<ShadowType> accountMancombVolatileTarget = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME,
                getDummyResourceObject(RESOURCE_DUMMY_VOLATILE_NAME));
        display("Account mancomb on target", accountMancombVolatileTarget);
        assertNotNull("No mancomb target account shadow", accountMancombVolatileTarget);
        assertEquals("Wrong resourceRef in mancomb target account", RESOURCE_DUMMY_VOLATILE_OID,
                accountMancombVolatileTarget.asObjectable().getResourceRef().getOid());
        assertEquals("Wrong name in mancomb target account", "Sir Mancomb Seepgood",
                getAttributeValue(accountMancombHr.asObjectable(),
                        DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME));
        assertShadowOperationalData(accountMancombVolatileTarget, SynchronizationSituationType.LINKED, null);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb is not there", userMancomb);
        assertLiveLinks(userMancomb, 2);
        assertEquals("Wrong name in mancomb user", "Sir Mancomb Seepgood",
                userMancomb.asObjectable().getFullName().getOrig());

        assertLinked(userMancomb, accountMancombHr);
        assertLinked(userMancomb, accountMancombVolatileTarget);

        String descriptionOnResource = getAttributeValue(accountMancombVolatileTarget.asObjectable(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DESCRIPTION_QNAME);
        String descriptionOfUser = userMancomb.asObjectable().getDescription();
        String expectedDescription = "Updated description of " + ACCOUNT_MANCOMB_DUMMY_USERNAME;

        assertEquals("Wrong description on resource account", expectedDescription, descriptionOnResource);
        assertEquals("Wrong description in user record", expectedDescription, descriptionOfUser);

        // notifications
        notificationManager.setDisabled(true);
    }

    @Test
    public void test200ModifyGuybrushAssignAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_GUYBRUSH_OID, RESOURCE_DUMMY_VOLATILE_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        modelService.executeChanges(deltas, null, task, result);

        then();

        PrismObject<UserType> userGuybrush = findUserByUsername(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("User guybrush", userGuybrush);
        assertNotNull("User guybrush is not there", userGuybrush);
        assertLiveLinks(userGuybrush, 1);

        PrismObject<ShadowType> accountGuybrushVolatileTarget = findAccountByUsername(ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                getDummyResourceObject(RESOURCE_DUMMY_VOLATILE_NAME));
        display("Account guybrush on target", accountGuybrushVolatileTarget);
        assertNotNull("No guybrush target account shadow", accountGuybrushVolatileTarget);
        assertEquals("Wrong resourceRef in guybrush target account", RESOURCE_DUMMY_VOLATILE_OID,
                accountGuybrushVolatileTarget.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountGuybrushVolatileTarget, SynchronizationSituationType.LINKED, null);

        assertLinked(userGuybrush, accountGuybrushVolatileTarget);

        String descriptionOnResource = getAttributeValue(accountGuybrushVolatileTarget.asObjectable(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DESCRIPTION_QNAME);
        String descriptionOfUser = userGuybrush.asObjectable().getDescription();
        String expectedDescription = "Description of " + ACCOUNT_GUYBRUSH_DUMMY_USERNAME;

        assertEquals("Wrong description on resource account", expectedDescription, descriptionOnResource);
        assertEquals("Wrong description in user record", expectedDescription, descriptionOfUser);

        notificationManager.setDisabled(true);
    }

    @Test
    public void test300AddLargo() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_LARGO_WITH_ASSIGNMENT_FILE);
        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        modelService.executeChanges(deltas, null, task, result);

        then();

        PrismObject<UserType> userLargo = findUserByUsername(ACCOUNT_LARGO_DUMMY_USERNAME);
        display("User largo", userLargo);
        assertNotNull("User largo is not there", userLargo);
        assertLiveLinks(userLargo, 1);

        PrismObject<ShadowType> accountLargoVolatileTarget = findAccountByUsername(ACCOUNT_LARGO_DUMMY_USERNAME,
                getDummyResourceObject(RESOURCE_DUMMY_VOLATILE_NAME));
        display("Account largo on target", accountLargoVolatileTarget);
        assertNotNull("No largo target account shadow", accountLargoVolatileTarget);
        assertEquals("Wrong resourceRef in largo target account", RESOURCE_DUMMY_VOLATILE_OID,
                accountLargoVolatileTarget.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountLargoVolatileTarget, SynchronizationSituationType.LINKED, null);

        assertLinked(userLargo, accountLargoVolatileTarget);

        String descriptionOnResource = getAttributeValue(accountLargoVolatileTarget.asObjectable(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DESCRIPTION_QNAME);
        String descriptionOfUser = userLargo.asObjectable().getDescription();
        String expectedDescription = "Description of " + ACCOUNT_LARGO_DUMMY_USERNAME;

        assertEquals("Wrong description on resource account", expectedDescription, descriptionOnResource);
        assertEquals("Wrong description in user record", expectedDescription, descriptionOfUser);

        notificationManager.setDisabled(true);
    }

    /**
     * MID-3727
     */
    @Test
    public void test400AddHerman() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = PrismTestUtil.parseObject(USER_HERMAN_FILE);
        AssignmentType assignmentType = createAccountAssignment(RESOURCE_DUMMY_MONSTERIZED_OID, null);
        userBefore.asObjectable().getAssignment().add(assignmentType);
        userBefore.asObjectable().getOrganization().add(createPolyStringType("foo"));
        userBefore.asObjectable().getOrganization().add(createPolyStringType(DummyResource.VALUE_COOKIE));
        userBefore.asObjectable().getOrganization().add(createPolyStringType("bar"));
        display("User before", userBefore);

        when();
        addObject(userBefore, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_HERMAN_OID);
        display("User after", userAfter);
        assertNotNull("User not there", userAfter);
        assertLiveLinks(userAfter, 1);

        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_MONSTERIZED_NAME, USER_HERMAN_USERNAME);
        displayDumpable("Dummy account", dummyAccount);
        assertDummyAccountAttribute(RESOURCE_DUMMY_MONSTERIZED_NAME, USER_HERMAN_USERNAME,
                DummyAccount.ATTR_INTERESTS_NAME, "foo", "bar", DummyResource.VALUE_COOKIE);
    }

    /**
     * Monsterized resource is volatile: Monster has eaten the cookie. But we still
     * want the cookie to be in the values. The volatility=explosive should fix it.
     * MID-3727
     */
    @Test
    public void test402ModifyHermanMonster() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        modifyUserAdd(USER_HERMAN_OID, UserType.F_ORGANIZATION, task, result,
                PolyString.fromOrig(DummyResource.VALUE_MONSTER));

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_HERMAN_OID);
        display("User after", userAfter);
        assertNotNull("User not there", userAfter);
        assertLiveLinks(userAfter, 1);

        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_MONSTERIZED_NAME, USER_HERMAN_USERNAME);
        displayDumpable("Dummy account", dummyAccount);
        assertDummyAccountAttribute(RESOURCE_DUMMY_MONSTERIZED_NAME, USER_HERMAN_USERNAME,
                DummyAccount.ATTR_INTERESTS_NAME,
                "foo", "bar", DummyResource.VALUE_COOKIE, DummyResource.VALUE_MONSTER);
    }
}
