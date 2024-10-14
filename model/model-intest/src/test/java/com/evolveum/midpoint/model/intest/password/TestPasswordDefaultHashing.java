/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.password;

import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowPurposeType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsStorageTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Password test with HASHING storage for all credential types.
 *
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestPasswordDefaultHashing extends AbstractPasswordTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Override
    protected String getSecurityPolicyOid() {
        return SECURITY_POLICY_DEFAULT_STORAGE_HASHING_OID;
    }

    @Override
    protected CredentialsStorageTypeType getPasswordStorageType() {
        return CredentialsStorageTypeType.HASHING;
    }

    @Override
    protected void assertShadowPurpose(PrismObject<ShadowType> shadow, boolean focusCreated) {
        if (focusCreated) {
            assertShadowPurpose(shadow, null);
        } else {
            assertShadowPurpose(shadow, ShadowPurposeType.INCOMPLETE);
        }
    }

    /**
     * There is a RED account with a strong password
     * mapping. The reconcile and the strong mapping would normally try to set the short
     * password to RED account which would fail on RED account password policy. But not today.
     * As we do not have password cleartext in the user then no password change should happen.
     * And everything should go smoothly.
     */
    @Test
    public void test202ReconcileUserJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertLiveLinks(userBefore, 4);

        // WHEN
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

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
        assertDummyPassword(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null);
        PrismObject<ShadowType> shadow = getBlueShadow(userAfter);
        assertNoShadowPassword(shadow);
    }

    @Override
    protected void assert31xBluePasswordAfterPasswordChange(PrismObject<UserType> userAfter) throws Exception {
        // Password is set during the assign operation. As password mapping is weak it is never changed.
        assertDummyPassword(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_2);
        PrismObject<ShadowType> shadow = getBlueShadow(userAfter);
        assertIncompleteShadowPassword(shadow);
    }

    @Override
    protected void assertAccountActivationNotification(String dummyResourceName, String username) {
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_ACTIVATION_NAME, 1);
        String body = getDummyTransportMessageBody(NOTIFIER_ACCOUNT_ACTIVATION_NAME, 0);
        if (!body.contains("activat")) {
            fail("Activation not mentioned in "+dummyResourceName+" dummy account activation notification message : "+body);
        }
        if (!body.contains("activate/accounts")) {
            fail("Link seems to be missing in "+dummyResourceName+" dummy account activation notification message : "+body);
        }
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Attempt to create account on blue account should pass in this case.
     * User password is hashed, therefore account password is not set from user.
     * No need to panic.
     * MID-4791
     */
    @Test
    @Override
    public void test345AssignMonkeyAccountBlue() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();

        assignAccountToUser(USER_THREE_HEADED_MONKEY_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_THREE_HEADED_MONKEY_NAME);

        // CLEANUP
        displayCleanup();

        unassignAccountFromUser(USER_THREE_HEADED_MONKEY_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);

        PrismObject<UserType> userCleanup = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User cleanup", userCleanup);
        assertAssignments(userCleanup, 1);
        assertUserPassword(userCleanup, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_THREE_HEADED_MONKEY_NAME);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Let's assign yellow account. Attempt to create account on blue account
     * should pass in this case. User password is hashed, therefore account
     * password is not set from user. No need to panic.
     * MID-4791
     */
    @Test
    @Override
    public void test347AssignMonkeyAccountYellow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();

        assignAccountToUser(USER_THREE_HEADED_MONKEY_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_THREE_HEADED_MONKEY_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_THREE_HEADED_MONKEY_NAME);

        // CLEANUP
        displayCleanup();

        unassignAccountFromUser(USER_THREE_HEADED_MONKEY_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);

        PrismObject<UserType> userCleanup = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User cleanup", userCleanup);
        assertAssignments(userCleanup, 1);
        assertUserPassword(userCleanup, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_THREE_HEADED_MONKEY_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_THREE_HEADED_MONKEY_NAME);
    }

    /**
     * Monkey has password that does not comply with current password policy.
     * Let's assign yellow account. Yellow resource has a minimum password length
     * (resource-enforced). User password is hashed, therefore account
     * password is not set from user. No need to panic.
     * MID-4793
     */
    @Test
    @Override
    public void test966AssignMonkeyAccountYellow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        // WHEN
        when();

        assignAccountToUser(USER_THREE_HEADED_MONKEY_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_THREE_HEADED_MONKEY_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 4);
        assertUserPassword(userAfter, USER_PASSWORD_A_CLEAR);

        assertDummyAccount(null, USER_THREE_HEADED_MONKEY_NAME);
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_THREE_HEADED_MONKEY_NAME);
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_THREE_HEADED_MONKEY_NAME);
    }

}
