/*
 * Copyright (c) 2016-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.evaluator;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.authentication.api.evaluator.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.AutheticationFailedData;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.FocusAuthenticationResultRecorder;
import com.evolveum.midpoint.authentication.impl.channel.GuiAuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.evaluator.CredentialsAuthenticationEvaluatorImpl;

import com.evolveum.midpoint.authentication.impl.filter.SequenceAuditFilter;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.model.impl.AbstractModelImplementationIntegrationTest;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;

import org.jetbrains.annotations.NotNull;
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
import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationMessageSource;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = "classpath:ctx-authentication-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class TestAbstractAuthenticationEvaluator<V, AC extends AbstractAuthenticationContext, T extends AuthenticationEvaluator<AC, ?>> extends AbstractModelImplementationIntegrationTest {

    public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
    private static final File SECURITY_POLICY_FILE = new File(COMMON_DIR, "security-policy.xml");
    private static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
    private static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");

    private static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
    protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
    private static final String USER_JACK_USERNAME = "jack";
    static final String USER_JACK_PASSWORD = "deadmentellnotales";

    private static final TestObject<UserType> USER_PAINTER = TestObject.file(
            COMMON_DIR, "user-painter.xml", "4e6b2224-4577-4558-a48b-fac1078124b8");
    private static final String USER_PAINTER_NAME = "painter";

    // painter (user) -> blue (role) -> yellow (role);
    // and red role is not assigned
    private static final TestObject<RoleType> ROLE_RED = TestObject.file(
            COMMON_DIR, "role-red.xml", "11cc2082-5e60-480b-9ac0-002ba20ab5c5");
    private static final TestObject<RoleType> ROLE_YELLOW = TestObject.file(
            COMMON_DIR, "role-yellow.xml", "b40d6287-5c76-4a45-a61a-3cedebcf99b2");
    private static final TestObject<RoleType> ROLE_BLUE = TestObject.file(
            COMMON_DIR, "role-blue.xml", "8eed0da1-e949-4d0d-b154-0a81167e287b");

    private static final File USER_GUYBRUSH_FILE = new File(COMMON_DIR, "user-guybrush.xml");
    static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
    private static final String USER_GUYBRUSH_USERNAME = "guybrush";
    static final String USER_GUYBRUSH_PASSWORD = "XmarksTHEspot";

    private static final TestTask TASK_TRIGGER_SCANNER_ON_DEMAND =
            new TestTask(COMMON_DIR, "task-trigger-scanner-on-demand.xml", "2ee5c2a9-0f46-438a-8748-7ac71f46a343");

    @Autowired private LocalizationMessageSource messageSource;
    @Autowired private GuiProfiledPrincipalManager focusProfileService;
    @Autowired private Clock clock;
    @Autowired private FocusAuthenticationResultRecorder authenticationRecorder;

    private MessageSourceAccessor messages;

    private SequenceAuditFilter auditFilter;

    public abstract T getAuthenticationEvaluator();
    public abstract AC getAuthenticationContext(String username, V value, List<ObjectReferenceType> requiredAssignments);

    private AC getAuthenticationContext(String username, V value) {
        return getAuthenticationContext(username, value, List.of());
    }

    public abstract V getGoodPasswordJack();
    public abstract V getBadPasswordJack();
    public abstract V getGoodPasswordGuybrush();
    public abstract V getBadPasswordGuybrush();
    public abstract V get103EmptyPasswordJack();
    public abstract String getEmptyPasswordExceptionMessageKey();

    @Deprecated
    public abstract AbstractCredentialType getCredentialUsedForAuthentication(UserType user);

    public abstract String getModuleIdentifier();
    public abstract String getSequenceIdentifier();
    public abstract QName getCredentialType();

    public abstract void modifyUserCredential(Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // System Configuration
        try {
            repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
                    "looks like the previous test haven't cleaned it up", e);
        }
        modelService.postInit(initResult);

        repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);

        // Administrator
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
        PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
        login(userAdministrator);

        // Roles
        addObject(ROLE_RED, initTask, initResult);
        addObject(ROLE_YELLOW, initTask, initResult);
        addObject(ROLE_BLUE, initTask, initResult);

        // Users
        addObject(USER_PAINTER, initTask, initResult); // using model because of assignments

        repoAddObjectFromFile(USER_JACK_FILE, UserType.class, initResult).asObjectable();
        repoAddObjectFromFile(USER_GUYBRUSH_FILE, UserType.class, initResult).asObjectable();

        TASK_TRIGGER_SCANNER_ON_DEMAND.init(this, initTask, initResult);

        messages = new MessageSourceAccessor(messageSource);

        ((CredentialsAuthenticationEvaluatorImpl) getAuthenticationEvaluator()).setPrincipalManager(new GuiProfiledPrincipalManager() {

            @Override
            public <F extends FocusType, O extends ObjectType> Collection<PrismObject<F>> resolveOwner(PrismObject<O> object) throws CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
                return focusProfileService.resolveOwner(object);
            }

            @Override
            public void updateFocus(MidPointPrincipal principal, Collection<? extends ItemDelta<?, ?>> itemDeltas) {
                focusProfileService.updateFocus(principal, itemDeltas);
            }

            @Override
            public GuiProfiledPrincipal getPrincipal(
                    PrismObject<? extends FocusType> user, ProfileCompilerOptions options, OperationResult result)
                    throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
                    ExpressionEvaluationException {
                return getPrincipal(user, null, options, result);
            }

            @Override
            public GuiProfiledPrincipal getPrincipal(PrismObject<? extends FocusType> user,
                    AuthorizationTransformer authorizationLimiter, ProfileCompilerOptions options, OperationResult result)
                    throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
                    ExpressionEvaluationException {
                GuiProfiledPrincipal principal = focusProfileService.getPrincipal(user, options, result);
                addFakeAuthorization(principal);
                return principal;
            }

            @Override
            public GuiProfiledPrincipal getPrincipal(
                    String username, Class<? extends FocusType> clazz, ProfileCompilerOptions options)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
                    SecurityViolationException, ExpressionEvaluationException {
                GuiProfiledPrincipal principal = focusProfileService.getPrincipal(username, clazz, options);
                addFakeAuthorization(principal);
                return principal;
            }

            @Override
            public GuiProfiledPrincipal getPrincipal(
                    ObjectQuery query, Class<? extends FocusType> clazz, ProfileCompilerOptions options)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
                    SecurityViolationException, ExpressionEvaluationException {
                GuiProfiledPrincipal principal = focusProfileService.getPrincipal(query, clazz, options);
                addFakeAuthorization(principal);
                return principal;
            }

            @Override
            public GuiProfiledPrincipal getPrincipalByOid(
                    String oid, Class<? extends FocusType> clazz, ProfileCompilerOptions options)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
                    SecurityViolationException, ExpressionEvaluationException {
                GuiProfiledPrincipal principal = focusProfileService.getPrincipalByOid(oid, clazz, options);
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

            @Override
            public @NotNull CompiledGuiProfile refreshCompiledProfile(GuiProfiledPrincipal guiProfiledPrincipal) {
                return guiProfiledPrincipal.getCompiledGuiProfile();
            }

            @Override
            public @NotNull CompiledGuiProfile refreshCompiledProfile(GuiProfiledPrincipal guiProfiledPrincipal, ProfileCompilerOptions options) {
                return guiProfiledPrincipal.getCompiledGuiProfile();
            }
        });

        auditFilter = new SequenceAuditFilter(authenticationRecorder);
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
        Authentication authentication = getAuthenticationEvaluator().authenticate(
                connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));
        writeGlobalBehaviour(authentication);

        // THEN
        then();
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        assertGoodPasswordAuthentication(authentication, USER_JACK_USERNAME);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 0);
        assertFailedLoginsForFocusBehavior(userAfter, 0);
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
            writeGlobalBehaviour(e.getMessage(), USER_JACK_USERNAME);

            then();
            displayExpectedException(e);
            assertBadPasswordException(e);
        }
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 1);
        assertFailedLoginsForFocusBehavior(userAfter, 1);
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
            writeGlobalBehaviour(e.getMessage(), USER_JACK_USERNAME);
            then();
            displayExpectedException(e);
            assertEmptyPasswordException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 1);
        assertFailedLoginsForFocusBehavior(userAfter, 2);
        assertUserLockout(userAfter, LockoutStatusType.NORMAL);
    }

    @Test
    public void test103PasswordLoginEmptyPasswordJack() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();

        try {

            // WHEN
            when();

            getAuthenticationEvaluator().authenticate(
                    connEnv, getAuthenticationContext(USER_JACK_USERNAME, get103EmptyPasswordJack()));

            AssertJUnit.fail("Unexpected success");

        } catch (BadCredentialsException e) {
            writeGlobalBehaviour(e.getMessage(), USER_JACK_USERNAME);

            then();
            displayExpectedException(e);
            assertEmptyPasswordException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 1);
        assertFailedLoginsForFocusBehavior(userAfter, 3);
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
            writeGlobalBehaviour(e.getMessage(), null);

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
            writeGlobalBehaviour(e.getMessage(), "");

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
            writeGlobalBehaviour(e.getMessage(), "NoSuchUser");

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
            writeGlobalBehaviour(e.getMessage(), USER_JACK_USERNAME);
            // This is expected

            // THEN
            then();
            displayExpectedException(e);
            assertBadPasswordException(e);
        }
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 1);
        assertFailedLoginsForFocusBehavior(userAfter, 4);
        assertLastFailedLogin(userAfter, startTs, endTs);
        assertUserLockout(userAfter, LockoutStatusType.NORMAL);
        clock.resetOverride();
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
            writeGlobalBehaviour(e.getMessage(), USER_JACK_USERNAME);

            displayExpectedException(e);
            assertBadPasswordException(e);
        }

        PrismObject<UserType> userBetween = getUser(USER_JACK_OID);
        display("user after", userBetween);
        assertFailedLoginsForModuleAttemptBehaviour(userBetween, 2);
        assertFailedLoginsForFocusBehavior(userBetween, 5);
        assertUserLockout(userBetween, LockoutStatusType.NORMAL);

        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (BadCredentialsException e) {
            writeGlobalBehaviour(e.getMessage(), USER_JACK_USERNAME);

            displayExpectedException(e);
            assertBadPasswordException(e);
        }

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        // THEN
        then();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 3);
        assertFailedLoginsForFocusBehavior(userAfter, 6);
        assertLastFailedLogin(userAfter, startTs, endTs);
        assertUserLockout(userAfter, LockoutStatusType.LOCKED);
    }

    @Test
    public void test132PasswordLoginLockedOutGoodPassword() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();

        // WHEN
        when();
        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (LockedException e) {
            writeGlobalBehaviour(e.getMessage(), USER_JACK_USERNAME);

            then();
            displayExpectedException(e);
            assertLockedException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 4);
        assertFailedLoginsForFocusBehavior(userAfter, 7);
        assertUserLockout(userAfter, LockoutStatusType.LOCKED);
    }

    @Test
    public void test133PasswordLoginLockedOutBadPassword() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();

        // WHEN
        when();
        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (LockedException e) {
            writeGlobalBehaviour(e.getMessage(), USER_JACK_USERNAME);

            then();
            displayExpectedException(e);

            // This is important.
            // The exception should give no indication whether the password is good or bad.
            assertLockedException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 5);
        assertFailedLoginsForFocusBehavior(userAfter, 8);
        assertUserLockout(userAfter, LockoutStatusType.LOCKED);
    }

    @Test
    public void test135PasswordLoginLockedOutLockExpires() throws Exception {
        // GIVEN
        clock.overrideDuration("PT30M");

        ConnectionEnvironment connEnv = createConnectionEnvironment();
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        Authentication authentication = getAuthenticationEvaluator().authenticate(
                connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));
        writeGlobalBehaviour(authentication);

        // THEN
        then();
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        assertGoodPasswordAuthentication(authentication, USER_JACK_USERNAME);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 0);
        assertFailedLoginsForFocusBehavior(userAfter, 0);
        assertLastSuccessfulLogin(userAfter, startTs, endTs);
        assertUserLockout(userAfter, LockoutStatusType.NORMAL);
        clock.resetOverride();
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
            writeGlobalBehaviour(e.getMessage(), USER_JACK_USERNAME);

            then();
            displayExpectedException(e);
            assertBadPasswordException(e);
        }

        PrismObject<UserType> userBetween = getUser(USER_JACK_OID);
        display("user after", userBetween);
        assertFailedLoginsForModuleAttemptBehaviour(userBetween, 1);
        assertFailedLoginsForFocusBehavior(userBetween, 1);
        assertUserLockout(userBetween, LockoutStatusType.NORMAL);

        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (BadCredentialsException e) {
            writeGlobalBehaviour(e.getMessage(), USER_JACK_USERNAME);

            then();
            displayExpectedException(e);
            assertBadPasswordException(e);
        }

        userBetween = getUser(USER_JACK_OID);
        display("user after", userBetween);
        assertFailedLoginsForModuleAttemptBehaviour(userBetween, 2);
        assertFailedLoginsForFocusBehavior(userBetween, 2);
        assertUserLockout(userBetween, LockoutStatusType.NORMAL);

        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (BadCredentialsException e) {
            writeGlobalBehaviour(e.getMessage(), USER_JACK_USERNAME);

            then();
            displayExpectedException(e);
            assertBadPasswordException(e);
        }

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 3);
        assertFailedLoginsForFocusBehavior(userAfter, 3);
        assertLastFailedLogin(userAfter, startTs, endTs);
        assertUserLockout(userAfter, LockoutStatusType.LOCKED);
    }

    @Test
    public void test137PasswordLoginLockedOutGoodPasswordAgain() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();

        // WHEN
        when();
        try {

            getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));

            AssertJUnit.fail("Unexpected success");
        } catch (LockedException e) {
            writeGlobalBehaviour(e.getMessage(), USER_JACK_USERNAME);

            then();
            displayExpectedException(e);
            assertLockedException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 4);
        assertFailedLoginsForFocusBehavior(userAfter, 4);
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
        assertFailedLoginsForModuleAttemptBehaviour(userBetween, 0);
        assertFailedLoginsForFocusBehavior(userBetween, 0);
        assertUserLockout(userBetween, LockoutStatusType.NORMAL);

        given("preparing for new login");
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        when("a good password is provided");
        Authentication authentication =
                getAuthenticationEvaluator()
                        .authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));
        writeGlobalBehaviour(authentication);

        then("everything is OK");
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        assertGoodPasswordAuthentication(authentication, USER_JACK_USERNAME);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 0);
        assertFailedLoginsForFocusBehavior(userAfter, 0);
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
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 0);
        assertFailedLoginsForFocusBehavior(userAfter, 0);
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

        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result, SchemaConstants.LIFECYCLE_PROPOSED);

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

        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 0);
        assertFailedLoginsForFocusBehavior(userAfter, 0);
    }

    @Test
    public void test201UserGuybrushPasswordLoginGoodPassword() throws Exception {
        // GIVEN
        ConnectionEnvironment connEnv = createConnectionEnvironment();
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        Authentication authentication = getAuthenticationEvaluator().authenticate(
                connEnv, getAuthenticationContext(USER_GUYBRUSH_USERNAME, getGoodPasswordGuybrush()));
        writeGlobalBehaviour(authentication);

        // THEN
        then();
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        assertGoodPasswordAuthentication(authentication, USER_GUYBRUSH_USERNAME);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 0);
        assertFailedLoginsForFocusBehavior(userAfter, 0);
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

            getAuthenticationEvaluator().authenticate(
                    connEnv, getAuthenticationContext(USER_GUYBRUSH_USERNAME, getBadPasswordGuybrush()));

            AssertJUnit.fail("Unexpected success");

        } catch (BadCredentialsException e) {
            writeGlobalBehaviour(e.getMessage(), USER_GUYBRUSH_USERNAME);

            then();
            displayExpectedException(e);
            assertBadPasswordException(e);
        }
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 1);
        assertFailedLoginsForFocusBehavior(userAfter, 1);
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
        Authentication authentication = getAuthenticationEvaluator().authenticate(
                connEnv, getAuthenticationContext(USER_GUYBRUSH_USERNAME, getGoodPasswordGuybrush()));
        writeGlobalBehaviour(authentication);

        // THEN
        then();
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        assertGoodPasswordAuthentication(authentication, USER_GUYBRUSH_USERNAME);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 0);
        assertFailedLoginsForFocusBehavior(userAfter, 0);
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

            getAuthenticationEvaluator().authenticate(
                    connEnv, getAuthenticationContext(USER_GUYBRUSH_USERNAME, getGoodPasswordGuybrush()));

            AssertJUnit.fail("Unexpected success");

        } catch (CredentialsExpiredException e) {
            writeGlobalBehaviour(e.getMessage(), USER_GUYBRUSH_USERNAME);

            then();
            displayExpectedException(e);
            assertExpiredException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 1);
        assertFailedLoginsForFocusBehavior(userAfter, 1);
    }

    /**
     * Authentication requires assignment to `blue` role. The user has this role directly assigned.
     * The authentication should be therefore successful.
     *
     * MID-8123
     */
    @Test
    public void test300RequiredAssignmentPresentDirectly() {
        clock.resetOverride();

        given("auth context that requires membership in role 'blue' (present directly)");
        ConnectionEnvironment connEnv = createConnectionEnvironment();
        AC context = getAuthenticationContext(USER_PAINTER_NAME, getGoodPasswordJack(), List.of(ROLE_BLUE.ref()));

        when("authentication is attempted");
        Authentication authentication = getAuthenticationEvaluator().authenticate(connEnv, context);
        writeGlobalBehaviour(authentication);

        then("authentication is successful");
        assertGoodPasswordAuthentication(authentication, USER_PAINTER_NAME);
    }

    /**
     * Authentication requires assignment to `yellow` role. The user has this role indirectly assigned.
     * The authentication should be successful even in this case.
     *
     * MID-8123
     */
    @Test
    public void test310RequiredAssignmentPresentIndirectly() {
        clock.resetOverride();

        given("auth context that requires membership in role 'yellow' (present indirectly)");
        ConnectionEnvironment connEnv = createConnectionEnvironment();
        AC context = getAuthenticationContext(USER_PAINTER_NAME, getGoodPasswordJack(), List.of(ROLE_YELLOW.ref()));

        when("authentication is attempted");
        Authentication authentication = getAuthenticationEvaluator().authenticate(connEnv, context);
        writeGlobalBehaviour(authentication);

        then("authentication is successful");
        assertGoodPasswordAuthentication(authentication, USER_PAINTER_NAME);
    }

    /**
     * Authentication requires assignment to `red` role. The user does not have this role. The authentication should fail.
     *
     * MID-8123
     */
    @Test
    public void test320RequiredAssignmentNotPresent() {
        clock.resetOverride();

        given("auth context that requires membership in role 'red' (not present)");
        ConnectionEnvironment connEnv = createConnectionEnvironment();
        AC context = getAuthenticationContext(USER_PAINTER_NAME, getGoodPasswordJack(), List.of(ROLE_RED.ref()));

        when("authentication is attempted");
        try {
            getAuthenticationEvaluator().authenticate(connEnv, context);

            fail("unexpected success");
        } catch (DisabledException e) {
            writeGlobalBehaviour(e.getMessage(), USER_PAINTER_NAME);

            then("an exception is raised");
            displayExpectedException(e);
            assertMissingAssignment(e);
        }
    }

    private void assertGoodPasswordAuthentication(Authentication authentication, String expectedUsername) {
        assertNotNull("No authentication", authentication);
        assertTrue("authentication: not authenticated", authentication.isAuthenticated());
        MidPointAsserts.assertInstanceOf("authentication", authentication, UsernamePasswordAuthenticationToken.class);
        assertEquals("authentication: principal mismatch", expectedUsername, ((MidPointPrincipal) authentication.getPrincipal()).getUsername());
    }

    private void assertBadPasswordException(BadCredentialsException e) {
        assertEquals("Wrong exception message (key)", messages.getMessage("web.security.provider.invalid.credentials"), getTranslatedMessage(e));
    }

    private void assertEmptyPasswordException(BadCredentialsException e) {
        assertEquals("Wrong exception message (key)", messages.getMessage(getEmptyPasswordExceptionMessageKey()), getTranslatedMessage(e));
    }

    private String getTranslatedMessage(Throwable t) {
        return localizationService.translate(t.getMessage(), new Object[0], Locale.getDefault());
    }

    private void assertLockedException(LockedException e) {
        assertEquals("Wrong exception message (key)", messages.getMessage("web.security.provider.locked"), getTranslatedMessage(e));
    }

    private void assertDisabledException(DisabledException e) {
        assertEquals("Wrong exception message (key)", messages.getMessage("web.security.provider.disabled"), getTranslatedMessage(e));
    }

    private void assertExpiredException(CredentialsExpiredException e) {
        assertEquals("Wrong exception message (key)", messages.getMessage("web.security.provider.credential.expired"), getTranslatedMessage(e));
    }

    private void assertNoUserException(UsernameNotFoundException e) {
        assertEquals("Wrong exception message (key)", messages.getMessage("web.security.provider.invalid.credentials"), getTranslatedMessage(e));
    }

    private void assertMissingAssignment(DisabledException e) {
        assertEquals("Wrong exception message (key)", messages.getMessage("web.security.flexAuth.invalid.required.assignment"), getTranslatedMessage(e));
    }

    private ConnectionEnvironment createConnectionEnvironment() {
        HttpConnectionInformation connInfo = new HttpConnectionInformation();
        connInfo.setRemoteHostAddress("remote.example.com");
        ConnectionEnvironment connectionEnvironment = new ConnectionEnvironment(null, connInfo);
        connectionEnvironment.setModuleIdentifier(getModuleIdentifier());
        connectionEnvironment.setSequenceIdentifier(getSequenceIdentifier());
        return connectionEnvironment;
    }

    private void assertFailedLoginsForModuleAttemptBehaviour(PrismObject<UserType> user, int expected) {
        Integer failedModuleAttempts = getAuthenticationForModule(user.asObjectable()).getFailedAttempts();
        if (expected == 0 && failedModuleAttempts == null) {
            return;
        }
        assertEquals("Wrong failed logins in " + user, (Integer) expected, failedModuleAttempts);
    }

    private void assertFailedLoginsForFocusBehavior(PrismObject<UserType> user, int expected) {
        if (expected == 0 && getAuthenticationBehavior(user.asObjectable()).getFailedLogins() == null) {
            return;
        }
        assertEquals("Wrong failed logins in " + user, (Integer) expected, getAuthenticationBehavior(user.asObjectable()).getFailedLogins());
    }

    private void assertLastSuccessfulLogin(PrismObject<UserType> user, XMLGregorianCalendar startTs,
            XMLGregorianCalendar endTs) {
        LoginEventType lastSuccessfulLogin = getAuthenticationForModule(user.asObjectable()).getLastSuccessfulAuthentication();
        assertNotNull("no last successful module login attempt in " + user, lastSuccessfulLogin);
        XMLGregorianCalendar successfulLoginTs = lastSuccessfulLogin.getTimestamp();
        TestUtil.assertBetween("last successful module login attempt timestamp", startTs, endTs, successfulLoginTs);

        LoginEventType lastSuccessfulLoginFromBehavior = getAuthenticationBehavior(user.asObjectable()).getLastSuccessfulLogin();
        assertNotNull("no last successful login in " + user, lastSuccessfulLoginFromBehavior);
        XMLGregorianCalendar successfulLoginTsFromBehavior = lastSuccessfulLoginFromBehavior.getTimestamp();
        TestUtil.assertBetween("last successful login timestamp", startTs, endTs, successfulLoginTsFromBehavior);
    }

    private void assertLastFailedLogin(PrismObject<UserType> user, XMLGregorianCalendar startTs,
            XMLGregorianCalendar endTs) {
        LoginEventType lastFailedLogin = getAuthenticationForModule(user.asObjectable()).getLastFailedAuthentication();
        assertNotNull("no last failed module login attempt in " + user, lastFailedLogin);
        XMLGregorianCalendar failedLoginTs = lastFailedLogin.getTimestamp();
        TestUtil.assertBetween("last failed module login attempt timestamp", startTs, endTs, failedLoginTs);

        LoginEventType lastFailedLoginFromBehavior = getAuthenticationBehavior(user.asObjectable()).getLastFailedLogin();
        assertNotNull("no last failed login in " + user, lastFailedLoginFromBehavior);
        XMLGregorianCalendar failedLoginTsFromBehavior = lastFailedLoginFromBehavior.getTimestamp();
        TestUtil.assertBetween("last failed login timestamp", startTs, endTs, failedLoginTsFromBehavior);
    }

    private void addFakeAuthorization(MidPointPrincipal principal) {
        if (principal == null) {
            return;
        }
        if (principal.getAuthorities().isEmpty()) {
            principal.addAuthorization(
                    new Authorization(
                            new AuthorizationType().action("FAKE")));
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
        Authentication authentication = getAuthenticationEvaluator().authenticate(
                connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));
        writeGlobalBehaviour(authentication);

        // THEN
        then();
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        assertGoodPasswordAuthentication(authentication, USER_JACK_USERNAME);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, 0);
        assertFailedLoginsForFocusBehavior(userAfter, 0);
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
            writeGlobalBehaviour(e.getMessage(), USER_JACK_USERNAME);

            then();
            displayExpectedException(e);

            // This is important.
            // The exception should give no indication whether the password is good or bad.
            assertDisabledException(e);
        }

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("user after", userAfter);
        assertFailedLoginsForModuleAttemptBehaviour(userAfter, expectedFailInBehavior);
        assertFailedLoginsForFocusBehavior(userAfter, expectedFailInBehavior);
    }

    private AuthenticationBehavioralDataType getAuthenticationBehavior(UserType user) {
        return AuthUtil.getBehavioralDataForSequence(user, getSequenceIdentifier());
    }

    private AuthenticationAttemptDataType getAuthenticationForModule(UserType user) {
        return AuthUtil.findOrCreateAuthenticationAttemptDataFoModule(createConnectionEnvironment(), user);
    }

    private void writeGlobalBehaviour(String message, String username) {
        MidpointAuthentication mpAuthentication = createFakeGlobalAuth(AuthenticationModuleState.FAILURE);
        AutheticationFailedData failedData = new AutheticationFailedData(message, username);
        mpAuthentication.getFirstFailedAuthenticationModule().setFailureData(failedData);

        auditFilter.writeRecord(null, mpAuthentication);
    }

    private void writeGlobalBehaviour(Authentication authentication) {
        AuthenticationModuleState state = authentication.isAuthenticated() ? AuthenticationModuleState.SUCCESSFULLY : AuthenticationModuleState.FAILURE;
        MidpointAuthentication mpAuthentication = createFakeGlobalAuth(state);
        mpAuthentication.setPrincipal(authentication.getPrincipal());

        auditFilter.writeRecord(null, mpAuthentication);

    }

    private MidpointAuthentication createFakeGlobalAuth(AuthenticationModuleState status) {
        AuthenticationSequenceModuleType moduleType = new AuthenticationSequenceModuleType()
                .necessity(AuthenticationSequenceModuleNecessityType.REQUIRED)
                .order(10)
                .identifier(getModuleIdentifier());

        AuthenticationSequenceType sequence = new AuthenticationSequenceType()
                .identifier(getSequenceIdentifier())
                .module(moduleType);

        MidpointAuthentication mPAuthentication = new MidpointAuthentication(sequence);

        AuthenticationSequenceChannelType channel = new AuthenticationSequenceChannelType()
                .channelId(SchemaConstants.CHANNEL_USER_URI);
        mPAuthentication.setAuthenticationChannel(new GuiAuthenticationChannel(channel, taskManager, modelInteractionService));

        mPAuthentication.setAuthModules(List.of(new AuthModuleImpl()));

        ModuleAuthenticationImpl module = new ModuleAuthenticationImpl(getModuleIdentifier(), moduleType);
        module.setState(status);
        module.setNameOfModule(getModuleIdentifier());

        mPAuthentication.addAuthentications(module);
        return mPAuthentication;
    }

}
