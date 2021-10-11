/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.LocalizationTestUtil;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.KeyStoreBasedProtectorBuilder;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.DirectoryFileObjectResolver;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ScriptExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.test.util.ParallelTestThread;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class AbstractScriptTest extends AbstractUnitTest
        implements InfraTestMixin {

    protected static final QName PROPERTY_NAME = new QName(MidPointConstants.NS_MIDPOINT_TEST_PREFIX, "whatever");
    protected static final File BASE_TEST_DIR = new File("src/test/resources/expression");
    protected static final File OBJECTS_DIR = new File("src/test/resources/objects");
    protected static final String USER_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

    public static final String VAR_POISON = "poison";
    protected static final String RESULT_POISON_OK = "ALIVE";

    protected static final String RESULT_STRING_EXEC = "Hello world";

    protected PrismContext prismContext;
    protected ScriptExpressionFactory scriptExpressionfactory;
    protected ScriptEvaluator evaluator;
    protected LocalizationService localizationService;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @BeforeClass
    public void setupFactory() {
        prismContext = PrismTestUtil.getPrismContext();
        ObjectResolver resolver = new DirectoryFileObjectResolver(OBJECTS_DIR);
        Protector protector = KeyStoreBasedProtectorBuilder.create(prismContext).buildOnly();
        Clock clock = new Clock();
        Collection<FunctionLibrary> functions = new ArrayList<>();
        functions.add(FunctionLibraryUtil.createBasicFunctionLibrary(prismContext, protector, clock));
        scriptExpressionfactory = new ScriptExpressionFactory(prismContext, protector, null);
        scriptExpressionfactory.setObjectResolver(resolver);
        scriptExpressionfactory.setFunctions(functions);
        localizationService = LocalizationTestUtil.getLocalizationService();
        evaluator = createEvaluator(prismContext, protector);
        String languageUrl = evaluator.getLanguageUrl();
        display("Expression test for " + evaluator.getLanguageName() + ": registering " + evaluator + " with URL " + languageUrl);
        scriptExpressionfactory.registerEvaluator(languageUrl, evaluator);
    }

    protected abstract ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector);

    protected abstract File getTestDir();

    protected boolean supportsRootNode() {
        return false;
    }

    @Test
    public void testExpressionSimple() throws Exception {
        evaluateAndAssertStringScalarExpression("expression-simple.xml",
                "testExpressionSimple", null, "foobar");
    }

    @Test
    public void testExpressionStringVariables() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-string-variables.xml",
                "testExpressionStringVariables",
                createVariables(
                        "foo", "FOO", PrimitiveType.STRING,
                        "bar", "BAR", PrimitiveType.STRING
                ),
                "FOOBAR");
    }

    /**
     * Make sure that the script engine can work well in parallel and that
     * individual script runs do not influence each other.
     */
    @Test
    public void testExpressionStringVariablesParallel() throws Exception {
        // WHEN

        ParallelTestThread[] threads = TestUtil.multithread(
                (threadIndex) -> {

                    String foo = "FOO" + threadIndex;
                    String bar = "BAR" + threadIndex;

                    evaluateAndAssertStringScalarExpression(
                            "expression-string-variables.xml",
                            "testExpressionStringVariablesParallel-" + threadIndex,
                            createVariables(
                                    "foo", foo, PrimitiveType.STRING,
                                    "bar", bar, PrimitiveType.STRING
                            ),
                            foo + bar);

                }, 30, 3);

        // THEN
        TestUtil.waitForThreads(threads, 60000L);

    }

    @Test
    public void testExpressionObjectRefVariables() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-objectref-variables.xml",
                "testExpressionObjectRefVariables",
                createVariables(
                        "foo", "Captain", String.class,
                        "jack",
                        MiscSchemaUtil.createObjectReference(USER_OID, UserType.COMPLEX_TYPE),
                        // We want 'jack' variable to contain user object, not the reference. We want the reference resolved.
                        prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class)
                ),
                "Captain emp1234");
    }

    @Test
    public void testExpressionObjectRefVariablesPolyString() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-objectref-variables-polystring.xml",
                "testExpressionObjectRefVariablesPolyString",
                createVariables(
                        "foo", "Captain", PrimitiveType.STRING,
                        "jack",
                        MiscSchemaUtil.createObjectReference(USER_OID, UserType.COMPLEX_TYPE),
                        // We want 'jack' variable to contain user object, not the reference. We want the reference resolved.
                        prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class)
                ),
                "Captain Jack Sparrow");
    }

    // Using similar settings that will be used with mapping and SYSTEM VARIABLES

    @Test
    public void testUserGivenName() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-given-name.xml",
                "testUserGivenName",
                createUserScriptVariables(),
                "Jack");
    }

    @Test
    public void testUserExtensionShip() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-extension-ship.xml",
                "testUserExtensionShip",
                createUserScriptVariables(),
                "Black Pearl");
    }

    @Test
    public void testUserExtensionShipPath() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-extension-ship-path.xml",
                "testUserExtensionShipPath",
                createUserScriptVariables(),
                "Black Pearl");
    }

    @Test
    public void testUserExtensionStringifyFullName() throws Exception {
        evaluateAndAssertStringScalarExpression(
                "expression-user-stringify-full-name.xml",
                "testUserExtensionStringifyFullName",
                createUserScriptVariables(),
                "Jack Sparrow");
    }

    // TODO: user + multivalue (organizationalUnit)
    // TODO: user + polystring
    // TODO: user + numeric
    // TODO: user + no property value

    private ExpressionVariables createUserScriptVariables() {
        return createVariables(
                ExpressionConstants.VAR_USER,
                MiscSchemaUtil.createObjectReference(USER_OID, UserType.COMPLEX_TYPE),
                // We want 'user' variable to contain user object, not the reference. We want the reference resolved.
                prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class),
                ExpressionConstants.VAR_FOCUS,
                MiscSchemaUtil.createObjectReference(USER_OID, UserType.COMPLEX_TYPE),
                // We want 'user' variable to contain user object, not the reference. We want the reference resolved.
                prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class));
    }

    // TODO: shadow + attributes

    @Test
    public void testRootNode() throws Exception {
        if (!supportsRootNode()) {
            return;
        }

        evaluateAndAssertStringScalarExpression(
                "expression-root-node.xml",
                "testRootNode",
                createVariables(
                        null, // root node
                        MiscSchemaUtil.createObjectReference(USER_OID, UserType.COMPLEX_TYPE),
                        prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class)),
                "Black Pearl");
    }

    @Test
    public void testExpressionList() throws Exception {
        evaluateAndAssertStringListExpression(
                "expression-list.xml",
                "testExpressionList",
                createVariables(
                        "jack",
                        MiscSchemaUtil.createObjectReference(USER_OID, UserType.COMPLEX_TYPE),
                        // We want 'jack' variable to contain user object, not the reference. We want the reference resolved.
                        prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class)
                ),
                "Leaders", "Followers");
    }

    @Test
    public void testExpressionFunc() throws Exception {
        evaluateAndAssertStringScalarExpression("expression-func.xml",
                "testExpressionFunc", null, "gulocka v jamocke");
    }

    @Test
    public void testExpressionFuncConcatName() throws Exception {
        evaluateAndAssertStringScalarExpression("expression-func-concatname.xml",
                "testExpressionFuncConcatName", null, "Horatio Torquemada Marley");
    }

    private ScriptExpressionEvaluatorType parseScriptType(String fileName) throws SchemaException, IOException {
        return PrismTestUtil.parseAtomicValue(
                new File(getTestDir(), fileName), ScriptExpressionEvaluatorType.COMPLEX_TYPE);
    }

    private <T> List<PrismPropertyValue<T>> evaluateExpression(
            ScriptExpressionEvaluatorType scriptType, ItemDefinition<?> outputDefinition,
            ExpressionVariables variables, String shortDesc, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ScriptExpression scriptExpression = createScriptExpression(scriptType, outputDefinition);

        ScriptExpressionEvaluationContext context = new ScriptExpressionEvaluationContext();
        context.setVariables(variables);
        context.setEvaluateNew(false);
        context.setScriptExpression(scriptExpression);
        context.setContextDescription(shortDesc);
        context.setResult(result);

        List<PrismPropertyValue<T>> resultValues = scriptExpression.evaluate(context);
        if (resultValues != null) {
            for (PrismPropertyValue<T> resultVal : resultValues) {
                if (resultVal.getParent() != null) {
                    AssertJUnit.fail("Result value " + resultVal + " from expression " + scriptExpression + " has parent");
                }
            }
        }
        return resultValues;
    }

    private ScriptExpression createScriptExpression(
            ScriptExpressionEvaluatorType expressionType, ItemDefinition<?> outputDefinition) {
        String language = expressionType.getLanguage();
        ScriptExpression expression = new ScriptExpression(
                scriptExpressionfactory.getEvaluators().get(language), expressionType);
        expression.setOutputDefinition(outputDefinition);
        expression.setObjectResolver(scriptExpressionfactory.getObjectResolver());
        expression.setFunctions(new ArrayList<>(scriptExpressionfactory.getFunctions()));
        ScriptExpressionProfile scriptExpressionProfile = getScriptExpressionProfile(language);
        expression.setScriptExpressionProfile(scriptExpressionProfile);
        expression.setExpressionProfile(getExpressionProfile(scriptExpressionProfile));
        return expression;
    }

    protected ExpressionProfile getExpressionProfile(ScriptExpressionProfile scriptExpressionProfile) {
        if (scriptExpressionProfile == null) {
            return null;
        }
        ExpressionProfile expressionProfile = new ExpressionProfile(this.getClass().getSimpleName());
        expressionProfile.setDecision(AccessDecision.DENY);
        ExpressionEvaluatorProfile evaluatorProfile = new ExpressionEvaluatorProfile(ScriptExpressionEvaluatorFactory.ELEMENT_NAME);
        expressionProfile.add(evaluatorProfile);
        evaluatorProfile.setDecision(AccessDecision.DENY);
        evaluatorProfile.add(scriptExpressionProfile);
        return expressionProfile;
    }

    protected ScriptExpressionProfile getScriptExpressionProfile(String language) {
        return null;
    }

    private <T> List<PrismPropertyValue<T>> evaluateExpression(
            ScriptExpressionEvaluatorType scriptType, QName typeName, boolean scalar,
            ExpressionVariables variables, String shortDesc, OperationResult opResult)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        MutableItemDefinition<?> outputDefinition = PrismTestUtil.getPrismContext()
                .definitionFactory().createPropertyDefinition(PROPERTY_NAME, typeName);
        if (!scalar) {
            outputDefinition.setMaxOccurs(-1);
        }
        return evaluateExpression(scriptType, outputDefinition, variables, shortDesc, opResult);
    }

    private <T> PrismPropertyValue<T> asScalar(
            List<PrismPropertyValue<T>> expressionResultList, String shortDesc) {
        if (expressionResultList.size() > 1) {
            AssertJUnit.fail("Expression " + shortDesc + " produces a list of " + expressionResultList.size() + " while only expected a single value: " + expressionResultList);
        }
        if (expressionResultList.isEmpty()) {
            return null;
        }
        return expressionResultList.iterator().next();
    }

    protected void evaluateAndAssertStringScalarExpression(
            String fileName, String testName, ExpressionVariables variables, String expectedValue)
            throws ObjectNotFoundException, CommunicationException, SecurityViolationException,
            SchemaException, IOException, ExpressionEvaluationException, ConfigurationException {
        List<PrismPropertyValue<String>> expressionResultList = evaluateStringExpression(fileName, testName, variables);
        PrismPropertyValue<String> expressionResult = asScalar(expressionResultList, testName);
        assertNotNull("Expression " + testName + " resulted in null value (expected '" + expectedValue + "')", expressionResult);
        assertEquals("Expression " + testName + " resulted in wrong value", expectedValue, expressionResult.getValue());
    }

    protected void evaluateAndAssertStringScalarExpressionRestricted(
            String fileName, String testName, ExpressionVariables variables)
            throws ObjectNotFoundException, CommunicationException, SchemaException, IOException,
            ExpressionEvaluationException, ConfigurationException {
        try {
            List<PrismPropertyValue<String>> expressionResultList = evaluateStringExpression(fileName, testName, variables);
            AssertJUnit.fail("Expression " + testName + ": unexpected success, result value: " + expressionResultList);
        } catch (SecurityViolationException e) {
            displayExpectedException(e);
        }
    }

    private void evaluateAndAssertStringListExpression(String fileName, String testName,
            ExpressionVariables variables, String... expectedValues)
            throws ObjectNotFoundException, CommunicationException, SecurityViolationException,
            SchemaException, IOException, ExpressionEvaluationException, ConfigurationException {
        List<PrismPropertyValue<String>> expressionResultList =
                evaluateStringExpression(fileName, testName, variables);
        TestUtil.assertSetEquals("Expression " + testName + " resulted in wrong values",
                PrismValueCollectionsUtil.getValues(expressionResultList), expectedValues);
    }

    protected void evaluateAndAssertBooleanScalarExpression(String fileName,
            String testName, ExpressionVariables variables, Boolean expectedValue)
            throws ObjectNotFoundException, CommunicationException, SecurityViolationException,
            SchemaException, IOException, ExpressionEvaluationException, ConfigurationException {
        List<PrismPropertyValue<Boolean>> expressionResultList = evaluateBooleanExpression(fileName, testName, variables);
        PrismPropertyValue<Boolean> expressionResult = asScalar(expressionResultList, testName);
        assertNotNull("Expression " + testName + " resulted in null value (expected '" + expectedValue + "')", expressionResult);
        assertEquals("Expression " + testName + " resulted in wrong value", expectedValue, expressionResult.getValue());
    }

    private List<PrismPropertyValue<String>> evaluateStringExpression(
            String fileName, String testName, ExpressionVariables variables)
            throws ObjectNotFoundException, CommunicationException, SecurityViolationException,
            SchemaException, IOException, ExpressionEvaluationException, ConfigurationException {
        ScriptExpressionEvaluatorType scriptType = parseScriptType(fileName);
        OperationResult opResult = createOperationResult();

        return evaluateExpression(scriptType, DOMUtil.XSD_STRING, true, variables, testName, opResult);
    }

    private List<PrismPropertyValue<Boolean>> evaluateBooleanExpression(
            String fileName, String testName, ExpressionVariables variables)
            throws ObjectNotFoundException, CommunicationException, SecurityViolationException,
            SchemaException, IOException, ExpressionEvaluationException, ConfigurationException {
        ScriptExpressionEvaluatorType scriptType = parseScriptType(fileName);
        OperationResult opResult = createOperationResult();

        return evaluateExpression(scriptType, DOMUtil.XSD_BOOLEAN, true, variables, testName, opResult);
    }

    protected ExpressionVariables createVariables(Object... params) {
        return ExpressionVariables.create(prismContext, params);
    }
}
