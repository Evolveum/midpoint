/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.filter;

import java.util.Collection;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FilterExpressionEvaluatorType;

import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.Validate;
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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

/**
 * @author semancik
 */
@Component
public class FilterExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    public static final QName ELEMENT_NAME = SchemaConstantsGenerated.C_FILTER;

    @Autowired private LocalizationService localizationService;
    @Autowired private Protector protector;

    @SuppressWarnings("unused") // Used by Spring
    public FilterExpressionEvaluatorFactory() {
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

        Validate.notNull(outputDefinition, "output definition must be specified for filter expression evaluator");

        FilterExpressionEvaluatorType evaluatorBean = Objects.requireNonNull(
                getSingleEvaluatorBeanRequired(evaluatorElements, FilterExpressionEvaluatorType.class, contextDescription),
                () -> "missing filter specification in " + contextDescription);

        return new FilterExpressionEvaluator<>(
                ELEMENT_NAME,
                evaluatorBean,
                outputDefinition,
                protector,
                localizationService);
    }

}
