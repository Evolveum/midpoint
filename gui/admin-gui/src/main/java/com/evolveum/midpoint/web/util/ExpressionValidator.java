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
package com.evolveum.midpoint.web.util;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismContext;

import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

public class ExpressionValidator<T> implements INullAcceptingValidator<T> {

	private static final long serialVersionUID = 1L;
	
//	private InputPanel inputPanel;
	private IModel<ExpressionType> expressionTypeModel;
	private ModelServiceLocator serviceLocator;
//	private T realValue;
	
	private static final String OPERATION_EVALUATE_EXPRESSION = ExpressionValidator.class.getName() + ".evaluateValidationExpression";
	
	public ExpressionValidator(IModel<ExpressionType> expressionType, ModelServiceLocator serviceLocator) {
//		this.inputPanel = inputPanel;
		this.expressionTypeModel = expressionType;
		this.serviceLocator = serviceLocator;
//		this.realValue = realValue;
	}
	
	

	@Override
	public void validate(IValidatable<T> validatable) {
		ExpressionType expressionType = expressionTypeModel.getObject();
		
		if (expressionType == null) {
			return;
		}
		
		PrismContext prismContext = serviceLocator.getPrismContext();
		T valueToValidate = validatable.getValue();
		String contextDesc = " form component expression validation ";
    	PrismPropertyDefinition<OperationResultType> outputDefinition = prismContext.definitionFactory().createPropertyDefinition(ExpressionConstants.OUTPUT_ELEMENT_NAME,
			    OperationResultType.COMPLEX_TYPE);
    	Task task = serviceLocator.createSimpleTask(OPERATION_EVALUATE_EXPRESSION);
    	OperationResult result = new OperationResult(OPERATION_EVALUATE_EXPRESSION);
    	ExpressionFactory expressionFactory = serviceLocator.getExpressionFactory();
		Expression<PrismPropertyValue<OperationResultType>, PrismPropertyDefinition<OperationResultType>> expression;
		try {
			
			expression = expressionFactory
					.makeExpression(expressionType, outputDefinition, MiscSchemaUtil.getExpressionProfile(), contextDesc, task, result);
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException e) {
			ValidationError error = new ValidationError();
			error.setMessage("Cannot make expression: " + e.getMessage());
			validatable.error(error);
//			form.error("Cannot make expression: " + e.getMessage());
			return;
		}
		ExpressionVariables variables = new ExpressionVariables();
		if (valueToValidate != null) {
			variables.put(ExpressionConstants.VAR_INPUT, valueToValidate, valueToValidate.getClass());
		}
		variables.putObject(ExpressionConstants.VAR_OBJECT, (ObjectType)getObjectType(), ObjectType.class);
//		addAdditionalExpressionVariables(variables);
		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, contextDesc, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<OperationResultType>> outputTriple;
		try {
			outputTriple = expression.evaluate(context);
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
				| ConfigurationException | SecurityViolationException e) {
			ValidationError error = new ValidationError();
			error.setMessage("Cannot evaluate expression: " + e.getMessage());
			validatable.error(error);
//			form.error("Cannot evaluate expression: " + e.getMessage());
			return;
		}
		if (outputTriple == null) {
			return;
		}
		Collection<PrismPropertyValue<OperationResultType>> outputValues = outputTriple.getNonNegativeValues();
		if (outputValues.isEmpty()) {
			return;
		}
		if (outputValues.size() > 1) {
			ValidationError error = new ValidationError();
			error.setMessage("Expression "+contextDesc+" produced more than one value");
			validatable.error(error);
//			form.error("Expression "+contextDesc+" produced more than one value");
		}
		
		OperationResultType operationResultType = outputValues.iterator().next().getRealValue();
		
		if (operationResultType == null) {
			return;
		}
		
		OperationResult returnResult = OperationResult.createOperationResult(operationResultType);
		if (!returnResult.isSuccess()) {
			ValidationError error = new ValidationError();
			if (returnResult.getUserFriendlyMessage() != null) {
				error.setMessage(WebModelServiceUtils.translateMessage(returnResult, serviceLocator));				
			} else {
				error.setMessage(returnResult.getMessage());
			}
			validatable.error(error);
		}
		
	}
	
	protected <O extends ObjectType> O getObjectType() {
		return null;
	}

}
