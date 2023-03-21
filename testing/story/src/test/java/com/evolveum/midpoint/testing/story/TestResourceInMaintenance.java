/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.MiscSchemaUtil;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

import com.evolveum.midpoint.test.TestResource;

import com.evolveum.midpoint.util.exception.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author Martin Lizner
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestResourceInMaintenance extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "resource-in-maintenance");

    private static final File RESOURCE_CSV_FILE = new File(TEST_DIR, "csv-resource1.xml");
    private static final File RESOURCE_CSV_CONTENT_FILE = new File(TEST_DIR, "data-resource1.csv");
    private static final File ROLE1_FILE = new File(TEST_DIR, "role1.xml");

    private static String sourceFilePath;

    private static final File SHADOW_FILE = new File(TEST_DIR, "shadow-user1.xml");
    private static final File USERS_FILE = new File(TEST_DIR, "users.xml");

    private static final String RESOURCE_OID = "25dd0010-5115-4ac0-960f-4889d1b960ff";
    private static final String SHADOW_OID = "c4071f2e-3f8d-4301-9027-c57033c702ff";
    private static final String USER1_OID = "cdc33185-c817-4be7-8158-8f338824cdff";
    private static final String USER2_OID = "cdc33185-c817-4be7-8158-8f3388241234";
    private static final String USER3_OID = "cdc33185-c817-4be7-8158-8f3388245678";
    private static final String USER4_OID = "cdc33185-c817-4be7-8158-8f33882456bb";
    private static final String ROLE1_OID = "6ec32c86-66d4-4101-a25f-931db8d1e999";

    private static final QName CSV_ATTRIBUTE_DESC = new QName(NS_RI, "description");
    private static final QName CSV_ATTRIBUTE_FULLNAME = new QName(NS_RI, "fullname");
    private static final QName CSV_ATTRIBUTE_USERNAME = new QName(NS_RI, "username");

    private static final TestResource<TaskType> TASK_RECONCILE_CSV = new TestResource<>(TEST_DIR, "task-reconcile-csv.xml", "9b17f4f6-3085-4210-b160-883c0e842780");
    private static final TestResource<TaskType> TASK_REFRESH = new TestResource<>(TEST_DIR, "task-refresh.xml", "f996d5c4-0cc8-4544-9410-8154a89080fd");

    private static final String NS_RESOURCE_CSV = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.polygon.connector-csv/com.evolveum.polygon.connector.csv.CsvConnector";

    @Autowired
    private MidpointConfiguration midPointConfig;

    @Override
    protected File getSystemConfigurationFile() {
        return super.getSystemConfigurationFile();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        String home = midPointConfig.getMidpointHome();
        File resourceDir = new File(home, "resource-in-maintenance");
        //noinspection ResultOfMethodCallIgnored
        resourceDir.mkdir();

        File destinationFile = new File(resourceDir, "data-resource1.csv");
        ClassPathUtil.copyFile(new FileInputStream(RESOURCE_CSV_CONTENT_FILE), "data-resource1.csv", destinationFile);

        if (!destinationFile.exists()) {
            throw new SystemException("Source file for CSV resource was not created");
        }

        sourceFilePath = destinationFile.getAbsolutePath();

        super.initSystem(initTask, initResult);

        importObjectFromFile(RESOURCE_CSV_FILE);

        prepareCsvResource(initTask, initResult);

        addObject(TASK_RECONCILE_CSV, initTask, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        SystemConfigurationType systemConfiguration = getSystemConfiguration();
        assertNotNull("No system configuration", systemConfiguration);
        display("System config", systemConfiguration);

        importObjectFromFile(SHADOW_FILE);
        importObjectFromFile(USERS_FILE);
        importObjectFromFile(ROLE1_FILE);

        turnMaintenanceModeOn(result);
    }

    private void prepareCsvResource(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException, SecurityViolationException,
            PolicyViolationException, ObjectAlreadyExistsException {
        Object[] newRealValue = { sourceFilePath };

        ObjectDelta<ResourceType> objectDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(ResourceType.class, RESOURCE_OID, ItemPath.create(ResourceType.F_CONNECTOR_CONFIGURATION,
                        SchemaConstants.ICF_CONFIGURATION_PROPERTIES, new QName(NS_RESOURCE_CSV, "filePath")),
                        newRealValue);
        provisioningService.applyDefinition(objectDelta, task, result);
        provisioningService.modifyObject(ResourceType.class, objectDelta.getOid(), objectDelta.getModifications(), null, null, task, result);

        OperationResult csvTestResult = modelService.testResource(RESOURCE_OID, task, result);
        TestUtil.assertSuccess("CSV resource test result", csvTestResult);
    }

    @Test
    public void test010RecomputeUserWithoutChange() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modelService.recompute(UserType.class, USER1_OID, executeOptions().reconcile(), task, result);
        result.computeStatus();
        display(result);

        TestUtil.assertSuccess(result);

        // no change was requested = no pending operation is saved:
        assertRepoShadow(SHADOW_OID)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default")
                .assertIsExists()
                .assertResource(RESOURCE_OID)
                .pendingOperations()
                    .assertNone();
    }

    @Test
    public void test020UpdateAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        modifyUserReplace(USER1_OID, UserType.F_DESCRIPTION, task, result, "jedi");

        then();
        result.computeStatus();
        display(result);

        TestUtil.assertInProgress("resource in the maintenance pending delta", result);

        assertRepoShadow(SHADOW_OID)
                .display(getTestNameShort() + " Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsExists()
                .assertNotDead()
                .pendingOperations()
                    .modifyOperation()
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                    .assertAttemptNumber(1)
                    .delta()
                    .display()
                    .assertModify()
                    .end()
                .end();

        // now let's add second update delta:

        when("second update");
        OperationResult result2 = createOperationResult();
        modifyUserReplace(USER1_OID, UserType.F_DESCRIPTION, task, result2, "jedi knight");

        then("second update");
        result2.computeStatus();
        display(result2);

        TestUtil.assertInProgress("resource in the maintenance pending delta", result2);

        PrismObject<ShadowType> shadowAfter = getShadowModel(SHADOW_OID, null, false);
        //check that two pending update deltas are in progress:
        assertTwoPendingOperations(shadowAfter, PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS);
    }

    @Test
    public void test030ApplyPendingModify() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        turnMaintenanceModeOff(result);

        then();
        // Check description value hasn't changed yet:
        PrismObject<ShadowType> shadowBefore = getShadowModel(SHADOW_OID);
        ShadowAsserter.forShadow(shadowBefore)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default")
                .attributes()
                    .assertHasPrimaryIdentifier()
                    .assertValue(CSV_ATTRIBUTE_DESC, "sith")
                .end()
                .assertResource(RESOURCE_OID);

        // Apply pending deltas:
        when("recompute user");
        modelService.recompute(UserType.class, USER1_OID, executeOptions().reconcile(), task, result);

        then("recompute user");
        assertSuccess(result);

        // Now check description value in CSV file has changed according to pending operation:
        PrismObject<ShadowType> shadowAfter = getShadowModel(SHADOW_OID);
        ShadowAsserter.forShadow(shadowAfter)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default")
                .attributes()
                    .assertHasPrimaryIdentifier()
                    .assertValue(CSV_ATTRIBUTE_DESC, "jedi knight")
                .end()
                .assertResource(RESOURCE_OID);

        assertTwoPendingOperations(shadowAfter, PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS);
    }

    @Test
    public void test040CreateNewAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        turnMaintenanceModeOn(result);

        when();
        assignAccountToUser(USER2_OID, RESOURCE_OID, "default", task, result);

        then();
        result.computeStatus();
        display(result);

        TestUtil.assertInProgress("resource in the maintenance pending delta", result);

        String newShadowOid = getLiveLinkRefOid(USER2_OID, RESOURCE_OID);
        assertNotNull(newShadowOid);

        assertRepoShadow(newShadowOid)
                .display(getTestNameShort() + " Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .assertNotDead()
                .assertNoLegacyConsistency()
                .attributes()
                    .assertAttributes(CSV_ATTRIBUTE_USERNAME) // checks main attributes section, not attributes in the pending delta
                .end()
                .pendingOperations()
                    .singleOperation()
                    .display()
                    .assertType(PendingOperationTypeType.RETRY)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                    .assertAttemptNumber(1)
                    .delta()
                    .display()
                    .assertAdd();

        // lets try recompute just for fun, mp should do nothing and report success:
        when("recompute");
        OperationResult result2 = createOperationResult();
        modelService.recompute(UserType.class, USER2_OID, executeOptions().reconcile(), task, result2);

        then("recompute");
        assertSuccess(result2, 4); // Deep inside is an exception about maintenance

        int noOfLinks = getUser(USER2_OID).asObjectable().getLinkRef().size();
        assertEquals(noOfLinks, 1); // check that consequent recompute hasn't created second shadow

        // double check that nothing changed in the shadow after recompute:
        assertRepoShadow(newShadowOid)
                .display(getTestNameShort() + " Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .assertNotDead()
                .pendingOperations()
                    .singleOperation()
                        .display()
                        .assertType(PendingOperationTypeType.RETRY)
                        .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                        .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                        .assertAttemptNumber(1)
                        .delta()
                        .display()
                        .assertAdd();
    }

    @Test
    public void test050UpdateAfterCreate() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        // modify pending-created account
        modifyUserReplace(USER2_OID, UserType.F_FULL_NAME, task, result, new PolyString("Artoo-Deetoo"));

        then();
        result.computeStatus();
        display(result);
        TestUtil.assertInProgress("resource in the maintenance pending delta", result);

        when("recompute");
        // lets try recompute just for fun, mp should do nothing and report success:
        OperationResult result2 = createOperationResult();
        modelService.recompute(UserType.class, USER2_OID, executeOptions().reconcile(), task, result2);
        result2.computeStatus();

        then("recompute");
        String checkShadow = getLiveLinkRefOid(USER2_OID, RESOURCE_OID);
        assertNotNull(checkShadow);

        assertRepoShadow(checkShadow)
                .display(getTestNameShort() + " Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .assertNotDead()
                .pendingOperations()
                .addOperation()
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                    .assertAttemptNumber(1)
                    .delta()
                    .display()
                    .assertAdd()
                    .end()
                .end()
                .modifyOperation()
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                    .assertAttemptNumber(1)
                    .delta()
                    .display()
                    .assertModify()
                    .end()
                .end();
    }

    @Test
    public void test060ApplyPendingCreate() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        turnMaintenanceModeOff(result);

        // Check shadow does not exist yet:
        String newShadowOid = getLiveLinkRefOid(USER2_OID, RESOURCE_OID);
        PrismObject<ShadowType> shadowBefore = getShadowModel(newShadowOid);
        ShadowAsserter.forShadow(shadowBefore)
                .assertIsNotExists()
                .end();

        when();
        // Apply pending deltas (create + update):
        modelService.recompute(UserType.class, USER2_OID, executeOptions().reconcile(), task, result);

        then();
        assertSuccess(result);

        // Now check shadow exists in CSV file
        PrismObject<ShadowType> shadowCreated = getShadowModel(newShadowOid);
        ShadowAsserter.forShadow(shadowCreated)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default")
                .assertIsExists()
                .assertNotDead().assertKind(ShadowKindType.ACCOUNT)
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertValue(CSV_ATTRIBUTE_FULLNAME, "Artoo-Deetoo")
                .end()
                .assertResource(RESOURCE_OID);

        assertTwoPendingOperations(shadowCreated, PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS);
    }

    @Test
    public void test070DeleteAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        turnMaintenanceModeOn(result);

        String shadowOid = getLiveLinkRefOid(USER2_OID, RESOURCE_OID);
        assertNotNull(shadowOid);

        when();
        unassignAccountFromUser(USER2_OID, RESOURCE_OID, "default", task, result);

        then();
        result.computeStatus();
        display(result);

        TestUtil.assertInProgress("resource in the maintenance pending delta", result);

        assertModelShadowNoFetch(shadowOid)
                .display("Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .attributes()
                .assertHasPrimaryIdentifier()
                .end()
                .pendingOperations()
                    .assertOperations(3) // 1x create + 1x update + 1x delete
                    .deleteOperation()
                        .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                        .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                        .assertAttemptNumber(1)
                        .delta()
                        .display()
                        .assertDelete()
                    .end()
                .end();

        when("recompute");
        // lets try recompute just for fun, mp should do nothing and report success:
        OperationResult result2 = createOperationResult();
        modelService.recompute(UserType.class, USER2_OID, executeOptions().reconcile(), task, result2);

        then("recompute");
        result2.computeStatus();

        assertModelShadowNoFetch(shadowOid)
                .display("Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .attributes()
                .assertHasPrimaryIdentifier()
                .end()
                .pendingOperations()
                .assertOperations(3)
                .end();

        TestUtil.assertSuccess(result2);
    }

    @Test
    public void test080ApplyPendingDelete() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        turnMaintenanceModeOff(result);

        // Check shadow still exists:
        String shadowOid = getLiveLinkRefOid(USER2_OID, RESOURCE_OID);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        ShadowAsserter.forShadow(shadow)
                .display("Shadow before delete")
                .assertIsExists()
                .end();

        when();
        // Apply pending delete delta:
        modelService.recompute(UserType.class, USER2_OID, executeOptions().reconcile(), task, result);

        then();
        assertSuccess(result);

        // Now check shadow does not exist in CSV file
        PrismObject<ShadowType> shadowDeleted = getShadowModel(shadowOid);
        ShadowAsserter.forShadow(shadowDeleted)
                .assertIsNotExists()
                .assertDead()
                .end();

        assertModelShadowNoFetch(shadowOid)
                .display("Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .pendingOperations()
                .assertOperations(3)
                    .deleteOperation()
                        .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                        .assertResultStatus(OperationResultStatusType.SUCCESS)
                        .assertAttemptNumber(2) // attempt increases after successful provisioning
                        .delta()
                        .display()
                        .assertDelete()
                    .end()
                .end();
    }

    @Test
    public void test090CreateAndDeleteAccountWithRole() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        turnMaintenanceModeOn(result);

        when();
        assignRole(USER3_OID, ROLE1_OID, task, result);

        then();
        result.computeStatus();
        display(result);

        TestUtil.assertInProgress("resource in the maintenance pending delta", result);

        String newShadowOid = getLiveLinkRefOid(USER3_OID, RESOURCE_OID);
        assertNotNull(newShadowOid);

        assertRepoShadow(newShadowOid)
                .display(getTestNameShort() + " Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .assertNotDead()
                .assertNoLegacyConsistency()
                .attributes()
                .assertAttributes(CSV_ATTRIBUTE_USERNAME) // checks main attributes section, not attributes in the pending delta
                .end()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                .assertAttemptNumber(1)
                .delta()
                .display()
                .assertAdd();

        when("recompute");
        // lets try recompute just for fun, mp should do nothing and report success:
        OperationResult result2 = createOperationResult();
        modelService.recompute(UserType.class, USER3_OID, executeOptions().reconcile(), task, result2);

        then("recompute");
        assertSuccess(result2);

        assertUser(USER3_OID, getTestNameShort())
                .assertLiveLinks(1); // check that consequent recompute hasn't created second shadow

        // double check that nothing changed in the shadow after recompute:
        assertRepoShadow(newShadowOid)
                .display(getTestNameShort() + " Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .assertNotDead()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                .assertAttemptNumber(1)
                .delta()
                .display()
                .assertAdd();

        when("unassign");
        // now delete account:
        OperationResult result3 = createOperationResult();
        unassignRole(USER3_OID, ROLE1_OID, task, result3);

        then("unassign");
        result3.computeStatus();
        TestUtil.assertInProgress("resource in the maintenance pending delta", result3);

        assertModelShadowNoFetch(newShadowOid)
                .display("Shadow after delete")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .attributes()
                .assertHasPrimaryIdentifier()
                .end()
                .pendingOperations()
                .assertOperations(2) // 1x create + 1x delete
                .deleteOperation()
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                .assertAttemptNumber(1)
                .delta()
                .display()
                .assertDelete()
                .end()
                .end();

        when("maintenance off");

        turnMaintenanceModeOff(result);

        // Apply pending create + delete delta:
        OperationResult result4 = createOperationResult();
        modelService.recompute(UserType.class, USER2_OID, executeOptions().reconcile(), task, result4);

        then("maintenance off");
        assertSuccess(result4);

        // Check that nothing really happened on the resource:
        PrismObject<ShadowType> shadowDeleted = getShadowModel(newShadowOid, null, false);
        ShadowAsserter.forShadow(shadowDeleted)
                .assertIsNotExists()
                .end();
    }

    @Test
    public void test100CreateAndDeleteAccountWithResourceAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        turnMaintenanceModeOn(result);

        when();
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER4_OID, RESOURCE_OID, "default", true);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);

        then();
        result.computeStatus();

        TestUtil.assertInProgress("resource in the maintenance pending delta", result);

        String newShadowOid = getLiveLinkRefOid(USER4_OID, RESOURCE_OID);
        assertNotNull(newShadowOid);

        assertRepoShadow(newShadowOid)
                .display(getTestNameShort() + " Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .assertNotDead()
                .assertNoLegacyConsistency()
                .attributes()
                .assertAttributes(CSV_ATTRIBUTE_USERNAME) // checks main attributes section, not attributes in the pending delta
                .end()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                .assertAttemptNumber(1)
                .delta()
                .display()
                .assertAdd();

        when("recompute");
        // lets try recompute just for fun, mp should do nothing and report success:
        OperationResult result2 = createOperationResult();
        modelService.recompute(UserType.class, USER4_OID, executeOptions().reconcile(), task, result2);

        then("recompute");
        assertSuccess(result2);

        assertUser(USER4_OID, getTestNameShort())
                .assertLiveLinks(1); // check that consequent recompute hasn't created second shadow
    }

    /**
     * 1. Creates a user with linked (not assigned) account.
     * 2. Deletes the account in maintenance mode.
     * 3. Turns maintenance mode off and reconciles the resource.
     * 4. The user->account link should be gone.
     *
     * MID-6862
     */
    @Test
    public void test110DeleteLinkedAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        turnMaintenanceModeOff(result);

        UserWithAccount userWithAccount = createUserWithAccount("test110", task, result);
        UserType user = userWithAccount.getUser();

        turnMaintenanceModeOn(result);

        when("delete account");

        ObjectDelta<UserType> delta = userWithAccount.createDeleteAccountDelta();
        executeChanges(delta, null, task, result);

        then("delete account");

        userWithAccount.assertAccountDeletionPending();

        when("reconcile in normal mode");

        turnMaintenanceModeOff(result);
        rerunTask(TASK_RECONCILE_CSV.oid);

        then("reconcile in normal mode");

        assertUserAfter(user.getOid())
                .links()
                    .assertNoLiveLinks();
    }

    /**
     * The same as test110 but the reconciliation runs in maintenance mode.
     *
     * 1. Creates a user with linked (not assigned) account.
     * 2. Deletes the account in maintenance mode.
     * 3. Reconciles the resource. (This is different from test110.)
     *
     * MID-6946
     */
    @Test
    public void test115DeleteLinkedAccountAndReconcileInMaintenance() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        turnMaintenanceModeOff(result);

        UserWithAccount userWithAccount = createUserWithAccount("test115", task, result);

        turnMaintenanceModeOn(result);

        when("delete account");

        ObjectDelta<UserType> delta = userWithAccount.createDeleteAccountDelta();
        executeChanges(delta, null, task, result);

        then("delete account");

        userWithAccount.assertAccountDeletionPending();

        when("reconcile in maintenance mode");

        Task taskAfter = rerunTask(TASK_RECONCILE_CSV.oid);

        then("reconcile in maintenance mode");

        assertTask(taskAfter, "reconciliation after run")
                .display()
                .assertWarning()
                .assertExecutionState(TaskExecutionStateType.SUSPENDED); // This is a reaction to TEMPORARY_ERROR status.
    }

    /**
     * Tests MID-6892: Shadow refresh after updates were made during maintenance mode.
     */
    @Test
    public void test120UpdateAndRefresh() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        turnMaintenanceModeOff(result);

        UserType user = new UserType()
                .name("test120")
                .fullName("Jim Test120")
                .beginAssignment()
                    .beginConstruction()
                        .resourceRef(RESOURCE_OID, ResourceType.COMPLEX_TYPE)
                    .<AssignmentType>end()
                .end();

        String userOid = addObject(user, task, result);

        when();

        turnMaintenanceModeOn(result);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_NAME).replace(PolyString.fromOrig("test120-changed"))
                .asObjectDelta(userOid);

        executeChanges(delta, null, task, result);

        turnMaintenanceModeOff(result);

        clockForward("PT1H");

        addTask(TASK_REFRESH, result);
        Task taskAfter = waitForTaskFinish(TASK_REFRESH.oid, false);

        then();

        assertTask(taskAfter, "after")
                .display();
    }

    private void turnMaintenanceModeOn(OperationResult result) throws Exception {
        turnMaintenanceModeOn(RESOURCE_OID, result);
    }

    private void turnMaintenanceModeOff(OperationResult result) throws Exception {
        turnMaintenanceModeOff(RESOURCE_OID, result);
    }

    private void assertTwoPendingOperations (PrismObject<ShadowType> shadow,
            PendingOperationExecutionStatusType expectedExecutionStatus, OperationResultStatusType expectedResultStatus) {

        assertPendingOperationDeltas(shadow, 2);

        assertPendingOperation(shadow, shadow.asObjectable().getPendingOperation().get(0),
                null, null, expectedExecutionStatus, expectedResultStatus, null, null);

        assertPendingOperation(shadow, shadow.asObjectable().getPendingOperation().get(1),
                null, null, expectedExecutionStatus, expectedResultStatus, null, null);
    }


    private UserWithAccount createUserWithAccount(String name, Task task, OperationResult result) throws CommonException {
        return new UserWithAccount().invoke(name, task, result);
    }

    private class UserWithAccount {
        private ShadowType shadow;
        private UserType user;

        public ShadowType getShadow() {
            return shadow;
        }

        public UserType getUser() {
            return user;
        }

        UserWithAccount invoke(String name, Task task, OperationResult result) throws CommonException {
            shadow = new ShadowType()
                    .resourceRef(RESOURCE_OID, ResourceType.COMPLEX_TYPE)
                    .objectClass(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                    .kind(ShadowKindType.ACCOUNT)
                    .intent(SchemaConstants.INTENT_DEFAULT);

            user = new UserType()
                    .name(name)
                    .linkRef(ObjectTypeUtil.createObjectRefWithFullObject(shadow.clone()));

            String userOid = addObject(user, task, result);

            user = assertUser(userOid, "after creation")
                    .display()
                    .links()
                        .assertLiveLinks(1)
                        .end()
                    .getObject().asObjectable();

            String shadowOid = getLiveLinkRefOid(user.asPrismObject(), RESOURCE_OID);
            shadow.setOid(shadowOid);

            return this;
        }

        ObjectDelta<UserType> createDeleteAccountDelta() throws SchemaException {
            return deltaFor(UserType.class)
                    .item(UserType.F_LINK_REF).delete(ObjectTypeUtil.createObjectRefWithFullObject(shadow.clone()))
                    .asObjectDelta(user.getOid());
        }

        void assertAccountDeletionPending() throws CommonException {
            assertUser(user.getOid(), "after deletion")
                    .display()
                    .links()
                        .assertLiveLinks(1)
                        .singleAny()
                            .resolveTarget()
                            .display()
                                .pendingOperations()
                                    .assertOperations(1)
                                    .deleteOperation();
        }
    }
}
