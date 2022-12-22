/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.provisioning.ucf.api.UcfExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * Expression evaluator that is provided to lower-level components in UCF layer.
 */
@Component
@Experimental
public class UcfExpressionEvaluatorImpl implements UcfExpressionEvaluator {

    private static final String OP_EVALUATE = UcfExpressionEvaluatorImpl.class.getName() + ".evaluate";

    @Autowired private ExpressionFactory expressionFactory;

    @NotNull
    @Override
    public <O> List<O> evaluate(ExpressionType expressionBean, VariablesMap variables, QName outputPropertyName,
            String ctxDesc, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            Expression<PrismPropertyValue<O>, PrismPropertyDefinition<O>> expression =
                    expressionFactory
                            .makePropertyExpression(expressionBean, outputPropertyName, MiscSchemaUtil.getExpressionProfile(),
                                    ctxDesc, task, result);
            VariablesMap exprVariables = new VariablesMap();
            exprVariables.putAll(variables);
            ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, exprVariables, ctxDesc, task);
            context.setExpressionFactory(expressionFactory);
            PrismValueDeltaSetTriple<PrismPropertyValue<O>> exprResultTriple = expression.evaluate(context, result);
            List<O> list = new ArrayList<>();
            for (PrismPropertyValue<O> pv : exprResultTriple.getZeroSet()) {
                list.add(pv.getRealValue());
            }
            return list;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }
}
