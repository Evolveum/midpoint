/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismNamespaceContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.expression.ScriptLanguageExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptEvaluationTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context in which given {@link Script} is executed. The script expression is part of the context.
 *
 * @see #execute()
 * @see Script
 *
 * @author semancik
 */
public class ScriptExecutionContext {

    private static final ThreadLocal<ScriptExecutionContext> THREAD_LOCAL_CONTEXT = new ThreadLocal<>();

    private VariablesMap variables;
    @Nullable private Function<Object, Object> additionalConvertor;
    @Nullable private ScriptExpressionReturnTypeType suggestedReturnType;

    @NotNull private final Script script;

    /**
     * Whether we are evaluating 'old' or 'new' state of things.
     * Used only when executing the script as part of {@link Expression} evaluation.
     *
     * TODO this is probably a layering violation
     */
    private boolean evaluateNew = false;

    private String contextDescription;
    private Task task;
    private OperationResult result;

    private ScriptEvaluationTraceType trace;

    @Nullable private PrismNamespaceContext namespaceContext;

    public ScriptExecutionContext(@NotNull Script script) {
        this.script = script;
    }

    public @NotNull ScriptExpressionEvaluatorType getScriptBean() {
        return script.getScriptBean();
    }

    public VariablesMap getVariables() {
        return variables;
    }

    public void setVariables(VariablesMap variables) {
        this.variables = variables;
    }

    public ItemDefinition<?> getOutputDefinition() {
        return script.getOutputDefinition();
    }

    public @Nullable Function<Object, Object> getAdditionalConvertor() {
        return additionalConvertor;
    }

    public void setAdditionalConvertor(@Nullable Function<Object, Object> additionalConvertor) {
        this.additionalConvertor = additionalConvertor;
    }

    public @Nullable ScriptExpressionReturnTypeType getSuggestedReturnType() {
        return suggestedReturnType;
    }

    public void setSuggestedReturnType(@Nullable ScriptExpressionReturnTypeType suggestedReturnType) {
        this.suggestedReturnType = suggestedReturnType;
    }

    public ObjectResolver getObjectResolver() {
        return script.getObjectResolver();
    }

    public Collection<FunctionLibraryBinding> getFunctionLibraryBindings() {
        return script.getFunctionLibraryBindings();
    }

    public @NotNull ExpressionProfile getExpressionProfile() {
        return script.getExpressionProfile();
    }

    public @NotNull ScriptLanguageExpressionProfile getScriptLanguageExpressionProfile() {
        return script.getScriptLanguageExpressionProfile();
    }

    public @NotNull Script getScript() {
        return script;
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
    public ScriptExecutionContext setupThreadLocal() {
        ScriptExecutionContext oldContext = THREAD_LOCAL_CONTEXT.get();
        THREAD_LOCAL_CONTEXT.set(this);
        return oldContext;
    }

    @SuppressWarnings("WeakerAccess") // Can be used e.g. from the overlay code
    public void cleanupThreadLocal(ScriptExecutionContext oldContext) {
        THREAD_LOCAL_CONTEXT.set(oldContext);
    }

    /**
     * Returns the {@link ScriptExecutionContext} for the current thread. This is useful when script calls
     * methods e.g. in {@link MidpointFunctions} that need to access the context.
     */
    public static ScriptExecutionContext getThreadLocal() {
        return THREAD_LOCAL_CONTEXT.get();
    }

    public static @NotNull ScriptExecutionContext getThreadLocalRequired() {
        return MiscUtil.stateNonNull(
                THREAD_LOCAL_CONTEXT.get(),
                "No script execution context for current thread found");
    }

    public static @NotNull Task getTaskRequired() {
        return MiscUtil.stateNonNull(
                getThreadLocalRequired().getTask(),
                "No task in script execution context for the current thread found");
    }

    public static @NotNull OperationResult getOperationResultRequired() {
        return MiscUtil.stateNonNull(
                getThreadLocalRequired().getResult(),
                "No operation result in script execution context for the current thread found");
    }

    public ScriptEvaluationTraceType getTrace() {
        return trace;
    }

    public void setTrace(ScriptEvaluationTraceType trace) {
        this.trace = trace;
    }

    public @Nullable PrismNamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    public void setNamespaceContext(@Nullable PrismNamespaceContext namespaceContext) {
        this.namespaceContext = namespaceContext;
    }

    /**
     * Executes the {@link #script} in this context.
     *
     * Originally {@link Script#execute(ScriptExecutionContext)} method was used. But there is a duplication of
     * parameters there, as {@link Script} and {@link ScriptExecutionContext} are to be paired together.
     */
    public @NotNull <V extends PrismValue> List<V> execute()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return script.execute(this);
    }
}
