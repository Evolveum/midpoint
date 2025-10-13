/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.path;

import java.util.Collection;
import java.util.Objects;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.task.api.Task;
import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.AbstractObjectResolvableExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is NOT autowired evaluator factory.
 *
 * @author semancik
 */
public class PathExpressionEvaluatorFactory extends AbstractObjectResolvableExpressionEvaluatorFactory {

    private static final QName ELEMENT_NAME = SchemaConstantsGenerated.C_PATH;

    private final Protector protector;

    public PathExpressionEvaluatorFactory(ExpressionFactory expressionFactory, Protector protector) {
        super(expressionFactory);
        this.protector = protector;
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

        Validate.notNull(outputDefinition, "output definition must be specified for path expression evaluator");

        ItemPathType path = Objects.requireNonNull(
                getSingleEvaluatorBean(evaluatorElements, ItemPathType.class, contextDescription),
                () -> "missing path specification in " + contextDescription);

        return new PathExpressionEvaluator<>(ELEMENT_NAME, path, outputDefinition, protector);
    }
}
