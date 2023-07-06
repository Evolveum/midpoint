/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import jakarta.xml.bind.JAXBElement;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
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

/**
 * @author semancik
 */
public class Expression<V extends PrismValue, D extends ItemDefinition> {

    private final ExpressionType expressionType;
    private final D outputDefinition;
    private final ExpressionProfile expressionProfile;
    private final PrismContext prismContext;
    private final ObjectResolver objectResolver;
    private final SecurityContextManager securityContextManager;
    private final List<ExpressionEvaluator<V>> evaluators = new ArrayList<>(1);

    private static final Trace LOGGER = TraceManager.getTrace(Expression.class);

    public Expression(ExpressionType expressionType, D outputDefinition, ExpressionProfile expressionProfile, ObjectResolver objectResolver, SecurityContextManager securityContextManager, PrismContext prismContext) {
        //Validate.notNull(outputDefinition, "null outputDefinition");
        Validate.notNull(objectResolver, "null objectResolver");
        Validate.notNull(prismContext, "null prismContext");
        this.expressionType = expressionType;
        this.outputDefinition = outputDefinition;
        this.expressionProfile = expressionProfile;
        this.objectResolver = objectResolver;
        this.prismContext = prismContext;
        this.securityContextManager = securityContextManager;
    }

    public void parse(ExpressionFactory factory, String contextDescription, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        if (expressionType == null) {
            evaluators.add(createDefaultEvaluator(factory, contextDescription, task, result));
            return;
        }
        if (expressionType.getExpressionEvaluator() == null /* && expressionType.getSequence() == null */) {
            throw new SchemaException("No evaluator was specified in " + contextDescription);
        }
        if (expressionType.getExpressionEvaluator() != null) {
            ExpressionEvaluator<V> evaluator = createEvaluator(expressionType.getExpressionEvaluator(), factory,
                    contextDescription, task, result);
            evaluators.add(evaluator);
        }
        if (evaluators.isEmpty()) {
            evaluators.add(createDefaultEvaluator(factory, contextDescription, task, result));
        }
    }

    private ExpressionEvaluator<V> createEvaluator(Collection<JAXBElement<?>> evaluatorElements, ExpressionFactory factory,
            String contextDescription, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        if (evaluatorElements.isEmpty()) {
            throw new SchemaException("Empty evaluator list in " + contextDescription);
        }
        JAXBElement<?> firstEvaluatorElement = evaluatorElements.iterator().next();
        ExpressionEvaluatorFactory evaluatorFactory = factory.getEvaluatorFactory(firstEvaluatorElement.getName());
        if (evaluatorFactory == null) {
            throw new SchemaException("Unknown expression evaluator element " + firstEvaluatorElement.getName() + " in " + contextDescription);
        }
        return evaluatorFactory.createEvaluator(evaluatorElements, outputDefinition, expressionProfile, factory, contextDescription, task, result);
    }

    private ExpressionEvaluator<V> createDefaultEvaluator(ExpressionFactory factory, String contextDescription,
            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        ExpressionEvaluatorFactory evaluatorFactory = factory.getDefaultEvaluatorFactory();
        if (evaluatorFactory == null) {
            throw new SystemException("Internal error: No default expression evaluator factory");
        }
        return evaluatorFactory.createEvaluator(null, outputDefinition, expressionProfile, factory, contextDescription, task, result);
    }

    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        VariablesMap processedVariables = null;

        if (context.getExpressionProfile() == null) {
            context.setExpressionProfile(expressionProfile);
        }

        try {

            processedVariables = processInnerVariables(
                    context.getVariables(), context.getContextDescription(), context.getTask(), result);

            ExpressionEvaluationContext contextWithProcessedVariables = context.shallowClone();
            contextWithProcessedVariables.setVariables(processedVariables);
            PrismValueDeltaSetTriple<V> outputTriple;

            ObjectReferenceType runAsRef = null;
            if (expressionType != null) {
                runAsRef = expressionType.getRunAsRef();
            }

            if (runAsRef == null) {

                outputTriple = evaluateExpressionEvaluators(contextWithProcessedVariables, result);

            } else {

                UserType userType = objectResolver.resolve(runAsRef, UserType.class, null,
                        "runAs in " + context.getContextDescription(), context.getTask(), result);

                LOGGER.trace("Running {} as {} ({})", context.getContextDescription(), userType, runAsRef);

                try {
                    outputTriple = securityContextManager.runAs(() -> {
                        try {
                            return evaluateExpressionEvaluators(contextWithProcessedVariables, result);
                        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
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

    private PrismValueDeltaSetTriple<V> evaluateExpressionEvaluators(ExpressionEvaluationContext contextWithProcessedVariables,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        for (ExpressionEvaluator<?> evaluator : evaluators) {
            processEvaluatorProfile(contextWithProcessedVariables, evaluator);

            //noinspection unchecked
            PrismValueDeltaSetTriple<V> outputTriple =
                    (PrismValueDeltaSetTriple<V>) evaluator.evaluate(contextWithProcessedVariables, result);

            if (outputTriple != null) {
                boolean allowEmptyRealValues = false;
                if (expressionType != null) {
                    allowEmptyRealValues = BooleanUtils.isTrue(expressionType.isAllowEmptyValues());
                }
                outputTriple.removeEmptyValues(allowEmptyRealValues);
                if (InternalsConfig.consistencyChecks) {
                    try {
                        outputTriple.checkConsistence();
                    } catch (IllegalStateException e) {
                        throw new IllegalStateException(e.getMessage() + "; in expression " + this + ", evaluator " + evaluator, e);
                    }
                }
                return outputTriple;
            }
        }

        return null;

    }

    private void processEvaluatorProfile(ExpressionEvaluationContext context, ExpressionEvaluator<?> evaluator) throws SecurityViolationException {
        ExpressionProfile expressionProfile = context.getExpressionProfile();
        if (expressionProfile == null) {
            context.setExpressionEvaluatorProfile(null);
        } else {
            ExpressionEvaluatorProfile evaluatorProfile = expressionProfile.getEvaluatorProfile(evaluator.getElementName());
            if (evaluatorProfile == null) {
                if (expressionProfile.getDefaultDecision() == AccessDecision.ALLOW) {
                    context.setExpressionEvaluatorProfile(null);
                } else {
                    throw new SecurityViolationException("Access to expression evaluator " + evaluator.shortDebugDump() +
                            " not allowed (expression profile: " + expressionProfile.getIdentifier() + ") in " + context.getContextDescription());
                }
            } else {
                context.setExpressionEvaluatorProfile(evaluatorProfile);
            }
        }
    }

    private void traceSuccess(ExpressionEvaluationContext context, VariablesMap processedVariables, PrismValueDeltaSetTriple<V> outputTriple) {
        if (!isTrace()) {
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
        LOGGER.error("Error evaluating expression in {}: {}-{}", context.getContextDescription(), e.getMessage(), e);
        if (!isTrace()) {
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
    private boolean isTrace() {
        return LOGGER.isTraceEnabled() || (expressionType != null && expressionType.isTrace() == Boolean.TRUE);
    }

    private void trace(String msg) {
        if (expressionType != null && expressionType.isTrace() == Boolean.TRUE) {
            LOGGER.info(msg);
        } else {
            LOGGER.trace(msg);
        }
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

    private VariablesMap processInnerVariables(
            VariablesMap variables, String contextDescription, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (expressionType == null) {
            // shortcut
            return variables;
        }
        VariablesMap newVariables = new VariablesMap();

        // We need to add actor variable before we switch user identity (runAs)
        ExpressionUtil.addActorVariable(newVariables, securityContextManager, prismContext);
        boolean actorDefined = newVariables.get(ExpressionConstants.VAR_ACTOR) != null;

        for (Entry<String, TypedValue> entry : variables.entrySet()) {
            String key = entry.getKey();
            if (ExpressionConstants.VAR_ACTOR.equals(key) && actorDefined) {
                continue;            // avoid pointless warning about redefined value of actor
            }
            newVariables.put(key, entry.getValue());
            if (variables.isAlias(key)) {
                newVariables.registerAlias(key, variables.getAliasResolution(key));
            }
        }

        for (ExpressionVariableDefinitionType variableDefType : expressionType.getVariable()) {
            String varName = variableDefType.getName().getLocalPart();
            if (varName == null) {
                throw new SchemaException("No variable name in expression in " + contextDescription);
            }
            if (variableDefType.getObjectRef() != null) {
                ObjectReferenceType ref = variableDefType.getObjectRef();
                ref.setType(prismContext.getSchemaRegistry().qualifyTypeName(ref.getType()));
                ObjectType varObject = objectResolver.resolve(ref, ObjectType.class, null, "variable " + varName + " in " + contextDescription, task, result);
                newVariables.addVariableDefinition(varName, varObject, varObject.asPrismObject().getDefinition());
            } else if (variableDefType.getValue() != null) {
                // Only string is supported now
                Object valueObject = variableDefType.getValue();
                if (valueObject instanceof String) {
                    MutablePrismPropertyDefinition<Object> def = prismContext.definitionFactory().createPropertyDefinition(
                            new ItemName(SchemaConstants.NS_C, varName), PrimitiveType.STRING.getQname());
                    newVariables.addVariableDefinition(varName, valueObject, def);
                } else if (valueObject instanceof Element) {
                    MutablePrismPropertyDefinition<Object> def = prismContext.definitionFactory().createPropertyDefinition(
                            new ItemName(SchemaConstants.NS_C, varName), PrimitiveType.STRING.getQname());
                    newVariables.addVariableDefinition(varName, ((Element) valueObject).getTextContent(), def);
                } else if (valueObject instanceof RawType) {
                    ItemName varQName = new ItemName(SchemaConstants.NS_C, varName);
                    MutablePrismPropertyDefinition<Object> def = prismContext.definitionFactory().createPropertyDefinition(
                            varQName, PrimitiveType.STRING.getQname());
                    newVariables.addVariableDefinition(varName, ((RawType) valueObject).getParsedValue(null, varQName), def);
                } else {
                    throw new SchemaException("Unexpected type " + valueObject.getClass() + " in variable definition " + varName + " in " + contextDescription);
                }
            } else if (variableDefType.getPath() != null) {
                ItemPath itemPath = variableDefType.getPath().getItemPath();
                TypedValue resolvedValueAndDefinition = ExpressionUtil.resolvePathGetTypedValue(
                        itemPath,
                        variables,
                        false,
                        null,
                        objectResolver,
                        contextDescription,
                        task,
                        result);
                newVariables.put(varName, resolvedValueAndDefinition);
            } else {
                throw new SchemaException("No value for variable " + varName + " in " + contextDescription);
            }
        }

        return newVariables;
    }

    @Override
    public String toString() {
        return "Expression(expressionType=" + expressionType + ", outputDefinition=" + outputDefinition
                + ": " + shortDebugDump() + ")";
    }

    public String shortDebugDump() {
        if (evaluators.isEmpty()) {
            return "[]";
        }
        if (evaluators.size() == 1) {
            return evaluators.iterator().next().shortDebugDump();
        }
        StringBuilder sb = new StringBuilder("[");
        for (ExpressionEvaluator<V> evaluator : evaluators) {
            sb.append(evaluator.shortDebugDump());
            sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    @VisibleForTesting
    public List<ExpressionEvaluator<V>> getEvaluators() {
        return evaluators;
    }
}
