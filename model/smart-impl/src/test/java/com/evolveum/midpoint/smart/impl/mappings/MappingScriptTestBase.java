/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings;

import java.io.IOException;
import java.util.List;

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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.smart.impl.scoring.MappingScriptValidator;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public abstract class MappingScriptTestBase extends AbstractUnitTest implements InfraTestMixin {

    private final MappingScriptValidator validator;

    protected MappingScriptTestBase() throws SchemaException, IOException, SAXException {
        this.validator = validator();
    }

    protected String evaluateExpression(ExpressionType expression, String inputName, String inputValue)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return this.validator.evaluateExpression(expression, inputName, inputValue,
                new NullTaskImpl(), createOperationResult()).iterator().next();
    }

    /**
     * WARNING: Returned values pair contains only empty item paths and is always in "inbound" direction.
     */
    protected static ValuesPairSample<String, String> inboundValuesPair(String shadowValue, String focusValue) {
        return new ValuesPairSample<>(ItemPath.EMPTY_PATH, ItemPath.EMPTY_PATH,
                List.of(new ValuesPair<>(List.of(shadowValue), List.of(focusValue))),
                MappingDirection.INBOUND);
    }

    protected static ExpressionType createScriptExpression(String groovyCode, String description) {
        return new ExpressionType()
                .description(description)
                .expressionEvaluator(
                        new ObjectFactory().createScript(
                                new ScriptExpressionEvaluatorType().language("mel").code(groovyCode)));
    }

    /**
     * WARNING: Returned validator does not support invocation of the MidPoint Functions Library from MEL expressions.
     */
    private static MappingScriptValidator validator() throws SchemaException, IOException, SAXException {
        final ModelCommonBeans beans = ExpressionTestUtil.initializeModelCommonBeans();
        final ExpressionFactory expressionFactory = beans.expressionFactory;
        final var scriptExpressionEvaluatorFactory = (ScriptExpressionEvaluatorFactory) expressionFactory
                .getEvaluatorFactory(SchemaConstantsGenerated.C_SCRIPT);

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

        return new MappingScriptValidator(expressionFactory);
    }
}
