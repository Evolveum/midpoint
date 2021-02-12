/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static org.testng.AssertJUnit.*;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractSynchronizationStoryTest extends AbstractInitializedModelIntegrationTest {

    protected static final String ACCOUNT_WALLY_DUMMY_USERNAME = "wally";
    protected static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";
    private static final Date ACCOUNT_MANCOMB_VALID_FROM_DATE = MiscUtil.asDate(2011, 2, 3, 4, 5, 6);
    private static final Date ACCOUNT_MANCOMB_VALID_TO_DATE = MiscUtil.asDate(2066, 5, 4, 3, 2, 1);

    private static final String ACCOUNT_PROTECTED_SYSTEM = "system";

    protected static String userWallyOid;

    protected boolean allwaysCheckTimestamp = false;
    protected long timeBeforeSync;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
    }

    @Override
    protected ConflictResolutionActionType getDefaultConflictResolutionAction() {
        return ConflictResolutionActionType.NONE;       // too many conflicts in these scenarios
    }

    protected abstract void importSyncTask(PrismObject<ResourceType> resource) throws FileNotFoundException;

    protected abstract String getSyncTaskOid(PrismObject<ResourceType> resource);

    protected int getWaitTimeout() {
        return DEFAULT_TASK_WAIT_TIMEOUT;
    }

    protected int getNumberOfExtraDummyUsers() {
        return 0;
    }

    protected boolean isReconciliation() {
        return false;
    }

    @Test
    public void test100ImportLiveSyncTaskDummyGreen() throws Exception {
        /// WHEN
        when();
        importSyncTask(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // THEN
        then();

        waitForSyncTaskStart(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));
    }

    @Test
    public void test110AddDummyGreenAccountMancomb() throws Exception {
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(6);

        DummyAccount account = new DummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        account.setEnabled(true);
        account.setValidFrom(ACCOUNT_MANCOMB_VALID_FROM_DATE);
        account.setValidTo(ACCOUNT_MANCOMB_VALID_TO_DATE);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");

        /// WHEN
        when();

        getDummyResource(RESOURCE_DUMMY_GREEN_NAME).addAccount(account);

        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // THEN
        then();

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_GREEN_OID,
                accountMancomb.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED);
        assertValidFrom(accountMancomb, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(accountMancomb, ACCOUNT_MANCOMB_VALID_TO_DATE);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb was not created", userMancomb);
        assertLinks(userMancomb, 1);
        assertAdministrativeStatusEnabled(userMancomb);
        assertValidFrom(userMancomb, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userMancomb, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userMancomb, accountMancomb);

        assertUsers(7);

        // notifications
        displayAllNotifications();
        assertSingleDummyTransportMessageContaining("simpleAccountNotifier-SUCCESS", "Channel: " + getExpectedChannel());
        notificationManager.setDisabled(true);
    }

    protected abstract String getExpectedChannel();

    @Test
    public void test200ImportLiveSyncTaskDummyBlue() throws Exception {
        /// WHEN
        when();
        importSyncTask(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));

        // THEN
        then();

        waitForSyncTaskStart(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));
    }

    /**
     * Add wally to the green dummy resource. User should be created and linked.
     */
    @Test
    public void test210AddDummyGreenAccountWally() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();
        prepareNotifications();

        /// WHEN
        when();
        getDummyResourceController(RESOURCE_DUMMY_GREEN_NAME).addAccount(ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", "Scabb Island");

        // Wait for sync task to pick up the change
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // THEN
        then();

        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), result);

        PrismObject<ShadowType> accountWallyGreen = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), getDummyResource(RESOURCE_DUMMY_GREEN_NAME), "green", "Wally Feed");
        assertShadowOperationalData(accountWallyGreen, SynchronizationSituationType.LINKED);

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally was not created", userWally);
        userWallyOid = userWally.getOid();
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", null, null);
        assertLinks(userWally, 1);

        assertUsers(8);

        assertLinked(userWally, accountWallyGreen);

        // notifications
        notificationManager.setDisabled(true);
    }

    /**
     * Add wally also to the blue dummy resource. User should be linked to this account.
     */
    @Test
    public void test220AddDummyBlueAccountWally() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();
        prepareNotifications();

        /// WHEN
        when();
        getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME).addAccount(ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", "Scabb Island");

        // Wait for sync task to pick up the change
        // However, blue is not authoritative regarding creating new users, so wally will not be created at this time
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));

        // Make sure that the "kickback" sync cycle of the other resource runs to completion
        // We want to check the state after it gets stable
        // and it could spoil the next test
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));

        // THEN
        then();

        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), result);
        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), result);

        PrismObject<ShadowType> accountWallyGreen = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), getDummyResource(RESOURCE_DUMMY_GREEN_NAME), "green", "Wally Feed");
        assertShadowOperationalData(accountWallyGreen, SynchronizationSituationType.LINKED);
        PrismObject<ShadowType> accountWallyBlue = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME),
                getDummyResource(RESOURCE_DUMMY_BLUE_NAME), "blue", "Wally Feed");
        assertShadowOperationalData(accountWallyBlue, SynchronizationSituationType.LINKED);

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally was not created", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", null, null);
        assertLinks(userWally, 2);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);

        assertUsers(8);

        // notifications
        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);        // account itself is not added (only the shadow is!)
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 1);

    }

    /**
     * Add mancomb also to the blue dummy resource. This account should be linked to the existing user.
     * Similar to the previous test but blue resource has a slightly different correlation expression.
     */
    @Test
    public void test315AddDummyBlueAccountMancomb() throws Exception {
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        /// WHEN
        when();
        getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME).addAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME, "Mancomb Seepgood", "Melee Island");

        // Wait for sync task to pick up the change
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));

        // Make sure that the "kickback" sync cycle of the other resource runs to completion
        // We want to check the state after it gets stable
        // and it could spoil the next test
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));

        // THEN
        then();

        // The ckecks are simplified here because the developer has a lazy mood :-)
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME, "Mancomb Seepgood", true);
        assertDummyAccount(RESOURCE_DUMMY_GREEN_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME, "Mancomb Seepgood", true);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb disappeared", userMancomb);
        assertUser(userMancomb, userMancomb.getOid(), ACCOUNT_MANCOMB_DUMMY_USERNAME, "Mancomb Seepgood", null, null);
        assertLinks(userMancomb, 2);
        assertAccount(userMancomb, RESOURCE_DUMMY_BLUE_OID);
        assertAccount(userMancomb, RESOURCE_DUMMY_GREEN_OID);

        assertUsers(8);

        // notifications
        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("userPasswordNotifier", 1);                     // previously non-existing password is generated
//        checkDummyTransportMessages("accountPasswordNotifier", 1);                  // password is then set on the account
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 2);            // changes on green & blue (induced)
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);        // account itself is not added (only the shadow is!)
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }

    /**
     * Import sync task for default dummy resource as well. This does not do much as we will no be manipulating
     * the default dummy account directly. Just make sure that it does not do anything bad.
     */
    @Test
    public void test350ImportLiveSyncTaskDummyDefault() throws Exception {
        /// WHEN
        when();
        importSyncTask(getDummyResourceObject());

        // THEN
        then();

        waitForSyncTaskStart(getDummyResourceObject());

        // Dummy resource has some extra users that may be created in recon, so let's give it a chance to do it now
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject());

        assertUsers(8 + getNumberOfExtraDummyUsers());
    }

    /**
     * Import sync task for default dummy resource as well. This does not do much as we will no be manipulating
     * the default dummy account directly. Just make sure that it does not do anything bad.
     */
    @Test
    public void test360ModifyUserAddDummyDefaultAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        assertEquals("OID of user wally have changed", userWallyOid, userWally.getOid());

        ObjectDelta<UserType> userDelta = createModifyUserAddAccount(userWally.getOid(), getDummyResourceObject());
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(userDelta);

        /// WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();

        dumpSyncTaskTree(getDummyResourceObject(), result);
        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), result);
        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), result);

        // Make sure we have steady state
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject());
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        PrismObject<ShadowType> accountWallyDefault = checkWallyAccount(getDummyResourceObject(), getDummyResource(),
                "default", "Wally Feed");
        assertShadowOperationalData(accountWallyDefault, SynchronizationSituationType.LINKED);
        PrismObject<ShadowType> accountWallyBlue = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME),
                getDummyResource(RESOURCE_DUMMY_BLUE_NAME), "blue", "Wally Feed");
        if (allwaysCheckTimestamp) { assertShadowOperationalData(accountWallyBlue, SynchronizationSituationType.LINKED); }
        PrismObject<ShadowType> accountWallyGreen = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), getDummyResource(RESOURCE_DUMMY_GREEN_NAME),
                "green", "Wally Feed");
        if (allwaysCheckTimestamp) { assertShadowOperationalData(accountWallyGreen, SynchronizationSituationType.LINKED); }

        userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", null, null);
        assertLinks(userWally, 3);

        assertLinked(userWally, accountWallyDefault);
        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);

        assertUsers(8 + getNumberOfExtraDummyUsers());
    }

    /**
     * Change fullname on the green account. There is an inbound mapping to the user so it should propagate.
     * There is also outbound mapping from the user to dummy account, therefore it should propagate there as well.
     */
    @Test
    public void test370ModifyDummyGreenAccountWally() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        repoAddObjectFromFile(USER_TEMPLATE_SYNC_FILENAME, result);
        assumeUserTemplate(USER_TEMPLATE_SYNC_OID, getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME).asObjectable(), "default account type", result);

        rememberTimeBeforeSync();
        prepareNotifications();

        DummyAccount wallyDummyAccount = getDummyResource(RESOURCE_DUMMY_GREEN_NAME).getAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);

        /// WHEN
        when();
        wallyDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Wally B. Feed");

        // Wait for sync task to pick up the change
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // Make sure that the "kickback" sync cycle of the other resource runs to completion
        // We want to check the state after it gets stable
        // and it could spoil the next test
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));
        // Make sure we have steady state
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject());

        // THEN
        then();

        dumpSyncTaskTree(getDummyResourceObject(), result);
        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), result);
        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), result);

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally B. Feed", null, "Wally B. Feed from Sync");

        PrismObject<ShadowType> accountWallyBlue = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME),
                getDummyResource(RESOURCE_DUMMY_BLUE_NAME), "blue", "Wally Feed");
        if (allwaysCheckTimestamp) { assertShadowOperationalData(accountWallyBlue, SynchronizationSituationType.LINKED); }
        PrismObject<ShadowType> accountWallyGreen = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), getDummyResource(RESOURCE_DUMMY_GREEN_NAME), "green", "Wally B. Feed");
        assertShadowOperationalData(accountWallyGreen, SynchronizationSituationType.LINKED);
        PrismObject<ShadowType> accountWallyDefault;
        // See MID-2518
        if (isReconciliation()) {

            // Can be iether "Wally Feed" or "Wally B. Feed". Both are correct. Depends on he order of recon
            // task execution.
            accountWallyDefault = checkWallyAccount(getDummyResourceObject(), getDummyResource(), "default", null);

        } else {
            accountWallyDefault = checkWallyAccount(getDummyResourceObject(), getDummyResource(), "default", "Wally B. Feed");
        }
        assertShadowOperationalData(accountWallyDefault, SynchronizationSituationType.LINKED);

        assertLinks(userWally, 3);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);
        assertLinked(userWally, accountWallyDefault);

        assertUsers(8 + getNumberOfExtraDummyUsers());

        // notifications
        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessagesAtLeast("simpleUserNotifier", 1);                // actually I dont understand why sometimes is here 1, sometimes 2 messages (has to do something with mapping username->familyname)
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }

    /**
     * Change user fullname. Fullname has normal mapping on default dummy.
     * See if the change propagates correctly. Also see that there are no side-effects.
     */
    @Test
    public void test380ModifyUserWallyFullName() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();
        prepareNotifications();

        getDummyResource(RESOURCE_DUMMY_GREEN_NAME).getAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);

        /// WHEN
        when();
        modifyUserReplace(userWallyOid, UserType.F_FULL_NAME, task, result, PrismTestUtil.createPolyString("Bloodnose"));

        // Wait for sync tasks to pick up the change and have some chance to screw things
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject());
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // Run green recon twice here. If the recon already searched for current state of the
        // wally account, the old value ("Wally B. Feed") is stored in the memory. Even if we rewrite
        // the account state by this operation the value already read into memory will be used instead
        // and it will be propagated to other resources. The next recon run should fix it in the user.
        // But as the mapping to default dummy is normal, the recon will not fix it on the resource.
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // THEN
        then();

        dumpSyncTaskTree(getDummyResourceObject(), result);
        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), result);
        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), result);

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Bloodnose", null, "Bloodnose from Sync");

        PrismObject<ShadowType> accountWallyBlue = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME),
                getDummyResource(RESOURCE_DUMMY_BLUE_NAME), "blue", "Wally Feed");
        if (allwaysCheckTimestamp) { assertShadowOperationalData(accountWallyBlue, SynchronizationSituationType.LINKED); }
        PrismObject<ShadowType> accountWallyGreen = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), getDummyResource(RESOURCE_DUMMY_GREEN_NAME), "green", "Bloodnose");
        assertShadowOperationalData(accountWallyGreen, SynchronizationSituationType.LINKED);

        PrismObject<ShadowType> accountWallyDefault = findAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME, getDummyResourceObject());
        String fullNameDummyAttribute = IntegrationTestTools.getAttributeValue(accountWallyDefault.asObjectable(),
                new QName(ResourceTypeUtil.getResourceNamespace(getDummyResourceObject()), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));
        if (!"Bloodnose".equals(fullNameDummyAttribute) && !"Wally B. Feed".equals(fullNameDummyAttribute)) {
            AssertJUnit.fail("Wrong full name on default dummy resource: " + fullNameDummyAttribute);
        }
        assertShadowOperationalData(accountWallyDefault, SynchronizationSituationType.LINKED);

        assertLinks(userWally, 3);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);
        assertLinked(userWally, accountWallyDefault);

        assertUsers(8 + getNumberOfExtraDummyUsers());

        notificationManager.setDisabled(true);
    }

    /**
     * Change user locality. Locality has strong mapping on default dummy.
     * See if the change propagates correctly. Also see that there are no side-effects.
     */
    @Test
    public void test382ModifyUserWallyLocality() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();
        prepareNotifications();

        getDummyResource(RESOURCE_DUMMY_GREEN_NAME).getAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);

        /// WHEN
        when();
        modifyUserReplace(userWallyOid, UserType.F_LOCALITY, task, result, PrismTestUtil.createPolyString("Plunder island"));

        // Wait for sync tasks to pick up the change and have some chance to screw things
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject());
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // Run green recon twice here. If the recon already searched for current state of the
        // wally account, the old value is stored in the memory. Even if we rewrite
        // the account state by this operation the value already read into memory will be used instead
        // and it will be propagated to other resources. The next recon run should fix it.
        // Both in user and on the resource.
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // THEN
        then();

        dumpSyncTaskTree(getDummyResourceObject(), result);
        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), result);
        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), result);

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Bloodnose", null, "Bloodnose from Sync");
        PrismAsserts.assertPropertyValue(userWally, UserType.F_LOCALITY, PrismTestUtil.createPolyString("Plunder island"));

        PrismObject<ShadowType> accountWallyBlue = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME),
                getDummyResource(RESOURCE_DUMMY_BLUE_NAME), "blue", "Wally Feed");
        if (allwaysCheckTimestamp) { assertShadowOperationalData(accountWallyBlue, SynchronizationSituationType.LINKED); }
        PrismObject<ShadowType> accountWallyGreen = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), getDummyResource(RESOURCE_DUMMY_GREEN_NAME), "green", "Bloodnose");
        assertShadowOperationalData(accountWallyGreen, SynchronizationSituationType.LINKED);

        PrismObject<ShadowType> accountWallyDefault = findAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME, getDummyResourceObject());
        String fullNameDummyAttribute = IntegrationTestTools.getAttributeValue(accountWallyDefault.asObjectable(),
                new QName(ResourceTypeUtil.getResourceNamespace(getDummyResourceObject()), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));
        if (!"Bloodnose".equals(fullNameDummyAttribute) && !"Wally B. Feed".equals(fullNameDummyAttribute)) {
            AssertJUnit.fail("Wrong full name on default dummy resource: " + fullNameDummyAttribute);
        }
        assertShadowOperationalData(accountWallyDefault, SynchronizationSituationType.LINKED);
        assertShadowOperationalData(accountWallyDefault, SynchronizationSituationType.LINKED);

        assertDummyAccountAttribute(RESOURCE_DUMMY_GREEN_NAME, ACCOUNT_WALLY_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Plunder island");
        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_WALLY_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Scabb Island");
        assertDummyAccountAttribute(null, ACCOUNT_WALLY_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Plunder island");

        assertLinks(userWally, 3);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);
        assertLinked(userWally, accountWallyDefault);

        assertUsers(8 + getNumberOfExtraDummyUsers());

        notificationManager.setDisabled(true);
    }

    /**
     * Delete default dummy account.
     * Dummy resource has unlinkAccount sync reaction for deleted situation. The account should be unlinked
     * but the user and other accounts should remain as they were.
     */
    @Test
    public void test400DeleteDummyDefaultAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();
        prepareNotifications();

        /// WHEN
        when();
        getDummyResource().deleteAccountByName(ACCOUNT_WALLY_DUMMY_USERNAME);

        displayValue("Dummy (default) resource", getDummyResource().debugDump());

        // Make sure we have steady state
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject());
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // THEN
        then();

        dumpSyncTaskTree(getDummyResourceObject(), result);
        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), result);
        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), result);

        assertNoDummyAccount(ACCOUNT_WALLY_DUMMY_USERNAME);
        assertShadow(ACCOUNT_WALLY_DUMMY_USERNAME, getDummyResourceObject())
                .assertTombstone()
                .assertSynchronizationSituation(SynchronizationSituationType.DELETED);

        PrismObject<ShadowType> accountWallyBlue = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME),
                getDummyResource(RESOURCE_DUMMY_BLUE_NAME), "blue", "Wally Feed");
        if (allwaysCheckTimestamp) { assertShadowOperationalData(accountWallyBlue, SynchronizationSituationType.LINKED); }
        PrismObject<ShadowType> accountWallyGreen = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), getDummyResource(RESOURCE_DUMMY_GREEN_NAME), "green", "Bloodnose");
        if (allwaysCheckTimestamp) { assertShadowOperationalData(accountWallyGreen, SynchronizationSituationType.LINKED); }

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Bloodnose", null, "Bloodnose from Sync");
        assertLinks(userWally, 2);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);

        assertUsers(8 + getNumberOfExtraDummyUsers());

        // notifications
        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 0);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

    }

    /**
     * Delete green dummy account.
     * Green dummy resource has deleteUser sync reaction for deleted situation. This should delete the user
     * and all other accounts.
     * MID-4522
     */
    @Test
    public void test410DeleteDummyGreenAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareNotifications();

        /// WHEN
        when();
        getDummyResource(RESOURCE_DUMMY_GREEN_NAME).deleteAccountByName(ACCOUNT_WALLY_DUMMY_USERNAME);

        // Make sure we have steady state
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject());
        OperationResult takResultBlue = waitForSyncTaskNextRun(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // THEN
        then();

        dumpSyncTaskTree(getDummyResourceObject(), result);
        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), result);
        dumpSyncTaskTree(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), result);

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

        // Interesting things can happen here. Like this:
        //
        // Recon blue:  Search on resource, remember results in connector, sending them one
        //              by one to midpoint. But recon task is slow, it will not send wally
        //              account yet.
        // Recon green: Search on resource, got wally as the first result, sending to model,
        //              model deletes user and blue account.
        // Recon blue:  It finally gets to send wally account to the model. But the original
        //              shadow is gone. So, provisioning creates a new shadow. And it is right.
        //              the object just came from the connector and therefore provisioning
        //              thinks that it is fresh and it has been re-created.
        //
        // We have nothing simple to do about this. And really, we do not care much because
        // it is not really a problem. It is rare and the next recon run should fix it anyway.
        // It will not cause any real harm on target resource. It may do some (even quite nasty)
        // churn on source+target resource. But source+target is a dangerous lifestyle anyway.
        //
        // So, if that happens we will just wait for the next recon run and check that it
        // sorts it out.

        ObjectQuery wallyBlueAccountsQuery = createAccountShadowQuery(ACCOUNT_WALLY_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));
        List<PrismObject<ShadowType>> wallyBlueAccounts = repositoryService.searchObjects(ShadowType.class, wallyBlueAccountsQuery, null, result);
        if (!wallyBlueAccounts.isEmpty()) {
            waitForSyncTaskNextRun(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME));
        }

        assertNoShadow(ACCOUNT_WALLY_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), result);

        assertUsers(7 + getNumberOfExtraDummyUsers());

        if (isReconciliation()) {
            if (takResultBlue.getStatus() == OperationResultStatus.PARTIAL_ERROR) {
                // Blue resource recon may fail. The user may be deleted before the blue
                // recon task finishes. If the result (user, accounts) is OK then tolerate this error.
            } else {
                TestUtil.assertSuccess("Blue resource synchronization has failed", takResultBlue);
            }
        } else {
            TestUtil.assertSuccess("Blue resource synchronization has failed", takResultBlue);
        }

        // notifications
        notificationManager.setDisabled(true);
    }

    @Test
    public void test510AddDummyGreenAccountWallyUserTemplate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();

        assumeUserTemplate(USER_TEMPLATE_SYNC_OID, getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME).asObjectable(), null, result);

        /// WHEN
        when();
        getDummyResourceController(RESOURCE_DUMMY_GREEN_NAME).addAccount(ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", "Scabb Island");

        // Wait for sync task to pick up the change
        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // THEN
        then();

        PrismObject<ShadowType> accountWallyGreen = checkWallyAccount(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME), getDummyResource(RESOURCE_DUMMY_GREEN_NAME), "green", "Wally Feed");
        assertShadowOperationalData(accountWallyGreen, SynchronizationSituationType.LINKED);

        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally was not created", userWally);
        userWallyOid = userWally.getOid();
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", null, "Wally Feed from Sync");
        assertLinks(userWally, 1);
        assertLinked(userWally, accountWallyGreen);

        assertUsers(8 + getNumberOfExtraDummyUsers());
    }

    /**
     * Calypso is a protected account. It should not be touched by the sync.
     */
    @Test
    public void test600AddDummyGreenAccountCalypso() throws Exception {
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(8 + getNumberOfExtraDummyUsers());

        DummyAccount account = new DummyAccount(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Calypso");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "The Seven Seas");

        /// WHEN
        when();

        getDummyResource(RESOURCE_DUMMY_GREEN_NAME).addAccount(account);

        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // THEN
        then();

        PrismObject<ShadowType> accountShadow = findAccountByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));
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
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(8 + getNumberOfExtraDummyUsers());

        DummyAccount account = new DummyAccount(ACCOUNT_PROTECTED_SYSTEM);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "System Account");

        /// WHEN
        when();

        getDummyResource(RESOURCE_DUMMY_GREEN_NAME).addAccount(account);

        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // THEN
        then();

        PrismObject<ShadowType> accountShadow = findAccountByUsername(ACCOUNT_PROTECTED_SYSTEM, getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));
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
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(8 + getNumberOfExtraDummyUsers());

        DummyAccount account = new DummyAccount("Xjojo");
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jojo the Monkey");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Scabb Island");

        /// WHEN
        when();

        getDummyResource(RESOURCE_DUMMY_GREEN_NAME).addAccount(account);

        waitForSyncTaskNextRunAssertSuccess(getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));

        // THEN
        then();

        PrismObject<ShadowType> accountAfter = findAccountByUsername("Xjojo", getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));
        display("Account after", accountAfter);
        assertNotNull("No account shadow", accountAfter);
        assertEquals("Wrong resourceRef in account shadow", RESOURCE_DUMMY_GREEN_OID,
                accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);
        assertShadowKindIntent(accountAfter, ShadowKindType.ACCOUNT, "admin");

        PrismObject<UserType> userAfter = findUserByUsername("jojo");
        display("User after", userAfter);
        assertNotNull("User jojo was not created", userAfter);
        assertLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);

        assertLinked(userAfter, accountAfter);

        assertUsers(9 + getNumberOfExtraDummyUsers());

        // notifications
        notificationManager.setDisabled(true);
    }

    private void assumeUserTemplate(String templateOid, ResourceType resource, String syncConfigName, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        SynchronizationType resourceSync = resource.getSynchronization();
        resourceSync.getObjectSynchronization().get(0).setObjectTemplateRef(ObjectTypeUtil.createObjectRef(templateOid, ObjectTypes.OBJECT_TEMPLATE));

        Collection<? extends ItemDelta<?, ?>> refDelta = prismContext.deltaFactory().property()
                .createModificationReplacePropertyCollection(ResourceType.F_SYNCHRONIZATION, resource.asPrismObject().getDefinition(), resourceSync);
        repositoryService.modifyObject(ResourceType.class, resource.getOid(), refDelta, result);

        ResourceType res = repositoryService.getObject(ResourceType.class, resource.getOid(), null, result).asObjectable();
        assertNotNull(res);
        assertNotNull("Synchronization is not specified", res.getSynchronization());
        ObjectSynchronizationType ost = determineSynchronization(res, UserType.class, syncConfigName);
        assertNotNull("object sync type is not specified", ost);
        assertNotNull("user template not specified", ost.getObjectTemplateRef());
        assertEquals("Wrong user template in resource", templateOid, ost.getObjectTemplateRef().getOid());

    }

    protected void waitForSyncTaskStart(PrismObject<ResourceType> resource) throws Exception {
        waitForTaskStart(getSyncTaskOid(resource), false, getWaitTimeout());
    }

    protected OperationResult waitForSyncTaskNextRunAssertSuccess(PrismObject<ResourceType> resource) throws Exception {
        return waitForTaskNextRunAssertSuccess(getSyncTaskOid(resource), false, getWaitTimeout());
    }

    protected void dumpSyncTaskTree(PrismObject<ResourceType> resource, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        String oid = getSyncTaskOid(resource);
        dumpTaskTree(oid, result);
    }

    protected OperationResult waitForSyncTaskNextRun(PrismObject<ResourceType> resource) throws Exception {
        return waitForTaskNextRun(getSyncTaskOid(resource), false, getWaitTimeout());
    }

    private PrismObject<ShadowType> checkWallyAccount(PrismObject<ResourceType> resource, DummyResource dummy, String resourceDesc,
            String expectedFullName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, ExpressionEvaluationException, InterruptedException {
        return checkWallyAccount(resource, dummy, resourceDesc, expectedFullName, null, null);
    }

    private PrismObject<ShadowType> checkWallyAccount(PrismObject<ResourceType> resource, DummyResource dummy, String resourceDesc,
            String expectedFullName, String shipName, String quote) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, ExpressionEvaluationException, InterruptedException {
        PrismObject<ShadowType> accountShadowWally = findAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME, resource);
        display("Account shadow wally (" + resourceDesc + ")", accountShadowWally);
        assertEquals("Wrong resourceRef in wally account (" + resourceDesc + ")", resource.getOid(),
                accountShadowWally.asObjectable().getResourceRef().getOid());
        if (expectedFullName != null) {
            IntegrationTestTools.assertAttribute(accountShadowWally.asObjectable(), resource.asObjectable(),
                    DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, expectedFullName);
        }

        DummyAccount dummyAccount = dummy.getAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        displayDumpable("Account wally (" + resourceDesc + ")", dummyAccount);
        assertNotNull("No dummy account (" + resourceDesc + ")", dummyAccount);
        if (expectedFullName != null) {
            assertEquals("Wrong dummy account fullname (" + resourceDesc + ")", expectedFullName,
                    dummyAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));
        }

        if (shipName != null) {
            assertEquals("Wrong dummy account shipName (" + resourceDesc + ")", shipName,
                    dummyAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));
        }

        if (quote != null) {
            assertEquals("Wrong dummy account quote (" + resourceDesc + ")", quote,
                    dummyAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME));
        }

        return accountShadowWally;
    }

    protected void rememberTimeBeforeSync() {
        timeBeforeSync = System.currentTimeMillis();
    }

    protected void assertShadowOperationalData(PrismObject<ShadowType> shadow, SynchronizationSituationType expectedSituation) {
        super.assertShadowOperationalData(shadow, expectedSituation, timeBeforeSync);
    }

}
