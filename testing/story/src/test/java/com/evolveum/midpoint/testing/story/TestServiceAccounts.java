/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;

import com.evolveum.midpoint.prism.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestServiceAccounts extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "service-accounts");

    protected static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
    protected static final String RESOURCE_DUMMY_OID = "0069ac14-8377-11e8-b404-5b5a1a8af0db";
    private static final String RESOURCE_DUMMY_NS = MidPointConstants.NS_RI;
    protected static final String RESOURCE_DUMMY_INTENT_SERVICE = "service";

    private static final String ACCOUNT_RUM_STORAGE_DUMMY_USERNAME = "rum-storage";
    private static final String ACCOUNT_RUM_STORAGE_DUMMY_FULLNAME = "Rum Storage Application";

    private static final File SERVICE_BARELLIUM_FILE = new File(TEST_DIR, "service-barellium.xml");
    private static final String SERVICE_BARELLIUM_OID = "ba64f6e8-a77e-11e8-a0e8-fb9318a3952f";
    private static final String SERVICE_BARELLIUM_NAME = "barellium";
    private static final String SERVICE_BARELLIUM_DESCRIPTION = "Barellium Superiorum";

    private static final File ACCOUNT_BARELLIUM_DUMMY_FILE = new File(TEST_DIR, "account-barellium-dummy.xml");
    private static final String ACCOUNT_BARELLIUM_DUMMY_OID = "fe0d6d9a-a77d-11e8-a144-0bbeb63fd26b";
    private static final String ACCOUNT_BARELLIUM_DUMMY_USERNAME = "barellium";
    private static final String ACCOUNT_BARELLIUM_DUMMY_FULLNAME = "Barellium Magnum";

    private static final String ACCOUNT_MAGAZINE_DUMMY_USERNAME = "magazine";
    private static final String ACCOUNT_MAGAZINE_DUMMY_FULLNAME = "Gunpowder magazine";

    private static final File TASK_LIVE_SYNC_DUMMY_FILE = new File(TEST_DIR, "task-dummy-livesync.xml");
    private static final String TASK_LIVE_SYNC_DUMMY_OID = "474eb3ac-837e-11e8-8cf8-6bd4fe328f30";

    private static final File TASK_RECONCILE_DUMMY_FILE = new File(TEST_DIR, "task-dummy-reconcile.xml");
    private static final String TASK_RECONCILE_DUMMY_OID = "10335c7c-838f-11e8-93a6-4b1dd0ab58e4";

    private String serviceAccountShadowOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(null, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
        getDummyResource().setSyncStyle(DummySyncStyle.SMART);
    }

    @Test
    public void test100StartSyncTask() throws Exception {
        final String TEST_NAME = "test100StartSyncTask";

        assertUsers(getNumberOfUsers());
        assertServices(0);

        // WHEN
        when();

        importObjectFromFile(TASK_LIVE_SYNC_DUMMY_FILE);

        // THEN
        then();

        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_OID, true);

        assertServices(0);
        assertUsers(getNumberOfUsers());
    }

    @Test
    public void test101AddServiceAccountSync() throws Exception {
        final String TEST_NAME = "test101AddServiceAccountSync";

        // Preconditions
        assertServices(0);

        DummyAccount account = new DummyAccount(ACCOUNT_RUM_STORAGE_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                ACCOUNT_RUM_STORAGE_DUMMY_FULLNAME);

        // WHEN
        when();

        getDummyResource().addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_OID, true);

        // THEN
        then();

        assertServices(1);

        serviceAccountShadowOid = assertServiceAfterByName(ACCOUNT_RUM_STORAGE_DUMMY_USERNAME)
            .assertDescription(ACCOUNT_RUM_STORAGE_DUMMY_FULLNAME)
            .links()
                .single()
                    .resolveTarget()
                        .assertLife()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(RESOURCE_DUMMY_INTENT_SERVICE)
                        .getOid();

        assertDummyAccountByUsername(null, ACCOUNT_RUM_STORAGE_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_RUM_STORAGE_DUMMY_FULLNAME);
    }

    /**
     * Try to modify service account by using model service (account delta).
     * Modification capabilities for service accounts are disabled. Therefore
     * such attempt should fail.
     */
    @Test
    public void test102ModifyServiceAccount() throws Exception {
        final String TEST_NAME = "test102ModifyServiceAccount";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                serviceAccountShadowOid, getDummyResourceController().getAttributeFullnamePath(),
                "Where's all the rum?");

        try {
            // WHEN
            when();

            executeChanges(delta, null, task, result);

            assertNotReached();

        } catch (UnsupportedOperationException e) {
            // THEN
            then();
            display("expected exception", e);
        }

        assertFailure(result);

        assertServices(1);

        assertServiceAfterByName(ACCOUNT_RUM_STORAGE_DUMMY_USERNAME)
            .assertDescription(ACCOUNT_RUM_STORAGE_DUMMY_FULLNAME)
            .links()
                .single()
                    .resolveTarget()
                        .assertLife()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(RESOURCE_DUMMY_INTENT_SERVICE)
                        .getOid();

        assertDummyAccountByUsername(null, ACCOUNT_RUM_STORAGE_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_RUM_STORAGE_DUMMY_FULLNAME);
    }

    /**
     * Try to delete service account by using model service (account delta).
     * Deletion capabilities for service accounts are disabled. Therefore
     * such attempt should fail.
     */
    @Test
    public void test104DeleteServiceAccount() throws Exception {
        final String TEST_NAME = "test104DeleteServiceAccount";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createDeleteDelta(ShadowType.class,
                serviceAccountShadowOid);

        try {
            // WHEN
            when();

            executeChanges(delta, null, task, result);

            assertNotReached();

        } catch (UnsupportedOperationException e) {
            // THEN
            then();
            display("expected exception", e);
        }

        assertFailure(result);

        assertServices(1);

        assertServiceAfterByName(ACCOUNT_RUM_STORAGE_DUMMY_USERNAME)
            .assertDescription(ACCOUNT_RUM_STORAGE_DUMMY_FULLNAME)
            .links()
                .single()
                    .resolveTarget()
                        .assertLife()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(RESOURCE_DUMMY_INTENT_SERVICE)
                        .getOid();

        assertDummyAccountByUsername(null, ACCOUNT_RUM_STORAGE_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_RUM_STORAGE_DUMMY_FULLNAME);
    }

    // TODO: account modifications, check that the changes are synced to service

    /**
     * MID-4522
     */
    @Test
    public void test108DeleteServiceAccountSync() throws Exception {
        final String TEST_NAME = "test108DeleteServiceAccountSync";

        // Preconditions
        assertServices(1);

        // WHEN
        when();

        getDummyResource().deleteAccountByName(ACCOUNT_RUM_STORAGE_DUMMY_USERNAME);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_OID, true);

        // THEN
        then();

        assertNoServiceByName(ACCOUNT_RUM_STORAGE_DUMMY_USERNAME);
        assertServices(0);
    }

    @Test
    public void test109StopLivesyncTask() throws Exception {
        final String TEST_NAME = "test109StopLivesyncTask";

        // Preconditions
        assertServices(0);

        // WHEN
        when();

        suspendTask(TASK_LIVE_SYNC_DUMMY_OID);

        // THEN
        then();
        assertTaskExecutionStatus(TASK_LIVE_SYNC_DUMMY_OID, TaskExecutionStatus.SUSPENDED);
    }

    @Test
    public void test120StartReconTask() throws Exception {
        final String TEST_NAME = "test120StartReconTask";

        assertUsers(getNumberOfUsers());
        assertServices(0);

        // WHEN
        when();

        importObjectFromFile(TASK_RECONCILE_DUMMY_FILE);

        // THEN
        then();

        waitForTaskStart(TASK_RECONCILE_DUMMY_OID, true);

        assertServices(0);
        assertUsers(getNumberOfUsers());
    }

    @Test
    public void test121AddServiceAccountRecon() throws Exception {
        final String TEST_NAME = "test121AddServiceAccountRecon";

        // Preconditions
        assertServices(0);

        DummyAccount account = new DummyAccount(ACCOUNT_MAGAZINE_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                ACCOUNT_MAGAZINE_DUMMY_FULLNAME);

        // WHEN
        when();

        getDummyResource().addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_RECONCILE_DUMMY_OID, true);

        // THEN
        then();

        assertServices(1);

        assertServiceAfterByName(ACCOUNT_MAGAZINE_DUMMY_USERNAME)
            .assertDescription(ACCOUNT_MAGAZINE_DUMMY_FULLNAME);
    }

    /**
     * MID-4522
     */
    @Test
    public void test128DeleteServiceAccountRecon() throws Exception {
        final String TEST_NAME = "test128DeleteServiceAccountRecon";

        // Preconditions
        assertServices(1);

        // WHEN
        when();

        getDummyResource().deleteAccountByName(ACCOUNT_MAGAZINE_DUMMY_USERNAME);

        waitForTaskNextRunAssertSuccess(TASK_RECONCILE_DUMMY_OID, true);

        // THEN
        then();

        assertNoServiceByName(ACCOUNT_MAGAZINE_DUMMY_USERNAME);
        assertServices(0);
    }

    public void test129StopReconTask() throws Exception {
        final String TEST_NAME = "test129StopReconTask";

        // Preconditions
        assertServices(1);

        // WHEN
        when();

        suspendTask(TASK_RECONCILE_DUMMY_OID);

        // THEN
        then();
        assertTaskExecutionStatus(TASK_RECONCILE_DUMMY_OID, TaskExecutionStatus.SUSPENDED);
    }

    /**
     * Try to create new service account by using model service (account delta).
     * Creation capabilities for service accounts are disabled. Therefore
     * such attempt should fail.
     */
    @Test
    public void test140CreateServiceAccount() throws Exception {
        final String TEST_NAME = "test140CreateServiceAccount";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        addObject(SERVICE_BARELLIUM_FILE);

        PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_BARELLIUM_DUMMY_FILE);

        ObjectDelta<ServiceType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ServiceType.class, SERVICE_BARELLIUM_OID);
        PrismReferenceValue accountRefVal = getPrismContext().itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference()
                .createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        delta.addModification(accountDelta);

        try {
            // WHEN
            when();

            executeChanges(delta, null, task, result);

            assertNotReached();

        } catch (UnsupportedOperationException e) {
            // THEN
            then();
            display("expected exception", e);
        }

        assertFailure(result);

        assertServices(1);

        assertServiceAfter(SERVICE_BARELLIUM_OID)
            .assertName(SERVICE_BARELLIUM_NAME)
            .assertDescription(SERVICE_BARELLIUM_DESCRIPTION)
            .assertLinks(0);

        assertNoDummyAccount(ACCOUNT_BARELLIUM_DUMMY_USERNAME);
    }

}
