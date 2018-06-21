/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.repo.common.expression;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueSetDefinitionType;

/**
 * @author semancik
 *
 */
public class ValueSetDefinition {

	private ValueSetDefinitionType setDefinitionType;
	private String shortDesc;
	private Task task;
	private OperationResult result;
	private QName additionalVariableName;
	private Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> condition;

	public ValueSetDefinition(ValueSetDefinitionType setDefinitionType, QName additionalVariableName, String shortDesc, Task task, OperationResult result) {
		super();
		this.setDefinitionType = setDefinitionType;
		this.additionalVariableName = additionalVariableName;
		this.shortDesc = shortDesc;
		this.task = task;
		this.result = result;
	}

	public void init(ExpressionFactory expressionFactory) throws SchemaException, ObjectNotFoundException {
		ExpressionType conditionType = setDefinitionType.getCondition();
		condition =  ExpressionUtil.createCondition(conditionType, expressionFactory,
				shortDesc, task, result);
	}

	public <IV extends PrismValue> boolean contains(IV pval) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		return evalCondition(pval.getRealValue());
	}

	/**
	 * Same as contains, but wraps exceptions in TunnelException.
	 */
	public <IV extends PrismValue> boolean containsTunnel(IV pval) {
		try {
			return contains(pval);
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
			throw new TunnelException(e);
		}
	}

	private boolean evalCondition(Object value) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, value);
		if (additionalVariableName != null) {
			variables.addVariableDefinition(additionalVariableName, value);
		}
		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, shortDesc, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = condition.evaluate(context);
		if (outputTriple == null) {
			return false;
		}
		return ExpressionUtil.computeConditionResult(outputTriple.getNonNegativeValues());
	}



}
