/**
 * Copyright (c) 2016-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_OID;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_USERNAME;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.security.MidPointGuiAuthorizationEvaluator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-admin-gui-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestIntegrationSecurity extends AbstractInitializedGuiIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/security");

	protected static final File ROLE_UI_ALLOW_ALL_FILE = new File(TEST_DIR, "role-ui-allow-all.xml");
	protected static final String ROLE_UI_ALLOW_ALL_OID = "d8f78cfe-d05d-11e7-8ee6-038ce21862f3";
	
	protected static final File ROLE_UI_DENY_ALL_FILE = new File(TEST_DIR, "role-ui-deny-all.xml");
	protected static final String ROLE_UI_DENY_ALL_OID = "c4a5923c-d02b-11e7-9ac5-13b0d906fa81";

	protected static final File ROLE_UI_DENY_ALLOW_FILE = new File(TEST_DIR, "role-ui-deny-allow.xml");
	protected static final String ROLE_UI_DENY_ALLOW_OID = "da47fcf6-d02b-11e7-9e78-f31ae9aa0674";
	
	@Autowired private UserProfileService userProfileService;
	
	private MidPointGuiAuthorizationEvaluator midPointGuiAuthorizationEvaluator;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		midPointGuiAuthorizationEvaluator = new MidPointGuiAuthorizationEvaluator(securityEnforcer, securityContextManager, taskManager);

		repoAddObjectFromFile(ROLE_UI_ALLOW_ALL_FILE, initResult);
		repoAddObjectFromFile(ROLE_UI_DENY_ALL_FILE, initResult);
		repoAddObjectFromFile(ROLE_UI_DENY_ALLOW_FILE, initResult);
		
		// Temporary ... until initial object load is fixed
//		ch.qos.logback.classic.Logger l = (ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger("com.evolveum.midpoint.web");
//		l.setLevel(ch.qos.logback.classic.Level.TRACE);
	}

	
	@Test
    public void test100DecideNoRole() throws Exception {
		final String TEST_NAME = "test100DecideNoRole";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);
        login(USER_JACK_USERNAME);

        Authentication authentication = createPasswordAuthentication(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertDeny(authentication, "/admin/users");
        assertDeny(authentication, "/self/dashboard");
        assertDeny(authentication, "/admin/config/system");
        assertDeny(authentication, "/admin/config/debugs");

		// THEN
		displayThen(TEST_NAME);
	}
	
	@Test
    public void test110DecideRoleUiAllowAll() throws Exception {
		final String TEST_NAME = "test110DecideRoleUiAllowAll";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_UI_ALLOW_ALL_OID);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);
        login(USER_JACK_USERNAME);

        Authentication authentication = createPasswordAuthentication(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertAllow(authentication, "/admin/users");
        assertAllow(authentication, "/self/dashboard");
        assertAllow(authentication, "/admin/config/system");
        assertAllow(authentication, "/admin/config/debugs");
	     
		// THEN
		displayThen(TEST_NAME);
	}
	
	@Test
    public void test120DecideRoleUiDenyAll() throws Exception {
		final String TEST_NAME = "test120DecideRoleUiDenyAll";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_UI_DENY_ALL_OID);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);
        login(USER_JACK_USERNAME);

        Authentication authentication = createPasswordAuthentication(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertDeny(authentication, "/admin/users");
        assertDeny(authentication, "/self/dashboard");
        assertDeny(authentication, "/admin/config/system");
        assertDeny(authentication, "/admin/config/debugs");

		// THEN
		displayThen(TEST_NAME);

	}

	/**
	 * MID-4129
	 */
	@Test
    public void test200DecideRoleUiDenyAllow() throws Exception {
		final String TEST_NAME = "test200DecideRoleUiDenyAllow";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_UI_DENY_ALLOW_OID);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);
        login(USER_JACK_USERNAME);

        Authentication authentication = createPasswordAuthentication(USER_JACK_USERNAME);

        // WHEN
        displayWhen(TEST_NAME);

        assertAllow(authentication, "/self/dashboard");
        assertAllow(authentication, "/admin/users");
        assertDeny(authentication, "/admin/config/system");
        assertDeny(authentication, "/admin/config/debugs");

		// THEN
		displayThen(TEST_NAME);

	}
	
	// TODO: permitAll page
	// TODO: access to unauthorized page
	// TODO: access to page without annotations
	
	private void assertAllow(Authentication authentication, String path) {
		try {
			midPointGuiAuthorizationEvaluator.decide(authentication, createFilterInvocation(path), createAuthConfigAttributes());
        } catch (AccessDeniedException e) {
        	throw new AssertionError("Expected that access to "+path+" is allowed, but it was denied", e);
        }			
	}

	private void assertDeny(Authentication authentication, String path) {
		try {
	        midPointGuiAuthorizationEvaluator.decide(authentication, createFilterInvocation(path), createAuthConfigAttributes());
	     
	        fail("Expected that access to "+path+" is denied, but it was allowed");
        } catch (AccessDeniedException e) {
        	// expected
        }
		
	}
	
	private Authentication createPasswordAuthentication(String username) throws ObjectNotFoundException, SchemaException {
		MidPointPrincipal principal = userProfileService.getPrincipal(username);
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
		return auth;
	}
	
	private FilterInvocation createFilterInvocation(String requestPath) {
		return new FilterInvocation(requestPath, "http");
	}
	
	private Collection<ConfigAttribute> createAuthConfigAttributes() {
		return createConfigAttributes("fullyAuthenticated");	
	}
	
	private Collection<ConfigAttribute> createConfigAttributes(String... actions) {
		Collection<ConfigAttribute> configAttributes = new ArrayList<>();
		for (String action: actions) {
			configAttributes.add(new ConfigAttribute() {
				private static final long serialVersionUID = 1L;

				@Override
				public String getAttribute() {
					return action;
				}

				@Override
				public String toString() {
					return action;
				}
			});
		}
		return null;
	}

	private void cleanupAutzTest(String userOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, IOException {
		login(userAdministrator);
        unassignAllRoles(userOid);
	}
}
