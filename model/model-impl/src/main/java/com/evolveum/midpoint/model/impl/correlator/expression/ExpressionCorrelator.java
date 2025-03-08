/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.expression;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.model.api.correlation.CorrelationPropertyDefinition;
import com.evolveum.midpoint.model.api.correlator.Confidence;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.BaseCorrelator;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectSet;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * A correlator based on expressions that directly provide focal object(s) (or their references) for given resource object.
 * Similar to synchronization sorter, but simpler - it treats only correlation, not the classification part.
 *
 * Currently supports only shadow-based correlations.
 */
public class ExpressionCorrelator extends BaseCorrelator<ExpressionCorrelatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionCorrelator.class);

    ExpressionCorrelator(@NotNull CorrelatorContext<ExpressionCorrelatorType> correlatorContext, @NotNull ModelBeans beans) {
        super(LOGGER, "expression", correlatorContext, beans);
    }

    @Override
    public @NotNull CorrelationResult correlateInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        return new Correlation<>(correlationContext.asShadowCtx())
                .execute(result);
    }

    @Override
    protected @NotNull Confidence checkCandidateOwnerInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result) {
        throw new UnsupportedOperationException("ExpressionCorrelator is not supported in the 'opportunistic synchronization'"
                + " mode. Please disable this mode for this particular resource or object type.");
    }

    @Override
    public @NotNull Collection<CorrelationPropertyDefinition> getCorrelationPropertiesDefinitions(
            PrismObjectDefinition<? extends FocusType> focusDefinition, @NotNull Task task, @NotNull OperationResult result) {
        // Implement if really needed. But that would probably mean analyzing the expression, or declaring the properties
        // explicitly.
        return List.of();
    }

    private class Correlation<F extends FocusType> {

        @NotNull private final ShadowType resourceObject;
        @NotNull private final CorrelationContext correlationContext;
        @NotNull private final Task task;
        @NotNull private final String contextDescription;
        /** TODO: determine from the resource */
        @Nullable private final ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();

        Correlation(@NotNull CorrelationContext.Shadow correlationContext) {
            this.resourceObject = correlationContext.getResourceObject();
            this.correlationContext = correlationContext;
            this.task = correlationContext.getTask();
            this.contextDescription = getDefaultContextDescription(correlationContext);
        }

        CorrelationResult execute(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            ObjectSet<F> candidateOwners = findCandidatesUsingExpressions(result);
            return createResult(candidateOwners, null, task, result);
        }

        private @NotNull ObjectSet<F> findCandidatesUsingExpressions(OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            ExpressionType expressionBean;
            ItemDefinition<?> outputDefinition;
            if (configurationBean.getOwner() != null) {
                if (configurationBean.getOwnerRef() != null) {
                    throw new ConfigurationException("Both owner and ownerRef expressions found in " + contextDescription);
                }
                expressionBean = configurationBean.getOwner();
                outputDefinition =
                        Objects.requireNonNull(
                                PrismContext.get().getSchemaRegistry()
                                        .findObjectDefinitionByCompileTimeClass(correlationContext.getFocusType()),
                                () -> "No definition for focus type " + correlationContext.getFocusType());
            } else {
                if (configurationBean.getOwnerRef() == null) {
                    throw new ConfigurationException("Neither owner nor ownerRef expression found in " + contextDescription);
                }
                expressionBean = configurationBean.getOwnerRef();
                outputDefinition =
                        PrismContext.get().definitionFactory().newReferenceDefinition(
                                ExpressionConstants.OUTPUT_ELEMENT_NAME, ObjectReferenceType.COMPLEX_TYPE);
            }

            Expression<PrismValue, ItemDefinition<?>> expression =
                    beans.expressionFactory.makeExpression(
                            expressionBean, outputDefinition, expressionProfile, contextDescription, task, result);

            VariablesMap variables = getVariablesMap();
            ExpressionEvaluationContext eeContext =
                    new ExpressionEvaluationContext(null, variables, contextDescription, task);
            eeContext.setExpressionFactory(beans.expressionFactory);
            PrismValueDeltaSetTriple<?> outputTriple =
                    ExpressionUtil.evaluateAnyExpressionInContext(expression, eeContext, task, result);
            LOGGER.trace("Correlation expression returned:\n{}", DebugUtil.debugDumpLazily(outputTriple, 1));

            ObjectSet<F> allCandidates = new ObjectSet<>();
            if (outputTriple != null) {
                for (PrismValue candidateValue : outputTriple.getNonNegativeValues()) {
                    addCandidateOwner(allCandidates, candidateValue, result);
                }
            }

            LOGGER.debug("Found {} owner candidates for {} using correlation expression in {}: {}",
                    allCandidates.size(), resourceObject, contextDescription,
                    lazy(() -> PrettyPrinter.prettyPrint(allCandidates, 3)));

            return allCandidates;
        }

        private void addCandidateOwner(ObjectSet<F> allCandidates, PrismValue candidateValue, OperationResult result)
                throws SchemaException, ObjectNotFoundException {
            if (candidateValue == null) {
                return;
            }
            F candidateOwner;
            if (candidateValue instanceof PrismObjectValue) {
                //noinspection unchecked
                candidateOwner = ((PrismObjectValue<F>) candidateValue).asObjectable();
                checkOidPresent(candidateOwner.getOid(), candidateValue);
            } else if (candidateValue instanceof PrismReferenceValue candidateOwnerRef) {
                String oid = candidateOwnerRef.getOid();
                checkOidPresent(oid, candidateValue);
                if (allCandidates.containsOid(oid)) {
                    // This is to avoid needless resolution of the reference
                    return;
                }
                candidateOwner = resolveReference(candidateOwnerRef, result);
            } else {
                throw new IllegalStateException("Unexpected return value " + MiscUtil.getValueWithClass(candidateValue)
                        + " from correlation script in " + contextDescription);
            }
            allCandidates.add(candidateOwner);
        }

        private void checkOidPresent(String oid, PrismValue candidateValue) throws SchemaException {
            if (oid == null) {
                // Or other kind of exception?
                throw new SchemaException("No OID found in value returned from correlation script. Value: "
                        + candidateValue + " in: " + contextDescription);
            }
        }

        private F resolveReference(PrismReferenceValue candidateOwnerRef, OperationResult result)
                throws SchemaException, ObjectNotFoundException {
            Class<? extends ObjectType> type;
            if (candidateOwnerRef.getTargetType() != null) {
                type = ObjectTypes.getObjectTypeClass(candidateOwnerRef.getTargetType());
            } else {
                type = correlationContext.getFocusType();
            }
            try {
                //noinspection unchecked
                return beans.cacheRepositoryService
                        .getObject((Class<F>) type, candidateOwnerRef.getOid(), readOnly(), result)
                        .asObjectable();
            } catch (Exception e) {
                MiscUtil.throwAsSame(e, "Couldn't resolve OID returned by correlation expression: " + e.getMessage());
                throw e; // to make compiler happy
            }
        }

        @NotNull
        private VariablesMap getVariablesMap() {
            return CorrelatorUtil.getVariablesMap(null, resourceObject, correlationContext);
        }
    }
}
