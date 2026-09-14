/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.AbstractValueTransformationExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.ValueTransformationContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Executes specified script written e.g. in Groovy, JavaScript, Python, etc.
 * Apache Velocity template language is supported as well,
 *
 * It is a part of {@link Expression} and {@link ExpressionEvaluator} framework.
 *
 * This class is a bridge between the "relativity" and "script execution" aspects of the script expression evaluation,
 * using {@link Script#execute(ScriptExecutionContext)} to do the actual script execution.
 *
 * The division of labor is as follows:
 *
 * . {@link Script#execute(ScriptExecutionContext)} simply executes scripts and ignores aspects of expressions' relativity,
 * . and {@link AbstractValueTransformationExpressionEvaluator} and the super-classes deal with relativity handling (etc)
 * and ignore technical aspects of running Groovy/JS/whatever scripts.
 *
 * @author Radovan Semancik
 */
public class ScriptExpressionEvaluator<V extends PrismValue, D extends ItemDefinition<?>>
                extends AbstractValueTransformationExpressionEvaluator<V, D, ScriptExpressionEvaluatorType> {

    private final Script script;

    ScriptExpressionEvaluator(
            QName elementName, Script script, Protector protector, LocalizationService localizationService) {
        //noinspection unchecked
        super(elementName, script.getScriptBean(), (D) script.getOutputDefinition(), protector, localizationService);
        this.script = script;
    }

    @Override
    protected void checkEvaluatorProfile(ExpressionEvaluationContext context) {
        // Do nothing here. The profile will be checked inside Script.
    }

    @Override
    protected @NotNull List<V> transformSingleValue(
            @NotNull ValueTransformationContext vtCtx, @NotNull OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        var eeCtx = vtCtx.getExpressionEvaluationContext();
        ScriptExecutionContext sCtx = new ScriptExecutionContext(script);
        sCtx.setVariables(vtCtx.getVariablesMap());
        sCtx.setSuggestedReturnType(getReturnType());
        sCtx.setEvaluateNew(vtCtx.isEvaluateNew());
        sCtx.setContextDescription(vtCtx.getContextDescription());
        sCtx.setAdditionalConvertor(eeCtx.getAdditionalConvertor());
        sCtx.setTask(eeCtx.getTask());
        sCtx.setResult(result);
        sCtx.setNamespaceContext(eeCtx.getNamespaceContext());

        return sCtx.execute();
    }

    @Nullable
    private ScriptExpressionReturnTypeType getReturnType() {
        ScriptExpressionReturnTypeType explicitReturnType = expressionEvaluatorBean.getReturnType();
        if (explicitReturnType != null) {
            return explicitReturnType;
        } else if (isRelative()) {
            return ScriptExpressionReturnTypeType.SCALAR;
        } else {
            return null;
        }
    }

    @Override
    public String shortDebugDump() {
        return "script: "+ script.toString();
    }
}
