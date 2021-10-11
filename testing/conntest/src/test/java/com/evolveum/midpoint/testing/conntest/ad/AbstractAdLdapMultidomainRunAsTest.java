/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.ad;

import java.util.List;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.RunAsCapabilityType;

/**
 * @author semancik
 */
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class AbstractAdLdapMultidomainRunAsTest extends AbstractAdLdapMultidomainTest {

    @Override
    protected void assertAdditionalCapabilities(List<Object> nativeCapabilities) {
        super.assertAdditionalCapabilities(nativeCapabilities);

        assertCapability(nativeCapabilities, RunAsCapabilityType.class);
    }

    /**
     * Try to set the same password again. If this is "admin mode" (no runAs capability - in superclass)
     * the such change should be successful. In "selfservice mode" (runAs capability)
     * this change should fail.
     */
    @Test
    @Override
    public void test222ModifyUserBarbossaPasswordSelfServicePassword1Again() throws Exception {
        testModifyUserBarbossaPasswordSelfServiceFailure(
                USER_BARBOSSA_PASSWORD_AD_1, USER_BARBOSSA_PASSWORD_AD_1);

        assertUserAfter(USER_BARBOSSA_OID)
                .assertPassword(USER_BARBOSSA_PASSWORD_AD_1);
    }

    /**
     * Change password back to the first password. This password was used before.
     * In admin mode (in superclass) this should go well. Admin can set password to anything.
     * But in self-service mode this should fail due to password history check.
     * MID-5242
     */
    @Test
    @Override
    public void test226ModifyUserBarbossaPasswordSelfServicePassword1AgainAgain() throws Exception {
        testModifyUserBarbossaPasswordSelfServiceFailure(
                USER_BARBOSSA_PASSWORD_AD_2, USER_BARBOSSA_PASSWORD_AD_1);
    }

    /**
     * Now we have strange situation. Password in midPoint was changed. But AD password was not.
     * Attempt to change the password again should fail on AD, because old password no longer matches.
     * But we care that this is the right way of failure. User should get reasonable error message.
     * There should be no pending operation in shadow - retrying the operation does not make sense.
     */
    @Test
    public void test228ModifyUserBarbossaPasswordSelfServiceDesynchronized() throws Exception {
        // GIVEN

        // preconditions
        assertUserBefore(USER_BARBOSSA_OID)
                .assertPassword(USER_BARBOSSA_PASSWORD_AD_1);
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, USER_BARBOSSA_PASSWORD_AD_2);

        login(USER_BARBOSSA_USERNAME);

        Task task = getTestTask();
        task.setChannel(SchemaConstants.CHANNEL_GUI_SELF_SERVICE_URI);
        OperationResult result = task.getResult();

        ObjectDelta<UserType> objectDelta = createOldNewPasswordDelta(USER_BARBOSSA_OID,
                USER_BARBOSSA_PASSWORD_AD_1, USER_BARBOSSA_PASSWORD_AD_3);

        // WHEN
        when();
        executeChanges(objectDelta, null, task, result);

        // THEN
        then();
        login(USER_ADMINISTRATOR_USERNAME);
        display(result);
        assertPartialError(result);
        assertMessageContains(result.getMessage(), "CONSTRAINT_ATT_TYPE");

        assertBarbossaEnabled(USER_BARBOSSA_PASSWORD_AD_1);
        assertUserAfter(USER_BARBOSSA_OID)
                .assertPassword(USER_BARBOSSA_PASSWORD_AD_3)
                .singleLink()
                .resolveTarget()
                .pendingOperations()
                .assertNone();

        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, USER_BARBOSSA_PASSWORD_AD_2);

        assertLdapConnectorInstances(2);
    }

}
