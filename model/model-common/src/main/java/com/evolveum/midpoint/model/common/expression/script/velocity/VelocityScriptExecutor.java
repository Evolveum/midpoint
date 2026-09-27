/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.velocity;

import com.evolveum.midpoint.repo.common.SystemObjectCache.ExpressionsConfigurationView;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;
import com.evolveum.midpoint.model.common.expression.script.ScriptExecutionContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.exception.*;

/**
 * Expression evaluator that is using Apache Velocity engine in the original way (before midPoint 4.11).
 */
public class VelocityScriptExecutor extends AbstractVelocityScriptExecutor {

    public VelocityScriptExecutor(
            PrismContext prismContext,
            Protector protector,
            LocalizationService localizationService,
            ExpressionsConfigurationSection configuration,
            @NotNull ExpressionsConfigurationView expressionsConfigurationView) {
        super(prismContext, protector, localizationService, configuration, expressionsConfigurationView);
    }

    @Override
    ExecutionMode getExecutionModeFromExecutor() {
        return ExecutionMode.FULL;
    }

    @Override
    protected void checkProfileAndSafetyRestrictions(ScriptExecutionContext context) throws SecurityViolationException {
        super.checkProfileAndSafetyRestrictions(context);
        if (configuration.safeVelocityExpressionsOnly()) {
            throw new SecurityViolationException("Unsafe velocity expressions are not allowed in this configuration");
        }
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
