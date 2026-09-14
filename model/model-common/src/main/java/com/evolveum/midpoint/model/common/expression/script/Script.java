/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;

import com.evolveum.midpoint.repo.common.expression.Expression;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.expression.ExpressionPermissionProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ScriptLanguageExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.TraceUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptEvaluationTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

/**
 * Executable form of {@link ScriptExpressionEvaluatorType}.
 *
 * Normally created as part of evaluating {@link Expression} (via {@link ScriptExpressionEvaluator}).
 * But can be also used in standalone mode by calling {@link ScriptFactory#createScript
 * (ScriptExpressionEvaluatorType, ItemDefinition, ExpressionProfile, String, OperationResult)} directly.
 *
 * @see ScriptExpressionEvaluator
 * @see ScriptExecutionContext
 *
 * @author Radovan Semancik
 */
public class Script {

    private static final String OP_EXECUTE = Script.class.getName() + ".execute";

    private final ScriptExecutor executor;
    private final ScriptExpressionEvaluatorType scriptBean;

    private ItemDefinition<?> outputDefinition;
    private ObjectResolver objectResolver;
    private Collection<FunctionLibraryBinding> functionLibraryBindings;
    private ExpressionProfile expressionProfile;
    private ScriptLanguageExpressionProfile scriptExpressionProfile;
    private PrismContext prismContext;

    private static final Trace LOGGER = TraceManager.getTrace(Script.class);
    private static final int MAX_CODE_CHARS = 42;

    Script(ScriptExecutor executor, ScriptExpressionEvaluatorType scriptBean) {
        this.scriptBean = scriptBean;
        this.executor = executor;
    }

    public ScriptExpressionEvaluatorType getScriptBean() {
        return scriptBean;
    }

    public ItemDefinition<?> getOutputDefinition() {
        return outputDefinition;
    }

    public void setOutputDefinition(ItemDefinition<?> outputDefinition) {
        this.outputDefinition = outputDefinition;
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

    void setScriptExpressionProfile(ScriptLanguageExpressionProfile scriptExpressionProfile) {
        this.scriptExpressionProfile = scriptExpressionProfile;
    }

    public ScriptLanguageExpressionProfile getScriptExpressionProfile() {
        return scriptExpressionProfile;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    /**
     * Evaluates this expression in the given context.
     *
     * The context must reference this expression. Hence, it is better to call
     * {@link ScriptExecutionContext#execute()} instead of this method.
     */
    @NotNull
    public <V extends PrismValue> List<V> execute(ScriptExecutionContext context)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        Preconditions.checkArgument(
                context.getScript() == this, "Context does not reference this expression");

        OperationResult parentResult = context.getResult();
        OperationResult result = parentResult.subresult(OP_EXECUTE)
                .setMinor()
                .addContext("context", context.getContextDescription())
                .build();
        if (result.isTracingNormal(ScriptEvaluationTraceType.class)) {
            ScriptEvaluationTraceType trace = new ScriptEvaluationTraceType();
            result.addTrace(trace);
            context.setTrace(trace);
            trace.setScriptExpressionEvaluator(context.getScriptBean());
        } else {
            context.setTrace(null);
        }
        context.setResult(result); // a bit of hack: this is to provide some tracing of script evaluation
        ScriptExecutionContext oldContext = context.setupThreadLocal();
        try {

            List<V> expressionResult = executor.execute(context);
            if (context.getTrace() != null) {
                context.getTrace().getResult().addAll(
                        TraceUtil.toAnyValueTypeList(expressionResult));
            }

            traceExpressionSuccess(context, expressionResult);
            return expressionResult;

        } catch (CommonException | RuntimeException | Error ex) {
            traceExpressionFailure(context, ex);
            result.recordException(ex);
            throw ex;
        } finally {
            context.cleanupThreadLocal(oldContext);
            result.close();
            context.setResult(parentResult); // a bit of hack
        }
    }

    private void traceExpressionSuccess(ScriptExecutionContext context, Object returnValue) {
        if (!isTrace()) {
            return;
        }
        trace("""
                        Script execution trace:
                        ---[ SCRIPT {}]---------------------------
                        Language: {}
                        Relativity mode: {}
                        Variables:
                        {}
                        Profile: {}
                        Code:
                        {}
                        Result: {}""",
                context.getContextDescription(),
                executor.getLanguageName(),
                scriptBean.getRelativityMode(),
                formatVariables(context.getVariables()),
                formatProfile(),
                formatCode(),
                SchemaDebugUtil.prettyPrint(returnValue));
    }

    private void traceExpressionFailure(ScriptExecutionContext context, Throwable exception) {
        LOGGER.error("Expression error: {}", exception.getMessage(), exception);
        if (!isTrace()) {
            return;
        }
        trace("""
                        Script execution failure:
                        ---[ SCRIPT {}]---------------------------
                        Language: {}
                        Relativity mode: {}
                        Variables:
                        {}
                        Profile: {}
                        Code:
                        {}
                        Error: {}""",
                context.getContextDescription(),
                executor.getLanguageName(),
                scriptBean.getRelativityMode(),
                formatVariables(context.getVariables()),
                formatProfile(),
                formatCode(),
                SchemaDebugUtil.prettyPrint(exception));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isTrace() {
        return isExplicitlyTraced() || LOGGER.isTraceEnabled();
    }

    private boolean isExplicitlyTraced() {
        return scriptBean != null && Boolean.TRUE.equals(scriptBean.isTrace());
    }

    private void trace(String msg, Object... args) {
        if (isExplicitlyTraced()) {
            LOGGER.info(msg, args);
        } else {
            LOGGER.trace(msg, args);
        }
    }

    private String formatVariables(VariablesMap variables) {
        if (variables == null) {
            return "null";
        }
        return variables.formatVariables();
    }

    private String formatProfile() {
        StringBuilder sb = new StringBuilder();
        if (expressionProfile != null) {
            sb.append(expressionProfile.getIdentifier());
        } else {
            sb.append("null (no profile)");
        }
        if (scriptExpressionProfile != null) {
            sb.append("; ");
            ExpressionPermissionProfile permissionProfile = scriptExpressionProfile.getPermissionProfile();
            if (permissionProfile != null) {
                sb.append("permission=").append(permissionProfile.getIdentifier());
            }
        }
        return sb.toString();
    }

    private String formatCode() {
        return DebugUtil.excerpt(scriptBean.getCode().replaceAll("[\\s\\r\\n]+", " "), MAX_CODE_CHARS);
    }

    @Override
    public String toString() {
        return "Script(" + formatCode() + ")";
    }
}
