/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ORG_DEFAULT;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.ORG_RELATED;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.impl.sync.tasks.recon.DebugReconciliationResultListener;
import com.evolveum.midpoint.model.impl.sync.tasks.recon.ReconciliationActivityHandler;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUuid extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/sync");

    protected static final File RESOURCE_DUMMY_UUID_FILE = new File(TEST_DIR, "resource-dummy-uuid.xml");
    protected static final String RESOURCE_DUMMY_UUID_OID = "9792acb2-0b75-11e5-b66e-001e8c717e5b";
    protected static final String RESOURCE_DUMMY_UUID_NAME = "uuid";

    protected static final File TASK_RECONCILE_DUMMY_UUID_FILE = new File(TEST_DIR, "task-reconcile-dummy-uuid.xml");
    protected static final String TASK_RECONCILE_DUMMY_UUID_OID = "98ae26fc-0b76-11e5-b943-001e8c717e5b";

    private static final String USER_AUGUSTUS_NAME = "augustus";

    private static final String ACCOUNT_AUGUSTUS_NAME = "augustus";
    private static final String ACCOUNT_AUGUSTUS_FULLNAME = "Augustus DeWaat";

    private static final String ACCOUNT_AUGUSTINA_NAME = "augustina";
    private static final String ACCOUNT_AUGUSTINA_FULLNAME = "Augustina LeWhat";

    protected DummyResource dummyResourceUuid;
    protected DummyResourceContoller dummyResourceCtlUuid;
    protected ResourceType resourceDummyUuidType;
    protected PrismObject<ResourceType> resourceDummyUuid;

    private String augustusShadowOid;

    @Autowired private ReconciliationActivityHandler reconciliationActivityHandler;

    private DebugReconciliationResultListener reconciliationTaskResultListener;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        reconciliationTaskResultListener = new DebugReconciliationResultListener();
        reconciliationActivityHandler.setReconciliationResultListener(reconciliationTaskResultListener);

        dummyResourceCtlUuid = DummyResourceContoller.create(RESOURCE_DUMMY_UUID_NAME, resourceDummyUuid);
        dummyResourceCtlUuid.extendSchemaPirate();
        dummyResourceUuid = dummyResourceCtlUuid.getDummyResource();
        resourceDummyUuid = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_UUID_FILE, RESOURCE_DUMMY_UUID_OID, initTask, initResult);
        resourceDummyUuidType = resourceDummyUuid.asObjectable();
        dummyResourceCtlUuid.setResource(resourceDummyUuid);

        InternalMonitor.reset();
        InternalMonitor.setTrace(InternalOperationClasses.SHADOW_FETCH_OPERATIONS, true);

//        DebugUtil.setDetailedDebugDump(true);
    }

    @Test
    public void test200ReconcileDummyUuid() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // TODO ... do what?

        getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        reconciliationTaskResultListener.clear();

        when();
        importObjectFromFile(TASK_RECONCILE_DUMMY_UUID_FILE);

        then();

        waitForTaskFinish(TASK_RECONCILE_DUMMY_UUID_OID);

        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_UUID_OID, 0, 0, 0, 0);

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);

        assertEquals("Unexpected number of users", 6, users.size());

        displayValue("Dummy resource", getDummyResource().debugDump());

        // MID-7110
        //assertReconAuditModifications(0, TASK_RECONCILE_DUMMY_UUID_OID);

        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_UUID_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult);
    }

    @Test
    public void test210ReconcileDummyUuidAddAugustus() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount account = new DummyAccount(ACCOUNT_AUGUSTUS_NAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_AUGUSTUS_FULLNAME);
        dummyResourceUuid.addAccount(account);

        getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        reconciliationTaskResultListener.clear();

        Task taskBefore = taskManager.getTaskPlain(TASK_RECONCILE_DUMMY_UUID_OID, result);

        when();
        restartTask(TASK_RECONCILE_DUMMY_UUID_OID);

        then();

        waitForTaskNextRunAssertSuccess(taskBefore);

        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_UUID_OID, 0, 1, 0, 0);

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);

        assertImportedUserByUsername(ACCOUNT_AUGUSTUS_NAME, RESOURCE_DUMMY_UUID_OID);
        assertDummyAccountAttribute(RESOURCE_DUMMY_UUID_NAME, ACCOUNT_AUGUSTUS_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                ACCOUNT_AUGUSTUS_FULLNAME);

        assertEquals("Unexpected number of users", 7, users.size());

        displayValue("Dummy resource", getDummyResource().debugDump());

        // MID-7110
        //assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_UUID_OID);

        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_UUID_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult);

        PrismObject<UserType> user = findUserByUsername(USER_AUGUSTUS_NAME);
        display("Augustus after recon", user);
        augustusShadowOid = getSingleLinkOid(user);
        repositoryService.getObject(ShadowType.class, augustusShadowOid, null, result);
    }

    /**
     * Augustus is deleted and re-added with the same username. New shadow needs to be created.
     */
    @Test
    public void test220ReconcileDummyUuidDeleteAddAugustus() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount oldAccount = dummyResourceUuid.getAccountByUsername(ACCOUNT_AUGUSTUS_NAME);
        displayDumpable("Deleting account", oldAccount);
        dummyResourceUuid.deleteAccountByName(ACCOUNT_AUGUSTUS_NAME);
        assertNoDummyAccount(ACCOUNT_AUGUSTUS_NAME, ACCOUNT_AUGUSTUS_NAME);

        DummyAccount newAccount = new DummyAccount(ACCOUNT_AUGUSTUS_NAME);
        newAccount.setEnabled(true);
        newAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_AUGUSTUS_FULLNAME);
        newAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, USER_AUGUSTUS_NAME);
        dummyResourceUuid.addAccount(newAccount);
        newAccount = dummyResourceUuid.getAccountByUsername(ACCOUNT_AUGUSTUS_NAME);
        displayDumpable("Created account", newAccount);

        assertThat(oldAccount.getId())
                .as("oldAccount ID")
                .isNotEqualTo(newAccount.getId());

        displayValue("Old shadow OID", augustusShadowOid);
        display("Account ID " + oldAccount.getId() + " -> " + newAccount.getId());

        Task taskBefore = taskManager.getTaskPlain(TASK_RECONCILE_DUMMY_UUID_OID, result);

        getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        reconciliationTaskResultListener.clear();

        when();
        restartTask(TASK_RECONCILE_DUMMY_UUID_OID);

        then();
        waitForTaskNextRunAssertSuccess(taskBefore);

        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_UUID_OID, 0, 1, 0, 1);

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);

        assertImportedUserByUsername(ACCOUNT_AUGUSTUS_NAME, RESOURCE_DUMMY_UUID_OID);
        assertDummyAccountAttribute(RESOURCE_DUMMY_UUID_NAME, ACCOUNT_AUGUSTUS_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                ACCOUNT_AUGUSTUS_FULLNAME);

        assertEquals("Unexpected number of users", 7, users.size());

        displayValue("Dummy resource", getDummyResource().debugDump());

        // MID-7110
        //assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_UUID_OID);

        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_UUID_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult.getOperation(), reconTaskResult, 4); // there is an error deep inside (that is expected)

        // @formatter:off
        augustusShadowOid = assertUserAfterByUsername(USER_AUGUSTUS_NAME)
                .links()
                .assertLinks(1, 1)
                .by().dead(true).relation(ORG_RELATED).find()
                    .assertOid(augustusShadowOid)
                    .end()
                .by().dead(false).relation(ORG_DEFAULT).find()
                    .assertOidDifferentThan(augustusShadowOid)
                .getOid();
        // @formatter:on
    }

    /**
     * Augustus is deleted and Augustina re-added. They correlate to the same user.
     * New shadow needs to be created.
     */
    @Test
    public void test230ReconcileDummyUuidDeleteAugustusAddAugustina() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount oldAccount = dummyResourceUuid.getAccountByUsername(ACCOUNT_AUGUSTUS_NAME);
        dummyResourceUuid.deleteAccountByName(ACCOUNT_AUGUSTUS_NAME);
        assertNoDummyAccount(ACCOUNT_AUGUSTUS_NAME, ACCOUNT_AUGUSTUS_NAME);

        DummyAccount account = new DummyAccount(ACCOUNT_AUGUSTINA_NAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_AUGUSTINA_FULLNAME);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, USER_AUGUSTUS_NAME);
        dummyResourceUuid.addAccount(account);
        account = dummyResourceUuid.getAccountByUsername(ACCOUNT_AUGUSTINA_NAME);

        assertThat(oldAccount.getId())
                .as("oldAccount ID")
                .isNotEqualTo(account.getId());

        displayValue("Old shadow OID", augustusShadowOid);
        display("Account ID " + oldAccount.getId() + " -> " + account.getId());

        Task taskBefore = taskManager.getTaskPlain(TASK_RECONCILE_DUMMY_UUID_OID, result);

        getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        reconciliationTaskResultListener.clear();

        when();
        restartTask(TASK_RECONCILE_DUMMY_UUID_OID);

        then();
        waitForTaskNextRunAssertSuccess(taskBefore);

        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_UUID_OID, 0, 1, 0, 2);

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);

        assertImportedUserByUsername(USER_AUGUSTUS_NAME, RESOURCE_DUMMY_UUID_OID);
        assertDummyAccountAttribute(RESOURCE_DUMMY_UUID_NAME, ACCOUNT_AUGUSTINA_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                ACCOUNT_AUGUSTINA_FULLNAME);

        assertEquals("Unexpected number of users", 7, users.size());

        displayValue("Dummy resource", getDummyResource().debugDump());

        // MID-7110
        //assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_UUID_OID);

        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_UUID_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult.getOperation(), reconTaskResult, 4); // there is an error deep inside (that is expected)

        // @formatter:off
        assertUserAfterByUsername(USER_AUGUSTUS_NAME)
                .displayWithProjections()
                .links()
                .assertLinks(1, 2)
                .by().dead(true)
                    .assertCount(2)
                .by().dead(false).find()
                    .assertOidDifferentThan(augustusShadowOid);
        // @formatter:on
    }

    private void assertImportedUserByOid(String userOid, String... resourceOids)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<UserType> user = getUser(userOid);
        assertNotNull("No user " + userOid, user);
        assertImportedUser(user, resourceOids);
    }

    private void assertImportedUserByUsername(String username, String... resourceOids)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<UserType> user = findUserByUsername(username);
        assertNotNull("No user " + username, user);
        assertImportedUser(user, resourceOids);
    }

    private void assertImportedUser(PrismObject<UserType> user, String... resourceOids)
            throws ObjectNotFoundException, SchemaException {
        UserAsserter<Void> userAsserter = assertUser(user, "imported")
                .displayWithProjections()
                .links()
                .by()
                .dead(false)
                .assertCount(resourceOids.length)
                .end()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);
        for (String resourceOid : resourceOids) {
            userAsserter
                    .links()
                    .by()
                    .resourceOid(resourceOid)
                    .dead(false)
                    .find()
                    .resolveTarget()
                    .assertKind(ShadowKindType.ACCOUNT);
        }
    }
}
