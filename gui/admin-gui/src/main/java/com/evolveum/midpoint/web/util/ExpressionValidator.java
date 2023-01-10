/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.util;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

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
import com.evolveum.midpoint.schema.expression.VariablesMap;
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

    private IModel<ExpressionType> expressionTypeModel;
    private ModelServiceLocator serviceLocator;

    private static final String OPERATION_EVALUATE_EXPRESSION = ExpressionValidator.class.getName() + ".evaluateValidationExpression";

    public ExpressionValidator(IModel<ExpressionType> expressionType, ModelServiceLocator serviceLocator) {
        this.expressionTypeModel = expressionType;
        this.serviceLocator = serviceLocator;
    }



    @Override
    public void validate(IValidatable<T> validatable) {
        ExpressionType expressionType = expressionTypeModel.getObject();

        if (expressionType == null) {
            return;
        }

        PrismContext prismContext = serviceLocator.getPrismContext();
        Object valueToValidate = getValueToValidate(validatable);
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
            return;
        }
        VariablesMap variables = new VariablesMap();
        Class typeClass = (valueToValidate == null ? String.class : valueToValidate.getClass());
        if (valueToValidate instanceof ObjectReferenceType && ((ObjectReferenceType) valueToValidate).asReferenceValue().isEmpty()) {
            valueToValidate = null;
        }
        variables.put(ExpressionConstants.VAR_INPUT, valueToValidate, typeClass);
        variables.putObject(ExpressionConstants.VAR_OBJECT, (ObjectType)getObjectType(), ObjectType.class);
        ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, contextDesc, task);
        context.setExpressionFactory(expressionFactory);
        PrismValueDeltaSetTriple<PrismPropertyValue<OperationResultType>> outputTriple;
        try {
            outputTriple = expression.evaluate(context, result);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
                | ConfigurationException | SecurityViolationException e) {
            ValidationError error = new ValidationError();
            error.setMessage("Cannot evaluate expression: " + e.getMessage());
            validatable.error(error);
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

    protected Object getValueToValidate(IValidatable<T> validatable) {
        return validatable.getValue();
    }

}
