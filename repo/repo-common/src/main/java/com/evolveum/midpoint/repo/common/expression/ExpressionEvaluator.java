/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.expression;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an expression evaluator (e.g. literal, path, script, assignmentTargetSearch, etc).
 * Can apply it in given evaluation context.
 *
 * The evaluators were originally stateless; but they are created anew for each expression evaluation (at least when evaluated
 * as part of mappings evaluation), so we can afford to keep some state in them - as needed for (experimental)
 * {@link #doesVetoTargetValueRemoval(PrismValue, OperationResult)} method invocation.
 *
 * @author Radovan Semancik
 */
public interface ExpressionEvaluator<V extends PrismValue> {

    /**
     * Executes the evaluation in a given context. The context provides necessary data,
     * evaluator provides definition of processing that should be carried out.
     *
     * @return Result of the evaluation in the form of delta set triple (i.e. added, deleted, unchanged values).
     */
    PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Fully qualified name of the element defining the expression (e.g. c:path).
     */
    QName getElementName();

    /**
     * Short characterization of the evaluator. One line, often only a single word.
     */
    String shortDebugDump();

    /** @see Expression#doesVetoTargetValueRemoval(PrismValue, OperationResult). */
    default boolean doesVetoTargetValueRemoval(@NotNull V value, @NotNull OperationResult result) {
        return false;
    }
}
