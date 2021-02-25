/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.general;

import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralChangeProcessorScenarioType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

import java.util.Collection;

/**
 * @author mederly
 */
@Component
public class GcpExpressionHelper {

    private static final Trace LOGGER = TraceManager.getTrace(GcpExpressionHelper.class);

    @Autowired
    private ExpressionFactory expressionFactory;

    boolean evaluateActivationCondition(GeneralChangeProcessorScenarioType scenarioType, ModelContext context, Task taskFromModel, OperationResult result) throws SchemaException {
        ExpressionType conditionExpression = scenarioType.getActivationCondition();

        if (conditionExpression == null) {
            return true;
        }

        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_MODEL_CONTEXT, context, ModelContext.class);

        boolean start;
        try {
            start = evaluateBooleanExpression(conditionExpression, variables, "workflow activation condition", taskFromModel, result);
        } catch (ObjectNotFoundException|ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            throw new SystemException("Couldn't evaluate generalChangeProcessor activation condition", e);
        }
        return start;
    }

    private boolean evaluateBooleanExpression(ExpressionType expressionType, VariablesMap VariablesMap, String opContext, Task taskFromModel, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        PrismContext prismContext = expressionFactory.getPrismContext();
        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<Boolean> resultDef = prismContext.definitionFactory().createPropertyDefinition(resultName, DOMUtil.XSD_BOOLEAN);
        Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression = expressionFactory.makeExpression(expressionType, resultDef, MiscSchemaUtil.getExpressionProfile(), opContext, taskFromModel, result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, VariablesMap, opContext, taskFromModel);
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> exprResultTriple = ModelExpressionThreadLocalHolder
                .evaluateExpressionInContext(expression, params, taskFromModel, result);

        Collection<PrismPropertyValue<Boolean>> exprResult = exprResultTriple.getZeroSet();
        if (exprResult.size() == 0) {
            return false;
        } else if (exprResult.size() > 1) {
            throw new IllegalStateException("Expression should return exactly one boolean value; it returned " + exprResult.size() + " ones");
        }
        Boolean boolResult = exprResult.iterator().next().getValue();
        return boolResult != null ? boolResult : false;
    }

}
