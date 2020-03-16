/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collection;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * Test for various aspects of provisioning failure handling
 * (aka "consistency mechanism").
 * <p>
 * MID-3603
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyConsistency extends AbstractDummyTest {

    public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "consistency");
    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy-retry.xml");

    protected static final String ACCOUNT_MORGAN_FULLNAME_HM = "Henry Morgan";
    protected static final String ACCOUNT_MORGAN_FULLNAME_CHM = "Captain Henry Morgan";

    private static final String ACCOUNT_JP_MORGAN_FULLNAME = "J.P. Morgan";
    private static final String ACCOUNT_BETTY_USERNAME = "betty";
    private static final String ACCOUNT_BETTY_FULLNAME = "Betty Rubble";
    private static final String ACCOUNT_ELIZABETH2_FULLNAME = "Her Majesty Queen Elizabeth II";

    protected static final File ACCOUNT_SHADOW_MURRAY_PENDING_FILE = new File(TEST_DIR, "account-shadow-murray-pending-operation.xml");
    protected static final String ACCOUNT_SHADOW_MURRAY_PENDING_OID = "34132742-2085-11e9-a956-17770b09881b";
    private static final String ACCOUNT_MURRAY_USERNAME = "murray";
    private static final String ACCOUNT_MURRAY_FULL_NAME = "Murray";

    private XMLGregorianCalendar lastRequestStartTs;
    private XMLGregorianCalendar lastRequestEndTs;
    private XMLGregorianCalendar lastAttemptStartTs;
    private XMLGregorianCalendar lastAttemptEndTs;
    private String shadowMorganOid = ACCOUNT_MORGAN_OID;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Test
    public void test000Integrity() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        displayValue("Dummy resource instance", dummyResource.toString());

        OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID, task);
        assertSuccess(testResult);

        resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, null, result);
        resourceType = resource.asObjectable();
        assertSuccess(result);
        rememberSteadyResources();
    }

    /**
     * Mostly just a sanity test. Make sure that normal (non-failure) operation works.
     * Also prepares some state for later tests.
     */
    @Test
    public void test050AddAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> account = prismContext.parseObject(getAccountWillFile());
        account.checkConsistence();
        display("Adding shadow", account);

        // WHEN
        when();
        String addedObjectOid = provisioningService.addObject(account, null, null, task, result);

        // THEN
        then();
        assertSuccess(result);
        assertEquals(ACCOUNT_WILL_OID, addedObjectOid);
        syncServiceMock.assertNotifySuccessOnly();

        PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Account provisioning", accountProvisioning);
        // @formatter:off
        ShadowAsserter.forShadow(accountProvisioning)
            .assertNoLegacyConsistency()
            .pendingOperations()
                .assertNone();
        // @formatter:on

        DummyAccount dummyAccount = dummyResource.getAccountByUsername(transformNameFromResource(ACCOUNT_WILL_USERNAME));
        assertNotNull("No dummy account", dummyAccount);
        assertEquals("Username is wrong", transformNameFromResource(ACCOUNT_WILL_USERNAME), dummyAccount.getName());
        assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
        assertTrue("The account is not enabled", dummyAccount.isEnabled());
        assertEquals("Wrong password", ACCOUNT_WILL_PASSWORD, dummyAccount.getPassword());

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        PrismObject<ShadowType> shadowFromRepo = getShadowRepo(addedObjectOid);
        assertNotNull("Shadow was not created in the repository", shadowFromRepo);
        display("Repository shadow", shadowFromRepo);

        checkRepoAccountShadow(shadowFromRepo);
        // @formatter:off
        ShadowAsserter.forShadow(shadowFromRepo)
            .assertNoLegacyConsistency()
            .pendingOperations()
                .assertNone();
        // @formatter:on

        checkUniqueness(accountProvisioning);
        assertSteadyResources();
    }

    // TODO: asssert operationExecution

    @Test
    public void test100AddAccountMorganCommunicationFailure() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_MORGAN_FILE);
        account.checkConsistence();
        display("Adding shadow", account);

        lastRequestStartTs = lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        String addedObjectOid = provisioningService.addObject(account, null, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertInProgress(result);
        assertEquals(ACCOUNT_MORGAN_OID, addedObjectOid);
        account.checkConsistence();
        lastRequestEndTs = lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifyInProgressOnly();

        assertUncreatedMorgan(1);

        // Resource -> down
        assertResourceStatusChangeCounterIncrements();
        assertSteadyResources();
    }

    /**
     * Test add with pending operation and recovered resource. This happens when refresh task
     * does not have a chance to run yet. The forceFresh option is NOT used here.
     * Therefore nothing significant should happen.
     */
    @Test
    public void test102GetAccountMorganRecovery() throws Exception {
        // GIVEN
        syncServiceMock.reset();

        dummyResource.resetBreakMode();

        // WHEN
        when();
        assertGetUncreatedShadow(ACCOUNT_MORGAN_OID);

        // THEN
        then();
        syncServiceMock.assertNoNotifcations();

        assertUncreatedMorgan(1);

        assertSteadyResources();
    }

    /**
     * Refresh while the resource is down. Retry interval is not yet reached.
     * Nothing should really happen yet.
     */
    @Test
    public void test104RefreshAccountMorganCommunicationFailure() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(ACCOUNT_MORGAN_OID);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        syncServiceMock.assertNoNotifcations();

        assertUncreatedMorgan(1);

        assertSteadyResources();
    }

    /**
     * Get account with forceRefresh while the resource is down. Retry interval is not yet reached.
     * Nothing should really happen yet.
     */
    @Test
    public void test105GetForceRefreshAccountMorganCommunicationFailure() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createForceRefresh());

        try {
            // WHEN
            when();

            provisioningService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID, options, task, result);

            assertNotReached();

        } catch (GenericConnectorException e) {
            // expected
        }

        // THEN
        then();
        display("Result", result);
        assertFailure(result);
        syncServiceMock.assertNoNotifcations();

        assertUncreatedMorgan(1);

        assertSteadyResources();
    }

    /**
     * Wait for retry interval to pass. Now provisioning should retry add operation.
     * But no luck yet. Resource is still down.
     */
    @Test
    public void test106RefreshAccountMorganCommunicationFailureRetry() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT17M");

        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(ACCOUNT_MORGAN_OID);

        lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        result.computeStatus();
        TestUtil.assertResultStatus(result, OperationResultStatus.HANDLED_ERROR);
        lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifyInProgressOnly();

        assertUncreatedMorgan(2);

        assertSteadyResources();
    }

    /**
     * Wait for yet another retry interval to pass. Now provisioning should retry add operation
     * again. Still no luck. As this is the third and last attempt the operation should now be
     * completed, fatal error recorded and the shadow should be dead.
     */
    @Test
    public void test108RefreshAccountMorganCommunicationFailureRetryAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT17M");

        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(ACCOUNT_MORGAN_OID);

        lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        result.computeStatus();
        TestUtil.assertResultStatus(result, OperationResultStatus.HANDLED_ERROR);
        lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifyFailureOnly();

        assertMorganDead();
    }

    private void assertMorganDead() throws Exception {
        PrismObject<ShadowType> repoShadow = getShadowRepo(ACCOUNT_MORGAN_OID);
        assertNotNull("Shadow was not created in the repository", repoShadow);

        // @formatter:off
        assertRepoShadow(ACCOUNT_MORGAN_OID)
            .pendingOperations()
                .singleOperation()
                    .display()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(3)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .assertCompletionTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .display()
                        .assertAdd()
                        .end()
                    .end()
                .end()
            .assertBasicRepoProperties()
            .assertKind(ShadowKindType.ACCOUNT)
            .assertDead()
            .assertIsNotExists()
            .assertNoLegacyConsistency()
            .attributes()
                .assertAttributes(SchemaConstants.ICFS_NAME);

        assertShadowNoFetch(ACCOUNT_MORGAN_OID)
            .pendingOperations()
                .singleOperation()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(3)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .assertCompletionTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .assertAdd()
                        .end()
                    .end()
                .end()
            .assertDead()
            .assertIsNotExists()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertNoPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(1);

        ShadowAsserter<Void> shadowProvisioningFutureAsserter =
                assertShadowFuture(ACCOUNT_MORGAN_OID)
            .assertDead()
            .assertIsNotExists()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertNoPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end();

        assertShadowFutureNoFetch(ACCOUNT_MORGAN_OID)
            .assertDead()
            .assertIsNotExists()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertNoPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end();
        // @formatter:on

        dummyResource.resetBreakMode();

        DummyAccount dummyAccount = dummyResource.getAccountByUsername(transformNameFromResource(ACCOUNT_WILL_USERNAME));
        assertNotNull("No dummy account", dummyAccount);
        assertEquals("Username is wrong", transformNameFromResource(ACCOUNT_WILL_USERNAME), dummyAccount.getName());
        assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
        assertTrue("The account is not enabled", dummyAccount.isEnabled());
        assertEquals("Wrong password", ACCOUNT_WILL_PASSWORD, dummyAccount.getPassword());

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)

        checkUniqueness(shadowProvisioningFutureAsserter.getObject());

        assertSteadyResources();
    }

    /**
     * Attempt to refresh dead shadow. Nothing should happen.
     */
    @Test
    public void test109RefreshAccountMorganDead() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT17M");

        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(ACCOUNT_MORGAN_OID);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        syncServiceMock.assertNoNotifcations();

        assertMorganDead();
    }

    /**
     * Try to add morgan account again. New shadow should be created.
     */
    @Test
    public void test110AddAccountMorganAgainCommunicationFailure() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_MORGAN_FILE);
        // Reset morgan OID. We cannot use the same OID, as there is a dead shadow with
        // the original OID.
        account.setOid(null);
        account.checkConsistence();
        display("Adding shadow", account);

        lastRequestStartTs = lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        shadowMorganOid = provisioningService.addObject(account, null, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertInProgress(result);
        account.checkConsistence();
        lastRequestEndTs = lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifyInProgressOnly();

        assertUncreatedMorgan(1);

        assertSteadyResources();
    }

    /**
     * Refresh while the resource is down. Retry interval is not yet reached.
     * Nothing should really happen yet.
     */
    @Test
    public void test114RefreshAccountMorganCommunicationFailure() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        clockForward("PT5M");

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        syncServiceMock.assertNoNotifcations();

        assertUncreatedMorgan(1);

        assertSteadyResources();
    }

    /**
     * Wait for retry interval to pass. Now provisioning should retry add operation.
     * Resource is up now and the operation can proceed.
     */
    @Test
    public void test116RefreshAccountMorganRetrySuccess() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT17M");

        syncServiceMock.reset();

        dummyResource.resetBreakMode();

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifySuccessOnly();

        assertCreatedMorgan(2);

        // Resource -> up
        assertResourceStatusChangeCounterIncrements();
        assertSteadyResources();
    }

    @Test
    public void test120ModifyMorganFullNameCommunicationFailure() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);
        lastRequestStartTs = lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                shadowMorganOid, dummyResourceCtl.getAttributeFullnamePath(), ACCOUNT_MORGAN_FULLNAME_HM);
        display("ObjectDelta", delta);

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                null, null, task, result);

        // THEN
        then();
        assertInProgress(result);
        lastRequestEndTs = lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifyInProgressOnly();

        assertUnmodifiedMorgan(1, 2, ACCOUNT_MORGAN_FULLNAME_HM);

        // Resource -> down
        assertResourceStatusChangeCounterIncrements();
        assertSteadyResources();
    }

    /**
     * Refresh while the resource is down. Retry interval is not yet reached.
     * Nothing should really happen yet.
     */
    @Test
    public void test124RefreshAccountMorganCommunicationFailure() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        syncServiceMock.assertNoNotifcations();

        assertUnmodifiedMorgan(1, 2, ACCOUNT_MORGAN_FULLNAME_HM);

        assertSteadyResources();
    }

    /**
     * Wait for retry interval to pass. Now provisioning should retry the operation.
     * But no luck yet. Resource is still down.
     */
    @Test
    public void test126RefreshAccountMorganCommunicationFailureRetry() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT17M");

        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        result.computeStatus();
        TestUtil.assertResultStatus(result, OperationResultStatus.HANDLED_ERROR);
        lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifyInProgressOnly();

        assertUnmodifiedMorgan(2, 2, ACCOUNT_MORGAN_FULLNAME_HM);

        assertSteadyResources();
    }

    /**
     * Wait for yet another retry interval to pass. Now provisioning should retry the operation
     * again. Still no luck. As this is the third and last attempt the operation should now be
     * completed.
     */
    @Test
    public void test128RefreshAccountMorganCommunicationFailureRetryAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT17M");

        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        result.computeStatus();
        TestUtil.assertResultStatus(result, OperationResultStatus.HANDLED_ERROR);
        lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifyFailureOnly();

        assertMorganModifyFailed();

    }

    /**
     * Attempt to refresh shadow with failed operation. Nothing should happen.
     */
    @Test
    public void test129RefreshAccountMorganFailed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT17M");

        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        syncServiceMock.assertNoNotifcations();

        assertMorganModifyFailed();
    }

    @Test
    public void test130ModifyMorganFullNameAgainCommunicationFailure() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();
        dummyResource.setBreakMode(BreakMode.NETWORK);

        lastRequestStartTs = lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                shadowMorganOid, dummyResourceCtl.getAttributeFullnamePath(), ACCOUNT_MORGAN_FULLNAME_CHM);
        display("ObjectDelta", delta);

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                null, null, task, result);

        // THEN
        then();
        assertInProgress(result);
        lastRequestEndTs = lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifyInProgressOnly();

        assertUnmodifiedMorgan(1, 3, ACCOUNT_MORGAN_FULLNAME_CHM);

        assertSteadyResources();
    }

    /**
     * Wait for retry interval to pass. Get account, but do NOT use forceRefresh option.
     * Nothing should happen.
     * Resource is still down.
     * Get operation should return repository shadow because staleness option is null.
     * Partial error should be indicated.
     */
    @Test
    public void test132GetAccountMorganCommunicationFailure() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT17M");

        syncServiceMock.reset();
        dummyResource.setBreakMode(BreakMode.NETWORK);

        // WHEN
        when();

        provisioningService.getObject(ShadowType.class, shadowMorganOid, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertPartialError(result);
        syncServiceMock.assertNoNotifcations();

        assertUnmodifiedMorgan(1, 3, ACCOUNT_MORGAN_FULLNAME_CHM);

        assertSteadyResources();
    }

    /**
     * Get account, but do NOT use forceRefresh option.
     * Nothing should happen.
     * Resource is still down.
     * Get operation should throw an error, as there is explicit staleness=0 option.
     * MID-4796
     */
    @Test(enabled = false) // MID-4796
    public void test133GetAccountMorganStalenessZeroCommunicationFailure() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();
        dummyResource.setBreakMode(BreakMode.NETWORK);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createStaleness(0L));

        try {
            // WHEN
            when();

            provisioningService.getObject(ShadowType.class, shadowMorganOid, options, task, result);

            assertNotReached();

        } catch (CommunicationException e) {
            // expected
        }

        // THEN
        then();
        display("Result", result);
        assertFailure(result);
        syncServiceMock.assertNoNotifcations();

        assertUnmodifiedMorgan(1, 3, ACCOUNT_MORGAN_FULLNAME_CHM);

        assertSteadyResources();
    }

    /**
     * Use forceRefresh option with get operation to force refresh.
     * We are over retry interval, therefore provisioning should re-try the operation.
     * Resource is still down.
     */
    @Test
    public void test134GetAccountMorganForceRefreshRetryCommunicationFailure() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();
        dummyResource.setBreakMode(BreakMode.NETWORK);

        lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createForceRefresh());

        // WHEN
        when();
        provisioningService.getObject(ShadowType.class, shadowMorganOid, options, task, result);

        // THEN
        then();
        display("Result", result);
        assertPartialError(result);
        lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifyInProgressOnly();

        assertUnmodifiedMorgan(2, 3, ACCOUNT_MORGAN_FULLNAME_CHM);

        assertSteadyResources();
    }

    /**
     * Wait for yet another retry interval to pass. Now provisioning should retry the operation.
     * Resource is up now and the operation can proceed.
     */
    @Test
    public void test136RefreshAccountMorganRetrySuccess() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT17M");

        syncServiceMock.reset();

        dummyResource.resetBreakMode();

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifySuccessOnly();

        assertModifiedMorgan(3, 3, ACCOUNT_MORGAN_FULLNAME_CHM);

        // Resource -> up
        assertResourceStatusChangeCounterIncrements();
        assertSteadyResources();
    }

    @Test
    public void test170DeleteMorganCommunicationFailure() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);
        lastRequestStartTs = lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                shadowMorganOid, dummyResourceCtl.getAttributeFullnamePath(), ACCOUNT_MORGAN_FULLNAME_HM);
        display("ObjectDelta", delta);

        // WHEN
        when();
        provisioningService.deleteObject(ShadowType.class, shadowMorganOid, null, null, task, result);

        // THEN
        then();
        assertInProgress(result);
        lastRequestEndTs = lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifyInProgressOnly();

        assertUndeletedMorgan(1, 4);

        // Resource -> down
        assertResourceStatusChangeCounterIncrements();
        assertSteadyResources();
    }

    /**
     * Refresh while the resource is down. Retry interval is not yet reached.
     * Nothing should really happen yet.
     */
    @Test
    public void test174RefreshAccountMorganCommunicationFailure() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        syncServiceMock.assertNoNotifcations();

        assertUndeletedMorgan(1, 4);

        assertSteadyResources();
    }

    /**
     * Wait for retry interval to pass. Now provisioning should retry the operation.
     * But no luck yet. Resource is still down.
     */
    @Test
    public void test176RefreshAccountMorganCommunicationFailureRetry() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT17M");

        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        result.computeStatus();
        TestUtil.assertResultStatus(result, OperationResultStatus.HANDLED_ERROR);
        lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifyInProgressOnly();

        assertUndeletedMorgan(2, 4);

        assertSteadyResources();
    }

    /**
     * Wait for yet another retry interval to pass. Now provisioning should retry the operation
     * again. Still no luck. As this is the third and last attempt the operation should now be
     * completed.
     */
    @Test
    public void test178RefreshAccountMorganCommunicationFailureRetryAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT17M");

        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        result.computeStatus();
        TestUtil.assertResultStatus(result, OperationResultStatus.HANDLED_ERROR);
        lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifyFailureOnly();

        assertMorganDeleteFailed();
    }

    /**
     * Attempt to refresh shadow with failed operation. Nothing should happen.
     */
    @Test
    public void test179RefreshAccountMorganFailed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT17M");

        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        syncServiceMock.assertNoNotifcations();

        assertMorganDeleteFailed();
    }

    @Test
    public void test180DeleteMorganCommunicationFailureAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        dummyResource.setBreakMode(BreakMode.NETWORK);
        lastRequestStartTs = lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                shadowMorganOid, dummyResourceCtl.getAttributeFullnamePath(), ACCOUNT_MORGAN_FULLNAME_HM);
        display("ObjectDelta", delta);

        // WHEN
        when();
        PrismObject<ShadowType> returnedShadow = provisioningService.deleteObject(ShadowType.class, shadowMorganOid, null, null, task, result);

        // THEN
        then();
        assertInProgress(result);
        lastRequestEndTs = lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifyInProgressOnly();

        assertUndeletedMorgan(1, 5);

        assertNotNull("No shadow returned from delete", returnedShadow);
//        ShadowAsserter.forShadow(returnedShadow, "returned shadow");

        assertSteadyResources();
    }

    /**
     * Wait for retry interval to pass. Now provisioning should retry the operation.
     * Resource is up now and the operation can proceed.
     */
    @Test
    public void test186RefreshAccountMorganRetrySuccess() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT17M");

        syncServiceMock.reset();

        dummyResource.resetBreakMode();

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();
        syncServiceMock.assertNotifySuccessOnly();

        assertDeletedMorgan(2, 5);

        // Resource -> up
        assertResourceStatusChangeCounterIncrements();
        assertSteadyResources();
    }

    /**
     * Refreshing dead shadow after pending operation is expired.
     * Pending operation should be gone.
     * This is the original dead shadow from test10x which is
     * a result of failed add operation.
     * MID-3891
     */
    @Test
    public void test190AccountMorganDeadExpireOperation() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("P1D");

        syncServiceMock.reset();

        dummyResource.resetBreakMode();

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(ACCOUNT_MORGAN_OID);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        syncServiceMock.assertNoNotifcations();

        // @formatter:off
        assertRepoShadow(ACCOUNT_MORGAN_OID)
            .assertKind(ShadowKindType.ACCOUNT)
            .assertDead()
            .assertIsNotExists()
            .assertNoLegacyConsistency()
            .attributes()
                .assertAttributes(SchemaConstants.ICFS_NAME)
                .end()
            .pendingOperations()
                .assertNone();

        assertShadowNoFetch(ACCOUNT_MORGAN_OID)
            .assertDead()
            .assertIsNotExists()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertNoPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(1)
                .end()
            .pendingOperations()
                .assertNone();

        ShadowAsserter<Void> asserterShadowFuture = assertShadowFuture(ACCOUNT_MORGAN_OID)
            .assertTombstone()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertNoPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end();

        assertShadowFutureNoFetch(ACCOUNT_MORGAN_OID)
            .assertTombstone()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertNoPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end();
        // @formatter:on

        dummyResource.resetBreakMode();

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)

        checkUniqueness(asserterShadowFuture.getObject());

        assertSteadyResources();
    }

    /**
     * Refreshing dead shadow after pending operation is expired.
     * Pending operation should be gone.
     * This is the original dead shadow from test18x which is a result
     * of delete operation.
     * MID-3891
     */
    @Test
    public void test192AccountMorganSecondDeadExpireOperation() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("P1D");

        syncServiceMock.reset();

        dummyResource.resetBreakMode();

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        syncServiceMock.assertNoNotifcations();

        // @formatter:off
        assertRepoShadow(shadowMorganOid)
            .assertKind(ShadowKindType.ACCOUNT)
            .assertTombstone()
            .assertNoLegacyConsistency()
            .attributes()
                .assertAttributes(SchemaConstants.ICFS_NAME, SchemaConstants.ICFS_UID)
                .end()
            .pendingOperations()
                .assertNone();

        assertShadowNoFetch(shadowMorganOid)
            .assertTombstone()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2)
                .end()
            .pendingOperations()
                .assertNone();

        assertShadowProvisioning(shadowMorganOid)
            .assertTombstone();

        assertShadowFuture(shadowMorganOid)
            .assertTombstone();

        assertShadowFutureNoFetch(shadowMorganOid)
            .assertTombstone();
        // @formatter:on

        dummyResource.resetBreakMode();

        assertSteadyResources();
    }

    /**
     * Refresh of dead shadow after the shadow itself expired. The shadow should be gone.
     * This is the original dead shadow from test10x which is
     * a result of failed add operation.
     * MID-3891
     */
    @Test
    public void test194AccountMorganDeadExpireShadow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("P10D");

        syncServiceMock.reset();

        dummyResource.resetBreakMode();

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(ACCOUNT_MORGAN_OID);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        syncServiceMock.assertNotifySuccessOnly();

        assertNoRepoObject(ShadowType.class, ACCOUNT_MORGAN_OID);

        assertSteadyResources();
    }

    /**
     * Refresh of dead shadow after the shadow itself expired. The shadow should be gone.
     * This is the original dead shadow from test18x which is a result
     * of delete operation.
     * MID-3891
     */
    @Test
    public void test196AccountMorganSecondDeadExpireShadow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("P1D");

        syncServiceMock.reset();

        dummyResource.resetBreakMode();

        PrismObject<ShadowType> shadowRepoBefore = getShadowRepo(shadowMorganOid);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowRepoBefore, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        syncServiceMock.assertNotifySuccessOnly();

        assertNoRepoObject(ShadowType.class, shadowMorganOid);

        assertSteadyResources();
    }

    /**
     * Try to add account for Captain Morgan. But there is already conflicting account on
     * the resource (J.P. Morgan). MidPoint does not know that and there is no shadow for it.
     * The operation should fail. But a new shadow should be created for that discovered
     * morgan account. And a proper synchronization notification should be called.
     * MID-3891
     */
    @Test
    public void test800AddAccountMorganAlreadyExists() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();
        dummyResource.resetBreakMode();

        dummyResourceCtl.addAccount(ACCOUNT_MORGAN_NAME, ACCOUNT_JP_MORGAN_FULLNAME);

        PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_MORGAN_FILE);
        account.checkConsistence();
        display("Adding shadow", account);

        // WHEN
        when();
        try {
            provisioningService.addObject(account, null, null, task, result);
            assertNotReached();
        } catch (ObjectAlreadyExistsException e) {
            then();
            display("expected exception", e);
        }

        // THEN
        display("Result", result);
        assertFailure(result);
        account.checkConsistence();

        PrismObject<ShadowType> conflictingShadowRepo = findAccountShadowByUsername(ACCOUNT_MORGAN_NAME, getResource(), result);
        assertNotNull("Shadow for conflicting object was not created in the repository", conflictingShadowRepo);
        // @formatter:off
        ShadowAsserter.forShadow(conflictingShadowRepo,"confligting repo shadow")
            .display()
            .assertBasicRepoProperties()
            .assertOidDifferentThan(shadowMorganOid)
            .assertName(ACCOUNT_MORGAN_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .assertNotDead()
            .assertIsExists()
            .pendingOperations()
                .assertNone();

        syncServiceMock
            .assertNotifyFailureOnly()
            .assertNotifyChange()
            .assertNotifyChangeCalls(1)
            .lastNotifyChange()
                .display()
                .assertNoDelta()
                .assertNoOldShadow()
                .assertUnrelatedChange(false)
                .assertProtected(false)
                .currentShadow()
                    .assertOid(conflictingShadowRepo.getOid())
                    .assertOidDifferentThan(shadowMorganOid)
                    .assertName(ACCOUNT_MORGAN_NAME)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertNotDead()
                    .assertIsExists()
                    .attributes()
                        .assertHasPrimaryIdentifier()
                        .assertHasSecondaryIdentifier()
                        .assertValue(dummyResourceCtl.getAttributeFullnameQName(), ACCOUNT_JP_MORGAN_FULLNAME)
                        .end()
                    .pendingOperations()
                        .assertNone();
        // @formatter:on

        shadowMorganOid = conflictingShadowRepo.getOid();

        assertNoRepoShadow(ACCOUNT_MORGAN_OID);
        assertSteadyResources();
    }

    /**
     * Same add attempt than before. But this time the conflicting shadow exists.
     * MID-3891
     */
    @Test
    public void test802AddAccountMorganAlreadyExistsAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();
        dummyResource.resetBreakMode();

        PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_MORGAN_FILE);
        account.checkConsistence();
        display("Adding shadow", account);

        // WHEN
        when();
        try {
            provisioningService.addObject(account, null, null, task, result);
            assertNotReached();
        } catch (ObjectAlreadyExistsException e) {
            then();
            display("expected exception", e);
        }

        // THEN
        display("Result", result);
        assertFailure(result);
        account.checkConsistence();

        // @formatter:off
        PrismObject<ShadowType> conflictingShadowRepo = getShadowRepo(shadowMorganOid);
        ShadowAsserter.forShadow(conflictingShadowRepo,"conflicting repo shadow")
            .display()
            .assertBasicRepoProperties()
            .assertOid(shadowMorganOid)
            .assertName(ACCOUNT_MORGAN_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .assertNotDead()
            .assertIsExists()
            .pendingOperations()
                .assertNone();

        syncServiceMock
            .assertNotifyFailureOnly()
            .assertNotifyChange()
            .assertNotifyChangeCalls(1)
            .lastNotifyChange()
                .display()
                .assertNoDelta()
                .oldShadow()
                    .assertOid(shadowMorganOid)
                    .assertName(ACCOUNT_MORGAN_NAME)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertNotDead()
                    .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
                    .assertIsExists()
                    .attributes()
                        .assertHasPrimaryIdentifier()
                        .assertHasSecondaryIdentifier()
                        .end()
                    .pendingOperations()
                        .assertNone()
                        .end()
                    .end()
                .assertUnrelatedChange(false)
                .assertProtected(false)
                .currentShadow()
                    .assertOid(shadowMorganOid)
                    .assertName(ACCOUNT_MORGAN_NAME)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertNotDead()
                    .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
                    .assertIsExists()
                    .attributes()
                        .assertHasPrimaryIdentifier()
                        .assertHasSecondaryIdentifier()
                        .assertValue(dummyResourceCtl.getAttributeFullnameQName(), ACCOUNT_JP_MORGAN_FULLNAME)
                        .end()
                    .pendingOperations()
                        .assertNone();
        // @formatter:on

        assertNoRepoShadow(ACCOUNT_MORGAN_OID);
        assertSteadyResources();
    }

    @Test
    public void test804AddAccountElizabeth() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();
        dummyResource.resetBreakMode();

        PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_ELIZABETH_FILE);
        account.checkConsistence();
        display("Adding shadow", account);

        // WHEN
        when();
        provisioningService.addObject(account, null, null, task, result);

        // THEN
        then();
        assertSuccess(result);
        account.checkConsistence();

        dummyResourceCtl.assertAccountByUsername(ACCOUNT_ELIZABETH_USERNAME)
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_ELIZABETH_FULLNAME);

        assertSteadyResources();
    }

    /**
     * Try to rename Elizabeth to Betty. But there is already Betty account on the resource.
     * MidPoint does not know anything about it (no shadow).
     */
    @Test
    public void test806RenameAccountElizabethAlreadyExists() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();
        dummyResource.resetBreakMode();

        dummyResourceCtl.addAccount(ACCOUNT_BETTY_USERNAME, ACCOUNT_BETTY_FULLNAME);

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_ELIZABETH_OID, ItemPath.create(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME),
                ACCOUNT_BETTY_USERNAME);

        // WHEN
        when();
        try {
            provisioningService.modifyObject(ShadowType.class, ACCOUNT_ELIZABETH_OID, delta.getModifications(), null, null, task, result);
            assertNotReached();
        } catch (ObjectAlreadyExistsException e) {
            then();
            display("expected exception", e);
        }

        // THEN
        then();
        assertFailure(result);

        PrismObject<ShadowType> conflictingShadowRepo = findAccountShadowByUsername(ACCOUNT_BETTY_USERNAME, getResource(), result);
        assertNotNull("Shadow for conflicting object was not created in the repository", conflictingShadowRepo);
        // @formatter:off
        ShadowAsserter.forShadow(conflictingShadowRepo,"conflicting repo shadow")
            .display()
            .assertBasicRepoProperties()
            .assertOidDifferentThan(ACCOUNT_ELIZABETH_OID)
            .assertName(ACCOUNT_BETTY_USERNAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .pendingOperations()
            .assertNone();

        syncServiceMock
            .assertNotifyFailureOnly()
            .assertNotifyChange()
            .assertNotifyChangeCalls(1)
            .lastNotifyChange()
                .display()
                .assertNoDelta()
                .assertNoOldShadow()
                .assertUnrelatedChange(false)
                .assertProtected(false)
                .currentShadow()
                    .assertOid(conflictingShadowRepo.getOid())
                    .assertOidDifferentThan(ACCOUNT_ELIZABETH_OID)
                    .assertName(ACCOUNT_BETTY_USERNAME)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertNotDead()
                    .assertIsExists()
                    .assertPrimaryIdentifierValue(ACCOUNT_BETTY_USERNAME)
                    .attributes()
                        .assertHasPrimaryIdentifier()
                        .assertHasSecondaryIdentifier()
                        .assertValue(dummyResourceCtl.getAttributeFullnameQName(), ACCOUNT_BETTY_FULLNAME)
                        .end()
                    .pendingOperations()
                        .assertNone();

        dummyResourceCtl.assertAccountByUsername(ACCOUNT_ELIZABETH_USERNAME)
            .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_ELIZABETH_FULLNAME);

        dummyResourceCtl.assertAccountByUsername(ACCOUNT_BETTY_USERNAME)
            .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_BETTY_FULLNAME);
        // @formatter:on

        assertSteadyResources();
    }

    /**
     * Same rename attempt as before. But this time the conflicting shadow exists.
     */
    @Test
    public void test808RenameAccountElizabethAlreadyExistsAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();
        dummyResource.resetBreakMode();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_ELIZABETH_OID, ItemPath.create(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME),
                ACCOUNT_BETTY_USERNAME);

        // WHEN
        when();
        try {
            provisioningService.modifyObject(ShadowType.class, ACCOUNT_ELIZABETH_OID, delta.getModifications(), null, null, task, result);
            assertNotReached();
        } catch (ObjectAlreadyExistsException e) {
            then();
            display("expected exception", e);
        }

        // THEN
        then();
        assertFailure(result);

        PrismObject<ShadowType> conflictingShadowRepo = findAccountShadowByUsername(ACCOUNT_BETTY_USERNAME, getResource(), result);
        assertNotNull("Shadow for conflicting object was not created in the repository", conflictingShadowRepo);
        // @formatter:off
        ShadowAsserter.forShadow(conflictingShadowRepo, "conflicting repo shadow")
            .display()
            .assertBasicRepoProperties()
            .assertOidDifferentThan(ACCOUNT_ELIZABETH_OID)
            .assertName(ACCOUNT_BETTY_USERNAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .pendingOperations()
            .assertNone();

        syncServiceMock
            .assertNotifyFailureOnly()
            .assertNotifyChange()
            .assertNotifyChangeCalls(1)
            .lastNotifyChange()
                .display()
                .assertNoDelta()
                .oldShadow()
                    .assertOid(conflictingShadowRepo.getOid())
                    .assertOidDifferentThan(ACCOUNT_ELIZABETH_OID)
                    .assertName(ACCOUNT_BETTY_USERNAME)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .attributes()
                        .assertHasPrimaryIdentifier()
                        .assertHasSecondaryIdentifier()
                        .end()
                    .pendingOperations()
                        .assertNone()
                        .end()
                    .end()
                .assertUnrelatedChange(false)
                .assertProtected(false)
                .currentShadow()
                    .assertOid(conflictingShadowRepo.getOid())
                    .assertOidDifferentThan(ACCOUNT_ELIZABETH_OID)
                    .assertName(ACCOUNT_BETTY_USERNAME)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertNotDead()
                    .assertIsExists()
                    .assertPrimaryIdentifierValue(ACCOUNT_BETTY_USERNAME)
                    .attributes()
                        .assertHasPrimaryIdentifier()
                        .assertHasSecondaryIdentifier()
                        .assertValue(dummyResourceCtl.getAttributeFullnameQName(), ACCOUNT_BETTY_FULLNAME)
                        .end()
                    .pendingOperations()
                        .assertNone();

        dummyResourceCtl.assertAccountByUsername(ACCOUNT_ELIZABETH_USERNAME)
            .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_ELIZABETH_FULLNAME);

        dummyResourceCtl.assertAccountByUsername(ACCOUNT_BETTY_USERNAME)
            .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_BETTY_FULLNAME);
        // @formatter:on

        assertSteadyResources();
    }

    /**
     * Try to get account morgan. But the account has disappeared from the resource
     * in the meantime. MidPoint does not know anything about this (shadow still exists).
     * Proper notification should be called.
     * MID-3891
     */
    @Test
    public void test810GetAccountMorganNotFound() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();
        dummyResource.resetBreakMode();
        dummyResourceCtl.deleteAccount(ACCOUNT_MORGAN_NAME);

        // WHEN
        when();
        PrismObject<ShadowType> provisioningShadow = provisioningService.getObject(ShadowType.class, shadowMorganOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        syncServiceMock
            .assertNotifyChange()
            .assertNotifyChangeCalls(1)
            .lastNotifyChange()
                .display()
                .assertUnrelatedChange(false)
                .assertProtected(false)
                .delta()
                    .assertDelete()
                    .end()
                .oldShadow()
                    .assertOid(shadowMorganOid)
                    .assertName(ACCOUNT_MORGAN_NAME)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertTombstone()
                    .attributes()
                        .assertHasPrimaryIdentifier()
                        .assertHasSecondaryIdentifier()
                        .end()
                    .pendingOperations()
                        .assertNone()
                        .end()
                    .end()
                .currentShadow()
                    .assertOid(shadowMorganOid)
                    .assertName(ACCOUNT_MORGAN_NAME)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertTombstone();

        assertRepoShadow(shadowMorganOid)
            .assertTombstone()
            .pendingOperations()
                .assertNone();

        ShadowAsserter.forShadow(provisioningShadow, "provisioning")
            .assertTombstone();
        // @formatter:on

        assertNoRepoShadow(ACCOUNT_MORGAN_OID);
        assertSteadyResources();
    }

    /**
     * Try to modify account will. But the account has disappeared from the resource
     * in the meantime. MidPoint does not know anything about this (shadow still exists).
     * Proper notification should be called.
     * MID-3891
     */
    @Test
    public void test812ModifyAccountWillNotFound() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();
        dummyResource.resetBreakMode();
        dummyResourceCtl.deleteAccount(ACCOUNT_WILL_USERNAME);

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, dummyResourceCtl.getAttributeFullnamePath(), "Pirate Will Turner");

        // WHEN
        when();
        try {
            provisioningService.modifyObject(ShadowType.class, ACCOUNT_WILL_OID, delta.getModifications(), null, null, task, result);
            assertNotReached();
        } catch (ObjectNotFoundException e) {
            then();
            display("expected exception", e);
        }

        // THEN
        display("Result", result);
        assertFailure(result);

        // @formatter:off
        syncServiceMock
            .assertNotifyChange()
            .assertNotifyChangeCalls(1)
            .lastNotifyChange()
                .display()
                .assertUnrelatedChange(false)
                .assertProtected(false)
                .delta()
                    .assertDelete()
                    .end()
                .oldShadow()
                    .assertOid(ACCOUNT_WILL_OID)
                    .assertName(ACCOUNT_WILL_USERNAME)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertTombstone()
                    .attributes()
                        .assertHasPrimaryIdentifier()
                        .assertHasSecondaryIdentifier()
                        .end()
                    .pendingOperations()
                        .assertNone()
                        .end()
                    .end()
                .currentShadow()
                    .assertOid(ACCOUNT_WILL_OID)
                    .assertName(ACCOUNT_WILL_USERNAME)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertTombstone();

        assertRepoShadow(ACCOUNT_WILL_OID)
            .assertDead()
            .assertIsNotExists()
            .pendingOperations()
                .assertNone();
        // @formatter:on

        assertSteadyResources();
    }

    /**
     * Try to delete account elizabeth. But the account has disappeared from the resource
     * in the meantime. MidPoint does not know anything about this (shadow still exists).
     * Proper notification should be called.
     * MID-3891
     */
    @Test
    public void test814DeleteAccountElizabethNotFound() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();
        dummyResource.resetBreakMode();
        dummyResourceCtl.deleteAccount(ACCOUNT_ELIZABETH_USERNAME);

        // WHEN
        when();
        provisioningService.deleteObject(ShadowType.class, ACCOUNT_ELIZABETH_OID, null, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertHadnledError(result);

        // @formatter:off
        syncServiceMock
            .assertNotifyChange()
            .assertNotifyChangeCalls(1)
            .lastNotifyChange()
                .display()
                .assertUnrelatedChange(false)
                .assertProtected(false)
                .delta()
                    .assertDelete()
                    .end()
                .oldShadow()
                    .assertOid(ACCOUNT_ELIZABETH_OID)
                    .assertName(ACCOUNT_ELIZABETH_USERNAME)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertTombstone()
                    .attributes()
                        .assertHasPrimaryIdentifier()
                        .assertHasSecondaryIdentifier()
                        .end()
                    .pendingOperations()
                        .assertNone()
                        .end()
                    .end()
                .currentShadow()
                    .assertOid(ACCOUNT_ELIZABETH_OID)
                    .assertName(ACCOUNT_ELIZABETH_USERNAME)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertTombstone();

        assertRepoShadow(ACCOUNT_ELIZABETH_OID)
            .assertDead()
            .assertIsNotExists()
            .pendingOperations()
                .assertNone();
        // @formatter:on

        assertSteadyResources();
    }

    /**
     * We try to add elizabeth account while there is still a dead shadow for that.
     * There is also new conflicting elizabeth account on the resource that midPoint
     * does not know about.
     */
    @Test
    public void test816AddAccountElizabethAfterDeathAlreadyExists() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();
        dummyResource.resetBreakMode();

        dummyResourceCtl.addAccount(ACCOUNT_ELIZABETH_USERNAME, ACCOUNT_ELIZABETH2_FULLNAME);

        PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_ELIZABETH_FILE);
        account.setOid(null);
        account.checkConsistence();
        display("Adding shadow", account);

        // WHEN
        when();
        try {
            provisioningService.addObject(account, null, null, task, result);
        } catch (ObjectAlreadyExistsException e) {
            then();
            display("expected exception", e);
        }

        // THEN
        then();
        assertFailure(result);

        // @formatter:off
        syncServiceMock
            .assertNotifyFailureOnly()
            .assertNotifyChange()
            .assertNotifyChangeCalls(1)
            .lastNotifyChange()
                .display()
                .assertNoDelta()
                .assertNoOldShadow()
                .assertUnrelatedChange(false)
                .assertProtected(false)
                .currentShadow()
                    .assertOidDifferentThan(ACCOUNT_ELIZABETH_OID)
                    .assertName(ACCOUNT_ELIZABETH_USERNAME)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertNotDead()
                    .assertIsExists()
                    .assertPrimaryIdentifierValue(ACCOUNT_ELIZABETH_USERNAME)
                    .attributes()
                        .assertHasPrimaryIdentifier()
                        .assertHasSecondaryIdentifier()
                        .assertValue(dummyResourceCtl.getAttributeFullnameQName(), ACCOUNT_ELIZABETH2_FULLNAME)
                        .end()
                    .pendingOperations()
                        .assertNone();
        // @formatter:on

        dummyResourceCtl.assertAccountByUsername(ACCOUNT_ELIZABETH_USERNAME)
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_ELIZABETH2_FULLNAME);

        assertSteadyResources();
    }

    // TODO: test no discovery options

    /**
     * Pending operation (was: legacy consistency items, MID-5076).
     * TODO consider removing this test.
     */
    @Test
    public void test900GetAccountMurrayPending() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();
        dummyResource.resetBreakMode();

        dummyResourceCtl.addAccount(ACCOUNT_MURRAY_USERNAME, ACCOUNT_MURRAY_FULL_NAME);
        repoAddObjectFromFile(ACCOUNT_SHADOW_MURRAY_PENDING_FILE, result);

        // WHEN
        when();
        PrismObject<ShadowType> accountMurray = provisioningService.getObject(ShadowType.class, ACCOUNT_SHADOW_MURRAY_PENDING_OID, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result);
        accountMurray.checkConsistence();

        // TODO: assert Murray

        assertSteadyResources();
    }

    private void assertUncreatedMorgan(int expectedAttemptNumber) throws Exception {

        // @formatter:off
        assertRepoShadow(shadowMorganOid)
            .assertKind(ShadowKindType.ACCOUNT)
            .assertIsNotExists()
            .assertNotDead()
            // We have name (secondary identifier), but we do not have primary identifier yet.
            // We will get that only when create operation is successful.
            .assertNoPrimaryIdentifierValue()
            .assertNoLegacyConsistency()
            .attributes()
                .assertAttributes(SchemaConstants.ICFS_NAME)
                .end()
            .pendingOperations()
                .singleOperation()
                    .display()
                    .assertType(PendingOperationTypeType.RETRY)
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(expectedAttemptNumber)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .display()
                        .assertAdd();

        assertShadowNoFetch(shadowMorganOid)
            .assertIsNotExists()
            .assertNotDead()
            .assertNoPrimaryIdentifierValue()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertNoPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(1)
                .end()
            .pendingOperations()
                .singleOperation()
                    .assertType(PendingOperationTypeType.RETRY)
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(expectedAttemptNumber)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .assertAdd();

        ShadowAsserter<Void> asserterFuture = assertShadowFuture(shadowMorganOid)
            .assertIsExists()
            .assertNotDead()
            .assertNoPrimaryIdentifierValue()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertNoPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(5)
                .assertValue(dummyResourceCtl.getAttributeFullnameQName(), ACCOUNT_MORGAN_FULLNAME)
                .end();
        // @formatter:on

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        checkUniqueness(asserterFuture.getObject());
    }

    private void assertCreatedMorgan(int expectedAttemptNumber) throws Exception {

        // @formatter:off
        assertRepoShadow(shadowMorganOid)
            .assertKind(ShadowKindType.ACCOUNT)
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertAttributes(SchemaConstants.ICFS_NAME, SchemaConstants.ICFS_UID)
                .end()
            .pendingOperations()
                .singleOperation()
                    .display()
                    .assertType(PendingOperationTypeType.RETRY)
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(expectedAttemptNumber)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .assertCompletionTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .display()
                        .assertAdd();

        assertShadowNoFetch(shadowMorganOid)
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2)
                .end()
            .pendingOperations()
                .singleOperation()
                    .assertType(PendingOperationTypeType.RETRY)
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(expectedAttemptNumber)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .assertCompletionTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .assertAdd();

        assertShadowProvisioning(shadowMorganOid)
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(6)
                .assertValue(dummyResourceCtl.getAttributeFullnameQName(), ACCOUNT_MORGAN_FULLNAME);

        ShadowAsserter<Void> asserterFuture = assertShadowFuture(shadowMorganOid)
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(6)
                .assertValue(dummyResourceCtl.getAttributeFullnameQName(), ACCOUNT_MORGAN_FULLNAME)
                .end();
        // @formatter:on

        dummyResource.resetBreakMode();

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        checkUniqueness(asserterFuture.getObject());

        dummyResourceCtl.assertAccountByUsername(ACCOUNT_MORGAN_NAME)
                .assertName(ACCOUNT_MORGAN_NAME)
                .assertFullName(ACCOUNT_MORGAN_FULLNAME)
                .assertEnabled()
                .assertPassword(ACCOUNT_MORGAN_PASSWORD);
    }

    private void assertUnmodifiedMorgan(
            int expectedAttemptNumber, int expectenNumberOfPendingOperations, String expectedFullName)
            throws Exception {

        PrismObject<ShadowType> repoShadow = getShadowRepo(shadowMorganOid);
        assertNotNull("Shadow was not created in the repository", repoShadow);

        // @formatter:off
        ShadowAsserter<Void> shadowAsserter = ShadowAsserter.forShadow(repoShadow, "repository");
        shadowAsserter
            .display()
            .pendingOperations()
                .assertOperations(expectenNumberOfPendingOperations)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .find()
                    .display()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(expectedAttemptNumber)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .display()
                        .assertModify();
        shadowAsserter
            .assertBasicRepoProperties()
            .assertKind(ShadowKindType.ACCOUNT)
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertAttributes(SchemaConstants.ICFS_NAME, SchemaConstants.ICFS_UID);

        PrismObject<ShadowType> shadowNoFetch = getShadowNoFetch(shadowMorganOid);
        shadowAsserter = ShadowAsserter.forShadow(shadowNoFetch, "noFetch");
        shadowAsserter
            .display()
            .pendingOperations()
                .assertOperations(expectenNumberOfPendingOperations)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .find()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(expectedAttemptNumber)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .assertModify();
        shadowAsserter
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2);

        Task task = getTestTask();
        OperationResult result = createSubresult("assertUnmodifiedMorgan");
        PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(ShadowType.class, shadowMorganOid, null, task, result);
        assertPartialError(result);
        shadowAsserter = ShadowAsserter.forShadow(accountProvisioning, "current");
        shadowAsserter
            .display()
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2);
        // TODO: assert caching metadata?

        PrismObject<ShadowType> accountProvisioningFuture = getShadowFuturePartialError(shadowMorganOid);
        shadowAsserter = ShadowAsserter.forShadow(accountProvisioningFuture, "future");
        shadowAsserter
            .display()
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(3)
                .assertValue(dummyResourceCtl.getAttributeFullnameQName(), expectedFullName);
        // @formatter:on

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        checkUniqueness(accountProvisioningFuture);

        dummyResource.resetBreakMode();
        dummyResourceCtl.assertAccountByUsername(ACCOUNT_MORGAN_NAME)
                .assertName(ACCOUNT_MORGAN_NAME)
                .assertFullName(ACCOUNT_MORGAN_FULLNAME)
                .assertEnabled()
                .assertPassword(ACCOUNT_MORGAN_PASSWORD);
    }

    private void assertModifiedMorgan(
            int expectedAttemptNumber, int expectenNumberOfPendingOperations, String expectedFullName)
            throws Exception {

        PrismObject<ShadowType> repoShadow = getShadowRepo(shadowMorganOid);
        assertNotNull("Shadow was not created in the repository", repoShadow);

        // @formatter:off
        ShadowAsserter<Void> shadowAsserter = ShadowAsserter.forShadow(repoShadow, "repository");
        shadowAsserter
            .display()
            .pendingOperations()
                .assertOperations(expectenNumberOfPendingOperations)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .resultStatus(OperationResultStatusType.SUCCESS)
                    .changeType(ChangeTypeType.MODIFY)
                .find()
                    .display()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(expectedAttemptNumber)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .display()
                        .assertModify();
        shadowAsserter
            .assertBasicRepoProperties()
            .assertKind(ShadowKindType.ACCOUNT)
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertAttributes(SchemaConstants.ICFS_NAME, SchemaConstants.ICFS_UID);

        PrismObject<ShadowType> shadowNoFetch = getShadowNoFetch(shadowMorganOid);
        shadowAsserter = ShadowAsserter.forShadow(shadowNoFetch, "noFetch");
        shadowAsserter
            .display()
            .pendingOperations()
                .assertOperations(expectenNumberOfPendingOperations)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .resultStatus(OperationResultStatusType.SUCCESS)
                    .changeType(ChangeTypeType.MODIFY)
                .find()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(expectedAttemptNumber)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .assertModify();
        shadowAsserter
            .assertIsExists()
            .assertNotDead()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2);

        OperationResult result = createSubresult("assertModifiedMorgan");
        PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(
                ShadowType.class, shadowMorganOid, null, getTestTask(), result);
        assertSuccess(result);
        shadowAsserter = ShadowAsserter.forShadow(accountProvisioning, "current");
        shadowAsserter
            .display()
            .assertIsExists()
            .assertNotDead()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(6)
                .assertValue(dummyResourceCtl.getAttributeFullnameQName(), expectedFullName);

        PrismObject<ShadowType> accountProvisioningFuture = getShadowFuture(shadowMorganOid);
        shadowAsserter = ShadowAsserter.forShadow(accountProvisioningFuture, "future");
        shadowAsserter
            .display()
            .assertIsExists()
            .assertNotDead()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(6)
                .assertValue(dummyResourceCtl.getAttributeFullnameQName(), expectedFullName);
        // @formatter:on

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        checkUniqueness(accountProvisioningFuture);

        dummyResource.resetBreakMode();
        dummyResourceCtl.assertAccountByUsername(ACCOUNT_MORGAN_NAME)
                .assertName(ACCOUNT_MORGAN_NAME)
                .assertFullName(expectedFullName)
                .assertEnabled()
                .assertPassword(ACCOUNT_MORGAN_PASSWORD);
    }

    private void assertMorganModifyFailed() throws Exception {
        PrismObject<ShadowType> repoShadow = getShadowRepo(shadowMorganOid);
        assertNotNull("Shadow was not created in the repository", repoShadow);

        // @formatter:off
        ShadowAsserter<Void> shadowAsserter = ShadowAsserter.forShadow(repoShadow, "repository");
        shadowAsserter
            .display()
            .pendingOperations()
                .assertOperations(2)
                .modifyOperation()
                    .display()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(3)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .assertCompletionTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .display()
                        .assertModify();
        shadowAsserter
            .assertBasicRepoProperties()
            .assertKind(ShadowKindType.ACCOUNT)
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertAttributes(SchemaConstants.ICFS_NAME, SchemaConstants.ICFS_UID);

        PrismObject<ShadowType> shadowNoFetch = getShadowNoFetch(shadowMorganOid);
        shadowAsserter = ShadowAsserter.forShadow(shadowNoFetch, "noFetch");
        shadowAsserter
            .display()
            .pendingOperations()
                .assertOperations(2)
                .modifyOperation()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(3)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .assertCompletionTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .assertModify();
        shadowAsserter
            .assertIsExists()
            .assertNotDead()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2);

        OperationResult result = createSubresult("assertMorganModifyFailed");
        PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(
                ShadowType.class, shadowMorganOid, null, getTestTask(), result);
        assertPartialError(result);
        shadowAsserter = ShadowAsserter.forShadow(accountProvisioning, "current");
        shadowAsserter
            .display()
            .assertIsExists()
            .assertNotDead()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2);
        // TODO: assert caching metadata?

        PrismObject<ShadowType> accountProvisioningFuture = getShadowFuturePartialError(shadowMorganOid);
        shadowAsserter = ShadowAsserter.forShadow(accountProvisioningFuture,"future");
        shadowAsserter
            .display()
            .assertIsExists()
            .assertNotDead()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2);
        // @formatter:on

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        checkUniqueness(accountProvisioningFuture);

        dummyResource.resetBreakMode();
        dummyResourceCtl.assertAccountByUsername(ACCOUNT_MORGAN_NAME)
                .assertName(ACCOUNT_MORGAN_NAME)
                .assertFullName(ACCOUNT_MORGAN_FULLNAME)
                .assertEnabled()
                .assertPassword(ACCOUNT_MORGAN_PASSWORD);
    }

    private void assertUndeletedMorgan(int expectedAttemptNumber, int expectenNumberOfPendingOperations) throws Exception {

        PrismObject<ShadowType> repoShadow = getShadowRepo(shadowMorganOid);
        assertNotNull("Shadow was not created in the repository", repoShadow);

        // @formatter:off
        ShadowAsserter<Void> shadowAsserter = ShadowAsserter.forShadow(repoShadow, "repository");
        shadowAsserter
            .display()
            .pendingOperations()
                .assertOperations(expectenNumberOfPendingOperations)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .find()
                    .display()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(expectedAttemptNumber)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .display()
                        .assertDelete();
        shadowAsserter
            .assertBasicRepoProperties()
            .assertKind(ShadowKindType.ACCOUNT)
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertAttributes(SchemaConstants.ICFS_NAME, SchemaConstants.ICFS_UID);

        PrismObject<ShadowType> shadowNoFetch = getShadowNoFetch(shadowMorganOid);
        shadowAsserter = ShadowAsserter.forShadow(shadowNoFetch, "noFetch");
        shadowAsserter
            .display()
            .pendingOperations()
                .assertOperations(expectenNumberOfPendingOperations)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .find()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(expectedAttemptNumber)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .assertDelete();
        shadowAsserter
            .assertIsExists()
            .assertNotDead()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2);

        OperationResult result = createSubresult("assertUndeletedMorgan");
        PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(
                ShadowType.class, shadowMorganOid, null, getTestTask(), result);
        assertPartialError(result);
        shadowAsserter = ShadowAsserter.forShadow(accountProvisioning, "current");
        shadowAsserter
            .display()
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2);
        // TODO: assert caching metadata?

        PrismObject<ShadowType> accountProvisioningFuture = getShadowFuturePartialError(shadowMorganOid);
        shadowAsserter = ShadowAsserter.forShadow(accountProvisioningFuture, "future");
        shadowAsserter
            .display()
            .assertIsNotExists()
            .assertDead()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2);
        // @formatter:on

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        checkUniqueness(accountProvisioningFuture);

        dummyResource.resetBreakMode();
        dummyResourceCtl.assertAccountByUsername(ACCOUNT_MORGAN_NAME)
                .assertName(ACCOUNT_MORGAN_NAME)
                .assertFullName(ACCOUNT_MORGAN_FULLNAME_CHM)
                .assertEnabled()
                .assertPassword(ACCOUNT_MORGAN_PASSWORD);
    }

    private void assertMorganDeleteFailed() throws Exception {
        PrismObject<ShadowType> repoShadow = getShadowRepo(shadowMorganOid);
        assertNotNull("Shadow was not created in the repository", repoShadow);

        // @formatter:off
        ShadowAsserter<?> shadowAsserter = ShadowAsserter.forShadow(repoShadow, "repository");
        shadowAsserter
            .display()
            .pendingOperations()
                .assertOperations(4)
                .by()
                    .changeType(ChangeTypeType.DELETE)
                .find()
                    .display()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(3)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .display()
                        .assertDelete();
        shadowAsserter
            .assertBasicRepoProperties()
            .assertKind(ShadowKindType.ACCOUNT)
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertAttributes(SchemaConstants.ICFS_NAME, SchemaConstants.ICFS_UID);

        PrismObject<ShadowType> shadowNoFetch = getShadowNoFetch(shadowMorganOid);
        shadowAsserter = ShadowAsserter.forShadow(shadowNoFetch, "noFetch");
        shadowAsserter
            .display()
            .pendingOperations()
                .assertOperations(4)
                .by()
                    .changeType(ChangeTypeType.DELETE)
                .find()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(3)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .assertDelete();
        shadowAsserter
            .assertIsExists()
            .assertNotDead()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2);

        OperationResult result = createSubresult("assertMorganDeleteFailed");
        PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(
                ShadowType.class, shadowMorganOid, null, getTestTask(), result);
        assertPartialError(result);
        shadowAsserter = ShadowAsserter.forShadow(accountProvisioning, "current");
        shadowAsserter
            .display()
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2);
        // TODO: assert caching metadata?

        PrismObject<ShadowType> accountProvisioningFuture = getShadowFuturePartialError(shadowMorganOid);
        shadowAsserter = ShadowAsserter.forShadow(accountProvisioningFuture, "future");
        shadowAsserter
            .display()
            .assertIsExists()
            .assertNotDead()
            .assertPrimaryIdentifierValue(ACCOUNT_MORGAN_NAME)
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2);
        // @formatter:on

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        checkUniqueness(accountProvisioningFuture);

        dummyResource.resetBreakMode();
        dummyResourceCtl.assertAccountByUsername(ACCOUNT_MORGAN_NAME)
                .assertName(ACCOUNT_MORGAN_NAME)
                .assertFullName(ACCOUNT_MORGAN_FULLNAME_CHM)
                .assertEnabled()
                .assertPassword(ACCOUNT_MORGAN_PASSWORD);
    }

    private void assertDeletedMorgan(int expectedAttemptNumber, int expectedNumberOfPendingOperations) throws Exception {

        PrismObject<ShadowType> repoShadow = getShadowRepo(shadowMorganOid);
        assertNotNull("Shadow was not created in the repository", repoShadow);

        // @formatter:off
        ShadowAsserter<Void> shadowAsserter = ShadowAsserter.forShadow(repoShadow, "repository");
        shadowAsserter
            .display()
            .pendingOperations()
                .assertOperations(expectedNumberOfPendingOperations)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .resultStatus(OperationResultStatusType.SUCCESS)
                    .changeType(ChangeTypeType.DELETE)
                .find()
                    .display()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(expectedAttemptNumber)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .display()
                        .assertDelete();
        shadowAsserter
            .assertBasicRepoProperties()
            .assertKind(ShadowKindType.ACCOUNT)
            .assertTombstone()
            .assertNoLegacyConsistency()
            .attributes()
                .assertAttributes(SchemaConstants.ICFS_NAME, SchemaConstants.ICFS_UID);

        assertShadowNoFetch(shadowMorganOid)
            .assertTombstone()
            .assertNoLegacyConsistency()
            .attributes()
                .assertResourceAttributeContainer()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertSize(2)
                .end()
            .pendingOperations()
                .assertOperations(expectedNumberOfPendingOperations)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .resultStatus(OperationResultStatusType.SUCCESS)
                    .changeType(ChangeTypeType.DELETE)
                .find()
                    .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                    .assertAttemptNumber(expectedAttemptNumber)
                    .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                    .delta()
                        .assertDelete();
        // @formatter:on

        assertShadowProvisioning(shadowMorganOid)
                .assertTombstone();

        assertShadowFuture(shadowMorganOid)
                .assertTombstone();

        assertShadowFutureNoFetch(shadowMorganOid)
                .assertTombstone();

        dummyResource.resetBreakMode();
        dummyResourceCtl.assertNoAccountByUsername(ACCOUNT_MORGAN_NAME);
    }

    private void assertResourceStatusChangeCounterIncrements() {
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
    }

    private void assertGetUncreatedShadow(String oid)
            throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = createSubresult("assertGetUncreatedShadow");
        try {
            PrismObject<ShadowType> shadow = provisioningService.getObject(
                    ShadowType.class, oid, null, getTestTask(), result);
            fail("Expected that get of uncreated shadow fails, but it was successful: " + shadow);
        } catch (GenericConnectorException e) {
            // Expected
        }
    }

    private PrismObject<ShadowType> getShadowNoFetch(String oid)
            throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = createSubresult("getShadowNoFetch");
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class,
                oid, options, getTestTask(), result);
        assertSuccess(result);
        return shadow;
    }

    private PrismObject<ShadowType> getShadowFuture(String oid)
            throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = createSubresult("getShadowFuture");
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
        PrismObject<ShadowType> shadow = provisioningService.getObject(
                ShadowType.class, oid, options, getTestTask(), result);
        assertSuccess(result);
        return shadow;
    }

    private PrismObject<ShadowType> getShadowFuturePartialError(String oid)
            throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = createSubresult("getShadowFuturePartialError");
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
        PrismObject<ShadowType> shadow = provisioningService.getObject(
                ShadowType.class, oid, options, getTestTask(), result);
        assertPartialError(result);
        return shadow;
    }

    // TODO: shadow with legacy postponed operation: should be cleaned up

    // TODO: retries of propagated operations
}
