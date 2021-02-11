package com.evolveum.midpoint.provisioning.impl.shadowcache;

import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.validateShadow;
import static com.evolveum.midpoint.schema.GetOperationOptions.*;

import java.util.Collection;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;

import org.apache.commons.lang.Validate;
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
import com.evolveum.midpoint.provisioning.impl.errorhandling.ErrorHandler;
import com.evolveum.midpoint.provisioning.impl.errorhandling.ErrorHandlerLocator;
import com.evolveum.midpoint.provisioning.impl.shadowmanager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.RefreshShadowOperation;
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
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
    @Autowired private AdoptionHelper adoptionHelper;
    @Autowired private RefreshHelper refreshHelper;

    private static final Trace LOGGER = TraceManager.getTrace(GetHelper.class);

    public PrismObject<ShadowType> getShadow(String oid, PrismObject<ShadowType> repositoryShadow,
            Collection<ResourceAttribute<?>> identifiersOverride, Collection<SelectorOptions<GetOperationOptions>> options,
            Task task, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {

        Validate.notNull(oid, "Object id must not be null.");

        if (repositoryShadow == null) {
            LOGGER.trace("Start getting object with oid {}; identifiers override = {}", oid, identifiersOverride);
        } else {
            LOGGER.trace("Start getting object {}; identifiers override = {}", repositoryShadow, identifiersOverride);
        }

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

        // We are using parent result directly, not creating subresult.
        // We want to hide the existence of shadow cache from the user.

        // Get the shadow from repository. There are identifiers that we need
        // for accessing the object by UCF.Later, the repository object may
        // have a fully cached object from the resource.
        if (repositoryShadow == null) {
            repositoryShadow = repositoryService.getObject(ShadowType.class, oid, null, parentResult);
            LOGGER.trace("Got repository shadow object:\n{}", repositoryShadow.debugDumpLazily());
        }

        // Sanity check
        if (!oid.equals(repositoryShadow.getOid())) {
            parentResult.recordFatalError("Provided OID is not equal to OID of repository shadow");
            throw new IllegalArgumentException("Provided OID is not equal to OID of repository shadow");
        }

        ProvisioningContext ctx = ctxFactory.create(repositoryShadow, task, parentResult);
        ctx.setGetOperationOptions(options);
        try {
            ctx.assertDefinition();
        } catch (SchemaException | ConfigurationException | ObjectNotFoundException | CommunicationException | ExpressionEvaluationException e) {
            if (isRaw(rootOptions)) {
                // when using raw (repository option), return the repo shadow as it is. it's better than nothing and in this case we don't even need resource
                //TODO maybe change assertDefinition to consider rawOption?
                parentResult.computeStatusIfUnknown();
                parentResult.muteError();
                return repositoryShadow;
            }
            throw e;
        }
        if (repositoryShadow.isImmutable()) {
            repositoryShadow = shadowCaretaker.applyAttributesDefinitionToImmutable(ctx, repositoryShadow);
        } else {
            shadowCaretaker.applyAttributesDefinition(ctx, repositoryShadow);
        }

        ResourceType resource = ctx.getResource();
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        if (isNoFetch(rootOptions) || isRaw(rootOptions)) {
            return processNoFetchGet(ctx, repositoryShadow, options, now, task, parentResult);
        }

        if (!ResourceTypeUtil.isReadCapabilityEnabled(resource)) {
            UnsupportedOperationException e = new UnsupportedOperationException("Resource does not support 'read' operation");
            parentResult.recordFatalError(e);
            throw e;
        }

        if (ResourceTypeUtil.isInMaintenance(resource)) {
            try {
                MaintenanceException ex = new MaintenanceException("Resource " + resource + " is in the maintenance");
                PrismObject<ShadowType> handledShadow = handleGetError(ctx, repositoryShadow, rootOptions, ex, task, parentResult);
                validateShadow(handledShadow, true);
                return handledShadow;
            } catch (GenericFrameworkException | ObjectAlreadyExistsException | PolicyViolationException e) {
                throw new SystemException(e.getMessage(), e);
            }
        }

        if (shouldRefreshOnRead(resource, rootOptions)) {
            LOGGER.trace("Refreshing {} before reading", repositoryShadow);
            ProvisioningOperationOptions refreshOpts = toProvisioningOperationOptions(rootOptions);
            RefreshShadowOperation refreshShadowOperation = refreshHelper.refreshShadow(repositoryShadow, refreshOpts, task, parentResult);
            if (refreshShadowOperation != null) {
                repositoryShadow = refreshShadowOperation.getRefreshedShadow();
            }
            LOGGER.trace("Refreshed repository shadow:\n{}", DebugUtil.debugDumpLazily(repositoryShadow, 1));
        }
        if (repositoryShadow == null) {
            // Dead shadow was just removed
            // TODO: is this OK? What about re-appeared objects
            LOGGER.warn("DEAD shadow {} DEAD?", oid);
            ObjectNotFoundException e = new ObjectNotFoundException("Resource object does not exist");
            parentResult.recordFatalError(e);
            throw e;
        }

        ShadowState shadowState = shadowCaretaker.determineShadowState(ctx, repositoryShadow, now);
        LOGGER.trace("State of shadow {}: {}", repositoryShadow, shadowState);

        if (canImmediatelyReturnCached(options, repositoryShadow, shadowState, resource)) {
            LOGGER.trace("Returning cached (repository) version of shadow {}", repositoryShadow);
            PrismObject<ShadowType> resultShadow = commonHelper.futurizeShadow(ctx, repositoryShadow, null, options, now);
            shadowCaretaker.applyAttributesDefinition(ctx, resultShadow);
            validateShadow(resultShadow, true);
            return resultShadow;
        }

        PrismObject<ShadowType> resourceObject;

        Collection<? extends ResourceAttribute<?>> identifiers;
        if (identifiersOverride != null) {
            identifiers = identifiersOverride;
        } else {
            Collection<? extends ResourceAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(repositoryShadow);
            if (primaryIdentifiers == null || primaryIdentifiers.isEmpty()) {
                if (ProvisioningUtil.hasPendingAddOperation(repositoryShadow) || ShadowUtil
                        .isDead(repositoryShadow.asObjectable())) {
                    if (ProvisioningUtil.isFuturePointInTime(options)) {
                        // Get of uncreated or dead shadow, we want to see future state (how the shadow WILL look like).
                        // We cannot even try fetch operation here. We do not have the identifiers.
                        // But we have quite a good idea how the shadow is going to look like. Therefore we can return it.
                        PrismObject<ShadowType> resultShadow = commonHelper.futurizeShadow(ctx, repositoryShadow, null, options, now);
                        shadowCaretaker.applyAttributesDefinition(ctx, resultShadow);
                        validateShadow(resultShadow, true);
                        // NOTE: do NOT re-try add operation here. It will be retried in separate task.
                        // re-trying the operation here would not provide big benefits and it will complicate the code.
                        return resultShadow;
                    } else {
                        // Get of uncreated shadow, but we want current state. Therefore we have to throw an error because
                        // the object does not exist yet - to our best knowledge. But we cannot really throw ObjectNotFound here.
                        // ObjectNotFound is a positive indication that the object does not exist.
                        // We do not know that for sure because resource is unavailable. The object might have been created in the meantime.
                        throw new GenericConnectorException(
                                "Unable to get object from the resource. Probably it has not been created yet because of previous unavailability of the resource.");
                    }
                }

                // No identifiers found
                SchemaException ex = new SchemaException("No primary identifiers found in the repository shadow "
                        + repositoryShadow + " with respect to " + resource);
                parentResult.recordFatalError("No primary identifiers found in the repository shadow " + repositoryShadow, ex);
                throw ex;
            }
            identifiers = ShadowUtil.getAllIdentifiers(repositoryShadow);
        }

        try {

            try {

                resourceObject = resourceObjectConverter.getResourceObject(ctx, identifiers, true, parentResult);

            } catch (ObjectNotFoundException e) {
                // This may be OK, e.g. for connectors that have running async add operation.
                if (shadowState == ShadowState.CONCEPTION || shadowState == ShadowState.GESTATION) {
                    LOGGER.trace("{} was not found, but we can return cached shadow because it is in {} state", repositoryShadow, shadowState);
                    parentResult.deleteLastSubresultIfError();        // we don't want to see 'warning-like' orange boxes in GUI (TODO reconsider this)
                    parentResult.recordSuccess();

                    PrismObject<ShadowType> resultShadow = commonHelper.futurizeShadow(ctx, repositoryShadow, null, options, now);
                    shadowCaretaker.applyAttributesDefinition(ctx, resultShadow);
                    LOGGER.trace("Returning futurized shadow:\n{}", DebugUtil.debugDumpLazily(resultShadow));
                    validateShadow(resultShadow, true);
                    return resultShadow;

                } else {
                    LOGGER.trace("{} was not found, following normal error processing because shadow is in {} state", repositoryShadow, shadowState);
                    // This is live shadow that was not found on resource. Just re-throw the exception. It will
                    // be caught later and the usual error handlers will bury the shadow.
                    throw e;
                }
            }

            LOGGER.trace("Shadow returned by ResourceObjectConverter:\n{}", resourceObject.debugDumpLazily(1));

            // Resource shadow may have different auxiliary object classes than
            // the original repo shadow. Make sure we have the definition that
            // applies to resource shadow. We will fix repo shadow later.
            // BUT we need also information about kind/intent and these information is only
            // in repo shadow, therefore the following 2 lines..
            resourceObject.asObjectable().setKind(repositoryShadow.asObjectable().getKind());
            resourceObject.asObjectable().setIntent(repositoryShadow.asObjectable().getIntent());
            ProvisioningContext shadowCtx = ctx.spawn(resourceObject);

            String operationCtx = "getting " + repositoryShadow + " was successful.";

            resourceManager.modifyResourceAvailabilityStatus(resource.getOid(), AvailabilityStatusType.UP, operationCtx, task, parentResult, false);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Shadow from repository:\n{}", repositoryShadow.debugDump(1));
                LOGGER.trace("Resource object fetched from resource:\n{}", resourceObject.debugDump(1));
            }

            repositoryShadow = shadowManager.updateShadow(shadowCtx, resourceObject, null, repositoryShadow, shadowState, parentResult);
            LOGGER.trace("Repository shadow after update:\n{}", repositoryShadow.debugDumpLazily(1));

            // Complete the shadow by adding attributes from the resource object
            // This also completes the associations by adding shadowRefs
            PrismObject<ShadowType> assembledShadow = adoptionHelper.completeShadow(shadowCtx, resourceObject, repositoryShadow, false, parentResult);
            LOGGER.trace("Shadow when assembled:\n{}", assembledShadow.debugDumpLazily(1));

            PrismObject<ShadowType> resultShadow = commonHelper.futurizeShadow(ctx, repositoryShadow, assembledShadow, options, now);
            LOGGER.trace("Futurized assembled shadow:\n{}", resultShadow.debugDumpLazily(1));

            parentResult.recordSuccess();
            validateShadow(resultShadow, true);
            return resultShadow;

        } catch (Exception ex) {
            try {
                PrismObject<ShadowType> handledShadow = handleGetError(ctx, repositoryShadow, rootOptions, ex, task, parentResult);
                if (handledShadow == null) {
                    throw ex;
                }
                if (parentResult.getStatus() == OperationResultStatus.FATAL_ERROR) {
                    // We are going to return an object. Therefore this cannot
                    // be fatal error, as at least some information
                    // is returned
                    parentResult.setStatus(OperationResultStatus.PARTIAL_ERROR);
                }
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

    private PrismObject<ShadowType> processNoFetchGet(ProvisioningContext ctx,
            PrismObject<ShadowType> repositoryShadow, Collection<SelectorOptions<GetOperationOptions>> options,
            XMLGregorianCalendar now, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {
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

        PrismObject<ShadowType> resultShadow = commonHelper.futurizeShadow(ctx, repositoryShadow, null, options, now);
        shadowCaretaker.applyAttributesDefinition(ctx, resultShadow);

        return resultShadow;
    }

    private PrismObject<ShadowType> handleGetError(ProvisioningContext ctx,
            PrismObject<ShadowType> repositoryShadow,
            GetOperationOptions rootOptions,
            Exception cause,
            Task task,
            OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        ErrorHandler handler = errorHandlerLocator.locateErrorHandler(cause);
        if (handler == null) {
            parentResult.recordFatalError("Error without a handler: " + cause.getMessage(), cause);
            throw new SystemException(cause.getMessage(), cause);
        }
        LOGGER.debug("Handling provisioning GET exception {}: {}", cause.getClass(), cause.getMessage());
        return handler.handleGetError(ctx, repositoryShadow, rootOptions, cause, task, parentResult);
    }

    private boolean canImmediatelyReturnCached(Collection<SelectorOptions<GetOperationOptions>> options, PrismObject<ShadowType> repositoryShadow, ShadowState shadowState, ResourceType resource) throws ConfigurationException {
        if (ProvisioningUtil.resourceReadIsCachingOnly(resource)) {
            return true;
        }
        if (shadowState == ShadowState.TOMBSTONE) {
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

    private boolean isCachedShadowValid(Collection<SelectorOptions<GetOperationOptions>> options, PrismObject<ShadowType> repositoryShadow) throws ConfigurationException {
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
