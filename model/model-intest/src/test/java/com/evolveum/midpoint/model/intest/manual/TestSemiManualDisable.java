/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.manual;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestSemiManualDisable extends TestSemiManual {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Override
    protected BackingStore createBackingStore() {
        return new CsvDisablingBackingStore();
    }

    @Override
    protected String getResourceOid() {
        return RESOURCE_SEMI_MANUAL_DISABLE_OID;
    }

    @Override
    protected File getResourceFile() {
        return RESOURCE_SEMI_MANUAL_DISABLE_FILE;
    }

    @Override
    protected String getRoleOneOid() {
        return ROLE_ONE_SEMI_MANUAL_DISABLE_OID;
    }

    @Override
    protected File getRoleOneFile() {
        return ROLE_ONE_SEMI_MANUAL_DISABLE_FILE;
    }

    @Override
    protected String getRoleTwoOid() {
        return ROLE_TWO_SEMI_MANUAL_DISABLE_OID;
    }

    @Override
    protected File getRoleTwoFile() {
        return ROLE_TWO_SEMI_MANUAL_DISABLE_FILE;
    }

    @Override
    protected boolean nativeCapabilitiesEntered() {
        return true;
    }

    @Override
    protected boolean isDisablingInsteadOfDeletion() {
        return true;
    }

    @Override
    protected void assertUnassignedShadow(ShadowAsserter<?> shadowModelAsserter, boolean backingStoreUpdated, ActivationStatusType expectAlternativeActivationStatus) {
        shadowModelAsserter
            .assertLive()
            .assertAdministrativeStatus(expectAlternativeActivationStatus);
    }

    @Override
    protected void assertUnassignedFuture(ShadowAsserter<?> shadowModelAsserterFuture, boolean assertPassword) {
        shadowModelAsserterFuture
            .assertLive()
            .assertAdministrativeStatus(ActivationStatusType.DISABLED);
        if (assertPassword) {
            assertShadowPassword(shadowModelAsserterFuture);
        }
    }

    @Override
    protected <R> void assertDeprovisionedTimedOutUser(UserAsserter<R> userAsserter, String accountOid) throws Exception {
        userAsserter
            .assertLiveLinks(1);

        assertModelShadow(accountOid)
            .assertAdministrativeStatus(ActivationStatusType.DISABLED);
    }

    @Override
    protected void assertWillUnassignPendingOperationExecuting(ShadowAsserter<Void> shadowRepoAsserter) {
        shadowRepoAsserter
            .pendingOperations()
                .by()
                    .changeType(ChangeTypeType.MODIFY)
                    .item(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)
                .find()
                    .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                    .assertId()
                    .end()
                .end();
    }

    @Override
    protected void assertWillUnassignPendingOperationCompleted(ShadowAsserter<Void> shadowRepoAsserter) {
        shadowRepoAsserter
            .pendingOperations()
                .by()
                    .changeType(ChangeTypeType.MODIFY)
                    .item(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)
                .find()
                    .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                    .assertId()
                    .end()
                .end();
    }

    @Override
    protected void cleanupUser(String userOid, String username, String accountOid) throws Exception {
        Task task = createTask("cleanupUser");
        OperationResult result = task.getResult();

        backingStore.deleteAccount(username);
        try {
            repositoryService.deleteObject(ShadowType.class, accountOid, result);
        } catch (ObjectNotFoundException e) {
            // no problem
        }
        recomputeUser(userOid, task, result);

        PrismObject<UserType> userAfter = getUser(userOid);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 0);
        assertNoShadow(accountOid);
    }

    /**
     * MID-4587
     */
    @Test
    @Override
    public void test416PhoenixAccountUnassignCloseCase() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        closeCase(phoenixLastCaseOid);

        // WHEN
        when();
        reconcileUser(USER_PHOENIX_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        // Make sure the operation will be picked up by propagation task
        clockForward("PT3M");

        // WHEN
        when();
        runPropagation();

        // THEN
        then();

        PrismObject<UserType> userAfter = getUser(USER_PHOENIX_OID);
        display("User after", userAfter);
        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadowModel = getShadowModel(shadowOid);
        display("Shadow after", shadowModel);

        // Shadow NOT dead. We are disabling instead of deleting
        assertShadowNotDead(shadowModel);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_PHOENIX_USERNAME);
        // Semi-manual ... we still see old activationStatus value
        assertActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);

        assertSinglePendingOperation(shadowModel, PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS);

        assertSteadyResources();
    }

    @Test
    @Override
    public void test418AssignPhoenixAccountAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignAccountToUser(USER_PHOENIX_OID, getResourceOid(), null, task, result);

        // THEN
        then();
        phoenixLastCaseOid = assertInProgress(result);

        // Make sure the operation will be picked up by propagation task
        clockForward("PT3M");

        // WHEN
        when();
        runPropagation();

        // THEN
        then();

        PrismObject<UserType> userAfter = getUser(USER_PHOENIX_OID);
        display("User after", userAfter);

        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadowModel = getShadowModel(shadowOid);
        display("Shadow after", shadowModel);

        assertShadowNotDead(shadowModel);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_PHOENIX_USERNAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_PHOENIX_FULL_NAME);

        assertPendingOperationDeltas(shadowModel, 2);
        PendingOperationType disableOperation = findPendingOperation(shadowModel, OperationResultStatusType.SUCCESS, ChangeTypeType.MODIFY);
        assertPendingOperation(shadowModel, disableOperation,
                        PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS);
        assertNotNull("Null completion timestamp", disableOperation.getCompletionTimestamp());
        PendingOperationType enableOperation = findPendingOperation(shadowModel, PendingOperationExecutionStatusType.EXECUTING, null,
                ChangeTypeType.MODIFY, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowModel, enableOperation,
                PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS);

        assertSteadyResources();
    }

    @Override
    protected void assertTest526Deltas(PrismObject<ShadowType> shadowRepo, OperationResult result) {
        assertPendingOperationDeltas(shadowRepo, 3);

        ObjectDeltaType deltaModify = null;
        ObjectDeltaType deltaAdd = null;
        ObjectDeltaType deltaDisable = null;
        for (PendingOperationType pendingOperation: shadowRepo.asObjectable().getPendingOperation()) {
            ObjectDeltaType delta = pendingOperation.getDelta();
            if (ChangeTypeType.ADD.equals(delta.getChangeType())) {
                deltaAdd = delta;
                assertEquals("Wrong status in add delta", OperationResultStatusType.SUCCESS, pendingOperation.getResultStatus());
            }
            if (ChangeTypeType.MODIFY.equals(delta.getChangeType()) && OperationResultStatusType.SUCCESS.equals(pendingOperation.getResultStatus())) {
                deltaModify = delta;
            }
            if (ChangeTypeType.MODIFY.equals(delta.getChangeType()) && OperationResultStatusType.IN_PROGRESS.equals(pendingOperation.getResultStatus())) {
                deltaDisable = delta;
            }
        }
        assertNotNull("No add pending delta", deltaAdd);
        assertNotNull("No modify pending delta", deltaModify);
        assertNotNull("No disable pending delta", deltaDisable);
    }

    @Override
    protected void assertTest528Deltas(PrismObject<ShadowType> shadowRepo, OperationResult result) {
        assertPendingOperationDeltas(shadowRepo, 3);

        for (PendingOperationType pendingOperation: shadowRepo.asObjectable().getPendingOperation()) {
            assertEquals("Wrong status in pending delta", OperationResultStatusType.SUCCESS, pendingOperation.getResultStatus());
        }
    }
}
