/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.ConstantsManager;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDeltaUtil;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstExpressionEvaluatorType;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class ConstExpressionEvaluator<V extends PrismValue, D extends ItemDefinition> extends AbstractExpressionEvaluator<V, D, ConstExpressionEvaluatorType> {

    private final ConstantsManager constantsManager;

    ConstExpressionEvaluator(QName elementName, @NotNull ConstExpressionEvaluatorType evaluatorBean, D outputDefinition,
            Protector protector, ConstantsManager constantsManager, PrismContext prismContext) {
        super(elementName, evaluatorBean, outputDefinition, protector, prismContext);
        this.constantsManager = constantsManager;
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException {
        checkEvaluatorProfile(context);

        String constName = expressionEvaluatorBean.getValue();
        String stringValue = constantsManager.getConstantValue(constName);

        Item<V, D> output = outputDefinition.instantiate();

        Object value = ExpressionUtil.convertToOutputValue(stringValue, outputDefinition, protector);

        if (output instanceof PrismProperty) {
            ((PrismProperty<Object>) output).addRealValue(value);
        } else {
            throw new UnsupportedOperationException(
                    "Can only generate values of property, not " + output.getClass());
        }

        return ItemDeltaUtil.toDeltaSetTriple(output, null, prismContext);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#
     * shortDebugDump()
     */
    @Override
    public String shortDebugDump() {
        return "const:"+ expressionEvaluatorBean.getValue();
    }
}
