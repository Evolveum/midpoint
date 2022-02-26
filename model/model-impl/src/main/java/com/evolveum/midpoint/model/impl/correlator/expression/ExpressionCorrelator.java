/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.expression;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.BaseCorrelator;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
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
 */
class ExpressionCorrelator extends BaseCorrelator {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionCorrelator.class);

    /**
     * Configuration of the correlator.
     */
    @NotNull private final ExpressionCorrelatorType configuration;

    /** Useful beans. */
    @NotNull private final ModelBeans beans;

    ExpressionCorrelator(@NotNull ExpressionCorrelatorType configuration, @NotNull ModelBeans beans) {
        this.configuration = configuration;
        this.beans = beans;
        LOGGER.trace("Instantiated the correlator with the configuration:\n{}", configuration.debugDumpLazily(1));
    }

    @Override
    public @NotNull CorrelationResult correlateInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        return new Correlation<>(correlationContext)
                .execute(result);
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

    private class Correlation<F extends FocusType> {

        @NotNull private final ShadowType resourceObject;
        @NotNull private final CorrelationContext correlationContext;
        @NotNull private final Task task;
        @NotNull private final String contextDescription;
        /** TODO: determine from the resource */
        @Nullable private final ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();

        Correlation(@NotNull CorrelationContext correlationContext) {
            this.resourceObject = correlationContext.getResourceObject();
            this.correlationContext = correlationContext;
            this.task = correlationContext.getTask();
            this.contextDescription =
                    ("expression correlator" +
                            (configuration.getName() != null ? " '" + configuration.getName() + "'" : ""))
                            + " for " + correlationContext.getObjectTypeDefinition().getHumanReadableName()
                            + " in " + correlationContext.getResource();
        }

        public CorrelationResult execute(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            List<F> candidateOwners = findCandidatesUsingExpressions(result);
            return beans.builtInResultCreator.createCorrelationResult(candidateOwners, correlationContext);
        }

        private @NotNull List<F> findCandidatesUsingExpressions(OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            ExpressionType expressionBean;
            ItemDefinition<?> outputDefinition;
            if (configuration.getOwner() != null) {
                if (configuration.getOwnerRef() != null) {
                    throw new ConfigurationException("Both owner and ownerRef expressions found in " + contextDescription);
                }
                expressionBean = configuration.getOwner();
                outputDefinition =
                        Objects.requireNonNull(
                                PrismContext.get().getSchemaRegistry()
                                        .findObjectDefinitionByCompileTimeClass(correlationContext.getFocusType()),
                                () -> "No definition for focus type " + correlationContext.getFocusType());
            } else {
                if (configuration.getOwnerRef() == null) {
                    throw new ConfigurationException("Neither owner nor ownerRef expression found in " + contextDescription);
                }
                expressionBean = configuration.getOwnerRef();
                outputDefinition =
                        PrismContext.get().definitionFactory().createReferenceDefinition(
                                ExpressionConstants.OUTPUT_ELEMENT_NAME, ObjectReferenceType.COMPLEX_TYPE);
            }

            Expression<PrismValue, ItemDefinition<?>> expression =
                    beans.expressionFactory.makeExpression(
                            expressionBean, outputDefinition, expressionProfile, contextDescription, task, result);

            VariablesMap variables = getVariablesMap();
            ExpressionEvaluationContext params =
                    new ExpressionEvaluationContext(null, variables, contextDescription, task);
            PrismValueDeltaSetTriple<?> outputTriple = ModelExpressionThreadLocalHolder
                    .evaluateAnyExpressionInContext(expression, params, task, result);
            LOGGER.trace("Correlation expression returned:\n{}", DebugUtil.debugDumpLazily(outputTriple, 1));

            List<F> allCandidates = new ArrayList<>();
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

        private void addCandidateOwner(List<F> allCandidates, PrismValue candidateValue, OperationResult result)
                throws SchemaException, ObjectNotFoundException {
            if (candidateValue == null) {
                return;
            }
            F candidateOwner;
            if (candidateValue instanceof PrismObjectValue) {
                //noinspection unchecked
                candidateOwner = ((PrismObjectValue<F>) candidateValue).asObjectable();
                if (containsOid(allCandidates, candidateValue, candidateOwner.getOid())) {
                    return;
                }
            } else if (candidateValue instanceof PrismReferenceValue) {
                // We first check for duplicates to avoid needless resolution of the reference
                PrismReferenceValue candidateOwnerRef = (PrismReferenceValue) candidateValue;
                if (containsOid(allCandidates, candidateValue, candidateOwnerRef.getOid())) {
                    return;
                }
                candidateOwner = resolveReference(candidateOwnerRef, result);
            } else {
                throw new IllegalStateException("Unexpected return value " + MiscUtil.getValueWithClass(candidateValue)
                        + " from correlation script in " + contextDescription);
            }
            allCandidates.add(candidateOwner);
        }

        /** Does some checking/logging besides OID presence checking. */
        private boolean containsOid(List<F> allCandidates, PrismValue candidateValue, String oid) throws SchemaException {
            if (oid == null) {
                // Or other kind of exception?
                throw new SchemaException("No OID found in value returned from correlation script. Value: "
                        + candidateValue + " in: " + contextDescription);
            }
            if (CorrelatorUtil.containsOid(allCandidates, oid)) {
                LOGGER.trace("Candidate owner {} already processed", candidateValue);
                return true;
            } else {
                LOGGER.trace("Adding {} to the list of candidate owners", candidateValue);
                return false;
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
                        .getObject((Class<F>) type, candidateOwnerRef.getOid(), null, result)
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
