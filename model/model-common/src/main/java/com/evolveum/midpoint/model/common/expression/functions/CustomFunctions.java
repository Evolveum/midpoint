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
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
	
	public Object execute(String functionName, Object... params) throws ExpressionEvaluationException {
		Validate.notNull("Function name must be specified", functionName);
		
		List<ExpressionType> functions = library.getFunction().stream().filter(expression -> functionName.equals(expression.getName())).collect(Collectors.toList());
		
		LOGGER.info("functions {}", functions);
		ExpressionType expression = functions.iterator().next();
//		return null;
		LOGGER.info("fuction to execute {}", expression);
		//TODO: fill expression variables
		ExpressionVariables variables = new ExpressionVariables();
		
		QName returnType = expression.getReturnType();
		if (returnType == null) {
			returnType = DOMUtil.XSD_STRING;
		}
		try {
			
			ItemDefinition outputDefinition = prismContext.getSchemaRegistry().findItemDefinitionByType(returnType);
			if (outputDefinition == null) {
				outputDefinition = new PrismPropertyDefinitionImpl<>(SchemaConstantsGenerated.C_VALUE, returnType, prismContext);
			}
			
			return ExpressionUtil.evaluateExpression(variables, outputDefinition, expression, expressionFactory, "description", task, result);
			
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException e) {
			throw new ExpressionEvaluationException(e.getMessage(), e);
		}
		
	}

}
