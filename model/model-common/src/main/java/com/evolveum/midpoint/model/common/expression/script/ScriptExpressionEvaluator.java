/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.AbstractValueTransformationExpressionEvaluator;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;

import org.jetbrains.annotations.NotNull;

/**
 * Executes specified script written e.g. in Groovy, JavaScript, Python, etc. Velocity template language is supported as well.
 *
 * @author Radovan Semancik
 */
public class ScriptExpressionEvaluator<V extends PrismValue,D extends ItemDefinition>
                extends AbstractValueTransformationExpressionEvaluator<V,D,ScriptExpressionEvaluatorType> {

    private final ScriptExpression scriptExpression;

    ScriptExpressionEvaluator(QName elementName, ScriptExpressionEvaluatorType scriptType, D outputDefinition, Protector protector, PrismContext prismContext,
            ScriptExpression scriptExpression,
            SecurityContextManager securityContextManager, LocalizationService localizationService) {
        super(elementName, scriptType, outputDefinition, protector, prismContext, securityContextManager, localizationService);
        this.scriptExpression = scriptExpression;
    }

    @Override
    protected void checkEvaluatorProfile(ExpressionEvaluationContext context) {
        // Do nothing here. The profile will be checked inside ScriptExpression.
    }

    @Override
    @NotNull
    protected List<V> transformSingleValue(ExpressionVariables variables, PlusMinusZero valueDestination, boolean useNew,
            ExpressionEvaluationContext eCtx, String contextDescription, Task task, OperationResult result)
                    throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        ScriptExpressionReturnTypeType returnType = expressionEvaluatorBean.getReturnType();
        if (returnType == null && isRelative()) {
            returnType = ScriptExpressionReturnTypeType.SCALAR;
        }
        scriptExpression.setAdditionalConvertor(eCtx.getAdditionalConvertor());
        ScriptExpressionEvaluationContext sCtx = new ScriptExpressionEvaluationContext();
        sCtx.setVariables(variables);
        sCtx.setSuggestedReturnType(returnType);
        sCtx.setEvaluateNew(useNew);
        sCtx.setContextDescription(contextDescription);
        sCtx.setAdditionalConvertor(eCtx.getAdditionalConvertor());
        sCtx.setTask(task);
        sCtx.setResult(result);

        //noinspection unchecked
        return (List<V>) scriptExpression.evaluate(sCtx);
    }

    @Override
    public String shortDebugDump() {
        return "script: "+scriptExpression.toString();
    }
}
