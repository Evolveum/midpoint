/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.velocity;

import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;
import com.evolveum.midpoint.model.common.expression.script.ScriptExecutionContext;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.ReferenceInsertionEventHandler;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.script.AbstractScriptExecutor;
import com.evolveum.midpoint.prism.binding.TypeSafeEnum;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.exception.*;

/**
 * Expression evaluator that is using Apache Velocity engine.
 */
public class VelocityScriptExecutor extends AbstractScriptExecutor {

    public VelocityScriptExecutor(
            PrismContext prismContext,
            Protector protector,
            LocalizationService localizationService,
            ExpressionsConfigurationSection configuration) {
        super(prismContext, protector, localizationService, configuration);
        Velocity.init(new Properties());
    }

    @Override
    public @NotNull Object executeInternal(
            @NotNull String codeString,
            @NotNull ScriptExecutionContext context)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, SubscriptionComplianceException {

        VelocityContext velocityCtx = createVelocityContext(context);

        StringWriter resultWriter = new StringWriter();

        InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
        Velocity.evaluate(velocityCtx, resultWriter, "", codeString);

        return resultWriter.toString();
    }

    private VelocityContext createVelocityContext(ScriptExecutionContext context)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, SubscriptionComplianceException {
        VelocityContext velocityCtx = new VelocityContext();

        // Render midPoint schema enums using their lexical values
        EventCartridge eventCartridge = new EventCartridge();
        eventCartridge.addEventHandler((ReferenceInsertionEventHandler)
                        (velocityContext, reference, value) ->
                                value instanceof TypeSafeEnum typeSafeEnum ? typeSafeEnum.value() : value);
        eventCartridge.attachToContext(velocityCtx);

        Map<String, Object> scriptVariables = prepareUnifiedScriptVariablesValueMap(context);
        for (Map.Entry<String, Object> scriptVariable : scriptVariables.entrySet()) {
            velocityCtx.put(scriptVariable.getKey(), scriptVariable.getValue());
        }
        return velocityCtx;
    }

    @Override
    public String getLanguageName() {
        return MidPointConstants.EXPRESSION_LANGUAGE_VELOCITY_NAME;
    }

    @Override
    public @NotNull String getLanguageUrl() {
        return MidPointConstants.EXPRESSION_LANGUAGE_VELOCITY_URL;
    }
}
