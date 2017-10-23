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
package com.evolveum.midpoint.model.common.expression.functions;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionReturnMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;


public class CustomFunctions {
	
	Trace LOGGER = TraceManager.getTrace(CustomFunctions.class);
	
	private ExpressionFactory expressionFactory;
	private FunctionLibraryType library;
	private OperationResult result;
	private Task task;
	private PrismContext prismContext;
	
	public CustomFunctions(FunctionLibraryType library, ExpressionFactory expressionFactory, OperationResult result, Task task) {
		this.library = library;
		this.expressionFactory = expressionFactory;
		this.prismContext = expressionFactory.getPrismContext();
		this.result = result;
		this.task = task;
	}
	
	public Object execute(String functionName, Map<String, Object> params) throws ExpressionEvaluationException {
		Validate.notNull("Function name must be specified", functionName);
		
		List<ExpressionType> functions = library.getFunction().stream().filter(expression -> functionName.equals(expression.getName())).collect(Collectors.toList());
		
		LOGGER.trace("functions {}", functions);
		ExpressionType expression = functions.iterator().next();

		LOGGER.trace("fuction to execute {}", expression);
		
		try {
			ExpressionVariables variables = new ExpressionVariables();
			if (MapUtils.isNotEmpty(params)) {
				for (Map.Entry<String, Object> entry : params.entrySet()) {
					variables.addVariableDefinition(new QName(entry.getKey()), convertInput(entry, expression));
				};
			}
			
			QName returnType = expression.getReturnType();
			if (returnType == null) {
				returnType = DOMUtil.XSD_STRING;
			}
			
			ItemDefinition outputDefinition = prismContext.getSchemaRegistry().findItemDefinitionByType(returnType);
			if (outputDefinition == null) {
				outputDefinition = new PrismPropertyDefinitionImpl<>(SchemaConstantsGenerated.C_VALUE, returnType, prismContext);
			}
			if (expression.getReturnMultiplicity() != null && expression.getReturnMultiplicity() == ExpressionReturnMultiplicityType.MULTI) {
				outputDefinition.setMaxOccurs(-1);
			} else {
				outputDefinition.setMaxOccurs(1);
			}
			
			return ExpressionUtil.evaluateExpression(variables, outputDefinition, expression, expressionFactory, "description", task, result);
			
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
			throw new ExpressionEvaluationException(e.getMessage(), e);
		}
		
	}

	private Object convertInput(Map.Entry<String, Object> entry, ExpressionType expression) throws SchemaException {
	
		ExpressionParameterType expressionParam = expression.getParameter().stream().filter(param -> param.getName().equals(entry.getKey())).findAny().orElseThrow(SchemaException :: new);
		Class<?> expressioNParameterClass = XsdTypeMapper.toJavaTypeIfKnown(expressionParam.getType());
		
		// FIXME: awful hack
		if (expressioNParameterClass.equals(String.class) && entry.getValue().getClass().equals(PolyString.class)) {
			return ((PolyString) entry.getValue()).getOrig();
		}
		
		if (!expressioNParameterClass.equals(entry.getValue().getClass())){
			throw new SchemaException("Unexpected type of value, expecting " + expressioNParameterClass.getSimpleName() + " but the actual value is " + entry.getValue().getClass().getSimpleName());
		}
		
		return entry.getValue();
		
	}

}
