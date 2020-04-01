/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.expression.ExpressionPermissionProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ScriptExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.TraceUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * The expressions should be created by ExpressionFactory. They expect correct setting of
 * expression evaluator and proper conversion form the XML ExpressionType. Factory does this.
 *
 * @author Radovan Semancik
 */
public class ScriptExpression {

    private static final String OP_EVALUATE = ScriptExpression.class.getName() + ".evaluate";

    private ScriptExpressionEvaluatorType scriptType;
    private ScriptEvaluator evaluator;
    private ItemDefinition outputDefinition;
    private Function<Object, Object> additionalConvertor;
    private ObjectResolver objectResolver;
    private Collection<FunctionLibrary> functions;
    private ExpressionProfile expressionProfile;
    private ScriptExpressionProfile scriptExpressionProfile;
    private PrismContext prismContext;

    private static final Trace LOGGER = TraceManager.getTrace(ScriptExpression.class);
    private static final int MAX_CODE_CHARS = 42;

    ScriptExpression(ScriptEvaluator evaluator, ScriptExpressionEvaluatorType scriptType) {
        this.scriptType = scriptType;
        this.evaluator = evaluator;
    }

    public ItemDefinition getOutputDefinition() {
        return outputDefinition;
    }

    public void setOutputDefinition(ItemDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
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

    public Function<Object, Object> getAdditionalConvertor() {
        return additionalConvertor;
    }

    public void setAdditionalConvertor(Function<Object, Object> additionalConvertor) {
        this.additionalConvertor = additionalConvertor;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @Deprecated
    public <V extends PrismValue> List<V> evaluate(ExpressionVariables variables, ScriptExpressionReturnTypeType suggestedReturnType,
            boolean useNew, String contextDescription, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

        ScriptExpressionEvaluationContext context = new ScriptExpressionEvaluationContext();
        context.setExpressionType(scriptType);
        context.setVariables(variables);
        context.setFunctions(functions);
        context.setExpressionProfile(expressionProfile);
        context.setScriptExpressionProfile(scriptExpressionProfile);
        context.setOutputDefinition(outputDefinition);
        context.setAdditionalConvertor(additionalConvertor);
        context.setSuggestedReturnType(suggestedReturnType);
        context.setObjectResolver(objectResolver);
        context.setEvaluateNew(useNew);
        context.setScriptExpression(this);
        context.setContextDescription(contextDescription);
        context.setTask(task);
        context.setResult(result);

        return evaluate(context);
    }

    public <V extends PrismValue> List<V> evaluate(ScriptExpressionEvaluationContext context)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

        if (context.getExpressionType() == null) {
            context.setExpressionType(scriptType);
        }
        if (context.getFunctions() == null) {
            context.setFunctions(functions);
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
            ScriptEvaluationTraceType trace = new ScriptEvaluationTraceType(prismContext);
            result.addTrace(trace);
            context.setTrace(trace);
            trace.setScriptExpressionEvaluator(context.getExpressionType());
        } else {
            context.setTrace(null);
        }
        context.setResult(result);      // a bit of hack: this is to provide some tracing of script evaluation
        ScriptExpressionEvaluationContext oldContext = context.setupThreadLocal();
        try {

            List<V> expressionResult = evaluator.evaluate(context);
            if (context.getTrace() != null) {
                context.getTrace().getResult().addAll(TraceUtil.toAnyValueTypeList(expressionResult, prismContext));
            }

            traceExpressionSuccess(context, expressionResult);
            return expressionResult;

        } catch (ExpressionEvaluationException | ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | RuntimeException | Error ex) {
            traceExpressionFailure(context, ex);
            result.recordFatalError(ex.getMessage(), ex);
            throw ex;
        } finally {
            context.cleanupThreadLocal(oldContext);
            result.computeStatusIfUnknown();
            context.setResult(parentResult);        // a bit of hack
        }
    }

    private void traceExpressionSuccess(ScriptExpressionEvaluationContext context, Object returnValue) {
        if (!isTrace()) {
            return;
        }
        trace("Script expression trace:\n"+
                "---[ SCRIPT expression {}]---------------------------\n"+
                "Language: {}\n"+
                "Relativity mode: {}\n"+
                "Variables:\n{}\n"+
                "Profile: {}\n" +
                "Code:\n{}\n"+
                "Result: {}", context.getContextDescription(), evaluator.getLanguageName(), scriptType.getRelativityMode(), formatVariables(context.getVariables()),
                formatProfile(), formatCode(), SchemaDebugUtil.prettyPrint(returnValue));
    }

    private void traceExpressionFailure(ScriptExpressionEvaluationContext context, Throwable exception) {
        LOGGER.error("Expression error: {}", exception.getMessage(), exception);
        if (!isTrace()) {
            return;
        }
        trace("Script expression failure:\n"+
                "---[ SCRIPT expression {}]---------------------------\n"+
                "Language: {}\n"+
                "Relativity mode: {}\n"+
                "Variables:\n{}\n"+
                "Profile: {}\n" +
                "Code:\n{}\n"+
                "Error: {}", context.getContextDescription(), evaluator.getLanguageName(), scriptType.getRelativityMode(), formatVariables(context.getVariables()),
                formatProfile(),formatCode(), SchemaDebugUtil.prettyPrint(exception));
    }

    private boolean isTrace() {
        return LOGGER.isTraceEnabled() || (scriptType != null && scriptType.isTrace() == Boolean.TRUE);
    }

    private void trace(String msg, Object... args) {
        if (scriptType != null && scriptType.isTrace() == Boolean.TRUE) {
            LOGGER.info(msg, args);
        } else {
            LOGGER.trace(msg, args);
        }
    }

    private String formatVariables(ExpressionVariables variables) {
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
        return DebugUtil.excerpt(scriptType.getCode().replaceAll("[\\s\\r\\n]+", " "), MAX_CODE_CHARS);
    }

    @Override
    public String toString() {
        return "ScriptExpression(" + formatCode() + ")";
    }

}
