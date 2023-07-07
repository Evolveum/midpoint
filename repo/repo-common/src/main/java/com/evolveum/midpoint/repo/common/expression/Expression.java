/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import java.util.List;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import jakarta.xml.bind.JAXBElement;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * "Compiled" form of {@link ExpressionType} bean.
 *
 * Instantiated through {@link ExpressionFactory#makeExpression(ExpressionType, ItemDefinition,
 * ExpressionProfile, String, Task, OperationResult)}.
 *
 * Main responsibilities:
 *
 * . parsing expression beans (with the help of respective {@link ExpressionEvaluatorFactory})
 * . invoking the expression evaluator with the following pre/post processing:
 * .. processing inner variables;
 * .. privilege switching (`runAs`, `runPrivileged`);
 * .. expression profile checking;
 * .. logfile tracing (but currently NOT trace file tracing);
 *
 * @author semancik
 */
public class Expression<V extends PrismValue, D extends ItemDefinition<?>> {

    /** The "source code" for the expression. May be null for default (currently `asIs`) case. */
    @Nullable private final ExpressionType expressionBean;

    /** Definition of the output item. Usually optional but may be required for some evaluators. */
    @Nullable private final D outputDefinition;

    /** Expression profile restricting what may and what may not be invoked in the expression. */
    @Nullable private final ExpressionProfile expressionProfile;

    /** The evaluator that contains the core of the processing. */
    @NotNull private final ExpressionEvaluator<V> evaluator;

    @NotNull private final ObjectResolver objectResolver;

    /** Can be `null` in some low-level tests. */
    @Nullable private final SecurityContextManager securityContextManager;

    private static final Trace LOGGER = TraceManager.getTrace(Expression.class);

    private Expression(
            @Nullable ExpressionType expressionBean,
            @Nullable D outputDefinition,
            @Nullable ExpressionProfile expressionProfile,
            @NotNull ExpressionEvaluator<V> evaluator,
            @NotNull ObjectResolver objectResolver,
            @Nullable SecurityContextManager securityContextManager) {

        Validate.notNull(objectResolver, "null objectResolver");

        this.expressionBean = expressionBean;
        this.outputDefinition = outputDefinition;
        this.expressionProfile = expressionProfile;
        this.evaluator = evaluator;

        this.objectResolver = objectResolver;
        this.securityContextManager = securityContextManager;
    }

    /** The only creation method. To be used through {@link ExpressionFactory} only. */
    static <V extends PrismValue, D extends ItemDefinition<?>> Expression<V, D> create(
            @Nullable ExpressionType expressionBean,
            @Nullable D outputDefinition,
            @Nullable ExpressionProfile expressionProfile,
            @NotNull ExpressionFactory factory,
            String contextDescription, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException {

        List<JAXBElement<?>> evaluatorElements = expressionBean != null ? expressionBean.getExpressionEvaluator() : List.of();

        ExpressionEvaluatorFactory evaluatorFactory;
        if (evaluatorElements.isEmpty()) {
            evaluatorFactory = stateNonNull(
                    factory.getDefaultEvaluatorFactory(),
                    "Internal error: No default expression evaluator factory");
        } else {
            QName firstEvaluatorElementName = evaluatorElements.get(0).getName();
            evaluatorFactory = configNonNull(
                    factory.getEvaluatorFactory(firstEvaluatorElementName),
                    "Unknown expression evaluator element '%s' in %s",
                    firstEvaluatorElementName, contextDescription);
        }

        return new Expression<>(
                expressionBean,
                outputDefinition,
                expressionProfile,
                evaluatorFactory.createEvaluator(
                        evaluatorElements,
                        outputDefinition,
                        expressionProfile,
                        factory,
                        contextDescription, task, result),
                factory.getObjectResolver(),
                factory.getSecurityContextManager());
    }

    public @Nullable PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        if (context.getExpressionProfile() == null) {
            context.setExpressionProfile(expressionProfile);
        }

        VariablesMap processedVariables = null;

        try {

            processedVariables = processActorAndInnerVariables(
                    context.getVariables(), context.getContextDescription(), context.getTask(), result);

            ExpressionEvaluationContext contextWithProcessedVariables = context.shallowClone();
            contextWithProcessedVariables.setVariables(processedVariables);
            PrismValueDeltaSetTriple<V> outputTriple;

            ObjectReferenceType runAsRef = expressionBean != null ? expressionBean.getRunAsRef() : null;

            if (runAsRef == null) {

                outputTriple = runExpressionEvaluator(contextWithProcessedVariables, result);

            } else {

                // TODO generalize to FocusType
                UserType userType = objectResolver.resolve(runAsRef, UserType.class, null,
                        "runAs in " + context.getContextDescription(), context.getTask(), result);

                LOGGER.trace("Running {} as {} ({})", context.getContextDescription(), userType, runAsRef);

                try {
                    assert securityContextManager != null; // low-level tests do not execute this code
                    outputTriple = securityContextManager.runAs(() -> {
                        try {
                            return runExpressionEvaluator(contextWithProcessedVariables, result);
                        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException
                                | CommunicationException | ConfigurationException | SecurityViolationException e) {
                            throw new TunnelException(e);
                        }
                    }, userType.asPrismObject());
                } catch (TunnelException te) {
                    return unwrapTunnelException(te);
                }
            }

            traceSuccess(context, processedVariables, outputTriple);
            return outputTriple;

        } catch (Throwable e) {
            traceFailure(context, processedVariables, e);
            throw e;
        }
    }

    private PrismValueDeltaSetTriple<V> unwrapTunnelException(TunnelException te)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        Throwable e = te.getCause();
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        if (e instanceof Error) {
            throw (Error) e;
        }
        if (e instanceof SchemaException) {
            throw (SchemaException) e;
        }
        if (e instanceof ExpressionEvaluationException) {
            throw (ExpressionEvaluationException) e;
        }
        if (e instanceof ObjectNotFoundException) {
            throw (ObjectNotFoundException) e;
        }
        if (e instanceof CommunicationException) {
            throw (CommunicationException) e;
        }
        if (e instanceof ConfigurationException) {
            throw (ConfigurationException) e;
        }
        if (e instanceof SecurityViolationException) {
            throw (SecurityViolationException) e;
        }
        throw te;
    }

    private @Nullable PrismValueDeltaSetTriple<V> runExpressionEvaluator(
            ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        context.setExpressionEvaluatorProfile(
                determineExpressionEvaluatorProfile(context));

        PrismValueDeltaSetTriple<V> outputTriple = evaluator.evaluate(context, result);

        if (outputTriple == null) {
            return null;
        }

        outputTriple.removeEmptyValues(isAllowEmptyValues());

        checkOutputTripleConsistence(outputTriple);

        return outputTriple;
    }

    private ExpressionEvaluatorProfile determineExpressionEvaluatorProfile(ExpressionEvaluationContext context)
            throws SecurityViolationException {
        ExpressionProfile expressionProfile = context.getExpressionProfile();
        if (expressionProfile == null) {
            return null; // everything is allowed
        }

        ExpressionEvaluatorProfile evaluatorProfile = expressionProfile.getEvaluatorProfile(evaluator.getElementName());
        if (evaluatorProfile != null) {
            return evaluatorProfile; // evaluator profile will sort everything out, no need to decide here
        }

        if (expressionProfile.getDefaultDecision() == AccessDecision.ALLOW) {
            return null; // no evaluator profile, but we are allowed at the expression level
        } else {
            throw new SecurityViolationException(
                    "Access to expression evaluator %s not allowed (expression profile: %s) in %s".formatted(
                            evaluator.shortDebugDump(), expressionProfile.getIdentifier(), context.getContextDescription()));
        }
    }

    private boolean isAllowEmptyValues() {
        return expressionBean != null && BooleanUtils.isTrue(expressionBean.isAllowEmptyValues());
    }

    private void checkOutputTripleConsistence(PrismValueDeltaSetTriple<V> outputTriple) {
        if (InternalsConfig.consistencyChecks) {
            try {
                outputTriple.checkConsistence();
            } catch (IllegalStateException e) {
                throw new IllegalStateException(e.getMessage() + "; in expression " + this + ", evaluator " + evaluator, e);
            }
        }
    }

    private void traceSuccess(
            ExpressionEvaluationContext context, VariablesMap processedVariables, PrismValueDeltaSetTriple<V> outputTriple) {
        if (!isTraced()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Expression trace:\n");
        appendTraceHeader(sb, context, processedVariables);
        sb.append("\nResult: ");
        if (outputTriple == null) {
            sb.append("null");
        } else {
            sb.append(outputTriple.toHumanReadableString());
        }
        appendTraceFooter(sb);
        trace(sb.toString());
    }

    private void traceFailure(ExpressionEvaluationContext context, VariablesMap processedVariables, Throwable e) {
        LOGGER.error("Error evaluating expression in {}: {}", context.getContextDescription(), e.getMessage(), e);
        if (!isTraced()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Expression failure:\n");
        appendTraceHeader(sb, context, processedVariables);
        sb.append("\nERROR: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
        appendTraceFooter(sb);
        trace(sb.toString());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isTraced() {
        return isExplicitlyTraced() || LOGGER.isTraceEnabled();
    }

    private void trace(String msg) {
        if (isExplicitlyTraced()) {
            LOGGER.info(msg);
        } else {
            LOGGER.trace(msg);
        }
    }

    private boolean isExplicitlyTraced() {
        return expressionBean != null && Boolean.TRUE.equals(expressionBean.isTrace());
    }

    private void appendTraceHeader(StringBuilder sb, ExpressionEvaluationContext context, VariablesMap processedVariables) {
        sb.append("---[ EXPRESSION in ");
        sb.append(context.getContextDescription());
        sb.append("]---------------------------");
        sb.append("\nSources:");
        for (Source<?, ?> source : context.getSources()) {
            sb.append("\n");
            sb.append(source.debugDump(1));
        }
        sb.append("\nVariables:");
        if (processedVariables == null) {
            sb.append(" null");
        } else {
            sb.append("\n");
            sb.append(processedVariables.debugDump(1));
        }
        sb.append("\nOutput definition: ").append(MiscUtil.toString(outputDefinition));
        if (context.getExpressionProfile() != null) {
            sb.append("\nExpression profile: ").append(context.getExpressionProfile().getIdentifier());
        }
        sb.append("\nEvaluators: ");
        sb.append(shortDebugDump());
    }

    private void appendTraceFooter(StringBuilder sb) {
        sb.append("\n------------------------------------------------------");
    }

    private VariablesMap processActorAndInnerVariables(
            VariablesMap variables, String contextDescription, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (expressionBean == null) {
            return variables; // no expression, no need to deal with variables
        }

        // Intentionally before "runAs" is executed.
        VariablesMap newVariables = variables.shallowClone();
        ExpressionUtil.addActorVariableIfNeeded(newVariables, securityContextManager);

        // Inner variables
        for (ExpressionVariableDefinitionType variableDefBean : expressionBean.getVariable()) {

            String varName =
                    configNonNull(variableDefBean.getName(), "no variable name in expression in %s", contextDescription)
                            .getLocalPart();

            ObjectReferenceType objectRef = variableDefBean.getObjectRef();
            if (objectRef != null) {
                objectRef.setType(PrismContext.get().getSchemaRegistry().qualifyTypeName(objectRef.getType()));
                ObjectType varObject = objectResolver.resolve(
                        objectRef,
                        ObjectType.class,
                        null,
                        "variable " + varName + " in " + contextDescription,
                        task, result);
                newVariables.addVariableDefinition(varName, varObject, varObject.asPrismObject().getDefinition());
                continue;
            }

            Object value = variableDefBean.getValue();
            if (value != null) {
                ItemName varQName = new ItemName(SchemaConstants.NS_C, varName);
                // Only String values are supported now
                var def = PrismContext.get().definitionFactory()
                        .createPropertyDefinition(varQName, PrimitiveType.STRING.getQname());
                Object variableValue;
                if (value instanceof String) {
                    variableValue = value;
                } else if (value instanceof Element element) {
                    variableValue = element.getTextContent();
                } else if (value instanceof RawType raw) {
                    variableValue = raw.getParsedValue(null, varQName);
                } else {
                    throw new ConfigurationException(
                            "Unexpected type %s in variable '%s' definition in %s".formatted(
                                    value.getClass(), varName, contextDescription));
                }
                newVariables.addVariableDefinition(varName, variableValue, def);
                continue;
            }

            ItemPathType pathBean = variableDefBean.getPath();
            if (pathBean != null) {
                TypedValue<?> resolvedValueAndDefinition = ExpressionUtil.resolvePathGetTypedValue(
                        pathBean.getItemPath(),
                        variables,
                        false,
                        null,
                        objectResolver,
                        contextDescription,
                        task, result);
                newVariables.put(varName, resolvedValueAndDefinition);
                continue;
            }

            throw new SchemaException(
                    "No value for variable '%s' in %s".formatted(varName, contextDescription));
        }

        return newVariables;
    }

    @Override
    public String toString() {
        return "Expression(expressionType=" + expressionBean + ", outputDefinition=" + outputDefinition
                + ": " + shortDebugDump() + ")";
    }

    public String shortDebugDump() {
        return evaluator.shortDebugDump();
    }
}
