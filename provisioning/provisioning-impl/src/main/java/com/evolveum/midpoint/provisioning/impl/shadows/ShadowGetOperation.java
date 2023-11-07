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
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.CompleteResourceObject;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningServiceImpl;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
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

    private static final String OP_GET_RESOURCE_OBJECT = ShadowGetOperation.class + ".getResourceObject";

    private static final Trace LOGGER = TraceManager.getTrace(ShadowGetOperation.class);

    /** Provisioning context derived from the repository shadow. May be updated after classification (if there's one). */
    @NotNull private ProvisioningContext ctx;

    /** OID of the shadow to be gotten. */
    @NotNull private final String oid;

    /**
     * The shadow obtained from the repository, gradually updated, fixed, futurized, and so on.
     * If the shadow disappears (currently, it can happen during error processing), this field
     * is not cleared, but an exception is thrown.
     */
    @NotNull private ShadowType repositoryShadow;

    /** Original value of {@link #repositoryShadow}, to be used for diagnostics should that one be overwritten with `null`. */
    @NotNull private final ShadowType originalRepoShadow;

    /** If present, overwrites the identifiers from the {@link #repositoryShadow}. */
    @Nullable private final Collection<ResourceAttribute<?>> identifiersOverride;

    /** The "readOnly" is never set. This is because we need to modify the shadow during post-processing. */
    @Nullable private final Collection<SelectorOptions<GetOperationOptions>> options;

    @Nullable private final GetOperationOptions rootOptions;

    @NotNull private final ShadowsLocalBeans b = ShadowsLocalBeans.get();

    @NotNull private final XMLGregorianCalendar now;

    private ShadowGetOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType repositoryShadow,
            @Nullable Collection<ResourceAttribute<?>> identifiersOverride,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options) {
        this.ctx = ctx;
        this.oid = repositoryShadow.getOid();
        this.repositoryShadow = repositoryShadow;
        this.originalRepoShadow = repositoryShadow;
        this.identifiersOverride = identifiersOverride;
        this.options = GetOperationOptions.updateToReadWrite(options);
        this.rootOptions = SelectorOptions.findRootOptions(this.options);
        assert !GetOperationOptions.isReadOnly(rootOptions);
        this.now = b.clock.currentTimeXMLGregorianCalendar();
    }

    static ShadowType execute(
            @NotNull String oid,
            @Nullable ShadowType providedRepositoryShadow,
            @Nullable Collection<ResourceAttribute<?>> identifiersOverride,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, EncryptionException {
        ShadowType repositoryShadow = obtainRepositoryShadow(oid, providedRepositoryShadow, options, result);
        ProvisioningContext ctx = createProvisioningContext(repositoryShadow, options, context, task, result);
        return new ShadowGetOperation(ctx, repositoryShadow, identifiersOverride, options)
                .executeInternal(result);
    }

    private ShadowType executeInternal(OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {

        if (isRaw()) {
            if (ctx.hasDefinition()) {
                ctx.applyAttributesDefinition(repositoryShadow);
            } else {
                LOGGER.trace("Definition-less provisioning context is accepted because of the raw mode");
            }
            return repositoryShadow;
        }

        ctx.assertDefinition();
        ctx.adoptShadow(repositoryShadow);

        if (isNoFetch()) {
            // Even here we want to delete expired pending operations; and delete the shadow if needed.
            doQuickShadowRefresh(parentResult);
            return returnCached("noFetch option");
        }

        checkReadCapability();

        if (ctx.isInMaintenance()) {
            parentResult.setPartialError("Resource is in maintenance mode");
            return returnCached("maintenance mode");
        }

        refreshBeforeReading(parentResult);

        String returnCachedReason = getReasonForReturningCachedShadow();
        if (returnCachedReason != null) {
            return returnCached(returnCachedReason);
        }

        var identification = getPrimaryIdentification();
        if (identification == null) {
            return returnCached("no primary identifiers but can return repository shadow");
        }

        ResourceObject resourceObject;
        OperationResult result = parentResult.createSubresult(OP_GET_RESOURCE_OBJECT);
        result.addArbitraryObjectCollectionAsParam("identifiers", identification.getPrimaryIdentifiers());
        result.addArbitraryObjectAsParam("context", ctx);
        try {
            resourceObject = getResourceObject(identification, result).resourceObject();
        } catch (ReturnCachedException e) {
            result.muteAllSubresultErrors();
            result.recordSuccess();
            return returnCached(e.reason);
        } catch (Exception ex) {
            result.recordException(ex);
            result.close(); // This is necessary before invoking the error handler
            try {
                var shadowAfterErrorProcessing = invokeErrorHandler(ex, result, parentResult);
                if (shadowAfterErrorProcessing != null) {
                    repositoryShadow = shadowAfterErrorProcessing;
                    return returnCached(
                            "(handled) exception during resource object retrieval: " + formatExceptionMessage(ex));
                } else {
                    throw ex;
                }
            } catch (GenericFrameworkException | ObjectAlreadyExistsException | PolicyViolationException e) {
                throw new SystemException(e.getMessage(), e);
            }
        } finally {
            result.close();
        }

        classifyIfNeeded(resourceObject, parentResult);
        updateShadowInRepository(resourceObject, parentResult);
        ShadowType shadowedObject = constructShadowedObject(resourceObject, parentResult);
        return returnRetrieved(shadowedObject);
    }

    private static @NotNull ShadowType obtainRepositoryShadow(
            @NotNull String oid,
            @Nullable ShadowType providedRepositoryShadow,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (providedRepositoryShadow != null) {
            LOGGER.trace("Start getting '{}' (opts {})", providedRepositoryShadow, options);
            argCheck(oid.equals(providedRepositoryShadow.getOid()), "Provided OID is not equal to OID of repository shadow");
            if (providedRepositoryShadow.isImmutable()) {
                return providedRepositoryShadow.clone();
            } else {
                return providedRepositoryShadow;
            }
        } else {
            LOGGER.trace("Start getting shadow '{}' (opts {})", oid, options);
            // Get the shadow from repository. There are identifiers that we need for accessing the object by UCF.
            ShadowType fetchedRepositoryShadow =
                    b().repositoryService
                            .getObject(ShadowType.class, oid, disableReadOnly(options), result)
                            .asObjectable();
            LOGGER.trace("Got repository shadow object:\n{}", fetchedRepositoryShadow.debugDumpLazily());
            return fetchedRepositoryShadow;
        }
    }

    private static ShadowsLocalBeans b() {
        return ShadowsLocalBeans.get();
    }

    private static @NotNull ProvisioningContext createProvisioningContext(
            @NotNull ShadowType repositoryShadow,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ProvisioningOperationContext operationContext,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException {
        ProvisioningContext ctx = b().ctxFactory.createForShadow(repositoryShadow, task, result);
        ctx.setGetOperationOptions(options);
        ctx.setOperationContext(operationContext);
        return ctx;
    }

    private boolean isRaw() {
        return GetOperationOptions.isRaw(rootOptions);
    }

    private boolean isNoFetch() {
        return GetOperationOptions.isNoFetch(rootOptions);
    }

    private void checkReadCapability() {
        if (!ctx.hasReadCapability()) {
            throw new UnsupportedOperationException("Resource does not support 'read' operation");
        }
    }

    private void refreshBeforeReading(@NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        if (isForceRefresh(rootOptions)
                || isForceRetry(rootOptions)
                || ResourceTypeUtil.isRefreshOnRead(ctx.getResource())) {
            LOGGER.trace("Doing full shadow refresh before read operation for {}", repositoryShadow);
            doFullShadowRefresh(result);
        } else {
            LOGGER.trace("Full refresh is not requested, doing quick one only for {}", repositoryShadow);
            doQuickShadowRefresh(result);
        }
    }

    private void doQuickShadowRefresh(OperationResult result) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        var refreshedShadow = ShadowRefreshOperation.executeQuick(ctx, repositoryShadow, result);
        if (refreshedShadow == null) {
            throw new ObjectNotFoundException(
                    "Resource object not found (after quick refresh)",
                    ShadowType.class,
                    oid,
                    ctx.isAllowNotFound());
        }
        ctx.updateShadowState(refreshedShadow);
        repositoryShadow = refreshedShadow;
    }

    private void doFullShadowRefresh(OperationResult result) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ProvisioningOperationOptions refreshOpts = toProvisioningOperationOptions(rootOptions);
        var refreshedShadow =
                ShadowRefreshOperation
                        .executeFull(repositoryShadow, refreshOpts, ctx.getOperationContext(), ctx.getTask(), result)
                        .getShadow();
        LOGGER.trace("Refreshed repository shadow:\n{}", DebugUtil.debugDumpLazily(refreshedShadow, 1));

        if (refreshedShadow == null) {
            // Most probably a dead shadow was just removed
            // TODO: is this OK? What about re-appeared objects
            LOGGER.debug("Shadow (no longer) exists: {}", originalRepoShadow);
            throw new ObjectNotFoundException("Resource object does not exist", ShadowType.class, oid);
        }

        // Refresh might change the shadow state.
        ctx.updateShadowState(refreshedShadow);
        repositoryShadow = refreshedShadow;
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
    private ResourceObjectIdentification.Primary getPrimaryIdentification() throws SchemaException {
        ResourceObjectDefinition objDef = ctx.getObjectDefinitionRequired();
        if (identifiersOverride != null) {
            LOGGER.trace("Using overridden identifiers: {}", identifiersOverride);
            var identification = ResourceObjectIdentification.fromIdentifiers(
                    objDef, identifiersOverride);
            if (identification instanceof ResourceObjectIdentification.Primary primary) {
                return primary;
            }
            throw new SchemaException("Overridden identifiers are not primary: " + identification);
        }

        var identification = ResourceObjectIdentification.fromShadow(objDef, repositoryShadow);
        if (identification instanceof ResourceObjectIdentification.Primary primary) {
            return primary;
        }

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
        }

        throw new SchemaException(
                String.format("No primary identifiers found in the repository shadow %s (%s) with respect to %s",
                        repositoryShadow, identification, ctx.getResource()));
    }

    private @NotNull CompleteResourceObject getResourceObject(
            ResourceObjectIdentification.Primary identification, OperationResult result)
            throws CommunicationException, SchemaException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ReturnCachedException, ObjectNotFoundException {

        InternalMonitor.recordCount(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        try {
            assert repositoryShadow != null;
            @NotNull var completeObject =
                    b.resourceObjectConverter.fetchResourceObject(
                            ctx, identification, ctx.createAttributesToReturn(), repositoryShadow, true, result);
            LOGGER.trace("Object returned by ResourceObjectConverter:\n{}", completeObject.debugDumpLazily(1));
            markResourceUp(result);
            return completeObject;
        } catch (ObjectNotFoundException e) {
            ShadowLifecycleStateType shadowState = repositoryShadow.getShadowLifecycleState();
            // This may be OK, e.g. for connectors that have running async add operation.
            if (shadowState == CONCEIVED || shadowState == GESTATING) {
                LOGGER.trace("{} was not found, but we can return cached shadow because it is in {} state",
                        repositoryShadow, shadowState);
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
        // The resourceManager.modifyResourceAvailabilityStatus method retrieves the resource from cache. It is a bit
        // costly now (e.g., it includes ResourceType cloning). Even if this cost could be reduced, we may skip the operation
        // altogether, if the (currently known) resource is marked as UP. The data may be slightly outdated, but not much.
        // Even if so, the resource would certainly be sooner or later marked as UP by a different "get" operation.
        if (!ResourceTypeUtil.isUp(ctx.getResource())) {
            b.resourceManager.modifyResourceAvailabilityStatus(
                    ctx.getResourceOid(),
                    AvailabilityStatusType.UP,
                    "getting " + repositoryShadow + " was successful.",
                    ctx.getTask(),
                    result,
                    false);
        }
    }

    private @Nullable ShadowType invokeErrorHandler(Exception cause, OperationResult failedOpResult, OperationResult result)
            throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException {
        LOGGER.debug("Handling provisioning GET exception {}: {}", cause.getClass(), cause.getMessage());
        assert repositoryShadow != null;
        var shadowAfterErrorProcessing = b.errorHandlerLocator
                .locateErrorHandlerRequired(cause)
                .handleGetError(ctx, repositoryShadow, cause, failedOpResult, result);
        // the shadow may be null here if it's found to be no longer existing
        if (shadowAfterErrorProcessing != null) {
            // We update the shadow lifecycle state because we are not sure if the shadow after handling the exception
            // is the same as it was before (that has its state set).
            ctx.updateShadowState(shadowAfterErrorProcessing);
        }
        return shadowAfterErrorProcessing;
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

    private @NotNull ShadowType constructShadowedObject(@NotNull ResourceObject resourceObject, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException, EncryptionException {
        // Complete the shadow by adding attributes from the resource object
        // This also completes the associations by adding shadowRefs
        ShadowType shadowedObject =
                ShadowedObjectConstruction.construct(
                        ctx, repositoryShadow, resourceObject, result);
        LOGGER.trace("Shadowed resource object:\n{}", shadowedObject.debugDumpLazily(1));
        return shadowedObject;
    }

    private void classifyIfNeeded(ResourceObject resourceObject, @NotNull OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (b.classificationHelper.shouldClassify(ctx, repositoryShadow)) {
            ResourceObjectClassification classification =
                    b.classificationHelper.classify(ctx, repositoryShadow, resourceObject.getBean(), result);
            if (classification.isKnown()) {
                // TODO deduplicate this code somehow
                LOGGER.debug("Classified {} as {}", repositoryShadow, classification.getDefinition());
                repositoryShadow = b.shadowUpdater.normalizeShadowAttributesInRepository(
                        ctx, repositoryShadow, classification, result);
                ctx.adoptShadow(repositoryShadow);
            }
        }
        // Resource shadow may have different auxiliary object classes than the original repo shadow. Make sure we have the
        // definition that applies to resource shadow. We will fix repo shadow later. BUT we need also information about
        // kind/intent and these information is only in repo shadow, therefore the following 2 lines...
        resourceObject.getBean().setKind(repositoryShadow.getKind());
        resourceObject.getBean().setIntent(repositoryShadow.getIntent());
        ctx = ctx.spawnForShadow(resourceObject.getBean());
    }

    private void updateShadowInRepository(ResourceObject resourceObject, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("updateShadowInRepository starting; shadow from repository:\n{}", repositoryShadow.debugDump(1));
            LOGGER.trace("Resource object fetched from resource:\n{}", resourceObject.debugDump(1));
        }
        repositoryShadow =
                b.shadowUpdater.updateShadowInRepository(
                        ctx, resourceObject, null, repositoryShadow, result);
        LOGGER.trace("Repository shadow after update:\n{}", repositoryShadow.debugDumpLazily(1));
    }

    private String getReasonForReturningCachedShadow() throws ConfigurationException {
        LOGGER.trace("Determining if we have a reason for returning cached shadow"); // the non-null result will be logged later
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
                LOGGER.trace("We need current reliable state -> we will NOT return cached data.");
                return null;
            case CACHED:
                if (isCachedShadowFreshEnough()) {
                    return "requested cached data";
                } else {
                    LOGGER.trace("Requested cached data, but the shadow is not fresh enough");
                    return null;
                }
            case FUTURE:
                LOGGER.trace("We were asked for future point in time. We could return cached, e.g. if there was a pending "
                        + "create operation. But let's try real get operation first and then we'll see.");
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
                throw new ConfigurationException("Cached version of " + repositoryShadow + " requested, but there is no cached value");
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
        return b.clock.currentTimeMillis() - retrievalTimestampMillis < stalenessOption;
    }

    private static class ReturnCachedException extends Exception {
        private final String reason;

        private ReturnCachedException(String reason) {
            this.reason = reason;
        }
    }
}
