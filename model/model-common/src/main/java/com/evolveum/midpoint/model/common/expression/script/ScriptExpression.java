/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;

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
 * FIXME review this description
 *
 * The expressions should be created by ExpressionFactory. They expect correct setting of
 * expression evaluator and proper conversion form the XML ExpressionType. Factory does this.
 *
 * FIXME resolve the duplication with {@link ScriptExpressionEvaluationContext}
 *
 * @author Radovan Semancik
 */
public class ScriptExpression {

    private static final String OP_EVALUATE = ScriptExpression.class.getName() + ".evaluate";

    private final ScriptEvaluator evaluator;
    private final ScriptExpressionEvaluatorType scriptBean;

    private ItemDefinition<?> outputDefinition;
    private Function<Object, Object> additionalConvertor;
    private ObjectResolver objectResolver;
    private Collection<FunctionLibraryBinding> functionLibraryBindings;
    private ExpressionProfile expressionProfile;
    private ScriptLanguageExpressionProfile scriptExpressionProfile;
    private PrismContext prismContext;

    private static final Trace LOGGER = TraceManager.getTrace(ScriptExpression.class);
    private static final int MAX_CODE_CHARS = 42;

    ScriptExpression(ScriptEvaluator evaluator, ScriptExpressionEvaluatorType scriptBean) {
        this.scriptBean = scriptBean;
        this.evaluator = evaluator;
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

    void setAdditionalConvertor(Function<Object, Object> additionalConvertor) {
        this.additionalConvertor = additionalConvertor;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @NotNull
    public <V extends PrismValue> List<V> evaluate(ScriptExpressionEvaluationContext context)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        if (context.getScriptBean() == null) {
            context.setScriptBean(scriptBean);
        }
        if (context.getFunctionLibraryBindings() == null) {
            context.setFunctionLibraryBindings(functionLibraryBindings);
        }
        if (context.getExpressionProfile() == null) {
            context.setExpressionProfile(expressionProfile);
        }
        if (context.getScriptExpressionProfile() == null) {
            context.setScriptExpressionProfile(scriptExpressionProfile);
        }
        if (context.getOutputDefinition() == null) {
            context.setOutputDefinition(outputDefinition);
        }
        if (context.getAdditionalConvertor() == null) {
            context.setAdditionalConvertor(additionalConvertor);
        }
        if (context.getObjectResolver() == null) {
            context.setObjectResolver(objectResolver);
        }

        OperationResult parentResult = context.getResult();
        OperationResult result = parentResult.subresult(OP_EVALUATE)
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
        ScriptExpressionEvaluationContext oldContext = context.setupThreadLocal();
        try {

            List<V> expressionResult = evaluator.evaluate(context);
            if (context.getTrace() != null) {
                context.getTrace().getResult().addAll(
                        TraceUtil.toAnyValueTypeList(expressionResult, prismContext));
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

    private void traceExpressionSuccess(ScriptExpressionEvaluationContext context, Object returnValue) {
        if (!isTrace()) {
            return;
        }
        trace("""
                        Script expression trace:
                        ---[ SCRIPT expression {}]---------------------------
                        Language: {}
                        Relativity mode: {}
                        Variables:
                        {}
                        Profile: {}
                        Code:
                        {}
                        Result: {}""",
                context.getContextDescription(),
                evaluator.getLanguageName(),
                scriptBean.getRelativityMode(),
                formatVariables(context.getVariables()),
                formatProfile(),
                formatCode(),
                SchemaDebugUtil.prettyPrint(returnValue));
    }

    private void traceExpressionFailure(ScriptExpressionEvaluationContext context, Throwable exception) {
        LOGGER.error("Expression error: {}", exception.getMessage(), exception);
        if (!isTrace()) {
            return;
        }
        trace("""
                        Script expression failure:
                        ---[ SCRIPT expression {}]---------------------------
                        Language: {}
                        Relativity mode: {}
                        Variables:
                        {}
                        Profile: {}
                        Code:
                        {}
                        Error: {}""",
                context.getContextDescription(),
                evaluator.getLanguageName(),
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
        return "ScriptExpression(" + formatCode() + ")";
    }
}
