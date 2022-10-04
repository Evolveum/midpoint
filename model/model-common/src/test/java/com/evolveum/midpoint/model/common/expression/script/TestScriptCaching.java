/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

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
import com.evolveum.midpoint.common.LocalizationTestUtil;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryUtil;
import com.evolveum.midpoint.model.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.KeyStoreBasedProtectorBuilder;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.DirectoryFileObjectResolver;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

/**
 * @author semancik
 */
public class TestScriptCaching extends AbstractUnitTest
        implements InfraTestMixin {

    private static final File TEST_DIR = new File("src/test/resources/expression/groovy");
    protected static final File OBJECTS_DIR = new File("src/test/resources/objects");

    private static final QName PROPERTY_NAME = new QName(MidPointConstants.NS_MIDPOINT_TEST_PREFIX, "whatever");

    protected ScriptExpressionFactory scriptExpressionfactory;
    protected ScriptEvaluator evaluator;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @BeforeClass
    public void setupFactory() {
        display("Setting up expression factory and evaluator");
        PrismContext prismContext = getPrismContext();
        ObjectResolver resolver = new DirectoryFileObjectResolver(OBJECTS_DIR);
        Protector protector = KeyStoreBasedProtectorBuilder.create(prismContext).buildOnly();
        Clock clock = new Clock();
        Collection<FunctionLibrary> functions = new ArrayList<>();
        functions.add(FunctionLibraryUtil.createBasicFunctionLibrary(prismContext, protector, clock));
        scriptExpressionfactory = new ScriptExpressionFactory(functions, resolver);
        evaluator = new Jsr223ScriptEvaluator("groovy", prismContext, protector, LocalizationTestUtil.getLocalizationService());
        scriptExpressionfactory.registerEvaluator(evaluator);
    }

    @Test
    public void testGetExtensionPropertyValue() throws Exception {
        // GIVEN
        InternalMonitor.reset();

        assertScriptMonitor(0, 0, "init");

        // WHEN, THEN
        long etimeFirst = executeScript("expression-string-variables.xml", "FOOBAR", "first");
        assertScriptMonitor(1, 1, "first");

        long etimeSecond = executeScript("expression-string-variables.xml", "FOOBAR", "second");
        assertScriptMonitor(1, 2, "second");
        assertTrue("Einstein was wrong! " + etimeFirst + " -> " + etimeSecond, etimeSecond <= etimeFirst);

        long etimeThird = executeScript("expression-string-variables.xml", "FOOBAR", "second");
        assertScriptMonitor(1, 3, "third");
        assertTrue("Einstein was wrong again! " + etimeFirst + " -> " + etimeThird, etimeThird <= etimeFirst);

        // Different script. Should compile.
        long horatio1Time = executeScript("expression-func-concatname.xml", "Horatio Torquemada Marley", "horatio");
        assertScriptMonitor(2, 4, "horatio");

        // Same script. No compilation.
        long etimeFourth = executeScript("expression-string-variables.xml", "FOOBAR", "fourth");
        assertScriptMonitor(2, 5, "fourth");
        assertTrue("Einstein was wrong all the time! " + etimeFirst + " -> " + etimeFourth, etimeFourth <= etimeFirst);

        // Try this again. No compile.
        long horatio2Time = executeScript("expression-func-concatname.xml", "Horatio Torquemada Marley", "horatio2");
        assertScriptMonitor(2, 6, "horatio2");
        assertTrue("Even Horatio was wrong! " + horatio1Time + " -> " + horatio2Time, horatio2Time <= horatio1Time);
    }

    private void assertScriptMonitor(int expCompilations, int expExecutions, String desc) {
        assertEquals("Unexpected number of script compilations after " + desc, expCompilations, InternalMonitor.getCount(InternalCounters.SCRIPT_COMPILE_COUNT));
        assertEquals("Unexpected number of script executions after " + desc, expExecutions, InternalMonitor.getCount(InternalCounters.SCRIPT_EXECUTION_COUNT));
    }

    private long executeScript(String filname, String expectedResult, String desc)
            throws SchemaException, SecurityViolationException, ExpressionEvaluationException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, IOException {
        // GIVEN
        OperationResult result = createOperationResult(desc);
        ScriptExpressionEvaluatorType scriptType = parseScriptType(filname);
        ItemDefinition outputDefinition = getPrismContext().definitionFactory().createPropertyDefinition(PROPERTY_NAME, DOMUtil.XSD_STRING);

        ScriptExpression scriptExpression = createScriptExpression(scriptType, outputDefinition);

        VariablesMap variables = VariablesMap.create(getPrismContext(),
                "foo", "FOO", PrimitiveType.STRING,
                "bar", "BAR", PrimitiveType.STRING
        );

        // WHEN
        long startTime = System.currentTimeMillis();

        ScriptExpressionEvaluationContext context = new ScriptExpressionEvaluationContext();
        context.setVariables(variables);
        context.setEvaluateNew(false);
        context.setScriptExpression(scriptExpression);
        context.setContextDescription(desc);
        context.setResult(result);

        List<PrismPropertyValue<String>> scripResults = scriptExpression.evaluate(context);
        long endTime = System.currentTimeMillis();

        // THEN
        displayValue(
                "Script results " + desc + ", etime: " + (endTime - startTime) + " ms",
                scripResults);

        String scriptResult = asScalarString(scripResults);
        assertEquals("Wrong script " + desc + " result", expectedResult, scriptResult);

        return (endTime - startTime);
    }

    private ScriptExpression createScriptExpression(
            ScriptExpressionEvaluatorType expressionType, ItemDefinition outputDefinition) {
        ScriptExpression expression = new ScriptExpression(scriptExpressionfactory.getEvaluators().get(expressionType.getLanguage()), expressionType);
        expression.setOutputDefinition(outputDefinition);
        expression.setObjectResolver(scriptExpressionfactory.getObjectResolver());
        expression.setFunctions(new ArrayList<>(scriptExpressionfactory.getStandardFunctionLibraries()));
        return expression;
    }

    private ScriptExpressionEvaluatorType parseScriptType(String fileName)
            throws SchemaException, IOException {
        return PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, fileName), ScriptExpressionEvaluatorType.COMPLEX_TYPE);
    }

    private String asScalarString(List<PrismPropertyValue<String>> expressionResultList) {
        if (expressionResultList.size() > 1) {
            AssertJUnit.fail("Expression produces a list of " + expressionResultList.size() + " while only expected a single value: " + expressionResultList);
        }
        if (expressionResultList.isEmpty()) {
            return null;
        }
        return expressionResultList.iterator().next().getValue();
    }

}
