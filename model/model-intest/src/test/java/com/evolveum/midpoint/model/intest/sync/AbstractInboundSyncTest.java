/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.polystring.PolyString;

import com.evolveum.midpoint.test.TestTask;

import com.evolveum.midpoint.util.exception.CommonException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractInboundSyncTest extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/sync");

    static final TestTask TASK_LIVE_SYNC_DUMMY_EMERALD = TestTask.file(
            TEST_DIR, "task-dummy-emerald-livesync.xml", "10000000-0000-0000-5555-55550000e404");
    static final TestTask TASK_RECON_DUMMY_EMERALD = TestTask.file(
            TEST_DIR, "task-dummy-emerald-recon.xml", "10000000-0000-0000-5656-56560000e404");

    static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";
    static final Date ACCOUNT_MANCOMB_VALID_FROM_DATE = MiscUtil.asDate(2011, 2, 3, 4, 5, 6);
    static final Date ACCOUNT_MANCOMB_VALID_TO_DATE = MiscUtil.asDate(2066, 5, 4, 3, 2, 1);

    private static final String ACCOUNT_POSIXUSER_DUMMY_USERNAME = "posixuser";

    private long timeBeforeSync;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        if (areMarksSupported()) {
            repoAdd(CommonInitialObjects.ARCHETYPE_OBJECT_MARK, initResult);
            repoAdd(CommonInitialObjects.MARK_PROTECTED, initResult);
        }
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        getSyncTask().init(this, initTask, initResult);
        runSyncTask(initResult); // necessary e.g. for live sync to obtain initial token (for recon should be idempotent)
    }

    abstract TestTask getSyncTask();

    void runSyncTask(OperationResult result) throws CommonException {
        getSyncTask().rerun(result);
    }

    @Test
    public void test110AddDummyEmeraldAccountMancomb() throws Exception {
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(6);

        DummyAccount account = new DummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");
        long loot = ACCOUNT_MANCOMB_VALID_FROM_DATE.getTime();
        account.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, loot);
        String gossip = XmlTypeConverter.createXMLGregorianCalendar(ACCOUNT_MANCOMB_VALID_TO_DATE).toXMLFormat();
        account.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, gossip);

        /// WHEN
        when();

        displayValue("Adding dummy account", account.debugDump());

        dummyResourceEmerald.addAccount(account);

        runSyncTask(getTestOperationResult());

        // THEN
        then();

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
                accountMancomb.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb was not created", userMancomb);
        assertLiveLinks(userMancomb, 1);
        assertAdministrativeStatusEnabled(userMancomb);
        assertValidFrom(userMancomb, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userMancomb, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userMancomb, accountMancomb);

        assertUsers(7);

        // notifications
        notificationManager.setDisabled(true);
    }

    @Test
    public void test120ModifyDummyEmeraldAccountMancombSeepbad() throws Exception {
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(7);

        DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        /// WHEN
        when();

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepbad");

        displayValue("Modified dummy account", account.debugDump());

        runSyncTask(getTestOperationResult());

        // THEN
        then();

        PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountAfter);
        assertNotNull("No mancomb account shadow", accountAfter);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
                accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);
        assertDummyAccountAttribute(RESOURCE_DUMMY_EMERALD_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepbad");

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userAfter);
        assertNotNull("User mancomb was not created", userAfter);
        assertLiveLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);
        assertValidFrom(userAfter, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userAfter, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userAfter, accountAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PolyString.fromOrig("Mancomb Seepbad"));

        assertUsers(7);

        // notifications
        notificationManager.setDisabled(true);
    }

    @Test
    public void test122ModifyDummyEmeraldAccountMancombSeepNULL() throws Exception {
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(7);

        DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        /// WHEN
        when();

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb SeepNULL");

        displayValue("Modified dummy account", account.debugDump());

        runSyncTask(getTestOperationResult());

        // THEN
        then();

        PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountAfter);
        assertNotNull("No mancomb account shadow", accountAfter);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
                accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);
        assertDummyAccountAttribute(RESOURCE_DUMMY_EMERALD_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb SeepNULL");

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userAfter);
        assertNotNull("User mancomb was not created", userAfter);
        assertLiveLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);
        assertValidFrom(userAfter, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userAfter, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userAfter, accountAfter);

        PrismAsserts.assertNoItem(userAfter, UserType.F_FULL_NAME);

        assertUsers(7);

        // notifications
        notificationManager.setDisabled(true);
    }

    @Test
    public void test124ModifyDummyEmeraldAccountMancombSeepevil() throws Exception {
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(7);

        DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        /// WHEN
        when();

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepevil");

        displayValue("Modified dummy account", account.debugDump());

        runSyncTask(getTestOperationResult());

        // THEN
        then();

        PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountAfter);
        assertNotNull("No mancomb account shadow", accountAfter);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
                accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);
        assertDummyAccountAttribute(RESOURCE_DUMMY_EMERALD_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepevil");

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userAfter);
        assertNotNull("User mancomb was not created", userAfter);
        assertLiveLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);
        assertValidFrom(userAfter, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userAfter, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userAfter, accountAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PolyString.fromOrig("Mancomb Seepevil"));

        assertUsers(7);

        // notifications
        notificationManager.setDisabled(true);
    }

    @Test
    public void test126ModifyDummyEmeraldAccountMancombTitlePirate() throws Exception {
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(7);

        DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        /// WHEN
        when();

        account.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");

        displayValue("Modified dummy account", account.debugDump());

        runSyncTask(getTestOperationResult());

        // THEN
        then();

        PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountAfter);
        assertNotNull("No mancomb account shadow", accountAfter);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
                accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);
        assertDummyAccountAttribute(RESOURCE_DUMMY_EMERALD_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userAfter);
        assertNotNull("User mancomb was not created", userAfter);
        assertLiveLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);
        assertValidFrom(userAfter, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userAfter, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userAfter, accountAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PolyString.fromOrig("Mancomb Seepevil"));
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_TITLE, PolyString.fromOrig("Pirate"));

        assertUsers(7);

        // notifications
        notificationManager.setDisabled(true);
    }

    @Test
    public void test127ModifyDummyEmeraldAccountMancombTitlePirateNull() throws Exception {
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(7);

        DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        /// WHEN
        when();

        account.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);

        displayValue("Modified dummy account", account.debugDump());

        runSyncTask(getTestOperationResult());

        // THEN
        then();

        PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountAfter);
        assertNotNull("No mancomb account shadow", accountAfter);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
                accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);
        assertDummyAccountAttribute(RESOURCE_DUMMY_EMERALD_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userAfter);
        assertNotNull("User mancomb was not created", userAfter);
        assertLiveLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);
        assertValidFrom(userAfter, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userAfter, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userAfter, accountAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PolyString.fromOrig("Mancomb Seepevil"));
        PrismAsserts.assertNoItem(userAfter, UserType.F_TITLE);

        assertUsers(7);

        // notifications
        notificationManager.setDisabled(true);
    }

    @Test
    public void test129ModifyDummyEmeraldAccountMancombSeepgood() throws Exception {
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(7);

        DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        /// WHEN
        when();

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");

        displayValue("Modified dummy account", account.debugDump());

        runSyncTask(getTestOperationResult());

        // THEN
        then();

        PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountAfter);
        assertNotNull("No mancomb account shadow", accountAfter);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
                accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);
        assertDummyAccountAttribute(RESOURCE_DUMMY_EMERALD_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userAfter);
        assertNotNull("User mancomb was not created", userAfter);
        assertLiveLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);
        assertValidFrom(userAfter, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userAfter, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userAfter, accountAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PolyString.fromOrig("Mancomb Seepgood"));

        assertUsers(7);

        // notifications
        notificationManager.setDisabled(true);
    }

    @Test
    public void test180NoChange() throws Exception {
        // default = no op
        // (method is here to be executed before test199)
    }

    @Test
    public abstract void test199DeleteDummyEmeraldAccountMancomb() throws Exception;

    @Test
    public void test300AddDummyEmeraldAccountPosixUser() throws Exception {
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(7);

        DummyAccount account = new DummyAccount(ACCOUNT_POSIXUSER_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Posix User");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");
        account.addAuxiliaryObjectClassName(DummyResourceContoller.DUMMY_POSIX_ACCOUNT_OBJECT_CLASS_NAME);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_POSIX_UID_NUMBER, Collections.<Object>singleton(1001));
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_POSIX_GID_NUMBER, Collections.<Object>singleton(10001));

        /// WHEN
        when();

        displayValue("Adding dummy account", account.debugDump());

        dummyResourceEmerald.addAccount(account);

        runSyncTask(getTestOperationResult());

        // THEN
        then();

        PrismObject<ShadowType> accountPosixUser = findAccountByUsername(ACCOUNT_POSIXUSER_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account posixuser", accountPosixUser);
        assertNotNull("No posixuser account shadow", accountPosixUser);
        assertEquals("Wrong resourceRef in posixuser account", RESOURCE_DUMMY_EMERALD_OID,
                accountPosixUser.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountPosixUser, SynchronizationSituationType.LINKED);

        PrismObject<UserType> userPosixUser = findUserByUsername(ACCOUNT_POSIXUSER_DUMMY_USERNAME);
        display("User posixuser", userPosixUser);
        assertNotNull("User posixuser was not created", userPosixUser);
        assertLiveLinks(userPosixUser, 1);
        assertAdministrativeStatusEnabled(userPosixUser);
        assertLinked(userPosixUser, accountPosixUser);

        assertUsers(8);

        // notifications
        notificationManager.setDisabled(true);

        // TODO create and test inbounds for uid and gid numbers; also other attributes
        // (Actually I'm not sure it will work, as even now the auxiliary object class is
        // removed right during the livesync. This has to be solved somehow...)
    }

    @Test
    public void test310ModifyDummyEmeraldAccountPosixUserUidNumber() throws Exception {
        // GIVEN
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(8);

        DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_POSIXUSER_DUMMY_USERNAME);

        /// WHEN
        when();

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_POSIX_UID_NUMBER, 1002);

        displayValue("Modified dummy account", account.debugDump());

        runSyncTask(getTestOperationResult());

        // THEN
        then();

        PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_POSIXUSER_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account posixuser", accountAfter);
        assertNotNull("No posixuser account shadow", accountAfter);
        assertEquals("Wrong resourceRef in posixuser account", RESOURCE_DUMMY_EMERALD_OID,
                accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_POSIXUSER_DUMMY_USERNAME);
        display("User posixuser", userAfter);
        assertNotNull("User posixuser was not created", userAfter);
        assertLiveLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);

        assertLinked(userAfter, accountAfter);

        assertUsers(8);

        // notifications
        notificationManager.setDisabled(true);

        // TODO create and test inbounds for uid and gid numbers; also other attributes
    }

    void rememberTimeBeforeSync() {
        timeBeforeSync = System.currentTimeMillis();
    }

    @SuppressWarnings("SameParameterValue")
    private void assertShadowOperationalData(PrismObject<ShadowType> shadow, SynchronizationSituationType expectedSituation) {
        ShadowType shadowType = shadow.asObjectable();
        SynchronizationSituationType actualSituation = shadowType.getSynchronizationSituation();
        assertEquals("Wrong situation in shadow " + shadow, expectedSituation, actualSituation);
        XMLGregorianCalendar actualTimestampCal = shadowType.getSynchronizationTimestamp();
        assert actualTimestampCal != null : "No synchronization timestamp in shadow " + shadow;
        long actualTimestamp = XmlTypeConverter.toMillis(actualTimestampCal);
        assert actualTimestamp >= timeBeforeSync : "Synchronization timestamp was not updated in shadow " + shadow;
        // TODO: assert sync description
    }

}
