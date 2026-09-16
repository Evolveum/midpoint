/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.expression.evaluator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NullExpressionEvaluatorType;

/**
 * Always returns an empty set of values (empty zero, plus and minus sets).
 * Combined with a target set (range) definition it can be used to remove all values of the target item.
 *
 * @author Viliam Repan
 */
public class NullExpressionEvaluator<V extends PrismValue, D extends ItemDefinition<?>>
        extends AbstractExpressionEvaluator<V, D, NullExpressionEvaluatorType> {

    NullExpressionEvaluator(
            QName elementName, NullExpressionEvaluatorType evaluatorBean, D outputDefinition, Protector protector) {
        super(elementName, evaluatorBean, outputDefinition, protector);
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SecurityViolationException {

        checkEvaluatorProfile(context);

        return PrismContext.get().deltaFactory().createPrismValueDeltaSetTriple();
    }

    @Override
    public String shortDebugDump() {
        return "null";
    }
}
