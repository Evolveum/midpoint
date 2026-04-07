/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.sync;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

import javax.xml.datatype.XMLGregorianCalendar;

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
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.CorrelatorDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
            setShadowCorrelationEndTime(existing.isCertain()); // isCertain is true here
            return existing;
        }

        OperationResult result = parentResult.subresult(OP_CORRELATE)
                .build();
        try {
            CompleteCorrelationResult correlationResult = correlateInRootCorrelator(result);
            syncCtx.applyShadowDeltas(correlationResult.toDeltaItems(this.beans.prismContext, getShadow()));

            if (correlationResult.isCertain()) {
                closeCaseIfNeeded(result);
            }
            result.addArbitraryObjectAsReturn("correlationResult", correlationResult);
            return correlationResult;
        } catch (Exception e) {
            result.recordFatalError(e);
            throw e;
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
            LOGGER.trace("-> Existing correlation state found, but the correlation process is done. Ignoring the "
                            + "state:\n{}",
                    correlation.debugDumpLazily(1));
            return null;
        }
        CorrelationSituationType situation = correlation.getSituation();
        ObjectReferenceType resultingOwnerRef = correlation.getResultingOwner();
        if (situation == CorrelationSituationType.EXISTING_OWNER && resultingOwnerRef != null) {
            ObjectType owner = resolveExistingOwner(resultingOwnerRef, result);
            if (owner != null) {
                LOGGER.trace("-> Found resultingOwnerRef, and resolved it into {}", owner);
                return CompleteCorrelationResult
                        .builderForExistingOwner(thisCorrelationStart, XmlTypeConverter.createXMLGregorianCalendar())
                        .owner(owner)
                        // We are not interested in other candidates here, so build it with just the owner.
                        .build();
            } else {
                LOGGER.trace("-> Owner reference could not be resolved -> retry the correlation.");
                return null;
            }
        } else if (situation == CorrelationSituationType.NO_OWNER) {
            LOGGER.trace("-> was resolved to 'no owner'");
            return CompleteCorrelationResult
                    .builderForNoOwner(thisCorrelationStart, XmlTypeConverter.createXMLGregorianCalendar())
                    .build();
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
            correlationResult = beans.correlationServiceImpl.correlate(rootCorrelatorContext, correlationContext,
                    result);
        } catch (Exception e) { // Other kinds of Throwable are intentionally passed upwards
            // The exception will be (probably) rethrown, so the stack trace is not strictly necessary here.
            LoggingUtils.logException(LOGGER, "Correlation ended with an exception", e);
            correlationResult = CompleteCorrelationResult
                    .builderForError(thisCorrelationStart, XmlTypeConverter.createXMLGregorianCalendar())
                    .error(e)
                    .build();
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

    private void setShadowCorrelationEndTime(boolean correlationIsCertain) throws SchemaException {
        if (!correlationIsCertain && getShadowCorrelationEndTimestamp() == null) {
            // If the current correlation is **not** certain, and the end timestamp is null (means previous
            // correlation wasn't certain as well), then we will not update the end timestamp (leave it empty).
            return;
        }
        // If the current correlation is **not** certain about the result, but the end timestamp is set (means
        // that some previous correlation was certain), we will erase the end timestamp.
        // If the current correlation **is** certain about the result, then we change the end timestamp to current time.
        syncCtx.applyShadowDeltas(
                PrismContext.get().deltaFor(ShadowType.class)
                        .oldObject(syncCtx.getShadowedResourceObjectBefore())
                        .optimizing()
                        .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_END_TIMESTAMP)
                        .replace(
                                correlationIsCertain ? XmlTypeConverter.createXMLGregorianCalendar() : null)
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
