/*
 * Copyright (C) 2013-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.common.AbstractModelCommonTest;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluatorFactory;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfiles;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.prism.PrismValueDeltaSetTripleAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author Radovan Semancik
 */
public class TestExpression extends AbstractModelCommonTest {

    protected static final File TEST_DIR = new File("src/test/resources/expression/expression");

    protected static final File USER_JACK_FILE = new File(TEST_DIR, "user-jack.xml");
    protected static final String USER_JACK_NAME = "jack";

    protected static final File ACCOUNT_JACK_DUMMY_FILE = new File(TEST_DIR, "account-jack-dummy.xml");

    protected static final File EXPRESSION_ASIS_FILE = new File(TEST_DIR, "expression-asis.xml");

    protected static final File EXPRESSION_PATH_FILE = new File(TEST_DIR, "expression-path.xml");
    protected static final File EXPRESSION_ITERATION_CONDITION_FILE = new File(TEST_DIR, "expression-iteration-condition.xml");

    protected static final File EXPRESSION_VALUE_FILE = new File(TEST_DIR, "expression-value.xml");
    protected static final String EXPRESSION_VALUE_OUTPUT = "GARBAGE OUT";

    protected static final File EXPRESSION_CONST_FILE = new File(TEST_DIR, "expression-const.xml");

    protected static final File EXPRESSION_SCRIPT_GROOVY_SIMPLE_FILE = new File(TEST_DIR, "expression-script-groovy-simple.xml");
    protected static final File EXPRESSION_SCRIPT_GROOVY_SYSTEM_ALLOW_FILE = new File(TEST_DIR, "expression-script-groovy-system-allow.xml");
    protected static final File EXPRESSION_SCRIPT_GROOVY_SYSTEM_DENY_FILE = new File(TEST_DIR, "expression-script-groovy-system-deny.xml");
    protected static final File EXPRESSION_SCRIPT_GROOVY_REF_FILE = new File(TEST_DIR, "expression-script-groovy-ref.xml");
    protected static final File EXPRESSION_SCRIPT_JAVASCRIPT_FILE = new File(TEST_DIR, "expression-script-javascript.xml");

    protected static final String VAR_FOO_NAME = "foo";
    protected static final String VAR_FOO_VALUE = "F00";
    protected static final String VAR_BAR_NAME = "bar";
    protected static final String VAR_BAR_VALUE = "B4R";
    protected static final String VAR_FOO_REF_NAME = "fooRef";
    protected static final String VAR_FOO_REF_VALUE = "0d3dd30e-260a-41be-af34-d9ca50a2be79";
    protected static final String INPUT_VALUE = "garbage in";
    protected static final String GROOVY_SCRIPT_OUTPUT_SUCCESS = "SUCCESS";

    private static final Trace logger = TraceManager.getTrace(TestExpression.class);

    protected PrismContext prismContext;
    private ExpressionFactory expressionFactory;

    // Default "null" expression profile, no restrictions.
    private ExpressionProfile expressionProfile = null;

    private long lastScriptExecutionCount;

    @BeforeClass
    public void setup() throws SchemaException, SAXException, IOException, ConfigurationException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);

        ModelCommonBeans beans = ExpressionTestUtil.initializeModelCommonBeans();
        prismContext = beans.prismContext;
        expressionFactory = beans.expressionFactory;
        expressionProfile = compileExpressionProfile(getExpressionProfileName());
        System.out.println("Using expression profile: " + expressionProfile);
        logger.info("EXPRESSION PROFILE: {}", expressionProfile);
    }

    @Test
    public void test100AsIs() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_ASIS_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        VariablesMap variables = prepareBasicVariables();
        ExpressionEvaluationContext expressionContext =
                new ExpressionEvaluationContext(sources, variables, getTestNameShort(), createTask());

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                evaluatePropertyExpression(
                        expressionType, PrimitiveType.STRING, expressionContext, result);

        // THEN
        assertOutputTriple(outputTriple)
                .assertEmptyMinus()
                .assertEmptyPlus()
                .zeroSet()
                .assertSinglePropertyValue(INPUT_VALUE);

        assertScriptExecutionIncrement(0);
    }

    @Test
    public void test110Path() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_PATH_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        VariablesMap variables = prepareBasicVariables();
        ExpressionEvaluationContext expressionContext =
                new ExpressionEvaluationContext(sources, variables, getTestNameShort(), createTask());

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                evaluatePropertyExpression(expressionType, PrimitiveType.STRING, expressionContext, result);

        // THEN
        assertOutputTriple(outputTriple)
                .assertEmptyMinus()
                .assertEmptyPlus()
                .zeroSet()
                .assertSinglePropertyValue(USER_JACK_NAME);

        assertScriptExecutionIncrement(0);
    }

    @Test
    public void test120Value() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_VALUE_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        VariablesMap variables = prepareBasicVariables();
        ExpressionEvaluationContext expressionContext =
                new ExpressionEvaluationContext(sources, variables, getTestNameShort(), createTask());

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                evaluatePropertyExpression(expressionType, PrimitiveType.STRING, expressionContext, result);

        // THEN
        assertOutputTriple(outputTriple)
                .assertEmptyMinus()
                .assertEmptyPlus()
                .zeroSet()
                .assertSinglePropertyValue(EXPRESSION_VALUE_OUTPUT);

        assertScriptExecutionIncrement(0);
    }

    @Test
    public void test130Const() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_CONST_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        VariablesMap variables = prepareBasicVariables();
        ExpressionEvaluationContext expressionContext =
                new ExpressionEvaluationContext(sources, variables, getTestNameShort(), createTask());

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                evaluatePropertyExpression(expressionType, PrimitiveType.STRING, expressionContext, result);

        // THEN
        assertOutputTriple(outputTriple)
                .assertEmptyMinus()
                .assertEmptyPlus()
                .zeroSet()
                .assertSinglePropertyValue(ExpressionTestUtil.CONST_FOO_VALUE);

        assertScriptExecutionIncrement(0);
    }

    @Test
    public void test150ScriptGroovySimple() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_SCRIPT_GROOVY_SIMPLE_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        VariablesMap variables = prepareBasicVariables();
        var expressionContext = new ExpressionEvaluationContext(sources, variables, getTestNameShort(), createTask());

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                evaluatePropertyExpression(expressionType, PrimitiveType.STRING, expressionContext, result);

        // THEN
        assertOutputTriple(outputTriple)
                .assertEmptyMinus()
                .assertEmptyPlus()
                .zeroSet()
                .assertSinglePropertyValue(VAR_FOO_VALUE + VAR_BAR_VALUE);

        assertScriptExecutionIncrement(1);
    }

    @Test
    public void test152ScriptGroovySystemAllow() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_SCRIPT_GROOVY_SYSTEM_ALLOW_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        VariablesMap variables = prepareBasicVariables();
        var expressionContext = new ExpressionEvaluationContext(sources, variables, getTestNameShort(), createTask());

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                evaluatePropertyExpression(expressionType, PrimitiveType.STRING, expressionContext, result);

        // THEN
        assertOutputTriple(outputTriple)
                .assertEmptyMinus()
                .assertEmptyPlus()
                .zeroSet()
                .assertSinglePropertyValue(GROOVY_SCRIPT_OUTPUT_SUCCESS);

        assertScriptExecutionIncrement(1);
    }

    @Test
    public void test154ScriptGroovySystemDeny() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_SCRIPT_GROOVY_SYSTEM_DENY_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        VariablesMap variables = prepareBasicVariables();
        var expressionContext = new ExpressionEvaluationContext(sources, variables, getTestNameShort(), createTask());

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                evaluatePropertyExpression(expressionType, PrimitiveType.STRING, expressionContext, result);

        // THEN
        assertOutputTriple(outputTriple)
                .assertEmptyMinus()
                .assertEmptyPlus()
                .zeroSet()
                .assertSinglePropertyValue(GROOVY_SCRIPT_OUTPUT_SUCCESS);

        assertScriptExecutionIncrement(1);
    }

    /** Checks that reference values are represented as {@link ObjectReferenceType}s in scripts. MID-10130 */
    @Test
    public void test156ScriptGroovyRef() throws Exception {

        skipTestIf(getExpressionProfile() != null, "Test is not profile-safe (.class call is restricted)");

        // GIVEN
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_SCRIPT_GROOVY_REF_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        VariablesMap variables = prepareBasicVariables();
        var expressionContext = new ExpressionEvaluationContext(sources, variables, getTestNameShort(), createTask());

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                evaluatePropertyExpression(expressionType, PrimitiveType.STRING, expressionContext, result);

        // THEN
        assertOutputTriple(outputTriple)
                .assertEmptyMinus()
                .assertEmptyPlus()
                .zeroSet()
                .assertSinglePropertyValue(ObjectReferenceType.class.getSimpleName());

        assertScriptExecutionIncrement(1);
    }

    @Test
    public void test160ScriptJavaScript() throws Exception {
        skipIfEcmaScriptEngineNotSupported();
        given();
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_SCRIPT_JAVASCRIPT_FILE);
        Collection<Source<?, ?>> sources = prepareStringSources();
        VariablesMap variables = prepareBasicVariables();
        var expressionContext = new ExpressionEvaluationContext(sources, variables, getTestNameShort(), createTask());

        when();
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                evaluatePropertyExpression(expressionType, PrimitiveType.STRING, expressionContext, result);

        then();
        assertOutputTriple(outputTriple)
                .assertEmptyMinus()
                .assertEmptyPlus()
                .zeroSet()
                .assertSinglePropertyValue(VAR_FOO_VALUE + VAR_BAR_VALUE);

        assertScriptExecutionIncrement(1);
    }

    @Test
    public void test200IterationCondition() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        rememberScriptExecutionCount();

        ExpressionType expressionType = parseExpression(EXPRESSION_ITERATION_CONDITION_FILE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        // We need to provide some definitions for account attributes here,
        // otherwise the tests will fail on unknown data types
        PrismObjectDefinition<ShadowType> shadowDef = account.getDefinition().deepClone(DeepCloneOperation.notUltraDeep());
        PrismContainerDefinition<Containerable> attrsDef = shadowDef.findContainerDefinition(ShadowType.F_ATTRIBUTES);
        attrsDef.toMutable().createPropertyDefinition(new QName(MidPointConstants.NS_RI, "quote"), PrimitiveType.STRING.getQname());
        account.setDefinition(shadowDef);
        IntegrationTestTools.display("Account", account);

        VariablesMap variables = prepareBasicVariables();
        variables.put(ExpressionConstants.VAR_PROJECTION, account, shadowDef);
        variables.put(ExpressionConstants.VAR_ITERATION, 1,
                TestUtil.createPrimitivePropertyDefinition(prismContext, ExpressionConstants.VAR_ITERATION, PrimitiveType.INT));
        variables.put(ExpressionConstants.VAR_ITERATION_TOKEN, "001",
                TestUtil.createPrimitivePropertyDefinition(prismContext, ExpressionConstants.VAR_ITERATION_TOKEN, PrimitiveType.STRING));

        var expressionContext = new ExpressionEvaluationContext(null, variables, getTestNameShort(), createTask());

        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple =
                evaluatePropertyExpression(expressionType, PrimitiveType.BOOLEAN, expressionContext, result);

        // THEN
        assertOutputTriple(outputTriple)
                .assertEmptyMinus()
                .assertEmptyPlus()
                .zeroSet()
                .assertSinglePropertyValue(Boolean.TRUE);

        // Make sure that the script is executed only once. There is no delta in the variables, no need to do it twice.
        assertScriptExecutionIncrement(1);
    }

    protected ExpressionType parseExpression(File file) throws SchemaException, IOException {
        return PrismTestUtil.parseAtomicValue(file, ExpressionType.COMPLEX_TYPE);
    }

    protected Source<PrismPropertyValue<String>, PrismPropertyDefinition<String>> prepareStringSource() throws SchemaException {
        PrismPropertyDefinition<String> propDef = prismContext.definitionFactory()
                .createPropertyDefinition(ExpressionConstants.VAR_INPUT_QNAME, PrimitiveType.STRING.getQname());
        PrismProperty<String> inputProp = prismContext.itemFactory().createProperty(ExpressionConstants.VAR_INPUT_QNAME, propDef);
        PrismPropertyValue<String> pval = prismContext.itemFactory().createPropertyValue(INPUT_VALUE);
        inputProp.add(pval);
        return new Source<>(inputProp, null, inputProp, ExpressionConstants.VAR_INPUT_QNAME, propDef);
    }

    protected Collection<Source<?, ?>> prepareStringSources() throws SchemaException {
        Collection<Source<?, ?>> sources = new ArrayList<>();
        sources.add(prepareStringSource());
        return sources;
    }

    protected VariablesMap prepareBasicVariables() throws SchemaException, IOException {
        VariablesMap variables = new VariablesMap();
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);
        variables.put(ExpressionConstants.VAR_FOCUS, user, user.getDefinition());
        variables.put(VAR_FOO_NAME, VAR_FOO_VALUE, String.class);
        variables.put(VAR_BAR_NAME, VAR_BAR_VALUE, String.class);

        var fooRefValue = prismContext.itemFactory().createReferenceValue(VAR_FOO_REF_VALUE, OrgType.COMPLEX_TYPE);
        variables.put(VAR_FOO_REF_NAME, fooRefValue, Referencable.class);

        return variables;
    }

    protected <V extends PrismValue, D extends ItemDefinition<?>> PrismValueDeltaSetTriple<V> evaluateExpression(
            ExpressionType expressionType, D outputDefinition, ExpressionEvaluationContext expressionContext,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        Expression<V, D> expression = expressionFactory.makeExpression(expressionType, outputDefinition, getExpressionProfile(),
                expressionContext.getContextDescription(), expressionContext.getTask(), result);
        logger.debug("Starting evaluation of expression: {}", expression);
        return expression.evaluate(expressionContext, result);
    }

    protected <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluatePropertyExpression(
            ExpressionType expressionType, QName outputType,
            ExpressionEvaluationContext expressionContext, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        PrismPropertyDefinition<T> outputDefinition = prismContext.definitionFactory().createPropertyDefinition(
                ExpressionConstants.OUTPUT_ELEMENT_NAME, outputType);
        return evaluateExpression(expressionType, outputDefinition, expressionContext, result);
    }

    protected <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluatePropertyExpression(
            ExpressionType expressionType, PrimitiveType outputType,
            ExpressionEvaluationContext expressionContext, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        return evaluatePropertyExpression(expressionType, outputType.getQname(), expressionContext, result);
    }

    protected <V extends PrismValue, D extends ItemDefinition<?>> void evaluateExpressionRestricted(
            ExpressionType expressionType, D outputDefinition,
            ExpressionEvaluationContext expressionContext, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        Expression<V, D> expression;
        try {

            expression = expressionFactory.makeExpression(expressionType, outputDefinition, getExpressionProfile(),
                    expressionContext.getContextDescription(), expressionContext.getTask(), result);

        } catch (SecurityViolationException e) {
            displayExpectedException(e);
            assertTrue("Wrong exception message: " + e.getMessage(), e.getMessage().contains("Access to script language"));
            return;
        }

        logger.debug("Starting evaluation of expression (expecting security violation): {}", expression);
        try {
            expression.evaluate(expressionContext, result);

            AssertJUnit.fail("Unexpected success of expression evaluation");
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
            assertTrue("Wrong exception message: " + e.getMessage(),
                    e.getMessage().contains("Access to expression evaluator")
                            || e.getMessage().contains("Access to Groovy method"));
        }
    }

    protected <T> void evaluatePropertyExpressionRestricted(ExpressionType expressionType,
            QName outputType, ExpressionEvaluationContext expressionContext, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        PrismPropertyDefinition<T> outputDefinition = prismContext.definitionFactory().createPropertyDefinition(
                ExpressionConstants.OUTPUT_ELEMENT_NAME, outputType);
        evaluateExpressionRestricted(expressionType, outputDefinition, expressionContext, result);
    }

    protected void evaluatePropertyExpressionRestricted(
            ExpressionType expressionType, PrimitiveType outputType,
            ExpressionEvaluationContext expressionContext, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        evaluatePropertyExpressionRestricted(expressionType, outputType.getQname(), expressionContext, result);
    }

    protected void rememberScriptExecutionCount() {
        lastScriptExecutionCount = InternalMonitor.getCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
    }

    protected void assertScriptExecutionIncrement(int expectedIncrement) {
        long currentScriptExecutionCount = InternalMonitor.getCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
        long actualIncrement = currentScriptExecutionCount - lastScriptExecutionCount;
        assertEquals("Unexpected increment in script execution count",
                expectedIncrement, actualIncrement);
        lastScriptExecutionCount = currentScriptExecutionCount;
    }

    protected ExpressionProfile getExpressionProfile() {
        return expressionProfile;
    }

    protected String getExpressionProfileName() {
        // Default "null" expression profile, no restrictions.
        return null;
    }

    private ExpressionProfile compileExpressionProfile(String profileName)
            throws SchemaException, IOException, ConfigurationException {
        if (profileName == null) {
            return null;
        }
        PrismObject<SystemConfigurationType> systemConfig = PrismTestUtil.parseObject(getSystemConfigurationFile());
        SystemConfigurationExpressionsType expressions = systemConfig.asObjectable().getExpressions();
        if (expressions == null) {
            throw new SchemaException("No expressions in system config");
        }
        ExpressionProfileCompiler compiler = new ExpressionProfileCompiler();
        ExpressionProfiles profiles = compiler.compile(expressions);
        return profiles.getProfile(profileName);
    }

    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    protected <V extends PrismValue> PrismValueDeltaSetTripleAsserter<V, Void> assertOutputTriple(PrismValueDeltaSetTriple<V> triple) {
        assertNotNull(triple);
        triple.checkConsistence();
        PrismValueDeltaSetTripleAsserter<V, Void> asserter = new PrismValueDeltaSetTripleAsserter<>(triple, "expression output triple");
        asserter.setPrismContext(prismContext);
        asserter.display();
        return asserter;
    }

    /**
     * Newer JDK is not shipped with JavaScript/ECMAScript engine, we want to skip related tests.
     */
    void skipIfEcmaScriptEngineNotSupported() {
        ScriptExpressionEvaluatorFactory evaluatorFactory = (ScriptExpressionEvaluatorFactory)
                expressionFactory.getEvaluatorFactory(SchemaConstantsGenerated.C_SCRIPT);
        if (evaluatorFactory.getScriptExpressionFactory().getEvaluatorSimple(
                "http://midpoint.evolveum.com/xml/ns/public/expression/language#ECMAScript") == null) {
            display("Script engine for ECMAScript missing, skipping the tests.");
            throw new SkipException("Script engine not available");
        }
    }
}
