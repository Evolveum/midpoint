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
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author katkav
 * @author semancik
 */
public class FunctionExpressionEvaluator<V extends PrismValue, D extends ItemDefinition>
		implements ExpressionEvaluator<V, D> {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(FunctionExpressionEvaluator.class); 

	private FunctionExpressionEvaluatorType functionEvaluatorType;
	private D outputDefinition;
	private Protector protector;
	private PrismContext prismContext;
	private ObjectResolver objectResolver;

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
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {


		List<ExpressionType> expressions;

		ObjectReferenceType functionLibraryRef = functionEvaluatorType.getLibraryRef();
		
		if (functionLibraryRef == null) {
			throw new SchemaException("No functions library defined");
		}
		
		OperationResult result = context.getResult().createMinorSubresult(FunctionExpressionEvaluator.class.getSimpleName() + ".resolveFunctionLibrary");
		Task task = context.getTask();
		
			FunctionLibraryType functionLibraryType = objectResolver.resolve(functionLibraryRef, FunctionLibraryType.class,
	        			null, "resolving value policy reference in generateExpressionEvaluator", task, result);
				expressions = functionLibraryType.getFunction();
	    
		if (CollectionUtils.isEmpty(expressions)) {
			throw new ObjectNotFoundException("No functions defined in referenced function library: " + functionLibraryType);
		}

		
		String functionName = functionEvaluatorType.getName();
		
		if (StringUtils.isEmpty(functionName)) {
			throw new SchemaException("Missing function name in " + functionEvaluatorType);
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
		
		OperationResult functionExpressionResult = result.createMinorSubresult(FunctionExpressionEvaluator.class.getSimpleName() + ".makeExpression");
		ExpressionFactory factory = context.getExpressionFactory();
		
		Expression<V, D> expression;
		try {
			expression = factory.makeExpression(functionToExecute, outputDefinition, "function execution", task, functionExpressionResult);
			functionExpressionResult.recordSuccess();
		} catch (SchemaException | ObjectNotFoundException e) {
			functionExpressionResult.recordFatalError("Cannot make expression for " + functionToExecute + ". Reason: " + e.getMessage(), e);
			throw e;
		}
		
		ExpressionVariables originVariables = context.getVariables();
		
		ExpressionEvaluationContext functionContext = context.shallowClone();
		ExpressionVariables functionVariables = new ExpressionVariables();
		
		for (ExpressionParameterType param : functionEvaluatorType.getParameter()) {
			ExpressionType valueExpression = param.getExpression();
			OperationResult variableResult = result.createMinorSubresult(FunctionExpressionEvaluator.class.getSimpleName() + ".resolveVariable");
			try {
				variableResult.addArbitraryObjectAsParam("valueExpression", valueExpression);
				D variableOutputDefinition = determineVariableOutputDefinition(functionToExecute, param.getName(), context);
				ExpressionUtil.evaluateExpression(originVariables, variableOutputDefinition, valueExpression, context.getExpressionFactory(), "resolve variable", task, variableResult);
				variableResult.recordSuccess();
			} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
					| ConfigurationException | SecurityViolationException e) {
				variableResult.recordFatalError("Failed to resolve variable: " + valueExpression + ". Reason: " + e.getMessage());
				throw e;
			}
		}
		
		functionContext.setVariables(functionVariables);
		
		return expression.evaluate(context);
	}

	private ExpressionType determineFunctionToExecute(List<ExpressionType> filteredExpressions) {
		
		if (filteredExpressions.size() == 1) {
			return filteredExpressions.iterator().next();
		}
		
		List<ExpressionParameterType> functionParams = functionEvaluatorType.getParameter();
		
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
	
	private D determineVariableOutputDefinition(ExpressionType functionToExecute, String paramName, ExpressionEvaluationContext context) throws SchemaException {
		
		ExpressionParameterType functionParameter = null;
		for (ExpressionParameterType functionParam: functionToExecute.getParameter()) {
			if (functionParam.getName().equals(paramName)) {
				functionParameter = functionParam;
				break;
			}
		}
		
		if (functionParameter == null) {
			throw new SchemaException("Unexpected parameter " + paramName + " for function: " + functionToExecute);
		}
		
		QName returnType = functionParameter.getType();
		
		if (returnType == null) {
			throw new SchemaException("Cannot determine parameter output definition for " + functionParameter);
		}
		
		
			D returnTypeDef = (D) prismContext.getSchemaRegistry().findItemDefinitionByType(returnType);
			if (returnTypeDef == null) {
				returnTypeDef = (D) new PrismPropertyDefinitionImpl(SchemaConstantsGenerated.C_VALUE, returnType, prismContext);
				returnTypeDef.setMaxOccurs(functionToExecute.getReturnMultiplicity() != null && functionToExecute.getReturnMultiplicity() == ExpressionReturnMultiplicityType.SINGLE ? 1 : -1);
			}
			
			return returnTypeDef;
		
	
	}
	
//	private D determineParamDefinitionFromSource(Collection<Source<?,?>> sources, String paramName) throws SchemaException {
//		List<Source<?,?>> sourceParam = sources.stream().filter(s -> QNameUtil.match(s.getName(), new QName(paramName))).collect(Collectors.toList());
//		
//		if (sourceParam.size() == 0) {
//			throw new SchemaException("Cannot find variable definition with name: " + paramName);
//		}
//		
//		if (sourceParam.size() > 1) {
//			throw new SchemaException("Found more than one variable with name: " + paramName);
//		}
//		
//		Source<?,?> source = sourceParam.iterator().next();
//		return (D) source.getDefinition();
//	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#
	 * shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "function: " + functionEvaluatorType.getName();
	}

}
