/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

/*
 * @author Martin Lizner
 */

package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.MiscSchemaUtil;

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
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

    private static final QName CSV_ATTRIBUTE_DESC = new QName(MidPointConstants.NS_RI, "description");
    private static final QName CSV_ATTRIBUTE_FULLNAME = new QName(MidPointConstants.NS_RI, "fullname");
    private static final QName CSV_ATTRIBUTE_USERNAME = new QName(MidPointConstants.NS_RI, "username");

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
        resourceDir.mkdir();

        File desticationFile = new File(resourceDir, "data-resource1.csv");
        ClassPathUtil.copyFile(new FileInputStream(RESOURCE_CSV_CONTENT_FILE), "data-resource1.csv", desticationFile);

        if (!desticationFile.exists()) {
            throw new SystemException("Source file for CSV resource was not created");
        }

        sourceFilePath = desticationFile.getAbsolutePath();

        super.initSystem(initTask, initResult);

        importObjectFromFile(RESOURCE_CSV_FILE);
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        Object[] newRealValue = { sourceFilePath };

        ObjectDelta<ResourceType> objectDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(ResourceType.class, RESOURCE_OID, ItemPath.create(ResourceType.F_CONNECTOR_CONFIGURATION,
                        SchemaConstants.ICF_CONFIGURATION_PROPERTIES, new QName(NS_RESOURCE_CSV, "filePath")),
                        newRealValue);
        provisioningService.applyDefinition(objectDelta, task, result);
        provisioningService.modifyObject(ResourceType.class, objectDelta.getOid(), objectDelta.getModifications(), null, null, task, result);

        OperationResult csvTestResult = modelService.testResource(RESOURCE_OID, task);
        TestUtil.assertSuccess("CSV resource test result", csvTestResult);

        SystemConfigurationType systemConfiguration = getSystemConfiguration();
        assertNotNull("No system configuration", systemConfiguration);
        display("System config", systemConfiguration);

        importObjectFromFile(SHADOW_FILE);
        importObjectFromFile(USERS_FILE);
        importObjectFromFile(ROLE1_FILE);

        // Turn maintenance mode ON:
        switchResourceMaintenance(AdministrativeAvailabilityStatusType.MAINTENANCE, task, result);
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
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER1_OID, UserType.F_DESCRIPTION, task, result, "jedi");

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

        //not lets add second update delta:
        OperationResult result2 = createOperationResult();
        modifyUserReplace(USER1_OID, UserType.F_DESCRIPTION, task, result2, "jedi knight");

        result2.computeStatus();
        display(result2);

        TestUtil.assertInProgress("resource in the maintenance pending delta", result2);

        PrismObject<ShadowType> shadowAfter = getShadowModel(SHADOW_OID);
        //check that two pending update deltas are in progress:
        assertTwoPendingOperations(shadowAfter, PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS);
    }

    @Test
    public void test030ApplyPendingModify() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Cancel maintenance mode:
        switchResourceMaintenance(AdministrativeAvailabilityStatusType.OPERATIONAL, task, result);

        // Check description value hasnt changed yet:
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
        modelService.recompute(UserType.class, USER1_OID, executeOptions().reconcile(), task, result);
        result.computeStatus();

        TestUtil.assertSuccess(result);

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
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Turn maintenance mode ON:
        switchResourceMaintenance(AdministrativeAvailabilityStatusType.MAINTENANCE, task, result);

        assignAccountToUser(USER2_OID, RESOURCE_OID, "default", task, result);

        result.computeStatus();
        display(result);

        TestUtil.assertInProgress("resource in the maintenance pending delta", result);

        String newShadowOid = getLinkRefOid(USER2_OID, RESOURCE_OID);
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
        OperationResult result2 = createOperationResult();
        modelService.recompute(UserType.class, USER2_OID, executeOptions().reconcile(), task, result2);
        result2.computeStatus();

        TestUtil.assertSuccess(result2);

        int noOfLinks = getUser(USER2_OID).asObjectable().getLinkRef().size();
        assertEquals(noOfLinks, 1); // check that consequent recompute hasnt created second shadow

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
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // modify pending-created account
        modifyUserReplace(USER2_OID, UserType.F_FULL_NAME, task, result, new PolyString("Artoo-Deetoo"));

        result.computeStatus();
        display(result);
        TestUtil.assertInProgress("resource in the maintenance pending delta", result);

        // lets try recompute just for fun, mp should do nothing and report success:
        OperationResult result2 = createOperationResult();
        modelService.recompute(UserType.class, USER2_OID, executeOptions().reconcile(), task, result2);
        result2.computeStatus();

        String checkShadow = getLinkRefOid(USER2_OID, RESOURCE_OID);
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
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Cancel maintenance mode:
        switchResourceMaintenance(AdministrativeAvailabilityStatusType.OPERATIONAL, task, result);

        // Check shadow does not exist yet:
        String newShadowOid = getLinkRefOid(USER2_OID, RESOURCE_OID);
        PrismObject<ShadowType> shadowBefore = getShadowModel(newShadowOid);
        ShadowAsserter.forShadow(shadowBefore)
                .assertIsNotExists()
                .end();

        // Apply pending deltas (create + update):
        modelService.recompute(UserType.class, USER2_OID, executeOptions().reconcile(), task, result);
        result.computeStatus();

        TestUtil.assertSuccess(result);

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
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Turn maintenance mode ON:
        switchResourceMaintenance(AdministrativeAvailabilityStatusType.MAINTENANCE, task, result);

        String shadowOid = getLinkRefOid(USER2_OID, RESOURCE_OID);
        assertNotNull(shadowOid);
        unassignAccountFromUser(USER2_OID, RESOURCE_OID, "default", task, result);

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

        // lets try recompute just for fun, mp should do nothing and report success:
        OperationResult result2 = createOperationResult();
        modelService.recompute(UserType.class, USER2_OID, executeOptions().reconcile(), task, result2);
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
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Cancel maintenance mode:
        switchResourceMaintenance(AdministrativeAvailabilityStatusType.OPERATIONAL, task, result);

        // Check shadow still exists:
        String shadowOid = getLinkRefOid(USER2_OID, RESOURCE_OID);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        ShadowAsserter.forShadow(shadow)
                .display("Shadow before delete")
                .assertIsExists()
                .end();

        // Apply pending delete delta:
        modelService.recompute(UserType.class, USER2_OID, executeOptions().reconcile(), task, result);
        result.computeStatus();

        TestUtil.assertSuccess(result);

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
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Turn maintenance mode ON:
        switchResourceMaintenance(AdministrativeAvailabilityStatusType.MAINTENANCE, task, result);

        assignRole(USER3_OID, ROLE1_OID, task, result);

        result.computeStatus();
        display(result);

        TestUtil.assertInProgress("resource in the maintenance pending delta", result);

        String newShadowOid = getLinkRefOid(USER3_OID, RESOURCE_OID);
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
        OperationResult result2 = createOperationResult();
        modelService.recompute(UserType.class, USER3_OID, executeOptions().reconcile(), task, result2);
        result2.computeStatus();

        TestUtil.assertSuccess(result2);

        assertUser(USER3_OID, getTestNameShort()).assertLinks(1); // check that consequent recompute hasnt created second shadow

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

        // now delete account:
        OperationResult result3 = createOperationResult();
        unassignRole(USER3_OID, ROLE1_OID, task, result3);
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

        // Cancel maintenance mode:
        switchResourceMaintenance(AdministrativeAvailabilityStatusType.OPERATIONAL, task, result);

        // Apply pending create + delete delta:
        OperationResult result4 = createOperationResult();
        modelService.recompute(UserType.class, USER2_OID, executeOptions().reconcile(), task, result4);
        result4.computeStatus();
        TestUtil.assertSuccess(result4);

        // Check that nothing really happened on the resource:
        PrismObject<ShadowType> shadowDeleted = getShadowModel(newShadowOid);
        ShadowAsserter.forShadow(shadowDeleted)
                .assertIsNotExists()
                .end();
    }

    @Test
    public void test100CreateAndDeleteAccountWithResourceAssignment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Turn maintenance mode ON:
        switchResourceMaintenance(AdministrativeAvailabilityStatusType.MAINTENANCE, task, result);

        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER4_OID, RESOURCE_OID, "default", true);
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
        result.computeStatus();

        TestUtil.assertInProgress("resource in the maintenance pending delta", result);

        String newShadowOid = getLinkRefOid(USER4_OID, RESOURCE_OID);
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
        OperationResult result2 = createOperationResult();
        modelService.recompute(UserType.class, USER4_OID, executeOptions().reconcile(), task, result2);
        result2.computeStatus();

        TestUtil.assertSuccess(result2);

        assertUser(USER4_OID, getTestNameShort()).assertLinks(1); // check that consequent recompute hasnt created second shadow
    }

    private void switchResourceMaintenance (AdministrativeAvailabilityStatusType mode, Task task, OperationResult result) throws Exception {
        ObjectDelta<ResourceType> objectDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(ResourceType.class, RESOURCE_OID, ItemPath.create(ResourceType.F_ADMINISTRATIVE_OPERATIONAL_STATE,
                        new QName("administrativeAvailabilityStatus")), mode);

        provisioningService.applyDefinition(objectDelta, task, result);
        provisioningService.modifyObject(ResourceType.class, objectDelta.getOid(), objectDelta.getModifications(), null, null, task, result);
    }

    private void assertTwoPendingOperations (PrismObject<ShadowType> shadow,
            PendingOperationExecutionStatusType expectedExecutionStatus, OperationResultStatusType expectedResultStatus) {

        assertPendingOperationDeltas(shadow, 2);

        assertPendingOperation(shadow, shadow.asObjectable().getPendingOperation().get(0),
                null, null, expectedExecutionStatus, expectedResultStatus, null, null);

        assertPendingOperation(shadow, shadow.asObjectable().getPendingOperation().get(1),
                null, null, expectedExecutionStatus, expectedResultStatus, null, null);

    }
}
