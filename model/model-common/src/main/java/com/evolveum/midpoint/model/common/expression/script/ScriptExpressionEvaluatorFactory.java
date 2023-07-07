/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.Collection;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

/**
 * @author semancik
 */
@Component
public class ScriptExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    public static final QName ELEMENT_NAME = SchemaConstantsGenerated.C_SCRIPT;

    @Autowired private ScriptExpressionFactory scriptExpressionFactory;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private LocalizationService localizationService;
    @Autowired private Protector protector;

    @SuppressWarnings("unused") // Used by Spring
    public ScriptExpressionEvaluatorFactory() {
    }

    @VisibleForTesting
    public ScriptExpressionEvaluatorFactory(
            ScriptExpressionFactory scriptExpressionFactory, SecurityContextManager securityContextManager) {
        this.scriptExpressionFactory = scriptExpressionFactory;
        this.securityContextManager = securityContextManager;
    }

    @Override
    public QName getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public <V extends PrismValue, D extends ItemDefinition<?>> ExpressionEvaluator<V> createEvaluator(
            @NotNull Collection<JAXBElement<?>> evaluatorElements,
            @Nullable D outputDefinition,
            @Nullable ExpressionProfile expressionProfile,
            @NotNull ExpressionFactory expressionFactory,
            @NotNull String contextDescription,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, SecurityViolationException {

        ScriptExpressionEvaluatorType evaluatorBean =
                getSingleEvaluatorBeanRequired(evaluatorElements, ScriptExpressionEvaluatorType.class, contextDescription);

        ScriptExpression scriptExpression =
                scriptExpressionFactory.createScriptExpression(
                        evaluatorBean, outputDefinition, expressionProfile, expressionFactory, contextDescription, result);

        return new ScriptExpressionEvaluator<>(
                ELEMENT_NAME,
                evaluatorBean,
                outputDefinition,
                protector,
                scriptExpression,
                localizationService);
    }

    public ScriptExpressionFactory getScriptExpressionFactory() {
        return scriptExpressionFactory;
    }
}
