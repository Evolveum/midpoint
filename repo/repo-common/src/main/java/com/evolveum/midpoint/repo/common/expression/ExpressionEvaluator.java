/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

/**
 * Represents an expression evaluator (e.g. literal, path, script, assignmentTargetSearch, etc).
 * Can apply it in given evaluation context.
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
}
