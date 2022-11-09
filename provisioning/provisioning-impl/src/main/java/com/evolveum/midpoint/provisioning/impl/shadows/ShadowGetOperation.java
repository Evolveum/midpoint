/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.validateShadow;
import static com.evolveum.midpoint.schema.GetOperationOptions.*;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.formatExceptionMessage;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType.CONCEIVED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType.GESTATING;

import java.util.Collection;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassification;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningServiceImpl;
import com.evolveum.midpoint.provisioning.impl.shadows.errors.ErrorHandler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Implements the `get` operation.
 *
 * See {@link ProvisioningService#getObject(Class, String, Collection, Task, OperationResult)} for the full contract.
 * (The processing between {@link ProvisioningServiceImpl} and this class is minimal. So the contract there is quite relevant
 * for what is done here.)
 */
class ShadowGetOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowGetOperation.class);

    /** OID of the shadow to be gotten. */
    @NotNull private final String oid;

    /** The shadow obtained from the repository, gradually updated, fixed, futurized, and so on. */
    private ShadowType repositoryShadow;

    /** Original value of {@link #repositoryShadow}, to be used for diagnostics should that one be overwritten with `null`. */
    private ShadowType originalRepoShadow;

    /** If present, overwrites the identifiers from the {@link #repositoryShadow}. */
    @Nullable private final Collection<ResourceAttribute<?>> identifiersOverride;

    /** The "readOnly" is never set. This is because we need to modify the shadow during post-processing. */
    @Nullable private final Collection<SelectorOptions<GetOperationOptions>> options;

    @Nullable private final GetOperationOptions rootOptions;
    @NotNull private final Task task;
    @NotNull private final ShadowsLocalBeans localBeans;
    @NotNull private final XMLGregorianCalendar now;

    /** Provisioning context derived from the repository shadow. May be updated after classification (if there's one). */
    private ProvisioningContext ctx;

    ShadowGetOperation(
            @NotNull String oid,
            @Nullable ShadowType repositoryShadow,
            @Nullable Collection<ResourceAttribute<?>> identifiersOverride,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull ShadowsLocalBeans localBeans) {
        this.oid = oid;
        this.repositoryShadow = repositoryShadow;
        this.identifiersOverride = identifiersOverride;
        this.options = GetOperationOptions.updateToReadWrite(options);
        this.rootOptions = SelectorOptions.findRootOptions(this.options);
        assert !GetOperationOptions.isReadOnly(rootOptions);
        this.task = task;
        this.localBeans = localBeans;
        this.now = localBeans.clock.currentTimeXMLGregorianCalendar();
    }

    public ShadowType execute(OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {

        establishRepositoryShadow(result);

        if (!establishProvisioningContext(result)) {
            return repositoryShadow;
        }

        ctx.applyAttributesDefinition(repositoryShadow);

        if (isRaw()) {
            return repositoryShadow;
        }

        updateShadowState();

        if (isNoFetch()) {
            // Even here we want to delete expired pending operations; and delete the shadow if needed.
            doQuickShadowRefresh(result);
            return returnCached("noFetch option");
        }

        checkReadCapability();

        if (ctx.isInMaintenance()) {
            return returnCached("maintenance mode");
        }

        refreshBeforeReading(result);

        String returnCachedReason = getReasonForReturningCachedShadow();
        if (returnCachedReason != null) {
            return returnCached(returnCachedReason);
        }

        Collection<? extends ResourceAttribute<?>> identifiers = getIdentifiers();
        if (identifiers == null) {
            return returnCached("no identifiers but can return repository shadow");
        }

        try {
            ShadowType resourceObject = getResourceObject(identifiers, result);
            classifyIfNeeded(resourceObject, result);
            updateShadowInRepository(resourceObject, result);
            ShadowType shadowedObject = constructShadowedObject(resourceObject, result);
            return returnRetrieved(shadowedObject);
        } catch (ReturnCachedException e) {
            return returnCached(e.reason);
        } catch (Exception ex) {
            try {
                invokeErrorHandler(ex, result);
                if (repositoryShadow == null) {
                    throw ex;
                }
            } catch (GenericFrameworkException | ObjectAlreadyExistsException | PolicyViolationException e) {
                throw new SystemException(e.getMessage(), e);
            }
            fixOperationResult(result);
            return returnCached("exception during resource object retrieval: " + formatExceptionMessage(ex));
        }
    }

    private void establishRepositoryShadow(OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (repositoryShadow != null) {
            LOGGER.trace("Start getting '{}' (opts {}); identifiers override = {}",
                    repositoryShadow, options, identifiersOverride);
            argCheck(oid.equals(repositoryShadow.getOid()), "Provided OID is not equal to OID of repository shadow");
            if (repositoryShadow.isImmutable()) {
                repositoryShadow = repositoryShadow.clone();
            }
        } else {
            LOGGER.trace("Start getting shadow '{}' (opts {}); identifiers override = {}", oid, options, identifiersOverride);
            // Get the shadow from repository. There are identifiers that we need for accessing the object by UCF.
            repositoryShadow =
                    localBeans.repositoryService
                            .getObject(ShadowType.class, oid, disableReadOnly(options), result)
                            .asObjectable();
            LOGGER.trace("Got repository shadow object:\n{}", repositoryShadow.debugDumpLazily());
        }
        assert repositoryShadow != null;
        originalRepoShadow = repositoryShadow;
    }

    private boolean establishProvisioningContext(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException {
        assert repositoryShadow != null;
        try {
            ctx = localBeans.ctxFactory.createForShadow(repositoryShadow, task, result);
            ctx.setGetOperationOptions(options);
            ctx.assertDefinition();
            return true;
        } catch (CommonException e) {
            if (isRaw()) {
                // When using raw, return the repo shadow as it is.
                // It's better than nothing and in this case we don't even need the resource.
                // TODO maybe change assertDefinition to consider rawOption?
                result.computeStatusIfUnknown();
                result.muteError();
                localBeans.shadowCaretaker.updateShadowStateInEmergency(repositoryShadow);
                return false;
            } else {
                throw e;
            }
        }
    }

    private boolean isRaw() {
        return GetOperationOptions.isRaw(rootOptions);
    }

    private boolean isNoFetch() {
        return GetOperationOptions.isNoFetch(rootOptions);
    }

    private void updateShadowState() {
        ctx.updateShadowState(repositoryShadow);
    }

    private void checkReadCapability() {
        if (!ctx.hasReadCapability()) {
            throw new UnsupportedOperationException("Resource does not support 'read' operation");
        }
    }

    private void refreshBeforeReading(@NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, EncryptionException {
        if (isForceRefresh(rootOptions)
                || isForceRetry(rootOptions)
                || ResourceTypeUtil.isRefreshOnRead(ctx.getResource())) {
            LOGGER.trace("Doing full refresh shadow refresh before read operation for {}", repositoryShadow);
            doFullShadowRefresh(result);
        } else {
            LOGGER.trace("Full refresh is not requested, doing quick one only for {}", repositoryShadow);
            doQuickShadowRefresh(result);
        }
    }

    private void doQuickShadowRefresh(OperationResult result) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        repositoryShadow = localBeans.refreshHelper.refreshShadowQuick(ctx, repositoryShadow, now, task, result);
        if (repositoryShadow == null) {
            throw new ObjectNotFoundException(
                    "Resource object not found (after quick refresh)",
                    ShadowType.class,
                    oid);
        }
        updateShadowState();
    }

    private void doFullShadowRefresh(OperationResult result) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {
        ProvisioningOperationOptions refreshOpts = toProvisioningOperationOptions(rootOptions);
        repositoryShadow = localBeans.refreshHelper
                .refreshShadow(repositoryShadow, refreshOpts, task, result)
                .getRefreshedShadow();
        LOGGER.trace("Refreshed repository shadow:\n{}", DebugUtil.debugDumpLazily(repositoryShadow, 1));

        if (repositoryShadow == null) {
            // Most probably a dead shadow was just removed
            // TODO: is this OK? What about re-appeared objects
            LOGGER.debug("Shadow (no longer) exists: {}", originalRepoShadow);
            throw new ObjectNotFoundException("Resource object does not exist", ShadowType.class, oid);
        }

        // Refresh might change the shadow state.
        updateShadowState();
    }

    private ProvisioningOperationOptions toProvisioningOperationOptions(GetOperationOptions getOpts) {
        if (getOpts == null) {
            return null;
        }

        ProvisioningOperationOptions provisioningOpts = new ProvisioningOperationOptions();
        // for now, we are interested in forceRetry option. In the future, there can be more.
        provisioningOpts.setForceRetry(getOpts.getForceRetry());
        return provisioningOpts;
    }

    /**
     * Returns `null` in case there are no suitable identifiers but we can return the cached shadow.
     */
    private Collection<? extends ResourceAttribute<?>> getIdentifiers() throws SchemaException {
        if (identifiersOverride != null) {
            return identifiersOverride;
        } else {
            Collection<? extends ResourceAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(repositoryShadow);
            if (primaryIdentifiers == null || primaryIdentifiers.isEmpty()) {
                if (ProvisioningUtil.hasPendingAddOperation(repositoryShadow)
                        || ProvisioningUtil.hasPendingDeleteOperation(repositoryShadow)
                        || ShadowUtil.isDead(repositoryShadow)) {
                    if (ProvisioningUtil.isFuturePointInTime(options)) {
                        // Trying to get the future state of uncreated or dead shadow.
                        // But we cannot even try fetch operation here, as we do not have the identifiers.
                        // But we have quite a good idea how the shadow is going to look like.
                        // Therefore we can return it.
                        //
                        // NOTE: do NOT re-try add operation here (for "pending add" shadows). It will be retried in separate task.
                        // Re-trying the operation here would not provide big benefits and it will complicate the code.
                        return null;
                    } else {
                        // Get of uncreated shadow, but we want current state. Therefore we have to throw an error because
                        // the object does not exist yet - to our best knowledge. But we cannot really throw ObjectNotFound here.
                        // ObjectNotFound is a positive indication that the object does not exist.
                        // We do not know that for sure because resource is unavailable.
                        // The object might have been created in the meantime.
                        throw new GenericConnectorException(
                                "Unable to get object from the resource. Probably it has not been created yet because "
                                        + "of previous unavailability of the resource.");
                    }
                } else {
                    throw new SchemaException(
                            String.format("No primary identifiers found in the repository shadow %s with respect to %s",
                                    repositoryShadow, ctx.getResource()));
                }
            } else {
                return Objects.requireNonNull(
                        ShadowUtil.getAllIdentifiers(repositoryShadow));
            }
        }
    }

    private @NotNull ShadowType getResourceObject(Collection<? extends ResourceAttribute<?>> identifiers, OperationResult result)
            throws CommunicationException, SchemaException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ReturnCachedException, ObjectNotFoundException {

        InternalMonitor.recordCount(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        try {
            ShadowType resourceObject =
                    localBeans.resourceObjectConverter.getResourceObject(
                            ctx, identifiers, repositoryShadow, true, result);
            LOGGER.trace("Object returned by ResourceObjectConverter:\n{}", resourceObject.debugDumpLazily(1));
            markResourceUp(result);
            return resourceObject;
        } catch (ObjectNotFoundException e) {
            ShadowLifecycleStateType shadowState = repositoryShadow.getShadowLifecycleState();
            // This may be OK, e.g. for connectors that have running async add operation.
            if (shadowState == CONCEIVED || shadowState == GESTATING) {
                LOGGER.trace("{} was not found, but we can return cached shadow because it is in {} state",
                        repositoryShadow, shadowState);
                result.deleteLastSubresultIfError(); // we don't want to see 'warning-like' orange boxes in GUI (TODO reconsider this)
                result.recordSuccess();
                throw new ReturnCachedException("'conceived' or 'gestating' shadow was not found on resource");
            } else {
                LOGGER.trace("{} was not found, following normal error processing because shadow is in {} state",
                        repositoryShadow, shadowState);
                // This is live shadow that was not found on resource. Just re-throw the exception. It will
                // be caught later and the usual error handlers will bury the shadow.
                //
                // We re-wrap the exception (in a custom way) instead of re-throwing it or calling e.wrap() method
                // in order to provide shadow OID and "allow not found" information.
                throw new ObjectNotFoundException(
                        "Resource object for shadow " + oid + " could not be retrieved: " + e.getMessage(),
                        e,
                        ShadowType.class,
                        oid,
                        isAllowNotFound(rootOptions));
            }
        }
    }

    private void markResourceUp(OperationResult result) throws ObjectNotFoundException {
        localBeans.resourceManager.modifyResourceAvailabilityStatus(
                ctx.getResourceOid(),
                AvailabilityStatusType.UP,
                "getting " + repositoryShadow + " was successful.",
                task,
                result,
                false);
    }

    private void invokeErrorHandler(Exception cause, OperationResult result)
            throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException {
        LOGGER.debug("Handling provisioning GET exception {}: {}", cause.getClass(), cause.getMessage());
        ErrorHandler handler = localBeans.errorHandlerLocator.locateErrorHandlerRequired(cause);
        repositoryShadow = handler.handleGetError(ctx, repositoryShadow, rootOptions, cause, task, result);
        if (repositoryShadow != null) {
            // We update the shadow lifecycle state because we are not sure if the shadow after handling the exception
            // is the same as it was before (that has its state set).
            updateShadowState();
        }
    }

    /** FIXME this is probably not going to work (depending on the result handling in the lower layers) */
    private void fixOperationResult(OperationResult result) {
        if (result.getStatus() == OperationResultStatus.FATAL_ERROR) {
            // We are going to return an object. Therefore this cannot be fatal error, as at least some information
            // is returned.
            result.setStatus(OperationResultStatus.PARTIAL_ERROR);
        }
    }

    /**
     * Analogous to {@link ShadowSearchLikeOperation#processRepoShadow(PrismObject, OperationResult)}.
     *
     * TODO shouldn't we try to set the "protected" flag here (like we do in the search-like operation)?
     */
    private @NotNull ShadowType returnCached(String reason) throws SchemaException, ConfigurationException {
        LOGGER.trace("Returning cached (repository) version of shadow {} because of: {}", repositoryShadow, reason);
        repositoryShadow = ctx.futurizeShadow(repositoryShadow, null, options, now);
        ctx.applyAttributesDefinition(repositoryShadow);
        LOGGER.trace("Futurized shadow:\n{}", DebugUtil.debugDumpLazily(repositoryShadow));
        validateShadow(repositoryShadow, true);
        return repositoryShadow;
    }

    private @NotNull ShadowType returnRetrieved(@NotNull ShadowType shadowedObject)
            throws SchemaException, ConfigurationException {
        assert repositoryShadow != null;
        ShadowType shadowToReturn = ctx.futurizeShadow(repositoryShadow, shadowedObject, options, now);
        LOGGER.trace("Futurized shadowed resource object:\n{}", shadowToReturn.debugDumpLazily(1));
        validateShadow(shadowToReturn, true);
        return shadowToReturn;
    }

    @NotNull
    private ShadowType constructShadowedObject(@NotNull ShadowType resourceObject, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException, EncryptionException {
        // Complete the shadow by adding attributes from the resource object
        // This also completes the associations by adding shadowRefs
        ShadowType shadowedObject =
                localBeans.shadowedObjectConstructionHelper.constructShadowedObject(
                        ctx, repositoryShadow, resourceObject, result);
        LOGGER.trace("Shadowed resource object:\n{}", shadowedObject.debugDumpLazily(1));
        return shadowedObject;
    }

    private void classifyIfNeeded(ShadowType resourceObject, @NotNull OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (localBeans.classificationHelper.shouldClassify(ctx, repositoryShadow)) {
            ResourceObjectClassification classification =
                    localBeans.classificationHelper.classify(ctx, repositoryShadow, resourceObject, result);
            if (classification.isKnown()) {
                // TODO deduplicate this code somehow
                LOGGER.debug("Classified {} as {}", repositoryShadow, classification.getDefinition());
                repositoryShadow = localBeans.shadowManager.fixShadow(ctx, repositoryShadow, result);
                updateShadowState();
                ProvisioningContext tempCtx = ctx.spawnForShadow(repositoryShadow);
                tempCtx.applyAttributesDefinition(repositoryShadow);
            }
        }
        // Resource shadow may have different auxiliary object classes than the original repo shadow. Make sure we have the
        // definition that applies to resource shadow. We will fix repo shadow later. BUT we need also information about
        // kind/intent and these information is only in repo shadow, therefore the following 2 lines...
        resourceObject.setKind(repositoryShadow.getKind());
        resourceObject.setIntent(repositoryShadow.getIntent());
        ctx = ctx.spawnForShadow(resourceObject);
    }

    private void updateShadowInRepository(ShadowType resourceObject, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Shadow from repository:\n{}", repositoryShadow.debugDump(1));
            LOGGER.trace("Resource object fetched from resource:\n{}", resourceObject.debugDump(1));
        }
        repositoryShadow =
                localBeans.shadowManager.updateShadowInRepository(
                        ctx, resourceObject, null, repositoryShadow,
                        repositoryShadow.getShadowLifecycleState(), result);
        LOGGER.trace("Repository shadow after update:\n{}", repositoryShadow.debugDumpLazily(1));
    }

    private String getReasonForReturningCachedShadow() throws ConfigurationException {
        if (ctx.isReadingCachingOnly()) {
            return "resource is caching only";
        }
        ShadowLifecycleStateType shadowState = repositoryShadow.getShadowLifecycleState();
        if (shadowState == ShadowLifecycleStateType.TOMBSTONE) {
            // Once shadow is buried it stays nine feet under. Therefore there is no point in trying to access the resource.
            // NOTE: this is just for tombstone! Schrodinger's shadows (corpse) will still work as if they were alive.
            return "shadow is tombstone";
        }
        long stalenessOption = getStaleness(rootOptions);
        PointInTimeType pit = getPointInTimeType(rootOptions);
        if (pit == null) {
            if (stalenessOption > 0) {
                pit = PointInTimeType.CACHED;
            } else {
                pit = PointInTimeType.CURRENT;
            }
        }
        switch (pit) {
            case CURRENT:
                LOGGER.trace("We need current reliable state  -> we will NOT return cached data.");
                return null;
            case CACHED:
                if (isCachedShadowFreshEnough()) {
                    return "requested cached data";
                } else {
                    LOGGER.trace("Requested cached data, but the shadow is not fresh enough");
                    return null;
                }
            case FUTURE:
                LOGGER.trace("We could return cached, e.g. if there is a pending create operation. "
                        + "But let's try real get operation first.");
                return null;
            default:
                throw new IllegalArgumentException("Unknown point in time: " + pit);
        }
    }

    private boolean isCachedShadowFreshEnough() throws ConfigurationException {
        long stalenessOption = getStaleness(rootOptions);
        if (stalenessOption == 0L) {
            return false;
        }
        CachingMetadataType cachingMetadata = repositoryShadow.getCachingMetadata();
        if (cachingMetadata == null) {
            if (stalenessOption == Long.MAX_VALUE) {
                // We must return cached version but there is no cached version.
                throw new ConfigurationException("Cached version of "+repositoryShadow+" requested, but there is no cached value");
            }
            return false;
        }
        if (stalenessOption == Long.MAX_VALUE) {
            return true;
        }

        XMLGregorianCalendar retrievalTimestamp = cachingMetadata.getRetrievalTimestamp();
        if (retrievalTimestamp == null) {
            return false;
        }
        long retrievalTimestampMillis = XmlTypeConverter.toMillis(retrievalTimestamp);
        return localBeans.clock.currentTimeMillis() - retrievalTimestampMillis < stalenessOption;
    }

    private static class ReturnCachedException extends Exception {
        private final String reason;

        private ReturnCachedException(String reason) {
            this.reason = reason;
        }
    }
}
