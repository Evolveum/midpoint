/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.expression.evaluator;

import java.util.Collection;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.crypto.Protector;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.AbstractAutowiredExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Creates {@link LiteralExpressionEvaluator} objects.
 *
 * @author semancik
 */
@Component
public class LiteralExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    private static final QName ELEMENT_NAME = SchemaConstantsGenerated.C_VALUE;

    @Autowired private Protector protector;

    @SuppressWarnings("unused") // Used by Spring
    public LiteralExpressionEvaluatorFactory() {
    }

    @VisibleForTesting
    public LiteralExpressionEvaluatorFactory(Protector protector) {
        this.protector = protector;
    }

    @Override
    public QName getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public <V extends PrismValue, D extends ItemDefinition<?>> ExpressionEvaluator<V> createEvaluator(
            @NotNull Collection<JAXBElement<?>> evaluatorElements,
            D outputDefinition, // nullable in interface, but required here
            @Nullable ExpressionProfile expressionProfile,
            @NotNull ExpressionFactory expressionFactory,
            @NotNull String contextDescription,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException {

        Validate.notNull(outputDefinition, "output definition must be specified for literal expression evaluator");

        return new LiteralExpressionEvaluator<>(ELEMENT_NAME, evaluatorElements, outputDefinition, protector);
    }
}
