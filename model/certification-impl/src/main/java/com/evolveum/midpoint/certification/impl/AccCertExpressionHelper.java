/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.model.common.expression.*;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
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

    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertExpressionHelper.class);

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ExpressionFactory expressionFactory;

    public <T> List<T> evaluateExpressionChecked(Class<T> resultClass, ExpressionType expressionType, ExpressionVariables expressionVariables,
                                                  String shortDesc, Task task, OperationResult result) {

        try {
            return evaluateExpression(resultClass, expressionType, expressionVariables, shortDesc, task, result);
        } catch (ObjectNotFoundException|SchemaException|ExpressionEvaluationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", e, shortDesc, expressionType);
            result.recordFatalError("Couldn't evaluate " + shortDesc, e);
            throw new SystemException(e);
        }
    }

    private <T> List<T> evaluateExpression(Class<T> resultClass, ExpressionType expressionType, ExpressionVariables expressionVariables,
            String shortDesc, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        QName xsdType = XsdTypeMapper.toXsdType(resultClass);

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<T> resultDef = new PrismPropertyDefinitionImpl<>(resultName, xsdType, prismContext);

        Expression<PrismPropertyValue<T>,PrismPropertyDefinition<T>> expression = expressionFactory.makeExpression(expressionType, resultDef, shortDesc, task, result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, shortDesc, task, result);

        PrismValueDeltaSetTriple<PrismPropertyValue<T>> exprResult = ModelExpressionThreadLocalHolder.evaluateExpressionInContext(expression, params, task, result);

        List<T> retval = new ArrayList<>();
        for (PrismPropertyValue<T> item : exprResult.getZeroSet()) {
            retval.add(item.getValue());
        }
        return retval;
    }

    public List<ObjectReferenceType> evaluateRefExpressionChecked(ExpressionType expressionType,
			ExpressionVariables expressionVariables, String shortDesc, Task task, OperationResult result) {

        try {
            return evaluateRefExpression(expressionType, expressionVariables, shortDesc, task, result);
        } catch (ObjectNotFoundException|SchemaException|ExpressionEvaluationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", e, shortDesc, expressionType);
            result.recordFatalError("Couldn't evaluate " + shortDesc, e);
            throw new SystemException(e);
        }
    }

    private List<ObjectReferenceType> evaluateRefExpression(ExpressionType expressionType, ExpressionVariables expressionVariables,
			String shortDesc, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismReferenceDefinition resultDef = new PrismReferenceDefinitionImpl(resultName, ObjectReferenceType.COMPLEX_TYPE, prismContext);

        Expression<PrismReferenceValue,PrismReferenceDefinition> expression = expressionFactory.makeExpression(expressionType, resultDef, shortDesc, task, result);
        ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, expressionVariables, shortDesc, task, result);
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

    public boolean evaluateBooleanExpressionChecked(ExpressionType expressionType, ExpressionVariables expressionVariables,
                                                       String shortDesc, Task task, OperationResult result) {

        try {
            return evaluateBooleanExpression(expressionType, expressionVariables, shortDesc, task, result);
        } catch (ObjectNotFoundException|SchemaException|ExpressionEvaluationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", e, shortDesc, expressionType);
            result.recordFatalError("Couldn't evaluate " + shortDesc, e);
            throw new SystemException(e);
        }
    }

    public boolean evaluateBooleanExpression(ExpressionType expressionType, ExpressionVariables expressionVariables, String shortDesc,
			Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
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
