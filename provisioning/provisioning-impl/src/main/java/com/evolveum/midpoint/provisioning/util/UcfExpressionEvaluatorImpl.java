/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.provisioning.util;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.provisioning.ucf.api.UcfExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Component
@Experimental
public class UcfExpressionEvaluatorImpl implements UcfExpressionEvaluator {

	@Autowired private ExpressionFactory expressionFactory;
	@Autowired private TaskManager taskManager;

	@NotNull
	@Override
	public <O> List<O> evaluate(ExpressionType expressionBean, Map<QName, Object> variables, QName outputPropertyName,
			String ctxDesc) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException {
		// TODO consider getting the task instance from the caller
		Task task = taskManager.createTaskInstance(UcfExpressionEvaluatorImpl.class.getName() + ".evaluate");
		OperationResult result = task.getResult();

		Expression<PrismPropertyValue<O>, PrismPropertyDefinition<O>> expression =
				expressionFactory.makePropertyExpression(expressionBean, outputPropertyName, ctxDesc, task, result);
		ExpressionVariables exprVariables = new ExpressionVariables();
		exprVariables.addVariableDefinitions(variables);
		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, exprVariables, ctxDesc, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<O>> exprResultTriple = expression.evaluate(context);
		List<O> list = new ArrayList<>();
		for (PrismPropertyValue<O> pv : exprResultTriple.getZeroSet()) {
			list.add(pv.getRealValue());
		}
		return list;
	}
}
