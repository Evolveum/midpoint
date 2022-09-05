/*
 * Copyright (c) 2016-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.test.TestTask;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.TerminateSessionEvent;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationMessageSource;
import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class TestAbstractAuthenticationEvaluator<V, AC extends AbstractAuthenticationContext, T extends AuthenticationEvaluator<AC>> extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "security");

    protected static final String USER_GUYBRUSH_PASSWORD = "XmarksTHEspot";

    private static final TestTask TASK_TRIGGER_SCANNER_ON_DEMAND =
            new TestTask(COMMON_DIR, "task-trigger-scanner-on-demand.xml", "2ee5c2a9-0f46-438a-8748-7ac71f46a343");

    @Autowired private LocalizationMessageSource messageSource;
    @Autowired private GuiProfiledPrincipalManager focusProfileService;
    @Autowired private Clock clock;

    private MessageSourceAccessor messages;

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem(com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
     */

    public abstract T getAuthenticationEvaluator();
    public abstract AC getAuthenticationContext(String username, V value);

    public abstract V getGoodPasswordJack();
    public abstract V getBadPasswordJack();
    public abstract V getGoodPasswordGuybrush();
    public abstract V getBadPasswordGuybrush();
    public abstract V get103EmptyPasswordJack();
    public abstract String getEmptyPasswordExceptionMessageKey();

    public abstract AbstractCredentialType getCredentialUsedForAuthentication(UserType user);
    public abstract QName getCredentialType();

    public abstract void modifyUserCredential(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        TASK_TRIGGER_SCANNER_ON_DEMAND.initialize(this, initTask, initResult);

        messages = new MessageSourceAccessor(messageSource);

        ((AuthenticationEvaluatorImpl) getAuthenticationEvaluator()).focusProfileService = new GuiProfiledPrincipalManager() {

            @Override
            public <F extends FocusType, O extends ObjectType> PrismObject<F> resolveOwner(PrismObject<O> object) throws CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
                return focusProfileService.resolveOwner(object);
            }

            @Override
            public void updateFocus(MidPointPrincipal principal, Collection<? extends ItemDelta<?, ?>> itemDeltas) {
                focusProfileService.updateFocus(principal, itemDeltas);
            }

            @Override
            public GuiProfiledPrincipal getPrincipal(PrismObject<? extends FocusType> user) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
                return getPrincipal(user, null, null);
            }

            @Override
            public GuiProfiledPrincipal getPrincipal(PrismObject<? extends FocusType> user,
                    AuthorizationTransformer authorizationLimiter, OperationResult result)
                    throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
                GuiProfiledPrincipal principal = focusProfileService.getPrincipal(user);
                addFakeAuthorization(principal);
                return principal;
            }

            @Override
            public GuiProfiledPrincipal getPrincipal(String username, Class<? extends FocusType> clazz) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
                GuiProfiledPrincipal principal = focusProfileService.getPrincipal(username, clazz);
                addFakeAuthorization(principal);
                return principal;
            }

            @Override
            public GuiProfiledPrincipal getPrincipalByOid(String oid, Class<? extends FocusType> clazz) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
                GuiProfiledPrincipal principal = focusProfileService.getPrincipalByOid(oid, clazz);
                addFakeAuthorization(principal);
                return principal;
            }

            //TODO test maybe later?
            @Override
            public List<UserSessionManagementType> getLocalLoggedInPrincipals() {
                return null;
            }

            @Override
            public void terminateLocalSessions(TerminateSessionEvent terminateSessionEvent) {
                //TOTO test it
            }
        };
    }

    @Test
    public void test000Sanity() throws Exception {
        assertNotNull(getAuthenticationEvaluator());
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_JACK_USERNAME, UserType.class);
        assertPrincipalJack(principal);
    }

    @Test
    public void test100PasswordLoginGoodPasswordJack() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        Authentication authentication = getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));

        // THEN
        then();
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        assertGoodPasswordAuthentication(authentication, USER_JACK_USERNAME);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 0);
        assertFailedLoginsForBehavior(userAfter, 0);
        assertLastSuccessfulLogin(userAfter, startTs, endTs);
    }

    @Test
    public void test101PasswordLoginBadPasswordJack() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        try {

            // WHEN
            when();

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");

        } catch (BadCredentialsException e) {
            then();
            displayExpectedException(e);
            assertBadPasswordException(e);
        }
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 1);
        assertFailedLoginsForBehavior(userAfter, 1);
        assertUserLockout(userAfter, LockoutStatusType.NORMAL);
        assertLastFailedLogin(userAfter, startTs, endTs);
    }

    @Test
    public void test102PasswordLoginNullPasswordJack() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();

        try {

            // WHEN
            when();

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, null));

            AssertJUnit.fail("Unexpected success");

        } catch (BadCredentialsException e) {
            then();
            displayExpectedException(e);
            assertEmptyPasswordException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 1);
        assertFailedLoginsForBehavior(userAfter, 2);
        assertUserLockout(userAfter, LockoutStatusType.NORMAL);
    }

    @Test
    public void test103PasswordLoginEmptyPasswordJack() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();

        try {

            // WHEN
            when();

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, get103EmptyPasswordJack()));

            AssertJUnit.fail("Unexpected success");

        } catch (BadCredentialsException e) {
            then();
            displayExpectedException(e);
            assertEmptyPasswordException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 1);
        assertFailedLoginsForBehavior(userAfter, 3);
        assertUserLockout(userAfter, LockoutStatusType.NORMAL);
    }

    @Test
    public void test105PasswordLoginNullUsernameNullPassword() {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();

        try {

            // WHEN
            when();

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(null, null));

            AssertJUnit.fail("Unexpected success");

        } catch (UsernameNotFoundException e) {
            then();
            displayExpectedException(e);
            assertNoUserException(e);
        } catch (BadCredentialsException e) {
            then();
            displayExpectedException(e);
            assertEmptyPasswordException(e);
        }

    }

    @Test
    public void test106PasswordLoginEmptyUsernameBadPassword() {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();

        try {

            // WHEN
            when();

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext("", getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");

        } catch (UsernameNotFoundException e) {
            then();
            displayExpectedException(e);
            assertNoUserException(e);
        }
    }

    @Test
    public void test107PasswordLoginBadUsernameBadPassword() {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();

        try {

            // WHEN
            when();

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext("NoSuchUser", getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");

        } catch (UsernameNotFoundException e) {
            then();
            displayExpectedException(e);
            assertNoUserException(e);
        }
    }

    /**
     * Wait for 5 minutes. The failed login count should reset after 3 minutes. Therefore bad login
     * count should be one after we try to make a bad login.
     */
    @Test
    public void test125PasswordLoginBadPasswordJackAfterLockoutFailedAttemptsDuration() throws Exception {
        // GIVEN
        clock.overrideDuration("PT5M");

        ConnectionEnvironment connEnv = createConnectionEnvironment();
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        try {

            // WHEN
            when();

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");

        } catch (BadCredentialsException e) {
            // This is expected

            // THEN
            then();
            displayExpectedException(e);
            assertBadPasswordException(e);
        }
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 1);
        assertFailedLoginsForBehavior(userAfter, 4);
        assertLastFailedLogin(userAfter, startTs, endTs);
        assertUserLockout(userAfter, LockoutStatusType.NORMAL);
    }

    @Test
    public void test130PasswordLoginLockout() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (BadCredentialsException e) {
            displayExpectedException(e);
            assertBadPasswordException(e);
        }

        PrismObject<UserType> userBetween = getUser(USER_JACK_OID);
        display("user after", userBetween);
        assertFailedLoginsForCredentials(userBetween, 2);
        assertFailedLoginsForBehavior(userBetween, 5);
        assertUserLockout(userBetween, LockoutStatusType.NORMAL);

        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (BadCredentialsException e) {
            displayExpectedException(e);
            assertBadPasswordException(e);
        }

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        // THEN
        then();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 3);
        assertFailedLoginsForBehavior(userAfter, 6);
        assertLastFailedLogin(userAfter, startTs, endTs);
        assertUserLockout(userAfter, LockoutStatusType.LOCKED);
    }

    @Test
    public void test132PasswordLoginLockedoutGoodPassword() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();

        // WHEN
        when();
        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (LockedException e) {
            then();
            displayExpectedException(e);
            assertLockedException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 3);
        assertFailedLoginsForBehavior(userAfter, 7);
        assertUserLockout(userAfter, LockoutStatusType.LOCKED);
    }

    @Test
    public void test133PasswordLoginLockedoutBadPassword() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();

        // WHEN
        when();
        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (LockedException e) {
            then();
            displayExpectedException(e);

            // This is important.
            // The exception should give no indication whether the password is good or bad.
            assertLockedException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 3);
        assertFailedLoginsForBehavior(userAfter, 8);
        assertUserLockout(userAfter, LockoutStatusType.LOCKED);
    }

    @Test
    public void test135PasswordLoginLockedoutLockExpires() throws Exception {
        // GIVEN
        clock.overrideDuration("PT30M");

        ConnectionEnvironment connEnv = createConnectionEnvironment();
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        Authentication authentication = getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));

        // THEN
        then();
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        assertGoodPasswordAuthentication(authentication, USER_JACK_USERNAME);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 0);
        assertFailedLoginsForBehavior(userAfter, 0);
        assertLastSuccessfulLogin(userAfter, startTs, endTs);
        assertUserLockout(userAfter, LockoutStatusType.NORMAL);
    }

    @Test
    public void test136PasswordLoginLockoutAgain() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (BadCredentialsException e) {
            then();
            displayExpectedException(e);
            assertBadPasswordException(e);
        }

        PrismObject<UserType> userBetween = getUser(USER_JACK_OID);
        display("user after", userBetween);
        assertFailedLoginsForCredentials(userBetween, 1);
        assertFailedLoginsForBehavior(userBetween, 1);
        assertUserLockout(userBetween, LockoutStatusType.NORMAL);

        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (BadCredentialsException e) {
            then();
            displayExpectedException(e);
            assertBadPasswordException(e);
        }

        userBetween = getUser(USER_JACK_OID);
        display("user after", userBetween);
        assertFailedLoginsForCredentials(userBetween, 2);
        assertFailedLoginsForBehavior(userBetween, 2);
        assertUserLockout(userBetween, LockoutStatusType.NORMAL);

        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (BadCredentialsException e) {
            then();
            displayExpectedException(e);
            assertBadPasswordException(e);
        }

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 3);
        assertFailedLoginsForBehavior(userAfter, 3);
        assertLastFailedLogin(userAfter, startTs, endTs);
        assertUserLockout(userAfter, LockoutStatusType.LOCKED);
    }

    @Test
    public void test137PasswordLoginLockedoutGoodPasswordAgain() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();

        // WHEN
        when();
        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (LockedException e) {
            then();
            displayExpectedException(e);
            assertLockedException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 3);
        assertFailedLoginsForBehavior(userAfter, 4);
        assertUserLockout(userAfter, LockoutStatusType.LOCKED);
    }

    @Test
    public void test138UnlockUserGoodPassword() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ConnectionEnvironment connEnv = createConnectionEnvironment();

        when("trigger scanner runs (after 30 minutes) - should clear the lockout flag");
        clock.overrideDuration("PT30M");
        TASK_TRIGGER_SCANNER_ON_DEMAND.rerun(result);
        clock.resetOverride();

        then("user is unlocked");

        TASK_TRIGGER_SCANNER_ON_DEMAND.assertAfter(); // just show the task

        PrismObject<UserType> userBetween = getUser(USER_JACK_OID);
        display("user after", userBetween);
        assertFailedLoginsForCredentials(userBetween, 0);
        assertFailedLoginsForBehavior(userBetween, 0);
        assertUserLockout(userBetween, LockoutStatusType.NORMAL);

        given("preparing for new login");
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        when("a good password is provided");
        Authentication authentication =
                getAuthenticationEvaluator()
                        .authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));

        then("everything is OK");
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        assertGoodPasswordAuthentication(authentication, USER_JACK_USERNAME);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 0);
        assertFailedLoginsForBehavior(userAfter, 0);
        assertLastSuccessfulLogin(userAfter, startTs, endTs);
        assertUserLockout(userAfter, LockoutStatusType.NORMAL);
    }

    /**
     * MID-2862
     */
    @Test
    public void test139TryToLockByModelService() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        try {

            modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, task, result, LockoutStatusType.LOCKED);

            AssertJUnit.fail("Unexpected success");
        } catch (SchemaException e) {
            then();
            displayExpectedException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 0);
        assertFailedLoginsForBehavior(userAfter, 0);
        assertUserLockout(userAfter, LockoutStatusType.NORMAL);
    }

    @Test
    public void test150PasswordLoginDisabledGoodPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

        loginJackGoodPasswordExpectDenied();
    }

    @Test
    public void test152PasswordLoginEnabledGoodPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);

        loginJackGoodPasswordExpectSuccess();
    }

    @Test
    public void test154PasswordLoginNotValidYetGoodPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        XMLGregorianCalendar validFrom = XmlTypeConverter.addDuration(clock.currentTimeXMLGregorianCalendar(), "PT1H");
        XMLGregorianCalendar validTo = XmlTypeConverter.addDuration(clock.currentTimeXMLGregorianCalendar(), "P2D");

        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result);
        modifyUserReplace(USER_JACK_OID, ACTIVATION_VALID_FROM_PATH, task, result, validFrom);
        modifyUserReplace(USER_JACK_OID, ACTIVATION_VALID_TO_PATH, task, result, validTo);

        loginJackGoodPasswordExpectDenied();
    }

    @Test
    public void test155PasswordLoginValidGoodPassword() throws Exception {
        // GIVEN
        clock.overrideDuration("PT2H");

        loginJackGoodPasswordExpectSuccess();
    }

    @Test
    public void test156PasswordLoginNotValidAnyLongerGoodPassword() throws Exception {
        // GIVEN
        clock.overrideDuration("P2D");

        loginJackGoodPasswordExpectDenied();
    }

    @Test
    public void test159PasswordLoginNoLongerValidEnabledGoodPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);

        loginJackGoodPasswordExpectSuccess();
    }

    @Test
    public void test160PasswordLoginLifecycleActiveGoodPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result,
                SchemaConstants.LIFECYCLE_ACTIVE);

        loginJackGoodPasswordExpectSuccess();
    }

    @Test
    public void test162PasswordLoginLifecycleDraftGoodPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result,
                SchemaConstants.LIFECYCLE_DRAFT);

        loginJackGoodPasswordExpectDenied();
    }

    @Test
    public void test164PasswordLoginLifecycleDeprecatedGoodPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result,
                SchemaConstants.LIFECYCLE_DEPRECATED);

        loginJackGoodPasswordExpectSuccess();
    }

    @Test
    public void test166PasswordLoginLifecycleProposedGoodPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result,
                SchemaConstants.LIFECYCLE_PROPOSED);

        loginJackGoodPasswordExpectDenied();
    }

    @Test
    public void test168PasswordLoginLifecycleArchivedGoodPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result,
                SchemaConstants.LIFECYCLE_ARCHIVED);

        loginJackGoodPasswordExpectDenied(2);
    }

    @Test
    public void test200UserGuybrushSetCredentials() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        modifyUserCredential(task, result);

        // THEN
        then();
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("user after", userAfter);

//        assertEncryptedUserPassword(userAfter, USER_GUYBRUSH_PASSWORD);
        assertPasswordMetadata(userAfter, getCredentialType(), false, startTs, endTs, null, SchemaConstants.CHANNEL_USER_URI);

        assertFailedLoginsForCredentials(userAfter, 0);
        assertFailedLoginsForBehavior(userAfter, 0);
    }

    @Test
    public void test201UserGuybrushPasswordLoginGoodPassword() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        Authentication authentication = getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_GUYBRUSH_USERNAME, getGoodPasswordGuybrush()));

        // THEN
        then();
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        assertGoodPasswordAuthentication(authentication, USER_GUYBRUSH_USERNAME);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 0);
        assertFailedLoginsForBehavior(userAfter, 0);
        assertLastSuccessfulLogin(userAfter, startTs, endTs);
    }

    @Test
    public void test202UserGuybrushPasswordLoginBadPassword() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        try {

            // WHEN
            when();

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_GUYBRUSH_USERNAME, getBadPasswordGuybrush()));

            AssertJUnit.fail("Unexpected success");

        } catch (BadCredentialsException e) {
            then();
            displayExpectedException(e);
            assertBadPasswordException(e);
        }
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 1);
        assertFailedLoginsForBehavior(userAfter, 1);
        assertLastFailedLogin(userAfter, startTs, endTs);
    }

    @Test
    public void test209UserGuybrushPasswordLoginGoodPasswordBeforeExpiration() throws Exception {
        // GIVEN
        clock.overrideDuration("P29D");

        ConnectionEnvironment connEnv = createConnectionEnvironment();
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        Authentication authentication = getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_GUYBRUSH_USERNAME, getGoodPasswordGuybrush()));

        // THEN
        then();
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        assertGoodPasswordAuthentication(authentication, USER_GUYBRUSH_USERNAME);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 0);
        assertFailedLoginsForBehavior(userAfter, 0);
        assertLastSuccessfulLogin(userAfter, startTs, endTs);
    }

    @Test
    public void test210UserGuybrushPasswordLoginGoodPasswordExpired() throws Exception {
        // GIVEN
        clock.overrideDuration("P2D");

        ConnectionEnvironment connEnv = createConnectionEnvironment();

        try {

            // WHEN
            when();

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_GUYBRUSH_USERNAME, getGoodPasswordGuybrush()));

            AssertJUnit.fail("Unexpected success");

        } catch (CredentialsExpiredException e) {
            then();
            displayExpectedException(e);
            assertExpiredException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 0);
        assertFailedLoginsForBehavior(userAfter, 1);
    }

    private void assertGoodPasswordAuthentication(Authentication authentication, String expectedUsername) {
        assertNotNull("No authentication", authentication);
        assertTrue("authentication: not authenticated", authentication.isAuthenticated());
        MidPointAsserts.assertInstanceOf("authentication", authentication, UsernamePasswordAuthenticationToken.class);
        assertEquals("authentication: principal mismatch", expectedUsername, ((MidPointPrincipal) authentication.getPrincipal()).getUsername());
    }

    private void assertBadPasswordException(BadCredentialsException e) {
        assertEquals("Wrong exception meessage (key)", messages.getMessage("web.security.provider.invalid"), getTranslatedMessage(e));
    }

    private void assertEmptyPasswordException(BadCredentialsException e) {
        assertEquals("Wrong exception meessage (key)", messages.getMessage(getEmptyPasswordExceptionMessageKey()), getTranslatedMessage(e));
    }

    private String getTranslatedMessage(Throwable t) {
        return localizationService.translate(t.getMessage(), new Object[0], Locale.getDefault());
    }

    private void assertPasswordEncodingException(BadCredentialsException e) {
        assertEquals("Wrong exception meessage (key)", messages.getMessage("web.security.provider.password.encoding"), getTranslatedMessage(e));
    }

    private void assertLockedException(LockedException e) {
        assertEquals("Wrong exception meessage (key)", messages.getMessage("web.security.provider.locked"), getTranslatedMessage(e));
    }

    private void assertDisabledException(DisabledException e) {
        assertEquals("Wrong exception meessage (key)", messages.getMessage("web.security.provider.disabled"), getTranslatedMessage(e));
    }

    private void assertExpiredException(CredentialsExpiredException e) {
        assertEquals("Wrong exception meessage (key)", messages.getMessage("web.security.provider.credential.expired"), getTranslatedMessage(e));
    }

    private void assertNoUserException(UsernameNotFoundException e) {
        assertEquals("Wrong exception meessage (key)", messages.getMessage("web.security.provider.invalid"), getTranslatedMessage(e));
    }

    private ConnectionEnvironment createConnectionEnvironment() {
        HttpConnectionInformation connInfo = new HttpConnectionInformation();
        connInfo.setRemoteHostAddress("remote.example.com");
        return new ConnectionEnvironment(null, connInfo);
    }

    private void assertFailedLoginsForCredentials(PrismObject<UserType> user, int expected) {
        if (expected == 0 && getCredentialUsedForAuthentication(user.asObjectable()).getFailedLogins() == null) {
            return;
        }
        assertEquals("Wrong failed logins in " + user, (Integer) expected, getCredentialUsedForAuthentication(user.asObjectable()).getFailedLogins());
    }

    private void assertFailedLoginsForBehavior(PrismObject<UserType> user, int expected) {
        if (expected == 0 && getAuthenticationBehavior(user.asObjectable()).getFailedLogins() == null) {
            return;
        }
        assertEquals("Wrong failed logins in " + user, (Integer) expected, getAuthenticationBehavior(user.asObjectable()).getFailedLogins());
    }

    private void assertLastSuccessfulLogin(PrismObject<UserType> user, XMLGregorianCalendar startTs,
            XMLGregorianCalendar endTs) {
        LoginEventType lastSuccessfulLogin = getCredentialUsedForAuthentication(user.asObjectable()).getLastSuccessfulLogin();
        assertNotNull("no last successful login in " + user, lastSuccessfulLogin);
        XMLGregorianCalendar successfulLoginTs = lastSuccessfulLogin.getTimestamp();
        TestUtil.assertBetween("wrong last successful login timestamp", startTs, endTs, successfulLoginTs);

        LoginEventType lastSuccessfulLoginFromBehavior = getAuthenticationBehavior(user.asObjectable()).getLastSuccessfulLogin();
        assertNotNull("no last successful login in " + user, lastSuccessfulLoginFromBehavior);
        XMLGregorianCalendar successfulLoginTsFromBehavior = lastSuccessfulLoginFromBehavior.getTimestamp();
        TestUtil.assertBetween("wrong last successful login timestamp", startTs, endTs, successfulLoginTsFromBehavior);
    }

    private void assertLastFailedLogin(PrismObject<UserType> user, XMLGregorianCalendar startTs,
            XMLGregorianCalendar endTs) {
        LoginEventType lastFailedLogin = getCredentialUsedForAuthentication(user.asObjectable()).getLastFailedLogin();
        assertNotNull("no last failed login in " + user, lastFailedLogin);
        XMLGregorianCalendar failedLoginTs = lastFailedLogin.getTimestamp();
        TestUtil.assertBetween("wrong last failed login timestamp", startTs, endTs, failedLoginTs);

        LoginEventType lastFailedLoginFromBehavior = getAuthenticationBehavior(user.asObjectable()).getLastFailedLogin();
        assertNotNull("no last failed login in " + user, lastFailedLoginFromBehavior);
        XMLGregorianCalendar failedLoginTsFromBehavior = lastFailedLoginFromBehavior.getTimestamp();
        TestUtil.assertBetween("wrong last failed login timestamp", startTs, endTs, failedLoginTsFromBehavior);
    }

    private void addFakeAuthorization(MidPointPrincipal principal) {
        if (principal == null) {
            return;
        }
        if (principal.getAuthorities().isEmpty()) {
            AuthorizationType authorizationType = new AuthorizationType();
            authorizationType.getAction().add("FAKE");
            principal.getAuthorities().add(new Authorization(authorizationType));
        }
    }

    private void assertPrincipalJack(MidPointPrincipal principal) {
        displayDumpable("principal", principal);
        assertEquals("Bad principal name", USER_JACK_USERNAME, principal.getName().getOrig());
        assertEquals("Bad principal name", USER_JACK_USERNAME, principal.getUsername());
        FocusType user = principal.getFocus();
        assertNotNull("No user in principal", user);
        assertEquals("Bad name in user in principal", USER_JACK_USERNAME, user.getName().getOrig());
    }

    private void loginJackGoodPasswordExpectSuccess()
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        displayValue("now", clock.currentTimeXMLGregorianCalendar());
        ConnectionEnvironment connEnv = createConnectionEnvironment();
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        Authentication authentication = getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));

        // THEN
        then();
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        assertGoodPasswordAuthentication(authentication, USER_JACK_USERNAME);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 0);
        assertFailedLoginsForBehavior(userAfter, 0);
        assertLastSuccessfulLogin(userAfter, startTs, endTs);
    }

    private void loginJackGoodPasswordExpectDenied() throws ObjectNotFoundException,
            SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        loginJackGoodPasswordExpectDenied(1);
    }

    private void loginJackGoodPasswordExpectDenied(int expectedFailInBehavior) throws ObjectNotFoundException,
            SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        displayValue("now", clock.currentTimeXMLGregorianCalendar());
        ConnectionEnvironment connEnv = createConnectionEnvironment();

        // WHEN
        when();
        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (DisabledException e) {
            then();
            displayExpectedException(e);

            // This is important.
            // The exception should give no indication whether the password is good or bad.
            assertDisabledException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForCredentials(userAfter, 0);
        assertFailedLoginsForBehavior(userAfter, expectedFailInBehavior);
    }

    public AuthenticationBehavioralDataType getAuthenticationBehavior(UserType user) {
        if (user.getBehavior() == null || user.getBehavior().getAuthentication() == null) {
            return new AuthenticationBehavioralDataType();
        }
        return user.getBehavior().getAuthentication();
    }
}
