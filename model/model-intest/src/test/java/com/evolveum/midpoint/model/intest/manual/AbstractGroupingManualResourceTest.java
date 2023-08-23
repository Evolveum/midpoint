/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.manual;

import static org.testng.AssertJUnit.*;

import java.io.File;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class AbstractGroupingManualResourceTest extends AbstractManualResourceTest {

    protected static final File TEST_DIR = new File("src/test/resources/manual/");

    protected static final File RESOURCE_MANUAL_GROUPING_FILE = new File(TEST_DIR, "resource-manual-grouping.xml");
    protected static final String RESOURCE_MANUAL_GROUPING_OID = "a6e228a0-f092-11e7-b5bc-579f2e54e15c";

    protected static final File RESOURCE_SEMI_MANUAL_GROUPING_FILE = new File(TEST_DIR, "resource-semi-manual-grouping.xml");
    protected static final String RESOURCE_SEMI_MANUAL_GROUPING_OID = "9eddca88-f222-11e7-98dc-cb6e4b08800c";

    protected static final File RESOURCE_SEMI_MANUAL_GROUPING_PROPOSED_FILE = new File(TEST_DIR, "resource-semi-manual-grouping-proposed.xml");

    protected static final File ROLE_ONE_MANUAL_GROUPING_FILE = new File(TEST_DIR, "role-one-manual-grouping.xml");
    protected static final String ROLE_ONE_MANUAL_GROUPING_OID = "bc586500-f092-11e7-9cda-f7cd4203a755";

    protected static final File ROLE_ONE_SEMI_MANUAL_GROUPING_FILE = new File(TEST_DIR, "role-one-semi-manual-grouping.xml");
    protected static final String ROLE_ONE_SEMI_MANUAL_GROUPING_OID = "dc961c9a-f222-11e7-b19a-0fa30f483712";

    protected static final File ROLE_TWO_MANUAL_GROUPING_FILE = new File(TEST_DIR, "role-two-manual-grouping.xml");
    protected static final String ROLE_TWO_MANUAL_GROUPING_OID = "c9de1300-f092-11e7-8c5f-3ff8ea609a1d";

    protected static final File ROLE_TWO_SEMI_MANUAL_GROUPING_FILE = new File(TEST_DIR, "role-two-semi-manual-grouping.xml");
    protected static final String ROLE_TWO_SEMI_MANUAL_GROUPING_OID = "17fafa4e-f223-11e7-bbee-ff66557fc83f";

    protected static final File TASK_PROPAGATION_MANUAL_GROUPING_FILE = new File(TEST_DIR, "task-propagation-manual-grouping.xml");
    protected static final String TASK_PROPAGATION_MANUAL_GROUPING_OID = "b84a2c46-f0b5-11e7-baff-d35c2f14080f";

    protected static final File TASK_PROPAGATION_MULTI_FILE = new File(TEST_DIR, "task-propagation-multi.xml");
    protected static final String TASK_PROPAGATION_MULTI_OID = "01db4542-f224-11e7-8833-bbe6634814e7";

    protected String propagationTaskOid = null;
    protected XMLGregorianCalendar accountWillExecutionTimestampStart;
    protected XMLGregorianCalendar accountWillExecutionTimestampEnd;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Override
    protected boolean isDirect() {
        return false;
    }

    @Override
    protected void runPropagation(OperationResultStatusType expectedStatus) throws Exception {
        if (propagationTaskOid == null) {
            addTask(getPropagationTaskFile());
            propagationTaskOid = getPropagationTaskOid();
            assertNewPropagationTask();
            waitForTaskStart(propagationTaskOid);
        } else {
            restartTask(propagationTaskOid);
        }
        Task finishedTask = waitForTaskFinish(propagationTaskOid, DEFAULT_TASK_WAIT_TIMEOUT, true);
        assertFinishedPropagationTask(finishedTask, expectedStatus);
    }

    protected void assertNewPropagationTask() throws Exception {

    }

    protected void assertFinishedPropagationTask(Task finishedTask, OperationResultStatusType expectedStatus) {
        display("Finished propagation task", finishedTask);
        OperationResultStatusType resultStatus = finishedTask.getResultStatus();
        if (expectedStatus == null) {
            if (resultStatus != OperationResultStatusType.SUCCESS && resultStatus != OperationResultStatusType.IN_PROGRESS) {
                fail("Unexpected propagation task result " + resultStatus);
            }
        } else {
            assertEquals("Unexpected propagation task result", expectedStatus, resultStatus);
        }
    }

    protected abstract String getPropagationTaskOid();

    protected abstract File getPropagationTaskFile();

    // Grouping execution. The operation is delayed for a while.
    @Override
    protected PendingOperationExecutionStatusType getExpectedExecutionStatus(PendingOperationExecutionStatusType executionStage) {
        return executionStage;
    }

    @Override
    protected OperationResultStatusType getExpectedResultStatus(PendingOperationExecutionStatusType executionStage) {
        if (executionStage == PendingOperationExecutionStatusType.EXECUTION_PENDING) {
            return null;
        }
        if (executionStage == PendingOperationExecutionStatusType.EXECUTING) {
            return OperationResultStatusType.IN_PROGRESS;
        }
        return null;
    }

    @Test
    @Override
    public void test100AssignWillRoleOne() throws Exception {
        // The count will be checked only after propagation was run, so we can address both direct and grouping cases
        rememberCounter(InternalCounters.CONNECTOR_MODIFICATION_COUNT);

        assignWillRoleOne(USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTION_PENDING);

        // No counter increment asserted here. Connector instance is initialized in propagation. Not yet.
        assertSteadyResources();
    }

    /**
     * disable - do not run propagation yet
     * (do not wait for delta to expire, we want several deltas at once so we can test grouping).
     */
    @Test
    public void test220ModifyUserWillDisable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        modifyUserReplace(userWillOid, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

        // THEN
        then();
        display("result", result);
        willLastCaseOid = assertInProgress(result);

        accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);
        assertPendingOperationDeltas(shadowRepo, 2);
        PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
                PendingOperationExecutionStatusType.EXECUTION_PENDING, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowRepo, pendingOperation,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.EXECUTION_PENDING, null,
                null, null);

        assertNotNull("No ID in pending operation", pendingOperation.getId());
        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_NAME, ATTR_USERNAME_QNAME, prismContext));
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_FULL_NAME_PIRATE, ATTR_FULLNAME_QNAME, prismContext));

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 2);
        pendingOperation = findPendingOperation(shadowModel,
                PendingOperationExecutionStatusType.EXECUTION_PENDING, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowModel, pendingOperation,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.EXECUTION_PENDING, null,
                null, null);

        PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowModelFuture);
        assertShadowName(shadowModelFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowModelFuture, ActivationStatusType.DISABLED);
        assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelFuture);

        assertNull("Unexpected async reference in result", willLastCaseOid);
    }

    /**
     * Do NOT run propagation yet. Change password, enable.
     * There is still pending disable delta. Make sure all the deltas are
     * stored correctly.
     */
    @Test
    public void test230ModifyAccountWillChangePasswordAndEnable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                userWillOid, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                ActivationStatusType.ENABLED);
        ProtectedStringType ps = new ProtectedStringType();
        ps.setClearValue(USER_WILL_PASSWORD_NEW);
        delta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE, ps);
        displayDumpable("ObjectDelta", delta);

        accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        display("result", result);
        assertInProgress(result);

        accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        assertAccountWillAfterChangePasswordAndEnable();
    }

    protected void assertAccountWillAfterChangePasswordAndEnable() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);
        assertPendingOperationDeltas(shadowRepo, 3);
        PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
                PendingOperationExecutionStatusType.EXECUTION_PENDING, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowRepo, pendingOperation,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
                PendingOperationExecutionStatusType.EXECUTION_PENDING, null,
                null, null);

        assertNotNull("No ID in pending operation", pendingOperation.getId());
        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_NAME, ATTR_USERNAME_QNAME, prismContext));
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_FULL_NAME_PIRATE, ATTR_FULLNAME_QNAME, prismContext));

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 3);
        pendingOperation = findPendingOperation(shadowModel,
                PendingOperationExecutionStatusType.EXECUTION_PENDING, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowModel, pendingOperation,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
                PendingOperationExecutionStatusType.EXECUTION_PENDING, null,
                null, null);

        PrismObject<ShadowType> shadowProvisioningFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        new PrismObjectAsserter<>(shadowProvisioningFuture)
                .assertSanity();

        assertNull("Unexpected async reference in result", willSecondLastCaseOid);
    }

    /**
     * Run propagation before the interval is over. Nothing should happen.
     */
    @Test
    public void test232RunPropagationBeforeInterval() throws Exception {
        when();
        runPropagation();

        then();
        assertAccountWillAfterChangePasswordAndEnable();
    }

    /**
     * ff 2min, run propagation. There should be just one operation (one case) that
     * combines both deltas.
     */
    @Test
    public void test235RunPropagationAfterInterval() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT2M");

        accountWillExecutionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        when();
        runPropagation();

        then();
        accountWillExecutionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);

        assertPendingOperationDeltas(shadowRepo, 3);

        PendingOperationType pendingOperation1 = findPendingOperation(shadowRepo,
                PendingOperationExecutionStatusType.EXECUTING, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowRepo, pendingOperation1,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS,
                null, null);
        willLastCaseOid = pendingOperation1.getAsynchronousOperationReference();
        assertNotNull("No case ID in pending operation", willLastCaseOid);

        PendingOperationType pendingOperation2 = findPendingOperation(shadowRepo,
                PendingOperationExecutionStatusType.EXECUTING, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowRepo, pendingOperation2,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
                PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS,
                null, null);
        assertNotNull("No ID in pending operation", pendingOperation2.getId());
        assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation2.getAsynchronousOperationReference());

        // TODO: check execution timestamps

        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_NAME, ATTR_USERNAME_QNAME, prismContext));
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_FULL_NAME_PIRATE, ATTR_FULLNAME_QNAME, prismContext));

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 3);

        pendingOperation1 = findPendingOperation(shadowModel,
                PendingOperationExecutionStatusType.EXECUTING, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowRepo, pendingOperation1,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS,
                null, null);
        assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation1.getAsynchronousOperationReference());

        pendingOperation2 = findPendingOperation(shadowModel,
                PendingOperationExecutionStatusType.EXECUTING, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowModel, pendingOperation2,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
                PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS,
                null, null);
        assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation2.getAsynchronousOperationReference());

        // TODO: check execution timestamps

        PrismObject<ShadowType> shadowProvisioningFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        new PrismObjectAsserter<>(shadowProvisioningFuture)
                .assertSanity();

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);

        // TODO: check number of cases
    }

    /**
     * Close the case. Both deltas should be marked as completed.
     * Do NOT explicitly refresh the shadow in this case. Just reading it should cause the refresh.
     */
    @Test
    public void test240CloseCaseAndReadAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        closeCase(willLastCaseOid);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        assertAccountWillAfterChangePasswordAndEnableCaseClosed(shadowModel);

    }

    /**
     * lets ff 5min just for fun. Refresh, make sure everything should be the same (grace not expired yet)
     */
    @Test
    public void test250RecomputeWillAfter5min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT5M");

        PrismObject<ShadowType> shadowBefore = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertAccountWillAfterChangePasswordAndEnableCaseClosed(null);
    }

    /**
     * ff 7min. Backing store updated. Grace expired.
     * But operation is still in the shadow. Not expired yet.
     */
    @Test
    public void test272UpdateBackingStoreAndGetAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        //  ff 7min. Refresh. Oldest delta over grace. But not expired yet.
        clockForward("PT7M");

        backingStoreUpdateWill(USER_WILL_FULL_NAME_PIRATE, INTEREST_ONE, ActivationStatusType.ENABLED, USER_WILL_PASSWORD_NEW);

        // WHEN
        when();
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        ShadowAsserter<Void> shadowModelAsserter = ShadowAsserter.forShadow(shadowModel, "model")
                .assertName(USER_WILL_NAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                .end()
                .pendingOperations()
                .assertOperations(3)
                .end();
        assertAttributeFromBackingStore(shadowModelAsserter, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelAsserter);

        assertRepoShadow(accountWillOid)
                .pendingOperations()
                .assertOperations(3);

        ShadowAsserter<Void> shadowModelFutureAsserter = assertModelShadowFuture(accountWillOid)
                .assertName(USER_WILL_NAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                .end();
        assertAttributeFromBackingStore(shadowModelFutureAsserter, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        // TODO
//        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * ff 15min. One pending operation exipired.
     */
    @Test
    public void test273GetAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        //  ff 15min. Oldest delta should expire.
        clockForward("PT15M");

        // WHEN
        when();
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        ShadowAsserter<Void> shadowModelAsserter = ShadowAsserter.forShadow(shadowModel, "model")
                .assertName(USER_WILL_NAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                .end()
                .pendingOperations()
                .assertOperations(2)
                .end();
        assertAttributeFromBackingStore(shadowModelAsserter, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelAsserter);

        assertRepoShadow(accountWillOid)
                .pendingOperations()
                .assertOperations(2);

        ShadowAsserter<Void> shadowModelFutureAsserter = assertModelShadowFuture(accountWillOid)
                .assertName(USER_WILL_NAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                .end();
        assertAttributeFromBackingStore(shadowModelFutureAsserter, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        // TODO
//        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * ff 15min. Refresh. All delta should expire.
     */
    @Test
    public void test290RecomputeWillAfter15min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT15M");

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);

        assertPendingOperationDeltas(shadowRepo, 0);

        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_NAME, ATTR_USERNAME_QNAME, prismContext));
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_FULL_NAME_PIRATE, ATTR_FULLNAME_QNAME, prismContext));

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 0);

        PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowModelFuture);
        assertShadowName(shadowModelFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowModelFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    @Test
    public void test300UnassignAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        unassignRole(userWillOid, getRoleOneOid(), task, result);

        // THEN
        then();
        display("result", result);
        willLastCaseOid = assertInProgress(result);

        accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);
        assertPendingOperationDeltas(shadowRepo, 1);
        PendingOperationType pendingOperation = findPendingOperation(shadowRepo, PendingOperationExecutionStatusType.EXECUTION_PENDING);
        assertPendingOperation(shadowRepo, pendingOperation,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.EXECUTION_PENDING, null,
                null, null);

        assertNotNull("No ID in pending operation", pendingOperation.getId());
        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_NAME, ATTR_USERNAME_QNAME, prismContext));
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_FULL_NAME_PIRATE, ATTR_FULLNAME_QNAME, prismContext));

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 1);
        pendingOperation = findPendingOperation(shadowModel, PendingOperationExecutionStatusType.EXECUTION_PENDING);
        assertPendingOperation(shadowRepo, pendingOperation,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.EXECUTION_PENDING, null,
                null, null);

        assertWillUnassignedFuture(assertModelShadowFuture(accountWillOid), true);

        // Make sure that the account is still linked
        PrismObject<UserType> userAfter = getUser(userWillOid);
        display("User after", userAfter);
        String accountWillOidAfter = getSingleLinkOid(userAfter);
        assertEquals(accountWillOid, accountWillOidAfter);
        assertNoAssignments(userAfter);

        assertNull("Unexpected async reference in result", willLastCaseOid);

//        assertNotNull("No async reference in result", willLastCaseOid);
//        assertCase(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    /**
     * ff 2min, run propagation.
     */
    @Test
    public void test302RunPropagationAfterInterval() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT2M");

        accountWillExecutionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        runPropagation();

        // THEN
        then();

        accountWillExecutionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);
        assertPendingOperationDeltas(shadowRepo, 1);
        PendingOperationType pendingOperation = findPendingOperation(shadowRepo, PendingOperationExecutionStatusType.EXECUTING);
        assertPendingOperation(shadowRepo, pendingOperation,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS,
                null, null);
        willLastCaseOid = pendingOperation.getAsynchronousOperationReference();
        assertNotNull("No case ID in pending operation", willLastCaseOid);

        assertNotNull("No ID in pending operation", pendingOperation.getId());
        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_NAME, ATTR_USERNAME_QNAME, prismContext));
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_FULL_NAME_PIRATE, ATTR_FULLNAME_QNAME, prismContext));

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 1);
        pendingOperation = findPendingOperation(shadowModel, PendingOperationExecutionStatusType.EXECUTING);
        assertPendingOperation(shadowRepo, pendingOperation,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS,
                null, null);
        assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation.getAsynchronousOperationReference());

        assertWillUnassignedFuture(assertModelShadowFuture(accountWillOid), true);

        // Make sure that the account is still linked
        PrismObject<UserType> userAfter = getUser(userWillOid);
        display("User after", userAfter);
        String accountWillOidAfter = getSingleLinkOid(userAfter);
        assertEquals(accountWillOid, accountWillOidAfter);
        assertNoAssignments(userAfter);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    /**
     * Case is closed. The operation is complete.
     */
    @Test
    public void test310CloseCaseAndRecomputeWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        closeCase(willLastCaseOid);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        // We need reconcile and not recompute here. We need to fetch the updated case status.
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        display("result", result);
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);
        assertSinglePendingOperation(shadowRepo,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
        assertUnassignedShadow(shadowRepo);

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertUnassignedShadow(shadowModel); // backing store not yet updated
        assertShadowPassword(shadowModel);

        assertSinglePendingOperation(
                shadowModel, accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        assertWillUnassignedFuture(assertModelShadowFuture(accountWillOid), true);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    @Test
    public void test330UpdateBackingStoreAndRecomputeWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        backingStoreDeprovisionWill();
        displayBackingStore();

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);
        assertSinglePendingOperation(shadowRepo,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
        assertUnassignedShadow(shadowRepo);

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertUnassignedShadow(shadowModel);

        assertSinglePendingOperation(shadowModel,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        assertWillUnassignedFuture(assertModelShadowFuture(accountWillOid), false);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * Put everything in a clean state so we can start over.
     */
    @Test
    public void test349CleanUp() throws Exception {
        cleanupUser(userWillOid, USER_WILL_NAME, accountWillOid);
    }

//    TODO: test400: create -> modify -> propagation

    protected void assertUnassignedShadow(PrismObject<ShadowType> shadow) {
        assertShadowDead(shadow);
    }

    protected void assertAccountWillAfterChangePasswordAndEnableCaseClosed(
            PrismObject<ShadowType> shadowModel) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);

        assertPendingOperationDeltas(shadowRepo, 3);

        PendingOperationType pendingOperation1 = findPendingOperation(shadowRepo,
                PendingOperationExecutionStatusType.COMPLETED, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowRepo, pendingOperation1,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
        assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation1.getAsynchronousOperationReference());

        PendingOperationType pendingOperation2 = findPendingOperation(shadowRepo,
                PendingOperationExecutionStatusType.COMPLETED, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowRepo, pendingOperation2,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
                PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
        assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation2.getAsynchronousOperationReference());

        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_NAME, ATTR_USERNAME_QNAME, prismContext));
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME,
                RawType.fromPropertyRealValue(USER_WILL_FULL_NAME_PIRATE, ATTR_FULLNAME_QNAME, prismContext));

        if (shadowModel == null) {
            shadowModel = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);
        }
        display("Model shadow", shadowModel);
        ShadowType shadowTypeModel = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeModel.getKind());
        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);

        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        // Password is here, even though it (strictly speaking) should not be here
        // It is here because we have just applied the delta. So we happen to know the password now.
        // The password will NOT be there in next recompute.
//        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 3);

        pendingOperation1 = findPendingOperation(shadowModel,
                PendingOperationExecutionStatusType.COMPLETED, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowModel, pendingOperation1,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
        assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation1.getAsynchronousOperationReference());

        pendingOperation2 = findPendingOperation(shadowModel,
                PendingOperationExecutionStatusType.COMPLETED, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowRepo, pendingOperation2,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
                PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
        assertEquals("Case ID mismatch", willLastCaseOid, pendingOperation2.getAsynchronousOperationReference());

        PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowModelFuture);
        assertShadowName(shadowModelFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowModelFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        // TODO
//        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

}
