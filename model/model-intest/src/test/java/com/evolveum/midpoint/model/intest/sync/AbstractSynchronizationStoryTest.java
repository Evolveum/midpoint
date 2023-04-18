/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static com.evolveum.midpoint.model.intest.sync.AbstractSynchronizationStoryTest.Color.*;
import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME;
import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelPublicConstants;

import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.TestTask;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.TaskOperationStatsUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests the synchronization among three dummy resources: default, green, and blue.
 *
 * Exists in live-sync and reconciliation versions.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractSynchronizationStoryTest extends AbstractInitializedModelIntegrationTest {

    static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "sync-story");

    private static final TestResource<ObjectTemplateType> USER_TEMPLATE_SYNC = new TestResource<>(
            TEST_DIR, "user-template-sync.xml", "10000000-0000-0000-0000-000000000333");

    private static final String ACCOUNT_WALLY_DUMMY_USERNAME = "wally";
    private static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";
    private static final Date ACCOUNT_MANCOMB_VALID_FROM_DATE = MiscUtil.asDate(2011, 2, 3, 4, 5, 6);
    private static final Date ACCOUNT_MANCOMB_VALID_TO_DATE = MiscUtil.asDate(2066, 5, 4, 3, 2, 1);

    private static final String ACCOUNT_PROTECTED_SYSTEM = "system";

    private static String userWallyOid;

    boolean alwaysCheckTimestamp;

    private long timeBeforeSync;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(USER_TEMPLATE_SYNC, initResult);

        if (areMarksSupported()) {
            repoAdd(CommonInitialObjects.ARCHETYPE_OBJECT_MARK, initResult);
            repoAdd(CommonInitialObjects.MARK_PROTECTED, initResult);
        }

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
    }

    protected int getNumberOfExtraDummyUsers() {
        return 0;
    }

    protected boolean isReconciliation() {
        return false;
    }

    /**
     * Mancomb is added on green resource. User should be created and linked.
     */
    @Test
    public void test110AddGreenAccountMancomb() throws Exception {
        given();
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(6);

        DummyAccount account = new DummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        account.setEnabled(true);
        account.setValidFrom(ACCOUNT_MANCOMB_VALID_FROM_DATE);
        account.setValidTo(ACCOUNT_MANCOMB_VALID_TO_DATE);
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");

        when("mancomb account is added on dummy green");
        getGreenResource()
                .addAccount(account);

        and("green sync is run");
        runSyncTasks(GREEN);

        then();
        dumpSyncTaskTrees(GREEN);

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, getGreenResourceObject());
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_GREEN_OID,
                accountMancomb.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancomb);
        assertValidFrom(accountMancomb, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(accountMancomb, ACCOUNT_MANCOMB_VALID_TO_DATE);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb was not created", userMancomb);
        assertLiveLinks(userMancomb, 1);
        assertAdministrativeStatusEnabled(userMancomb);
        assertValidFrom(userMancomb, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userMancomb, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userMancomb, accountMancomb);

        assertUsers(7);

        assertTaskStatistics();

        // notifications
        displayAllNotifications();
        assertSingleDummyTransportMessageContaining("simpleAccountNotifier-SUCCESS", "Channel: " + getExpectedChannel());
        notificationManager.setDisabled(true);
    }

    private void assertTaskStatistics() throws CommonException {
        String syncTaskOid = getGreenSyncTask().oid;
        PrismObject<TaskType> syncTaskTree = getTaskTree(syncTaskOid);
        OperationStatsType stats = TaskOperationStatsUtil.getOperationStatsFromTree(syncTaskTree.asObjectable(), prismContext);
        displayValue("sync task stats", TaskOperationStatsUtil.format(stats));

        ProvisioningStatisticsType provisioningStatistics = stats.getEnvironmentalPerformanceInformation().getProvisioningStatistics();
        assertThat(provisioningStatistics.getEntry()).hasSize(1);
        assertThat(provisioningStatistics.getEntry().get(0).getResourceRef().getOid()).isEqualTo(RESOURCE_DUMMY_GREEN_OID);
        assertThat(getOrig(provisioningStatistics.getEntry().get(0).getResourceRef().getTargetName())).isEqualTo("Dummy Resource Green");
        assertThat(provisioningStatistics.getEntry().get(0).getOperation()).isNotEmpty(); // search and sometimes get

        if (isReconciliation()) {
            // MID-6930: We should process exactly 1 item even for partitioned reconciliation:
            // mancomb must not be processed in the 3rd part!
            // @formatter:off
            assertPerformance(syncTaskOid, "progress")
                    .display()
                    .child(ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_ID)
                        .assertItemsProcessed(1)
                    .end()
                    .child(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_ID)
                        .assertItemsProcessed(0);
            // @formatter:on
        }
    }

    protected abstract String getExpectedChannel();

    /**
     * Add wally to the green dummy resource. User should be created and linked.
     */
    @Test
    public void test210AddGreenAccountWally() throws Exception {
        given();
        rememberTimeBeforeSync();

        when("Wally is added on green resource");
        getGreenController()
                .addAccount(ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", "Scabb Island");

        and("green task is run");
        runSyncTasks(GREEN);

        then();
        PrismObject<ShadowType> accountWallyGreen = checkGreenWallyAccount("Wally Feed");
        assertShadowOperationalData(accountWallyGreen);

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally was not created", userWally);
        userWallyOid = userWally.getOid();
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", null, null);
        assertLiveLinks(userWally, 1);

        assertUsers(8);

        assertLinked(userWally, accountWallyGreen);
    }

    /**
     * Add wally also to the blue dummy resource. User should be linked to this account.
     */
    @Test
    public void test220AddBlueAccountWally() throws Exception {
        given();
        rememberTimeBeforeSync();

        when("wally is added to blue resource");
        getBlueController()
                .addAccount(ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", "Scabb Island");

        and("sync tasks are run");
        // Note that blue is not authoritative regarding creating new users.
        // Also, make sure that the "kickback" sync cycle of the other resource runs to completion.
        runSyncTasks(BLUE, GREEN, BLUE);

        then();
        PrismObject<ShadowType> accountWallyGreen = checkGreenWallyAccount("Wally Feed");
        assertShadowOperationalData(accountWallyGreen);

        PrismObject<ShadowType> accountWallyBlue = checkBlueWallyAccount("Wally Feed");
        assertShadowOperationalData(accountWallyBlue);

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally was not created", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", null, null);
        assertLiveLinks(userWally, 2);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);

        assertUsers(8);
    }

    /**
     * Add mancomb also to the blue dummy resource. This account should be linked to the existing user.
     * Similar to the previous test but blue resource has a slightly different correlation expression.
     */
    @Test
    public void test315AddBlueAccountMancomb() throws Exception {
        given();
        rememberTimeBeforeSync();

        when("mancomb is added to blue");
        getBlueController()
                .addAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME, "Mancomb Seepgood", "Melee Island");

        and("sync tasks are run");
        runSyncTasks(BLUE, GREEN, BLUE);

        then();
        // The checks are simplified here because the developer has a lazy mood :-)
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME, "Mancomb Seepgood", true);
        assertDummyAccount(RESOURCE_DUMMY_GREEN_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME, "Mancomb Seepgood", true);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb disappeared", userMancomb);
        assertUser(userMancomb, userMancomb.getOid(), ACCOUNT_MANCOMB_DUMMY_USERNAME, "Mancomb Seepgood", null, null);
        assertLiveLinks(userMancomb, 2);
        assertAccount(userMancomb, RESOURCE_DUMMY_BLUE_OID);
        assertAccount(userMancomb, RESOURCE_DUMMY_GREEN_OID);

        assertUsers(8);
    }

    /**
     * Wally gets the default account - directly by modifying the user
     */
    @Test
    public void test360AddDefaultAccountWallyByUserModification() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        assertEquals("OID of user wally have changed", userWallyOid, userWally.getOid());

        ObjectDelta<UserType> userDelta = createModifyUserAddAccount(userWally.getOid(), getDummyResourceObject());
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(userDelta);

        when("default account for wally is added to the user");
        modelService.executeChanges(deltas, null, task, result);

        and("sync tasks are run, checking they do not break anything");
        // We don't expect these tasks doing anything useful (because we modified the user directly).
        // We just check they don't break anything.
        runSyncTasks(DEFAULT, BLUE, GREEN);

        then();
        PrismObject<ShadowType> accountWallyDefault = checkWallyAccount(
                getDummyResourceObject(), getDummyResource(), "default", "Wally Feed");
        assertShadowOperationalData(accountWallyDefault);

        PrismObject<ShadowType> accountWallyBlue = checkBlueWallyAccount("Wally Feed");
        if (alwaysCheckTimestamp) { assertShadowOperationalData(accountWallyBlue); }

        PrismObject<ShadowType> accountWallyGreen = checkGreenWallyAccount("Wally Feed");
        if (alwaysCheckTimestamp) { assertShadowOperationalData(accountWallyGreen); }

        userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", null, null);
        assertLiveLinks(userWally, 3);

        assertLinked(userWally, accountWallyDefault);
        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);

        assertUsers(8 + getNumberOfExtraDummyUsers());
    }

    /**
     * Sets the special user template for the green resource.
     */
    @Test
    public void test365AssumeUserTemplateForGreenResource() throws CommonException {
        assumeUserTemplate(
                USER_TEMPLATE_SYNC.oid,
                getGreenResourceObject().asObjectable(),
                getTestOperationResult());
    }

    /**
     * Change wally's full name on the green account. There is an inbound mapping to the user so it should propagate.
     * There is also outbound mapping from the user to dummy account, therefore it should propagate there as well.
     * (But see the comment in the test.)
     */
    @Test
    public void test370ModifyFullNameOnGreenAccountWally() throws Exception {
        given();
        rememberTimeBeforeSync();

        when("wally is changed on green");
        getGreenResource()
                .getAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME)
                .replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Wally B. Feed");

        and("blue and green sync tasks are run");
        runSyncTasks(BLUE, GREEN);

        then();
        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(
                userWally,
                userWallyOid,
                ACCOUNT_WALLY_DUMMY_USERNAME,
                "Wally B. Feed",
                null,
                "Wally B. Feed from Sync");

        PrismObject<ShadowType> accountWallyBlue = checkBlueWallyAccount("Wally Feed");
        if (alwaysCheckTimestamp) { assertShadowOperationalData(accountWallyBlue); }

        PrismObject<ShadowType> accountWallyGreen = checkGreenWallyAccount("Wally B. Feed");
        assertShadowOperationalData(accountWallyGreen);

        PrismObject<ShadowType> accountWallyDefault;

        if (isReconciliation()) {
            // The analysis described in MID-2518 is still valid:
            // - If we run the sync tasks like "blue then green", the full name on the default resource is not changed.
            // - If "green then blue", the full name is changed during the green sync task execution.
            accountWallyDefault = checkDefaultWallyAccount(null);
        } else {
            accountWallyDefault = checkDefaultWallyAccount("Wally B. Feed");
        }
        assertShadowOperationalData(accountWallyDefault);

        assertLiveLinks(userWally, 3);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);
        assertLinked(userWally, accountWallyDefault);

        assertUsers(8 + getNumberOfExtraDummyUsers());
    }

    /**
     * Change wally's user full name. Full name has normal mapping on default dummy.
     * See if the change propagates correctly. Also see that there are no side-effects.
     */
    @Test
    public void test380ModifyFullNameOnUserWally() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();

        when("Wally's full name is modified in user object");
        modifyUserReplace(userWallyOid, UserType.F_FULL_NAME, task, result, PrismTestUtil.createPolyString("Bloodnose"));

        and("sync tasks are run - will they screw things up?");
        runSyncTasks(DEFAULT, BLUE, GREEN, GREEN);

        // Note that we run green recon twice here. If the recon already searched for current state of the
        // wally account, the old value ("Wally B. Feed") is stored in the memory. Even if we rewrite
        // the account state by this operation the value already read into memory will be used instead
        // and it will be propagated to other resources. The next recon run should fix it in the user.
        // But as the mapping to default dummy is normal, the recon will not fix it on the resource.

        then();
        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(
                userWally,
                userWallyOid,
                ACCOUNT_WALLY_DUMMY_USERNAME,
                "Bloodnose",
                null,
                "Bloodnose from Sync");

        PrismObject<ShadowType> accountWallyBlue = checkBlueWallyAccount("Wally Feed");
        if (alwaysCheckTimestamp) { assertShadowOperationalData(accountWallyBlue); }

        PrismObject<ShadowType> accountWallyGreen = checkGreenWallyAccount("Bloodnose");
        assertShadowOperationalData(accountWallyGreen);

        PrismObject<ShadowType> accountWallyDefault = findAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME, getDummyResourceObject());
        String fullNameDummyAttribute = IntegrationTestTools.getAttributeValue(
                accountWallyDefault.asObjectable(),
                new QName(MidPointConstants.NS_RI, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));
        if (!"Bloodnose".equals(fullNameDummyAttribute) && !"Wally B. Feed".equals(fullNameDummyAttribute)) {
            AssertJUnit.fail("Wrong full name on default dummy resource: " + fullNameDummyAttribute);
        }
        assertShadowOperationalData(accountWallyDefault);

        assertLiveLinks(userWally, 3);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);
        assertLinked(userWally, accountWallyDefault);

        assertUsers(8 + getNumberOfExtraDummyUsers());
    }

    /**
     * Change user locality. Locality has strong mapping on default dummy.
     * See if the change propagates correctly. Also see that there are no side-effects.
     */
    @Test
    public void test382ModifyLocalityOnUserWally() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();

        when();
        modifyUserReplace(userWallyOid, UserType.F_LOCALITY, task, result, PrismTestUtil.createPolyString("Plunder island"));

        and("sync tasks are run - will they screw things up?");
        runSyncTasks(DEFAULT, BLUE, GREEN, GREEN);

        // Note we run green recon twice here. If the recon already searched for current state of the
        // wally account, the old value is stored in the memory. Even if we rewrite
        // the account state by this operation the value already read into memory will be used instead
        // and it will be propagated to other resources. The next recon run should fix it.
        // Both in user and on the resource.

        then();
        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Bloodnose", null, "Bloodnose from Sync");
        PrismAsserts.assertPropertyValue(userWally, UserType.F_LOCALITY, PrismTestUtil.createPolyString("Plunder island"));

        PrismObject<ShadowType> accountWallyBlue = checkBlueWallyAccount("Wally Feed");
        if (alwaysCheckTimestamp) { assertShadowOperationalData(accountWallyBlue); }

        PrismObject<ShadowType> accountWallyGreen = checkGreenWallyAccount("Bloodnose");
        assertShadowOperationalData(accountWallyGreen);

        PrismObject<ShadowType> accountWallyDefault = findAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME, getDummyResourceObject());
        String fullNameDummyAttribute =
                IntegrationTestTools.getAttributeValue(accountWallyDefault.asObjectable(), DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME);
        if (!"Bloodnose".equals(fullNameDummyAttribute) && !"Wally B. Feed".equals(fullNameDummyAttribute)) {
            AssertJUnit.fail("Wrong full name on default dummy resource: " + fullNameDummyAttribute);
        }
        assertShadowOperationalData(accountWallyDefault);
        assertShadowOperationalData(accountWallyDefault);

        assertDummyAccountAttribute(RESOURCE_DUMMY_GREEN_NAME, ACCOUNT_WALLY_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Plunder island");
        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_WALLY_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Scabb Island");
        assertDummyAccountAttribute(null, ACCOUNT_WALLY_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Plunder island");

        assertLiveLinks(userWally, 3);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);
        assertLinked(userWally, accountWallyDefault);

        assertUsers(8 + getNumberOfExtraDummyUsers());
    }

    /**
     * Delete default dummy account of wally.
     *
     * Dummy resource has unlinkAccount sync reaction for deleted situation. The account should be unlinked
     * but the user and other accounts should remain as they were.
     */
    @Test
    public void test400DeleteDefaultAccountWally() throws Exception {
        given();
        rememberTimeBeforeSync();

        when();
        getDummyResource().deleteAccountByName(ACCOUNT_WALLY_DUMMY_USERNAME);

        displayValue("Dummy (default) resource", getDummyResource().debugDump());

        and("sync tasks are run");
        runSyncTasks(DEFAULT, BLUE, GREEN);

        then();
        assertNoDummyAccount(ACCOUNT_WALLY_DUMMY_USERNAME);
        assertShadow(ACCOUNT_WALLY_DUMMY_USERNAME, getDummyResourceObject())
                .assertTombstone()
                .assertSynchronizationSituation(SynchronizationSituationType.DELETED);

        PrismObject<ShadowType> accountWallyBlue = checkBlueWallyAccount("Wally Feed");
        if (alwaysCheckTimestamp) { assertShadowOperationalData(accountWallyBlue); }

        PrismObject<ShadowType> accountWallyGreen = checkGreenWallyAccount("Bloodnose");
        if (alwaysCheckTimestamp) { assertShadowOperationalData(accountWallyGreen); }

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Bloodnose", null, "Bloodnose from Sync");
        assertLinks(userWally, 2, 1);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);

        assertUsers(8 + getNumberOfExtraDummyUsers());
    }

    /**
     * Delete green dummy account of wally.
     *
     * Green dummy resource has deleteUser sync reaction for deleted situation. This should delete the user
     * and all other accounts.
     *
     * MID-4522
     */
    @Test
    public void test410DeleteGreenAccountWally() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        getGreenResource().deleteAccountByName(ACCOUNT_WALLY_DUMMY_USERNAME);

        and("sync tasks are run");
        runSyncTasks(DEFAULT);
        if (isReconciliation()) {
            // Reconciliation finds here that green account is missing, runs the discovery,
            // and this produces an error (because the user is deleted during the discovery).
            runSyncTasksErrorsOk(BLUE);
        } else {
            runSyncTasks(BLUE);
        }
        runSyncTasks(GREEN);

        then();
        assertNoDummyAccount(ACCOUNT_WALLY_DUMMY_USERNAME);
        assertShadow(ACCOUNT_WALLY_DUMMY_USERNAME, getDummyResourceObject())
                .assertTombstone()
                .assertSynchronizationSituation(SynchronizationSituationType.DELETED);

        assertNoDummyAccount(RESOURCE_DUMMY_GREEN_NAME, ACCOUNT_WALLY_DUMMY_USERNAME);
        assertShadow(ACCOUNT_WALLY_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME))
                .assertTombstone()
                .assertSynchronizationSituation(SynchronizationSituationType.DELETED);

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNull("User wally is not gone", userWally);

        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_WALLY_DUMMY_USERNAME);
        assertNoShadow(ACCOUNT_WALLY_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), result);

        assertUsers(7 + getNumberOfExtraDummyUsers());
    }

    /**
     * Re-adds wally green account.
     */
    @Test
    public void test510AddGreenAccountWally() throws Exception {
        given();
        rememberTimeBeforeSync();

        when();
        getGreenController()
                .addAccount(ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", "Scabb Island");

        and("green sync task is run");
        runSyncTasks(GREEN);

        then();
        PrismObject<ShadowType> accountWallyGreen = checkGreenWallyAccount("Wally Feed");
        assertShadowOperationalData(accountWallyGreen);

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally was not created", userWally);
        userWallyOid = userWally.getOid();
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", null, "Wally Feed from Sync");
        assertLiveLinks(userWally, 1);
        assertLinked(userWally, accountWallyGreen);

        assertUsers(8 + getNumberOfExtraDummyUsers());
    }

    /**
     * Calypso is a protected account. It should not be touched by the sync.
     */
    @Test
    public void test600AddDummyGreenAccountCalypso() throws Exception {
        given();
        rememberTimeBeforeSync();

        // Preconditions
        assertUsers(8 + getNumberOfExtraDummyUsers());

        DummyAccount account = new DummyAccount(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Calypso");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "The Seven Seas");

        when("calypso green account is added");
        getGreenResource().addAccount(account);

        and("green sync task is run");
        runSyncTasks(GREEN);

        then();
        PrismObject<ShadowType> accountShadow = findAccountByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME, getGreenResourceObject());
        display("Account calypso", accountShadow);
        assertNotNull("No calypso account shadow", accountShadow);
        assertEquals("Wrong resourceRef in calypso account", RESOURCE_DUMMY_GREEN_OID,
                accountShadow.asObjectable().getResourceRef().getOid());
        assertTrue("Calypso shadow is NOT protected", accountShadow.asObjectable().isProtectedObject());

        PrismObject<UserType> userCalypso = findUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        display("User calypso", userCalypso);
        assertNull("User calypso was created, it should not", userCalypso);

        assertUsers(8 + getNumberOfExtraDummyUsers());
    }

    /**
     * System is a protected account. It should not be touched by the sync.
     * (using script in protected filter)
     */
    @Test
    public void test601AddDummyGreenAccountSystem() throws Exception {
        given();
        rememberTimeBeforeSync();

        // Preconditions
        assertUsers(8 + getNumberOfExtraDummyUsers());

        DummyAccount account = new DummyAccount(ACCOUNT_PROTECTED_SYSTEM);
        account.setEnabled(true);
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "System Account");

        when();
        getGreenResource().addAccount(account);

        and("green sync task is run");
        runSyncTasks(GREEN);

        then();
        PrismObject<ShadowType> accountShadow = findAccountByUsername(ACCOUNT_PROTECTED_SYSTEM, getGreenResourceObject());
        display("Account system", accountShadow);
        assertNotNull("No system account shadow", accountShadow);
        assertEquals("Wrong resourceRef in system account", RESOURCE_DUMMY_GREEN_OID,
                accountShadow.asObjectable().getResourceRef().getOid());
        assertTrue("System shadow is NOT protected", accountShadow.asObjectable().isProtectedObject());

        PrismObject<UserType> userSystem = findUserByUsername(ACCOUNT_PROTECTED_SYSTEM);
        display("User system", userSystem);
        assertNull("User system was created, it should not", userSystem);

        assertUsers(8 + getNumberOfExtraDummyUsers());
    }

    /**
     * Accounts starting with X are admin accounts (intent "admin"). Check if synchronization gets this right.
     */
    @Test
    public void test700AddDummyGreenAccountXjojo() throws Exception {
        given();
        rememberTimeBeforeSync();

        // Preconditions
        assertUsers(8 + getNumberOfExtraDummyUsers());

        DummyAccount account = new DummyAccount("Xjojo");
        account.setEnabled(true);
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jojo the Monkey");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Scabb Island");

        when("Xjojo is added on green");
        getGreenResource().addAccount(account);

        and("green sync task is run");
        runSyncTasks(GREEN);

        then();
        PrismObject<ShadowType> accountAfter = findAccountByUsername("Xjojo", getGreenResourceObject());
        display("Account after", accountAfter);
        assertNotNull("No account shadow", accountAfter);
        assertEquals("Wrong resourceRef in account shadow", RESOURCE_DUMMY_GREEN_OID,
                accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter);
        assertShadowKindIntent(accountAfter, ShadowKindType.ACCOUNT, "admin");

        PrismObject<UserType> userAfter = findUserByUsername("jojo");
        display("User after", userAfter);
        assertNotNull("User jojo was not created", userAfter);
        assertLiveLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);

        assertLinked(userAfter, accountAfter);

        assertUsers(9 + getNumberOfExtraDummyUsers());

        PrismObject<TaskType> syncTaskTree = getTaskTree(getGreenSyncTask().oid);
        OperationStatsType stats = TaskOperationStatsUtil.getOperationStatsFromTree(syncTaskTree.asObjectable(), prismContext);
        displayValue("sync task stats", TaskOperationStatsUtil.format(stats));
    }

    private void assumeUserTemplate(String templateOid, ResourceType resource, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        repositoryService.modifyObject(
                ResourceType.class,
                resource.getOid(),
                deltaFor(ResourceType.class)
                        .item(ResourceType.F_SYNCHRONIZATION,
                                SynchronizationType.F_OBJECT_SYNCHRONIZATION,
                                resource.getSynchronization().getObjectSynchronization().get(0).getId(), // TODO stop assuming the order!
                                ObjectSynchronizationType.F_OBJECT_TEMPLATE_REF)
                        .replace(
                                ObjectTypeUtil.createObjectRef(templateOid, ObjectTypes.OBJECT_TEMPLATE))
                        .asItemDeltas(),
                result);

        ResourceType res = repositoryService.getObject(ResourceType.class, resource.getOid(), null, result).asObjectable();
        assertNotNull(res);
        assertNotNull("Synchronization is not specified", res.getSynchronization());
        ObjectSynchronizationType ost = determineSynchronization(res, UserType.COMPLEX_TYPE, "default account type");
        assertNotNull("object sync type is not specified", ost);
        assertNotNull("user template not specified", ost.getObjectTemplateRef());
        assertEquals("Wrong user template in resource", templateOid, ost.getObjectTemplateRef().getOid());
    }

    @SuppressWarnings("SameParameterValue")
    private @NotNull PrismObject<ShadowType> checkBlueWallyAccount(String expectedFullName)
            throws CommonException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException,
            InterruptedException {
        return checkWallyAccount(
                getBlueResourceObject(), getBlueResource(), "blue", expectedFullName);
    }

    private @NotNull PrismObject<ShadowType> checkGreenWallyAccount(String expectedFullName)
            throws CommonException, ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException,
            ConnectException {
        return checkWallyAccount(
                getGreenResourceObject(), getGreenResource(), "green", expectedFullName);
    }

    private @NotNull PrismObject<ShadowType> checkDefaultWallyAccount(String expectedFullName)
            throws CommonException, ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException,
            ConnectException {
        return checkWallyAccount(
                getDummyResourceObject(), getDummyResource(), "default", expectedFullName);
    }

    private PrismObject<ShadowType> checkWallyAccount(
            PrismObject<ResourceType> resource, DummyResource dummy, String resourceDesc, String expectedFullName)
            throws CommonException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException,
            InterruptedException {
        PrismObject<ShadowType> accountShadowWally = findAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME, resource);
        display("Account shadow wally (" + resourceDesc + ")", accountShadowWally);
        assertEquals("Wrong resourceRef in wally account (" + resourceDesc + ")", resource.getOid(),
                accountShadowWally.asObjectable().getResourceRef().getOid());
        if (expectedFullName != null) {
            IntegrationTestTools.assertAttribute(accountShadowWally.asObjectable(), resource.asObjectable(),
                    DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, expectedFullName);
        }

        DummyAccount dummyAccount = dummy.getAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        displayDumpable("Account wally (" + resourceDesc + ")", dummyAccount);
        assertNotNull("No dummy account (" + resourceDesc + ")", dummyAccount);
        if (expectedFullName != null) {
            assertEquals("Wrong dummy account fullname (" + resourceDesc + ")", expectedFullName,
                    dummyAccount.getAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));
        }

        return accountShadowWally;
    }

    void rememberTimeBeforeSync() {
        timeBeforeSync = System.currentTimeMillis();
    }

    private void assertShadowOperationalData(PrismObject<ShadowType> shadow) {
        assertShadowOperationalData(shadow, SynchronizationSituationType.LINKED, timeBeforeSync);
    }

    private DummyResource getBlueResource() {
        return getDummyResource(RESOURCE_DUMMY_BLUE_NAME);
    }

    DummyResource getGreenResource() {
        return getDummyResource(RESOURCE_DUMMY_GREEN_NAME);
    }

    private PrismObject<ResourceType> getBlueResourceObject() {
        return getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME);
    }

    private PrismObject<ResourceType> getGreenResourceObject() {
        return getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME);
    }

    private DummyResourceContoller getBlueController() {
        return getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME);
    }

    private DummyResourceContoller getGreenController() {
        return getDummyResourceController(RESOURCE_DUMMY_GREEN_NAME);
    }

    private TestTask getGreenSyncTask() {
        return getSyncTask(GREEN);
    }

    /**
     * @return Map providing a synchronization task for each resource color.
     */
    protected abstract Map<Color, TestTask> getTaskMap();

    protected TestTask getSyncTask(Color whichOne) {
        return MiscUtil.requireNonNull(
                getTaskMap().get(whichOne),
                () -> new IllegalStateException("No sync task for " + whichOne));
    }

    void runSyncTasks(Color... whichOnes) throws CommonException {
        for (Color resourceColor : whichOnes) {
            getSyncTask(resourceColor)
                    .rerun(getTestOperationResult());
        }
    }

    private void runSyncTasksErrorsOk(Color... whichOnes) throws CommonException {
        for (Color resourceColor : whichOnes) {
            getSyncTask(resourceColor)
                    .rerunErrorsOk(getTestOperationResult());
        }
    }

    private void dumpSyncTaskTrees(Color... whichOnes) throws SchemaException, ObjectNotFoundException {
        for (Color resourceColor : whichOnes) {
            dumpTaskTree(getSyncTask(resourceColor).oid, getTestOperationResult());
        }
    }

    enum Color {
        DEFAULT, BLUE, GREEN
    }
}
