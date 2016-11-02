/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.common.test;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.policy.PasswordPolicyUtils;
import com.evolveum.midpoint.common.policy.StringPolicyUtils;
import com.evolveum.midpoint.common.policy.ValuePolicyGenerator;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringLimitType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

public class PasswordPolicyValidatorTest {

	public PasswordPolicyValidatorTest() {

	}

	public static final String BASE_PATH = "src/test/resources/policy/";

	private static final transient Trace LOGGER = TraceManager.getTrace(PasswordPolicyValidatorTest.class);

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	@Test
	public void stringPolicyUtilsMinimalTest() throws JAXBException, SchemaException, IOException {
		String filename = "password-policy-minimal.xml";
		String pathname = BASE_PATH + filename;
		File file = new File(pathname);
		ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
		StringPolicyType sp = pp.getStringPolicy();
		StringPolicyUtils.normalize(sp);
		AssertJUnit.assertNotNull(sp.getCharacterClass());
		AssertJUnit.assertNotNull(sp.getLimitations().getLimit());
		AssertJUnit.assertTrue(-1 == sp.getLimitations().getMaxLength());
		AssertJUnit.assertTrue(0 == sp.getLimitations().getMinLength());
		AssertJUnit.assertTrue(0 == " !\"#$%&'()*+,-.01234567890:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
				.compareTo(sp.getCharacterClass().getValue()));
	}

	@Test
	public void stringPolicyUtilsComplexTest() {
		String filename = "password-policy-complex.xml";
		String pathname = BASE_PATH + filename;
		File file = new File(pathname);
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
		String filename = "password-policy-complex.xml";
		String pathname = BASE_PATH + filename;
		File file = new File(pathname);
		ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
		OperationResult op = new OperationResult("passwordGeneratorComplexTest");
		
		op = new OperationResult("passwordGeneratorComplexTest");
		// Make switch some cosistency
		pp.getStringPolicy().getLimitations().setMinLength(2);
		pp.getStringPolicy().getLimitations().setMinUniqueChars(5);
		String psswd = ValuePolicyGenerator.generate(pp.getStringPolicy(), 10, op);
		op.computeStatus();
		assertNotNull(psswd);
		AssertJUnit.assertTrue(op.isAcceptable());

		// Switch to all must be first :-) to test if there is error
		for (StringLimitType l : pp.getStringPolicy().getLimitations().getLimit()) {
			l.setMustBeFirst(true);
		}
		LOGGER.info("Negative testing: passwordGeneratorComplexTest");
		psswd = ValuePolicyGenerator.generate(pp.getStringPolicy(), 10, op);
		assertNull(psswd);
		op.computeStatus();
		AssertJUnit.assertTrue(op.getStatus() == OperationResultStatus.FATAL_ERROR);
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
		LOGGER.info("===[ {} ]===", "testValueGenerateRandomPin");
		String pathname = BASE_PATH + "value-policy-random-pin.xml";
		File file = new File(pathname);
		LOGGER.info("Positive testing {}: {}", "testValueGenerateRandomPin", "value-policy-random-pin.xml");
		ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
		OperationResult op = new OperationResult("testValueGenerateRandomPin");
		
		String psswd;
			psswd = ValuePolicyGenerator.generate(pp.getStringPolicy(), 10, true, op);
			LOGGER.info("Generated password:" + psswd);
			System.out.println("Generated password: " + psswd);
			op.computeStatus();
			if (!op.isSuccess()) {
				LOGGER.info("Result:" + op.debugDump());
				AssertJUnit.fail("Password generator failed:\n"+op.debugDump());
			}
			assertNotNull(psswd);
			assertPassword(psswd, pp);
	
	}
	
	@Test
	public void testValueGenerate() throws Exception {
		LOGGER.info("===[ {} ]===", "testValueGenerate");
		String pathname = BASE_PATH + "value-policy-generate.xml";
		File file = new File(pathname);
		LOGGER.info("Positive testing {}: {}", "testValueGenerate", "value-policy-generate.xml");
		ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
		OperationResult op = new OperationResult("testValueGenerate");
		
		String psswd;
			psswd = ValuePolicyGenerator.generate(pp.getStringPolicy(), 10, true, op);
			LOGGER.info("Generated password:" + psswd);
			System.out.println("Generated password: " + psswd);
			op.computeStatus();
			if (!op.isSuccess()) {
				LOGGER.info("Result:" + op.debugDump());
				AssertJUnit.fail("Password generator failed:\n"+op.debugDump());
			}
			assertNotNull(psswd);
			assertPassword(psswd, pp);
	
	}
	
	@Test
	public void testValueGenerateEmpty() throws Exception {
		LOGGER.info("===[ {} ]===", "testValueGenerateEmpty");
		String pathname = BASE_PATH + "value-policy-generate-empty.xml";
		File file = new File(pathname);
		LOGGER.info("Positive testing {}: {}", "testValueGenerate", "value-policy-generate-empty.xml");
		ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
		OperationResult op = new OperationResult("testValueGenerateEmpty");
		
		String psswd;
			psswd = ValuePolicyGenerator.generate(pp.getStringPolicy(), 10, true, op);
			LOGGER.info("Generated password:" + psswd);
			System.out.println("Generated password: " + psswd);
			op.computeStatus();
			if (!op.isSuccess()) {
				LOGGER.info("Result:" + op.debugDump());
				AssertJUnit.fail("Password generator failed:\n"+op.debugDump());
			}
			assertNotNull(psswd);
			assertPassword(psswd, pp);
	
	}

	public void passwordGeneratorTest(final String TEST_NAME, String policyFilename) throws JAXBException, SchemaException, IOException {
		LOGGER.info("===[ {} ]===", TEST_NAME);
		String pathname = BASE_PATH + policyFilename;
		File file = new File(pathname);
		LOGGER.info("Positive testing {}: {}", TEST_NAME, policyFilename);
		ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
		OperationResult op = new OperationResult(TEST_NAME);
		
		String psswd;
		// generate minimal size passwd
		for (int i = 0; i < 100; i++) {
			psswd = ValuePolicyGenerator.generate(pp.getStringPolicy(), 10, true, op);
			LOGGER.info("Generated password:" + psswd);
			op.computeStatus();
			if (!op.isSuccess()) {
				LOGGER.info("Result:" + op.debugDump());
				AssertJUnit.fail("Password generator failed:\n"+op.debugDump());
			}
			assertNotNull(psswd);
			assertPassword(psswd, pp);
		}
		// genereata to meet as possible
		LOGGER.info("-------------------------");
		// Generate up to possible
		for (int i = 0; i < 100; i++) {
			psswd = ValuePolicyGenerator.generate(pp.getStringPolicy(), 10, false, op);
			LOGGER.info("Generated password:" + psswd);
			op.computeStatus();
			if (!op.isSuccess()) {
				LOGGER.info("Result:" + op.debugDump());
			}
			AssertJUnit.assertTrue(op.isSuccess());
			assertNotNull(psswd);

		}
	}

	private void assertPassword(String passwd, ValuePolicyType pp) {
		OperationResult validationResult = PasswordPolicyUtils.validatePassword(passwd, null, pp);
		if (!validationResult.isSuccess()) {
			AssertJUnit.fail(validationResult.debugDump());
		}
	}

	@Test
	public void passwordValidationTestComplex() throws Exception {
		final String TEST_NAME = "passwordValidationTestComplex";
		TestUtil.displayTestTile(TEST_NAME);
		
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
		TestUtil.displayTestTile(TEST_NAME);
		
		ValuePolicyType pp = parsePasswordPolicy("password-policy-tri.xml");

		// WHEN, THEN
		AssertJUnit.assertTrue(pwdValidHelper("Password1", pp));
		AssertJUnit.assertFalse(pwdValidHelper("password1", pp)); // no capital letter
		AssertJUnit.assertFalse(pwdValidHelper("1PASSWORD", pp)); // no lowecase letter
		AssertJUnit.assertFalse(pwdValidHelper("Password", pp)); // no numeral
		AssertJUnit.assertFalse(pwdValidHelper("Pa1", pp)); // too short
		AssertJUnit.assertFalse(pwdValidHelper("PPPPPPPPPPPPPPPPPPPPPPPaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa11111111111111111111111111111111111111111111111111111111111111111111", pp)); // too long
	}

	private ValuePolicyType parsePasswordPolicy(String filename) throws SchemaException, IOException {
		File file = new File(BASE_PATH, filename);
		return (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
	}

	private boolean pwdValidHelper(String password, ValuePolicyType pp) {
		OperationResult op = new OperationResult("Password Validator test with password:" + password);
		PasswordPolicyUtils.validatePassword(password, null, pp, op);
		op.computeStatus();
		String msg = "-> Policy "+pp.getName()+", password '"+password+"': "+op.getStatus();
		System.out.println(msg);
		LOGGER.info(msg);
		LOGGER.trace(op.debugDump());
		return (op.isSuccess());
	}

	@Test
	public void passwordValidationMultipleTest() throws Exception {
		final String TEST_NAME = "passwordValidationMultipleTest";
    	TestUtil.displayTestTile(TEST_NAME);
    	
		String filename = "password-policy-complex.xml";
		String pathname = BASE_PATH + filename;
		File file = new File(pathname);
		
		ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();

		String password = "582a**A";
		
		OperationResult op = new OperationResult(TEST_NAME);
		List<ValuePolicyType> pps = new ArrayList<ValuePolicyType>();
		pps.add(pp);
		pps.add(pp);
		pps.add(pp);
		
		PasswordPolicyUtils.validatePassword(password, null, pps, op);
		op.computeStatus();
		LOGGER.error(op.debugDump());
		AssertJUnit.assertTrue(op.isSuccess());
		
	}

	@Test
	public void XMLPasswordPolicy() throws JAXBException, SchemaException, IOException {

		String filename = "password-policy-complex.xml";
		String pathname = BASE_PATH + filename;
		File file = new File(pathname);

		ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();

		OperationResult op = new OperationResult("Generator testing");

		// String pswd = PasswordPolicyUtils.generatePassword(pp, op);
		// LOGGER.info("Generated password: " + pswd);
		// assertNotNull(pswd);
		// assertTrue(op.isSuccess());
	}
}
