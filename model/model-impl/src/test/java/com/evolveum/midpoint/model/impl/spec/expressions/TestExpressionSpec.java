/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.spec.expressions;

import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.model.impl.spec.expressions.TestExpressionSpec.VariablesStyle.DELTA;
import static com.evolveum.midpoint.model.impl.spec.expressions.TestExpressionSpec.VariablesStyle.NO_DELTA;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.AbstractModelImplementationIntegrationTest;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.prism.PrismValueDeltaSetTripleAsserter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestExpressionSpec extends AbstractModelImplementationIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestExpressionSpec.class);

    private static final String TEST_DIR = "src/test/resources/spec/expressions";

    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private PrismContext prismContext;

    private static final File EXPRESSION_GIVEN_NAME_TO_UPPERCASE_FILE = new File(TEST_DIR, "givenName-to-uppercase.xml");
    private static final File EXPRESSION_FULL_NAME_FROM_PARTS_FILE = new File(TEST_DIR, "fullName-from-parts.xml");

    private long lastScriptExecutionCount;

    private static final String SRC_GIVEN_NAME = "givenName";
    private static final String SRC_FAMILY_NAME = "familyName";
    private static final String SRC_HONORIFIC_PREFIX = "honorificPrefix";
    private static final String SRC_HONORIFIC_SUFFIX = "honorificSuffix";
    private static final String VAR_FOO = "foo";
    private static final String VAR_BAR = "bar";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {

//        predefinedTestMethodTracing = PredefinedTestMethodTracing.MODEL_LOGGING;
        rememberScriptExecutionCount();
    }

    @Test
    public void test100GivenNameUppercaseNoDelta() throws Exception {
        given();
        Collection<Source<?, ?>> sources =
                source(SRC_GIVEN_NAME)
                        .old("jack")
                        .buildSources();

        when();
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                evaluate(EXPRESSION_GIVEN_NAME_TO_UPPERCASE_FILE, sources, NO_DELTA);

        then();
        assertOutputTriple(outputTriple)
                .assertEmptyMinus()
                .assertEmptyPlus()
                .zeroSet()
                .assertSinglePropertyValue("JACK");

        assertScriptExecutionIncrement(1);
    }

    @Test
    public void test110GivenNameUppercaseReplaceDelta() throws Exception {
        given();
        Collection<Source<?, ?>> sources =
                source(SRC_GIVEN_NAME)
                        .old("jack")
                        .replace("jackie")
                        .buildSources();

        when();
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                evaluate(EXPRESSION_GIVEN_NAME_TO_UPPERCASE_FILE, sources, DELTA);

        then();
        assertOutputTriple(outputTriple)
                .assertEmptyZero()
                .plusSet()
                .assertSinglePropertyValue("JACKIE")
                .end()
                .minusSet()
                .assertSinglePropertyValue("JACK")
                .end();

        assertScriptExecutionIncrement(2);
    }

    @Test
    public void test120GivenNameUppercaseAddDeleteDelta() throws Exception {
        given();
        Collection<Source<?, ?>> sources =
                source(SRC_GIVEN_NAME)
                        .old("jack")
                        .delete("jack")
                        .add("jackie")
                        .buildSources();

        when();
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                evaluate(EXPRESSION_GIVEN_NAME_TO_UPPERCASE_FILE, sources, DELTA);

        then();
        assertOutputTriple(outputTriple)
                .assertEmptyZero()
                .plusSet()
                .assertSinglePropertyValue("JACKIE")
                .end()
                .minusSet()
                .assertSinglePropertyValue("JACK")
                .end();

        assertScriptExecutionIncrement(2);
    }

    private PrismValueDeltaSetTriple<PrismPropertyValue<String>> evaluate(File expressionFile, Collection<Source<?, ?>> sources,
            VariablesStyle variablesStyle) throws Exception {
        ExpressionType expression = parseExpression(expressionFile);
        VariablesMap variables = prepareVariables(variablesStyle);
        Task task = getTestTask();
        var expressionContext = new ExpressionEvaluationContext(sources, variables, getTestNameShort(), task);

        return evaluatePropertyExpression(expression, PrimitiveType.STRING, expressionContext, task.getResult());
    }

    private VariablesMap prepareVariables(VariablesStyle variablesStyle) throws SchemaException {
        switch (variablesStyle) {
            case NO_DELTA:
                return prepareBasicVariablesNoDelta();
            case DELTA:
                return prepareBasicVariablesWithDelta();
            default:
                throw new AssertionError(variablesStyle);
        }
    }

    @Test
    public void test200FullNameFromPartsNoDelta() throws Exception {
        given();
        Collection<Source<?, ?>> sources = Arrays.asList(
                source(SRC_GIVEN_NAME)
                        .old("Jack")
                        .build(),
                source(SRC_FAMILY_NAME)
                        .old("Sparrow")
                        .build(),
                source(SRC_HONORIFIC_PREFIX)
                        .old("Cpt.")
                        .build(),
                source(SRC_HONORIFIC_SUFFIX)
                        .build()
        );

        when();
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                evaluate(EXPRESSION_FULL_NAME_FROM_PARTS_FILE, sources, NO_DELTA);

        then();
        assertOutputTriple(outputTriple)
                .assertEmptyMinus()
                .assertEmptyPlus()
                .zeroSet()
                .assertSinglePropertyValue("Cpt. Jack Sparrow");

        assertScriptExecutionIncrement(1);
    }

    private ExpressionType parseExpression(File file) throws SchemaException, IOException {
        return PrismTestUtil.parseAtomicValue(file, ExpressionType.COMPLEX_TYPE);
    }

    private VariablesMap prepareBasicVariablesNoDelta() {
        VariablesMap variables = new VariablesMap();

        variables.put(VAR_FOO, "fooValue", String.class);
        variables.put(VAR_BAR, "barValue", String.class);

        return variables;
    }

    private VariablesMap prepareBasicVariablesWithDelta() throws SchemaException {
        VariablesMap variables = new VariablesMap();

        variables.put(VAR_FOO, "fooValue", String.class);

        PrismProperty<String> barProperty = createProperty(VAR_BAR, singleton("barValueOld"));
        PropertyDelta<String> barDelta = barProperty.createDelta();
        barDelta.setRealValuesToReplace("barValueNew");
        ItemDeltaItem<PrismPropertyValue<String>, PrismPropertyDefinition<String>> barIdi = new ItemDeltaItem<>(barProperty, barDelta, null, barProperty.getDefinition());
        barIdi.recompute();
        variables.put(VAR_BAR, barIdi, String.class);
        return variables;
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> PrismValueDeltaSetTriple<V> evaluateExpression(
            ExpressionType expressionType, D outputDefinition, ExpressionEvaluationContext expressionContext,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        Expression<V, D> expression = expressionFactory.makeExpression(expressionType, outputDefinition, null,
                expressionContext.getContextDescription(), expressionContext.getTask(), result);
        LOGGER.debug("Starting evaluation of expression: {}", expression);
        return expression.evaluate(expressionContext, result);
    }

    private <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluatePropertyExpression(
            ExpressionType expressionType, QName outputType, ExpressionEvaluationContext expressionContext, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        PrismPropertyDefinition<T> outputDefinition = prismContext.definitionFactory().createPropertyDefinition(
                ExpressionConstants.OUTPUT_ELEMENT_NAME, outputType);
        return evaluateExpression(expressionType, outputDefinition, expressionContext, result);
    }

    private <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluatePropertyExpression(
            ExpressionType expressionType, PrimitiveType outputType, ExpressionEvaluationContext expressionContext,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        return evaluatePropertyExpression(expressionType, outputType.getQname(), expressionContext, result);
    }

    private void rememberScriptExecutionCount() {
        lastScriptExecutionCount = InternalMonitor.getCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
    }

    private void assertScriptExecutionIncrement(int expectedIncrement) {
        long currentScriptExecutionCount = InternalMonitor.getCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
        long actualIncrement = currentScriptExecutionCount - lastScriptExecutionCount;
        assertEquals("Unexpected increment in script execution count",
                expectedIncrement, actualIncrement);
        lastScriptExecutionCount = currentScriptExecutionCount;
    }

    private <V extends PrismValue> PrismValueDeltaSetTripleAsserter<V, Void> assertOutputTriple(PrismValueDeltaSetTriple<V> triple) {
        assertNotNull(triple);
        triple.checkConsistence();
        PrismValueDeltaSetTripleAsserter<V, Void> asserter = new PrismValueDeltaSetTripleAsserter<>(triple, "expression output triple");
        asserter.setPrismContext(prismContext);
        asserter.display();
        return asserter;
    }

    private SourceBuilder source(String name) {
        return new SourceBuilder(name);
    }

    private class SourceBuilder {

        private final String name;
        private final Collection<String> oldValues = new HashSet<>();
        private Collection<String> addValues;
        private Collection<String> deleteValues;
        private Collection<String> replaceValues;

        private SourceBuilder(String name) {
            this.name = name;
        }

        private SourceBuilder old(String... values) {
            oldValues.addAll(Arrays.asList(values));
            return this;
        }

        private SourceBuilder add(String... values) {
            addValues = Arrays.asList(values);
            return this;
        }

        private SourceBuilder delete(String... values) {
            deleteValues = Arrays.asList(values);
            return this;
        }

        private SourceBuilder replace(String... values) {
            replaceValues = Arrays.asList(values);
            return this;
        }

        private Collection<Source<?, ?>> buildSources() throws SchemaException {
            return singleton(build());
        }

        private Source<?, ?> build() throws SchemaException {
            PrismProperty<String> property = createProperty(name, oldValues);
            PropertyDelta<String> delta;
            if (addValues != null || deleteValues != null || replaceValues != null) {
                delta = property.createDelta();
                if (addValues != null) {
                    delta.addRealValuesToAdd(addValues);
                }
                if (deleteValues != null) {
                    delta.addRealValuesToDelete(deleteValues);
                }
                if (replaceValues != null) {
                    delta.setRealValuesToReplace(replaceValues.toArray(new String[0]));
                }
            } else {
                delta = null;
            }
            Source<PrismPropertyValue<String>, PrismPropertyDefinition<String>> source =
                    new Source<>(property, delta, null, property.getElementName(), property.getDefinition());
            source.recompute();
            return source;
        }
    }

    private PrismProperty<String> createProperty(String name, Collection<String> values)
            throws SchemaException {
        PrismPropertyDefinition<String> propDef = prismContext.definitionFactory()
                .createPropertyDefinition(new QName(SchemaConstants.NS_C, name), PrimitiveType.STRING.getQname());
        PrismProperty<String> property = prismContext.itemFactory().createProperty(propDef.getItemName(), propDef);
        for (String value : values) {
            property.add(prismContext.itemFactory().createPropertyValue(value));
        }
        return property;
    }

    enum VariablesStyle {
        NO_DELTA, DELTA
    }
}
