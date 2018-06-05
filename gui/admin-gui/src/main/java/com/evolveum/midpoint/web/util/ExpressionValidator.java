package com.evolveum.midpoint.web.util;

import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

public class ExpressionValidator<T> extends AbstractFormValidator {

	private static final long serialVersionUID = 1L;
	
	private InputPanel inputPanel;
	private ExpressionType expressionType;
	private ModelServiceLocator serviceLocator;
	private T realValue;
	
	private static final String OPERATION_EVALUATE_EXPRESSION = ExpressionValidator.class.getName() + ".evaluateValidationExpression";
	
	public ExpressionValidator(InputPanel inputPanel, T realValue, ExpressionType expressionType, ModelServiceLocator serviceLocator) {
		this.inputPanel = inputPanel;
		this.expressionType = expressionType;
		this.serviceLocator = serviceLocator;
		this.realValue = realValue;
	}

	@Override
	public FormComponent<?>[] getDependentFormComponents() {
		return new FormComponent<?>[]{inputPanel.getBaseFormComponent()};
	}

	@Override
	public void validate(Form<?> form) {
		Object valueToValidate = realValue;
		String contextDesc = " form component expression validation ";
    	PrismPropertyDefinition<OperationResultType> outputDefinition = new PrismPropertyDefinitionImpl<>(ExpressionConstants.OUTPUT_ELEMENT_NAME,
    			OperationResultType.COMPLEX_TYPE, serviceLocator.getPrismContext());
    	Task task = serviceLocator.createSimpleTask(OPERATION_EVALUATE_EXPRESSION);
    	OperationResult result = new OperationResult(OPERATION_EVALUATE_EXPRESSION);
    	ExpressionFactory expressionFactory = serviceLocator.getExpressionFactory();
		Expression<PrismPropertyValue<OperationResultType>, PrismPropertyDefinition<OperationResultType>> expression;
		try {
			expression = expressionFactory
					.makeExpression(expressionType, outputDefinition, contextDesc, task, result);
		} catch (SchemaException | ObjectNotFoundException e) {
			form.error("Cannot make expression: " + e.getMessage());
			return;
		}
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_OBJECT, valueToValidate);
//		addAdditionalExpressionVariables(variables);
		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, contextDesc, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<OperationResultType>> outputTriple;
		try {
			outputTriple = expression.evaluate(context);
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
				| ConfigurationException | SecurityViolationException e) {
			form.error("Cannot evaluate expression: " + e.getMessage());
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
			form.error("Expression "+contextDesc+" produced more than one value");
		}
		
		OperationResultType operationResultType = outputValues.iterator().next().getRealValue();
		
		if (operationResultType == null) {
			return;
		}
		
		OperationResult returnResult;
		try {
			returnResult = OperationResult.createOperationResult(operationResultType);
		} catch (SchemaException e) {
			return;
		}
	
		if (!returnResult.isSuccess()) {
			ValidationError error = new ValidationError();
//			error.setMessage(WebModelServiceUtils.translateMessage(returnResult, serviceLocator));
			if (returnResult.getUserFriendlyMessage() != null) {
				form.error(WebModelServiceUtils.translateMessage(returnResult, serviceLocator));
			} else {
				form.error(returnResult.getMessage());
			}
		}
	}
	
	

}
