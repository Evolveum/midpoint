/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication;

import com.evolveum.midpoint.authentication.impl.authorization.evaluator.MidPointGuiAuthorizationEvaluator;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationActionValue;
import com.evolveum.midpoint.authentication.impl.authorization.DescriptorLoaderImpl;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

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

    @Autowired private GuiProfiledPrincipalManager focusProfileService;
    @Autowired private ApplicationContext applicationContext;

    private MidPointGuiAuthorizationEvaluator midPointGuiAuthorizationEvaluator;
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

    private void assertAllow(Authentication authentication, String path) {
        try {
            LOGGER.debug("*** Attempt to DECIDE {} (expected allow)", path);

            midPointGuiAuthorizationEvaluator.decide(
                    authentication, createFilterInvocation(path), createAuthConfigAttributes());

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
                    authentication, createFilterInvocation(path), createAuthConfigAttributes());

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
        return new FilterInvocation(requestPath, "http");
    }

    private Collection<ConfigAttribute> createAuthConfigAttributes() {
        return createConfigAttributes("fullyAuthenticated");
    }

    private void cleanupAutzTest()
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        login(userAdministrator);
        unassignAllRoles(TestIntegrationSecurity.USER_JACK_OID);
    }
}
