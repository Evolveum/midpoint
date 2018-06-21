/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processors.general;

import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
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

        ExpressionVariables variables = new ExpressionVariables();
        variables.addVariableDefinition(new QName(SchemaConstants.NS_C, "context"), context);

        boolean start;
        try {
            start = evaluateBooleanExpression(conditionExpression, variables, "workflow activation condition", taskFromModel, result);
        } catch (ObjectNotFoundException|ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            throw new SystemException("Couldn't evaluate generalChangeProcessor activation condition", e);
        }
        return start;
    }

    private boolean evaluateBooleanExpression(ExpressionType expressionType, ExpressionVariables expressionVariables, String opContext, Task taskFromModel, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        PrismContext prismContext = expressionFactory.getPrismContext();
        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<Boolean> resultDef = new PrismPropertyDefinitionImpl(resultName, DOMUtil.XSD_BOOLEAN, prismContext);
        Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression = expressionFactory.makeExpression(expressionType, resultDef, opContext, taskFromModel, result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, opContext, taskFromModel, result);
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
