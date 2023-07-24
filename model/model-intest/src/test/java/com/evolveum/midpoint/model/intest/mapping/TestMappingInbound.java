/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.mapping;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME;

import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import javax.xml.namespace.QName;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.model.impl.sync.SynchronizationContext;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.model.intest.sync.AbstractSynchronizationStoryTest;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Tests inbound mappings. Uses live sync to do that.
 * These tests are much simpler and more focused than those in {@link AbstractSynchronizationStoryTest}.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMappingInbound extends AbstractMappingTest {

    private static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";

    private static final String ACCOUNT_LEELOO_USERNAME = "leeloo";
    private static final String ACCOUNT_LEELOO_FULL_NAME_MULTIPASS = "Leeloo Dallas Multipass";
    private static final String ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI = "Leeloominaï Lekatariba Lamina-Tchaï Ekbat De Sebat";
    private static final String ACCOUNT_LEELOO_PROOF_STRANGE = "Hereby and hèrěnow\nThis is a multi-line claim\nwith a sôme of špecial chäracters\nAnd even some CRLF file endings\r\nLike this\r\nAnd to be completely nuts, even some LFRC\n\rThis does not really proves anything\n   It is just trying to reproduce the problem\nIn addition to be quite long\nand ugly\nLorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\nUt enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\nDuis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.\nExcepteur sint occaecat cupidatat non proident,\nsunt in culpa qui officia deserunt mollit anim id est laborum.\nAnd so on …";

    private static final String ACCOUNT_RISKY_USERNAME = "risky";
    private static final String ACCOUNT_GDPR_USERNAME = "gdpr";

    private static final TestResource<RoleType> ROLE_WHEEL = new TestResource<>(TEST_DIR, "role-wheel.xml", "ad1782d5-48ae-4f86-a26b-efea45d88f3c");
    private static final String GROUP_WHEEL_NAME = "wheel";

    private static final TestTask TASK_LIVE_SYNC_DUMMY_TEA_GREEN = new TestTask(
            TEST_DIR, "task-dummy-tea-green-livesync.xml", "10000000-0000-0000-5555-55550000c404");

    private static final String LOCKER_BIG_SECRET = "BIG secret";

    private static final ItemName DATA_PROTECTION = new ItemName(NS_PIRACY, "dataProtection");

    private static final ItemName RISK_VECTOR = new ItemName(NS_PIRACY, "riskVector");
    private static final ItemName RISK = new ItemName(NS_PIRACY, "risk");
    private static final ItemName VALUE = new ItemName(NS_PIRACY, "value");

    private static final String DUMMY_ACCOUNT_ATTRIBUTE_CONTROLLER_NAME = "controllerName";
    private static final String DUMMY_ACCOUNT_ATTRIBUTE_ROLE_NAME = "roleName";
    private static final String DUMMY_ACCOUNT_ATTRIBUTE_ARCHETYPE_NAME = "archetypeName";

    private static final TestResource<RoleType> ROLE_SIMPLE = new TestResource<>(TEST_DIR, "role-simple.xml", "dc2b28f4-3aab-4212-8ab7-c4f5fc0c511a");
    private static final TestResource<ArchetypeType> ARCHETYPE_PIRATE = new TestResource<>(TEST_DIR, "archetype-pirate.xml", "0bb1d8df-501d-4648-9d36-c8395df95183");

    private static final DummyTestResource RESOURCE_DUMMY_TEA_GREEN = new DummyTestResource(
            TEST_DIR, "resource-dummy-tea-green.xml", "10000000-0000-0000-0000-00000000c404", "tea-green",
            controller -> {
                controller.extendSchemaPirate();
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                                DUMMY_ACCOUNT_ATTRIBUTE_LOCKER_NAME, String.class, false, false)
                        .setSensitive(true);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        DUMMY_ACCOUNT_ATTRIBUTE_PROOF_NAME, String.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        DUMMY_ACCOUNT_ATTRIBUTE_TREASON_RISK_NAME, Integer.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        DUMMY_ACCOUNT_ATTRIBUTE_CONTROLLER_NAME, String.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        DUMMY_ACCOUNT_ATTRIBUTE_ROLE_NAME, String.class, false, true);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        DUMMY_ACCOUNT_ATTRIBUTE_ARCHETYPE_NAME, String.class, false, false);
                controller.setSyncStyle(DummySyncStyle.SMART);
            }
    );

    private static final String ATTR_PHOTO = "photo";

    private static final DummyTestResource RESOURCE_DUMMY_TARGET_PHOTOS = new DummyTestResource(
            TEST_DIR, "resource-target-photos.xml", "8d181eee-458a-439d-a9f7-d7256a91f557", "target-photos",
            controller ->
                    controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                            ATTR_PHOTO, byte[].class, false, false)
                            .setReturnedByDefault(false));

    private static final TestResource<TaskType> TASK_IMPORT_TARGET_PHOTOS = new TestResource<>(
            TEST_DIR, "task-import-target-photos.xml", "734cca6b-0012-4e43-888b-9717deedd8bc");

    private ProtectedStringType mancombLocker;
    private String userLeelooOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        repoAdd(ROLE_SIMPLE, initResult);
        repoAdd(ARCHETYPE_PIRATE, initResult);

        initDummyResource(RESOURCE_DUMMY_TEA_GREEN, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_TARGET_PHOTOS, initTask, initResult);

        addObject(ROLE_WHEEL, initTask, initResult); // creates a resource object as well

        TASK_LIVE_SYNC_DUMMY_TEA_GREEN.init(this, initTask, initResult);
        TASK_LIVE_SYNC_DUMMY_TEA_GREEN.rerun(initResult); // to get the token
    }

    @Test
    public void test010Sanity() throws Exception {
        given();
        Task task = getTestTask();

        when();
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_TEA_GREEN.oid, task, task.getResult());

        then();
        TestUtil.assertSuccess(testResult);

        ResourceType resourceType = getDummyResourceType(RESOURCE_DUMMY_TEA_GREEN.name);
        ResourceSchema returnedSchema = ResourceSchemaFactory.getRawSchema(resourceType);
        displayDumpable("Parsed resource schema (tea-green)", returnedSchema);
        ResourceObjectDefinition accountDef = getDummyResourceController(RESOURCE_DUMMY_TEA_GREEN.name)
                .assertDummyResourceSchemaSanityExtended(returnedSchema, resourceType, false,
                        DummyResourceContoller.PIRATE_SCHEMA_NUMBER_OF_DEFINITIONS + 6); // MID-5197

        ResourceAttributeDefinition<?> lockerDef = accountDef.findAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_LOCKER_NAME);
        assertNotNull("No locker attribute definition", lockerDef);
        assertEquals("Wrong locker attribute definition type", ProtectedStringType.COMPLEX_TYPE, lockerDef.getTypeName());

        assertDummyGroup(RESOURCE_DUMMY_TEA_GREEN.name, GROUP_WHEEL_NAME, "This is the wheel group", true);
    }

    /**
     * Create mancomb's account and run the live sync.
     *
     * The account has a single entitlement (group `wheel`).
     */
    @Test
    public void test110AddDummyTeaGreenAccountMancomb() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = new DummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_LOCKER_NAME, LOCKER_BIG_SECRET); // MID-5197
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "water");
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_ROLE_NAME, "simple");
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_ARCHETYPE_NAME, "pirate");
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_PROOF_NAME, "x-x-x");
        DummyGroup wheel = RESOURCE_DUMMY_TEA_GREEN.controller.getDummyResource().getGroupByName(GROUP_WHEEL_NAME);
        wheel.addMember(account.getName());

        when();

        getDummyResource(RESOURCE_DUMMY_TEA_GREEN.name).addAccount(account);

        TASK_LIVE_SYNC_DUMMY_TEA_GREEN.rerun(result);

        then();

        PrismObject<ShadowType> accountMancomb = getAndCheckMancombAccount();
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED, null);

        UserAsserter<Void> mancombUserAsserter = assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        // @formatter:off
        mancombLocker = mancombUserAsserter
                .links()
                    .singleLive()
                        .assertOid(accountMancomb.getOid())
                        .end()
                    .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .assertDescription("x-x-x")
                .assertFullName("Mancomb Seepgood")
                .assignments()
                    .forRole(ROLE_SIMPLE.oid)
                        .assertSubtype("auto-role")
                        .assertOriginMappingName("Role by name") // MID-5846
                    .end()
                    .forArchetype(ARCHETYPE_PIRATE.oid)
                        .assertSubtype("auto-archetype")
                        .assertOriginMappingName("Archetype by name") // MID-5846
                    .end()
                    .forRole(ROLE_WHEEL.oid)
                        .assertSubtype("inbound")
                    .end()
                .end()
                .extension()
                    .property(PIRACY_LOCKER)
                        .singleValue()
                        .protectedString()
                        .assertIsEncrypted()
                        .assertCompareCleartext(LOCKER_BIG_SECRET)
                        .getProtectedString();
        // @formatter:on

        assertJpegPhoto(UserType.class, mancombUserAsserter.getOid(), "water".getBytes(StandardCharsets.UTF_8), result);
    }

    /**
     * Change an attribute that maps onto jpegPhoto, and check that the photo is updated.
     *
     * MID-5912
     */
    @Test
    public void test120ModifyMancombPhotoSource() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        DummyAccount account = getMancombAccount();
        account.removeAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "water");
        account.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");

        TASK_LIVE_SYNC_DUMMY_TEA_GREEN.rerun(result);

        then();

        getAndCheckMancombAccount();

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);
        assertJpegPhoto(UserType.class, userMancomb.getOid(), "rum".getBytes(StandardCharsets.UTF_8), result);
    }

    /**
     * The same as test120 but using reconcile instead of LS.
     *
     * MID-5912
     */
    @Test
    public void test130ModifyMancombPhotoSourceAndReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        DummyAccount account = getMancombAccount();
        account.removeAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");
        account.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "beer");

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);

        reconcileUser(userMancomb.getOid(), task, result);

        then();

        getAndCheckMancombAccount();

        assertJpegPhoto(UserType.class, userMancomb.getOid(), "beer".getBytes(StandardCharsets.UTF_8), result);
    }

    /**
     * Now changing the photo directly on user object; with reconcile.
     * The original value from the resource should be restored.
     *
     * MID-5912
     */
    @Test
    public void test140ModifyMancombPhotoInRepo() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_JPEG_PHOTO).replaceRealValues(singleton("cherry".getBytes(StandardCharsets.UTF_8)))
                .asObjectDelta(userMancomb.getOid());
        executeChanges(delta, executeOptions().reconcile(), task, result);

        then();

        assertSuccess(result);

        PrismObject<UserType> userMancombAfter = repositoryService.getObject(UserType.class, userMancomb.getOid(),
                schemaService.getOperationOptionsBuilder().retrieve().build(), result);
        display("user mancomb after", userMancombAfter);

        assertJpegPhoto(UserType.class, userMancomb.getOid(), "beer".getBytes(StandardCharsets.UTF_8), result);
    }

    /**
     * Check that the protected value (locker) has not changed during reconciliation,
     * not even in unimportant aspects (IV).
     *
     * MID-5197
     */
    @Test
    public void test150ReconcileMancomb() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        when();

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);

        reconcileUser(userMancomb.getOid(), task, result);

        then();

        PrismObject<ShadowType> accountMancomb = getAndCheckMancombAccount();
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED, null);

        // @formatter:off
        assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME)
                .links()
                    .singleLive()
                        .assertOid(accountMancomb.getOid())
                    .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .assertDescription("x-x-x")
                .assertFullName("Mancomb Seepgood")
                .extension()
                    .property(PIRACY_LOCKER)
                        .singleValue()
                            .protectedString()
                                .assertIsEncrypted()
                                .assertCompareCleartext(LOCKER_BIG_SECRET)
                                // Make sure that this is exactly the same content of protected string
                                // including all the randomized things (IV). If it is the same,
                                // there is a good chance we haven't had any phantom changes
                                // MID-5197
                                .assertEquals(mancombLocker);
        // @formatter:on

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
    }

    /**
     * Reconciles user `mancomb` in maintenance mode. Nothing should change.
     *
     * MID-7062
     */
    @Test
    public void test160ReconcileMancombInMaintenanceMode() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        when();

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);

        turnMaintenanceModeOn(result);
        try {
            reconcileUser(userMancomb.getOid(), task, result);
        } finally {
            turnMaintenanceModeOff(result);
        }

        then();

        assertMancombNotDestroyed();

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
    }

    /**
     * Import user `mancomb` in maintenance mode. Nothing should change.
     *
     * We intentionally skip the maintenance checks that occur on the start of import activity
     * and on the start of synchronization processing.
     *
     * MID-7062
     */
    @Test
    public void test170ImportMancombInMaintenanceModeChecksOff() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        when();

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);

        turnMaintenanceModeOn(result);
        SyncTaskHelper.setSkipMaintenanceCheck(true);
        SynchronizationContext.setSkipMaintenanceCheck(true);
        try {
            executeImportInBackground(task, result);
        } finally {
            SyncTaskHelper.setSkipMaintenanceCheck(false);
            SynchronizationContext.setSkipMaintenanceCheck(false);
            turnMaintenanceModeOff(result);
        }

        then();

        assertTask(task.getOid(), "after")
                .assertSuccess()
                .rootItemProcessingInformation()
                    .display()
                .end();

        assertMancombNotDestroyed();

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(0);
    }

    /**
     * Live sync user `mancomb` in maintenance mode. Nothing should change.
     * We intentionally skip the maintenance checks that occurs on the start of LS activity
     * and on the start of synchronization processing.
     *
     * MID-7062
     */
    @Test
    public void test180LiveSyncMancombInMaintenanceModeChecksOff() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        when();

        // This is a dummy change
        getMancombAccount()
                .addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);

        turnMaintenanceModeOn(result);
        SyncTaskHelper.setSkipMaintenanceCheck(true);
        SynchronizationContext.setSkipMaintenanceCheck(true);
        try {
            TASK_LIVE_SYNC_DUMMY_TEA_GREEN.rerunErrorsOk(result);
        } finally {
            SyncTaskHelper.setSkipMaintenanceCheck(false);
            SynchronizationContext.setSkipMaintenanceCheck(false);
            turnMaintenanceModeOff(result);
        }

        then();

        TASK_LIVE_SYNC_DUMMY_TEA_GREEN.assertAfter()
                .assertFatalError(); // there's an extra check for maintenance mode in ProvisioningServiceImpl#synchronize

        // In the current state of the code no synchronization takes place, so technically the following check is not needed.
        // However, when hunting for MID-7062 it was useful (because the sync was executing) - and it was passing!
        assertMancombNotDestroyed();
    }

    /**
     * Import user `mancomb` in maintenance mode.
     *
     * We disable the initial check for maintenance mode, but keep the before-processing one enabled.
     * So the import task should finish with partial errors. Nothing should be processed.
     *
     * MID-7062
     */
    @Test
    public void test190ImportMancombInMaintenanceModeChecksOn() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        when();

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);

        turnMaintenanceModeOn(result);
        SyncTaskHelper.setSkipMaintenanceCheck(true);
        try {
            executeImportInBackgroundErrorsOk(task, result);
        } finally {
            SyncTaskHelper.setSkipMaintenanceCheck(false);
            turnMaintenanceModeOff(result);
        }

        then();

        assertTask(task.getOid(), "after")
                .assertPartialError()
                .rootItemProcessingInformation()
                    .display()
                    .assertTotalCounts(0, 1, 0);

        // Again, here the synchronization did not take place.
        assertMancombNotDestroyed();

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(0);
    }

    /**
     * Resume the LS task, delete the account, and check that the user is disabled, and user->shadow link is marked as dead.
     * (The reaction is "inactivateFocus".)
     */
    @Test
    public void test300DeleteDummyTeaGreenAccountMancomb() throws Exception {
        OperationResult result = getTestOperationResult();

        when();
        getDummyResource(RESOURCE_DUMMY_TEA_GREEN.name).deleteAccountByName(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        displayValue("Dummy (tea green) resource", getDummyResource(RESOURCE_DUMMY_TEA_GREEN.name).debugDump());

        when("live sync task is run");
        TASK_LIVE_SYNC_DUMMY_TEA_GREEN.rerun(result);

        then();

        assertNoDummyAccount(RESOURCE_DUMMY_TEA_GREEN.name, ACCOUNT_MANCOMB_DUMMY_USERNAME);

        // @formatter:off
        assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME)
                .assertFullName("Mancomb Seepgood")
                .assertAdministrativeStatus(ActivationStatusType.DISABLED)
                .links()
                    .singleDead()
                    .resolveTarget()
                        .display()
                        .assertTombstone()
                        .assertSynchronizationSituation(SynchronizationSituationType.DELETED);
        // @formatter:on
    }

    /**
     * Imports account `leeloo` manually.
     */
    @Test
    public void test400AddUserLeeloo() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        DummyAccount account = new DummyAccount(ACCOUNT_LEELOO_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_LEELOO_FULL_NAME_MULTIPASS);
        getDummyResource(RESOURCE_DUMMY_TEA_GREEN.name).addAccount(account);

        when();

        executeImportInBackground(task, result);

        then();

        // @formatter:off
        userLeelooOid = assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_MULTIPASS)
                .links()
                    .singleLive()
                    .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .getOid();
        // @formatter:on

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class); // metadata
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class); // link
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Nothing has changed in the account. Expect no changes in user.
     * MID-5314
     */
    @Test
    public void test402UserLeelooRecompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        when();

        recomputeUser(userLeelooOid, task, result);

        then();

        // @formatter:off
        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_MULTIPASS)
                .links()
                    .singleLive()
                    .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);
        // @formatter:on

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(0);
    }

    /**
     * Nothing has changed in the account. Expect no changes in user.
     * MID-5314
     */
    @Test
    public void test404UserLeelooReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        when();

        reconcileUser(userLeelooOid, task, result);

        then();

        // @formatter:off
        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_MULTIPASS)
                .links()
                    .singleLive()
                    .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);
        // @formatter:on

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Changed Leeloo's full name. Reconcile should reflect that to user.
     * MID-5314
     */
    @Test
    public void test410UserLeeloominaiReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = getDummyResource(RESOURCE_DUMMY_TEA_GREEN.name).getAccountByUsername(ACCOUNT_LEELOO_USERNAME);
        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI);

        dummyAuditService.clear();

        when();

        reconcileUser(userLeelooOid, task, result);

        then();

        // @formatter:off
        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
                .links()
                    .singleLive()
                    .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);
        // @formatter:on

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Nothing has changed in the account. Expect no changes in user.
     * MID-5314
     */
    @Test
    public void test412UserLeeloominaiRecompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        when();

        recomputeUser(userLeelooOid, task, result);

        then();

        // @formatter:off
        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
                .links()
                    .singleLive()
                    .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);
        // @formatter:on

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(0);
    }

    /**
     * Nothing has changed in the account. Expect no changes in user.
     * MID-5314
     */
    @Test
    public void test414UserLeeloominaiReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        when();

        reconcileUser(userLeelooOid, task, result);

        then();

        // @formatter:off
        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
                .links()
                    .singleLive()
                    .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);
        // @formatter:on

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Changed Leeloo's full name. Reconcile should reflect that to user.
     * MID-5314
     */
    @Test
    public void test420UserLeelooStrangeReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = getDummyResource(RESOURCE_DUMMY_TEA_GREEN.name).getAccountByUsername(ACCOUNT_LEELOO_USERNAME);
        account.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_PROOF_NAME, ACCOUNT_LEELOO_PROOF_STRANGE);

        dummyAuditService.clear();

        when();

        reconcileUser(userLeelooOid, task, result);

        then();

        // @formatter:off
        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
                .assertDescription(ACCOUNT_LEELOO_PROOF_STRANGE)
                .links()
                    .singleLive()
                    .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);
        // @formatter:on

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Nothing has changed in the account. Expect no changes in user.
     * MID-5314
     */
    @Test
    public void test424UserLeelooStrangeReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        when();

        reconcileUser(userLeelooOid, task, result);

        then();

        // @formatter:off
        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
                .assertDescription(ACCOUNT_LEELOO_PROOF_STRANGE)
                .links()
                    .singleLive()
                    .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);
        // @formatter:on

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Tests execution of an inbound that converts a plain value to a (run-time defined) structure.
     *
     * Creates `risky` user and `risky` account and then imports the account manually.
     */
    @Test
    public void test500UserRiskVector() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        /*
         * State before: (death, 1); (treason, 2)
         */
        UserType user = new UserType()
                .name(ACCOUNT_RISKY_USERNAME);
        PrismContainerValue<Containerable> riskVectorDeath =
                user.asPrismObject().getOrCreateExtension().createNewValue()
                        .findOrCreateContainer(RISK_VECTOR).createNewValue();
        riskVectorDeath.findOrCreateProperty(RISK).setRealValue("death");
        riskVectorDeath.findOrCreateProperty(VALUE).setRealValue(1);
        PrismContainerValue<Containerable> riskVectorTreason =
                user.asPrismObject().getExtensionContainerValue()
                        .findContainer(RISK_VECTOR).createNewValue();
        riskVectorTreason.findOrCreateProperty(RISK).setRealValue("treason");
        riskVectorTreason.findOrCreateProperty(VALUE).setRealValue(2);

        addObject(user, task, result);

        DummyAccount account = new DummyAccount(ACCOUNT_RISKY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Risky Risk");
        account.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_TREASON_RISK_NAME, 999);
        getDummyResource(RESOURCE_DUMMY_TEA_GREEN.name).addAccount(account);

        when();

        executeImportInBackground(task, result);

        then();

        assertSuccess(task.getResult());

        /*
         * Expected state after: (death, 1); (treason, 999)
         *
         * The former was kept intact because of set specification.
         * The latter was taken from treasonRisk value from dummy account (and overwritten because of set specification).
         */
        // @formatter:off
        assertUserAfterByUsername(ACCOUNT_RISKY_USERNAME)
                .assertExtensionItems(1)
                .extensionContainer(RISK_VECTOR)
                    .assertSize(2)
                    .value(ValueSelector.itemEquals(RISK, "treason"))
                        .assertPropertyEquals(VALUE, 999)
                        .end()
                    .value(ValueSelector.itemEquals(RISK, "death"))
                        .assertPropertyEquals(VALUE, 1);
        // @formatter:on
    }

    /**
     * Creates user and account `gdpr` and runs an import of the account manually.
     * This is to test a more sophisticated comparison of computed values.
     *
     * MID-6129
     */
    @Test
    public void test510UserDataProtection() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        UserType user = new UserType()
                .name(ACCOUNT_GDPR_USERNAME);
        DataProtectionType protection = new DataProtectionType()
                .controllerName("controller")
                .controllerContact("controller@evolveum.com");
        PrismContainerDefinition<DataProtectionType> protectionDef =
                user.asPrismObject().getDefinition().findContainerDefinition(ItemPath.create(UserType.F_EXTENSION, DATA_PROTECTION));
        PrismContainer<DataProtectionType> protectionContainer = protectionDef.instantiate();
        //noinspection unchecked
        protectionContainer.add(protection.asPrismContainerValue());
        user.asPrismObject().addExtensionItem(protectionContainer);

        addObject(user, task, result);

        DummyAccount account = new DummyAccount(ACCOUNT_GDPR_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "GDPR");
        account.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_CONTROLLER_NAME, "new-controller");
        getDummyResource(RESOURCE_DUMMY_TEA_GREEN.name).addAccount(account);

        when();

        executeImportInBackground(task, result);

        then();

        assertSuccess(task.getResult());

        // @formatter:off
        assertUserAfterByUsername(ACCOUNT_GDPR_USERNAME)
                .assertExtensionItems(1)
                .extensionContainer(DATA_PROTECTION)
                    .assertSize(1)
                    .value(0)
                        .assertPropertyEquals(DataProtectionType.F_CONTROLLER_NAME, "new-controller")
                        .assertPropertyEquals(DataProtectionType.F_CONTROLLER_CONTACT, "new-controller@evolveum.com");
        // @formatter:on
    }

    /**
     * Testing the propagation of user photo from the source resource to the user and then to outbound resource.
     *
     * 1. There is a source account (`test520` on `tea-green`) with the photo (drink) attribute set to `water`.
     * 2. After an import, the user is created.
     * 3. There is an (unlinked) corresponding account (`test520`) on `target-photos` resource, with no photo (yet).
     * 4. The import from `target-photos` should cause the propagation of the photo to the target.
     *
     * MID-7916
     */
    @Test
    public void test520PhotoPropagation() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String name = "test520";

        String water = "water";
        byte[] waterBytes = water.getBytes(StandardCharsets.UTF_8);

        given("there is an account on the source resource");
        DummyAccount sourceAccount = new DummyAccount(name);
        sourceAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, water);
        RESOURCE_DUMMY_TEA_GREEN.controller.getDummyResource().addAccount(sourceAccount);

        and("it is imported via LS");
        TASK_LIVE_SYNC_DUMMY_TEA_GREEN.rerun(result);

        and("the user is created and has a photo");
        String oid = assertUserAfterByUsername(name).getOid();
        assertJpegPhoto(UserType.class, oid, waterBytes, result);

        and("corresponding account exists on the target-photo resource");
        DummyAccount targetAccount = new DummyAccount(name);
        RESOURCE_DUMMY_TARGET_PHOTOS.controller.getDummyResource().addAccount(targetAccount);

        when("import from target resource is run");
        addObject(TASK_IMPORT_TARGET_PHOTOS, task, result);
        rerunTask(TASK_IMPORT_TARGET_PHOTOS.oid, result);

        then("photo on target resource is set");
        assertDummyAccountByUsername(RESOURCE_DUMMY_TARGET_PHOTOS.name, name)
                .display()
                .assertBinaryAttribute(ATTR_PHOTO, waterBytes);
    }

    private void turnMaintenanceModeOn(OperationResult result) throws Exception {
        turnMaintenanceModeOn(RESOURCE_DUMMY_TEA_GREEN.oid, result);
    }

    private void turnMaintenanceModeOff(OperationResult result) throws Exception {
        turnMaintenanceModeOff(RESOURCE_DUMMY_TEA_GREEN.oid, result);
    }

    private void executeImportInBackground(Task task, OperationResult result) throws Exception {
        modelService.importFromResource(RESOURCE_DUMMY_TEA_GREEN.oid, new QName(MidPointConstants.NS_RI,
                SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), task, result);
        waitForTaskFinish(task, true);
    }

    private void executeImportInBackgroundErrorsOk(Task task, OperationResult result) throws Exception {
        modelService.importFromResource(RESOURCE_DUMMY_TEA_GREEN.oid, new QName(MidPointConstants.NS_RI,
                SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), task, result);
        waitForTaskCloseOrSuspend(task.getOid(), 30000);
    }

    private PrismObject<ShadowType> getAndCheckMancombAccount() throws CommonException {
        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME,
                getDummyResourceObject(RESOURCE_DUMMY_TEA_GREEN.name));
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_TEA_GREEN.oid,
                accountMancomb.asObjectable().getResourceRef().getOid());
        return accountMancomb;
    }

    private @NotNull DummyAccount getMancombAccount()
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = getDummyResource(RESOURCE_DUMMY_TEA_GREEN.name)
                .getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("No mancomb account", account);
        return account;
    }

    /** Check that mancomb was not destroyed by inbounds running in the maintenance mode. */
    private void assertMancombNotDestroyed() throws CommonException {
        PrismObject<ShadowType> accountMancomb = getAndCheckMancombAccount();
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED, null);

        // @formatter:off
        assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME)
                .links()
                    .singleLive()
                        .assertOid(accountMancomb.getOid())
                    .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .assertDescription("x-x-x")
                .assertFullName("Mancomb Seepgood")
                .assignments()
                    .forRole(ROLE_WHEEL.oid)
                        .assertSubtype("inbound")
                    .end();
        // @formatter:on
    }
}
