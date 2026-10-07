/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.password;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowPurposeType;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Password test with HASHING storage for all credential types.
 *
 * @author semancik
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestPasswordDefaultHashing extends AbstractPasswordTest {

    private static final File USER_TEMPLATE_ACTIVATION_FILE = new File(TEST_DIR, "user-template-activation.xml");
    private static final String USER_TEMPLATE_ACTIVATION_OID = "5490aaaa-0000-0000-0000-000000000099";

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

    /**
     * The activation link must lead to the account activation authentication sequence and must carry the one-time
     * nonce that was stored in the user. A bare link with just the user identification is not acceptable.
     *
     * Issue: 5490
     */
    @Override
    protected void assertAccountActivationNotification(String dummyResourceName, String username) throws Exception {
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_ACTIVATION_NAME, 1);
        String body = getDummyTransportMessageBody(NOTIFIER_ACCOUNT_ACTIVATION_NAME, 0);
        if (!body.contains("activat")) {
            fail("Activation not mentioned in " + dummyResourceName + " dummy account activation notification message : " + body);
        }
        String expectedLinkPrefix = "/auth/accountActivation?user=" + username + "&token=";
        int linkStart = body.indexOf(expectedLinkPrefix);
        if (linkStart < 0) {
            fail("Link to the account activation sequence is missing in " + dummyResourceName + " dummy account activation notification message : " + body);
        }
        String token = body.substring(linkStart + expectedLinkPrefix.length()).split("\\s")[0];
        assertThat(token)
                .as("nonce token in activation link")
                .isNotBlank();

        PrismObject<UserType> user = findUserByUsernameFullRequired(username);
        NonceType nonce = user.asObjectable().getCredentials().getNonce();
        assertThat(nonce)
                .as("nonce stored in user " + username)
                .isNotNull();
        assertThat(protector.decryptString(nonce.getValue())).as("stored nonce").isEqualTo(token);
        assertThat(nonce.getSequenceIdentifier()).as("sequence the nonce was issued for").isEqualTo("account-activation");
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

    /**
     * Account activation done by the user with the authorizations of the account activation channel only.
     * The activation succeeds; the result is a warning, because the notifiers of this test read the resource
     * and the channel does not authorize that (a role would, see the next test).
     *
     * Issue: 5490
     */
    @Test
    public void test970ActivateOwnAccountAsUser() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        given("user with hashed password gets a red account, which cannot get the password");
        String userOid = addActivationUser("activator", task, result);
        String accountRedOid = getLiveLinkRefOid(getUser(userOid), RESOURCE_DUMMY_RED_OID);
        assertShadowPurpose(getShadowRepo(accountRedOid), ShadowPurposeType.INCOMPLETE);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, "activator", null);

        given("the user is logged in through the account activation channel");
        loginThroughActivationChannel(userOid);

        try {
            when("the user activates the account the way the activation page does");
            modelService.executeChanges(List.of(createActivationDelta(accountRedOid)), null, task, result);
        } finally {
            login(USER_ADMINISTRATOR_USERNAME);
        }

        then("the account is activated, the notifiers of this test could not read the resource as the user");
        result.computeStatusIfUnknown();
        display("Result", result);
        assertThat(result.getStatus()).as("result status").isEqualTo(OperationResultStatus.WARNING);
        assertThat(result.getMessage()).as("result message").contains("Couldn't resolve object " + RESOURCE_DUMMY_RED_OID);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, "activator", USER_PASSWORD_VALID_1);
        assertShadowPurpose(getShadowRepo(accountRedOid), null);
        assertUserPassword(getUser(userOid), USER_PASSWORD_VALID_1);
    }

    /**
     * Account activation with side effects on the user and the account (object template) and with notifications,
     * authorized by a role assigned to the user.
     *
     * Issue: 5490
     */
    @Test
    public void test972ActivateOwnAccountAsUserWithTemplateAndRole() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareTest();

        given("user with hashed password, red account without password, and the role for activation side effects");
        String userOid = addActivationUser("activator2", task, result);
        String accountRedOid = getLiveLinkRefOid(getUser(userOid), RESOURCE_DUMMY_RED_OID);
        assertShadowPurpose(getShadowRepo(accountRedOid), ShadowPurposeType.INCOMPLETE);
        String roleOid = addObject(createActivationSideEffectsRole().asPrismObject(), task, result);
        assignRole(userOid, roleOid, task, result);

        given("object template that changes the user whenever the clockwork runs");
        addObject(USER_TEMPLATE_ACTIVATION_FILE, task, result);
        setDefaultUserTemplate(USER_TEMPLATE_ACTIVATION_OID);

        given("the user is logged in through the account activation channel");
        loginThroughActivationChannel(userOid);

        try {
            when("the user activates the account the way the activation page does");
            modelService.executeChanges(List.of(createActivationDelta(accountRedOid)), null, task, result);
        } finally {
            login(USER_ADMINISTRATOR_USERNAME);
            setDefaultUserTemplate(null);
        }

        then();
        assertSuccess(result);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, "activator2", USER_PASSWORD_VALID_1);
        assertShadowPurpose(getShadowRepo(accountRedOid), null);
        PrismObject<UserType> userAfter = getUser(userOid);
        display("User after", userAfter);
        assertThat(userAfter.asObjectable().getDescription())
                .as("description set by the object template during activation")
                .startsWith("template run");
    }

    private String addActivationUser(String name, Task task, OperationResult result) throws Exception {
        UserType user = new UserType()
                .name(name)
                .fullName("Account Activator")
                .credentials(new CredentialsType()
                        .password(new PasswordType()
                                .value(protectedString(USER_PASSWORD_VALID_1))));
        String userOid = addObject(user.asPrismObject(), task, result);
        assignAccountToUser(userOid, RESOURCE_DUMMY_RED_OID, null, task, result);
        assertSuccess(result);
        return userOid;
    }

    /** The same delta as the activation page creates. */
    private ObjectDelta<ShadowType> createActivationDelta(String shadowOid) throws Exception {
        ObjectDelta<ShadowType> shadowDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(ShadowType.class, shadowOid,
                        SchemaConstants.PATH_PASSWORD_VALUE, protectedString(USER_PASSWORD_VALID_1));
        shadowDelta.addModificationReplaceProperty(ShadowType.F_PURPOSE, ShadowPurposeType.REGULAR);
        return shadowDelta;
    }

    /**
     * Principal as compiled for the account activation channel: assigned authorizations without other UI pages
     * plus what the channel grants. Keep in sync with AccountActivationAuthenticationChannel.
     */
    private void loginThroughActivationChannel(String userOid) throws Exception {
        MidPointPrincipal principal = getMidPointPrincipal(getUser(userOid));
        List<Authorization> authorizations = new ArrayList<>();
        for (Authorization assigned : principal.getAuthorities()) {
            Authorization resolved = assigned.clone();
            resolved.getAction().removeIf(action ->
                    !AuthorizationConstants.AUTZ_UI_ACCOUNT_ACTIVATION_URL.equals(action)
                            && action.contains(AuthorizationConstants.NS_AUTHORIZATION_UI));
            if (!resolved.getAction().isEmpty()) {
                authorizations.add(resolved);
            }
        }
        authorizations.addAll(accountActivationChannelAuthorizations());
        principal.resetAuthorizationsList(authorizations);
        login(principal);
    }

    private List<Authorization> accountActivationChannelAuthorizations() {
        return List.of(
                new Authorization(new AuthorizationType()
                        .name("account-activation-ui")
                        .action(AuthorizationConstants.AUTZ_UI_ACCOUNT_ACTIVATION_URL)),
                new Authorization(new AuthorizationType()
                        .name("account-activation-read-own-shadows")
                        .action(ModelAuthorizationAction.READ.getUrl())
                        .object(ownShadows())),
                new Authorization(new AuthorizationType()
                        .name("account-activation-activate-own-shadows")
                        .action(ModelAuthorizationAction.MODIFY.getUrl())
                        .object(ownShadows())
                        .item(new ItemPathType(ItemPath.create(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD)))
                        .item(new ItemPathType(ShadowType.F_PURPOSE))));
    }

    /** What an administrator assigns when the activation has side effects on the user and notifications are on. */
    private RoleType createActivationSideEffectsRole() {
        return new RoleType()
                .name("activation-side-effects")
                .authorization(new AuthorizationType()
                        .name("read-self")
                        .action(ModelAuthorizationAction.READ.getUrl())
                        .object(new OwnedObjectSelectorType().special(SpecialObjectSpecificationType.SELF)))
                .authorization(new AuthorizationType()
                        .name("read-resource-names")
                        .action(ModelAuthorizationAction.READ.getUrl())
                        .object(new OwnedObjectSelectorType().type(ResourceType.COMPLEX_TYPE))
                        .item(new ItemPathType(ItemPath.create(ResourceType.F_NAME))))
                .authorization(new AuthorizationType()
                        .name("modify-self-execution")
                        .action(ModelAuthorizationAction.MODIFY.getUrl())
                        .phase(AuthorizationPhaseType.EXECUTION)
                        .object(new OwnedObjectSelectorType().special(SpecialObjectSpecificationType.SELF)))
                .authorization(new AuthorizationType()
                        .name("modify-own-shadows-execution")
                        .action(ModelAuthorizationAction.MODIFY.getUrl())
                        .phase(AuthorizationPhaseType.EXECUTION)
                        .object(ownShadows()));
    }

    private OwnedObjectSelectorType ownShadows() {
        return new OwnedObjectSelectorType()
                .type(ShadowType.COMPLEX_TYPE)
                .owner(new SubjectedObjectSelectorType().special(SpecialObjectSpecificationType.SELF));
    }

    private ProtectedStringType protectedString(String clearValue) {
        ProtectedStringType ps = new ProtectedStringType();
        ps.setClearValue(clearValue);
        return ps;
    }
}
