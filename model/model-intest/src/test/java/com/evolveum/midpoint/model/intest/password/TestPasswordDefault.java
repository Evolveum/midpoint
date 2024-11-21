/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.password;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Password test with DEFAULT configuration of password storage.
 *
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestPasswordDefault extends AbstractPasswordTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

    }

    @Override
    protected String getSecurityPolicyOid() {
        return SECURITY_POLICY_OID;
    }

    @Override
    protected void assertShadowPurpose(PrismObject<ShadowType> shadow, boolean focusCreated) {
        assertShadowPurpose(shadow, null);
    }

    /**
     * Reconcile user after password policy change. There is a RED account with a strong password
     * mapping. The reconcile and the strong mapping will try to set the short password to RED account.
     * That fails on RED account password policy.
     */
    @Test
    public void test202ReconcileUserJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertLiveLinks(userBefore, 4);

        // If caching is enabled, there is a cached password on RED resource. Once it was legal, now it's illegal.
        // If the cache would be used, the reconciliation below would NOT fail, because there would be no delta -> no check.
        // We want the check to be done, so we need to invalidate the cache.
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_RED_OID);

        // WHEN
        when();
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertPartialError(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertLiveLinks(userAfter, 4);
        accountJackYellowOid = getLiveLinkRefOid(userAfter, RESOURCE_DUMMY_YELLOW_OID);

        // Check account in dummy resource (yellow): password is too short for this, original password should remain there
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        // Check account in dummy resource (red)
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_AA_CLEAR);

        // User and default dummy account should have unchanged passwords
        assertUserPassword(userAfter, USER_PASSWORD_AA_CLEAR);
        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_AA_CLEAR);

        // this one is not changed
        assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

        assertPasswordHistoryEntries(userAfter);
    }

    @Override
    protected void assert31xBluePasswordAfterAssignment(PrismObject<UserType> userAfter) throws Exception {
        assertDummyPassword(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);
        PrismObject<ShadowType> shadow = getBlueShadow(userAfter);
        assertIncompleteShadowPassword(shadow);
    }

    @Override
    protected void assert31xBluePasswordAfterPasswordChange(PrismObject<UserType> userAfter) throws Exception {
        // Password is set during the assign operation. As password mapping is weak it is never changed.
        assertDummyPassword(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);
        PrismObject<ShadowType> shadow = getBlueShadow(userAfter);
        assertIncompleteShadowPassword(shadow);
    }

    @Override
    protected void assertAccountActivationNotification(String dummyResourceName, String username) {
        // We have passwords here. We are not doing initialization.
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_ACTIVATION_NAME, 0);
    }

}
