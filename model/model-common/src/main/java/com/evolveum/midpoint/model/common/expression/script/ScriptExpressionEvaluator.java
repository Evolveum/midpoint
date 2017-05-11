/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.List;

import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.evaluator.AbstractValueTransformationExpressionEvaluator;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;

/**
 * @author Radovan Semancik
 */
public class ScriptExpressionEvaluator<V extends PrismValue,D extends ItemDefinition> 
				extends AbstractValueTransformationExpressionEvaluator<V,D,ScriptExpressionEvaluatorType> {

	private ScriptExpression scriptExpression;
	
	private static final Trace LOGGER = TraceManager.getTrace(ScriptExpressionEvaluator.class);

    ScriptExpressionEvaluator(ScriptExpressionEvaluatorType scriptType, ScriptExpression scriptExpression, SecurityEnforcer securityEnforcer) {
    	super(scriptType, securityEnforcer);
        this.scriptExpression = scriptExpression;
    }
    
    @Override
	protected List<V> transformSingleValue(ExpressionVariables variables, PlusMinusZero valueDestination, boolean useNew, 
			ExpressionEvaluationContext context, String contextDescription, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		ScriptExpressionReturnTypeType returnType = getExpressionEvaluatorType().getReturnType();
		if (returnType == null && isRelative()) {
			returnType = ScriptExpressionReturnTypeType.SCALAR;
		}
		scriptExpression.setAdditionalConvertor(context.getAdditionalConvertor());
		return (List<V>) scriptExpression.evaluate(variables, returnType, useNew, contextDescription, task, result);
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "script: "+scriptExpression.toString();
	}
}
