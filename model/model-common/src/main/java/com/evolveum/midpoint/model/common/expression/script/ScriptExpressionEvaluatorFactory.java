/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import java.util.Collection;

import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.AbstractAutowiredExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

/**
 * @author semancik
 */
@NullMarked
@Component
public class ScriptExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    public static final QName ELEMENT_NAME = SchemaConstantsGenerated.C_SCRIPT;

    @Autowired private ScriptFactory scriptFactory;
    @Autowired private LocalizationService localizationService;
    @Autowired private Protector protector;

    @SuppressWarnings("unused") // Used by Spring
    public ScriptExpressionEvaluatorFactory() {
    }

    @VisibleForTesting
    public ScriptExpressionEvaluatorFactory(ScriptFactory scriptFactory) {
        this.scriptFactory = scriptFactory;
    }

    @Override
    public QName getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public <V extends PrismValue, D extends ItemDefinition<?>> ExpressionEvaluator<V> createEvaluator(
            Collection<JAXBElement<?>> evaluatorElements,
            @Nullable D outputDefinition,
            ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory,
            String contextDescription,
            Task task,
            OperationResult result) throws SchemaException, SecurityViolationException {

        ScriptExpressionEvaluatorType scriptBean =
                getSingleEvaluatorBeanRequired(evaluatorElements, ScriptExpressionEvaluatorType.class, contextDescription);
        var expressionEvaluatorProfile = getEvaluatorProfile(expressionProfile);

        Script script =
                scriptFactory.createScript(
                        scriptBean, outputDefinition, expressionProfile, expressionEvaluatorProfile, contextDescription, result);

        return new ScriptExpressionEvaluator<>(ELEMENT_NAME, script, protector, localizationService);
    }

    public ScriptFactory getScriptFactory() {
        return scriptFactory;
    }

    public static ExpressionEvaluatorProfile getEvaluatorProfile(ExpressionProfile expressionProfile) {
        return expressionProfile.getEvaluatorProfile(ELEMENT_NAME);
    }
}
