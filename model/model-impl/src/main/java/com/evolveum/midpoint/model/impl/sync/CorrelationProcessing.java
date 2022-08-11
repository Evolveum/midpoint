/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.model.api.correlator.CompleteCorrelationResult;
import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlation.CorrelationCaseManager;
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Manages correlation that occurs _during synchronization pre-processing_.
 *
 * The correlation itself is delegated to appropriate {@link Correlator} instance.
 *
 * Specific responsibilities:
 *
 * 1. updating shadow with the result of the correlation
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

    /**
     * What timestamp to write as "correlation end": current timestamp if we're done, and null otherwise.
     * Kept globally to use e.g. for case cancel record. TODO not implemented yet
     */
    private XMLGregorianCalendar thisCorrelationEnd;

    CorrelationProcessing(@NotNull SynchronizationContext<F> syncCtx, @NotNull ModelBeans beans)
            throws SchemaException, ConfigurationException {
        this.syncCtx = syncCtx;
        this.task = syncCtx.getTask();
        this.beans = beans;
        this.correlationContext = new CorrelationContext(
                syncCtx.getShadowedResourceObject(),
                syncCtx.getPreFocus(),
                syncCtx.getResource(),
                syncCtx.getObjectDefinitionRequired(),
                syncCtx.getObjectTemplateForCorrelation(),
                syncCtx.getSystemConfiguration(),
                syncCtx.getTask());
        syncCtx.setCorrelationContext(correlationContext);
        this.rootCorrelatorContext =
                beans.correlationService.createRootCorrelatorContext(
                        syncCtx.getSynchronizationPolicyRequired(),
                        syncCtx.getObjectTemplateForCorrelation(),
                        syncCtx.getSystemConfigurationBean());
        this.thisCorrelationStart = XmlTypeConverter.createXMLGregorianCalendar();
    }

    @NotNull public CompleteCorrelationResult correlate(OperationResult parentResult) throws CommonException {

        assert syncCtx.getLinkedOwner() == null;

        CompleteCorrelationResult existing = getResultFromExistingState(parentResult);
        if (existing != null) {
            LOGGER.debug("Result determined from existing correlation state in shadow: {}", existing.getSituation());
            return existing;
        }

        OperationResult result = parentResult.subresult(OP_CORRELATE)
                .build();
        try {
            CompleteCorrelationResult correlationResult = correlateInRootCorrelator(result);
            applyResultToShadow(correlationResult);

            if (correlationResult.isDone()) {
                processFinalResult(result);
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

    private CompleteCorrelationResult getResultFromExistingState(OperationResult result) throws SchemaException {
        ShadowType shadow = syncCtx.getShadowedResourceObject();
        if (shadow.getCorrelation() == null) {
            return null;
        }
        CorrelationSituationType situation = shadow.getCorrelation().getSituation();
        if (situation == CorrelationSituationType.EXISTING_OWNER && shadow.getCorrelation().getResultingOwner() != null) {
            ObjectType owner = resolveExistingOwner(shadow.getCorrelation().getResultingOwner(), result);
            if (owner != null) {
                // We are not interested in other candidates here.
                return CompleteCorrelationResult.existingOwner(owner, null);
            } else {
                // Something is wrong. Let us try the correlation (again).
                // TODO perhaps we should clear the correlation state from the shadow
                return null;
            }
        } else if (situation == CorrelationSituationType.NO_OWNER) {
            return CompleteCorrelationResult.noOwner();
        } else {
            // We need to do the correlation
            return null;
        }
    }

    private @Nullable ObjectType resolveExistingOwner(@NotNull ObjectReferenceType ownerRef, OperationResult result)
            throws SchemaException {
        try {
            return beans.cacheRepositoryService.getObject(
                            ObjectTypeUtil.getTargetClassFromReference(ownerRef),
                            ownerRef.getOid(),
                            null,
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
            correlationResult = beans.correlationService.correlate(rootCorrelatorContext, correlationContext, result);
        } catch (Exception e) { // Other kinds of Throwable are intentionally passed upwards
            // The exception will be (probably) rethrown, so the stack trace is not strictly necessary here.
            LoggingUtils.logException(LOGGER, "Correlation ended with an exception", e);
            correlationResult = CompleteCorrelationResult.error(e);
        }

        LOGGER.trace("Correlation result:\n{}", correlationResult.debugDumpLazily(1));

        if (correlationResult.isDone()) {
            thisCorrelationEnd = XmlTypeConverter.createXMLGregorianCalendar();
        } else {
            thisCorrelationEnd = null;
        }

        return correlationResult;
    }

    @NotNull
    private Correlator instantiateRootCorrelator(OperationResult result) throws ConfigurationException {
        return beans.correlatorFactoryRegistry.instantiateCorrelator(rootCorrelatorContext, task, result);
    }

    private void processFinalResult(OperationResult result) throws SchemaException {
        beans.correlationCaseManager.closeCaseIfStillOpen(getShadow(), result);
        // TODO record case close if needed
    }

    private void applyResultToShadow(CompleteCorrelationResult correlationResult) throws SchemaException {
        S_ItemEntry builder = PrismContext.get().deltaFor(ShadowType.class);
        if (getShadowCorrelationStartTimestamp() == null) {
            builder = builder
                    .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_START_TIMESTAMP)
                    .replace(thisCorrelationStart);
        }
        builder = builder
                .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_END_TIMESTAMP)
                .replace(thisCorrelationEnd);
        if (correlationResult.isError()) {
            if (getShadowCorrelationSituation() == null) {
                // We set ERROR only if there is no previous situation recorded
                // ...and we set none of the other items.
                builder = builder
                        .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_SITUATION)
                        .replace(CorrelationSituationType.ERROR);
            }
        } else {
            // @formatter:off
            builder = builder
                    .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_SITUATION)
                        .replace(correlationResult.getSituation())
                    .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_OWNER_OPTIONS)
                        .replace(CloneUtil.clone(correlationResult.getOwnerOptions()))
                    .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_RESULTING_OWNER)
                        .replace(ObjectTypeUtil.createObjectRef(correlationResult.getOwner()))
                    // The following may be already applied by the correlator. But better twice than not at all.
                    .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATOR_STATE)
                        .replace(correlationContext.getCorrelatorState());
            // @formatter:on
        }

        syncCtx.addShadowDeltas(
                builder.asItemDeltas());
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

    private @Nullable XMLGregorianCalendar getShadowCorrelationCaseOpenTimestamp() {
        ShadowCorrelationStateType state = getShadowCorrelationState();
        return state != null ? state.getCorrelationCaseOpenTimestamp() : null;
    }

    private @Nullable CorrelationSituationType getShadowCorrelationSituation() {
        ShadowCorrelationStateType state = getShadowCorrelationState();
        return state != null ? state.getSituation() : null;
    }
}
