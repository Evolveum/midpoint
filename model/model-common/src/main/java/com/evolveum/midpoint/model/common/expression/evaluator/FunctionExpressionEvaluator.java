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
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;

/**
 * @author semancik
 *
 */
public class FunctionExpressionEvaluator<V extends PrismValue, D extends ItemDefinition>
		implements ExpressionEvaluator<V, D> {

	public static final int DEFAULT_LENGTH = 8;

	private FunctionExpressionEvaluatorType functionEvaluatorType;
	private D outputDefinition;
	private Protector protector;
	private PrismContext prismContext;
	private ObjectResolver objectResolver;
	private StringPolicyType elementStringPolicy;

	FunctionExpressionEvaluator(FunctionExpressionEvaluatorType generateEvaluatorType, D outputDefinition,
			Protector protector, ObjectResolver objectResolver, PrismContext prismContext) {
		this.functionEvaluatorType = generateEvaluatorType;
		this.outputDefinition = outputDefinition;
		this.protector = protector;
		this.objectResolver = objectResolver;
		this.prismContext = prismContext;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.evolveum.midpoint.common.expression.ExpressionEvaluator#evaluate(java
	 * .util.Collection, java.util.Map, boolean, java.lang.String,
	 * com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {


		List<ExpressionType> expressions;

		ObjectReferenceType functionLibraryRef = functionEvaluatorType.getLibraryRef();
		
		if (functionLibraryRef == null) {
			throw new SchemaException("No functions library defined");
		}
		
			FunctionLibraryType functionLibraryType = objectResolver.resolve(functionLibraryRef, FunctionLibraryType.class,
	        			null, "resolving value policy reference in generateExpressionEvaluator", context.getTask(), context.getResult());
				expressions = functionLibraryType.getFunction();
	    
		if (CollectionUtils.isEmpty(expressions)) {
			throw new ObjectNotFoundException("No functions defined in referenced function library: " + functionLibraryType);
		}

		FunctionType functionType = functionEvaluatorType.getFunction();
		
		if (functionType == null) {
			throw new SchemaException("No function declaration found in " + functionEvaluatorType);
		}
		
		String functionName = functionType.getName();
		
		if (StringUtils.isEmpty(functionName)) {
			throw new SchemaException("Missing function name in " + functionType);
		}
		
		List<ExpressionType> filteredExpressions = expressions.stream().filter(expression -> functionName.equals(expression.getName())).collect(Collectors.toList());
		if (filteredExpressions.size() ==0 ){
			String possibleFunctions = "";
			for (ExpressionType expression : expressions) {
				possibleFunctions += expression.getName() + ", ";
			}
			possibleFunctions = possibleFunctions.substring(0, possibleFunctions.lastIndexOf(","));
			throw new ObjectNotFoundException("No function with name " + functionName + " found in " + functionEvaluatorType + ". Function defined are: " + possibleFunctions);
		}
		
		ExpressionType functionToExecute = determineFunctionToExecute(filteredExpressions);
		
		ExpressionFactory factory = context.getExpressionFactory();
		Expression<V, D> expression = factory.makeExpression(functionToExecute, outputDefinition, "function execution", context.getTask(), context.getResult());
		
		ExpressionVariables originVariables = context.getVariables();
		
		ExpressionEvaluationContext functionContext = context.shallowClone();
		ExpressionVariables functionVariables = new ExpressionVariables();
		
		functionEvaluatorType.getFunction().getParameter().forEach(param -> functionVariables.addVariableDefinition(new QName(param.getName()), originVariables.get(new QName((String)param.getValue()))));
		functionContext.setVariables(functionVariables);
		
		return expression.evaluate(context);
	}

	private ExpressionType determineFunctionToExecute(List<ExpressionType> filteredExpressions) {
		
		if (filteredExpressions.size() == 1) {
			return filteredExpressions.iterator().next();
		}
		
		List<ExpressionParameterType> functionParams = functionEvaluatorType.getFunction().getParameter();
		
		for (ExpressionType filteredExpression : filteredExpressions) {
			List<ExpressionParameterType> filteredExpressionParameters = filteredExpression.getParameter();
			if (functionParams.size() != filteredExpressionParameters.size()) {
				continue;
			}
			if (!compareParameters(functionParams, filteredExpressionParameters)) {
				continue;
			}
			return filteredExpression;
		}
		return null;
	}
	
	private boolean compareParameters(List<ExpressionParameterType> functionParams, List<ExpressionParameterType> filteredExpressionParameters) {
		for (ExpressionParameterType  functionParam: functionParams ) {
			boolean found = false;
			for (ExpressionParameterType filteredExpressionParam : filteredExpressionParameters) {
			
				if (filteredExpressionParam.getName().equals(functionParam.getName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#
	 * shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "function";
	}

}
