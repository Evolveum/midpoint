/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.ModifyDnRequest;
import org.apache.directory.api.ldap.model.message.ModifyDnRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyDnResponse;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-conntest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractLdapSynchronizationTest extends AbstractLdapTest {

    protected static final String ACCOUNT_HT_UID = "ht";
    protected static final String ACCOUNT_HT_CN = "Herman Toothrot";
    protected static final String ACCOUNT_HT_GIVENNAME = "Herman";
    protected static final String ACCOUNT_HT_SN = "Toothrot";
    protected static final String ACCOUNT_HT_SN_MODIFIED = "Torquemeda Marley";

    protected static final String ACCOUNT_HTM_UID = "htm";
    protected static final String ACCOUNT_HTM_CN = "Horatio Torquemada Marley";

    protected static final String GROUP_MONKEYS_CN = "monkeys";
    protected static final String GROUP_MONKEYS_DESCRIPTION = "Monkeys of Monkey Island";

    protected static final String GROUP_FOOLS_CN = "fools";
    protected static final String GROUP_FOOLS_DESCRIPTION = "not quite the shilling";

    protected abstract void assertStepSyncToken(String syncTaskOid, int step, long tsStart, long tsEnd) throws ObjectNotFoundException, SchemaException;

    protected boolean syncCanDetectDelete() {
        return true;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        cleanupDelete(toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN));
        cleanupDelete(toAccountDn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN));
        cleanupDelete(toGroupDn(GROUP_MONKEYS_CN));
        cleanupDelete(toGroupDn(GROUP_FOOLS_CN));
    }

    @Override
    protected void assertAdditionalCapabilities(CapabilityCollectionType nativeCapabilities) {
        super.assertAdditionalCapabilities(nativeCapabilities);

        assertCapability(nativeCapabilities, LiveSyncCapabilityType.class);
    }

    @Test
    public void test800ImportSyncTask() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        getSyncTask().init(AbstractLdapSynchronizationTest.this, task, result);

        // THEN
        then();
        assertSuccess(result);

        getSyncTask().rerun(result);

        long tsEnd = System.currentTimeMillis();

        assertStepSyncToken(getSyncTask().oid, 0, tsStart, tsEnd);
    }

    @Test
    public void test801SyncAddAccountHt() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        addLdapAccount(ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);

        syncWait();

        getSyncTask().rerun(result);


        // THEN
        then();
        assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        displayUsers();

        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user " + ACCOUNT_HT_UID + " created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);

        assertStepSyncToken(getSyncTask().oid, 1, tsStart, tsEnd);
    }

    /**
     * Changing account sn directly.
     * Do not change cn here. But even if sn is not changed, this triggers rename in the AD case.
     */
    @Test
    public void test802ModifyAccountHtSn() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        LdapNetworkConnection connection = ldapConnect();
        Modification modSn = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "sn", ACCOUNT_HT_SN_MODIFIED);
        connection.modify(toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN), modSn);

        // reread to show the timestamps. Good for timestamp debugging.
        Entry entryBefore = assertLdapAccount(ACCOUNT_HT_UID, ACCOUNT_HT_CN);
        displayValue("HT AD entry", entryBefore);

        ldapDisconnect(connection);

        syncWait();

        getSyncTask().rerun(result);

        // THEN
        then();
        assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user " + ACCOUNT_HT_UID + " created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN_MODIFIED);

        assertStepSyncToken(getSyncTask().oid, 2, tsStart, tsEnd);

    }

    @Test
    public void test810SyncAddGroupMonkeys() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        if (needsGroupFakeMemberEntry()) {
            addLdapGroup(GROUP_MONKEYS_CN, GROUP_MONKEYS_DESCRIPTION, "uid=fake," + getPeopleLdapSuffix());
        } else {
            addLdapGroup(GROUP_MONKEYS_CN, GROUP_MONKEYS_DESCRIPTION);
        }

        syncWait();

        getSyncTask().rerun(result);

        // THEN
        then();
        assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        PrismObject<RoleType> role = findObjectByName(RoleType.class, GROUP_MONKEYS_CN);
        display("Role", role);
        assertNotNull("no role " + GROUP_MONKEYS_CN, role);
        PrismAsserts.assertPropertyValue(role, RoleType.F_DESCRIPTION, GROUP_MONKEYS_DESCRIPTION);
        assertNotNull("No role " + GROUP_MONKEYS_CN + " created", role);

        assertStepSyncToken(getSyncTask().oid, 3, tsStart, tsEnd);
    }

    @Test
    public void test817RenameAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        LdapNetworkConnection connection = ldapConnect();

        ModifyDnRequest modDnRequest = new ModifyDnRequestImpl();
        modDnRequest.setName(new Dn(toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN)));
        modDnRequest.setNewRdn(toAccountRdn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN));
        modDnRequest.setDeleteOldRdn(true);
        ModifyDnResponse modDnResponse = connection.modifyDn(modDnRequest);
        display("Modified " + toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN) + " -> " + toAccountRdn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN) + ": " + modDnResponse);

        doAdditionalRenameModifications(connection);

        ldapDisconnect(connection);

        syncWait();

        getSyncTask().rerun(result);

        // THEN
        then();
        assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HTM_UID);
        assertNotNull("No user " + ACCOUNT_HTM_UID + " created", user);
        assertUser(user, user.getOid(), ACCOUNT_HTM_UID, getAccountHtmCnAfterRename(), ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN_MODIFIED);
        assertNull("User " + ACCOUNT_HT_UID + " still exist", findUserByUsername(ACCOUNT_HT_UID));

        assertStepSyncToken(getSyncTask().oid, 4, tsStart, tsEnd);

    }

    protected void syncWait() throws InterruptedException {
        // Nothing to do here. It can be overridden in subclasses to give us better chance to "catch" the event (e.g. in timestamp-based sync).
    }

    protected String getAccountHtmCnAfterRename() {
        return ACCOUNT_HT_CN;
    }

    protected void doAdditionalRenameModifications(LdapNetworkConnection connection) throws LdapException {
        // Nothing to do here
    }

    @Test
    public void test818DeleteAccountHtm() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HTM_UID);

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        deleteLdapEntry(toAccountDn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN));

        syncWait();

        getSyncTask().rerun(result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        if (syncCanDetectDelete()) {
            assertNull("User " + ACCOUNT_HTM_UID + " still exist", findUserByUsername(ACCOUNT_HTM_UID));
            assertNull("User " + ACCOUNT_HT_UID + " still exist", findUserByUsername(ACCOUNT_HT_UID));
        } else {
            // Just delete the user so we have consistent state for subsequent tests
            deleteObject(UserType.class, user.getOid(), task, result);
        }

        assertStepSyncToken(getSyncTask().oid, 5, tsStart, tsEnd);
    }

    // TODO: sync with "ALL" object class

    @Test
    public void test819DeleteSyncTask() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        getSyncTask().suspend();
        deleteObject(TaskType.class, getSyncTask().oid);

        // THEN
        then();
        assertNoObject(TaskType.class, getSyncTask().oid, task, result);
    }

    @Test
    public void test820ImportSyncTaskInetOrgPerson() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        getSyncTaskInetOrgPerson().init(AbstractLdapSynchronizationTest.this, task, result);

        // THEN
        then();
        assertSuccess(result);

        getSyncTaskInetOrgPerson().rerun(result);

        long tsEnd = System.currentTimeMillis();

        PrismObject<TaskType> syncTask = getTask(getSyncTaskInetOrgPerson().oid);
        display("Sync task after start", syncTask);

        assertStepSyncToken(getSyncTaskInetOrgPerson().oid, 5, tsStart, tsEnd);
    }

    @Test
    public void test821SyncAddAccountHt() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        addLdapAccount(ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);
        getSyncTask().rerun(result);

        // THEN
        then();
        assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        displayUsers();

        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user " + ACCOUNT_HT_UID + " created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);

        assertStepSyncToken(getSyncTask().oid, 6, tsStart, tsEnd);
    }

    @Test
    public void test822ModifyAccountHt() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        LdapNetworkConnection connection = ldapConnect();
        Modification modCn = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "sn", ACCOUNT_HT_SN_MODIFIED);
        connection.modify(toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN), modCn);
        ldapDisconnect(connection);

        getSyncTask().rerun(result);

        // THEN
        then();
        assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user " + ACCOUNT_HT_UID + " created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN_MODIFIED);

        assertStepSyncToken(getSyncTask().oid, 7, tsStart, tsEnd);
    }

    /**
     * Add a new group. Check that this event is ignored (because group is not inetOrgPerson, object class limitation is set in the sync task).
     */
    @Test
    public void test830AddGroupFools() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        addLdapGroup(GROUP_FOOLS_CN, GROUP_FOOLS_DESCRIPTION, toGroupDn("nobody"));
        getSyncTask().rerun(result);

        // THEN
        then();
        assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        PrismObject<RoleType> roleFools = findObjectByName(RoleType.class, GROUP_FOOLS_CN);
        assertNull("Unexpected role " + roleFools, roleFools);

        assertStepSyncToken(getSyncTask().oid, 8, tsStart, tsEnd);
    }

    @Test
    public void test837RenameAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        LdapNetworkConnection connection = ldapConnect();

        ModifyDnRequest modDnRequest = new ModifyDnRequestImpl();
        modDnRequest.setName(new Dn(toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN)));
        modDnRequest.setNewRdn(toAccountRdn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN));
        modDnRequest.setDeleteOldRdn(true);
        ModifyDnResponse modDnResponse = connection.modifyDn(modDnRequest);
        display("Modified " + toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN) + " -> " + toAccountRdn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN) + ": " + modDnResponse);

        doAdditionalRenameModifications(connection);

        ldapDisconnect(connection);

        getSyncTask().rerun(result);

        // THEN
        then();
        assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HTM_UID);
        assertNotNull("No user " + ACCOUNT_HTM_UID + " created", user);
        assertUser(user, user.getOid(), ACCOUNT_HTM_UID, getAccountHtmCnAfterRename(), ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN_MODIFIED);
        assertNull("User " + ACCOUNT_HT_UID + " still exist", findUserByUsername(ACCOUNT_HT_UID));

        assertStepSyncToken(getSyncTask().oid, 9, tsStart, tsEnd);

    }

    // TODO: create object of a different object class. See that it is ignored by sync.

    @Test
    public void test838DeleteAccountHtm() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HTM_UID);

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        deleteLdapEntry(toAccountDn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN));

        getSyncTask().rerun(result);

        // THEN
        then();
        assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        if (syncCanDetectDelete()) {
            assertNull("User " + ACCOUNT_HTM_UID + " still exist", findUserByUsername(ACCOUNT_HTM_UID));
            assertNull("User " + ACCOUNT_HT_UID + " still exist", findUserByUsername(ACCOUNT_HT_UID));
        } else {
            // Just delete the user so we have consistent state for subsequent tests
            deleteObject(UserType.class, user.getOid(), task, result);
        }

        assertStepSyncToken(getSyncTask().oid, 10, tsStart, tsEnd);
    }

    @Test
    public void test849DeleteSyncTask() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        getSyncTask().suspend();
        deleteObject(TaskType.class, getSyncTask().oid);

        // THEN
        then();
        assertSuccess(result);

        assertNoObject(TaskType.class, getSyncTask().oid, task, result);
    }

}
