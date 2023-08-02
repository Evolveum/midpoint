/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.helpers;

import com.evolveum.midpoint.prism.MutableItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * TODO clean up this mess
 */
@Component
public class CaseExpressionEvaluationHelper {

    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private PrismContext prismContext;

    public List<ObjectReferenceType> evaluateRefExpressions(List<ExpressionType> expressions,
            VariablesMap variables, String contextDescription,
            Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        List<ObjectReferenceType> retval = new ArrayList<>();
        for (ExpressionType expression : expressions) {
            retval.addAll(evaluateRefExpression(expression, variables, contextDescription, task, result));
        }
        return retval;
    }

    private List<ObjectReferenceType> evaluateRefExpression(ExpressionType expressionType, VariablesMap variables,
            String contextDescription, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return evaluateExpression(expressionType, variables, contextDescription, ObjectReferenceType.class,
                ObjectReferenceType.COMPLEX_TYPE, false, ExpressionUtil.createRefConvertor(UserType.COMPLEX_TYPE), task, result);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> List<T> evaluateExpression(ExpressionType expressionType, VariablesMap variables,
            String contextDescription, Class<T> clazz, QName typeName,
            boolean multiValued, Function<Object, Object> additionalConvertor, Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        MutableItemDefinition<?> resultDef;
        ItemName resultName = new ItemName(SchemaConstants.NS_C, "result");
        if (QNameUtil.match(typeName, ObjectReferenceType.COMPLEX_TYPE)) {
            resultDef = prismContext.definitionFactory().createReferenceDefinition(resultName, typeName);
        } else {
            resultDef = prismContext.definitionFactory().createPropertyDefinition(resultName, typeName);
        }
        if (multiValued) {
            resultDef.setMaxOccurs(-1);
        }
        Expression<?,?> expression = expressionFactory.makeExpression(
                expressionType, resultDef, MiscSchemaUtil.getExpressionProfile(), contextDescription, task, result);
        ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, contextDescription, task);
        context.setExpressionFactory(expressionFactory);
        context.setAdditionalConvertor(additionalConvertor);
        PrismValueDeltaSetTriple<?> exprResultTriple =
                ExpressionUtil.evaluateAnyExpressionInContext(expression, context, task, result);
        List<T> list = new ArrayList<>();
        for (PrismValue pv : exprResultTriple.getZeroSet()) {
            T realValue;
            if (pv instanceof PrismReferenceValue) {
                // pv.getRealValue sometimes returns synthesized Referencable, not ObjectReferenceType
                // If we would stay with that we would need to make many changes throughout workflow module.
                // So it is safer to stay with ObjectReferenceType.
                ObjectReferenceType ort = new ObjectReferenceType();
                ort.setupReferenceValue((PrismReferenceValue) pv);
                realValue = (T) ort;
            } else {
                realValue = pv.getRealValue();
            }
            list.add(realValue);
        }
        return list;
    }

    public boolean evaluateBooleanExpression(ExpressionType expressionType, VariablesMap VariablesMap,
            String contextDescription, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        Collection<Boolean> values = evaluateExpression(expressionType, VariablesMap, contextDescription,
                Boolean.class, DOMUtil.XSD_BOOLEAN, false, null, task, result);
        return MiscUtil.getSingleValue(values, false, contextDescription);
    }

    String evaluateStringExpression(ExpressionType expressionType, VariablesMap VariablesMap,
            String contextDescription, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        Collection<String> values = evaluateExpression(expressionType, VariablesMap, contextDescription,
                String.class, DOMUtil.XSD_STRING, false, null, task, result);
        return MiscUtil.getSingleValue(values, null, contextDescription);
    }
}
