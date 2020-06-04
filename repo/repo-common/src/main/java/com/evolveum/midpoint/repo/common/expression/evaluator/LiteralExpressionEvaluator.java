/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression.evaluator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

/**
 * Expression evaluator that provides literal (constant) value(s).
 *
 * @author Radovan Semancik
 */
public class LiteralExpressionEvaluator<V extends PrismValue,D extends ItemDefinition> implements ExpressionEvaluator<V> {

    private final QName elementName;
    private final PrismValueDeltaSetTriple<V> outputTriple;

    LiteralExpressionEvaluator(QName elementName, PrismValueDeltaSetTriple<V> outputTriple) {
        this.elementName = elementName;
        this.outputTriple = outputTriple;
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException {
        ExpressionUtil.checkEvaluatorProfileSimple(this, context);
        return outputTriple != null ? outputTriple.clone() : null;
    }

    @Override
    public QName getElementName() {
        return elementName;
    }

    @Override
    public String shortDebugDump() {
        return "literal: "+outputTriple;
    }
}
