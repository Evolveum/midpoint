/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ScriptExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptEvaluationTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;

import org.jetbrains.annotations.NotNull;

/**
 * The whole evaluation of a script: {@link ScriptExpressionEvaluatorType} compiled into {@link ScriptExpression} and evaluated.
 *
 * The "context" can be understood just like e.g. `LensContext` - the whole operation, including the script itself.
 *
 * @see ScriptEvaluator#evaluate(ScriptExpressionEvaluationContext)
 *
 * @author semancik
 */
public class ScriptExpressionEvaluationContext {

    private static final ThreadLocal<ScriptExpressionEvaluationContext> THREAD_LOCAL_CONTEXT = new ThreadLocal<>();

    private ScriptExpressionEvaluatorType scriptBean;
    private VariablesMap variables;
    private ItemDefinition<?> outputDefinition;
    private Function<Object, Object> additionalConvertor;
    private ScriptExpressionReturnTypeType suggestedReturnType;
    private ObjectResolver objectResolver;
    private Collection<FunctionLibraryBinding> functionLibraryBindings;
    private ExpressionProfile expressionProfile;
    private ScriptExpressionProfile scriptExpressionProfile;

    private ScriptExpression scriptExpression;
    private boolean evaluateNew = false;

    private String contextDescription;
    private Task task;
    private OperationResult result;

    private ScriptEvaluationTraceType trace;

    ScriptExpressionEvaluatorType getScriptBean() {
        return scriptBean;
    }

    public void setScriptBean(ScriptExpressionEvaluatorType scriptBean) {
        this.scriptBean = scriptBean;
    }

    public VariablesMap getVariables() {
        return variables;
    }

    public void setVariables(VariablesMap variables) {
        this.variables = variables;
    }

    public ItemDefinition<?> getOutputDefinition() {
        return outputDefinition;
    }

    public void setOutputDefinition(ItemDefinition<?> outputDefinition) {
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

    public Collection<FunctionLibraryBinding> getFunctionLibraryBindings() {
        return functionLibraryBindings;
    }

    public void setFunctionLibraryBindings(Collection<FunctionLibraryBinding> functionLibraryBindings) {
        this.functionLibraryBindings = functionLibraryBindings;
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

    public static @NotNull ScriptExpressionEvaluationContext getThreadLocalRequired() {
        return MiscUtil.stateNonNull(
                THREAD_LOCAL_CONTEXT.get(),
                "No ScriptExpressionEvaluationContext for current thread found");
    }

    public static @NotNull Task getTaskRequired() {
        return MiscUtil.stateNonNull(
                getThreadLocalRequired().getTask(),
                "No task in ScriptExpressionEvaluationContext for the current thread found");
    }

    public static @NotNull OperationResult getOperationResultRequired() {
        return MiscUtil.stateNonNull(
                getThreadLocalRequired().getResult(),
                "No operation result in ScriptExpressionEvaluationContext for the current thread found");
    }

    public ScriptEvaluationTraceType getTrace() {
        return trace;
    }

    public void setTrace(ScriptEvaluationTraceType trace) {
        this.trace = trace;
    }
}
