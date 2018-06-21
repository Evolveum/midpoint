/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.wf.impl.processes.common;

import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getExpressionFactory;

/**
 * @author mederly
 */
@Component
public class WfExpressionEvaluationHelper {

	public List<ObjectReferenceType> evaluateRefExpressions(List<ExpressionType> expressions,
			ExpressionVariables variables, String contextDescription,
			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		List<ObjectReferenceType> retval = new ArrayList<>();
		for (ExpressionType expression : expressions) {
			retval.addAll(evaluateRefExpression(expression, variables, contextDescription, task, result));
		}
		return retval;
	}

	public List<ObjectReferenceType> evaluateRefExpression(ExpressionType expressionType, ExpressionVariables variables,
			String contextDescription, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		return evaluateExpression(expressionType, variables, contextDescription, ObjectReferenceType.class,
				ObjectReferenceType.COMPLEX_TYPE, false, ExpressionUtil.createRefConvertor(UserType.COMPLEX_TYPE), task, result);
	}

	@SuppressWarnings("unchecked")
	@NotNull
	public <T> List<T> evaluateExpression(ExpressionType expressionType, ExpressionVariables variables,
			String contextDescription, Class<T> clazz, QName typeName,
			boolean multiValued, Function<Object, Object> additionalConvertor, Task task,
			OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		ExpressionFactory expressionFactory = getExpressionFactory();
		PrismContext prismContext = expressionFactory.getPrismContext();
		ItemDefinition<?> resultDef;
		QName resultName = new QName(SchemaConstants.NS_C, "result");
		if (QNameUtil.match(typeName, ObjectReferenceType.COMPLEX_TYPE)) {
			resultDef = new PrismReferenceDefinitionImpl(resultName, typeName, prismContext);
		} else {
			resultDef = new PrismPropertyDefinitionImpl<>(resultName, typeName, prismContext);
		}
		if (multiValued) {
			resultDef.setMaxOccurs(-1);
		}
		Expression<?,?> expression = expressionFactory.makeExpression(expressionType, resultDef, contextDescription, task, result);
		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, contextDescription, task, result);
		context.setAdditionalConvertor(additionalConvertor);
		PrismValueDeltaSetTriple<?> exprResultTriple = ModelExpressionThreadLocalHolder
				.evaluateAnyExpressionInContext(expression, context, task, result);
		List<T> list = new ArrayList<>();
		for (PrismValue pv : exprResultTriple.getZeroSet()) {
			T realValue;
			if (pv instanceof PrismReferenceValue) {
				// pv.getRealValue sometimes returns synthesized Referencable, not ObjectReferenceType
				// If we would stay with that we would need to make many changes throughout workflow module.
				// So it is safer to stay with ObjectReferenceType.
				ObjectReferenceType ort = new ObjectReferenceType();
				ort.setupReferenceValue((PrismReferenceValue) pv);
				realValue = (T) ort;
			} else {
				realValue = pv.getRealValue();
			}
			list.add(realValue);
		}
		return list;
	}

	public boolean evaluateBooleanExpression(ExpressionType expressionType, ExpressionVariables expressionVariables,
			String contextDescription, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		Collection<Boolean> values = evaluateExpression(expressionType, expressionVariables, contextDescription,
				Boolean.class, DOMUtil.XSD_BOOLEAN, false, null, task, result);
		return MiscUtil.getSingleValue(values, false, contextDescription);
	}

	public String evaluateStringExpression(ExpressionType expressionType, ExpressionVariables expressionVariables,
			String contextDescription, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		Collection<String> values = evaluateExpression(expressionType, expressionVariables, contextDescription,
				String.class, DOMUtil.XSD_STRING, false, null, task, result);
		return MiscUtil.getSingleValue(values, null, contextDescription);
	}
}
