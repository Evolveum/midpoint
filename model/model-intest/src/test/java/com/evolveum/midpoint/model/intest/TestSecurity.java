/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertFalse;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.FilterInvocation;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.security.Authorization;
import com.evolveum.midpoint.common.security.AuthorizationConstants;
import com.evolveum.midpoint.common.security.AuthorizationEvaluator;
import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.model.security.api.UserDetailsService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurity extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/contract");

	@Autowired(required=true)
	private UserDetailsService userDetailsService;
	
	@Autowired(required=true)
	private AuthorizationEvaluator authorizationEvaluator;
	
	public TestSecurity() throws JAXBException {
		super();
	}
	
	@Test
    public void test010GetUserAdministrator() throws Exception {
		final String TEST_NAME = "test010GetUserAdministrator";
        TestUtil.displayTestTile(this, TEST_NAME);

        // WHEN
        MidPointPrincipal principal = userDetailsService.getUser(USER_ADMINISTRATOR_USERNAME);
        
        // THEN
        display("Administrator principal", principal);
        assertEquals("Wrong number of authorizations", 2, principal.getAuthorities().size());
        assertHasAuthotizationAllow(principal.getAuthorities().iterator().next(), AuthorizationConstants.AUTZ_ALL_URL);

        assertAuthorized(principal, AUTZ_LOOT_URL);
        assertAuthorized(principal, AUTZ_COMMAND_URL);
	}
		
	@Test
    public void test050GetUserJack() throws Exception {
		final String TEST_NAME = "test050GetUserJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // WHEN
        MidPointPrincipal principal = userDetailsService.getUser(USER_JACK_USERNAME);
        
        // THEN
        assertJack(principal);
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());

        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	@Test
    public void test051GetUserBarbossa() throws Exception {
		final String TEST_NAME = "test051GetUserBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);

        // WHEN
        MidPointPrincipal principal = userDetailsService.getUser(USER_BARBOSSA_USERNAME);
        
        // THEN
        display("Principal barbossa", principal);
        assertEquals("wrong username", USER_BARBOSSA_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_BARBOSSA_OID, principal.getOid());
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());
        display("User in principal barbossa", principal.getUser().asPrismObject());
        
        principal.getUser().asPrismObject().checkConsistence(true, true);
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	@Test
    public void test052GetUserGuybrush() throws Exception {
		final String TEST_NAME = "test052GetUserGuybrush";
        TestUtil.displayTestTile(this, TEST_NAME);

        // WHEN
        MidPointPrincipal principal = userDetailsService.getUser(USER_GUYBRUSH_USERNAME);
        
        // THEN
        display("Principal guybrush", principal);
        assertEquals("wrong username", USER_GUYBRUSH_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_GUYBRUSH_OID, principal.getOid());
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());
        display("User in principal guybrush", principal.getUser().asPrismObject());
        
        principal.getUser().asPrismObject().checkConsistence(true, true);
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	
	@Test
    public void test100JackRolePirate() throws Exception {
		final String TEST_NAME = "test100JackRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // WHEN
        MidPointPrincipal principal = userDetailsService.getUser(USER_JACK_USERNAME);
        
        // THEN
        assertJack(principal);
        
        assertEquals("Wrong number of authorizations", 1, principal.getAuthorities().size());
        assertHasAuthotizationAllow(principal.getAuthorities().iterator().next(), AUTZ_LOOT_URL);
        
        assertAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	@Test
    public void test110GuybrushRoleNicePirate() throws Exception {
		final String TEST_NAME = "test110GuybrushRoleNicePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assignRole(USER_GUYBRUSH_OID, ROLE_NICE_PIRATE_OID, task, result);
        
        // WHEN
        MidPointPrincipal principal = userDetailsService.getUser(USER_GUYBRUSH_USERNAME);
        
        // THEN
        display("Principal guybrush", principal);
        assertEquals("Wrong number of authorizations", 2, principal.getAuthorities().size());
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	@Test
    public void test111GuybrushRoleCaptain() throws Exception {
		final String TEST_NAME = "test111GuybrushRoleCaptain";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assignRole(USER_GUYBRUSH_OID, ROLE_CAPTAIN_OID, task, result);
        
        // WHEN
        MidPointPrincipal principal = userDetailsService.getUser(USER_GUYBRUSH_USERNAME);
        
        // THEN
        display("Principal guybrush", principal);
        assertEquals("Wrong number of authorizations", 3, principal.getAuthorities().size());
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	private void assertJack(MidPointPrincipal principal) {
		display("Principal jack", principal);
        assertEquals("wrong username", USER_JACK_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_JACK_OID, principal.getOid());
		assertJack(principal.getUser());		
	}
	
	private void assertJack(UserType userType) {
        display("User in principal jack", userType.asPrismObject());
        assertUserJack(userType.asPrismObject());
        
        userType.asPrismObject().checkConsistence(true, true);		
	}
	
	private void assertHasAuthotizationAllow(Authorization authorization, String... action) {
		assertNotNull("Null authorization", authorization);
		assertEquals("Wrong decision in "+authorization, AuthorizationDecisionType.ALLOW, authorization.getDecision());
		TestUtil.assertSetEquals("Wrong action in "+authorization, authorization.getAction(), action);
	}

	private void assertAuthorized(MidPointPrincipal principal, String action) {
		createSecurityContext(principal);
		assertTrue("AuthorizationEvaluator.isAuthorized: Principal "+principal+" NOT authorized for action "+action, authorizationEvaluator.isAuthorized(action));
		authorizationEvaluator.decide(SecurityContextHolder.getContext().getAuthentication(), createSecureObject(), 
				createConfigAttributes(action));
	}
	
	private void assertNotAuthorized(MidPointPrincipal principal, String action) {
		createSecurityContext(principal);
		assertFalse("AuthorizationEvaluator.isAuthorized: Principal "+principal+" IS authorized for action "+action+" but he should not be", authorizationEvaluator.isAuthorized(action));
	}

	private void createSecurityContext(MidPointPrincipal principal) {
		SecurityContext context = new SecurityContextImpl();
		Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null);
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}
	
	private Object createSecureObject() {
		return new FilterInvocation("/midpoint", "whateverServlet", "doSomething");
	}

	private Collection<ConfigAttribute> createConfigAttributes(String action) {
		Collection<ConfigAttribute> attrs = new ArrayList<ConfigAttribute>();
		attrs.add(new SecurityConfig(action));
		return attrs;
	}
}
