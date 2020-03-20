/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.mapping;

import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.prism.path.ItemPath;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Tests inbound mappings. Uses live sync to do that.
 * These tests are much simpler and more focused than those in AbstractSynchronizationStoryTest.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMappingInbound extends AbstractMappingTest {

    private static final File RESOURCE_DUMMY_TEA_GREEN_FILE = new File(TEST_DIR, "resource-dummy-tea-green.xml");
    private static final String RESOURCE_DUMMY_TEA_GREEN_OID = "10000000-0000-0000-0000-00000000c404";
    private static final String RESOURCE_DUMMY_TEA_GREEN_NAME = "tea-green";

    private static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";

    private static final String ACCOUNT_LEELOO_USERNAME = "leeloo";
    private static final String ACCOUNT_LEELOO_FULL_NAME_MULTIPASS = "Leeloo Dallas Multipass";
    private static final String ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI = "Leeloominaï Lekatariba Lamina-Tchaï Ekbat De Sebat";
    private static final String ACCOUNT_LEELOO_PROOF_STRANGE = "Hereby and hèrěnow\nThis is a multi-line claim\nwith a sôme of špecial chäracters\nAnd even some CRLF file endings\r\nLike this\r\nAnd to be completely nuts, even some LFRC\n\rThis does not really proves anything\n   It is just trying to reproduce the problem\nIn addition to be quite long\nand ugly\nLorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\nUt enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\nDuis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.\nExcepteur sint occaecat cupidatat non proident,\nsunt in culpa qui officia deserunt mollit anim id est laborum.\nAnd so on …";

    private static final String ACCOUNT_RISKY_USERNAME = "risky";
    private static final String ACCOUNT_GDPR_USERNAME = "gdpr";

    private static final File TASK_LIVE_SYNC_DUMMY_TEA_GREEN_FILE = new File(TEST_DIR, "task-dumy-tea-green-livesync.xml");
    private static final String TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID = "10000000-0000-0000-5555-55550000c404";

    private static final String LOCKER_BIG_SECRET = "BIG secret";

    private static final ItemName DATA_PROTECTION = new ItemName(NS_PIRACY, "dataProtection");

    private static final ItemName RISK_VECTOR = new ItemName(NS_PIRACY, "riskVector");
    private static final ItemName RISK = new ItemName(NS_PIRACY, "risk");
    private static final ItemName VALUE = new ItemName(NS_PIRACY, "value");

    private static final String DUMMY_ACCOUNT_ATTRIBUTE_CONTROLLER_NAME = "controllerName";

    private ProtectedStringType mancombLocker;
    private String userLeelooOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        initDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME, RESOURCE_DUMMY_TEA_GREEN_FILE, RESOURCE_DUMMY_TEA_GREEN_OID,
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
                    controller.setSyncStyle(DummySyncStyle.SMART);
                },
                initTask, initResult);
    }

    @Test
    public void test010SanitySchema() throws Exception {
        // GIVEN
        Task task = getTestTask();

        /// WHEN
        when();
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_TEA_GREEN_OID, task);

        // THEN
        then();
        TestUtil.assertSuccess(testResult);

        ResourceType resourceType = getDummyResourceType(RESOURCE_DUMMY_TEA_GREEN_NAME);
        ResourceSchema returnedSchema = RefinedResourceSchemaImpl.getResourceSchema(resourceType, prismContext);
        display("Parsed resource schema (tea-green)", returnedSchema);
        ObjectClassComplexTypeDefinition accountDef = getDummyResourceController(RESOURCE_DUMMY_TEA_GREEN_NAME)
                .assertDummyResourceSchemaSanityExtended(returnedSchema, resourceType, false,
                        DummyResourceContoller.PIRATE_SCHEMA_NUMBER_OF_DEFINITIONS + 4); // MID-5197

        ResourceAttributeDefinition<ProtectedStringType> lockerDef = accountDef.findAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_LOCKER_NAME);
        assertNotNull("No locker attribute definition", lockerDef);
        assertEquals("Wrong locker attribute definition type", ProtectedStringType.COMPLEX_TYPE, lockerDef.getTypeName());
    }

    @Test
    public void test100ImportLiveSyncTaskDummyTeaGreen() throws Exception {
        when();
        importSyncTask();

        then();
        waitForSyncTaskStart();
    }

    @Test
    public void test110AddDummyTeaGreenAccountMancomb() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Preconditions
        //assertUsers(5);

        DummyAccount account = new DummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_LOCKER_NAME, LOCKER_BIG_SECRET); // MID-5197
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "water");

        /// WHEN
        when();

        getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).addAccount(account);

        waitForSyncTaskNextRun();

        // THEN
        then();

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_TEA_GREEN_NAME));
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_TEA_GREEN_OID,
                accountMancomb.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED, null);

        UserAsserter<Void> mancombUserAsserter = assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        mancombLocker = mancombUserAsserter
                .links()
                .single()
                .assertOid(accountMancomb.getOid())
                .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .extension()
                .property(PIRACY_LOCKER)
                .singleValue()
                .protectedString()
                .assertIsEncrypted()
                .assertCompareCleartext(LOCKER_BIG_SECRET)
                .getProtectedString();

        assertJpegPhoto(UserType.class, mancombUserAsserter.getOid(), "water".getBytes(StandardCharsets.UTF_8), result);
//        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);
    }

    /**
     * MID-5912
     */
    @Test
    public void test120ModifyMancombPhotoSource() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        /// WHEN
        when();

        DummyAccount account = getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME)
                .getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("No mancomb account", account);
        account.removeAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "water");
        account.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");

        waitForSyncTaskNextRun();

        // THEN
        then();

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_TEA_GREEN_NAME));
        display("Account mancomb", accountMancomb);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);
        assertJpegPhoto(UserType.class, userMancomb.getOid(), "rum".getBytes(StandardCharsets.UTF_8), result);

        notificationManager.setDisabled(true);
    }

    /**
     * MID-5912 (reconcile without livesync task)
     */
    @Test
    public void test130ModifyMancombPhotoSourceAndReconcile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        /// WHEN
        when();

        // stop the task to avoid interference with the reconciliations
        suspendTask(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID);

        DummyAccount account = getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME)
                .getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("No mancomb account", account);
        account.removeAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");
        account.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "beer");

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);

        reconcileUser(userMancomb.getOid(), task, result);

        // THEN
        then();

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_TEA_GREEN_NAME));
        display("Account mancomb", accountMancomb);

        assertJpegPhoto(UserType.class, userMancomb.getOid(), "beer".getBytes(StandardCharsets.UTF_8), result);

        notificationManager.setDisabled(true);
    }

    /**
     * MID-5912 (changing the photo directly on service object; with reconcile)
     */
    @Test
    public void test140ModifyMancombPhotoInRepo() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        /// WHEN
        when();

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_JPEG_PHOTO).replaceRealValues(singleton("cherry".getBytes(StandardCharsets.UTF_8)))
                .asObjectDelta(userMancomb.getOid());
        executeChanges(delta, ModelExecuteOptions.createReconcile(), task, result);

        // THEN
        then();

        assertSuccess(result);

        PrismObject<UserType> userMancombAfter = repositoryService.getObject(UserType.class, userMancomb.getOid(),
                schemaHelper.getOperationOptionsBuilder().retrieve().build(), result);
        display("user mancomb after", userMancombAfter);

        //assertJpegPhoto(UserType.class, userMancomb.getOid(), "beer".getBytes(StandardCharsets.UTF_8), result);

        notificationManager.setDisabled(true);
    }

    /**
     * MID-5197
     */
    @Test
    public void test150UserReconcile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Preconditions
        //assertUsers(5);

        /// WHEN
        when();

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);

        reconcileUser(userMancomb.getOid(), task, result);

        // THEN
        then();

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_TEA_GREEN_NAME));
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_TEA_GREEN_OID,
                accountMancomb.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED, null);

        assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME)
                .links()
                .single()
                .assertOid(accountMancomb.getOid())
                .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
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

//        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
    }

    @Test
    public void test300DeleteDummyTeaGreenAccountMancomb() throws Exception {
        when();
        getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).deleteAccountByName(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        displayValue("Dummy (tea green) resource", getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).debugDump());

        // Make sure we have steady state
        resumeTaskAndWaitForNextFinish(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID, false, 20000);
        waitForSyncTaskNextRun();

        // THEN
        then();

        assertNoDummyAccount(RESOURCE_DUMMY_TEA_GREEN_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME);

        assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME)
                .assertFullName("Mancomb Seepgood")
                .links()
                .single()
                .resolveTarget()
                .assertTombstone()
                .assertSynchronizationSituation(SynchronizationSituationType.DELETED);

//        assertUsers(7 + getNumberOfExtraDummyUsers());

        // notifications
        notificationManager.setDisabled(true);
    }

    // Remove livesync task so it won't get into the way for next tests
    @Test
    public void test399DeleteDummyTeaGreenAccountMancomb() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        /// WHEN
        when();
        deleteObject(TaskType.class, TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID, task, result);

        // THEN
        then();

        assertNoObject(TaskType.class, TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID);
    }

    @Test
    public void test400AddUserLeeloo() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Preconditions
        //assertUsers(5);

        DummyAccount account = new DummyAccount(ACCOUNT_LEELOO_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_LEELOO_FULL_NAME_MULTIPASS);
        getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).addAccount(account);

        /// WHEN
        when();

        modelService.importFromResource(RESOURCE_DUMMY_TEA_GREEN_OID, new QName(MidPointConstants.NS_RI, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), task, result);

        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);
        waitForTaskFinish(task, true);

        // THEN
        then();

        userLeelooOid = assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_MULTIPASS)
                .links()
                .single()
                .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .getOid();

        display("Audit", dummyAuditService);
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        /// WHEN
        when();

        recomputeUser(userLeelooOid, task, result);

        // THEN
        then();

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_MULTIPASS)
                .links()
                .single()
                .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(0);
    }

    /**
     * Nothing has changed in the account. Expect no changes in user.
     * MID-5314
     */
    @Test
    public void test404UserLeelooReconcile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        /// WHEN
        when();

        reconcileUser(userLeelooOid, task, result);

        // THEN
        then();

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_MULTIPASS)
                .links()
                .single()
                .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).getAccountByUsername(ACCOUNT_LEELOO_USERNAME);
        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI);

        dummyAuditService.clear();

        /// WHEN
        when();

        reconcileUser(userLeelooOid, task, result);

        // THEN
        then();

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
                .links()
                .single()
                .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        /// WHEN
        when();

        recomputeUser(userLeelooOid, task, result);

        // THEN
        then();

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
                .links()
                .single()
                .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(0);
    }

    /**
     * Nothing has changed in the account. Expect no changes in user.
     * MID-5314
     */
    @Test
    public void test414UserLeeloominaiReconcile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        /// WHEN
        when();

        reconcileUser(userLeelooOid, task, result);

        // THEN
        then();

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
                .links()
                .single()
                .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).getAccountByUsername(ACCOUNT_LEELOO_USERNAME);
        account.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_PROOF_NAME, ACCOUNT_LEELOO_PROOF_STRANGE);

        dummyAuditService.clear();

        /// WHEN
        when();

        reconcileUser(userLeelooOid, task, result);

        // THEN
        then();

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
                .assertDescription(ACCOUNT_LEELOO_PROOF_STRANGE)
                .links()
                .single()
                .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        /// WHEN
        when();

        reconcileUser(userLeelooOid, task, result);

        // THEN
        then();

        assertUserAfterByUsername(ACCOUNT_LEELOO_USERNAME)
                .assertFullName(ACCOUNT_LEELOO_FULL_NAME_LEELOOMINAI)
                .assertDescription(ACCOUNT_LEELOO_PROOF_STRANGE)
                .links()
                .single()
                .end()
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test500UserRiskVector() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        /*
         * State before: (death, 1); (treason, 2)
         */
        UserType user = new UserType(prismContext)
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

        addObject(user.asPrismObject(), task, result);

        DummyAccount account = new DummyAccount(ACCOUNT_RISKY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Risky Risk");
        account.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_TREASON_RISK_NAME, 999);
        getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).addAccount(account);

        when();

        modelService.importFromResource(RESOURCE_DUMMY_TEA_GREEN_OID, new QName(MidPointConstants.NS_RI, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), task, result);
        waitForTaskFinish(task, true);

        then();

        assertSuccess(task.getResult());

        /*
         * Expected state after: (death, 1); (treason, 999)
         *
         * The former was kept intact because of set specification.
         * The latter was taken from treasonRisk value from dummy account (and overwritten because of set specification).
         */
        assertUserAfterByUsername(ACCOUNT_RISKY_USERNAME)
                .assertExtensionItems(1)
                .extensionContainer(RISK_VECTOR)
                    .assertSize(2)
                    .value(ValueSelector.itemEquals(RISK, "treason"))
                        .assertPropertyEquals(VALUE, 999)
                        .end()
                    .value(ValueSelector.itemEquals(RISK, "death"))
                        .assertPropertyEquals(VALUE, 1);
    }

    /**
     * MID-6129
     */
    @Test
    public void test510UserDataProtection() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        UserType user = new UserType(prismContext)
                .name(ACCOUNT_GDPR_USERNAME);
        DataProtectionType protection = new DataProtectionType(prismContext)
                .controllerName("controller")
                .controllerContact("controller@evolveum.com");
        PrismContainerDefinition<DataProtectionType> protectionDef =
                user.asPrismObject().getDefinition().findContainerDefinition(ItemPath.create(UserType.F_EXTENSION, DATA_PROTECTION));
        PrismContainer<DataProtectionType> protectionContainer = protectionDef.instantiate();
        //noinspection unchecked
        protectionContainer.add(protection.asPrismContainerValue());
        user.asPrismObject().addExtensionItem(protectionContainer);

        addObject(user.asPrismObject(), task, result);

        DummyAccount account = new DummyAccount(ACCOUNT_GDPR_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "GDPR");
        account.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_CONTROLLER_NAME, "new-controller");
        getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).addAccount(account);

        when();

        modelService.importFromResource(RESOURCE_DUMMY_TEA_GREEN_OID, new QName(MidPointConstants.NS_RI, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), task, result);
        waitForTaskFinish(task, true);

        then();

        assertSuccess(task.getResult());

        assertUserAfterByUsername(ACCOUNT_GDPR_USERNAME)
                .assertExtensionItems(1)
                .extensionContainer(DATA_PROTECTION)
                    .assertSize(1)
                    .value(0)
                        .assertPropertyEquals(DataProtectionType.F_CONTROLLER_NAME, "new-controller")
                        .assertPropertyEquals(DataProtectionType.F_CONTROLLER_CONTACT, "new-controller@evolveum.com");

    }

    protected void importSyncTask() throws FileNotFoundException {
        importObjectFromFile(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_FILE);
    }

    private void waitForSyncTaskStart() throws Exception {
        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID, false, 10000);
    }

    private void waitForSyncTaskNextRun() throws Exception {
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID, false, 10000);
    }
}
