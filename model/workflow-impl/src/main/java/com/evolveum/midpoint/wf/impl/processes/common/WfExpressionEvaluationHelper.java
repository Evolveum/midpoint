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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.activiti.engine.delegate.DelegateExecution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getExpressionFactory;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getMiscDataUtil;

/**
 * @author mederly
 */
@Component
public class WfExpressionEvaluationHelper {

	public List<ObjectReferenceType> evaluateRefExpressions(List<ExpressionType> expressions,
			ExpressionVariables variables, String contextDescription,
			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		List<ObjectReferenceType> retval = new ArrayList<>();
		for (ExpressionType expression : expressions) {
			retval.addAll(evaluateRefExpression(expression, variables, contextDescription, task, result));
		}
		return retval;
	}

	public List<ObjectReferenceType> evaluateRefExpression(ExpressionType expressionType, ExpressionVariables variables,
			String contextDescription, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
		return evaluateExpression(expressionType, variables, contextDescription, ObjectReferenceType.class,
				ObjectReferenceType.COMPLEX_TYPE, ExpressionUtil.createRefConvertor(UserType.COMPLEX_TYPE), task, result);
	}

	public <T> T getSingleValue(Collection<T> values, T defaultValue, String contextDescription) {
		if (values.size() == 0) {
			return defaultValue;
		} else if (values.size() > 1) {
			throw new IllegalStateException("Expression should return exactly one value; it returned "
					+ values.size() + " ones in " + contextDescription);
		} else {
			T value = values.iterator().next();
			return value != null ? value : defaultValue;
		}
	}

	@SuppressWarnings("unchecked")
	@NotNull
	public <T> List<T> evaluateExpression(ExpressionType expressionType, ExpressionVariables variables,
			String contextDescription, Class<T> clazz, QName typeName,
			Function<Object, Object> additionalConvertor, Task task,
			OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
		ExpressionFactory expressionFactory = getExpressionFactory();
		PrismContext prismContext = expressionFactory.getPrismContext();
		PrismPropertyDefinition<String> resultDef = new PrismPropertyDefinitionImpl<>(
				new QName(SchemaConstants.NS_C, "result"), typeName, prismContext);
		Expression<PrismPropertyValue<String>,PrismPropertyDefinition<String>> expression =
				expressionFactory.makeExpression(expressionType, resultDef, contextDescription, task, result);
		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, contextDescription, task, result);
		context.setAdditionalConvertor(additionalConvertor);
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> exprResultTriple = ModelExpressionThreadLocalHolder
				.evaluateExpressionInContext(expression, context, task, result);
		return exprResultTriple.getZeroSet().stream()
				.map(ppv -> (T) ppv.getRealValue())
				.collect(Collectors.toList());
	}

	public boolean evaluateBooleanExpression(ExpressionType expressionType, ExpressionVariables expressionVariables,
			String contextDescription, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
		Collection<Boolean> values = evaluateExpression(expressionType, expressionVariables, contextDescription,
				Boolean.class, DOMUtil.XSD_BOOLEAN, null, task, result);
		return getSingleValue(values, false, contextDescription);
	}

	public ExpressionVariables getDefaultVariables(@Nullable DelegateExecution execution, Task wfTask, OperationResult result)
			throws SchemaException, ObjectNotFoundException {

		MiscDataUtil miscDataUtil = getMiscDataUtil();

		ExpressionVariables variables = new ExpressionVariables();

		variables.addVariableDefinition(C_REQUESTER,
				miscDataUtil.resolveObjectReference(wfTask.getWorkflowContext().getRequesterRef(), result));

		variables.addVariableDefinition(C_OBJECT,
				miscDataUtil.resolveObjectReference(wfTask.getWorkflowContext().getObjectRef(), result));

		// might be null
		variables.addVariableDefinition(C_TARGET,
				miscDataUtil.resolveObjectReference(wfTask.getWorkflowContext().getTargetRef(), result));

		ObjectDelta objectDelta;
		try {
			objectDelta = miscDataUtil.getFocusPrimaryDelta(wfTask.getWorkflowContext(), true);
		} catch (JAXBException e) {
			throw new SchemaException("Couldn't get object delta: " + e.getMessage(), e);
		}
		variables.addVariableDefinition(SchemaConstants.T_OBJECT_DELTA, objectDelta);

		// todo other variables?

		// Activiti process instance variables (use with care)
		if (execution != null) {
			execution.getVariables().entrySet().forEach(e ->
					variables.addVariableDefinition(new QName("_" + e.getKey()), e.getValue()));
		}

		return variables;
	}

	public static Function<Object, Object> createOutcomeConvertor() {
		return (o) -> {
			if (o == null || o instanceof String) {
				return o;
			} else if (o instanceof ApprovalLevelOutcomeType) {
				return ApprovalUtils.toUri((ApprovalLevelOutcomeType) o);
			} else if (o instanceof QName) {
				return QNameUtil.qNameToUri((QName) o);
			} else {
				//throw new IllegalArgumentException("Couldn't create an URI from " + o);
				return o;		// let someone else complain about this
			}
		};
	}
}
