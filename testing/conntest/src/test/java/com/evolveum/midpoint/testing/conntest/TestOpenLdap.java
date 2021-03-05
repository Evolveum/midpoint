/*
 * Copyright (C) 2014-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.text.ParseException;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.util.GeneralizedTime;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationLockoutStatusCapabilityType;

/**
 * @author semancik
 */
public class TestOpenLdap extends AbstractLdapConnTest {

    @Override
    protected String getResourceOid() {
        return "2a7c7130-7a34-11e4-bdf6-001e8c717e5b";
    }

    @Override
    protected File getBaseDir() {
        return new File(MidPointTestConstants.TEST_RESOURCES_DIR, "openldap");
    }

    @Override
    public String getStartSystemCommand() {
        return "sudo " + getScriptDirectoryName() + "/openldap-start";
    }

    @Override
    public String getStopSystemCommand() {
        return "sudo " + getScriptDirectoryName() + "/openldap-stop";
    }

    @Override
    protected String getLdapServerHost() {
        return "localhost";
    }

    @Override
    protected int getLdapServerPort() {
        return 11389;
    }

    @Override
    protected String getLdapBindDn() {
        return "cn=admin,dc=example,dc=com";
    }

    @Override
    protected String getLdapBindPassword() {
        return "secret";
    }

    @Override
    protected String getAccount0Cn() {
        return "Riwibmix Juvotut (00000000)";
    }

    @Override
    protected int getSearchSizeLimit() {
        return 500;
    }

    @Override
    protected String getPeopleLdapSuffix() {
        return "ou=people," + getLdapSuffix();
    }

    @Override
    protected String getGroupsLdapSuffix() {
        return "ou=groups," + getLdapSuffix();
    }

    @Override
    protected String getLdapGroupObjectClass() {
        return "groupOfNames";
    }

    @Override
    protected String getLdapGroupMemberAttribute() {
        return "member";
    }

    @Override
    protected String getSyncTaskOid() {
        return "cd1e0ff2-0099-11e5-9e22-001e8c717e5b";
    }

    @Override
    protected boolean syncCanDetectDelete() {
        return false;
    }

    @Override
    protected boolean needsGroupFakeMemberEntry() {
        return true;
    }

    @Override
    protected void assertActivationCapability(ActivationCapabilityType activationCapabilityType) {
        assertNotNull("No activation capability", activationCapabilityType);

        ActivationLockoutStatusCapabilityType lockoutCapability = CapabilityUtil.getEnabledActivationLockoutStatus(activationCapabilityType);
        assertNotNull("No lockout capability", lockoutCapability);
        displayValue("Lockout capability", lockoutCapability);
    }

    @Override
    protected void assertStepSyncToken(String syncTaskOid, int step, long tsStart, long tsEnd)
            throws ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        Task task = taskManager.getTaskPlain(syncTaskOid, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismProperty<String> syncTokenProperty = task.getExtensionPropertyOrClone(SchemaConstants.SYNC_TOKEN);
        assertNotNull("No sync token in " + task, syncTokenProperty);
        String syncToken = syncTokenProperty.getRealValue();
        assertNotNull("No sync token in " + task, syncToken);
        displayValue("Sync token", syncToken);

        GeneralizedTime syncTokenGt;
        try {
            syncTokenGt = new GeneralizedTime(syncToken);
        } catch (ParseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        TestUtil.assertBetween("Wrong time in sync token: " + syncToken, roundTsDown(tsStart), roundTsUp(tsEnd), syncTokenGt.getCalendar().getTimeInMillis());

    }

    @Test
    public void test700CheckBarbossaLockoutStatus() throws Exception {
        // WHEN
        when();
        PrismObject<ShadowType> shadow = getShadowModel(accountBarbossaOid);

        // THEN
        then();
        display("Shadow (model)", shadow);
        ActivationType activation = shadow.asObjectable().getActivation();
        if (activation != null) {
            LockoutStatusType lockoutStatus = shadow.asObjectable().getActivation().getLockoutStatus();
            if (lockoutStatus != null && lockoutStatus != LockoutStatusType.NORMAL) {
                AssertJUnit.fail("Barbossa is locked!");
            }
        }

        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_PASSWORD_2);
    }

    @Test
    public void test702LockOutBarbossa() throws Exception {
        Entry entry = getLdapAccountByUid(USER_BARBOSSA_USERNAME);
        displayValue("LDAP Entry before", entry);

        // WHEN
        when();
        for (int i = 0; i < 10; i++) {
            try {
                ldapConnect(null, entry.getDn().toString(), "this password is wrong");
            } catch (SecurityException e) {
                // Good bad attempt
                continue;
            }
            assertNotReached();
        }

        // THEN
        then();

        entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        displayValue("LDAP Entry after", entry);

        PrismObject<ShadowType> shadow = getShadowModel(accountBarbossaOid);
        display("Shadow (model)", shadow);
        ActivationType activation = shadow.asObjectable().getActivation();
        assertNotNull("No activation", activation);
        LockoutStatusType lockoutStatus = shadow.asObjectable().getActivation().getLockoutStatus();
        assertEquals("Wrong lockout status", LockoutStatusType.LOCKED, lockoutStatus);
    }

    @Test
    public void test705UnlockBarbossaAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountBarbossaOid, null,
                SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, LockoutStatusType.NORMAL);

        // WHEN
        when();
        executeChanges(accountDelta, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<ShadowType> shadow = getShadowModel(accountBarbossaOid);
        display("Shadow (model)", shadow);

        ActivationType activation = shadow.asObjectable().getActivation();
        if (activation != null) {
            LockoutStatusType lockoutStatus = shadow.asObjectable().getActivation().getLockoutStatus();
            if (lockoutStatus != null && lockoutStatus != LockoutStatusType.NORMAL) {
                AssertJUnit.fail("Barbossa is locked!");
            }
        }

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        displayValue("LDAP Entry", entry);
        assertNoAttribute(entry, "pwdAccountLockedTime");

        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_PASSWORD_2);
    }

}
