/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.impl;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoginEventType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestAuthenticationEvaluator extends AbstractInternalModelIntegrationTest {
	
	protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "security");
	
	private static final Trace LOGGER = TraceManager.getTrace(TestAuthenticationEvaluator.class);
	
	@Autowired(required=true)
	private AuthenticationEvaluator authenticationEvaluator;
	
	@Autowired(required=true)
	private UserProfileService userProfileService;
	
	@Autowired(required = true)
	private Clock clock;

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem(com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}
	
	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTile(TEST_NAME);
		
		assertNotNull(authenticationEvaluator);
		assertNotNull(authenticationEvaluator.getUserProfileService());
	}
	
	@Test
	public void test020UserProfileServiceUsername() throws Exception {
		final String TEST_NAME = "test020UserProfileServiceUsername";
		TestUtil.displayTestTile(TEST_NAME);
		
		MidPointPrincipal principal = userProfileService.getPrincipal(USER_JACK_USERNAME);
		assertPrincipalJack(principal);
	}

	
	@Test
	public void test100PasswordLoginGoodPasswordJack() throws Exception {
		final String TEST_NAME = "test100PasswordLoginGoodPasswordJack";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		MidPointPrincipal principal = getAuthorizedPrincipal(USER_JACK_USERNAME);
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		Authentication authentication = authenticationEvaluator.authenticateUserPassword(principal, connEnv, USER_JACK_PASSWORD);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		assertGoodPasswordAuthentication(authentication, principal);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 0);
		assertLastSuccessfulLogin(userAfter, startTs, endTs);
	}
	
	@Test
	public void test101PasswordLoginBadPasswordJack() throws Exception {
		final String TEST_NAME = "test101PasswordLoginBadPasswordJack";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		MidPointPrincipal principal = getAuthorizedPrincipal(USER_JACK_USERNAME);
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		try {
		
			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			
			authenticationEvaluator.authenticateUserPassword(principal, connEnv, "thisIsNotMyPassword");
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertBadPasswordException(e, principal);
		}
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 1);
		assertLastFailedLogin(userAfter, startTs, endTs);
	}
	
	@Test
	public void test102PasswordLoginNullPasswordJack() throws Exception {
		final String TEST_NAME = "test102PasswordLoginNullPasswordJack";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		MidPointPrincipal principal = getAuthorizedPrincipal(USER_JACK_USERNAME);
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		
		try {
		
			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			
			authenticationEvaluator.authenticateUserPassword(principal, connEnv, null);
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertDeniedException(e, principal);
		}
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 1);
	}


	@Test
	public void test103PasswordLoginEmptyPasswordJack() throws Exception {
		final String TEST_NAME = "test103PasswordLoginEmptyPasswordJack";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		MidPointPrincipal principal = getAuthorizedPrincipal(USER_JACK_USERNAME);
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		
		try {
		
			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			
			authenticationEvaluator.authenticateUserPassword(principal, connEnv, "");
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertDeniedException(e, principal);
		}
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 1);
	}
	
	/**
	 * Wait for 5 minutes. The failed login count should reset after 3 minutes. Therefore bad login
	 * count should be one after we try to make a bad login.
	 */
	@Test
	public void test105PasswordLoginBadPasswordJackAfterLockoutFailedAttemptsDuration() throws Exception {
		final String TEST_NAME = "test105PasswordLoginBadPasswordJackAfterLockoutFailedAttemptsDuration";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		clock.overrideDuration("PT5M");
		
		MidPointPrincipal principal = getAuthorizedPrincipal(USER_JACK_USERNAME);
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		try {
		
			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			
			authenticationEvaluator.authenticateUserPassword(principal, connEnv, "thisIsNotMyPassword");
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertBadPasswordException(e, principal);
		}
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 1);
		assertLastFailedLogin(userAfter, startTs, endTs);
	}


	@Test
	public void test110PasswordLoginLockout() throws Exception {
		final String TEST_NAME = "test110PasswordLoginLockout";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		MidPointPrincipal principal = getAuthorizedPrincipal(USER_JACK_USERNAME);
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		try {
			
			authenticationEvaluator.authenticateUserPassword(principal, connEnv, "not my password either");
			
			AssertJUnit.fail("Unexpected success");
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertBadPasswordException(e, principal);
		}
		
		PrismObject<UserType> userBetween = getUser(USER_JACK_OID);
		display("user after", userBetween);
		assertFailedLogins(userBetween, 2);
		
		try {
			
			authenticationEvaluator.authenticateUserPassword(principal, connEnv, "absoLUTELY NOT my PASSword");
			
			AssertJUnit.fail("Unexpected success");
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertBadPasswordException(e, principal);
		}
		
		
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 3);
		assertLastFailedLogin(userAfter, startTs, endTs);
	}
	
	@Test
	public void test112PasswordLoginLockedoutGoodPassword() throws Exception {
		final String TEST_NAME = "test112PasswordLoginLockedoutGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		MidPointPrincipal principal = getAuthorizedPrincipal(USER_JACK_USERNAME);
		ConnectionEnvironment connEnv = createConnectionEnvironment();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		try {
			
			authenticationEvaluator.authenticateUserPassword(principal, connEnv, USER_JACK_PASSWORD);
			
			AssertJUnit.fail("Unexpected success");
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertLockedException(e, principal);
		}
				
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 3);
	}

	@Test
	public void test113PasswordLoginLockedoutBadPassword() throws Exception {
		final String TEST_NAME = "test113PasswordLoginLockedoutBadPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		MidPointPrincipal principal = getAuthorizedPrincipal(USER_JACK_USERNAME);
		ConnectionEnvironment connEnv = createConnectionEnvironment();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		try {
			
			authenticationEvaluator.authenticateUserPassword(principal, connEnv, "bad bad password!");
			
			AssertJUnit.fail("Unexpected success");
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			
			// this is important. The exception should give no indication whether the password is
			// good or bad
			assertLockedException(e, principal);
		}
				
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 3);
	}


	@Test
	public void test118PasswordLoginLockedoutLockExpires() throws Exception {
		final String TEST_NAME = "test118PasswordLoginLockedoutLockExpires";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		clock.overrideDuration("PT30M");
		
		MidPointPrincipal principal = getAuthorizedPrincipal(USER_JACK_USERNAME);
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		Authentication authentication = authenticationEvaluator.authenticateUserPassword(principal, connEnv, USER_JACK_PASSWORD);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		assertGoodPasswordAuthentication(authentication, principal);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 0);
		assertLastSuccessfulLogin(userAfter, startTs, endTs);
	}

	
	private void assertGoodPasswordAuthentication(Authentication authentication, MidPointPrincipal principal) {
		assertNotNull("No authentication", authentication);
		assertTrue("authentication: not authenticated", authentication.isAuthenticated());
		MidPointAsserts.assertInstanceOf("authentication", authentication, UsernamePasswordAuthenticationToken.class);
		assertEquals("authentication: principal mismatch", principal, authentication.getPrincipal());
	}

	private void assertBadPasswordException(BadCredentialsException e, MidPointPrincipal principal) {
		assertEquals("Wrong exception meessage (key)", "web.security.provider.invalid", e.getMessage());
	}
	
	private void assertDeniedException(BadCredentialsException e, MidPointPrincipal principal) {
		assertEquals("Wrong exception meessage (key)", "web.security.provider.access.denied", e.getMessage());
	}
	
	private void assertLockedException(BadCredentialsException e, MidPointPrincipal principal) {
		assertEquals("Wrong exception meessage (key)", "web.security.provider.locked", e.getMessage());
	}
	
	private ConnectionEnvironment createConnectionEnvironment() {
		ConnectionEnvironment connEnv = new ConnectionEnvironment();
		connEnv.setRemoteHost("remote.example.com");
		return connEnv;
	}
	
	private void assertFailedLogins(PrismObject<UserType> user, int expected) {
		if (expected == 0 && user.asObjectable().getCredentials().getPassword().getFailedLogins() == null) {
			return;
		}
		assertEquals("Wrong failed logins in "+user, (Integer)expected, user.asObjectable().getCredentials().getPassword().getFailedLogins());
	}

	private void assertLastSuccessfulLogin(PrismObject<UserType> user, XMLGregorianCalendar startTs,
			XMLGregorianCalendar endTs) {
		LoginEventType lastSuccessfulLogin = user.asObjectable().getCredentials().getPassword().getLastSuccessfulLogin();
		assertNotNull("no last successful login in "+user, lastSuccessfulLogin);
		XMLGregorianCalendar successfulLoginTs = lastSuccessfulLogin.getTimestamp();
		TestUtil.assertBetween("wrong last successful login timestamp", startTs, endTs, successfulLoginTs);
	}
	
	private void assertLastFailedLogin(PrismObject<UserType> user, XMLGregorianCalendar startTs,
			XMLGregorianCalendar endTs) {
		LoginEventType lastFailedLogin = user.asObjectable().getCredentials().getPassword().getLastFailedLogin();
		assertNotNull("no last failed login in "+user, lastFailedLogin);
		XMLGregorianCalendar failedLoginTs = lastFailedLogin.getTimestamp();
		TestUtil.assertBetween("wrong last failed login timestamp", startTs, endTs, failedLoginTs);
	}

	private MidPointPrincipal getAuthorizedPrincipal(String userJackUsername) throws ObjectNotFoundException {
		MidPointPrincipal principal = userProfileService.getPrincipal(USER_JACK_USERNAME);
		assertPrincipalJack(principal);
		if (principal.getAuthorities().isEmpty()) {
			AuthorizationType authorizationType = new AuthorizationType();
	        authorizationType.getAction().add("FAKE");
			principal.getAuthorities().add(new Authorization(authorizationType));
		}
		return principal;
	}

	private void assertPrincipalJack(MidPointPrincipal principal) {
		display("principal", principal);
		assertEquals("Bad principal name", USER_JACK_USERNAME, principal.getName().getOrig());
		assertEquals("Bad principal name", USER_JACK_USERNAME, principal.getUsername());
		UserType user = principal.getUser();
		assertNotNull("No user in principal",user);
		assertEquals("Bad name in user in principal", USER_JACK_USERNAME, user.getName().getOrig());
	}

}
