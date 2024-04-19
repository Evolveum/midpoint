/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.Collection;
import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowOwnerReferenceSearchExpressionEvaluatorType;

/**
 * Factory for {@link ShadowOwnerReferenceSearchExpressionEvaluator} objects.
 */
public class ShadowOwnerReferenceSearchExpressionEvaluatorFactory extends ReferenceSearchExpressionEvaluatorFactory {

    private static final QName ELEMENT_NAME = SchemaConstantsGenerated.C_SHADOW_OWNER_REFERENCE_SEARCH;

    public ShadowOwnerReferenceSearchExpressionEvaluatorFactory(ExpressionFactory expressionFactory, Protector protector) {
        super(expressionFactory, protector);
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
            @NotNull OperationResult result) throws SchemaException {

        var evaluatorBean = getSingleEvaluatorBeanRequired(
                evaluatorElements, ShadowOwnerReferenceSearchExpressionEvaluatorType.class, contextDescription);

        //noinspection unchecked
        return (ExpressionEvaluator<V>)
                new ShadowOwnerReferenceSearchExpressionEvaluator(
                        ELEMENT_NAME,
                        evaluatorBean,
                        (PrismReferenceDefinition) outputDefinition,
                        protector,
                        getObjectResolver(),
                        getLocalizationService());
    }
}
