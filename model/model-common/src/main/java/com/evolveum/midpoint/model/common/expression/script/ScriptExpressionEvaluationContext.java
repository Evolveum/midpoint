/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ScriptExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptEvaluationTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;

/**
 * @author semancik
 *
 */
public class ScriptExpressionEvaluationContext {

    private static final ThreadLocal<ScriptExpressionEvaluationContext> THREAD_LOCAL_CONTEXT = new ThreadLocal<>();

    private ScriptExpressionEvaluatorType expressionType;
    private ExpressionVariables variables;
    private ItemDefinition outputDefinition;
    private Function<Object, Object> additionalConvertor;
    private ScriptExpressionReturnTypeType suggestedReturnType;
    private ObjectResolver objectResolver;
    private Collection<FunctionLibrary> functions;
    private ExpressionProfile expressionProfile;
    private ScriptExpressionProfile scriptExpressionProfile;

    private ScriptExpression scriptExpression;
    private boolean evaluateNew = false;

    private String contextDescription;
    private Task task;
    private OperationResult result;

    private ScriptEvaluationTraceType trace;

    public ScriptExpressionEvaluatorType getExpressionType() {
        return expressionType;
    }

    public void setExpressionType(ScriptExpressionEvaluatorType expressionType) {
        this.expressionType = expressionType;
    }

    public ExpressionVariables getVariables() {
        return variables;
    }

    public void setVariables(ExpressionVariables variables) {
        this.variables = variables;
    }

    public ItemDefinition getOutputDefinition() {
        return outputDefinition;
    }

    public void setOutputDefinition(ItemDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
    }

    public Function<Object, Object> getAdditionalConvertor() {
        return additionalConvertor;
    }

    public void setAdditionalConvertor(Function<Object, Object> additionalConvertor) {
        this.additionalConvertor = additionalConvertor;
    }

    public ScriptExpressionReturnTypeType getSuggestedReturnType() {
        return suggestedReturnType;
    }

    public void setSuggestedReturnType(ScriptExpressionReturnTypeType suggestedReturnType) {
        this.suggestedReturnType = suggestedReturnType;
    }

    public ObjectResolver getObjectResolver() {
        return objectResolver;
    }

    public void setObjectResolver(ObjectResolver objectResolver) {
        this.objectResolver = objectResolver;
    }

    public Collection<FunctionLibrary> getFunctions() {
        return functions;
    }

    public void setFunctions(Collection<FunctionLibrary> functions) {
        this.functions = functions;
    }

    public ExpressionProfile getExpressionProfile() {
        return expressionProfile;
    }

    public void setExpressionProfile(ExpressionProfile expressionProfile) {
        this.expressionProfile = expressionProfile;
    }

    public ScriptExpressionProfile getScriptExpressionProfile() {
        return scriptExpressionProfile;
    }

    public void setScriptExpressionProfile(ScriptExpressionProfile scriptExpressionProfile) {
        this.scriptExpressionProfile = scriptExpressionProfile;
    }

    public ScriptExpression getScriptExpression() {
        return scriptExpression;
    }

    public void setScriptExpression(ScriptExpression scriptExpression) {
        this.scriptExpression = scriptExpression;
    }

    public boolean isEvaluateNew() {
        return evaluateNew;
    }

    public void setEvaluateNew(boolean evaluateNew) {
        this.evaluateNew = evaluateNew;
    }

    public String getContextDescription() {
        return contextDescription;
    }

    public void setContextDescription(String contextDescription) {
        this.contextDescription = contextDescription;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public OperationResult getResult() {
        return result;
    }

    public void setResult(OperationResult result) {
        this.result = result;
    }

    @SuppressWarnings("WeakerAccess") // Can be used e.g. from the overlay code
    public ScriptExpressionEvaluationContext setupThreadLocal() {
        ScriptExpressionEvaluationContext oldContext = THREAD_LOCAL_CONTEXT.get();
        THREAD_LOCAL_CONTEXT.set(this);
        return oldContext;
    }

    @SuppressWarnings("WeakerAccess") // Can be used e.g. from the overlay code
    public void cleanupThreadLocal(ScriptExpressionEvaluationContext oldContext) {
        THREAD_LOCAL_CONTEXT.set(oldContext);
    }

    public static ScriptExpressionEvaluationContext getThreadLocal() {
        return THREAD_LOCAL_CONTEXT.get();
    }

    public ScriptEvaluationTraceType getTrace() {
        return trace;
    }

    public void setTrace(ScriptEvaluationTraceType trace) {
        this.trace = trace;
    }
}
