/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_OID;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_USERNAME;

import java.io.File;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.security.MidPointGuiAuthorizationEvaluator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-admin-gui-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestIntegrationSecurity extends AbstractInitializedGuiIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/security");

    private static final File ROLE_UI_ALLOW_ALL_FILE = new File(TEST_DIR, "role-ui-allow-all.xml");
    private static final String ROLE_UI_ALLOW_ALL_OID = "d8f78cfe-d05d-11e7-8ee6-038ce21862f3";

    private static final File ROLE_UI_DENY_ALL_FILE = new File(TEST_DIR, "role-ui-deny-all.xml");
    private static final String ROLE_UI_DENY_ALL_OID = "c4a5923c-d02b-11e7-9ac5-13b0d906fa81";

    private static final File ROLE_UI_DENY_ALLOW_FILE = new File(TEST_DIR, "role-ui-deny-allow.xml");
    private static final String ROLE_UI_DENY_ALLOW_OID = "da47fcf6-d02b-11e7-9e78-f31ae9aa0674";

    private static final TestResource ROLE_AUTHORIZATION_1 = new TestResource(TEST_DIR, "role-authorization-1.xml", "97984277-e809-4a86-ae9b-d5c40e09df0b");
    private static final TestResource ROLE_AUTHORIZATION_2 = new TestResource(TEST_DIR, "role-authorization-2.xml", "96b02d58-5147-4f5a-852c-0f415230ce2c");

    @Autowired private GuiProfiledPrincipalManager focusProfileService;

    private MidPointGuiAuthorizationEvaluator midPointGuiAuthorizationEvaluator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        midPointGuiAuthorizationEvaluator = new MidPointGuiAuthorizationEvaluator(
                securityEnforcer, securityContextManager, taskManager);

        repoAddObjectFromFile(ROLE_UI_ALLOW_ALL_FILE, initResult);
        repoAddObjectFromFile(ROLE_UI_DENY_ALL_FILE, initResult);
        repoAddObjectFromFile(ROLE_UI_DENY_ALLOW_FILE, initResult);
        repoAdd(ROLE_AUTHORIZATION_1, initResult);
        repoAdd(ROLE_AUTHORIZATION_2, initResult);
    }

    // TODO: decide tests with anon user

    @Test
    public void test100DecideNoRole() throws Exception {
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);
        login(USER_JACK_USERNAME);

        Authentication authentication = createPasswordAuthentication(USER_JACK_USERNAME, UserType.class);

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
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_UI_ALLOW_ALL_OID);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);
        login(USER_JACK_USERNAME);

        Authentication authentication = createPasswordAuthentication(USER_JACK_USERNAME, UserType.class);

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
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_UI_DENY_ALL_OID);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);
        login(USER_JACK_USERNAME);

        Authentication authentication = createPasswordAuthentication(USER_JACK_USERNAME, UserType.class);

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
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_UI_DENY_ALLOW_OID);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);
        login(USER_JACK_USERNAME);

        Authentication authentication = createPasswordAuthentication(USER_JACK_USERNAME, UserType.class);

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
        cleanupAutzTest(USER_JACK_OID);
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
            logger.debug("*** Attempt to DECIDE {} (expected allow)", path);

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
            logger.debug("*** Attempt to DECIDE {} (expected deny)", path);

            midPointGuiAuthorizationEvaluator.decide(
                    authentication, createFilterInvocation(path), createAuthConfigAttributes());

            display("DECIDE WRONG failed to deny access to " + path);
            fail("Expected that access to " + path + " is denied, but it was allowed");
        } catch (AccessDeniedException e) {
            // expected
            display("DECIDE OK denied access to " + path);
        }

    }

    private Authentication createPasswordAuthentication(
            String username, Class<? extends FocusType> focusType)
            throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        MidPointPrincipal principal = focusProfileService.getPrincipal(username, focusType);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private FilterInvocation createFilterInvocation(String requestPath) {
        return new FilterInvocation(requestPath, "http");
    }

    private Collection<ConfigAttribute> createAuthConfigAttributes() {
        return createConfigAttributes("fullyAuthenticated");
    }

    private void cleanupAutzTest(String userOid)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        login(userAdministrator);
        unassignAllRoles(userOid);
    }
}
