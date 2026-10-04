/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.script.ScriptFactory;
import com.evolveum.midpoint.model.common.expression.script.TestingExpressionConfiguration;
import com.evolveum.midpoint.model.common.expression.script.mel.MelScriptExecutor;
import com.evolveum.midpoint.model.common.stringpolicy.StringPolicy;

import com.evolveum.midpoint.model.common.stringpolicy.StringPolicy.CharacterClassLimitation;
import com.evolveum.midpoint.schema.config.ConfigurationItem;

import com.evolveum.midpoint.schema.expression.TrustDescriptorSetter;

import com.evolveum.midpoint.test.IntegrationTestTools;

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

/** Tests generating and validating passwords (or values in general) using a value policy. */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPasswordPolicy extends AbstractInternalModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/lens/ppolicy/");

    private static final String DEFAULT_PASSWORD_POLICY_RESOURCE =
            "/initial-objects/value-policy/010-value-policy.xml";

    private static final String USER_AB_USERNAME = "ab";
    private static final String USER_AB_GIVEN_NAME = "Ad";
    private static final String USER_AB_FAMILY_NAME = "Fel";
    private static final String USER_AB_ADDITIONAL_NAME = "x";
    private static final int USERNAME_ATTEMPTS = 200;
    private static final int USER_PROPS_ATTEMPTS = 5000;

    @Autowired private ValuePolicyProcessor valuePolicyProcessor;
    @Autowired private ScriptFactory scriptFactory;
    @Autowired private MelScriptExecutor melScriptExecutor;
    @Autowired private BasicExpressionFunctions basicExpressionFunctions;

    /** Tests parsing the minimal (empty) value policy. */
    @Test
    public void stringPolicyUtilsMinimalTest() throws SchemaException, IOException, ConfigurationException {
        File file = new File(TEST_DIR, "password-policy-minimal.xml");
        ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
        StringPolicy sp = StringPolicy.compile(ConfigurationItem.embedded(pp.getStringPolicy()));
        assertThat(sp.getCharacterClassLimitations()).isEmpty();
    }

    /** Tests parsing more complex value policy. */
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

    /** Tests generating a password, first with consistent requirements, then with inconsistent ones. */
    @Test
    public void testPasswordGeneratorComplexNegative() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        File file = new File(TEST_DIR, "password-policy-complex.xml");
        ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();

        pp.getStringPolicy().getLimitations().setMinLength(2);
        pp.getStringPolicy().getLimitations().setMinUniqueChars(5);

        // WHEN
        when();
        String password = valuePolicyProcessor.generate(
                SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, null,
                getTestNameShort(), task, result);

        // THEN
        then();
        displayValue("Generated password", password);
        result.computeStatus();
        AssertJUnit.assertTrue(result.isAcceptable());
        assertNotNull(password);

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
        generateValidateRepeatedly("password-policy-complex.xml");
    }

    @Test
    public void testPasswordGeneratorLong() throws Exception {
        generateValidateRepeatedly("password-policy-long.xml");
    }

    @Test
    public void testPasswordGeneratorNumeric() throws Exception {
        generateValidateRepeatedly("password-policy-numeric.xml");
    }

    @Test
    public void testPasswordGeneratorNumericAcceptingAlphas() throws Exception {
        String policyFileName = "password-policy-gen-numeric-accepting-alphas.xml";

        // Check that we never generate alphabetic chars
        generateValidateRepeatedly(
                policyFileName,
                s -> {
                    try {
                        Long.parseLong(s);
                    } catch (NumberFormatException e) {
                        throw new AssertionError("Generated password is not numeric: " + s);
                    }
                });

        // Check that we accept them
        var pp = parsePasswordPolicy(policyFileName);

        assertThat(isPasswordValid("1234567890", pp)).isTrue();
        assertThat(isPasswordValid("abcdefghij", pp)).isTrue();
        assertThat(isPasswordValid("abcdefghij#", pp)).isFalse(); // extra char
        assertThat(isPasswordValid("1234567890123", pp)).isFalse(); // too many chars
        assertThat(isPasswordValid("abcdefghijklm", pp)).isFalse(); // too many chars
        assertThat(isPasswordValid("1234567", pp)).isFalse(); // too little chars
        assertThat(isPasswordValid("abcdefg", pp)).isFalse(); // too little chars
    }

    /** Using invalid policy: there's only one character class, and it's marked as ignored for generation. */
    @Test
    public void testPasswordGeneratorWithAllLimitationsIgnoredForGeneration() throws Exception {
        String policyFileName = "password-policy-all-ignored-for-generation.xml";

        try {
            generateValidateOnce(policyFileName, 10);
        } catch (ExpressionEvaluationException e) {
            assertExpectedException(e)
                    .hasMessageContaining("all character classes are marked as ignored");
        }
    }

    /** Using invalid policy: a required character class is ignored when generating. */
    @Test
    public void testPasswordGeneratorRequiredCharIgnoredForGeneration() throws Exception {
        String policyFileName = "password-policy-required-char-ignored-for-generation.xml";

        try {
            generateValidateOnce(policyFileName, 10);
        } catch (ExpressionEvaluationException e) {
            assertExpectedException(e)
                    .hasMessageContaining(
                            "Character class is marked as ignored for generation, but has non-zero min occurrences");
        }
    }

    @Test
    public void testValueGeneratorMustBeFirst() throws Exception {
        generateValidateRepeatedly("value-policy-must-be-first.xml");
    }

    @Test
    public void testValueGenerateRandomPin() throws Exception {
        generateValidateOnce("value-policy-random-pin.xml", 10);
    }

    @Test
    public void testValueGenerateMailNonce() throws Exception {
        generateValidateOnce("value-policy-generate-without-limit-with-unique.xml", 24);
    }

    @Test
    public void testValueGenerate() throws Exception {
        generateValidateOnce("value-policy-generate.xml", 10);
    }

    @Test
    public void testValueGenerateEmpty() throws Exception {
        generateValidateOnce("value-policy-generate-empty.xml", 10);
    }

    /** Runs 100 rounds of password generation + password validation. */
    private void generateValidateRepeatedly(String policyFilename) throws CommonException, IOException {
        generateValidateRepeatedly(policyFilename, s -> {});
    }

    /** Runs 100 rounds of password generation + password validation. */
    private void generateValidateRepeatedly(String policyFilename, Consumer<String> extraChecker)
            throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        File file = new File(TEST_DIR, policyFilename);
        logger.info("Positive testing {}: {}", getTestNameShort(), policyFilename);
        ValuePolicyType pp = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();

        for (int i = 0; i < 100; i++) {
            String password = valuePolicyProcessor.generate(
                    SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, null, getTestNameShort(), task, result);
            logger.info("Generated password: {}", password);
            result.computeStatus();
            if (!result.isSuccess()) {
                logger.info("Result: {}", result.debugDump());
                AssertJUnit.fail("Password generator failed:\n" + result.debugDump());
            }
            assertNotNull(password);
            assertPasswordValid(password, pp);
            extraChecker.accept(password);
        }
    }

    /** Just generates and validates a password (once). */
    private void generateValidateOnce(String filename, int defaultLength) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ValuePolicyType pp = parsePasswordPolicy(filename);

        when();
        String password = valuePolicyProcessor.generate(
                SchemaConstants.PATH_PASSWORD_VALUE, pp, defaultLength, null,
                getTestNameShort(), task, result);

        then();
        displayValue("Generated password", password);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertNotNull(password);
        assertPasswordValid(password, pp);
    }

    private void assertPasswordValid(String passwd, ValuePolicyType pp)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException, SubscriptionComplianceException {
        assertPasswordValid(passwd, pp, null);
    }

    private void assertPasswordValid(String passwd, ValuePolicyType pp, PrismObject<UserType> object)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException, SubscriptionComplianceException {
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

    /** Just validating passwords according to a policy. */
    @Test
    public void passwordValidationTestComplex() throws Exception {
        ValuePolicyType pp = parsePasswordPolicy("password-policy-complex.xml");

        // WHEN, THEN
        AssertJUnit.assertTrue(isPasswordValid("582a**A", pp));
        AssertJUnit.assertFalse(isPasswordValid("58", pp));
        AssertJUnit.assertFalse(isPasswordValid("333a**aGaa", pp));
        AssertJUnit.assertFalse(isPasswordValid("AAA4444", pp));
    }

    /** Just validating passwords according to a policy. */
    @Test
    public void passwordValidationTestTri() throws Exception {
        ValuePolicyType pp = parsePasswordPolicy("password-policy-tri.xml");

        // WHEN, THEN
        AssertJUnit.assertTrue(isPasswordValid("Password1", pp));
        AssertJUnit.assertFalse(isPasswordValid("password1", pp)); // no capital letter
        AssertJUnit.assertFalse(isPasswordValid("1PASSWORD", pp)); // no lowercase letter
        AssertJUnit.assertFalse(isPasswordValid("Password", pp)); // no numeral
        AssertJUnit.assertFalse(isPasswordValid("Pa1", pp)); // too short
        AssertJUnit.assertFalse(isPasswordValid("PPPPPPPPPPPPPPPPPPPPPPPaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa11111111111111111111111111111111111111111111111111111111111111111111", pp)); // too long
    }

    /**
     * Checks that generated password is never equal to the username (as the policy prohibits it).
     *
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

            String password = valuePolicyProcessor.generate(
                    SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, createUserOriginResolver(user), getTestNameShort(), task, result);
            displayValue("Generated password (" + i + ")", password);

            result.computeStatus();
            TestUtil.assertSuccess(result);
            assertNotNull(password);
            assertPasswordValid(password, pp, user);

            //noinspection SimplifiableAssertion
            assertFalse("Generated password that matches the username: " + password, password.equals(USER_AB_USERNAME));
        }

        // THEN
        then();
    }

    /**
     * Checks that generated password does not contain selected user properties (as the policy prohibits it).
     *
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

            String password = valuePolicyProcessor.generate(
                    SchemaConstants.PATH_PASSWORD_VALUE, pp, 10, createUserOriginResolver(user), getTestNameShort(), task, result);
            displayValue("Generated password (" + i + ")", password);

            result.computeStatus();
            TestUtil.assertSuccess(result);
            assertNotNull(password);
            assertPasswordValid(password, pp, user);

            assertNotContains(password, USER_AB_USERNAME);
            assertNotContains(password, USER_AB_GIVEN_NAME);
            assertNotContains(password, USER_AB_ADDITIONAL_NAME);
        }

        // THEN
        then();
    }

    /** The shipped default password policy must retain its semantics in safe-scripting-languages mode. See bug MID-12370 */
    @Test
    public void testDefaultPasswordPolicyMelInSafeScriptingLanguagesMode() throws Exception {
        ValuePolicyType policy = parseDefaultPasswordPolicy();
        PrismObject<UserType> user = createUserAb();

        var restrictedMelExecutor = new MelScriptExecutor(
                prismContext,
                protector,
                localizationService,
                new TestingExpressionConfiguration(true, java.util.List.of()),
                basicExpressionFunctions,
                libraryMidpointFunctions,
                null);

        scriptFactory.replaceExecutor(restrictedMelExecutor);
        try {
            assertThat(isPasswordValid("Goodab123!", policy, user)).as("contains username").isFalse();
            assertThat(isPasswordValid("GoodAB123!", policy, user)).as("contains mixed-case username").isFalse();
            assertThat(isPasswordValid("GoodAD123!", policy, user)).as("contains given name").isFalse();
            assertThat(isPasswordValid("GoodFEL123!", policy, user)).as("contains family name").isFalse();
            assertThat(isPasswordValid("GoodX123!", policy, user)).as("contains additional name").isFalse();
            assertThat(isPasswordValid("GoodSafe123!", policy, user)).as("unrelated compliant password").isTrue();

            user.asObjectable().setGivenName(null);
            user.asObjectable().setFamilyName(null);
            user.asObjectable().setAdditionalName(null);
            assertThat(isPasswordValid("GoodSafe123!", policy, user))
                    .as("password with null optional personal names")
                    .isTrue();
        } finally {
            scriptFactory.replaceExecutor(melScriptExecutor);
        }
    }

    private PrismObject<UserType> createUserAb() throws SchemaException {
        PrismObject<UserType> user = createUser(USER_AB_USERNAME, USER_AB_GIVEN_NAME, USER_AB_FAMILY_NAME, true);
        user.asObjectable().setAdditionalName(createPolyStringType(USER_AB_ADDITIONAL_NAME));
        return user;
    }

    private void assertNotContains(String password, String val) {
        assertFalse("Generated password " + password + " contains value " + val,
                StringUtils.containsIgnoreCase(password, val));
    }

    private ValuePolicyType parsePasswordPolicy(String filename) throws SchemaException, IOException {
        File file = new File(TEST_DIR, filename);
        var object = (ValuePolicyType) PrismTestUtil.parseObject(file).asObjectable();
        TrustDescriptorSetter.setDescriptors(object, IntegrationTestTools.trustedForTests());
        return object;
    }

    private ValuePolicyType parseDefaultPasswordPolicy() throws SchemaException, IOException {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getResourceAsStream(DEFAULT_PASSWORD_POLICY_RESOURCE),
                DEFAULT_PASSWORD_POLICY_RESOURCE)) {
            var object = prismContext.parserFor(stream).xml().<ValuePolicyType>parse().asObjectable();
            TrustDescriptorSetter.setDescriptors(object, IntegrationTestTools.trustedForTests());
            return object;
        }
    }

    private boolean isPasswordValid(String password, ValuePolicyType pp) throws CommonException {
        return isPasswordValid(password, pp, null, getTestTask());
    }

    private boolean isPasswordValid(String password, ValuePolicyType pp, PrismObject<UserType> object) throws CommonException {
        return isPasswordValid(password, pp, object, createPlainTask("default password policy validation"));
    }

    private boolean isPasswordValid(String password, ValuePolicyType pp, PrismObject<UserType> object, Task task) throws CommonException {
        OperationResult result = task.getResult();
        valuePolicyProcessor.validateValue(
                password, pp, createUserOriginResolver(object), "pwdValidHelper", task, result);
        result.computeStatus();
        String msg = "-> Policy " + pp.getName() + ", password '" + password + "': " + result.getStatus();
        System.out.println(msg);
        logger.info(msg);
        logger.trace(result.debugDump());
        return (result.isSuccess());
    }
}
