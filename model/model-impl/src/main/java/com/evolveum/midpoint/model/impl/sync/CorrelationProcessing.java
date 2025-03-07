/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;

import com.evolveum.midpoint.schema.CorrelatorDiscriminator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CompleteCorrelationResult;
import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlation.CorrelationCaseManager;
import com.evolveum.midpoint.model.impl.correlation.CorrelationServiceImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

/**
 * Manages correlation that occurs _during synchronization pre-processing_.
 *
 * The correlation itself is delegated to appropriate {@link Correlator} instance via {@link CorrelationServiceImpl}.
 *
 * Specific responsibilities:
 *
 * 1. updating shadow with the status (e.g. all the timestamps) and the result of the correlation
 * (although the {@link CorrelationCaseManager} and some other classes manipulate the state as well);
 * 2. calls {@link CorrelationCaseManager} to open, update, or cancel cases (if needed)
 */
class CorrelationProcessing<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationProcessing.class);

    private static final String OP_CORRELATE = CorrelationProcessing.class.getName() + ".correlate";

    @NotNull private final SynchronizationContext<F> syncCtx;

    @NotNull private final Task task;

    @NotNull private final ModelBeans beans;

    /** Context of the whole correlation. Used when calling the root correlator. */
    @NotNull private final CorrelationContext correlationContext;

    /** [Instantiation] context of the root correlator. */
    @NotNull private final CorrelatorContext<?> rootCorrelatorContext;

    /**
     * When this particular correlation started. Will not be propagated to the shadow if there's another
     * (presumably earlier) correlation start is already there.
     */
    @NotNull private final XMLGregorianCalendar thisCorrelationStart;

    CorrelationProcessing(@NotNull SynchronizationContext<F> syncCtx, @NotNull ModelBeans beans)
            throws SchemaException, ConfigurationException {
        this.syncCtx = syncCtx;
        this.task = syncCtx.getTask();
        this.beans = beans;
        this.correlationContext = new CorrelationContext.Shadow(
                syncCtx.getShadowedResourceObject(),
                syncCtx.getResource(),
                syncCtx.getObjectDefinitionRequired(),
                syncCtx.getPreFocus(),
                null,
                syncCtx.getSystemConfiguration(),
                syncCtx.getTask());
        syncCtx.setCorrelationContext(correlationContext);
        this.rootCorrelatorContext =
                beans.correlationServiceImpl.createRootCorrelatorContext(
                        syncCtx.getSynchronizationPolicyRequired(),
                        syncCtx.getObjectTemplateForCorrelation(),
                        CorrelatorDiscriminator.forSynchronization(),
                        syncCtx.getSystemConfigurationBean());
        this.thisCorrelationStart = XmlTypeConverter.createXMLGregorianCalendar();
    }

    @NotNull public CompleteCorrelationResult correlate(OperationResult parentResult) throws CommonException {

        assert syncCtx.getLinkedOwner() == null;

        CompleteCorrelationResult existing = getDefiniteResultFromExistingCorrelationState(parentResult);
        if (existing != null) {
            LOGGER.debug("Definite result determined from existing correlation state in shadow: {}", existing.getSituation());
            setShadowCorrelationEndTime(existing.isDone()); // isDone is true here
            return existing;
        }

        OperationResult result = parentResult.subresult(OP_CORRELATE)
                .build();
        try {
            CompleteCorrelationResult correlationResult = correlateInRootCorrelator(result);
            applyCorrelationResultToShadow(correlationResult);

            if (correlationResult.isDone()) {
                closeCaseIfNeeded(result);
            }
            result.addArbitraryObjectAsReturn("correlationResult", correlationResult);
            return correlationResult;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /** Returns non-null only if there's certain result. */
    private CompleteCorrelationResult getDefiniteResultFromExistingCorrelationState(OperationResult result)
            throws SchemaException {
        LOGGER.trace("Trying to get existing correlation result from the correlation state");
        ShadowCorrelationStateType correlation = syncCtx.getShadowedResourceObject().getCorrelation();
        if (correlation == null) {
            LOGGER.trace("-> No correlation state");
            return null;
        }
        if (correlation.getCorrelationEndTimestamp() != null) {
            LOGGER.trace("-> Existing correlation state found, but the correlation process is done. Ignoring the state:\n{}",
                    correlation.debugDumpLazily(1));
            return null;
        }
        CorrelationSituationType situation = correlation.getSituation();
        ObjectReferenceType resultingOwnerRef = correlation.getResultingOwner();
        if (situation == CorrelationSituationType.EXISTING_OWNER && resultingOwnerRef != null) {
            ObjectType owner = resolveExistingOwner(resultingOwnerRef, result);
            if (owner != null) {
                LOGGER.trace("-> Found resultingOwnerRef, and resolved it into {}", owner);
                // We are not interested in other candidates here, hence the null value into candidateOwnersMap.
                return CompleteCorrelationResult.existingOwner(owner, null, null);
            } else {
                LOGGER.trace("-> Owner reference could not be resolved -> retry the correlation.");
                return null;
            }
        } else if (situation == CorrelationSituationType.NO_OWNER) {
            LOGGER.trace("-> was resolved to 'no owner'");
            return CompleteCorrelationResult.noOwner();
        } else {
            LOGGER.trace("-> Neither 'existing owner' nor 'no owner' situation -> retry the correlation.");
            return null;
        }
    }

    private @Nullable ObjectType resolveExistingOwner(@NotNull ObjectReferenceType ownerRef, OperationResult result)
            throws SchemaException {
        try {
            return beans.cacheRepositoryService.getObject(
                            ObjectTypeUtil.getTargetClassFromReference(ownerRef),
                            ownerRef.getOid(),
                            readOnly(),
                            result)
                    .asObjectable();
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Owner reference {} cannot be resolved", ownerRef, e);
            return null;
        }
    }

    @Experimental
    void update(OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        // We throw all exceptions from the correlator. We have no structure to return the exception in.
        instantiateRootCorrelator(result)
                .update(correlationContext, result);
    }

    private @NotNull CompleteCorrelationResult correlateInRootCorrelator(OperationResult result) {

        CompleteCorrelationResult correlationResult;
        try {
            correlationResult = beans.correlationServiceImpl.correlate(rootCorrelatorContext, correlationContext, result);
        } catch (Exception e) { // Other kinds of Throwable are intentionally passed upwards
            // The exception will be (probably) rethrown, so the stack trace is not strictly necessary here.
            LoggingUtils.logException(LOGGER, "Correlation ended with an exception", e);
            correlationResult = CompleteCorrelationResult.error(e);
        }

        LOGGER.trace("Correlation result:\n{}", correlationResult.debugDumpLazily(1));
        return correlationResult;
    }

    @NotNull
    private Correlator instantiateRootCorrelator(OperationResult result) throws ConfigurationException {
        return beans.correlatorFactoryRegistry.instantiateCorrelator(rootCorrelatorContext, task, result);
    }

    private void closeCaseIfNeeded(OperationResult result) throws SchemaException {
        beans.correlationCaseManager.closeCaseIfStillOpen(getShadow(), result);
        // TODO record case close if needed
    }

    private void applyCorrelationResultToShadow(CompleteCorrelationResult correlationResult) throws SchemaException {
        S_ItemEntry builder =
                PrismContext.get().deltaFor(ShadowType.class)
                        .oldObject(syncCtx.getShadowedResourceObjectBefore())
                        .optimizing();
        XMLGregorianCalendar lastStart = getShadowCorrelationStartTimestamp();
        XMLGregorianCalendar lastEnd = getShadowCorrelationEndTimestamp();
        if (lastStart == null || lastEnd != null) {
            builder = builder
                    .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_START_TIMESTAMP)
                    .replace(thisCorrelationStart);
        }
        if (correlationResult.isError()) {
            if (getShadowCorrelationSituation() == null) {
                // We set ERROR only if there is no previous situation recorded
                // ...and we set none of the other items.
                builder = builder
                        .item(CORRELATION_SITUATION_PATH)
                        .replace(CorrelationSituationType.ERROR);
            }
        } else {
            // The default delta-optimization does not work here because of PCV IDs, so we must compare on our own.
            if (ownerOptionsChanged(correlationResult)) {
                builder = builder
                        .item(CORRELATION_OWNER_OPTIONS_PATH)
                        .replace(CloneUtil.clone(correlationResult.getOwnerOptions()));
            }

            // @formatter:off
            builder = builder
                    .item(CORRELATION_SITUATION_PATH)
                        .replace(correlationResult.getSituation())
                    .item(CORRELATION_RESULTING_OWNER_PATH)
                        .replace(ObjectTypeUtil.createObjectRef(correlationResult.getOwner()))
                    // The following may be already applied by the correlator. But better twice than not at all.
                    .item(CORRELATION_CORRELATOR_STATE_PATH)
                        .replace(correlationContext.getCorrelatorState());
            // @formatter:on
        }

        syncCtx.applyShadowDeltas(
                builder.asItemDeltas());

        setShadowCorrelationEndTime(correlationResult.isDone());
    }

    private boolean ownerOptionsChanged(CompleteCorrelationResult correlationResult) {
        var oldCorrelation = getShadow().getCorrelation();
        ResourceObjectOwnerOptionsType oldOptions = oldCorrelation != null ? oldCorrelation.getOwnerOptions() : null;
        ResourceObjectOwnerOptionsType newOptions = correlationResult.getOwnerOptions();

        if (oldOptions == null) {
            return newOptions != null;
        } else {
            if (newOptions == null) {
                return true;
            } else {
                // We have to ignore auto-generated PCV IDs
                return !oldOptions.asPrismContainerValue().equals(
                        newOptions.asPrismContainerValue(), EquivalenceStrategy.REAL_VALUE);
            }
        }
    }

    private void setShadowCorrelationEndTime(boolean done) throws SchemaException {
        if (!done && getShadowCorrelationEndTimestamp() == null) {
            return;
        }
        syncCtx.applyShadowDeltas(
                PrismContext.get().deltaFor(ShadowType.class)
                        .oldObject(syncCtx.getShadowedResourceObjectBefore())
                        .optimizing()
                        .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_END_TIMESTAMP)
                        .replace(
                                done ? XmlTypeConverter.createXMLGregorianCalendar() : null)
                        .asItemDeltas());
    }

    private @NotNull ShadowType getShadow() {
        return syncCtx.getShadowedResourceObject();
    }

    private @Nullable ShadowCorrelationStateType getShadowCorrelationState() {
        return getShadow().getCorrelation();
    }

    private @Nullable XMLGregorianCalendar getShadowCorrelationStartTimestamp() {
        ShadowCorrelationStateType state = getShadowCorrelationState();
        return state != null ? state.getCorrelationStartTimestamp() : null;
    }

    private @Nullable XMLGregorianCalendar getShadowCorrelationEndTimestamp() {
        ShadowCorrelationStateType state = getShadowCorrelationState();
        return state != null ? state.getCorrelationEndTimestamp() : null;
    }

    private @Nullable CorrelationSituationType getShadowCorrelationSituation() {
        ShadowCorrelationStateType state = getShadowCorrelationState();
        return state != null ? state.getSituation() : null;
    }
}
