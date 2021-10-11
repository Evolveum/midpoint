/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class AccCertExpressionHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AccCertExpressionHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;

    @SuppressWarnings("SameParameterValue")
    private <T> List<T> evaluateExpression(Class<T> resultClass, ExpressionType expressionType, ExpressionVariables expressionVariables,
            String shortDesc, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        QName xsdType = XsdTypeMapper.toXsdType(resultClass);

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<T> resultDef = prismContext.definitionFactory().createPropertyDefinition(resultName, xsdType);

        Expression<PrismPropertyValue<T>,PrismPropertyDefinition<T>> expression = expressionFactory.makeExpression(expressionType, resultDef, MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, shortDesc, task);

        PrismValueDeltaSetTriple<PrismPropertyValue<T>> exprResult = ModelExpressionThreadLocalHolder.evaluateExpressionInContext(expression, params, task, result);

        List<T> retval = new ArrayList<>();
        for (PrismPropertyValue<T> item : exprResult.getZeroSet()) {
            retval.add(item.getValue());
        }
        return retval;
    }

    List<ObjectReferenceType> evaluateRefExpressionChecked(ExpressionType expressionType,
            ExpressionVariables expressionVariables, String shortDesc, Task task, OperationResult result) {
        try {
            return evaluateRefExpression(expressionType, expressionVariables, shortDesc, task, result);
        } catch (CommonException|RuntimeException e) {
            LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", e, shortDesc, expressionType);
            result.recordFatalError("Couldn't evaluate " + shortDesc, e);
            throw new SystemException(e);
        }
    }

    private List<ObjectReferenceType> evaluateRefExpression(ExpressionType expressionType, ExpressionVariables expressionVariables,
            String shortDesc, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismReferenceDefinition resultDef = prismContext.definitionFactory().createReferenceDefinition(resultName, ObjectReferenceType.COMPLEX_TYPE);

        Expression<PrismReferenceValue,PrismReferenceDefinition> expression = expressionFactory.makeExpression(expressionType, resultDef, MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);
        ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, expressionVariables, shortDesc, task);
        context.setAdditionalConvertor(ExpressionUtil.createRefConvertor(UserType.COMPLEX_TYPE));
        PrismValueDeltaSetTriple<PrismReferenceValue> exprResult =
                ModelExpressionThreadLocalHolder.evaluateRefExpressionInContext(expression, context, task, result);

        List<ObjectReferenceType> retval = new ArrayList<>();
        for (PrismReferenceValue value : exprResult.getZeroSet()) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setupReferenceValue(value);
            retval.add(ort);
        }
        return retval;
    }

//    public boolean evaluateBooleanExpressionChecked(ExpressionType expressionType, ExpressionVariables expressionVariables,
//                                                       String shortDesc, Task task, OperationResult result) {
//
//        try {
//            return evaluateBooleanExpression(expressionType, expressionVariables, shortDesc, task, result);
//        } catch (ObjectNotFoundException|SchemaException|ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
//            LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", e, shortDesc, expressionType);
//            result.recordFatalError("Couldn't evaluate " + shortDesc, e);
//            throw new SystemException(e);
//        }
//    }

    public boolean evaluateBooleanExpression(ExpressionType expressionType, ExpressionVariables expressionVariables, String shortDesc,
            Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        List<Boolean> exprResult = evaluateExpression(Boolean.class, expressionType, expressionVariables, shortDesc, task, result);
        if (exprResult.size() == 0) {
            return false;
        } else if (exprResult.size() > 1) {
            throw new IllegalStateException("Filter expression should return exactly one boolean value; it returned " + exprResult.size() + " ones");
        }
        Boolean boolResult = exprResult.get(0);
        return boolResult != null ? boolResult : false;
    }
}
