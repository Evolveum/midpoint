/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.common.stringpolicy.StringPolicy;

import com.evolveum.midpoint.model.common.stringpolicy.StringPolicy.CharacterClassLimitation;
import com.evolveum.midpoint.schema.config.ConfigurationItem;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringLimitType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPasswordPolicy extends AbstractInternalModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/lens/ppolicy/");

    private static final String USER_AB_USERNAME = "ab";
    private static final String USER_AB_GIVEN_NAME = "Ad";
    private static final String USER_AB_FAMILY_NAME = "Fel";
    private static final String USER_AB_ADDITIONAL_NAME = "x";
    private static final int USERNAME_ATTEMPTS = 200;
    private static final int USER_PROPS_ATTEMPTS = 5000;

    @Autowired private ValuePolicyProcessor valuePolicyProcessor;

    @Test
    public void stringPolicyUtilsMinimalTest() throws SchemaException, IOException, ConfigurationException {
        File file = new File(TEST_DIR, "password-policy-minimal.xml");
        ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
        StringPolicy sp = StringPolicy.compile(ConfigurationItem.embedded(pp.getStringPolicy()));
        assertThat(sp.getCharacterClassLimitations()).isEmpty();
    }

    @Test
    public void stringPolicyUtilsComplexTest() throws Exception {
        File file = new File(TEST_DIR, "password-policy-complex.xml");
        ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
        StringPolicy sp = StringPolicy.compile(ConfigurationItem.embedded(pp.getStringPolicy()));
        Collection<CharacterClassLimitation> limitations = sp.getCharacterClassLimitations();
        assertThat(limitations).as("limitations").hasSize(6);
        var limitationsMap = limitations.stream()
                .collect(Collectors.toMap(
                        lim -> lim.getDescription(),
                        lim -> lim));
        assertLimitation(limitationsMap, "extreme", 1, 2, true, "345678");
        assertLimitation(limitationsMap, "Alphas", 1, 2, false, "ABCDabcd");
        assertLimitation(limitationsMap, "Numbers", 1, 2, false, "1234");
        assertLimitation(limitationsMap, "Lowers", 1, 2, false, "abcd");
        assertLimitation(limitationsMap, "Specials", 1, 2, false, "!#$%*+@");
        assertLimitation(limitationsMap, "Alphanum", 1, 3, false, "1234ABCDabcd");
    }

    @SuppressWarnings("SameParameterValue")
    private void assertLimitation(
            Map<String, CharacterClassLimitation> limitationsMap, String description,
            int minOccurs, Integer maxOccurs, boolean first, String chars) {
        var lim = limitationsMap.get(description);
        assertThat(lim).as("limitation " + description).isNotNull();
        assertThat(lim.minOccurrences()).as("minOccurs").isEqualTo(minOccurs);
        assertThat(lim.declaredMaxOccurrences()).as("maxOccurs").isEqualTo(maxOccurs);
        assertThat(lim.mustBeFirst()).as("mustBeFirst").isEqualTo(first);
        assertThat(lim.characterClass().getCharactersAsString()).as("characters").isEqualTo(chars);
    }

    @Test
    public void testPasswordGeneratorComplexNegative() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        File file = new File(TEST_DIR, "password-policy-complex.xml");
        ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();

        // Make switch some cosistency
        pp.getStringPolicy().getLimitations().setMinLength(2);
        pp.getStringPolicy().getLimitations().setMinUniqueChars(5);

        // WHEN
        when();
        String psswd = valuePolicyProcessor.generate(
                SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, null,
                getTestNameShort(), task, result);

        // THEN
        then();
        displayValue("Generated password", psswd);
        result.computeStatus();
        AssertJUnit.assertTrue(result.isAcceptable());
        assertNotNull(psswd);

        // Switch to all must be first :-) to test if there is error
        for (StringLimitType l : pp.getStringPolicy().getLimitations().getLimit()) {
            l.setMustBeFirst(true);
        }
        logger.info("Negative testing: passwordGeneratorComplexTest");
        try {
            valuePolicyProcessor.generate(
                    SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, null,
                    getTestNameShort(), task, result);
            assertNotReached();
        } catch (ExpressionEvaluationException e) {
            displayExpectedException(e);
            result.computeStatus();
            TestUtil.assertFailure(result);
        }
    }

    @Test
    public void testPasswordGeneratorComplex() throws Exception {
        passwordGeneratorTest("password-policy-complex.xml");
    }

    @Test
    public void testPasswordGeneratorLong() throws Exception {
        passwordGeneratorTest("password-policy-long.xml");
    }

    @Test
    public void testPasswordGeneratorNumeric() throws Exception {
        passwordGeneratorTest("password-policy-numeric.xml");
    }

    @Test
    public void testValueGeneratorMustBeFirst() throws Exception {
        passwordGeneratorTest("value-policy-must-be-first.xml");
    }

    @Test
    public void testValueGenerateRandomPin() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ValuePolicyType pp = parsePasswordPolicy("value-policy-random-pin.xml");

        when();
        String psswd = valuePolicyProcessor.generate(
                SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, null,
                getTestNameShort(), task, result);

        then();
        displayValue("Generated password", psswd);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertNotNull(psswd);
        assertPassword(psswd, pp);
    }

    @Test
    public void testValueGenerateMailNonce() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ValuePolicyType pp = parsePasswordPolicy("value-policy-generate-without-limit-with-unique.xml");

        when();
        String mailNonce = valuePolicyProcessor.generate(
                SchemaConstants.PATH_PASSWORD_VALUE, pp, 24, null,
                getTestNameShort(), task, result);

        then();
        displayValue("Generated password", mailNonce);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertNotNull(mailNonce);
        assertPassword(mailNonce, pp);
    }

    @Test
    public void testValueGenerate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ValuePolicyType pp = parsePasswordPolicy("value-policy-generate.xml");

        // WHEN
        when();
        String psswd = valuePolicyProcessor.generate(SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, null, getTestNameShort(), task, result);

        // THEN
        then();
        displayValue("Generated password", psswd);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertNotNull(psswd);
        assertPassword(psswd, pp);

    }

    @Test
    public void testValueGenerateEmpty() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        File file = new File(TEST_DIR, "value-policy-generate-empty.xml");
        logger.info("Positive testing {}: {}", "testValueGenerate", "value-policy-generate-empty.xml");
        ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();

        // WHEN
        when();
        String psswd = valuePolicyProcessor.generate(
                SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, null, getTestNameShort(), task, result);

        // THEN
        then();
        displayValue("Generated password", psswd);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertNotNull(psswd);
        assertPassword(psswd, pp);
    }

    public void passwordGeneratorTest(String policyFilename)
            throws SchemaException, IOException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        File file = new File(TEST_DIR, policyFilename);
        logger.info("Positive testing {}: {}", getTestNameShort(), policyFilename);
        ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();

        String psswd;
        // generate minimal size passwd
        for (int i = 0; i < 100; i++) {
            psswd = valuePolicyProcessor.generate(
                    SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, null, getTestNameShort(), task, result);
            logger.info("Generated password:" + psswd);
            result.computeStatus();
            if (!result.isSuccess()) {
                logger.info("Result:" + result.debugDump());
                AssertJUnit.fail("Password generator failed:\n" + result.debugDump());
            }
            assertNotNull(psswd);
            assertPassword(psswd, pp);
        }
        // genereata to meet as possible
        logger.info("-------------------------");
        // Generate up to possible
        for (int i = 0; i < 100; i++) {
            psswd = valuePolicyProcessor.generate(
                    SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, null, getTestNameShort(), task, result);
            logger.info("Generated password:" + psswd);
            result.computeStatus();
            if (!result.isSuccess()) {
                logger.info("Result:" + result.debugDump());
            }
            AssertJUnit.assertTrue(result.isSuccess());
            assertNotNull(psswd);

        }
    }

    private void assertPassword(String passwd, ValuePolicyType pp)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        assertPassword(passwd, pp, null);
    }

    private void assertPassword(String passwd, ValuePolicyType pp, PrismObject<UserType> object)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        valuePolicyProcessor.validateValue(passwd, pp, createUserOriginResolver(object), "assertPassword", task, result);
        boolean isValid = result.isAcceptable();
        result.computeStatus();
        if (!result.isSuccess()) {
            AssertJUnit.fail(result.debugDump());
        }
        AssertJUnit.assertTrue("Password not valid (but result is success)", isValid);
    }

    @Test
    public void passwordValidationTestComplex() throws Exception {
        ValuePolicyType pp = parsePasswordPolicy("password-policy-complex.xml");

        // WHEN, THEN
        AssertJUnit.assertTrue(pwdValidHelper("582a**A", pp));
        AssertJUnit.assertFalse(pwdValidHelper("58", pp));
        AssertJUnit.assertFalse(pwdValidHelper("333a**aGaa", pp));
        AssertJUnit.assertFalse(pwdValidHelper("AAA4444", pp));
    }

    @Test
    public void passwordValidationTestTri() throws Exception {
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
        PrismObject<UserType> user = createUserAb();
        ValuePolicyType pp = parsePasswordPolicy("password-policy-username.xml");

        // WHEN
        when();

        for (int i = 0; i < USERNAME_ATTEMPTS; i++) {
            Task task = getTestTask();
            OperationResult result = task.getResult();

            String psswd = valuePolicyProcessor.generate(
                    SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, createUserOriginResolver(user), getTestNameShort(), task, result);
            displayValue("Generated password (" + i + ")", psswd);

            result.computeStatus();
            TestUtil.assertSuccess(result);
            assertNotNull(psswd);
            assertPassword(psswd, pp, user);

            assertFalse("Generated password that matches the username: " + psswd, psswd.equals(USER_AB_USERNAME));
        }

        // THEN
        then();
    }

    /**
     * MID-1657
     */
    @Test
    public void testUserProps() throws Exception {
        PrismObject<UserType> user = createUserAb();
        display("User", user);
        ValuePolicyType pp = parsePasswordPolicy("password-policy-props.xml");

        // WHEN
        when();

        for (int i = 0; i < USER_PROPS_ATTEMPTS; i++) {
            Task task = getTestTask();
            OperationResult result = task.getResult();

            String psswd = valuePolicyProcessor.generate(
                    SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, createUserOriginResolver(user), getTestNameShort(), task, result);
            displayValue("Generated password (" + i + ")", psswd);

            result.computeStatus();
            TestUtil.assertSuccess(result);
            assertNotNull(psswd);
            assertPassword(psswd, pp, user);

            assertNotContains(psswd, USER_AB_USERNAME);
            assertNotContains(psswd, USER_AB_GIVEN_NAME);
            assertNotContains(psswd, USER_AB_ADDITIONAL_NAME);
        }

        // THEN
        then();
    }

    private PrismObject<UserType> createUserAb() throws SchemaException {
        PrismObject<UserType> user = createUser(USER_AB_USERNAME, USER_AB_GIVEN_NAME, USER_AB_FAMILY_NAME, true);
        user.asObjectable().setAdditionalName(createPolyStringType(USER_AB_ADDITIONAL_NAME));
        return user;
    }

    private void assertNotContains(String psswd, String val) {
        assertFalse("Generated password " + psswd + " contains value " + val, StringUtils.containsIgnoreCase(psswd, val));
    }

    private ValuePolicyType parsePasswordPolicy(String filename) throws SchemaException, IOException {
        File file = new File(TEST_DIR, filename);
        return (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
    }

    private boolean pwdValidHelper(String password, ValuePolicyType pp) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        valuePolicyProcessor.validateValue(password, pp, null, "pwdValidHelper", task, result);
        result.computeStatus();
        String msg = "-> Policy " + pp.getName() + ", password '" + password + "': " + result.getStatus();
        System.out.println(msg);
        logger.info(msg);
        logger.trace(result.debugDump());
        return (result.isSuccess());
    }
}
