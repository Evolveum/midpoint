/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.expression;

import java.util.Collection;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates expression evaluators from their definitions (evaluator elements) and parts of the context
 * (e.g. output item definition).
 *
 * @author semancik
 */
public interface ExpressionEvaluatorFactory {

    /**
     * Qualified element name (i.e. type) of evaluator elements this factory is able to process.
     */
    QName getElementName();

    /**
     * Creates an evaluator.
     *
     * @param evaluatorElements Definition of the evaluator. May be empty. If it's not, all elements must be of the same type.
     * @param outputDefinition Definition of output values.
     * @param expressionProfile Expression profile to be used during evaluation.
     * @param expressionFactory Necessary for ScriptExpressionEvaluator.
     */
    <V extends PrismValue, D extends ItemDefinition<?>> ExpressionEvaluator<V> createEvaluator(
            @NotNull Collection<JAXBElement<?>> evaluatorElements,
            @Nullable D outputDefinition,
            @Nullable ExpressionProfile expressionProfile,
            @NotNull ExpressionFactory expressionFactory,
            @NotNull String contextDescription,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException;
}
