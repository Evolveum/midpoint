/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.validateShadow;
import static com.evolveum.midpoint.schema.GetOperationOptions.*;

import java.util.Collection;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.provisioning.api.ResourceObjectClassification;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;

import com.evolveum.midpoint.provisioning.impl.resources.ResourceManager;
import com.evolveum.midpoint.schema.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.*;
import com.evolveum.midpoint.provisioning.impl.shadows.errors.ErrorHandler;
import com.evolveum.midpoint.provisioning.impl.shadows.errors.ErrorHandlerLocator;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Helps with the `get` operation.
 */
@Component
@Experimental
class GetHelper {

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private ErrorHandlerLocator errorHandlerLocator;
    @Autowired private ResourceManager resourceManager;
    @Autowired private Clock clock;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired protected ShadowManager shadowManager;
    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private CommonHelper commonHelper;
    @Autowired private ShadowedObjectConstructionHelper shadowedObjectConstructionHelper;
    @Autowired private RefreshHelper refreshHelper;
    @Autowired private ClassificationHelper classificationHelper;

    private static final Trace LOGGER = TraceManager.getTrace(GetHelper.class);

    /**
     * Provides more-or-less the whole functionality of provisioning `getObject` method for shadows.
     */
    public @NotNull PrismObject<ShadowType> getShadow(
            @NotNull String oid,
            @Nullable PrismObject<ShadowType> repoShadow,
            @Nullable Collection<ResourceAttribute<?>> identifiersOverride,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {

        if (repoShadow == null) {
            LOGGER.trace("Start getting object with oid {}; identifiers override = {}", oid, identifiersOverride);
        } else {
            LOGGER.trace("Start getting object '{}'; identifiers override = {}", repoShadow, identifiersOverride);
        }

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

        // We are using parent result directly, not creating subresult.
        // We want to hide the existence of shadow cache from the user.

        // Get the shadow from repository. There are identifiers that we need
        // for accessing the object by UCF.Later, the repository object may
        // have a fully cached object from the resource.
        if (repoShadow == null) {
            repoShadow = repositoryService.getObject(ShadowType.class, oid, disableReadOnly(options), result);
            LOGGER.trace("Got repository shadow object:\n{}", repoShadow.debugDumpLazily());
        }

        @NotNull ShadowType originalRepoShadow = repoShadow.asObjectable();

        // Sanity check
        if (!oid.equals(repoShadow.getOid())) {
            result.recordFatalError("Provided OID is not equal to OID of repository shadow");
            throw new IllegalArgumentException("Provided OID is not equal to OID of repository shadow");
        }

        ProvisioningContext ctx;
        try {
            ctx = ctxFactory.createForShadow(repoShadow, task, result);
            ctx.setGetOperationOptions(options);
            ctx.assertDefinition();
        } catch (SchemaException | ConfigurationException | ObjectNotFoundException | CommunicationException | ExpressionEvaluationException e) {
            if (isRaw(rootOptions)) {
                // when using raw (repository option), return the repo shadow as it is. it's better than nothing and in this case we don't even need resource
                //TODO maybe change assertDefinition to consider rawOption?
                result.computeStatusIfUnknown();
                result.muteError();
                shadowCaretaker.updateShadowStateInEmergency(repoShadow);
                return repoShadow;
            }
            throw e;
        }
        if (repoShadow.isImmutable()) {
            repoShadow = shadowCaretaker.applyAttributesDefinitionToImmutable(ctx, repoShadow);
        } else {
            shadowCaretaker.applyAttributesDefinition(ctx, repoShadow);
        }

        ResourceType resource = ctx.getResource();
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        if (isNoFetch(rootOptions) || isRaw(rootOptions)) {
            return processNoFetchGet(ctx, repoShadow, options, now, task, result);
        }

        if (!ResourceTypeUtil.isReadCapabilityEnabled(resource)) {
            UnsupportedOperationException e = new UnsupportedOperationException("Resource does not support 'read' operation");
            result.recordFatalError(e);
            throw e;
        }

        if (ResourceTypeUtil.isInMaintenance(resource)) {
            try {
                MaintenanceException ex = new MaintenanceException("Resource " + resource + " is in the maintenance");
                PrismObject<ShadowType> handledShadow = handleGetError(ctx, repoShadow, rootOptions, ex, task, result);
                validateShadow(handledShadow, true);
                shadowCaretaker.applyAttributesDefinition(ctx, handledShadow);
                shadowCaretaker.updateShadowState(ctx, handledShadow);
                return handledShadow;
            } catch (GenericFrameworkException | ObjectAlreadyExistsException | PolicyViolationException e) {
                throw new SystemException(e.getMessage(), e);
            }
        }

        if (shouldRefreshOnRead(resource, rootOptions)) {
            LOGGER.trace("Refreshing {} before reading", repoShadow);
            ProvisioningOperationOptions refreshOpts = toProvisioningOperationOptions(rootOptions);
            repoShadow = refreshHelper
                    .refreshShadow(repoShadow, refreshOpts, task, result)
                    .getRefreshedShadow();
            LOGGER.trace("Refreshed repository shadow:\n{}", DebugUtil.debugDumpLazily(repoShadow, 1));
        }
        if (repoShadow == null) {
            // Most probably a dead shadow was just removed
            // TODO: is this OK? What about re-appeared objects
            LOGGER.debug("Shadow (no longer) exists: {}", originalRepoShadow);
            ObjectNotFoundException e = new ObjectNotFoundException("Resource object does not exist", oid);
            result.recordFatalError(e);
            throw e;
        }

        ShadowLifecycleStateType shadowState = shadowCaretaker.updateAndReturnShadowState(ctx, repoShadow, now);
        LOGGER.trace("State of shadow {}: {}", repoShadow, shadowState);

        if (canImmediatelyReturnCached(options, repoShadow, shadowState, resource)) {
            LOGGER.trace("Returning cached (repository) version of shadow {}", repoShadow);
            PrismObject<ShadowType> resultShadow = commonHelper.futurizeShadow(ctx, repoShadow, null, options, now);
            shadowCaretaker.applyAttributesDefinition(ctx, resultShadow);
            validateShadow(resultShadow, true);
            return resultShadow;
        }

        PrismObject<ShadowType> resourceObject;

        Collection<? extends ResourceAttribute<?>> identifiers;
        if (identifiersOverride != null) {
            identifiers = identifiersOverride;
        } else {
            Collection<? extends ResourceAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(repoShadow);
            if (primaryIdentifiers == null || primaryIdentifiers.isEmpty()) {
                if (ProvisioningUtil.hasPendingAddOperation(repoShadow) ||
                        ProvisioningUtil.hasPendingDeleteOperation(repoShadow) ||
                        ShadowUtil.isDead(repoShadow.asObjectable())) {
                    if (ProvisioningUtil.isFuturePointInTime(options)) {
                        // Get of uncreated or dead shadow, we want to see future state (how the shadow WILL look like).
                        // We cannot even try fetch operation here. We do not have the identifiers.
                        // But we have quite a good idea how the shadow is going to look like. Therefore we can return it.
                        PrismObject<ShadowType> resultShadow = commonHelper.futurizeShadow(ctx, repoShadow, null, options, now);
                        shadowCaretaker.applyAttributesDefinition(ctx, resultShadow);
                        validateShadow(resultShadow, true);
                        // NOTE: do NOT re-try add operation here. It will be retried in separate task.
                        // re-trying the operation here would not provide big benefits and it will complicate the code.
                        return resultShadow;
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

                // No identifiers found
                SchemaException ex = new SchemaException("No primary identifiers found in the repository shadow "
                        + repoShadow + " with respect to " + resource);
                result.recordFatalError("No primary identifiers found in the repository shadow " + repoShadow, ex);
                throw ex;
            }

            identifiers = Objects.requireNonNull(
                    ShadowUtil.getAllIdentifiers(repoShadow));
        }

        try {

            try {

                resourceObject =
                        resourceObjectConverter.getResourceObject(
                                ctx, identifiers, repoShadow, true, result);

            } catch (ObjectNotFoundException e) {
                // This may be OK, e.g. for connectors that have running async add operation.
                if (shadowState == ShadowLifecycleStateType.CONCEIVED || shadowState == ShadowLifecycleStateType.GESTATING) {
                    LOGGER.trace("{} was not found, but we can return cached shadow because it is in {} state", repoShadow, shadowState);
                    result.deleteLastSubresultIfError(); // we don't want to see 'warning-like' orange boxes in GUI (TODO reconsider this)
                    result.recordSuccess();

                    PrismObject<ShadowType> resultShadow = commonHelper.futurizeShadow(ctx, repoShadow, null, options, now);
                    shadowCaretaker.applyAttributesDefinition(ctx, resultShadow);
                    LOGGER.trace("Returning futurized shadow:\n{}", DebugUtil.debugDumpLazily(resultShadow));
                    validateShadow(resultShadow, true);
                    return resultShadow;

                } else {
                    LOGGER.trace("{} was not found, following normal error processing because shadow is in {} state", repoShadow, shadowState);
                    // This is live shadow that was not found on resource. Just re-throw the exception. It will
                    // be caught later and the usual error handlers will bury the shadow.
                    throw e;
                }
            }

            LOGGER.trace("Object returned by ResourceObjectConverter:\n{}", resourceObject.debugDumpLazily(1));

            if (!ShadowUtil.isClassified(repoShadow.asObjectable())) {
                ResourceObjectClassification classification =
                        classificationHelper.classify(ctx, repoShadow, resourceObject, result);
                if (classification.isKnown()) {
                    // TODO deduplicate this code somehow
                    LOGGER.debug("Classified {} as {}", repoShadow, classification.getDefinition());
                    repoShadow = shadowManager.fixShadow(ctx, repoShadow, result);
                    shadowCaretaker.updateAndReturnShadowState(ctx, repoShadow, now);
                    ProvisioningContext tempCtx = ctx.spawnForShadow(repoShadow.asObjectable());
                    shadowCaretaker.applyAttributesDefinition(tempCtx, repoShadow);
                }
            }

            // Resource shadow may have different auxiliary object classes than the original repo shadow. Make sure we have the
            // definition that applies to resource shadow. We will fix repo shadow later. BUT we need also information about
            // kind/intent and these information is only in repo shadow, therefore the following 2 lines...
            resourceObject.asObjectable().setKind(repoShadow.asObjectable().getKind());
            resourceObject.asObjectable().setIntent(repoShadow.asObjectable().getIntent());
            ProvisioningContext shadowCtx = ctx.spawnForShadow(resourceObject.asObjectable());

            String operationCtx = "getting " + repoShadow + " was successful.";

            resourceManager.modifyResourceAvailabilityStatus(
                    resource.getOid(), AvailabilityStatusType.UP, operationCtx, task, result, false);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Shadow from repository:\n{}", repoShadow.debugDump(1));
                LOGGER.trace("Resource object fetched from resource:\n{}", resourceObject.debugDump(1));
            }

            repoShadow =
                    shadowManager.updateShadow(
                            shadowCtx, resourceObject, null, repoShadow, shadowState, result);
            LOGGER.trace("Repository shadow after update:\n{}", repoShadow.debugDumpLazily(1));

            // Complete the shadow by adding attributes from the resource object
            // This also completes the associations by adding shadowRefs
            PrismObject<ShadowType> shadowedObject =
                    shadowedObjectConstructionHelper.constructShadowedObject(shadowCtx, repoShadow, resourceObject, result);
            LOGGER.trace("Shadowed resource object:\n{}", shadowedObject.debugDumpLazily(1));

            PrismObject<ShadowType> resultShadow = commonHelper.futurizeShadow(ctx, repoShadow, shadowedObject, options, now);
            LOGGER.trace("Futurized shadowed resource:\n{}", resultShadow.debugDumpLazily(1));

            result.recordSuccess();
            validateShadow(resultShadow, true);
            return resultShadow;

        } catch (Exception ex) {
            try {
                PrismObject<ShadowType> handledShadow = handleGetError(ctx, repoShadow, rootOptions, ex, task, result);
                if (handledShadow == null) {
                    throw ex;
                }
                if (result.getStatus() == OperationResultStatus.FATAL_ERROR) {
                    // We are going to return an object. Therefore this cannot
                    // be fatal error, as at least some information
                    // is returned
                    result.setStatus(OperationResultStatus.PARTIAL_ERROR);
                }
                // We update the shadow lifecycle state because we are not sure if the handledShadow is the same
                // as repoShadow (that has its state set).
                shadowCaretaker.updateShadowState(ctx, handledShadow); // must be called before futurizeShadow
                PrismObject<ShadowType> futurizedShadow = commonHelper.futurizeShadow(ctx, handledShadow, null, options, now);
                validateShadow(futurizedShadow, true);
                return futurizedShadow;

            } catch (GenericFrameworkException | ObjectAlreadyExistsException | PolicyViolationException e) {
                throw new SystemException(e.getMessage(), e);
            }
        } finally {
            // We need to record the fetch down here. Now it is certain that we
            // are going to fetch from resource (we do not have raw/noFetch option)
            InternalMonitor.recordCount(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        }
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

    private boolean shouldRefreshOnRead(ResourceType resource, GetOperationOptions rootOptions) {
        return isForceRefresh(rootOptions) || isForceRetry(rootOptions) || ResourceTypeUtil.isRefreshOnRead(resource);
    }

    private @NotNull PrismObject<ShadowType> processNoFetchGet(ProvisioningContext ctx,
            PrismObject<ShadowType> repositoryShadow, Collection<SelectorOptions<GetOperationOptions>> options,
            XMLGregorianCalendar now, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        LOGGER.trace("Processing noFetch get for {}", repositoryShadow);

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        if (!isRaw(rootOptions)) {
            // Even with noFetch we still want to delete expired pending operations. And even delete
            // the shadow if needed.
            repositoryShadow = refreshHelper.refreshShadowQuick(ctx, repositoryShadow, now, task, parentResult);
        }

        if (repositoryShadow == null) {
            ObjectNotFoundException e = new ObjectNotFoundException("Resource object not found");
            parentResult.recordFatalError(e);
            throw e;
        }

        // TODO do we really want to do this in raw mode (MID-7419)?
        shadowCaretaker.updateShadowState(ctx, repositoryShadow); // must be done before futurizeShadow
        PrismObject<ShadowType> resultShadow = commonHelper.futurizeShadow(ctx, repositoryShadow, null, options, now);
        shadowCaretaker.applyAttributesDefinition(ctx, resultShadow);
        return resultShadow;
    }

    private PrismObject<ShadowType> handleGetError(ProvisioningContext ctx,
            PrismObject<ShadowType> repositoryShadow,
            GetOperationOptions rootOptions,
            Exception cause,
            Task task,
            OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException {
        ErrorHandler handler = errorHandlerLocator.locateErrorHandler(cause);
        if (handler == null) {
            parentResult.recordFatalError("Error without a handler: " + cause.getMessage(), cause);
            throw new SystemException(cause.getMessage(), cause);
        }
        LOGGER.debug("Handling provisioning GET exception {}: {}", cause.getClass(), cause.getMessage());
        return handler.handleGetError(ctx, repositoryShadow, rootOptions, cause, task, parentResult);
    }

    private boolean canImmediatelyReturnCached(Collection<SelectorOptions<GetOperationOptions>> options,
            PrismObject<ShadowType> repositoryShadow, ShadowLifecycleStateType shadowState, ResourceType resource)
            throws ConfigurationException {
        if (ProvisioningUtil.resourceReadIsCachingOnly(resource)) {
            return true;
        }
        if (shadowState == ShadowLifecycleStateType.TOMBSTONE) {
            // Once shadow is buried it stays nine feet under. Therefore there is no point in trying to access the resource.
            // NOTE: this is just for tombstone! Schrodinger's shadows (corpse) will still work as if they were alive.
            return true;
        }
        long stalenessOption = getStaleness(SelectorOptions.findRootOptions(options));
        PointInTimeType pit = getPointInTimeType(SelectorOptions.findRootOptions(options));
        if (pit == null) {
            if (stalenessOption > 0) {
                pit = PointInTimeType.CACHED;
            } else {
                pit = PointInTimeType.CURRENT;
            }
        }
        switch (pit) {
            case CURRENT:
                // We need current reliable state. Never return cached data.
                return false;
            case CACHED:
                return isCachedShadowValid(options, repositoryShadow);
            case FUTURE:
                // We could, e.g. if there is a pending create operation. But let's try real get operation first.
                return false;
            default:
                throw new IllegalArgumentException("Unknown point in time: "+pit);
        }
    }

    private boolean isCachedShadowValid(
            Collection<SelectorOptions<GetOperationOptions>> options, PrismObject<ShadowType> repositoryShadow)
            throws ConfigurationException {
        long stalenessOption = getStaleness(SelectorOptions.findRootOptions(options));
        if (stalenessOption == 0L) {
            return false;
        }
        CachingMetadataType cachingMetadata = repositoryShadow.asObjectable().getCachingMetadata();
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
        return (clock.currentTimeMillis() - retrievalTimestampMillis < stalenessOption);
    }

}
