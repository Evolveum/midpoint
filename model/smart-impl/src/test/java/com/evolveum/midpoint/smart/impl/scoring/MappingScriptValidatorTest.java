/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.scoring;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationTestUtil;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.expression.ExpressionTestUtil;
import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryUtil;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.script.mel.MelScriptEvaluator;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

public class MappingScriptValidatorTest extends AbstractUnitTest implements InfraTestMixin {
    private static final String GROOVY = "Groovy";
    private static final String MEL = "mel";

    private ExpressionFactory expressionFactory;

    @BeforeClass
    void setupBeans() throws SchemaException, IOException, SAXException {
        final ModelCommonBeans beans = ExpressionTestUtil.initializeModelCommonBeans();
        this.expressionFactory = beans.expressionFactory;
        final var scriptExpressionEvaluatorFactory = ((ScriptExpressionEvaluatorFactory) this.expressionFactory
                .getEvaluatorFactory(SchemaConstantsGenerated.C_SCRIPT));

        final FunctionLibraryBinding basicFunctionLibraryBinding =
                FunctionLibraryUtil.createBasicFunctionLibraryBinding(beans.prismContext, beans.protector, new Clock());
        //noinspection DataFlowIssue - supress warnings caused by the `null` midpointFunctions parameter
        scriptExpressionEvaluatorFactory.getScriptExpressionFactory().registerEvaluator(
                new MelScriptEvaluator(
                        beans.prismContext,
                        beans.protector,
                        LocalizationTestUtil.getLocalizationService(),
                        (BasicExpressionFunctions) basicFunctionLibraryBinding.getImplementation(),
                        // Instantiating MidPointFunctionsImpl manually in this test would be a nightmare (if even
                        // possible). We don't even need it for our purposes, so just set it to null. We just need to
                        // be careful to not invoke any Midpoint functions library method in tested expressions.
                        null));
    }

    @Test
    void groovyScriptIsValid_validateScript_scriptFailsDueToSecurityRestriction() {
        final ExpressionType expression = createExpression(GROOVY, "input.replaceAll('-', '')");

        final MappingScriptValidator validator = new MappingScriptValidator(this.expressionFactory);
        Assert.assertThrows(SecurityViolationException.class, () -> validator.evaluateExpression(
                expression, "input", "1-2-3", String.class, new NullTaskImpl(), createOperationResult()));
    }

    @Test
    void melExpressionIsValid_validateScript_expressionPassValidation()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final ExpressionType expression = createExpression(MEL, "input.replace('-', '')");

        final Collection<String> result = new MappingScriptValidator(this.expressionFactory)
                .evaluateExpression(expression, "input", "1-2-3", String.class,
                        new NullTaskImpl(), createOperationResult());

        assertEquals(result.size(), 1);
        assertEquals(result.iterator().next(), "123");
    }

    @Test
    void melExpressionIsNotValid_validateExpression_expressionValidationFails() {
        final ExpressionType expression = createExpression(MEL, "input.replce('-', '')");

        final MappingScriptValidator validator = new MappingScriptValidator(this.expressionFactory);
        Assert.assertThrows(ExpressionEvaluationException.class, () -> validator.evaluateExpression(
                expression, "input", "1-2-3", String.class, new NullTaskImpl(), createOperationResult()));
    }

    /**
     * Demonstrates that the validator passes the variable to the expression in its original type
     * so that the expression can call methods specific to that type.
     */
    @Test
    void melExpressionUsesXmlGregorianCalendarMethod_validateScript_expressionPassValidation()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final ExpressionType expression = createExpression(MEL, "input.formatDateTime('yyyy')");
        final XMLGregorianCalendar dateTime =
                XmlTypeConverter.createXMLGregorianCalendar(2024, 5, 17, 10, 30, 0);

        final Collection<String> result = new MappingScriptValidator(this.expressionFactory)
                .evaluateExpression(expression, "input", dateTime, XMLGregorianCalendar.class,
                        new NullTaskImpl(), createOperationResult());

        assertEquals(result.size(), 1);
        assertEquals(result.iterator().next(), "2024");
    }

    /**
     * When the same MEL expression is evaluated with
     * the variable wrongly typed as String, validation must fail.
     */
    @Test
    void melExpressionUsesXmlGregorianCalendarMethod_variablePassedAsString_expressionValidationFails() {
        final ExpressionType expression = createExpression(MEL, "input.formatDateTime('yyyy')");

        final MappingScriptValidator validator = new MappingScriptValidator(this.expressionFactory);
        Assert.assertThrows(ExpressionEvaluationException.class, () -> validator.evaluateExpression(
                expression, "input", "2024-05-17T10:30:00", String.class,
                new NullTaskImpl(), createOperationResult()));
    }

    private static ExpressionType createExpression(String lang, String code) {
        return new ExpressionType()
                .description("test expression in %s language".formatted(lang))
                .expressionEvaluator(new ObjectFactory().createScript(
                        new ScriptExpressionEvaluatorType()
                                .language(lang)
                                .code(code)));
    }
}
