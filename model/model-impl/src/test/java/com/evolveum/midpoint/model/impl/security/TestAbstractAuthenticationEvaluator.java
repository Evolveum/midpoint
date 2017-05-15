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
package com.evolveum.midpoint.model.impl.security;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoginEventType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class TestAbstractAuthenticationEvaluator<V, AC extends AbstractAuthenticationContext, T extends AuthenticationEvaluator<AC>> extends AbstractInternalModelIntegrationTest {
	
	protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "security");
	
	private static final Trace LOGGER = TraceManager.getTrace(TestAbstractAuthenticationEvaluator.class);

	protected static final String USER_GUYBRUSH_PASSWORD = "XmarksTHEspot";
		
	
	@Autowired(required=true)
	private UserProfileService userProfileService;
	
	@Autowired(required = true)
	private Clock clock;

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
	
	public abstract AbstractCredentialType getCredentialUsedForAuthentication(UserType user);
	public abstract QName getCredentialType();
	
	public abstract void modifyUserCredential(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		((AuthenticationEvaluatorImpl)getAuthenticationEvaluator()).userProfileService = new UserProfileService() {
			
			@Override
			public <F extends FocusType, O extends ObjectType> PrismObject<F> resolveOwner(PrismObject<O> object) {
				return userProfileService.resolveOwner(object);
			}
			
			@Override
			public void updateUser(MidPointPrincipal principal) {
				userProfileService.updateUser(principal);
			}
			
			@Override
			public MidPointPrincipal getPrincipal(PrismObject<UserType> user) throws SchemaException {
				MidPointPrincipal principal = userProfileService.getPrincipal(user);
				addFakeAuthorization(principal);
				return principal;
			}
			
			@Override
			public MidPointPrincipal getPrincipal(String username) throws ObjectNotFoundException, SchemaException {
				MidPointPrincipal principal = userProfileService.getPrincipal(username);
				addFakeAuthorization(principal);
				return principal;
			}
		};
	}
	
	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTile(TEST_NAME);
		
		assertNotNull(getAuthenticationEvaluator());
		MidPointPrincipal principal = userProfileService.getPrincipal(USER_JACK_USERNAME);
		assertPrincipalJack(principal);
	}
	
	@Test
	public void test100PasswordLoginGoodPasswordJack() throws Exception {
		final String TEST_NAME = "test100PasswordLoginGoodPasswordJack";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		Authentication authentication = getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		assertGoodPasswordAuthentication(authentication, USER_JACK_USERNAME);
		
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
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		try {
		
			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertBadPasswordException(e, USER_JACK_USERNAME);
		}
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 1);
		assertUserLockout(userAfter, LockoutStatusType.NORMAL);
		assertLastFailedLogin(userAfter, startTs, endTs);
	}
	
	@Test
	public void test102PasswordLoginNullPasswordJack() throws Exception {
		final String TEST_NAME = "test102PasswordLoginNullPasswordJack";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		
		try {
		
			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, null));
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertPasswordEncodingException(e, USER_JACK_USERNAME);
		}
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 1);
		assertUserLockout(userAfter, LockoutStatusType.NORMAL);
	}


	@Test
	public void test103PasswordLoginEmptyPasswordJack() throws Exception {
		final String TEST_NAME = "test103PasswordLoginEmptyPasswordJack";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		
		try {
		
			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, get103EmptyPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertPasswordEncodingException(e, USER_JACK_USERNAME);
		}
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 1);
		assertUserLockout(userAfter, LockoutStatusType.NORMAL);
	}
	
	@Test
	public void test105PasswordLoginNullUsernameNullPassword() throws Exception {
		final String TEST_NAME = "test105PasswordLoginNullUsernameNullPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		
		try {
		
			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(null, null));
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertPasswordEncodingException(e, null);
		}
		
	}
	
	@Test
	public void test106PasswordLoginEmptyUsernameBadPassword() throws Exception {
		final String TEST_NAME = "test106PasswordLoginEmptyUsernameBadPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		
		try {
		
			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext("", getBadPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (UsernameNotFoundException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertNoUserException(e, null);
		}
		
	}

	@Test
	public void test107PasswordLoginBadUsernameBadPassword() throws Exception {
		final String TEST_NAME = "test107PasswordLoginBadUsernameBadPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		
		try {
		
			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext("NoSuchUser", getBadPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (UsernameNotFoundException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertNoUserException(e, null);
		}
		
	}

	/**
	 * Wait for 5 minutes. The failed login count should reset after 3 minutes. Therefore bad login
	 * count should be one after we try to make a bad login.
	 */
	@Test
	public void test125PasswordLoginBadPasswordJackAfterLockoutFailedAttemptsDuration() throws Exception {
		final String TEST_NAME = "test125PasswordLoginBadPasswordJackAfterLockoutFailedAttemptsDuration";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		clock.overrideDuration("PT5M");
		
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		try {
		
			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertBadPasswordException(e, USER_JACK_USERNAME);
		}
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 1);
		assertLastFailedLogin(userAfter, startTs, endTs);
		assertUserLockout(userAfter, LockoutStatusType.NORMAL);
	}


	@Test
	public void test130PasswordLoginLockout() throws Exception {
		final String TEST_NAME = "test130PasswordLoginLockout";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		try {
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
		} catch (BadCredentialsException e) {
			// This is expected
			
			display("expected exception", e);
			assertBadPasswordException(e, USER_JACK_USERNAME);
		}
		
		PrismObject<UserType> userBetween = getUser(USER_JACK_OID);
		display("user after", userBetween);
		assertFailedLogins(userBetween, 2);
		assertUserLockout(userBetween, LockoutStatusType.NORMAL);
		
		try {
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
		} catch (BadCredentialsException e) {
			// This is expected
			
			display("expected exception", e);
			assertBadPasswordException(e, USER_JACK_USERNAME);
		}
		
		
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 3);
		assertLastFailedLogin(userAfter, startTs, endTs);
		assertUserLockout(userAfter, LockoutStatusType.LOCKED);
	}
	
	@Test
	public void test132PasswordLoginLockedoutGoodPassword() throws Exception {
		final String TEST_NAME = "test132PasswordLoginLockedoutGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		ConnectionEnvironment connEnv = createConnectionEnvironment();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		try {
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
		} catch (LockedException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertLockedException(e, USER_JACK_USERNAME);
		}
				
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 3);
		assertUserLockout(userAfter, LockoutStatusType.LOCKED);
	}

	@Test
	public void test133PasswordLoginLockedoutBadPassword() throws Exception {
		final String TEST_NAME = "test133PasswordLoginLockedoutBadPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		ConnectionEnvironment connEnv = createConnectionEnvironment();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		try {
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
		} catch (LockedException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			
			// this is important. The exception should give no indication whether the password is
			// good or bad
			assertLockedException(e, USER_JACK_USERNAME);
		}
				
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 3);
		assertUserLockout(userAfter, LockoutStatusType.LOCKED);
	}

	@Test
	public void test135PasswordLoginLockedoutLockExpires() throws Exception {
		final String TEST_NAME = "test135PasswordLoginLockedoutLockExpires";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		clock.overrideDuration("PT30M");
		
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		Authentication authentication = getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		assertGoodPasswordAuthentication(authentication, USER_JACK_USERNAME);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 0);
		assertLastSuccessfulLogin(userAfter, startTs, endTs);
		assertUserLockout(userAfter, LockoutStatusType.NORMAL);
	}
	
	@Test
	public void test136PasswordLoginLockoutAgain() throws Exception {
		final String TEST_NAME = "test136PasswordLoginLockoutAgain";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		try {
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertBadPasswordException(e, USER_JACK_USERNAME);
		}
		
		PrismObject<UserType> userBetween = getUser(USER_JACK_OID);
		display("user after", userBetween);
		assertFailedLogins(userBetween, 1);
		assertUserLockout(userBetween, LockoutStatusType.NORMAL);
		
		try {
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertBadPasswordException(e, USER_JACK_USERNAME);
		}

		userBetween = getUser(USER_JACK_OID);
		display("user after", userBetween);
		assertFailedLogins(userBetween, 2);
		assertUserLockout(userBetween, LockoutStatusType.NORMAL);
		
		try {
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getBadPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertBadPasswordException(e, USER_JACK_USERNAME);
		}

		
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 3);
		assertLastFailedLogin(userAfter, startTs, endTs);
		assertUserLockout(userAfter, LockoutStatusType.LOCKED);
	}
	
	@Test
	public void test137PasswordLoginLockedoutGoodPasswordAgain() throws Exception {
		final String TEST_NAME = "test137PasswordLoginLockedoutGoodPasswordAgain";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		ConnectionEnvironment connEnv = createConnectionEnvironment();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		try {
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
		} catch (LockedException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertLockedException(e, USER_JACK_USERNAME);
		}
				
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 3);
		assertUserLockout(userAfter, LockoutStatusType.LOCKED);
	}
	
	@Test
	public void test138UnlockUserGoodPassword() throws Exception {
		final String TEST_NAME = "test138UnlockUserGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
				
		ConnectionEnvironment connEnv = createConnectionEnvironment();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, task, result, LockoutStatusType.NORMAL);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		
		PrismObject<UserType> userBetween = getUser(USER_JACK_OID);
		display("user after", userBetween);
		assertFailedLogins(userBetween, 0);
		assertUserLockout(userBetween, LockoutStatusType.NORMAL);

		// GIVEN
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		Authentication authentication = getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		assertGoodPasswordAuthentication(authentication, USER_JACK_USERNAME);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 0);
		assertLastSuccessfulLogin(userAfter, startTs, endTs);
		assertUserLockout(userAfter, LockoutStatusType.NORMAL);
	}
	
	/**
	 * MID-2862
	 */
	@Test
	public void test139TryToLockByModelService() throws Exception {
		final String TEST_NAME = "test139TryToLockByModelService";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
				
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		try {
			
			modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, task, result, LockoutStatusType.LOCKED);
			
			AssertJUnit.fail("Unexpected success");
		} catch (SchemaException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			
		}
				
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 0);
		assertUserLockout(userAfter, LockoutStatusType.NORMAL);
	}
	
	@Test
	public void test150PasswordLoginDisabledGoodPassword() throws Exception {
		final String TEST_NAME = "test150PasswordLoginDisabledGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);
		
		loginJackGoodPasswordExpectDenied(TEST_NAME, task, result);
	}
	
	@Test
	public void test152PasswordLoginEnabledGoodPassword() throws Exception {
		final String TEST_NAME = "test152PasswordLoginEnabledGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);
		
		loginJackGoodPasswordExpectSuccess(TEST_NAME, task, result);
	}
	
	@Test
	public void test154PasswordLoginNotValidYetGoodPassword() throws Exception {
		final String TEST_NAME = "test154PasswordLoginNotValidYetGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		XMLGregorianCalendar validFrom = XmlTypeConverter.addDuration(clock.currentTimeXMLGregorianCalendar(), "PT1H");
		XMLGregorianCalendar validTo = XmlTypeConverter.addDuration(clock.currentTimeXMLGregorianCalendar(), "P2D");
		
		modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result);
		modifyUserReplace(USER_JACK_OID, ACTIVATION_VALID_FROM_PATH, task, result, validFrom);
		modifyUserReplace(USER_JACK_OID, ACTIVATION_VALID_TO_PATH, task, result, validTo);
		
		loginJackGoodPasswordExpectDenied(TEST_NAME, task, result);
	}
	
	@Test
	public void test155PasswordLoginValidGoodPassword() throws Exception {
		final String TEST_NAME = "test155PasswordLoginValidGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		clock.overrideDuration("PT2H");
		
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
				
		loginJackGoodPasswordExpectSuccess(TEST_NAME, task, result);
	}
	
	@Test
	public void test156PasswordLoginNotValidAnyLongerGoodPassword() throws Exception {
		final String TEST_NAME = "test156PasswordLoginNotValidAnyLongerGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		clock.overrideDuration("P2D");
		
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		loginJackGoodPasswordExpectDenied(TEST_NAME, task, result);
	}
	
	@Test
	public void test159PasswordLoginNoLongerValidEnabledGoodPassword() throws Exception {
		final String TEST_NAME = "test159PasswordLoginNoLongerValidEnabledGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);
		
		loginJackGoodPasswordExpectSuccess(TEST_NAME, task, result);
	}
	
	@Test
	public void test160PasswordLoginLifecycleActiveGoodPassword() throws Exception {
		final String TEST_NAME = "test160PasswordLoginLifecycleActiveGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result, 
				SchemaConstants.LIFECYCLE_ACTIVE);
		
		loginJackGoodPasswordExpectSuccess(TEST_NAME, task, result);
	}
	
	@Test
	public void test162PasswordLoginLifecycleDraftGoodPassword() throws Exception {
		final String TEST_NAME = "test162PasswordLoginLifecycleDraftGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result, 
				SchemaConstants.LIFECYCLE_DRAFT);
		
		loginJackGoodPasswordExpectDenied(TEST_NAME, task, result);
	}
	
	@Test
	public void test164PasswordLoginLifecycleDeprecatedGoodPassword() throws Exception {
		final String TEST_NAME = "test164PasswordLoginLifecycleDeprecatedGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result, 
				SchemaConstants.LIFECYCLE_DEPRECATED);
		
		loginJackGoodPasswordExpectSuccess(TEST_NAME, task, result);
	}
	
	@Test
	public void test166PasswordLoginLifecycleProposedGoodPassword() throws Exception {
		final String TEST_NAME = "test166PasswordLoginLifecycleProposedGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result, 
				SchemaConstants.LIFECYCLE_PROPOSED);
				
		loginJackGoodPasswordExpectDenied(TEST_NAME, task, result);
	}
	
	@Test
	public void test168PasswordLoginLifecycleArchivedGoodPassword() throws Exception {
		final String TEST_NAME = "test168PasswordLoginLifecycleArchivedGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result, 
				SchemaConstants.LIFECYCLE_ARCHIVED);
		
		loginJackGoodPasswordExpectDenied(TEST_NAME, task, result);
	}
		
	@Test
	public void test200UserGuybrushSetCredentials() throws Exception {
		final String TEST_NAME = "test200UserGuybrushSetPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TestAbstractAuthenticationEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
        
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserCredential(task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("user after", userAfter);
		
//		assertEncryptedUserPassword(userAfter, USER_GUYBRUSH_PASSWORD);
		assertPasswordMetadata(userAfter, getCredentialType(), false, startTs, endTs, null, SchemaConstants.CHANNEL_GUI_USER_URI);
		
		assertFailedLogins(userAfter, 0);
	}

	@Test
	public void test201UserGuybrushPasswordLoginGoodPassword() throws Exception {
		final String TEST_NAME = "test201UserGuybrushPasswordLoginGoodPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		Authentication authentication = getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_GUYBRUSH_USERNAME, getGoodPasswordGuybrush()));
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		assertGoodPasswordAuthentication(authentication, USER_GUYBRUSH_USERNAME);
		
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 0);
		assertLastSuccessfulLogin(userAfter, startTs, endTs);
	}
	
	@Test
	public void test202UserGuybrushPasswordLoginBadPassword() throws Exception {
		final String TEST_NAME = "test202UserGuybrushPasswordLoginBadPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		try {
		
			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_GUYBRUSH_USERNAME, getBadPasswordGuybrush()));
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (BadCredentialsException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertBadPasswordException(e, USER_GUYBRUSH_USERNAME);
		}
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 1);
		assertLastFailedLogin(userAfter, startTs, endTs);
	}
	
	@Test
	public void test209UserGuybrushPasswordLoginGoodPasswordBeforeExpiration() throws Exception {
		final String TEST_NAME = "test209UserGuybrushPasswordLoginGoodPasswordBeforeExpiration";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		clock.overrideDuration("P29D");
		
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		Authentication authentication = getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_GUYBRUSH_USERNAME, getGoodPasswordGuybrush()));
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		assertGoodPasswordAuthentication(authentication, USER_GUYBRUSH_USERNAME);
		
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 0);
		assertLastSuccessfulLogin(userAfter, startTs, endTs);
	}
	
	@Test
	public void test210UserGuybrushPasswordLoginGoodPasswordExpired() throws Exception {
		final String TEST_NAME = "test210UserGuybrushPasswordLoginGoodPasswordExpired";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		clock.overrideDuration("P2D");
		
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();
		
		try {
		
			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_GUYBRUSH_USERNAME, getGoodPasswordGuybrush()));
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (CredentialsExpiredException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			assertExpiredException(e, USER_GUYBRUSH_USERNAME);
		}
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 0);
	}
	
	private void assertGoodPasswordAuthentication(Authentication authentication, String expectedUsername) {
		assertNotNull("No authentication", authentication);
		assertTrue("authentication: not authenticated", authentication.isAuthenticated());
		MidPointAsserts.assertInstanceOf("authentication", authentication, UsernamePasswordAuthenticationToken.class);
		assertEquals("authentication: principal mismatch", expectedUsername, ((MidPointPrincipal)authentication.getPrincipal()).getUsername());
	}

	private void assertBadPasswordException(BadCredentialsException e, String username) {
		assertEquals("Wrong exception meessage (key)", "web.security.provider.invalid", e.getMessage());
	}
	
	private void assertPasswordEncodingException(BadCredentialsException e, String principal) {
		assertEquals("Wrong exception meessage (key)", "web.security.provider.password.encoding", e.getMessage());
	}
	
	private void assertDeniedException(AccessDeniedException e, String principal) {
		assertEquals("Wrong exception meessage (key)", "web.security.provider.access.denied", e.getMessage());
	}
	
	private void assertLockedException(LockedException e, String principal) {
		assertEquals("Wrong exception meessage (key)", "web.security.provider.locked", e.getMessage());
	}
	
	private void assertDisabledException(DisabledException e, String principal) {
		assertEquals("Wrong exception meessage (key)", "web.security.provider.disabled", e.getMessage());
	}
	
	private void assertExpiredException(CredentialsExpiredException e, String principal) {
		assertEquals("Wrong exception meessage (key)", "web.security.provider.password.bad", e.getMessage());
	}
	
	private void assertNoUserException(UsernameNotFoundException e, String principal) {
		assertEquals("Wrong exception meessage (key)", "web.security.provider.invalid", e.getMessage());
	}
	
	private ConnectionEnvironment createConnectionEnvironment() {
		ConnectionEnvironment connEnv = new ConnectionEnvironment();
		connEnv.setRemoteHost("remote.example.com");
		return connEnv;
	}
	
	private void assertFailedLogins(PrismObject<UserType> user, int expected) {
		if (expected == 0 && getCredentialUsedForAuthentication(user.asObjectable()).getFailedLogins() == null) {
			return;
		}
		assertEquals("Wrong failed logins in "+user, (Integer)expected, getCredentialUsedForAuthentication(user.asObjectable()).getFailedLogins());
	}

	private void assertLastSuccessfulLogin(PrismObject<UserType> user, XMLGregorianCalendar startTs,
			XMLGregorianCalendar endTs) {
		LoginEventType lastSuccessfulLogin = getCredentialUsedForAuthentication(user.asObjectable()).getLastSuccessfulLogin();
		assertNotNull("no last successful login in "+user, lastSuccessfulLogin);
		XMLGregorianCalendar successfulLoginTs = lastSuccessfulLogin.getTimestamp();
		TestUtil.assertBetween("wrong last successful login timestamp", startTs, endTs, successfulLoginTs);
	}
	
	private void assertLastFailedLogin(PrismObject<UserType> user, XMLGregorianCalendar startTs,
			XMLGregorianCalendar endTs) {
		LoginEventType lastFailedLogin = getCredentialUsedForAuthentication(user.asObjectable()).getLastFailedLogin();
		assertNotNull("no last failed login in "+user, lastFailedLogin);
		XMLGregorianCalendar failedLoginTs = lastFailedLogin.getTimestamp();
		TestUtil.assertBetween("wrong last failed login timestamp", startTs, endTs, failedLoginTs);
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
		display("principal", principal);
		assertEquals("Bad principal name", USER_JACK_USERNAME, principal.getName().getOrig());
		assertEquals("Bad principal name", USER_JACK_USERNAME, principal.getUsername());
		UserType user = principal.getUser();
		assertNotNull("No user in principal",user);
		assertEquals("Bad name in user in principal", USER_JACK_USERNAME, user.getName().getOrig());
	}
	
	private void loginJackGoodPasswordExpectSuccess(final String TEST_NAME, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		display("now", clock.currentTimeXMLGregorianCalendar());
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		Authentication authentication = getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
		assertGoodPasswordAuthentication(authentication, USER_JACK_USERNAME);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 0);
		assertLastSuccessfulLogin(userAfter, startTs, endTs);
	}
	
	private void loginJackGoodPasswordExpectDenied(final String TEST_NAME, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		display("now", clock.currentTimeXMLGregorianCalendar());
		ConnectionEnvironment connEnv = createConnectionEnvironment();
		XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		try {
			
			getAuthenticationEvaluator().authenticate(connEnv, getAuthenticationContext(USER_JACK_USERNAME, getGoodPasswordJack()));
			
			AssertJUnit.fail("Unexpected success");
		} catch (DisabledException e) {
			// This is expected
			
			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("expected exception", e);
			
			// this is important. The exception should give no indication whether the password is
			// good or bad
			assertDisabledException(e, USER_JACK_USERNAME);
		}
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("user after", userAfter);
		assertFailedLogins(userAfter, 0);
	}
}
