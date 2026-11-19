/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.MidpointSessionRegistry;
import com.evolveum.midpoint.authentication.impl.authorization.evaluator.MidPointGuiAuthorizationEvaluator;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationActionValue;
import com.evolveum.midpoint.authentication.impl.authorization.DescriptorLoaderImpl;
import com.evolveum.midpoint.authentication.impl.channel.AuthenticationChannelImpl;
import com.evolveum.midpoint.authentication.impl.channel.InvitationAuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.channel.RestAuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.channel.SelfRegistrationAuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.filter.FinishAuthenticationFilter;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfileCompilerRegistry;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.ProfileCompilerOptions;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.web.FilterInvocation;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author semancik
 */
@ContextConfiguration(locations = "classpath:ctx-authentication-test-main.xml")
@DirtiesContext
public class TestIntegrationSecurity extends AbstractModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestIntegrationSecurity.class);

    private static final File ROLE_UI_ALLOW_ALL_FILE = new File(COMMON_DIR, "role-ui-allow-all.xml");
    private static final String ROLE_UI_ALLOW_ALL_OID = "d8f78cfe-d05d-11e7-8ee6-038ce21862f3";

    private static final File ROLE_UI_DENY_ALL_FILE = new File(COMMON_DIR, "role-ui-deny-all.xml");
    private static final String ROLE_UI_DENY_ALL_OID = "c4a5923c-d02b-11e7-9ac5-13b0d906fa81";

    private static final File ROLE_UI_DENY_ALLOW_FILE = new File(COMMON_DIR, "role-ui-deny-allow.xml");
    private static final String ROLE_UI_DENY_ALLOW_OID = "da47fcf6-d02b-11e7-9e78-f31ae9aa0674";

    private static final TestObject<RoleType> ROLE_AUTHORIZATION_1 = TestObject.file(COMMON_DIR, "role-authorization-1.xml", "97984277-e809-4a86-ae9b-d5c40e09df0b");
    private static final TestObject<RoleType> ROLE_AUTHORIZATION_2 = TestObject.file(COMMON_DIR, "role-authorization-2.xml", "96b02d58-5147-4f5a-852c-0f415230ce2c");

    protected static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
    public static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");

    private static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
    private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
    private static final String USER_JACK_USERNAME = "jack";

    private static final String ROLE_PROFILE_REFRESH_OID = "127e6738-93ef-4e34-85ed-13e6151ea719";
    private static final String USER_PROFILE_REFRESH_OID = "7d882d21-f0bd-49ca-bf21-c35346314f59";
    private static final String USER_PROFILE_REFRESH_NAME = "profile-refresh-user";

    @Autowired private GuiProfiledPrincipalManager focusProfileService;
    @Autowired private GuiProfileCompilerRegistry guiProfileCompilerRegistry;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private MidpointSessionRegistry sessionRegistry;

    private MidPointGuiAuthorizationEvaluator midPointGuiAuthorizationEvaluator;
    private FinishAuthenticationFilter finishAuthenticationFilter;
    protected PrismObject<UserType> userAdministrator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        LOGGER.trace("initSystem");
        super.initSystem(initTask, initResult);

        modelService.postInit(initResult);
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, true, initResult);
        userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, true, initResult);
        login(userAdministrator);

        repoAddObjectFromFile(USER_JACK_FILE, true, initResult);

        midPointGuiAuthorizationEvaluator = new MidPointGuiAuthorizationEvaluator(
                securityEnforcer, securityContextManager, taskManager, applicationContext);
        initializeDescriptorLoader();

        repoAddObjectFromFile(ROLE_UI_ALLOW_ALL_FILE, initResult);
        repoAddObjectFromFile(ROLE_UI_DENY_ALL_FILE, initResult);
        repoAddObjectFromFile(ROLE_UI_DENY_ALLOW_FILE, initResult);
        repoAdd(ROLE_AUTHORIZATION_1, initResult);
        repoAdd(ROLE_AUTHORIZATION_2, initResult);

        repoAddObject(new RoleType()
                .oid(ROLE_PROFILE_REFRESH_OID)
                .name("profile-refresh-role")
                .asPrismObject(), initResult);
        repoAddObject(new UserType()
                .oid(USER_PROFILE_REFRESH_OID)
                .name(USER_PROFILE_REFRESH_NAME)
                .assignment(new AssignmentType()
                        .targetRef(ROLE_PROFILE_REFRESH_OID, RoleType.COMPLEX_TYPE))
                .asPrismObject(), initResult);

        finishAuthenticationFilter = new FinishAuthenticationFilter();
        finishAuthenticationFilter.setPrincipalManager(focusProfileService);
    }

    private void initializeDescriptorLoader() {
        DescriptorLoaderImpl.getLoginPages().add("/login");
        DescriptorLoaderImpl.getMapForAuthPages().put(
                AuthenticationModuleNameConstants.LOGIN_FORM, Collections.singletonList("/login"));
        DescriptorLoaderImpl.getPermitAllUrls().add("/login");

        List<AuthorizationActionValue> listActions = new ArrayList<>();

        listActions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                "PageAdminUsers.auth.usersAll.label", "PageAdminUsers.auth.usersAll.description"));
        listActions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_UI_USERS_URL,
                "PageUsers.auth.users.label", "PageUsers.auth.users.description"));
        listActions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_UI_USERS_VIEW_URL,
                "PageUsers.auth.users.view.label", "PageUsers.auth.users.view.description"));
        listActions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_LABEL, AuthorizationConstants.AUTZ_GUI_ALL_DESCRIPTION));
        DescriptorLoaderImpl.getActions().put("/admin/users", listActions.toArray(new AuthorizationActionValue[0]));

        listActions.clear();
        listActions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_UI_SELF_DASHBOARD_URL,
                "PageSelfDashboard.auth.dashboard.label", "PageSelfDashboard.auth.dashboard.description"));
        listActions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_UI_SELF_ALL_URL,
                "PageSelf.auth.selfAll.label", "PageSelf.auth.selfAll.description"));
        listActions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_LABEL, AuthorizationConstants.AUTZ_GUI_ALL_DESCRIPTION));
        DescriptorLoaderImpl.getActions().put("/self/dashboard", listActions.toArray(new AuthorizationActionValue[0]));

        listActions.clear();
        listActions.add(new AuthorizationActionValue(AuthConstants.AUTH_CONFIGURATION_ALL,
                AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION));
        listActions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
                "PageSystemConfiguration.auth.configSystemConfiguration.label",
                "PageSystemConfiguration.auth.configSystemConfiguration.description"));
        listActions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_LABEL, AuthorizationConstants.AUTZ_GUI_ALL_DESCRIPTION));
        DescriptorLoaderImpl.getActions().put("/admin/config/system", listActions.toArray(new AuthorizationActionValue[0]));

        listActions.clear();
        listActions.add(new AuthorizationActionValue(AuthConstants.AUTH_CONFIGURATION_ALL,
                AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION));
        listActions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_UI_CONFIGURATION_DEBUGS_URL,
                "PageDebugList.auth.debugs.label", "PageDebugList.auth.debugs.description"));
        listActions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_LABEL, AuthorizationConstants.AUTZ_GUI_ALL_DESCRIPTION));
        DescriptorLoaderImpl.getActions().put("/admin/config/debugs", listActions.toArray(new AuthorizationActionValue[0]));

        listActions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_UI_USERS_VIEW_URL,
                "", ""));
    }

    @Test
    public void test100DecideNoRole() throws Exception {
        // GIVEN
        cleanupAutzTest();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);
        login(USER_JACK_USERNAME);

        Authentication authentication = createPasswordAuthentication();

        // WHEN
        when();

        assertAllow(authentication, "/login");
        assertAllow(authentication, "/");
        assertDeny(authentication, "/noautz");
        assertDeny(authentication, "/admin/users");
        assertDeny(authentication, "/self/dashboard");
        assertDeny(authentication, "/admin/config/system");
        assertDeny(authentication, "/admin/config/debugs");

        // THEN
        then();
    }

    @Test
    public void test110DecideRoleUiAllowAll() throws Exception {
        // GIVEN
        cleanupAutzTest();
        assignRole(USER_JACK_OID, ROLE_UI_ALLOW_ALL_OID);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);
        login(USER_JACK_USERNAME);

        Authentication authentication = createPasswordAuthentication();

        // WHEN
        when();

        assertAllow(authentication, "/login");
        assertAllow(authentication, "/");
        assertDeny(authentication, "/noautz");
        assertAllow(authentication, "/admin/users");
        assertAllow(authentication, "/self/dashboard");
        assertAllow(authentication, "/admin/config/system");
        assertAllow(authentication, "/admin/config/debugs");

        // THEN
        then();
    }

    @Test
    public void test120DecideRoleUiDenyAll() throws Exception {
        // GIVEN
        cleanupAutzTest();
        assignRole(USER_JACK_OID, ROLE_UI_DENY_ALL_OID);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);
        login(USER_JACK_USERNAME);

        Authentication authentication = createPasswordAuthentication();

        // WHEN
        when();

        assertAllow(authentication, "/login");
        assertAllow(authentication, "/");
        assertDeny(authentication, "/noautz");
        assertDeny(authentication, "/admin/users");
        assertDeny(authentication, "/self/dashboard");
        assertDeny(authentication, "/admin/config/system");
        assertDeny(authentication, "/admin/config/debugs");

        // THEN
        then();

    }

    /**
     * MID-4129
     */
    @Test
    public void test200DecideRoleUiDenyAllow() throws Exception {
        // GIVEN
        cleanupAutzTest();
        assignRole(USER_JACK_OID, ROLE_UI_DENY_ALLOW_OID);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);
        login(USER_JACK_USERNAME);

        Authentication authentication = createPasswordAuthentication();

        // WHEN
        when();

        assertAllow(authentication, "/login");
        assertAllow(authentication, "/");
        assertDeny(authentication, "/noautz");
        assertAllow(authentication, "/self/dashboard");
        assertAllow(authentication, "/admin/users");
        assertDeny(authentication, "/admin/config/system");
        assertDeny(authentication, "/admin/config/debugs");

        // THEN
        then();

    }

    /**
     * MID-5002
     */
    @Test
    public void test300ConflictingAuthorizationIds() throws Exception {
        // GIVEN
        cleanupAutzTest();
        assignRole(USER_JACK_OID, ROLE_AUTHORIZATION_1.oid);
        assignRole(USER_JACK_OID, ROLE_AUTHORIZATION_2.oid);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);

        // WHEN
        when("Jack logs in");
        login(USER_JACK_USERNAME);

        // THEN
        then();
        assertLoggedInUsername(USER_JACK_USERNAME);
    }

    @Test
    public void test400RemovedAuthorizationIsEnforcedOnNextRequest() throws Exception {
        given("an authenticated GUI session with users-page authorization");
        OperationResult result = getTestOperationResult();
        setProfileRefreshRoleAuthorizations(result, usersAuthorization());
        GuiProfiledPrincipal principal = getProfileRefreshPrincipal();
        MidpointAuthentication authentication = createAuthentication(principal, true);
        assertAllow(authentication, "/admin/users");

        when("the authorization is removed and the next request arrives");
        setProfileRefreshRoleAuthorizations(result);
        principal.getCompiledGuiProfile().markInvalid();
        executeRequest(authentication, "/admin/users", () -> assertDeny(authentication, "/admin/users"));

        then("the existing session uses the refreshed authorization set");
        assertTokenAuthoritiesSynchronized(authentication, principal);
        assertThat(principal.getCompiledGuiProfile().isInvalid()).isFalse();
    }

    @Test
    public void test410AddedAuthorizationIsEnforcedOnNextRequest() throws Exception {
        given("an authenticated GUI session without users-page authorization");
        OperationResult result = getTestOperationResult();
        setProfileRefreshRoleAuthorizations(result);
        GuiProfiledPrincipal principal = getProfileRefreshPrincipal();
        MidpointAuthentication authentication = createAuthentication(principal, true);
        assertDeny(authentication, "/admin/users");

        when("the authorization is added and the next request arrives");
        setProfileRefreshRoleAuthorizations(result, usersAuthorization());
        principal.getCompiledGuiProfile().markInvalid();
        executeRequest(authentication, "/admin/users", () -> assertAllow(authentication, "/admin/users"));

        then("the existing session uses the refreshed authorization set");
        assertTokenAuthoritiesSynchronized(authentication, principal);
        assertThat(principal.getCompiledGuiProfile().isInvalid()).isFalse();
    }

    @Test
    public void test420ValidProfileIsNotRecompiledAndTokenIsSynchronized() throws Exception {
        given("an authenticated GUI session with a valid profile and stale token authorities");
        setProfileRefreshRoleAuthorizations(getTestOperationResult(), usersAuthorization());
        GuiProfiledPrincipal principal = getProfileRefreshPrincipal();
        CompiledGuiProfile profile = principal.getCompiledGuiProfile();
        MidpointAuthentication authentication = createAuthentication(principal, true);
        authentication.setAuthorities(List.of());

        when("the next request arrives");
        executeRequest(authentication, "/admin/users", () -> assertAllow(authentication, "/admin/users"));

        then("the profile is reused and the token authorities are repaired");
        assertThat(principal.getCompiledGuiProfile()).isSameAs(profile);
        assertTokenAuthoritiesSynchronized(authentication, principal);
    }

    @Test
    public void test430ModelInteractionRefreshSynchronizesTokenAuthorities() throws Exception {
        given("an invalid profile refreshed through the model interaction service");
        setProfileRefreshRoleAuthorizations(getTestOperationResult(), usersAuthorization());
        GuiProfiledPrincipal principal = getProfileRefreshPrincipal();
        principal.getCompiledGuiProfile().markInvalid();
        MidpointAuthentication authentication = createAuthentication(principal, true);
        authentication.setAuthorities(List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            when("the compiled GUI profile is requested");
            Task task = getTestTask();
            CompiledGuiProfile profile = modelInteractionService.getCompiledGuiProfile(task, task.getResult());

            then("the profile and authentication token are refreshed consistently");
            assertThat(profile.isInvalid()).isFalse();
            assertTokenAuthoritiesSynchronized(authentication, principal);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void test440FailedInvalidProfileRefreshStopsRequest() throws Exception {
        given("a revoked authorization and an invalid profile whose recompilation fails");
        OperationResult result = getTestOperationResult();
        setProfileRefreshRoleAuthorizations(result, usersAuthorization());
        GuiProfiledPrincipal principal = getProfileRefreshPrincipal();
        MidpointAuthentication authentication = createAuthentication(principal, true);
        assertAllow(authentication, "/admin/users");
        setProfileRefreshRoleAuthorizations(result);
        principal.getCompiledGuiProfile().markInvalid();
        AtomicBoolean failCompilation = new AtomicBoolean(true);
        guiProfileCompilerRegistry.registerCompiler(profile -> {
            if (failCompilation.get()) {
                throw new IllegalStateException("Expected profile compilation failure");
            }
        });
        AtomicBoolean authorizationInvoked = new AtomicBoolean();

        try {
            when("the next request attempts to refresh the profile");
            SecurityContextHolder.getContext().setAuthentication(authentication);
            assertThatThrownBy(() -> finishAuthenticationFilter.doFilter(
                    request("/admin/users"),
                    new MockHttpServletResponse(),
                    (request, response) -> authorizationInvoked.set(true)))
                    .isInstanceOf(SystemException.class);

            then("the request does not reach authorization enforcement and remains refreshable");
            assertThat(authorizationInvoked.get()).isFalse();
            assertThat(principal.getCompiledGuiProfile().isInvalid()).isTrue();

            and("the lazy model-interaction refresh fails closed as well");
            Task task = getTestTask();
            assertThatThrownBy(() -> modelInteractionService.getCompiledGuiProfile(task, task.getResult()))
                    .isInstanceOf(SystemException.class);
            assertThat(principal.getCompiledGuiProfile().isInvalid()).isTrue();

            and("a subsequent request can retry the refresh");
            failCompilation.set(false);
            executeRequest(authentication, "/admin/users", () -> assertDeny(authentication, "/admin/users"));
            assertThat(principal.getCompiledGuiProfile().isInvalid()).isFalse();
            assertTokenAuthoritiesSynchronized(authentication, principal);
        } finally {
            failCompilation.set(false);
            SecurityContextHolder.clearContext();
        }
    }

    private void assertAllow(Authentication authentication, String path) {
        try {
            LOGGER.debug("*** Attempt to DECIDE {} (expected allow)", path);

            midPointGuiAuthorizationEvaluator.decide(
                    authentication, createFilterInvocation(path), createConfigAttributes("fullyAuthenticated"));

            display("DECIDE OK allowed access to " + path);
        } catch (AccessDeniedException e) {
            display("DECIDE WRONG failed to allowed access to " + path);
            throw new AssertionError("Expected that access to " + path + " is allowed, but it was denied", e);
        }
    }

    private void assertDeny(Authentication authentication, String path) {
        try {
            LOGGER.debug("*** Attempt to DECIDE {} (expected deny)", path);

            midPointGuiAuthorizationEvaluator.decide(
                    authentication, createFilterInvocation(path), createConfigAttributes("fullyAuthenticated"));

            display("DECIDE WRONG failed to deny access to " + path);
            fail("Expected that access to " + path + " is denied, but it was allowed");
        } catch (AccessDeniedException e) {
            // expected
            display("DECIDE OK denied access to " + path);
        }

    }

    private Authentication createPasswordAuthentication()
            throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        MidPointPrincipal principal = focusProfileService.getPrincipal(TestIntegrationSecurity.USER_JACK_USERNAME, UserType.class);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private FilterInvocation createFilterInvocation(String requestPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestPath);
        request.setServletPath(requestPath);
        return new FilterInvocation(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    private GuiProfiledPrincipal getProfileRefreshPrincipal() throws CommonException {
        return focusProfileService.getPrincipal(
                USER_PROFILE_REFRESH_NAME, UserType.class, ProfileCompilerOptions.create());
    }

    private AuthorizationType usersAuthorization() {
        return new AuthorizationType()
                .name("users-page")
                .action(AuthorizationConstants.AUTZ_UI_USERS_URL);
    }

    private void setProfileRefreshRoleAuthorizations(OperationResult result, AuthorizationType... authorizations)
            throws CommonException {
        repositoryService.modifyObject(
                RoleType.class,
                ROLE_PROFILE_REFRESH_OID,
                prismContext.deltaFor(RoleType.class)
                        .item(RoleType.F_AUTHORIZATION)
                        .replace(authorizations)
                        .asItemDeltas(),
                result);
    }

    private MidpointAuthentication createAuthentication(GuiProfiledPrincipal principal, boolean gui) {
        AuthenticationSequenceChannelType channel = new AuthenticationSequenceChannelType()
                .channelId(gui ? SchemaConstants.CHANNEL_USER_URI : SchemaConstants.CHANNEL_REST_URI);
        return createAuthentication(
                principal,
                channel,
                gui ? new AuthenticationChannelImpl(channel) : new RestAuthenticationChannel(channel));
    }

    private MidpointAuthentication createAuthentication(
            GuiProfiledPrincipal principal,
            AuthenticationSequenceChannelType channelBean,
            AuthenticationChannel channel) {
        MidpointAuthentication authentication = (MidpointAuthentication) createMpAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        authentication.getSequence().setChannel(channelBean);
        authentication.setAuthorities(principal.getAuthorities());
        authentication.setAlreadyCompiledGui(true);
        authentication.setAuthenticationChannel(channel);
        return authentication;
    }

    private void executeRequest(MidpointAuthentication authentication, String path, Runnable authorizationDecision)
            throws Exception {
        AtomicBoolean filterChainInvoked = new AtomicBoolean();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            finishAuthenticationFilter.doFilter(
                    request(path),
                    new MockHttpServletResponse(),
                    (request, response) -> {
                        filterChainInvoked.set(true);
                        authorizationDecision.run();
                    });
            assertThat(filterChainInvoked.get()).isTrue();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

    private void assertTokenAuthoritiesSynchronized(MidpointAuthentication authentication, GuiProfiledPrincipal principal) {
        assertThat(authentication.getAuthorities()).containsExactlyElementsOf(principal.getAuthorities());
    }

    private void cleanupAutzTest()
            throws CommonException {
        login(userAdministrator);
        unassignAllRoles(TestIntegrationSecurity.USER_JACK_OID);
    }
}
