/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script.velocity;

import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.script.AbstractScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.exception.*;

/**
 * Expression evaluator that is using Apache Velocity engine.
 */
public class VelocityScriptEvaluator extends AbstractScriptEvaluator {

    private static final String LANGUAGE_NAME = "velocity";
    private static final String LANGUAGE_URL = MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE + LANGUAGE_NAME;

    public VelocityScriptEvaluator(PrismContext prismContext, Protector protector, LocalizationService localizationService) {
        super(prismContext, protector, localizationService);
        Velocity.init(new Properties());
    }

    @Override
    public @NotNull Object evaluateInternal(
            @NotNull String codeString,
            @NotNull ScriptExpressionEvaluationContext context)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        VelocityContext velocityCtx = createVelocityContext(context);

        StringWriter resultWriter = new StringWriter();

        InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
        Velocity.evaluate(velocityCtx, resultWriter, "", codeString);

        return resultWriter.toString();
    }

    private VelocityContext createVelocityContext(ScriptExpressionEvaluationContext context)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        VelocityContext velocityCtx = new VelocityContext();
        Map<String, Object> scriptVariables = prepareScriptVariablesValueMap(context);
        for (Map.Entry<String, Object> scriptVariable : scriptVariables.entrySet()) {
            velocityCtx.put(scriptVariable.getKey(), scriptVariable.getValue());
        }
        return velocityCtx;
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    @Override
    public @NotNull String getLanguageUrl() {
        return LANGUAGE_URL;
    }
}
