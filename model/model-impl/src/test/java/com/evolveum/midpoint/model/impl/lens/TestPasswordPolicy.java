/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.model.impl.lens;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.common.stringpolicy.StringPolicyUtils;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.CredentialPolicyEvaluator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringLimitType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPasswordPolicy extends AbstractInternalModelIntegrationTest {

	public static final File TEST_DIR =  new File("src/test/resources/lens/ppolicy/");

	private static final transient Trace LOGGER = TraceManager.getTrace(TestPasswordPolicy.class);

	private static final String USER_AB_USERNAME = "ab";
	private static final String USER_AB_FULL_NAME = "Ad Fel";
	private static final String USER_AB_GIVEN_NAME = "Ad";
	private static final String USER_AB_FAMILY_NAME = "Fel";
	private static final String USER_AB_ADDITIONAL_NAME = "x";
	private static final int USERNAME_ATTEMPTS = 200;
	private static final int USER_PROPS_ATTEMPTS = 5000;

	@Autowired(required = true)
	private ValuePolicyProcessor valuePolicyProcessor;
		
	@Test
	public void stringPolicyUtilsMinimalTest() throws JAXBException, SchemaException, IOException {
		File file = new File(TEST_DIR, "password-policy-minimal.xml");
		ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
		StringPolicyType sp = pp.getStringPolicy();
		StringPolicyUtils.normalize(sp);
		AssertJUnit.assertNotNull(sp.getCharacterClass());
		AssertJUnit.assertNotNull(sp.getLimitations().getLimit());
		AssertJUnit.assertTrue(Integer.MAX_VALUE == sp.getLimitations().getMaxLength());
		AssertJUnit.assertTrue(0 == sp.getLimitations().getMinLength());
		AssertJUnit.assertTrue(0 == " !\"#$%&'()*+,-.01234567890:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
				.compareTo(sp.getCharacterClass().getValue()));
	}

	@Test
	public void stringPolicyUtilsComplexTest() {
		final String TEST_NAME = "stringPolicyUtilsComplexTest";
		TestUtil.displayTestTitle(TEST_NAME);
		
		File file = new File(TEST_DIR, "password-policy-complex.xml");
		ValuePolicyType pp = null;
		try {
			pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
		} catch (Exception e) {
			e.printStackTrace();
		}

		StringPolicyType sp = pp.getStringPolicy();
		StringPolicyUtils.normalize(sp);
	}
	
	@Test
	public void testPasswordGeneratorComplexNegative() throws Exception {
		final String TEST_NAME = "testPasswordGeneratorComplexNegative";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		File file = new File(TEST_DIR, "password-policy-complex.xml");
		ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();

		// Make switch some cosistency
		pp.getStringPolicy().getLimitations().setMinLength(2);
		pp.getStringPolicy().getLimitations().setMinUniqueChars(5);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		String psswd = valuePolicyProcessor.generate(SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, false, null, TEST_NAME, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		display("Generated password", psswd);
		result.computeStatus();
		AssertJUnit.assertTrue(result.isAcceptable());
		assertNotNull(psswd);

		// Switch to all must be first :-) to test if there is error
		for (StringLimitType l : pp.getStringPolicy().getLimitations().getLimit()) {
			l.setMustBeFirst(true);
		}
		LOGGER.info("Negative testing: passwordGeneratorComplexTest");
		try {
			valuePolicyProcessor.generate(SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, false, null, TEST_NAME, task, result);
			assertNotReached();
		} catch (ExpressionEvaluationException e) {
			result.computeStatus();
			TestUtil.assertFailure(result);
		}
	}
	
	@Test
	public void testPasswordGeneratorComplex() throws Exception {
		passwordGeneratorTest("testPasswordGeneratorComplex", "password-policy-complex.xml");
	}
	
	@Test
	public void testPasswordGeneratorLong() throws Exception {
		passwordGeneratorTest("testPasswordGeneratorLong", "password-policy-long.xml");
	}

	@Test
	public void testPasswordGeneratorNumeric() throws Exception {
		passwordGeneratorTest("testPasswordGeneratorNumeric", "password-policy-numeric.xml");
	}
	
	@Test
	public void testValueGeneratorMustBeFirst() throws Exception {
		passwordGeneratorTest("testValueGeneratorMustBeFirst", "value-policy-must-be-first.xml");
	}
	
	@Test
	public void testValueGenerateRandomPin() throws Exception {
		final String TEST_NAME = "testValueGenerateRandomPin";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ValuePolicyType pp = parsePasswordPolicy("value-policy-random-pin.xml");

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		String psswd = valuePolicyProcessor.generate(SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, true, null, TEST_NAME, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		display("Generated password", psswd);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertNotNull(psswd);
		assertPassword(psswd, pp);
	
	}
	
	@Test
	public void testValueGenerate() throws Exception {
		final String TEST_NAME = "testValueGenerate";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		ValuePolicyType pp = parsePasswordPolicy("value-policy-generate.xml");
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		String psswd = valuePolicyProcessor.generate(SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, true, null, TEST_NAME, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		display("Generated password", psswd);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertNotNull(psswd);
		assertPassword(psswd, pp);
	
	}
	
	@Test
	public void testValueGenerateEmpty() throws Exception {
		final String TEST_NAME = "testValueGenerateEmpty";
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		File file = new File(TEST_DIR, "value-policy-generate-empty.xml");
		LOGGER.info("Positive testing {}: {}", "testValueGenerate", "value-policy-generate-empty.xml");
		ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		String psswd = valuePolicyProcessor.generate(SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, true, null, TEST_NAME, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		display("Generated password", psswd);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertNotNull(psswd);
		assertPassword(psswd, pp);
	}

	public void passwordGeneratorTest(final String TEST_NAME, String policyFilename) throws JAXBException, SchemaException, IOException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		TestUtil.displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		File file = new File(TEST_DIR, policyFilename);
		LOGGER.info("Positive testing {}: {}", TEST_NAME, policyFilename);
		ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
		
		String psswd;
		// generate minimal size passwd
		for (int i = 0; i < 100; i++) {
			psswd = valuePolicyProcessor.generate(SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, true, null, TEST_NAME, task, result);
			LOGGER.info("Generated password:" + psswd);
			result.computeStatus();
			if (!result.isSuccess()) {
				LOGGER.info("Result:" + result.debugDump());
				AssertJUnit.fail("Password generator failed:\n"+result.debugDump());
			}
			assertNotNull(psswd);
			assertPassword(psswd, pp);
		}
		// genereata to meet as possible
		LOGGER.info("-------------------------");
		// Generate up to possible
		for (int i = 0; i < 100; i++) {
			psswd = valuePolicyProcessor.generate(SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, false, null, TEST_NAME, task, result);
			LOGGER.info("Generated password:" + psswd);
			result.computeStatus();
			if (!result.isSuccess()) {
				LOGGER.info("Result:" + result.debugDump());
			}
			AssertJUnit.assertTrue(result.isSuccess());
			assertNotNull(psswd);

		}
	}

	private void assertPassword(String passwd, ValuePolicyType pp) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		assertPassword(passwd, pp, null);
	}
	
	private <O extends ObjectType> void assertPassword(String passwd, ValuePolicyType pp, PrismObject<UserType> object) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = createTask("assertPassword");
		OperationResult result = task.getResult();
		boolean isValid = valuePolicyProcessor.validateValue(passwd, pp, createUserOriginResolver(object), "assertPassword", task, result);
		result.computeStatus();
		if (!result.isSuccess()) {
			AssertJUnit.fail(result.debugDump());
		}
		AssertJUnit.assertTrue("Password not valid (but result is success)", isValid);
	}

	@Test
	public void passwordValidationTestComplex() throws Exception {
		final String TEST_NAME = "passwordValidationTestComplex";
		TestUtil.displayTestTitle(TEST_NAME);
		
		ValuePolicyType pp = parsePasswordPolicy("password-policy-complex.xml");

		// WHEN, THEN
		AssertJUnit.assertTrue(pwdValidHelper("582a**A", pp));
		AssertJUnit.assertFalse(pwdValidHelper("58", pp));
		AssertJUnit.assertFalse(pwdValidHelper("333a**aGaa", pp));
		AssertJUnit.assertFalse(pwdValidHelper("AAA4444", pp));
	}

	@Test
	public void passwordValidationTestTri() throws Exception {
		final String TEST_NAME = "passwordValidationTestTri";
		TestUtil.displayTestTitle(TEST_NAME);
		
		ValuePolicyType pp = parsePasswordPolicy("password-policy-tri.xml");

		// WHEN, THEN
		AssertJUnit.assertTrue(pwdValidHelper("Password1", pp));
		AssertJUnit.assertFalse(pwdValidHelper("password1", pp)); // no capital letter
		AssertJUnit.assertFalse(pwdValidHelper("1PASSWORD", pp)); // no lowecase letter
		AssertJUnit.assertFalse(pwdValidHelper("Password", pp)); // no numeral
		AssertJUnit.assertFalse(pwdValidHelper("Pa1", pp)); // too short
		AssertJUnit.assertFalse(pwdValidHelper("PPPPPPPPPPPPPPPPPPPPPPPaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa11111111111111111111111111111111111111111111111111111111111111111111", pp)); // too long
	}
	
	/**
	 * MID-1657
	 */
	@Test
	public void testUsername() throws Exception {
		final String TEST_NAME = "testUsername";
		TestUtil.displayTestTitle(TEST_NAME);
				
		PrismObject<UserType> user = createUserAb();
		ValuePolicyType pp = parsePasswordPolicy("password-policy-username.xml");
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);

		for (int i = 0; i < USERNAME_ATTEMPTS; i++) {
			Task task = createTask(TEST_NAME+":"+i);
			OperationResult result = task.getResult();
		
			String psswd = valuePolicyProcessor.generate(SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, true, createUserOriginResolver(user), TEST_NAME, task, result);
			display("Generated password ("+i+")", psswd);
			
			result.computeStatus();
			TestUtil.assertSuccess(result);
			assertNotNull(psswd);
			assertPassword(psswd, pp, user);
			
			assertFalse("Generated password that matches the username: "+psswd, psswd.equals(USER_AB_USERNAME));
		}
		
		// THEN
		TestUtil.displayThen(TEST_NAME);	
	}

	/**
	 * MID-1657
	 */
	@Test
	public void testUserProps() throws Exception {
		final String TEST_NAME = "testUserProps";
		TestUtil.displayTestTitle(TEST_NAME);
				
		PrismObject<UserType> user = createUserAb();
		display("User", user);
		ValuePolicyType pp = parsePasswordPolicy("password-policy-props.xml");
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);

		for (int i = 0; i < USER_PROPS_ATTEMPTS; i++) {
			Task task = createTask(TEST_NAME+":"+i);
			OperationResult result = task.getResult();
		
			String psswd = valuePolicyProcessor.generate(SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, true, createUserOriginResolver(user), TEST_NAME, task, result);
			display("Generated password ("+i+")", psswd);
			
			result.computeStatus();
			TestUtil.assertSuccess(result);
			assertNotNull(psswd);
			assertPassword(psswd, pp, user);
			
			assertNotContains(psswd, USER_AB_USERNAME);
			assertNotContains(psswd, USER_AB_GIVEN_NAME);
			assertNotContains(psswd, USER_AB_ADDITIONAL_NAME);
		}
		
		// THEN
		TestUtil.displayThen(TEST_NAME);	
	}
	
	private PrismObject<UserType> createUserAb() throws SchemaException {
		PrismObject<UserType> user = createUser(USER_AB_USERNAME, USER_AB_GIVEN_NAME, USER_AB_FAMILY_NAME, true);
		user.asObjectable().setAdditionalName(createPolyStringType(USER_AB_ADDITIONAL_NAME));
		return user;
	}

	private void assertNotContains(String psswd, String val) {
		assertFalse("Generated password "+psswd+" contains value "+val, StringUtils.containsIgnoreCase(psswd, val));
	}

	private ValuePolicyType parsePasswordPolicy(String filename) throws SchemaException, IOException {
		File file = new File(TEST_DIR, filename);
		return (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
	}

	private boolean pwdValidHelper(String password, ValuePolicyType pp) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = createTask("pwdValidHelper");
		OperationResult result = task.getResult();
		valuePolicyProcessor.validateValue(password, pp, null, "pwdValidHelper", task, result);
		result.computeStatus();
		String msg = "-> Policy "+pp.getName()+", password '"+password+"': "+result.getStatus();
		System.out.println(msg);
		LOGGER.info(msg);
		LOGGER.trace(result.debugDump());
		return (result.isSuccess());
	}
}
