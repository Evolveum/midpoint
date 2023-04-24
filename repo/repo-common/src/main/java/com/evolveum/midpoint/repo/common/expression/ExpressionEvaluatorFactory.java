/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
     * @param evaluatorElements Definition of the evaluator. All elements must be of the same type.
     * @param outputDefinition Definition of output values.
     * @param expressionProfile Expression profile to be used during evaluation.
     * @param expressionFactory TODO - why?
     */
    <V extends PrismValue, D extends ItemDefinition<?>> ExpressionEvaluator<V> createEvaluator(
            Collection<JAXBElement<?>> evaluatorElements,
            D outputDefinition,
            ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory,
            String contextDescription, Task task, OperationResult result)
                    throws SchemaException, ObjectNotFoundException, SecurityViolationException;
}
