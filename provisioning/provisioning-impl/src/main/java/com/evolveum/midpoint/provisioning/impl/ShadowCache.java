/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.provisioning.impl.errorhandling.ErrorHandler;
import com.evolveum.midpoint.provisioning.impl.errorhandling.ErrorHandlerLocator;
import com.evolveum.midpoint.provisioning.impl.shadowmanager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.processor.ObjectFactory;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;


import java.util.*;

import static com.evolveum.midpoint.provisioning.api.ShadowState.*;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Shadow cache is a facade that covers all the operations with shadows. It
 * takes care of splitting the operations between repository and resource,
 * merging the data back, handling the errors and generally controlling the
 * process.
 *
 * The two principal classes that do the operations are:
 * ResourceObjectConvertor: executes operations on resource
 * ShadowManager: executes operations in the repository
 *
 * @author Radovan Semancik
 * @author Katarina Valalikova
 *
 */
@Component
public class ShadowCache {

    private static final String OP_DELAYED_OPERATION = ShadowCache.class.getName() + ".delayedOperation";
    private static final String OP_OPERATION_RETRY = ShadowCache.class.getName() + ".operationRetry";
    private static final String OP_RESOURCE_OPERATION = ShadowCache.class.getName() + ".resourceOperation";
    private static final String OP_REFRESH_RETRY = ShadowCache.class.getName() + ".refreshRetry";

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private ErrorHandlerLocator errorHandlerLocator;
    @Autowired private ResourceManager resourceManager;
    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;
    @Autowired private SchemaHelper schemaHelper;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired protected ShadowManager shadowManager;
    @Autowired private ChangeNotificationDispatcher operationListener;
    @Autowired private AccessChecker accessChecker;
    @Autowired private TaskManager taskManager;
    @Autowired private ChangeNotificationDispatcher changeNotificationDispatcher;
    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private Protector protector;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowCache.class);

    /**
     * Get the value of repositoryService.
     *
     * DO NOT USE. Only ShadowManager should access repository
     *
     * @return the value of repositoryService
     */
    @Deprecated
    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public PrismObject<ShadowType> getShadow(String oid, PrismObject<ShadowType> repositoryShadow,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
                    throws ObjectNotFoundException, CommunicationException, SchemaException,
                    ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {

        Validate.notNull(oid, "Object id must not be null.");

        if (repositoryShadow == null) {
            LOGGER.trace("Start getting object with oid {}", oid);
        } else {
            LOGGER.trace("Start getting object {}", repositoryShadow);
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
            if (GetOperationOptions.isRaw(rootOptions)) {
                // when using raw (repository option), return the repo shadow as it is. it's better than nothing and in this case we don't even need resource
                //TODO maybe change assertDefinition to consider rawOption?
                parentResult.computeStatusIfUnknown();
                parentResult.muteError();
                return repositoryShadow;
            }
            throw e;
        }

        shadowCaretaker.applyAttributesDefinition(ctx, repositoryShadow);
        ResourceType resource = ctx.getResource();
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        if (GetOperationOptions.isNoFetch(rootOptions) || GetOperationOptions.isRaw(rootOptions)) {
            return processNoFetchGet(ctx, repositoryShadow, options, now, task, parentResult);
        }

        if (!ResourceTypeUtil.isReadCapabilityEnabled(resource)) {
            UnsupportedOperationException e = new UnsupportedOperationException("Resource does not support 'read' operation");
            parentResult.recordFatalError(e);
            throw e;
        }

        if (shouldRefreshOnRead(resource, rootOptions)) {
            LOGGER.trace("Refreshing {} before reading", repositoryShadow);
            ProvisioningOperationOptions refreshOpts = toProvisioningOperationOptions(rootOptions);
            RefreshShadowOperation refreshShadowOperation = refreshShadow(repositoryShadow, refreshOpts, task, parentResult);
            if (refreshShadowOperation != null) {
                repositoryShadow = refreshShadowOperation.getRefreshedShadow();
            }
            LOGGER.trace("Refreshed repository shadow:\n{}", DebugUtil.debugDumpLazily(repositoryShadow,1));
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
            PrismObject<ShadowType> resultShadow = futurizeShadow(ctx, repositoryShadow, null, options, now);
            shadowCaretaker.applyAttributesDefinition(ctx, resultShadow);
            validateShadow(resultShadow, true);
            return resultShadow;
        }

        PrismObject<ShadowType> resourceShadow = null;

        Collection<? extends ResourceAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(repositoryShadow);
        if (primaryIdentifiers == null || primaryIdentifiers.isEmpty()) {
            if (ProvisioningUtil.hasPendingAddOperation(repositoryShadow) || ShadowUtil.isDead(repositoryShadow.asObjectable())) {
                if (ProvisioningUtil.isFuturePointInTime(options)) {
                    // Get of uncreated or dead shadow, we want to see future state (how the shadow WILL look like).
                    // We cannot even try fetch operation here. We do not have the identifiers.
                    // But we have quite a good idea how the shadow is going to look like. Therefore we can return it.
                    PrismObject<ShadowType> resultShadow = futurizeShadow(ctx, repositoryShadow, null, options, now);
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

        Collection<? extends ResourceAttribute<?>> identifiers = ShadowUtil.getAllIdentifiers(repositoryShadow);
        try {

            try {

                resourceShadow = resourceObjectConverter.getResourceObject(ctx, identifiers, true, parentResult);

            } catch (ObjectNotFoundException e) {
                // This may be OK, e.g. for connectors that have running async add operation.
                if (shadowState == ShadowState.CONCEPTION || shadowState == ShadowState.GESTATION) {
                    LOGGER.trace("{} was not found, but we can return cached shadow because it is in {} state", repositoryShadow, shadowState);
                    parentResult.deleteLastSubresultIfError();        // we don't want to see 'warning-like' orange boxes in GUI (TODO reconsider this)
                    parentResult.recordSuccess();

                    PrismObject<ShadowType> resultShadow = futurizeShadow(ctx, repositoryShadow, null, options, now);
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

            LOGGER.trace("Shadow returned by ResourceObjectConverter:\n{}", resourceShadow.debugDumpLazily(1));

            // Resource shadow may have different auxiliary object classes than
            // the original repo shadow. Make sure we have the definition that
            // applies to resource shadow. We will fix repo shadow later.
            // BUT we need also information about kind/intent and these information is only
            // in repo shadow, therefore the following 2 lines..
            resourceShadow.asObjectable().setKind(repositoryShadow.asObjectable().getKind());
            resourceShadow.asObjectable().setIntent(repositoryShadow.asObjectable().getIntent());
            ProvisioningContext shadowCtx = ctx.spawn(resourceShadow);

            String operationCtx = "getting " + repositoryShadow + " was successfull.";
            resourceManager.modifyResourceAvailabilityStatus(resource.asPrismObject(), AvailabilityStatusType.UP, operationCtx, parentResult);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Shadow from repository:\n{}", repositoryShadow.debugDump(1));
                LOGGER.trace("Resource object fetched from resource:\n{}", resourceShadow.debugDump(1));
            }

            repositoryShadow = shadowManager.updateShadow(shadowCtx, resourceShadow, null, repositoryShadow, shadowState, parentResult);
            LOGGER.trace("Repository shadow after update:\n{}", repositoryShadow.debugDumpLazily(1));

            // Complete the shadow by adding attributes from the resource object
            // This also completes the associations by adding shadowRefs
            PrismObject<ShadowType> assembledShadow = completeShadow(shadowCtx, resourceShadow, repositoryShadow, false, parentResult);
            LOGGER.trace("Shadow when assembled:\n{}", assembledShadow.debugDumpLazily(1));

            PrismObject<ShadowType> resultShadow = futurizeShadow(ctx, repositoryShadow, assembledShadow, options, now);
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
                PrismObject<ShadowType> futurizedShadow = futurizeShadow(ctx, handledShadow, null, options, now);
                validateShadow(futurizedShadow, true);
                return futurizedShadow;

            } catch (GenericFrameworkException | ObjectAlreadyExistsException | PolicyViolationException e) {
                throw new SystemException(e.getMessage(), e);
            }
        } finally {
            // We need to record the fetch down here. Now it is certain that we
            // are going to fetch from resource
            // (we do not have raw/noFetch option)
            // TODO: is this correct behaviour? don't we really want to record
            // fetch for protected objects?
            if (!ShadowUtil.isProtected(resourceShadow)) {
                InternalMonitor.recordCount(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
            }
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
        return GetOperationOptions.isForceRefresh(rootOptions) || GetOperationOptions.isForceRetry(rootOptions) || ResourceTypeUtil.isRefreshOnRead(resource);
    }

    private PrismObject<ShadowType> processNoFetchGet(ProvisioningContext ctx,
            PrismObject<ShadowType> repositoryShadow, Collection<SelectorOptions<GetOperationOptions>> options,
            XMLGregorianCalendar now, Task task, OperationResult parentResult)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {
        LOGGER.trace("Processing noFetch get for {}", repositoryShadow);

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        if (!GetOperationOptions.isRaw(rootOptions)) {
            // Even with noFetch we still want to delete expired pending operations. And even delete
            // the shadow if needed.
            repositoryShadow = refreshShadowQuick(ctx, repositoryShadow, now, task, parentResult);
        }

        if (repositoryShadow == null) {
            ObjectNotFoundException e = new ObjectNotFoundException("Resource object not found");
            parentResult.recordFatalError(e);
            throw e;
        }

        PrismObject<ShadowType> resultShadow = futurizeShadow(ctx, repositoryShadow, null, options, now);
        shadowCaretaker.applyAttributesDefinition(ctx, resultShadow);

        return resultShadow;
    }

    private PrismObject<ShadowType> futurizeShadow(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow, PrismObject<ShadowType> resourceShadow,
            Collection<SelectorOptions<GetOperationOptions>> options, XMLGregorianCalendar now) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        if (!ProvisioningUtil.isFuturePointInTime(options)) {
            if (resourceShadow == null) {
                return repoShadow;
            } else {
                return resourceShadow;
            }
        }
        return shadowCaretaker.applyPendingOperations(ctx, repoShadow, resourceShadow, false, now);
    }

    private boolean canReturnCachedAfterObjectNotFound(Collection<SelectorOptions<GetOperationOptions>> options,
            PrismObject<ShadowType> repositoryShadow, ResourceType resource) {
        if (repositoryShadow.asObjectable().getPendingOperation().isEmpty()) {
            return false;
        }
        // TODO: which case is this exactly?
        // Explicitly check the capability of the resource (primary connector), not capabilities of additional connectors
        return ProvisioningUtil.isPrimaryCachingOnly(resource);
    }

    private boolean canImmediatelyReturnCached(Collection<SelectorOptions<GetOperationOptions>> options, PrismObject<ShadowType> repositoryShadow, ShadowState shadowState, ResourceType resource) throws ConfigurationException {
        if (ProvisioningUtil.resourceReadIsCachingOnly(resource)) {
            return true;
        }
        if (shadowState == ShadowState.TOMBSTONE) {
            // Once shadow is buried it stays nine feet under. Therefore there is no point in trying to access the resource.
            // NOTE: this is just for tombstone! Schroedinger's shadows (corpse) will still work as if they were alive.
            return true;
        }
        long stalenessOption = GetOperationOptions.getStaleness(SelectorOptions.findRootOptions(options));
        PointInTimeType pit = GetOperationOptions.getPointInTimeType(SelectorOptions.findRootOptions(options));
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
                return isCachedShadowValid(options, repositoryShadow, resource);
            case FUTURE:
                // We could, e.g. if there is a pending create operation. But let's try real get operation first.
                return false;
            default:
                throw new IllegalArgumentException("Unknown point in time: "+pit);
        }
    }

    private boolean isCachedShadowValid(Collection<SelectorOptions<GetOperationOptions>> options, PrismObject<ShadowType> repositoryShadow, ResourceType resource) throws ConfigurationException {
        long stalenessOption = GetOperationOptions.getStaleness(SelectorOptions.findRootOptions(options));
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

    private boolean isCompensate(GetOperationOptions rootOptions) {
        return !GetOperationOptions.isDoNotDiscovery(rootOptions);
    }

    public String addShadow(PrismObject<ShadowType> shadowToAdd, OperationProvisioningScriptsType scripts,
            ResourceType resource, ProvisioningOperationOptions options, Task task,
            OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
                    ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException,
                    ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, EncryptionException {
        Validate.notNull(shadowToAdd, "Object to add must not be null.");

        InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Start adding shadow object{}:\n{}", getAdditionalOperationDesc(scripts, options), shadowToAdd.debugDump(1));
        }

        ProvisioningContext ctx = ctxFactory.create(shadowToAdd, task, parentResult);
        try {
            ctx.assertDefinition();
        } catch (SchemaException e) {
            parentResult.recordFatalError(e);
            ResourceOperationDescription operationDescription = ProvisioningUtil.createResourceFailureDescription(
                    shadowToAdd, ctx.getResource(), shadowToAdd.createAddDelta(), parentResult);
            operationListener.notifyFailure(operationDescription, task, parentResult);
            throw e;
        }

        ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState = new ProvisioningOperationState<>();

        return addShadowAttempt(ctx, shadowToAdd, scripts, opState, options, task, parentResult);
    }

    private String addShadowAttempt(ProvisioningContext ctx,
            PrismObject<ShadowType> shadowToAdd,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            ProvisioningOperationOptions options,
            Task task,
            OperationResult parentResult)
                    throws CommunicationException, GenericFrameworkException,
                    ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException,
                    ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, EncryptionException {

        PrismContainer<?> attributesContainer = shadowToAdd.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null || attributesContainer.isEmpty()) {
            SchemaException e = new SchemaException("Attempt to add shadow without any attributes: " + shadowToAdd);
            parentResult.recordFatalError(e);
            ResourceOperationDescription operationDescription = ProvisioningUtil.createResourceFailureDescription(
                    shadowToAdd, ctx.getResource(), shadowToAdd.createAddDelta(), parentResult);
            operationListener.notifyFailure(operationDescription, task, parentResult);
            throw e;
        }
        if (!(attributesContainer instanceof ResourceAttributeContainer)) {
            shadowCaretaker.applyAttributesDefinition(ctx, shadowToAdd);
            attributesContainer = shadowToAdd.findContainer(ShadowType.F_ATTRIBUTES);
        }

//        if (opState.getRepoShadow() != null) {
//            // HACK HACK HACK, not really right solution.
//            // We need this for reliable uniqueness check in preAddChecks() and addResourceObject()
//            // Maybe the right solution would be to pass opState as a parameter to addResourceObject()?
//            // Or maybe addResourceObject() should not check uniqueness and we shoudl check it here?
//            shadowToAdd.setOid(opState.getRepoShadow().getOid());
//        }

        preAddChecks(ctx, shadowToAdd, opState, task, parentResult);

        shadowManager.addNewProposedShadow(ctx, shadowToAdd, opState, task, parentResult);

        preprocessEntitlements(ctx, shadowToAdd, parentResult);

        shadowCaretaker.applyAttributesDefinition(ctx, shadowToAdd);
        shadowManager.setKindIfNecessary(shadowToAdd.asObjectable(), ctx.getObjectClassDefinition());
        accessChecker.checkAdd(ctx, shadowToAdd, parentResult);
        PrismObject<ShadowType> addedShadow = null;
        OperationResultStatus finalOperationStatus = null;

        if (shouldExecuteResourceOperationDirectly(ctx)) {

            ConnectorOperationOptions connOptions = createConnectorOperationOptions(ctx, options, parentResult);

            LOGGER.trace("ADD {}: resource operation, execution starting", shadowToAdd);

            try {

                // RESOURCE OPERATION: add
                AsynchronousOperationReturnValue<PrismObject<ShadowType>> asyncReturnValue =
                        resourceObjectConverter.addResourceObject(ctx, shadowToAdd, scripts, connOptions, false, parentResult);
                opState.processAsyncResult(asyncReturnValue);
                addedShadow = asyncReturnValue.getReturnValue();

            } catch (ObjectAlreadyExistsException e) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Object already exists error when trying to add {}, exploring the situation", ShadowUtil.shortDumpShadow(shadowToAdd));
                }

                // This exception may still be OK in some cases. Such as:
                // We are trying to add a shadow to a semi-manual connector.
                // But the resource object was recently deleted. The object is
                // still in the backing store (CSV file) because of a grace
                // period. Obviously, attempt to add such object would fail.
                // So, we need to handle this case specially. (MID-4414)

                OperationResult failedOperationResult = parentResult.getLastSubresult();

                if (hasDeadShadowWithDeleteOperation(ctx, shadowToAdd, parentResult)) {

                    if (failedOperationResult.isError()) {
                        failedOperationResult.setStatus(OperationResultStatus.HANDLED_ERROR);
                    }

                    // Try again, this time without explicit uniqueness check
                    try {

                        LOGGER.trace("ADD {}: retrying resource operation without uniqueness check (previous dead shadow found), execution starting", shadowToAdd);
                        AsynchronousOperationReturnValue<PrismObject<ShadowType>> asyncReturnValue =
                                resourceObjectConverter
                                        .addResourceObject(ctx, shadowToAdd, scripts, connOptions, true, parentResult);
                        opState.processAsyncResult(asyncReturnValue);
                        addedShadow = asyncReturnValue.getReturnValue();

                    } catch (ObjectAlreadyExistsException innerException) {
                        // Mark shadow dead before we handle the error. ADD operation obviously failed. Therefore this particular
                        // shadow was not created as resource object. It is dead on the spot. Make sure that error handler won't confuse
                        // this shadow with the conflicting shadow that it is going to discover.
                        // This may also be a gestation quantum state collapsing to tombstone
                        shadowManager.markShadowTombstone(opState.getRepoShadow(), parentResult);
                        finalOperationStatus = handleAddError(ctx, shadowToAdd, options, opState, innerException, failedOperationResult, task, parentResult);
                    } catch (Exception innerException) {
                        finalOperationStatus = handleAddError(ctx, shadowToAdd, options, opState, innerException, parentResult.getLastSubresult(), task, parentResult);
                    }

                } else {
                    // Mark shadow dead before we handle the error. ADD operation obviously failed. Therefore this particular
                    // shadow was not created as resource object. It is dead on the spot. Make sure that error handler won't confuse
                    // this shadow with the conflicting shadow that it is going to discover.
                    // This may also be a gestation quantum state collapsing to tombstone
                    shadowManager.markShadowTombstone(opState.getRepoShadow(), parentResult);
                    finalOperationStatus = handleAddError(ctx, shadowToAdd, options, opState, e, failedOperationResult, task, parentResult);
                }

            } catch (Exception e) {
                finalOperationStatus = handleAddError(ctx, shadowToAdd, options, opState, e, parentResult.getLastSubresult(), task, parentResult);
            }

            LOGGER.debug("ADD {}: resource operation executed, operation state: {}", shadowToAdd, opState.shortDumpLazily());

        } else {
            opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTION_PENDING);
            // Create dummy subresult with IN_PROGRESS state.
            // This will force the entire result (parent) to be IN_PROGRESS rather than SUCCESS.
            OperationResult delayedSubresult = parentResult.createSubresult(OP_DELAYED_OPERATION);
            delayedSubresult.setStatus(OperationResultStatus.IN_PROGRESS);
            LOGGER.debug("ADD {}: resource operation NOT executed, execution pending", shadowToAdd);
        }

        // REPO OPERATION: add
        // This is where the repo shadow is created or updated (if needed)
        shadowManager.recordAddResult(ctx, shadowToAdd, opState, parentResult);

        if (addedShadow == null) {
            addedShadow = shadowToAdd;
        }
        addedShadow.setOid(opState.getRepoShadow().getOid());

        notifyAfterAdd(ctx, addedShadow, opState, task, parentResult);

        setParentOperationStatus(parentResult, opState, finalOperationStatus);

        return opState.getRepoShadow().getOid();
    }

    private void setParentOperationStatus(OperationResult parentResult,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            OperationResultStatus finalOperationStatus) {
        if (finalOperationStatus != null) {
            parentResult.setStatus(finalOperationStatus);
        } else {
            if (opState.isCompleted()) {
                parentResult.computeStatus();
            } else {
                parentResult.recordInProgress();
            }
        }
        parentResult.setAsynchronousOperationReference(opState.getAsynchronousOperationReference());
    }

    private boolean hasDeadShadowWithDeleteOperation(ProvisioningContext ctx,
            PrismObject<ShadowType> shadowToAdd,
            OperationResult parentResult)
                    throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {


        Collection<PrismObject<ShadowType>> previousDeadShadows = shadowManager.lookForPreviousDeadShadows(ctx, shadowToAdd, parentResult);
        if (previousDeadShadows.isEmpty()) {
            return false;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Previous dead shadows:\n{}", DebugUtil.debugDump(previousDeadShadows, 1));
        }

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        for (PrismObject<ShadowType> previousDeadShadow : previousDeadShadows) {
            if (shadowCaretaker.findPreviousPendingLifecycleOperationInGracePeriod(ctx, previousDeadShadow, now) == ChangeTypeType.DELETE) {
                return true;
            }
        }
        return false;
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

    private OperationResultStatus handleAddError(ProvisioningContext ctx,
            PrismObject<ShadowType> shadowToAdd,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            Exception cause,
            OperationResult failedOperationResult,
            Task task,
            OperationResult parentResult)
                    throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        // TODO: record operationExecution

        ErrorHandler handler = errorHandlerLocator.locateErrorHandler(cause);
        if (handler == null) {
            parentResult.recordFatalError("Error without a handler: " + cause.getMessage(), cause);
            throw new SystemException(cause.getMessage(), cause);
        }
        LOGGER.debug("Handling provisioning ADD exception {}: {}", cause.getClass(), cause.getMessage());
        try {

            OperationResultStatus finalStatus = handler.handleAddError(ctx, shadowToAdd, options, opState, cause, failedOperationResult, task, parentResult);
            LOGGER.debug("Handled provisioning ADD exception, final status: {}, operation state: {}", finalStatus, opState.shortDumpLazily());
            return finalStatus;

        } catch (CommonException e) {
            LOGGER.debug("Handled provisioning ADD exception, final exception: {}, operation state: {}", e, opState.shortDumpLazily());
            ObjectDelta<ShadowType> delta = shadowToAdd.createAddDelta();
            handleErrorHandlerException(ctx, opState, delta, task, parentResult);
            throw e;
        }

    }

    private void handleErrorHandlerException(ProvisioningContext ctx,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            ObjectDelta<ShadowType> delta,
            Task task, OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
        // Error handler had re-thrown the exception. We will throw the exception later. But first we need to record changes in opState.
        shadowManager.recordOperationException(ctx, opState, delta, parentResult);

        PrismObject<ShadowType> shadow = opState.getRepoShadow();
        if (delta.isAdd()) {
            // This is more precise. Besides, there is no repo shadow in some cases (e.g. adding protected shadow).
            shadow = delta.getObjectToAdd();
        }
        ResourceOperationDescription operationDescription = ProvisioningUtil.createResourceFailureDescription(shadow, ctx.getResource(), delta, parentResult);
        operationListener.notifyFailure(operationDescription, task, parentResult);
    }

    private OperationResultStatus handleModifyError(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow,
            Collection<? extends ItemDelta> modifications,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            Exception cause,
            OperationResult failedOperationResult,
            Task task,
            OperationResult parentResult)
                    throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        // TODO: record operationExecution

        ErrorHandler handler = errorHandlerLocator.locateErrorHandler(cause);
        if (handler == null) {
            parentResult.recordFatalError("Error without a handler: " + cause.getMessage(), cause);
            throw new SystemException(cause.getMessage(), cause);
        }
        LOGGER.debug("Handling provisioning MODIFY exception {}: {}", cause.getClass(), cause.getMessage());
        try {

            OperationResultStatus finalStatus = handler.handleModifyError(ctx, repoShadow, modifications, options, opState, cause, failedOperationResult, task, parentResult);
            LOGGER.debug("Handled provisioning MODIFY exception, final status: {}, operation state: {}", finalStatus, opState.shortDumpLazily());
            return finalStatus;

        } catch (CommonException e) {
            LOGGER.debug("Handled provisioning MODIFY exception, final exception: {}, operation state: {}", e, opState.shortDumpLazily());
            ObjectDelta<ShadowType> delta = repoShadow.createModifyDelta();
            delta.addModifications(modifications);
            handleErrorHandlerException(ctx, opState, delta, task, parentResult);
            throw e;
        }
    }

    private OperationResultStatus handleDeleteError(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            Exception cause,
            OperationResult failedOperationResult,
            Task task,
            OperationResult parentResult)
                    throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        ErrorHandler handler = errorHandlerLocator.locateErrorHandler(cause);
        if (handler == null) {
            parentResult.recordFatalError("Error without a handler: " + cause.getMessage(), cause);
            throw new SystemException(cause.getMessage(), cause);
        }
        LOGGER.debug("Handling provisioning DELETE exception {}: {}", cause.getClass(), cause.getMessage());
        try {

            OperationResultStatus finalStatus = handler.handleDeleteError(ctx, repoShadow, options, opState, cause, failedOperationResult, task, parentResult);
            LOGGER.debug("Handled provisioning DELETE exception, final status: {}, operation state: {}", finalStatus, opState.shortDumpLazily());
            return finalStatus;

        } catch (CommonException e) {
            LOGGER.debug("Handled provisioning DELETE exception, final exception: {}, operation state: {}", e, opState.shortDumpLazily());
            ObjectDelta<ShadowType> delta = repoShadow.createDeleteDelta();
            handleErrorHandlerException(ctx, opState, delta, task, parentResult);
            throw e;
        }

    }

    private void notifyAfterAdd(
            ProvisioningContext ctx,
            PrismObject<ShadowType> addedShadow,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            Task task,
            OperationResult parentResult)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectDelta<ShadowType> delta = DeltaFactory.Object.createAddDelta(addedShadow);
        ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, addedShadow,
                delta, parentResult);

        if (opState.isExecuting()) {
            operationListener.notifyInProgress(operationDescription, task, parentResult);
        } else if (opState.isCompleted()) {
            operationListener.notifySuccess(operationDescription, task, parentResult);
        }
    }

    private void preAddChecks(ProvisioningContext ctx, PrismObject<ShadowType> shadow, ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, SecurityViolationException {
        checkConstraints(ctx, shadow, opState, task, result);
        validateSchema(ctx, shadow, task, result);
    }

    private void checkConstraints(ProvisioningContext ctx, PrismObject<ShadowType> shadow, ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, SecurityViolationException {
        ShadowCheckType shadowConstraintsCheck = ResourceTypeUtil.getShadowConstraintsCheck(ctx.getResource());
        if (shadowConstraintsCheck == ShadowCheckType.NONE) {
            return;
        }

        String shadowOid = shadow.getOid();
        if (opState.getRepoShadow() != null) {
            shadowOid = opState.getRepoShadow().getOid();
        }

        ConstraintsChecker checker = new ConstraintsChecker();
        checker.setRepositoryService(repositoryService);
        checker.setCacheConfigurationManager(cacheConfigurationManager);
        checker.setShadowCache(this);
        checker.setPrismContext(prismContext);
        checker.setProvisioningContext(ctx);
        checker.setShadowObject(shadow);
        checker.setShadowOid(shadowOid);
        checker.setConstraintViolationConfirmer(
                conflictingShadowCandidate -> isNotDeadOrReaping(conflictingShadowCandidate, ctx));
        checker.setUseCache(false);

        ConstraintsCheckingResult retval = checker.check(task, result);

        LOGGER.trace("Checked {} constraints, result={}", shadow.debugDump(), retval.isSatisfiesConstraints());
        if (!retval.isSatisfiesConstraints()) {
            throw new ObjectAlreadyExistsException("Conflicting shadow already exists on "+ctx.getResource());
        }
    }

    private boolean isNotDeadOrReaping(PrismObject<ShadowType> shadow, ProvisioningContext ctx)
            throws SchemaException {
        if (ShadowUtil.isDead(shadow)) {
            return false;
        }
        ProvisioningContext shadowCtx = ctx.spawn(shadow);
        ShadowState state;
        try {
            state = shadowCaretaker.determineShadowState(shadowCtx, shadow, clock.currentTimeXMLGregorianCalendar());
        } catch (ObjectNotFoundException | CommunicationException | ConfigurationException |
                 ExpressionEvaluationException e) {
            throw new SystemException("Couldn't determine shadow state: " + e.getMessage(), e);
        }
        return state != REAPING && state != CORPSE && state != TOMBSTONE;
    }

    private void validateSchema(ProvisioningContext ctx, PrismObject<ShadowType> shadow, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (ResourceTypeUtil.isValidateSchema(ctx.getResource())) {
            ShadowUtil.validateAttributeSchema(shadow, ctx.getObjectClassDefinition());
        }
    }

    private boolean shouldExecuteResourceOperationDirectly(ProvisioningContext ctx) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (ctx.isPropagation()) {
            return true;
        }
        ResourceConsistencyType consistency = ctx.getResource().getConsistency();
        if (consistency == null) {
            return true;
        }
        Duration operationGroupingInterval = consistency.getOperationGroupingInterval();
        if (operationGroupingInterval == null) {
            return true;
        }
        return false;
    }

    private ResourceOperationDescription createSuccessOperationDescription(ProvisioningContext ctx,
            PrismObject<ShadowType> shadowType, ObjectDelta delta, OperationResult parentResult)
                    throws ObjectNotFoundException, SchemaException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {
        ResourceOperationDescription operationDescription = new ResourceOperationDescription();
        operationDescription.setCurrentShadow(shadowType);
        operationDescription.setResource(ctx.getResource().asPrismObject());
        if (ctx.getTask() != null) {
            operationDescription.setSourceChannel(ctx.getTask().getChannel());
        }
        operationDescription.setObjectDelta(delta);
        operationDescription.setResult(parentResult);
        return operationDescription;
    }

    public String modifyShadow(PrismObject<ShadowType> repoShadow,
            Collection<? extends ItemDelta> modifications, OperationProvisioningScriptsType scripts,
            ProvisioningOperationOptions options, Task task, OperationResult parentResult)
                    throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
                    SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, EncryptionException, ObjectAlreadyExistsException {

        Validate.notNull(repoShadow, "Object to modify must not be null.");
        Validate.notNull(modifications, "Object modification must not be null.");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Start modifying {}{}:\n{}", repoShadow, getAdditionalOperationDesc(scripts, options),
                    DebugUtil.debugDump(modifications, 1));
        }

        InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);

        Collection<QName> additionalAuxiliaryObjectClassQNames = new ArrayList<>();
        for (ItemDelta modification : modifications) {
            if (ShadowType.F_AUXILIARY_OBJECT_CLASS.equivalent(modification.getPath())) {
                PropertyDelta<QName> auxDelta = (PropertyDelta<QName>) modification;
                for (PrismPropertyValue<QName> pval : auxDelta.getValues(QName.class)) {
                    additionalAuxiliaryObjectClassQNames.add(pval.getValue());
                }
            }
        }

        ProvisioningContext ctx = ctxFactory.create(repoShadow, additionalAuxiliaryObjectClassQNames, task, parentResult);
        ctx.assertDefinition();

        ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState = new ProvisioningOperationState<>();
        opState.setRepoShadow(repoShadow);

        // if not explicitly we want to force retry operations during modify
        // it is quite cheap and probably more safe then not do it
        if (options == null) {
            options = ProvisioningOperationOptions.createForceRetry(Boolean.TRUE);
        } else if (options.getForceRetry() == null) {
            options.setForceRetry(Boolean.TRUE);
        }

        return modifyShadowAttempt(ctx, modifications, scripts, options, opState, false, task, parentResult);
    }

    private String modifyShadowAttempt(ProvisioningContext ctx,
            Collection<? extends ItemDelta> modifications,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            boolean inRefresh,
            Task task, OperationResult parentResult)
                    throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
                    SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, EncryptionException, ObjectAlreadyExistsException {

        PrismObject<ShadowType> repoShadow = opState.getRepoShadow();

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        PendingOperationType duplicateOperation = shadowManager.checkAndRecordPendingModifyOperationBeforeExecution(ctx, repoShadow, modifications, opState, task, parentResult);
        if (duplicateOperation != null) {
            parentResult.recordInProgress();
            return repoShadow.getOid();
        }

        shadowCaretaker.applyAttributesDefinition(ctx, repoShadow);

        accessChecker.checkModify(ctx, repoShadow, modifications, parentResult);

        preprocessEntitlements(ctx, modifications, "delta for shadow " + repoShadow.getOid(), parentResult);

        OperationResultStatus finalOperationStatus = null;

        if (shadowManager.isRepositoryOnlyModification(modifications)) {
            opState.setExecutionStatus(PendingOperationExecutionStatusType.COMPLETED);
            LOGGER.debug("MODIFY {}: repository-only modification", repoShadow);
        } else {
            if (shouldExecuteResourceOperationDirectly(ctx)) {
                LOGGER.trace("MODIFY {}: resource modification, execution starting\n{}", repoShadow, DebugUtil.debugDumpLazily(modifications));

                RefreshShadowOperation refreshShadowOperation = null;
                if (!inRefresh && shouldRefresh(repoShadow)) {
                    refreshShadowOperation = refreshShadow(repoShadow, options, task, parentResult);
                }

                if (refreshShadowOperation != null) {
                    repoShadow = refreshShadowOperation.getRefreshedShadow();
                }

                if (repoShadow == null) {
                    LOGGER.trace("Shadow is gone. Nothing more to do");
                    parentResult.recordPartialError("Shadow disappeared during modify.");
                    throw new ObjectNotFoundException("Shadow is gone.");
                }

                ConnectorOperationOptions connOptions = createConnectorOperationOptions(ctx, options, parentResult);

                try {


                    if (!shouldExecuteModify(refreshShadowOperation)) {
                        ProvisioningUtil.postponeModify(ctx, repoShadow, modifications, opState, refreshShadowOperation.getRefreshResult(), parentResult);
                        shadowManager.recordModifyResult(ctx, repoShadow, modifications, opState, now, parentResult);
                        return repoShadow.getOid();
                    } else {
                        LOGGER.trace("Shadow exists: ", repoShadow.debugDump());
                    }

                    AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>> asyncReturnValue =
                            resourceObjectConverter
                                    .modifyResourceObject(ctx, repoShadow, scripts, connOptions, modifications, now, parentResult);
                    opState.processAsyncResult(asyncReturnValue);

                    Collection<PropertyDelta<PrismPropertyValue>> sideEffectChanges = asyncReturnValue.getReturnValue();
                    if (sideEffectChanges != null) {
                        ItemDeltaCollectionsUtil.addAll(modifications, sideEffectChanges);
                    }

                } catch (Exception ex) {
                    LOGGER.debug("Provisioning exception: {}:{}, attempting to handle it",
                            ex.getClass(), ex.getMessage(), ex);
                    finalOperationStatus = handleModifyError(ctx, repoShadow, modifications, options, opState, ex, parentResult.getLastSubresult(), task, parentResult);
                }

                LOGGER.debug("MODIFY {}: resource operation executed, operation state: {}", repoShadow, opState.shortDumpLazily());

            } else {
                opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTION_PENDING);
                // Create dummy subresult with IN_PROGRESS state.
                // This will force the entire result (parent) to be IN_PROGRESS rather than SUCCESS.
                OperationResult delayedSubresult = parentResult.createSubresult(OP_DELAYED_OPERATION);
                delayedSubresult.setStatus(OperationResultStatus.IN_PROGRESS);
                LOGGER.debug("MODIFY {}: Resource operation NOT executed, execution pending", repoShadow);
            }
        }

        shadowManager.recordModifyResult(ctx, repoShadow, modifications, opState, now, parentResult);

        notifyAfterModify(ctx, repoShadow, modifications, opState, task, parentResult);

        setParentOperationStatus(parentResult, opState, finalOperationStatus);

        return repoShadow.getOid();
    }

    private boolean shouldExecuteModify(RefreshShadowOperation refreshShadowOperation) {
        if (refreshShadowOperation == null) {
            LOGGER.trace("Nothing refreshed, modify can continue.");
            return true;
        }

        if (refreshShadowOperation.getExecutedDeltas() == null || refreshShadowOperation.getExecutedDeltas().isEmpty()) {
            LOGGER.trace("No executed deltas after refresh. Continue with modify operation.");
            return true;
        }

        if (refreshShadowOperation.getRefreshedShadow() == null) {
            LOGGER.trace("Shadow is gone. Probably it was deleted during refresh. Finishing modify operation now.");
            return false;
        }

        Collection<ObjectDeltaOperation<ShadowType>> objectDeltaOperations = refreshShadowOperation.getExecutedDeltas();
        for (ObjectDeltaOperation<ShadowType> shadowDelta : objectDeltaOperations) {
            if (!shadowDelta.getExecutionResult().isSuccess()) {
                LOGGER.trace("Refresh operation not successful. Finishing modify operation now.");
                return false;
            }
        }

        return true;
    }

    private boolean shouldRefresh(PrismObject<ShadowType> repoShadow) {
        if (repoShadow == null) {
            return false;
        }

        List<PendingOperationType> pendingOperations = repoShadow.asObjectable().getPendingOperation();
        if (pendingOperations == null || pendingOperations.isEmpty()) {
            return false;
        }

        for (PendingOperationType pendingOperationType : pendingOperations) {
            if (needsRetry(pendingOperationType)) {
                return true;
            }
        }

        return false;
    }

    private void notifyAfterModify(
            ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow,
            Collection<? extends ItemDelta> modifications,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            Task task,
            OperationResult parentResult)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModifyDelta(repoShadow.getOid(), modifications,
                repoShadow.getCompileTimeClass());
        ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, repoShadow,
                delta, parentResult);

        if (opState.isExecuting()) {
            operationListener.notifyInProgress(operationDescription, task, parentResult);
        } else {
            operationListener.notifySuccess(operationDescription, task, parentResult);
        }
    }

    /**
     * Used to execute delayed operations.
     * Mostly copy&paste from modifyShadow(). But as consistency (handleError()) branch expects to return immediately
     * I could not find a more elegant way to structure this without complicating the code too much.
     */
    private ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> executeResourceModify(
            ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow,
            Collection<? extends ItemDelta> modifications,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationOptions options,
            XMLGregorianCalendar now,
            Task task,
            OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState = new ProvisioningOperationState<>();
        opState.setRepoShadow(repoShadow);

        ConnectorOperationOptions connOptions = createConnectorOperationOptions(ctx, options, parentResult);

        try {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Applying change: {}", DebugUtil.debugDump(modifications));
            }

            AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>> asyncReturnValue =
                    resourceObjectConverter
                            .modifyResourceObject(ctx, repoShadow, scripts, connOptions, modifications, now, parentResult);
            opState.processAsyncResult(asyncReturnValue);

            Collection<PropertyDelta<PrismPropertyValue>> sideEffectChanges = asyncReturnValue.getReturnValue();
            if (sideEffectChanges != null) {
                ItemDeltaCollectionsUtil.addAll(modifications, sideEffectChanges);
            }

        } catch (Exception ex) {
            LOGGER.debug("Provisioning exception: {}:{}, attempting to handle it",
                    ex.getClass(), ex.getMessage(), ex);
            try {
                handleModifyError(ctx, repoShadow, modifications, options, opState, ex, parentResult.getLastSubresult(), task, parentResult);
                parentResult.computeStatus();
            } catch (ObjectAlreadyExistsException e) {
                parentResult.recordFatalError(
                        "While compensating communication problem for modify operation got: "
                                + ex.getMessage(),
                        ex);
                throw new SystemException(e);
            }

        }

        return opState;
    }

    public PrismObject<ShadowType> deleteShadow(PrismObject<ShadowType> repoShadow, ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts, Task task, OperationResult parentResult)
                    throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
                    SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        Validate.notNull(repoShadow, "Object to delete must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Start deleting {}{}", repoShadow, getAdditionalOperationDesc(scripts, options));
        }

        InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);

        ProvisioningContext ctx = ctxFactory.create(repoShadow, task, parentResult);
        try {
            ctx.assertDefinition();
        } catch (ObjectNotFoundException ex) {
            // if the force option is set, delete shadow from the repo
            // although the resource does not exists..
            if (ProvisioningOperationOptions.isForce(options)) {
                parentResult.muteLastSubresultError();
                shadowManager.deleteShadow(ctx, repoShadow, parentResult);
                parentResult.recordHandledError(
                        "Resource defined in shadow does not exists. Shadow was deleted from the repository.");
                return null;
            } else {
                throw ex;
            }
        }

        repoShadow = cancelAllPendingOperations(ctx, repoShadow, task, parentResult);

        ProvisioningOperationState<AsynchronousOperationResult> opState = new ProvisioningOperationState<>();
        opState.setRepoShadow(repoShadow);

        return deleteShadowAttempt(ctx, options, scripts, opState, task, parentResult);
    }


    private PrismObject<ShadowType> deleteShadowAttempt(ProvisioningContext ctx,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            Task task,
            OperationResult parentResult)
                    throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
                    SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        PrismObject<ShadowType> repoShadow = opState.getRepoShadow();
        shadowCaretaker.applyAttributesDefinition(ctx, repoShadow);

        PendingOperationType duplicateOperation = shadowManager.checkAndRecordPendingDeleteOperationBeforeExecution(ctx, repoShadow, opState, task, parentResult);
        if (duplicateOperation != null) {
            parentResult.recordInProgress();
            return repoShadow;
        }

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        ShadowState shadowState = shadowCaretaker.determineShadowState(ctx, repoShadow, now);

        LOGGER.trace("Deleting object {} from {}, options={}, shadowState={}", repoShadow, ctx.getResource(), options, shadowState);
        OperationResultStatus finalOperationStatus = null;

        if (shouldExecuteResourceOperationDirectly(ctx)) {

            if (shadowState == ShadowState.TOMBSTONE) {

                // Do not even try to delete resource object for tombstone shadows.
                // There may be dead shadow and live shadow for the resource object with the same identifiers.
                // If we try to delete dead shadow then we might delete existing object by mistake
                LOGGER.trace("DELETE {}: skipping resource deletion on tombstone shadow", repoShadow);

                opState.setExecutionStatus(PendingOperationExecutionStatusType.COMPLETED);
                OperationResult delayedSubresult = parentResult.createSubresult(OP_RESOURCE_OPERATION);
                delayedSubresult.setStatus(OperationResultStatus.NOT_APPLICABLE);

            } else {

                ConnectorOperationOptions connOptions = createConnectorOperationOptions(ctx, options, parentResult);

                LOGGER.trace("DELETE {}: resource deletion, execution starting", repoShadow);

                try {

                    AsynchronousOperationResult asyncReturnValue = resourceObjectConverter
                            .deleteResourceObject(ctx, repoShadow, scripts, connOptions, parentResult);
                    opState.processAsyncResult(asyncReturnValue);

                    String operationCtx = "deleting " + repoShadow + " finished successfully.";
                    resourceManager.modifyResourceAvailabilityStatus(ctx.getResource().asPrismObject(),
                            AvailabilityStatusType.UP, operationCtx, parentResult);

                } catch (Exception ex) {
                    try {
                        finalOperationStatus = handleDeleteError(ctx, repoShadow, options, opState, ex, parentResult.getLastSubresult(), task, parentResult);
                    } catch (ObjectAlreadyExistsException e) {
                        parentResult.recordFatalError(e);
                        throw new SystemException(e.getMessage(), e);
                    }
                }

                LOGGER.debug("DELETE {}: resource operation executed, operation state: {}", repoShadow, opState.shortDumpLazily());
            }

        } else {
            opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTION_PENDING);
            // Create dummy subresult with IN_PROGRESS state.
            // This will force the entire result (parent) to be IN_PROGRESS rather than SUCCESS.
            OperationResult delayedSubresult = parentResult.createSubresult(OP_DELAYED_OPERATION);
            delayedSubresult.setStatus(OperationResultStatus.IN_PROGRESS);
            LOGGER.debug("DELETE {}: resource operation NOT executed, execution pending", repoShadow);
        }

        now = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> resultShadow;
        try {
            resultShadow = shadowManager.recordDeleteResult(ctx, repoShadow, opState, options, now, parentResult);
        } catch (ObjectNotFoundException ex) {
            parentResult.recordFatalError("Can't delete object " + repoShadow + ". Reason: " + ex.getMessage(),
                    ex);
            throw new ObjectNotFoundException("An error occurred while deleting resource object " + repoShadow
                    + "with identifiers " + repoShadow + ": " + ex.getMessage(), ex);
        } catch (EncryptionException e) {
            throw new SystemException(e.getMessage(), e);
        }

        notifyAfterDelete(ctx, repoShadow, opState, task, parentResult);

        setParentOperationStatus(parentResult, opState, finalOperationStatus);

        LOGGER.trace("Delete operation for {} finished, result shadow: {}", repoShadow, resultShadow);
        return resultShadow;
    }

    private ProvisioningOperationState<AsynchronousOperationResult> executeResourceDelete(
            ProvisioningContext ctx,
            PrismObject<ShadowType> shadow,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationOptions options,
            Task task,
            OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        ProvisioningOperationState<AsynchronousOperationResult> opState = new ProvisioningOperationState<>();
        opState.setRepoShadow(shadow);
        ConnectorOperationOptions connOptions = createConnectorOperationOptions(ctx, options, parentResult);
        try {

            AsynchronousOperationResult asyncReturnValue = resourceObjectConverter
                    .deleteResourceObject(ctx, shadow, scripts, connOptions , parentResult);
            opState.processAsyncResult(asyncReturnValue);

        } catch (Exception ex) {
            try {
                handleDeleteError(ctx, shadow, options, opState, ex, parentResult.getLastSubresult(), task, parentResult);
            } catch (ObjectAlreadyExistsException e) {
                parentResult.recordFatalError(e);
                throw new SystemException(e.getMessage(), e);
            }
        }

        return opState;
    }

    private void notifyAfterDelete(
            ProvisioningContext ctx,
            PrismObject<ShadowType> shadow,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            Task task,
            OperationResult parentResult)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createDeleteDelta(shadow.getCompileTimeClass(),
                shadow.getOid());
        ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, shadow,
                delta, parentResult);

        if (opState.isExecuting()) {
            operationListener.notifyInProgress(operationDescription, task, parentResult);
        } else {
            operationListener.notifySuccess(operationDescription, task, parentResult);
        }
    }


    @Nullable
    public RefreshShadowOperation refreshShadow(PrismObject<ShadowType> repoShadow, ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {
        LOGGER.trace("Refreshing {}", repoShadow);
        ProvisioningContext ctx = ctxFactory.create(repoShadow, task, parentResult);
        ctx.assertDefinition();
        shadowCaretaker.applyAttributesDefinition(ctx, repoShadow);

        try {
            repoShadow = shadowManager.refreshProvisioningIndexes(ctx, repoShadow, true, parentResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected exception while refreshing provisioning indexes: " + e.getMessage(), e);
        }

        RefreshShadowOperation refreshShadowOperation = refreshShadowPendingOperations(ctx, repoShadow, options, task, parentResult);

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        repoShadow = cleanUpDeadShadow(ctx, repoShadow, now, task, parentResult);
        if (repoShadow == null) {
            refreshShadowOperation.setRefreshedShadow(null);
        }

        updateProvisioningIndexesAfterDeletion(ctx, refreshShadowOperation, parentResult);

        return refreshShadowOperation;
    }

    /**
     * When a deletion is determined to be failed, we try to restore the `primaryIdentifierValue` index.
     * (We may be unsuccessful if there was another shadow created in the meanwhile.)
     *
     * This method assumes that the pending operations have been already updated with the result of retried operations.
     *
     * (For simplicity and robustness, we just refresh provisioning indexes. It should be efficient enough.)
     */
    private void updateProvisioningIndexesAfterDeletion(
            ProvisioningContext ctx, RefreshShadowOperation refreshShadowOperation, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        PrismObject<ShadowType> shadow = refreshShadowOperation.getRefreshedShadow();
        if (shadow == null) {
            LOGGER.trace("updateProvisioningIndexesAfterDeletion: no shadow");
            return;
        }
        if (emptyIfNull(refreshShadowOperation.getExecutedDeltas()).stream()
                .noneMatch(d -> ObjectDelta.isDelete(d.getObjectDelta()))) {
            LOGGER.trace("updateProvisioningIndexesAfterDeletion: no DELETE delta found");
            return;
        }
        try {
            shadowManager.refreshProvisioningIndexes(ctx, shadow, false, result);
        } catch (ObjectAlreadyExistsException e) {
            LOGGER.debug("Couldn't set `primaryIdentifierValue` for {} - probably a new one was created in the meanwhile. "
                    + "Marking this one as dead.", shadow, e);
            shadowManager.markShadowTombstone(shadow, result);
        }
    }

    private RefreshShadowOperation refreshShadowPendingOperations(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow, ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {
        ShadowType shadowType = repoShadow.asObjectable();
        List<PendingOperationType> pendingOperations = shadowType.getPendingOperation();
        boolean isDead = ShadowUtil.isDead(shadowType);
        if (!isDead && pendingOperations.isEmpty()) {
            LOGGER.trace("Skipping refresh of {} pending operations because shadow is not dead and there are no pending operations", repoShadow);
            RefreshShadowOperation rso = new RefreshShadowOperation();
            rso.setRefreshedShadow(repoShadow);
            return rso;
        }

        LOGGER.trace("Pending operations refresh of {}, dead={}, {} pending operations", repoShadow, isDead, pendingOperations.size());

        ctx.assertDefinition();
        List<PendingOperationType> sortedOperations = shadowCaretaker.sortPendingOperations(shadowType.getPendingOperation());

        repoShadow = refreshShadowAsyncStatus(ctx, repoShadow, sortedOperations, task, parentResult);

        RefreshShadowOperation refreshShadowOperation = refreshShadowRetryOperations(ctx, repoShadow, sortedOperations, options, task, parentResult);

        return refreshShadowOperation;
    }

    /**
     * Used to quickly and efficiently refresh shadow before GET operations.
     */
    private PrismObject<ShadowType> refreshShadowQuick(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow,
            XMLGregorianCalendar now, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {

        ObjectDelta<ShadowType> shadowDelta = repoShadow.createModifyDelta();
        expirePendingOperations(ctx, repoShadow, shadowDelta, now, parentResult);

        if (!shadowDelta.isEmpty()) {
            shadowManager.modifyShadowAttributes(ctx, repoShadow, shadowDelta.getModifications(), parentResult);
            shadowDelta.applyTo(repoShadow);
        }

        repoShadow = cleanUpDeadShadow(ctx, repoShadow, now, task, parentResult);

        return repoShadow;
    }

    /**
     * Refresh status of asynchronous operation, e.g. status of manual connector ticket.
     * This method will get new status from resouceObjectConverter and it will process the
     * status in case that it has changed.
     */
    private PrismObject<ShadowType> refreshShadowAsyncStatus(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow, List<PendingOperationType> sortedOperations, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {
        Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);

        List<ObjectDelta<ShadowType>> notificationDeltas = new ArrayList<>();

        boolean shadowInception = false;
        ObjectDelta<ShadowType> shadowDelta = repoShadow.createModifyDelta();
        for (PendingOperationType pendingOperation: sortedOperations) {

            if (!needsRefresh(pendingOperation)) {
                continue;
            }

            ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();

            String asyncRef = pendingOperation.getAsynchronousOperationReference();
            if (asyncRef == null) {
                continue;
            }

            AsynchronousOperationResult refreshAsyncResult;
            try {
                refreshAsyncResult = resourceObjectConverter.refreshOperationStatus(ctx, repoShadow, asyncRef, parentResult);
            } catch (CommunicationException e) {
                LOGGER.debug("Communication error while trying to refresh pending operation of {}. Skipping refresh of this operation.", repoShadow, e);
                parentResult.recordPartialError(e);
                continue;
            }
            OperationResultStatus newStatus = refreshAsyncResult.getOperationResult().getStatus();

            if (newStatus == null) {
                continue;
            }
            OperationResultStatusType newStatusType = newStatus.createStatusType();
            if (newStatusType.equals(pendingOperation.getResultStatus())) {
                continue;
            }


            boolean operationCompleted = ProvisioningUtil.isCompleted(newStatusType) && pendingOperation.getCompletionTimestamp() == null;

            if (operationCompleted && gracePeriod == null) {
                LOGGER.trace("Deleting pending operation because it is completed (no grace): {}", pendingOperation);
                shadowDelta.addModificationDeleteContainer(ShadowType.F_PENDING_OPERATION, pendingOperation.clone());

                ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
                ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);

                if (pendingDelta.isAdd()) {
                    shadowInception = true;
                }

                if (pendingDelta.isDelete()) {
                    shadowInception = false;
                    shadowManager.addDeadShadowDeltas(repoShadow, refreshAsyncResult, (List)shadowDelta.getModifications());
                }

                continue;

            } else {
                PropertyDelta<OperationResultStatusType> resultStatusDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_RESULT_STATUS));
                resultStatusDelta.setRealValuesToReplace(newStatusType);
                shadowDelta.addModification(resultStatusDelta);
            }

            if (operationCompleted) {
                // Operation completed

                PropertyDelta<PendingOperationExecutionStatusType> executionStatusDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_EXECUTION_STATUS));
                executionStatusDelta.setRealValuesToReplace(PendingOperationExecutionStatusType.COMPLETED);
                shadowDelta.addModification(executionStatusDelta);

                PropertyDelta<XMLGregorianCalendar> completionTimestampDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_COMPLETION_TIMESTAMP));
                completionTimestampDelta.setRealValuesToReplace(clock.currentTimeXMLGregorianCalendar());
                shadowDelta.addModification(completionTimestampDelta);

                ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
                ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);

                if (pendingDelta.isAdd()) {
                    shadowInception = true;
                }

                if (pendingDelta.isModify()) {

                    // Apply shadow naming attribute modification
                    PrismContainer<ShadowAttributesType> shadowAttributesContainer = repoShadow.findContainer(ItemPath.create(ShadowType.F_ATTRIBUTES));
                    ResourceAttributeContainer resourceAttributeContainer = ResourceAttributeContainer.convertFromContainer(shadowAttributesContainer, ctx.getObjectClassDefinition());
                    ResourceAttributeContainerDefinition resourceAttrDefinition = resourceAttributeContainer.getDefinition();
                    if(resourceAttrDefinition != null) {

                        // If naming attribute is present in delta...
                        ResourceAttributeDefinition namingAttribute = resourceAttrDefinition.getNamingAttribute();
                        if (namingAttribute != null) {
                            if (pendingDelta.hasItemDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, namingAttribute.getItemName()))) {

                                // Retrieve a possible changed name per the defined naming attribute for the resource
                                ItemDelta namingAttributeDelta = pendingDelta.findItemDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, namingAttribute.getItemName()));
                                Collection<?> valuesToReplace = namingAttributeDelta.getValuesToReplace();
                                Optional<?> valueToReplace = valuesToReplace.stream().findFirst();

                                if (valueToReplace.isPresent()){
                                    Object valueToReplaceObj = ((PrismPropertyValue)valueToReplace.get()).getValue();
                                    if (valueToReplaceObj instanceof String) {

                                        // Apply the new naming attribute value to the shadow name by adding the change to the modification set for shadow delta
                                        PropertyDelta<PolyString> nameDelta = shadowDelta.createPropertyModification(ItemPath.create(ShadowType.F_NAME));
                                        Collection<PolyString> modificationSet = new ArrayList<>();
                                        PolyString nameAttributeReplacement = new PolyString((String) valueToReplaceObj);
                                        modificationSet.add(nameAttributeReplacement);
                                        nameDelta.setValuesToReplace(PrismValueCollectionsUtil.createCollection(prismContext, modificationSet));
                                        shadowDelta.addModification(nameDelta);
                                    }
                                }
                            }
                        }
                    }

                    // Apply shadow attribute modifications
                    for (ItemDelta<?, ?> pendingModification: pendingDelta.getModifications()) {
                        shadowDelta.addModification(pendingModification.clone());
                    }
                }

                if (pendingDelta.isDelete()) {
                    shadowInception = false;
                    shadowManager.addDeadShadowDeltas(repoShadow, refreshAsyncResult, (List)shadowDelta.getModifications());
                }

                notificationDeltas.add(pendingDelta);
            }

        }

        if (shadowInception) {
            // We do not need to care about attributes in add deltas here. The add operation is already applied to
            // attributes. We need this to "allocate" the identifiers, so iteration mechanism in the
            // model can find unique values while taking pending create operations into consideration.
            PropertyDelta<Boolean> existsDelta = shadowDelta.createPropertyModification(ShadowType.F_EXISTS);
            existsDelta.setRealValuesToReplace(true);
            shadowDelta.addModification(existsDelta);
        }

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        expirePendingOperations(ctx, repoShadow, shadowDelta, now, parentResult);

        if (!shadowDelta.isEmpty()) {
            shadowCaretaker.applyAttributesDefinition(ctx, shadowDelta);
            shadowManager.modifyShadowAttributes(ctx, repoShadow, shadowDelta.getModifications(), parentResult);
        }

        for (ObjectDelta<ShadowType> notificationDelta: notificationDeltas) {
            ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, repoShadow,
                    notificationDelta, parentResult);
            operationListener.notifySuccess(operationDescription, task, parentResult);
        }

        if (shadowDelta.isEmpty()) {
            return repoShadow;
        }
        shadowDelta.applyTo(repoShadow);
        return repoShadow;
    }

    private boolean needsRefresh(PendingOperationType pendingOperation) {
        PendingOperationExecutionStatusType executionStatus = pendingOperation.getExecutionStatus();
        if (executionStatus == null) {
            // LEGACY: 3.7 and earlier
            return OperationResultStatusType.IN_PROGRESS.equals(pendingOperation.getResultStatus());
        } else {
            return PendingOperationExecutionStatusType.EXECUTING.equals(executionStatus);
        }
    }


    private RefreshShadowOperation refreshShadowRetryOperations(ProvisioningContext ctx,
                                                                PrismObject<ShadowType> repoShadow, List<PendingOperationType> sortedOperations,
                                                                ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {
        ShadowType shadowType = repoShadow.asObjectable();
        OperationResult retryResult = new OperationResult(OP_REFRESH_RETRY);
        if (ShadowUtil.isDead(shadowType)) {
            RefreshShadowOperation rso = new RefreshShadowOperation();
            retryResult.recordSuccess();
            rso.setRefreshedShadow(repoShadow);
            rso.setRefreshResult(retryResult);
            return rso;
        }

        Duration retryPeriod = ProvisioningUtil.getRetryPeriod(ctx);

        List<ObjectDelta<ShadowType>> notificationDeltas = new ArrayList<>();

        Collection<ObjectDeltaOperation<ShadowType>> executedDeltas = new ArrayList<>();
        for (PendingOperationType pendingOperation: sortedOperations) {

            if (!needsRetry(pendingOperation)) {
                continue;
            }
            // We really want to get "now" here. Retrying operation may take some time. We want good timestamps that do not lie.
            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
            if (!isAfterRetryPeriod(ctx, pendingOperation, retryPeriod, now)) {
                if (PendingOperationTypeType.RETRY != pendingOperation.getType()) {
                    continue;
                }
                if (!ProvisioningOperationOptions.isForceRetry(options)) {
                    continue;
                }
            }

            LOGGER.trace("Going to retry operation {} on {}", pendingOperation, repoShadow);

            // Record attempt number and timestamp before the operation
            // TODO: later use this as an optimistic lock to make sure that two threads won't retry the operation at the same time

            // TODO: move to a better place
            ObjectDelta<ShadowType> shadowDelta = repoShadow.createModifyDelta();
            ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();

            int attemptNumber = pendingOperation.getAttemptNumber() + 1;
            PropertyDelta<Integer> attemptNumberDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_ATTEMPT_NUMBER));
            attemptNumberDelta.setRealValuesToReplace(attemptNumber);
            shadowDelta.addModification(attemptNumberDelta);

            PropertyDelta<XMLGregorianCalendar> lastAttemptTimestampDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_LAST_ATTEMPT_TIMESTAMP));
            lastAttemptTimestampDelta.setRealValuesToReplace(now);
            shadowDelta.addModification(lastAttemptTimestampDelta);

            PropertyDelta<OperationResultStatusType> resultStatusDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_RESULT_STATUS));
            resultStatusDelta.setRealValuesToReplace(OperationResultStatusType.IN_PROGRESS);
            shadowDelta.addModification(resultStatusDelta);

            shadowManager.modifyShadowAttributes(ctx, repoShadow, shadowDelta.getModifications(), parentResult);
            shadowDelta.applyTo(repoShadow);

            ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
            ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);

            ProvisioningOperationState<? extends AsynchronousOperationResult> opState =
                    ProvisioningOperationState.fromPendingOperation(repoShadow, pendingOperation);

            LOGGER.debug("Retrying operation {} on {}, attempt #{}", pendingDelta, repoShadow, attemptNumber);

            OperationResult result = parentResult.createSubresult(OP_OPERATION_RETRY);
            ObjectDeltaOperation<ShadowType> objectDeltaOperation = new ObjectDeltaOperation<>(pendingDelta);
            try {
                retryOperation(ctx, pendingDelta, opState, task, result);
                repoShadow = opState.getRepoShadow();
                result.computeStatus();
                if (result.isError()) {
                    retryResult.setStatus(result.getStatus());
                }
                //TODO maybe add whole "result" as subresult to the retryResult?
                result.muteError();
            } catch (CommunicationException | GenericFrameworkException | ObjectAlreadyExistsException | SchemaException | ObjectNotFoundException | ConfigurationException | SecurityViolationException e) {
                // This is final failure: the error is not handled.
                // Therefore the operation is now completed - finished with an error.
                // But we do not want to stop the task. Just log the error.
                LOGGER.error("Operation {} on {} ended up with an error after {} retries: {}",
                        pendingDelta, repoShadow, attemptNumber, e.getMessage(), e);
                // The retry itself was a success. Operation that was retried might have failed.
                // And that is recorded in the shadow. But we have successfully retried the operation.
                result.recordHandledError(e);
                retryResult.recordFatalError("Operation " + pendingDelta + " on " + repoShadow + " ended with an error after " + attemptNumber + " retries: " + e.getMessage());
            } catch (Throwable e) {
                // This is unexpected error during retry. This means that there was other
                // failure that we did not expected. This is likely to be bug - or maybe wrong
                // error handling. This means that the retry was a failure.
                result.recordFatalError(e);
                retryResult.recordFatalError(e);
            }

            objectDeltaOperation.setExecutionResult(result);
            executedDeltas.add(objectDeltaOperation);
        }

        RefreshShadowOperation rso = new RefreshShadowOperation();
        rso.setExecutedDeltas(executedDeltas);
        rso.setRefreshedShadow(repoShadow);
        parentResult.computeStatus();

        rso.setRefreshResult(retryResult);

        LOGGER.trace("refreshShadowOperation {}", rso.debugDump());
        return rso;
    }

    private void retryOperation(ProvisioningContext ctx,
            ObjectDelta<ShadowType> pendingDelta,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            Task task,
            OperationResult result)
                    throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, EncryptionException {

        ProvisioningOperationOptions options = ProvisioningOperationOptions.createForceRetry(false);
        OperationProvisioningScriptsType scripts = null; // TODO
        if (pendingDelta.isAdd()) {
            PrismObject<ShadowType> shadowToAdd = pendingDelta.getObjectToAdd();
            addShadowAttempt(ctx, shadowToAdd, scripts,
                    (ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>>) opState,
                    options, task, result);
            opState.setRepoShadow(shadowToAdd);
        }

        if (pendingDelta.isModify()) {
            modifyShadowAttempt(ctx, pendingDelta.getModifications(), scripts, options,
                    (ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>>) opState,
                    true, task, result);
        }

        if (pendingDelta.isDelete()) {
            deleteShadowAttempt(ctx, options, scripts,
                    (ProvisioningOperationState<AsynchronousOperationResult>) opState,
                    task, result);
        }
    }

    private boolean needsRetry(PendingOperationType pendingOperation) {
        return PendingOperationExecutionStatusType.EXECUTING.equals(pendingOperation.getExecutionStatus()) &&
                pendingOperation.getAttemptNumber() != null;
    }

    private boolean isAfterRetryPeriod(ProvisioningContext ctx, PendingOperationType pendingOperation, Duration retryPeriod, XMLGregorianCalendar now) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        XMLGregorianCalendar lastAttemptTimestamp = pendingOperation.getLastAttemptTimestamp();
        XMLGregorianCalendar scheduledRetryTimestamp = XmlTypeConverter.addDuration(lastAttemptTimestamp, retryPeriod);
        return XmlTypeConverter.compare(now, scheduledRetryTimestamp) == DatatypeConstants.GREATER;
    }

    // This is very simple code that essentially works only for postponed operations (retries).
    // TODO: better support for async and manual operations
    private PrismObject<ShadowType> cancelAllPendingOperations(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
        List<PendingOperationType> pendingOperations = repoShadow.asObjectable().getPendingOperation();
        if (pendingOperations.isEmpty()) {
            return repoShadow;
        }
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        ObjectDelta<ShadowType> shadowDelta = repoShadow.createModifyDelta();
        for (PendingOperationType pendingOperation: pendingOperations) {
            if (pendingOperation.getExecutionStatus() == PendingOperationExecutionStatusType.COMPLETED) {
                continue;
            }
            if (pendingOperation.getType() != PendingOperationTypeType.RETRY) {
                // Other operations are not cancellable now
                continue;
            }
            ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();
            PropertyDelta<PendingOperationExecutionStatusType> executionStatusDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_EXECUTION_STATUS));
            executionStatusDelta.setRealValuesToReplace(PendingOperationExecutionStatusType.COMPLETED);
            shadowDelta.addModification(executionStatusDelta);
            PropertyDelta<XMLGregorianCalendar> completionTimestampDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_COMPLETION_TIMESTAMP));
            completionTimestampDelta.setRealValuesToReplace(now);
            shadowDelta.addModification(completionTimestampDelta);
            PropertyDelta<OperationResultStatusType> resultStatusDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_RESULT_STATUS));
            resultStatusDelta.setRealValuesToReplace(OperationResultStatusType.NOT_APPLICABLE);
            shadowDelta.addModification(resultStatusDelta);
        }
        if (shadowDelta.isEmpty()) {
            return repoShadow;
        }
        LOGGER.debug("Cancelling pending operations on {}", repoShadow);
        shadowManager.modifyShadowAttributes(ctx, repoShadow, shadowDelta.getModifications(), parentResult);
        shadowDelta.applyTo(repoShadow);
        return repoShadow;
    }

    private PrismObject<ShadowType> cleanUpDeadShadow(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow,
            XMLGregorianCalendar now, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {
        ShadowType shadowType = repoShadow.asObjectable();
        if (!ShadowUtil.isDead(shadowType)) {
            return repoShadow;
        }
        Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);
        Duration deadRetentionPeriod = ProvisioningUtil.getDeadShadowRetentionPeriod(ctx);
        Duration expirationPeriod = XmlTypeConverter.longerDuration(gracePeriod, deadRetentionPeriod);
        XMLGregorianCalendar lastActivityTimestamp = null;

        for (PendingOperationType pendingOperation: shadowType.getPendingOperation()) {
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getRequestTimestamp());
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getLastAttemptTimestamp());
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getCompletionTimestamp());
        }
        if (lastActivityTimestamp == null) {
            MetadataType metadata = shadowType.getMetadata();
            if (metadata != null) {
                lastActivityTimestamp = metadata.getModifyTimestamp();
                if (lastActivityTimestamp == null) {
                    lastActivityTimestamp = metadata.getCreateTimestamp();
                }
            }
        }

        if (ProvisioningUtil.isOverPeriod(now, expirationPeriod, lastActivityTimestamp)) {
            // Perish you stinking corpse!
            LOGGER.debug("Deleting dead {} because it is expired", repoShadow);
            shadowManager.deleteShadow(ctx, repoShadow, parentResult);
            ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
            change.setCleanDeadShadow(true);
            change.setOldShadow(repoShadow);
            change.setResource(ctx.getResource().asPrismObject());
            change.setObjectDelta(repoShadow.createDeleteDelta());
            change.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_DISCOVERY_URI);
            changeNotificationDispatcher.notifyChange(change, task, parentResult);
            applyDefinition(repoShadow, parentResult);
            ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, repoShadow,
                    repoShadow.createDeleteDelta(), parentResult);
            operationListener.notifySuccess(operationDescription, task, parentResult);
            return null;
        } else {
            LOGGER.trace("Keeping dead {} because it is not expired yet, last activity={}, expiration period={}", repoShadow, lastActivityTimestamp, expirationPeriod);
            return repoShadow;
        }
    }

    private void expirePendingOperations(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow, ObjectDelta<ShadowType> shadowDelta, XMLGregorianCalendar now, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ShadowType shadowType = repoShadow.asObjectable();

        Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);
        Duration pendingOperationRetentionPeriod = ProvisioningUtil.getPendingOperationRetentionPeriod(ctx);
        Duration expirePeriod = XmlTypeConverter.longerDuration(gracePeriod, pendingOperationRetentionPeriod);
        for (PendingOperationType pendingOperation: shadowType.getPendingOperation()) {
            if (ProvisioningUtil.isOverPeriod(now, expirePeriod, pendingOperation)) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Deleting pending operation because it is completed '{}' and expired: {}", pendingOperation.getResultStatus().value(), pendingOperation);
                }
                shadowDelta.addModificationDeleteContainer(ShadowType.F_PENDING_OPERATION, pendingOperation.clone());
            }
        }
    }

    public void applyDefinition(ObjectDelta<ShadowType> delta, ShadowType repoShadow,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
                    CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ShadowType> shadow = null;
        ResourceShadowDiscriminator discriminator = null;
        if (delta.isAdd()) {
            shadow = delta.getObjectToAdd();
        } else if (delta.isModify()) {
            if (delta instanceof ShadowDiscriminatorObjectDelta) {
                // This one does not have OID, it has to be specially processed
                discriminator = ((ShadowDiscriminatorObjectDelta) delta).getDiscriminator();
            } else {
                String shadowOid = delta.getOid();
                if (shadowOid == null) {
                    if (repoShadow == null) {
                        throw new IllegalArgumentException("No OID in object delta " + delta
                                + " and no externally-supplied shadow is present as well.");
                    }
                    shadow = repoShadow.asPrismObject();
                } else {
                    shadow = repositoryService.getObject(delta.getObjectTypeClass(), shadowOid, null,
                            parentResult); // TODO consider fetching only when
                                            // really necessary
                }
            }
        } else {
            // Delete delta, nothing to do at all
            return;
        }
        ProvisioningContext ctx;
        if (shadow == null) {
            ctx = ctxFactory.create(discriminator, null, parentResult);
            ctx.assertDefinition();
        } else {
            ctx = ctxFactory.create(shadow, null, parentResult);
            ctx.assertDefinition();
        }
        shadowCaretaker.applyAttributesDefinition(ctx, delta);
    }

    public void applyDefinition(PrismObject<ShadowType> shadow, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ProvisioningContext ctx = ctxFactory.create(shadow, null, parentResult);
        ctx.assertDefinition();
        shadowCaretaker.applyAttributesDefinition(ctx, shadow);
    }

    public void setProtectedShadow(PrismObject<ShadowType> shadow, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ProvisioningContext ctx = ctxFactory.create(shadow, null, parentResult);
        ctx.assertDefinition();
        ProvisioningUtil.setProtectedFlag(ctx, shadow, matchingRuleRegistry, relationRegistry);
    }

    public void applyDefinition(final ObjectQuery query, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ResourceShadowDiscriminator coordinates = ObjectQueryUtil.getCoordinates(query.getFilter(), prismContext);
        ProvisioningContext ctx = ctxFactory.create(coordinates, null, result);
        ctx.assertDefinition();
        applyDefinition(ctx, query);
    }

    private void applyDefinition(final ProvisioningContext ctx, final ObjectQuery query)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (query == null) {
            return;
        }
        ObjectFilter filter = query.getFilter();
        if (filter == null) {
            return;
        }
        final RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
        com.evolveum.midpoint.prism.query.Visitor visitor = subfilter -> {
            if (subfilter instanceof PropertyValueFilter) {
                PropertyValueFilter<?> valueFilter = (PropertyValueFilter<?>) subfilter;
                ItemDefinition definition = valueFilter.getDefinition();
                if (definition instanceof ResourceAttributeDefinition) {
                    return;        // already has a resource-related definition
                }
                if (!ShadowType.F_ATTRIBUTES.equivalent(valueFilter.getParentPath())) {
                    return;
                }
                QName attributeName = valueFilter.getElementName();
                ResourceAttributeDefinition attributeDefinition = objectClassDefinition.findAttributeDefinition(attributeName);
                if (attributeDefinition == null) {
                    throw new TunnelException(new SchemaException("No definition for attribute "
                            + attributeName + " in query " + query));
                }
                valueFilter.setDefinition(attributeDefinition);
            }
        };
        try {
            filter.accept(visitor);
        } catch (TunnelException te) {
            throw (SchemaException) te.getCause();
        }
    }

    protected ResourceType getResource(ResourceShadowDiscriminator coords, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        String resourceOid = coords.getResourceOid();
        if (resourceOid == null) {
            throw new IllegalArgumentException("No resource OID in " + coords);
        }
        return resourceManager.getResource(resourceOid, null, task, parentResult).asObjectable();
    }

    //we need to remove resolved identifiers form the ShadowAssociationType before we save it to the shadow as an unfinished operation.
    void normalizeAssociationDeltasBeforeSave(Collection<PrismContainerValue<ShadowAssociationType>> associationContainers) {
        if (associationContainers == null) {
            return;
        }
        for (PrismContainerValue<ShadowAssociationType> associationContainer : associationContainers) {
            if (associationContainer.contains(ShadowAssociationType.F_IDENTIFIERS) && associationContainer.contains(ShadowAssociationType.F_SHADOW_REF)) {
                associationContainer.removeContainer(ShadowAssociationType.F_IDENTIFIERS);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // SEARCH
    ////////////////////////////////////////////////////////////////////////////

//    public SearchResultList<PrismObject<ShadowType>> searchObjects(ObjectQuery query,
//            Collection<SelectorOptions<GetOperationOptions>> options,
//            final boolean readFromRepository, Task task, final OperationResult parentResult)
//                    throws SchemaException, ObjectNotFoundException, CommunicationException,
//                    ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
//
//        SearchResultList<PrismObject<ShadowType>> list = new SearchResultList<>();
//        SearchResultMetadata metadata = searchObjectsIterative(query, options, (shadow,result) -> list.add(shadow), readFromRepository, task, parentResult);
//        list.setMetadata(metadata);
//        return list;
//
//    }

    public SearchResultMetadata searchObjectsIterative(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, final ResultHandler<ShadowType> handler,
            final boolean readFromRepository, Task task, final OperationResult parentResult)
                    throws SchemaException, ObjectNotFoundException, CommunicationException,
                    ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        ProvisioningContext ctx = createContextForSearch(query, options, task, parentResult);

        return searchObjectsIterative(ctx, query, options, handler, readFromRepository, parentResult);
    }

    private ProvisioningContext createContextForSearch(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ResourceShadowDiscriminator coordinates = ObjectQueryUtil.getCoordinates(query != null ? query.getFilter() : null,
                prismContext);
        final ProvisioningContext ctx = ctxFactory.create(coordinates, task, parentResult);
        ctx.setGetOperationOptions(options);
        ctx.assertDefinition();
        return ctx;
    }

    @NotNull
    public SearchResultList<PrismObject<ShadowType>> searchObjects(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, final OperationResult parentResult)
                    throws SchemaException, ObjectNotFoundException, CommunicationException,
                    ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        ProvisioningContext ctx = createContextForSearch(query, options, task, parentResult);
        return searchObjects(ctx, query, options, parentResult);
    }

    public SearchResultMetadata searchObjectsIterative(final ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, final ResultHandler<ShadowType> handler,
            final boolean readFromRepository, final OperationResult parentResult)
                    throws SchemaException, ObjectNotFoundException, CommunicationException,
                    ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        applyDefinition(ctx, query);

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        if (ProvisioningUtil.shouldDoRepoSearch(rootOptions)) {
            return searchObjectsIterativeRepository(ctx, query, options, handler, parentResult);
        }
        boolean isDoDiscovery = ProvisioningUtil.isDoDiscovery(ctx.getResource(), rootOptions);

        // We need to record the fetch down here. Now it is certain that we are
        // going to fetch from resource
        // (we do not have raw/noFetch option)
        InternalMonitor.recordCount(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        ObjectQuery attributeQuery = createAttributeQuery(query);

        ResultHandler<ShadowType> resultHandler = (PrismObject<ShadowType> resourceShadow, OperationResult objResult) -> {
            LOGGER.trace("Found resource object\n{}", resourceShadow.debugDumpLazily(1));
            PrismObject<ShadowType> resultShadow;
                try {
                    // The shadow does not have any kind or intent at this point.
                    // But at least locate the definition using object classes.
                    ProvisioningContext estimatedShadowCtx = shadowCaretaker.reapplyDefinitions(ctx, resourceShadow);
                    // Try to find shadow that corresponds to the resource object.
                    if (readFromRepository) {
                        PrismObject<ShadowType> repoShadow = acquireRepositoryShadow(
                                estimatedShadowCtx, resourceShadow, true, isDoDiscovery, parentResult);

                        // This determines the definitions exactly. How the repo
                        // shadow should have proper kind/intent
                        ProvisioningContext shadowCtx = shadowCaretaker.applyAttributesDefinition(ctx, repoShadow);
                        // TODO: shadowState
                        repoShadow = shadowManager.updateShadow(shadowCtx, resourceShadow, null, repoShadow,
                                null, parentResult);

                        resultShadow = completeShadow(shadowCtx, resourceShadow, repoShadow, isDoDiscovery, objResult);

                        // TODO do we want also to futurize the shadow like in getObject?

                        //check and fix kind/intent
                        ShadowType repoShadowType = repoShadow.asObjectable();
                        if (isDoDiscovery && (repoShadowType.getKind() == null || repoShadowType.getIntent() == null)) { //TODO: check also empty?
                            // notify resourceObjectChangeListeners to fix kind and intent for Shadow
                            // Do NOT invoke this if discovery is disabled. This may ruin the flow (e.g. when importing objects)
                            // or it may lead to discovery loops.
                            notifyResourceObjectChangeListeners(repoShadow, ctx.getResource().asPrismObject(), false);
                        }

                    } else {
                        resultShadow = resourceShadow;
                    }

                    validateShadow(resultShadow, readFromRepository);

                } catch (SchemaException e) {
                    objResult.recordFatalError("Schema error: " + e.getMessage(), e);
                    LOGGER.error("Schema error: {}", e.getMessage(), e);
                    return false;
                } catch (ConfigurationException e) {
                    objResult.recordFatalError("Configuration error: " + e.getMessage(), e);
                    LOGGER.error("Configuration error: {}", e.getMessage(), e);
                    return false;
                } catch (ObjectNotFoundException | CommunicationException
                        | SecurityViolationException | GenericConnectorException | ExpressionEvaluationException | EncryptionException e) {
                    objResult.recordFatalError(e.getMessage(), e);
                    LOGGER.error("{}", e.getMessage(), e);
                    return false;
                }

                boolean doContinue;
                try {

                    doContinue =  handler.handle(resultShadow, objResult);

                    objResult.computeStatus();
                    objResult.recordSuccessIfUnknown();

                } catch (RuntimeException | Error e) {
                    objResult.recordFatalError(e);
                    throw e;
                } finally {
                    objResult.computeStatus();
                    objResult.recordSuccessIfUnknown();
                    // FIXME: hack. Hardcoded ugly summarization of successes. something like
                    //  AbstractSummarizingResultHandler [lazyman]
                    if (objResult.isSuccess()) {
                        objResult.getSubresults().clear();
                    }
                    parentResult.summarize();
                }

                return doContinue;
            };

        boolean fetchAssociations = SelectorOptions.hasToLoadPath(ShadowType.F_ASSOCIATION, options);

        return resourceObjectConverter.searchResourceObjects(ctx, resultHandler, attributeQuery,
                fetchAssociations, parentResult);

    }

    @NotNull
    public SearchResultList<PrismObject<ShadowType>> searchObjects(final ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, final OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        applyDefinition(ctx, query);

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        if (ProvisioningUtil.shouldDoRepoSearch(rootOptions)) {
            return searchObjectsRepository(ctx, query, options, parentResult);
        } else {
            SearchResultList<PrismObject<ShadowType>> rv = new SearchResultList<>();
            SearchResultMetadata metadata = searchObjectsIterative(ctx, query, options, (s, opResult) -> rv.add(s), true,
                    parentResult);
            rv.setMetadata(metadata);
            return rv;
        }
    }

    /**
     * Acquires repository shadow for a provided resource shadow. The repository shadow is locate or created.
     * In case that the shadow is created, all additional ceremonies for a new shadow is done, e.g. invoking
     * change notifications (discovery).
     *
     * Returned shadow is NOT guaranteed to have all the attributes aligned and updated. That is only possible after
     * completeShadow(). But maybe, this method can later invoke completeShadow() and do all the necessary stuff?
     *
     * It may look like this method would rather belong to ShadowManager. But it does NOT. It does too much stuff
     * (e.g. change notification).
     */
    private PrismObject<ShadowType> acquireRepositoryShadow(ProvisioningContext ctx,
            PrismObject<ShadowType> resourceShadow, boolean unknownIntent, boolean isDoDiscovery, OperationResult parentResult)
                    throws SchemaException, ConfigurationException, ObjectNotFoundException,
                    CommunicationException, SecurityViolationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        PrismObject<ShadowType> repoShadow = shadowManager.lookupLiveShadowInRepository(ctx, resourceShadow, parentResult);

        if (repoShadow != null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Found shadow object in the repository {}", ShadowUtil.shortDumpShadow(repoShadow));
            }
            return repoShadow;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Shadow object (in repo) corresponding to the resource object (on the resource) was not found. "
                    + "The repo shadow will be created. The resource object:\n{}", resourceShadow);
        }

        // TODO: do something about shadows with mathcing secondary identifiers? We do not need to care about these any longer, do we?
        //MID-5844
        // we need it for the consistency. following is the use case. Account is beeing created on the resource
        // but the resource cannot be reached by midPoint. Meanwhile the account is created manually in the resource
        // when reconciliation runs, origin account created while resource was not reachable doesn't contain primary identifier
        // so we rather try also secondary identifier to be sure nothing goes wrong
//        PrismObject<ShadowType> repoShadow;

//        PrismObject<ShadowType> shadowBySecondaryIdentifier = shadowManager.lookupConflictingShadowBySecondaryIdentifiers(ctx,
//                resourceShadow, parentResult);
//
//        // check if the shadow contains primary identifier. if it does, we can skip the next session
//        if (shadowBySecondaryIdentifier != null) {
//
//            ShadowType shadowType = shadowBySecondaryIdentifier.asObjectable();
//            String currentPrimaryIdentifier = shadowManager.determinePrimaryIdentifierValue(ctx, shadowBySecondaryIdentifier);
//
//            LOGGER.info("###CURRENT PI: {}", currentPrimaryIdentifier);
//            String expectedPrimaryIdentifier = shadowManager.determinePrimaryIdentifierValue(ctx, resourceShadow);
//            LOGGER.info("### EXPECTED PI: {}", expectedPrimaryIdentifier);
//
//            if (!StringUtils.equals(currentPrimaryIdentifier, expectedPrimaryIdentifier)) {
//                shadowCaretaker.applyAttributesDefinition(ctx, shadowBySecondaryIdentifier);
//                shadowBySecondaryIdentifier = completeShadow(ctx, resourceShadow, shadowBySecondaryIdentifier, isDoDiscovery, parentResult);
//                Task task = taskManager.createTaskInstance();
//                ResourceOperationDescription failureDescription = ProvisioningUtil.createResourceFailureDescription(shadowBySecondaryIdentifier, ctx.getResource(), null, parentResult);
//                shadowBySecondaryIdentifier.asObjectable().setDead(Boolean.TRUE);
//                changeNotificationDispatcher.notifyFailure(failureDescription, task, parentResult);
//                shadowManager.deleteConflictedShadowFromRepo(shadowBySecondaryIdentifier, parentResult);
//            }
//
//        }
//

        // The resource object obviously exists on the resource, but appropriate shadow does
        // not exist in the repository we need to create the shadow to align repo state to the
        // reality (resource)

        try {

            repoShadow = shadowManager.addDiscoveredRepositoryShadow(ctx, resourceShadow, parentResult);

        } catch (ObjectAlreadyExistsException e) {
            // Conflict! But we haven't supplied an OID and we have checked for existing shadow before,
            // therefore there should not conflict. Unless someone managed to create the same shadow
            // between our check and our create attempt. In that case try to re-check for shadow existence
            // once more.

            OperationResult originalRepoAddSubresult = parentResult.getLastSubresult();

            LOGGER.debug("Attempt to create new repo shadow for {} ended up in conflict, re-trying the search for repo shadow", resourceShadow);
            PrismObject<ShadowType> conflictingShadow = shadowManager.lookupLiveShadowInRepository(ctx, resourceShadow, parentResult);

            if (conflictingShadow == null) {
                // This is really strange. The shadow should not have disappeared in the meantime, dead shadow would remain instead.
                // Maybe we have broken "indexes"? (e.g. primaryIdentifierValue column)

                // Do some "research" and log the results, so we have good data to diagnose this situation.
                String determinedPrimaryIdentifierValue = shadowManager.determinePrimaryIdentifierValue(ctx, resourceShadow);
                PrismObject<ShadowType> potentialConflictingShadow = shadowManager.lookupShadowByPrimaryIdentifierValue(ctx, determinedPrimaryIdentifierValue, parentResult);

                LOGGER.error("Unexpected repository behavior: object already exists error even after we double-checked shadow uniqueness: {}", e.getMessage(), e);
                LOGGER.debug("REPO CONFLICT: resource shadow\n{}", resourceShadow.debugDump(1));
                LOGGER.debug("REPO CONFLICT: resource shadow: determined primaryIdentifierValue: {}", determinedPrimaryIdentifierValue);
                LOGGER.debug("REPO CONFLICT: potential conflicting repo shadow (by primaryIdentifierValue)\n{}", potentialConflictingShadow==null?null:potentialConflictingShadow.debugDump(1));

                throw new SystemException(
                        "Unexpected repository behavior: object already exists error even after we double-checked shadow uniqueness: " + e.getMessage(), e);
            }

            originalRepoAddSubresult.muteError();
            return conflictingShadow;
        }

        resourceShadow.setOid(repoShadow.getOid());
        ObjectReferenceType resourceRef = resourceShadow.asObjectable().getResourceRef();
        if (resourceRef == null) {
            resourceRef = new ObjectReferenceType();
            resourceRef.asReferenceValue().setObject(ctx.getResource().asPrismObject());
            resourceShadow.asObjectable().setResourceRef(resourceRef);
        } else {
            resourceRef.asReferenceValue().setObject(ctx.getResource().asPrismObject());
        }

        if (isDoDiscovery) {
            // We have object for which there was no shadow. Which means that midPoint haven't known about this shadow before.
            // Invoke notifyChange() so the new shadow is properly initialized.

            notifyResourceObjectChangeListeners(resourceShadow, ctx.getResource().asPrismObject(), true);
        }

        if (unknownIntent) {
            // Intent may have been changed during the notifyChange processing.
            // Re-read the shadow if necessary.
            repoShadow = shadowManager.fixShadow(ctx, repoShadow, parentResult);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Final repo shadow (created):\n{}", repoShadow.debugDump(1));
        }

        return repoShadow;
    }

    private ObjectQuery createAttributeQuery(ObjectQuery query) throws SchemaException {
        QueryFactory queryFactory = prismContext.queryFactory();

        ObjectFilter filter = null;
        if (query != null) {
            filter = query.getFilter();
        }

        ObjectQuery attributeQuery = null;

        if (filter instanceof AndFilter) {
            List<? extends ObjectFilter> conditions = ((AndFilter) filter).getConditions();
            List<ObjectFilter> attributeFilter = createAttributeQueryInternal(conditions);
            if (attributeFilter.size() > 1) {
                attributeQuery = queryFactory.createQuery(queryFactory.createAnd(attributeFilter));
            } else if (attributeFilter.size() < 1) {
                LOGGER.trace("No attribute filter defined in the query.");
            } else {
                attributeQuery = queryFactory.createQuery(attributeFilter.iterator().next());
            }
        }

        if (query != null && query.getPaging() != null) {
            if (attributeQuery == null) {
                attributeQuery = queryFactory.createQuery();
            }
            attributeQuery.setPaging(query.getPaging());
        }
        if (query != null && query.isAllowPartialResults()) {
            if (attributeQuery == null) {
                attributeQuery = queryFactory.createQuery();
            }
            attributeQuery.setAllowPartialResults(true);
        }

        if (InternalsConfig.consistencyChecks && attributeQuery != null
                && attributeQuery.getFilter() != null) {
            attributeQuery.getFilter().checkConsistence(true);
        }
        return attributeQuery;
    }

    private List<ObjectFilter> createAttributeQueryInternal(List<? extends ObjectFilter> conditions)
            throws SchemaException {
        List<ObjectFilter> attributeFilter = new ArrayList<>();
        for (ObjectFilter f : conditions) {
            if (f instanceof PropertyValueFilter) { // TODO
                ItemPath parentPath = ((PropertyValueFilter) f).getParentPath();
                if (parentPath.isEmpty()) {
                    QName elementName = ((PropertyValueFilter) f).getElementName();
                    if (QNameUtil.match(ShadowType.F_OBJECT_CLASS, elementName) ||
                            QNameUtil.match(ShadowType.F_AUXILIARY_OBJECT_CLASS, elementName) ||
                            QNameUtil.match(ShadowType.F_KIND, elementName) ||
                            QNameUtil.match(ShadowType.F_INTENT, elementName)) {
                        continue;
                    }
                    throw new SchemaException("Cannot combine on-resource and off-resource properties in a shadow search query. Encountered property " +
                            ((PropertyValueFilter) f).getFullPath());
                }
                attributeFilter.add(f);
            } else if (f instanceof NaryLogicalFilter) {
                List<ObjectFilter> subFilters = createAttributeQueryInternal(
                        ((NaryLogicalFilter) f).getConditions());
                if (subFilters.size() > 1) {
                    if (f instanceof OrFilter) {
                        attributeFilter.add(prismContext.queryFactory().createOr(subFilters));
                    } else if (f instanceof AndFilter) {
                        attributeFilter.add(prismContext.queryFactory().createAnd(subFilters));
                    } else {
                        throw new IllegalArgumentException(
                                "Could not translate query filter. Unknown type: " + f);
                    }
                } else if (subFilters.size() < 1) {
                    continue;
                } else {
                    attributeFilter.add(subFilters.iterator().next());
                }
            } else if (f instanceof  UnaryLogicalFilter) {
                ObjectFilter subFilter = ((UnaryLogicalFilter) f).getFilter();
                attributeFilter.add(prismContext.queryFactory().createNot(subFilter));
            } else if (f instanceof SubstringFilter) {
                attributeFilter.add(f);
            } else if (f instanceof RefFilter) {
                ItemPath parentPath = ((RefFilter)f).getParentPath();
                if (parentPath.isEmpty()) {
                    QName elementName = ((RefFilter) f).getElementName();
                    if (QNameUtil.match(ShadowType.F_RESOURCE_REF, elementName)) {
                        continue;
                    }
                }
                throw new SchemaException("Cannot combine on-resource and off-resource properties in a shadow search query. Encountered filter " + f);
            } else {
                throw new SchemaException("Cannot combine on-resource and off-resource properties in a shadow search query. Encountered filter " + f);
            }

        }

        return attributeFilter;
    }

    private SearchResultMetadata searchObjectsIterativeRepository(final ProvisioningContext ctx,
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            final ResultHandler<ShadowType> shadowHandler, OperationResult parentResult)
                    throws SchemaException, ConfigurationException, ObjectNotFoundException,
                    CommunicationException, ExpressionEvaluationException {
        ResultHandler<ShadowType> repoHandler = createRepoShadowHandler(ctx, options, shadowHandler);
        return shadowManager.searchObjectsIterativeRepository(ctx, query, options, repoHandler, parentResult);
    }

    @NotNull
    private ResultHandler<ShadowType> createRepoShadowHandler(ProvisioningContext ctx,
            Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<ShadowType> shadowHandler) {
        return (PrismObject<ShadowType> shadow, OperationResult objResult) -> {
                try {
                    processRepoShadow(ctx, shadow, options, objResult);

                    boolean cont = shadowHandler == null || shadowHandler.handle(shadow, objResult);

                    objResult.computeStatus();
                    objResult.recordSuccessIfUnknown();
                    if (!objResult.isSuccess()) {
                        OperationResultType resultType = objResult.createOperationResultType();
                        shadow.asObjectable().setFetchResult(resultType);
                    }

                    return cont;
                } catch (RuntimeException e) {
                    objResult.recordFatalError(e);
                    throw e;
                } catch (SchemaException | ConfigurationException | ObjectNotFoundException
                        | CommunicationException | ExpressionEvaluationException e) {
                    objResult.recordFatalError(e);
                    shadow.asObjectable().setFetchResult(objResult.createOperationResultType());
                    throw new SystemException(e);
                }
            };
    }

    @NotNull
    private SearchResultList<PrismObject<ShadowType>> searchObjectsRepository(ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException {
        SearchResultList<PrismObject<ShadowType>> objects = shadowManager.searchObjectsRepository(ctx, query, options, parentResult);
        ResultHandler<ShadowType> repoHandler = createRepoShadowHandler(ctx, options, null);
        parentResult.setSummarizeSuccesses(true);
        for (PrismObject<ShadowType> object : objects) {
            repoHandler.handle(object, parentResult.createMinorSubresult(ShadowCache.class.getName() + ".handleObject"));
        }
        parentResult.summarize();       // todo is this ok?
        return objects;
    }

    private void processRepoShadow(ProvisioningContext ctx, PrismObject<ShadowType> shadow,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult objResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException {
        shadowCaretaker.applyAttributesDefinition(ctx, shadow);
        // fixing MID-1640; hoping that the protected object filter uses only identifiers
        // (that are stored in repo)
        ProvisioningUtil.setProtectedFlag(ctx, shadow, matchingRuleRegistry, relationRegistry);

        validateShadow(shadow, true);

        if (GetOperationOptions.isMaxStaleness(SelectorOptions.findRootOptions(options))) {
            CachingMetadataType cachingMetadata = shadow.asObjectable().getCachingMetadata();
            if (cachingMetadata == null) {
                objResult.recordFatalError("Requested cached data but no cached data are available in the shadow");
            }
        }
    }

    private void validateShadow(PrismObject<ShadowType> shadow, boolean requireOid) {
        if (requireOid) {
            Validate.notNull(shadow.getOid(), "null shadow OID");
        }
        if (InternalsConfig.encryptionChecks) {
            CryptoUtil.checkEncrypted(shadow);
        }
    }

    private void notifyResourceObjectChangeListeners(PrismObject<ShadowType> resourceShadow, PrismObject<ResourceType> resource, boolean newShadow) {
        ResourceObjectShadowChangeDescription shadowChangeDescription = new ResourceObjectShadowChangeDescription();
        shadowChangeDescription.setResource(resource);
        shadowChangeDescription.setOldShadow(newShadow ? null : resourceShadow);
        shadowChangeDescription.setCurrentShadow(resourceShadow);
        shadowChangeDescription.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_DISCOVERY_URI);
        shadowChangeDescription.setUnrelatedChange(true);

        Task task = taskManager.createTaskInstance();
        notifyResourceObjectChangeListeners(shadowChangeDescription, task, task.getResult());
    }

    public void notifyResourceObjectChangeListeners(ResourceObjectShadowChangeDescription change, Task task,
            OperationResult parentResult) {
        changeNotificationDispatcher.notifyChange(change, task, parentResult);
    }

    public Integer countObjects(ObjectQuery query, Task task, final OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        ResourceShadowDiscriminator coordinates = ObjectQueryUtil.getCoordinates(query.getFilter(), prismContext);
        final ProvisioningContext ctx = ctxFactory.create(coordinates, null, result);
        ctx.assertDefinition();
        applyDefinition(ctx, query);

        RefinedObjectClassDefinition objectClassDef = ctx.getObjectClassDefinition();
        ResourceType resourceType = ctx.getResource();
        CountObjectsCapabilityType countObjectsCapabilityType = objectClassDef
                .getEffectiveCapability(CountObjectsCapabilityType.class, resourceType);
        if (countObjectsCapabilityType == null) {
            // Unable to count. Return null which means "I do not know"
            LOGGER.trace("countObjects: cannot count (no counting capability)");
            result.recordNotApplicableIfUnknown();
            return null;
        } else {
            CountObjectsSimulateType simulate = countObjectsCapabilityType.getSimulate();
            if (simulate == null) {
                // We have native capability

                LOGGER.trace("countObjects: counting with native count capability");
                ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, result);
                try {
                    ObjectQuery attributeQuery = createAttributeQuery(query);
                    int count;
                    try {
                        count = connector.count(objectClassDef.getObjectClassDefinition(), attributeQuery,
                                objectClassDef.getPagedSearches(resourceType), ctx, result);
                    } catch (CommunicationException | GenericFrameworkException | SchemaException
                            | UnsupportedOperationException e) {
                        result.recordFatalError(e);
                        throw e;
                    }
                    result.computeStatus();
                    result.cleanupResult();
                    return count;
                } catch (GenericFrameworkException | UnsupportedOperationException e) {
                    SystemException ex = new SystemException(
                            "Couldn't count objects on resource " + resourceType + ": " + e.getMessage(), e);
                    result.recordFatalError(ex);
                    throw ex;
                }

            } else if (simulate == CountObjectsSimulateType.PAGED_SEARCH_ESTIMATE) {

                LOGGER.trace("countObjects: simulating counting with paged search estimate");
                if (!objectClassDef.isPagedSearchEnabled(resourceType)) {
                    throw new ConfigurationException(
                            "Configured count object capability to be simulated using a paged search but paged search capability is not present");
                }

                final Holder<Integer> countHolder = new Holder<>(0);

                final ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
                    @Override
                    public boolean handle(PrismObject<ShadowType> shadow, OperationResult objResult) {
                        int count = countHolder.getValue();
                        count++;
                        countHolder.setValue(count);
                        return true;
                    }

                    @Override
                    public String toString() {
                        return "(ShadowCache simulated counting handler)";
                    }
                };

                query = query.clone();
                ObjectPaging paging = prismContext.queryFactory().createPaging();
                // Explicitly set offset. This makes a difference for some resources.
                // E.g. LDAP connector will detect presence of an offset and it will initiate VLV search which
                // can estimate number of results. If no offset is specified then continuous/linear search is
                // assumed (e.g. Simple Paged Results search). Such search does not have ability to estimate
                // number of results.
                paging.setOffset(0);
                paging.setMaxSize(1);
                query.setPaging(paging);
                Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
                        .item(ShadowType.F_ASSOCIATION).dontRetrieve()
                        .build();
                SearchResultMetadata resultMetadata;
                try {
                    resultMetadata = searchObjectsIterative(query, options, handler, false, task, result);
                } catch (SchemaException | ObjectNotFoundException | ConfigurationException
                        | SecurityViolationException e) {
                    result.recordFatalError(e);
                    throw e;
                }
                result.computeStatus();
                result.cleanupResult();

                return resultMetadata.getApproxNumberOfAllResults();

            } else if (simulate == CountObjectsSimulateType.SEQUENTIAL_SEARCH) {
                //fix for MID-5204. as sequentialSearch option causes to fetch all resource objects,
                // query paging is senseless here
                if (query != null) {
                    query.setPaging(null);
                }
                LOGGER.trace("countObjects: simulating counting with sequential search (likely performance impact)");
                // traditional way of counting objects (i.e. counting them one by one)
                final Holder<Integer> countHolder = new Holder<>(0);

                final ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {

                    @Override
                    public boolean handle(PrismObject<ShadowType> shadow, OperationResult objResult) {
                        int count = countHolder.getValue();
                        count++;
                        countHolder.setValue(count);
                        return true;
                    }
                };

                Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
                        .item(ShadowType.F_ASSOCIATION).dontRetrieve()
                        .build();

                searchObjectsIterative(query, options, handler, false, task, result);
                // TODO: better error handling
                result.computeStatus();
                result.cleanupResult();
                return countHolder.getValue();

            } else {
                throw new IllegalArgumentException("Unknown count capability simulate type " + simulate);

            }
        }

    }

    private PrismObjectDefinition<ShadowType> getShadowDefinition() {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
    }

//    private void deleteShadowFromRepoIfNeeded(Change change, OperationResult parentResult)
//            throws ObjectNotFoundException {
//        if (change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE
//                && change.getOldShadow() != null) {
//            LOGGER.trace("Deleting detected shadow object form repository.");
//            try {
//                repositoryService.deleteObject(ShadowType.class, change.getOldShadow().getOid(),
//                        parentResult);
//                LOGGER.debug("Shadow object successfully deleted form repository.");
//            } catch (ObjectNotFoundException ex) {
//                // What we want to delete is already deleted. Not a big problem.
//                LOGGER.debug("Shadow object {} already deleted from repository ({})", change.getOldShadow(),
//                        ex);
//                parentResult.recordHandledError(
//                        "Shadow object " + change.getOldShadow() + " already deleted from repository", ex);
//            }
//
//        }
//    }

    /**
     * Make sure that the shadow is complete, e.g. that all the mandatory fields
     * are filled (e.g name, resourceRef, ...) Also transforms the shadow with
     * respect to simulated capabilities. Also shadowRefs are added to associations.
     */
    public PrismObject<ShadowType> completeShadow(ProvisioningContext ctx,
            PrismObject<ShadowType> resourceShadow, PrismObject<ShadowType> repoShadow, boolean isDoDiscovery,
            OperationResult parentResult)
                    throws SchemaException, ConfigurationException, ObjectNotFoundException,
                    CommunicationException, SecurityViolationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        PrismObject<ShadowType> resultShadow = repoShadow.clone();

        // The real definition may be different than that of repo shadow (e.g.
        // different auxiliary object classes).
        resultShadow.applyDefinition(ctx.getObjectClassDefinition().getObjectDefinition(), true);

        assert resultShadow.getPrismContext() != null : "No prism context in resultShadow";

        ResourceAttributeContainer resourceAttributesContainer = ShadowUtil
                .getAttributesContainer(resourceShadow);

        ShadowType resultShadowType = resultShadow.asObjectable();
        ShadowType repoShadowType = repoShadow.asObjectable();
        ShadowType resourceShadowType = resourceShadow.asObjectable();

        Collection<QName> auxObjectClassQNames = new ArrayList<>();

        // Always take auxiliary object classes from the resource. Unlike
        // structural object classes
        // the auxiliary object classes may change.
        resultShadow.removeProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
        PrismProperty<QName> resourceAuxOcProp = resourceShadow
                .findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
        if (resourceAuxOcProp != null) {
            PrismProperty<QName> resultAuxOcProp = resultShadow
                    .findOrCreateProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
            resultAuxOcProp.addAll(PrismValueCollectionsUtil.cloneCollection(resourceAuxOcProp.getValues()));
            auxObjectClassQNames.addAll(resultAuxOcProp.getRealValues());
        }

        resultShadowType.setName(new PolyStringType(ShadowUtil.determineShadowName(resourceShadow)));
        if (resultShadowType.getObjectClass() == null) {
            resultShadowType.setObjectClass(resourceAttributesContainer.getDefinition().getTypeName());
        }

        // Attributes
        resultShadow.removeContainer(ShadowType.F_ATTRIBUTES);
        ResourceAttributeContainer resultAttributes = resourceAttributesContainer.clone();
        accessChecker.filterGetAttributes(resultAttributes, ctx.computeCompositeObjectClassDefinition(auxObjectClassQNames), parentResult);
        resultShadow.add(resultAttributes);

        resultShadowType.setIgnored(resourceShadowType.isIgnored());

        resultShadowType.setActivation(resourceShadowType.getActivation());

        ShadowType resultAccountShadow = resultShadow.asObjectable();
        ShadowType resourceAccountShadow = resourceShadow.asObjectable();

        // Credentials
        resultAccountShadow.setCredentials(resourceAccountShadow.getCredentials());
        transplantPasswordMetadata(repoShadowType, resultAccountShadow);

        // protected
        ProvisioningUtil.setProtectedFlag(ctx, resultShadow, matchingRuleRegistry, relationRegistry);

        // exists, dead
        // This may seem strange, but always take exists and dead flags from the repository.
        // Repository is wiser in this case. It may seem that the shadow exists if it is returned
        // by the resource. But that may be just a quantum illusion (gestation and corpse shadow states).

        // Activation
        ActivationType resultActivationType = resultShadowType.getActivation();
        ActivationType repoActivation = repoShadowType.getActivation();
        if (repoActivation != null) {
            if (resultActivationType == null) {
                resultActivationType = new ActivationType();
                resultShadowType.setActivation(resultActivationType);
            }
            resultActivationType.setId(repoActivation.getId());
            // .. but we want metadata from repo
            resultActivationType.setDisableReason(repoActivation.getDisableReason());
            resultActivationType.setEnableTimestamp(repoActivation.getEnableTimestamp());
            resultActivationType.setDisableTimestamp(repoActivation.getDisableTimestamp());
            resultActivationType.setArchiveTimestamp(repoActivation.getArchiveTimestamp());
            resultActivationType.setValidityChangeTimestamp(repoActivation.getValidityChangeTimestamp());
        }

        // Associations
        PrismContainer<ShadowAssociationType> resourceAssociationContainer = resourceShadow
                .findContainer(ShadowType.F_ASSOCIATION);
        if (resourceAssociationContainer != null) {
            PrismContainer<ShadowAssociationType> associationContainer = resourceAssociationContainer.clone();
            resultShadow.addReplaceExisting(associationContainer);
            completeAssociations(ctx, resourceShadow, repoShadow, isDoDiscovery, associationContainer, parentResult);
        }

        resultShadowType.setCachingMetadata(resourceShadowType.getCachingMetadata());

        // Sanity asserts to catch some exotic bugs
        PolyStringType resultName = resultShadow.asObjectable().getName();
        assert resultName != null : "No name generated in " + resultShadow;
        assert !StringUtils.isEmpty(resultName.getOrig()) : "No name (orig) in " + resultShadow;
        assert !StringUtils.isEmpty(resultName.getNorm()) : "No name (norm) in " + resultShadow;

        return resultShadow;
    }

    private void completeAssociations(ProvisioningContext ctx, PrismObject<ShadowType> resourceShadow, PrismObject<ShadowType> repoShadow, boolean isDoDiscovery, PrismContainer<ShadowAssociationType> associationContainer, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException, SecurityViolationException, EncryptionException {
        if (associationContainer == null) {
            return;
        }

        Iterator<PrismContainerValue<ShadowAssociationType>> iterator = associationContainer.getValues().iterator();
        while (iterator.hasNext()) {
            PrismContainerValue<ShadowAssociationType> associationCVal = iterator.next();

            ResourceAttributeContainer identifierContainer = ShadowUtil.getAttributesContainer(associationCVal, ShadowAssociationType.F_IDENTIFIERS);
            Collection<ResourceAttribute<?>> entitlementIdentifiers = identifierContainer.getAttributes();
            if (entitlementIdentifiers == null || entitlementIdentifiers.isEmpty()) {
                throw new IllegalStateException("No entitlement identifiers present for association " + associationCVal + " " + ctx.getDesc());
            }
            ShadowAssociationType shadowAssociationType = associationCVal.asContainerable();
            QName associationName = shadowAssociationType.getName();
            RefinedAssociationDefinition rEntitlementAssociationDef = ctx.getObjectClassDefinition().findAssociationDefinition(associationName);
            if (rEntitlementAssociationDef == null) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Entitlement association with name {} couldn't be found in {} {}\nresource shadow:\n{}\nrepo shadow:\n{}",
                            associationName, ctx.getObjectClassDefinition(), ctx.getDesc(),
                            resourceShadow.debugDump(1), repoShadow == null ? null : repoShadow.debugDump(1));
                    LOGGER.trace("Full refined definition: {}", ctx.getObjectClassDefinition().debugDump());
                }
                throw new SchemaException("Entitlement association with name " + associationName
                        + " couldn't be found in " + ctx.getObjectClassDefinition() + " " + ctx.getDesc() + ", with using shadow coordinates " + ctx.isUseRefinedDefinition());
            }
            ShadowKindType entitlementKind = rEntitlementAssociationDef.getKind();
            if (entitlementKind == null) {
                entitlementKind = ShadowKindType.ENTITLEMENT;
            }

            for (String entitlementIntent : rEntitlementAssociationDef.getIntents()) {
                ProvisioningContext ctxEntitlement = ctx.spawn(entitlementKind, entitlementIntent);

                PrismObject<ShadowType> entitlementRepoShadow;
                PrismObject<ShadowType> entitlementShadow = identifierContainer.getUserData(ResourceObjectConverter.FULL_SHADOW_KEY);
                if (entitlementShadow == null) {
                    try {
                        entitlementRepoShadow = shadowManager.lookupShadowInRepository(ctxEntitlement, identifierContainer, parentResult);
                        if (entitlementRepoShadow == null) {

                            entitlementShadow = resourceObjectConverter.locateResourceObject(ctxEntitlement, entitlementIdentifiers, parentResult);

                            // Try to look up repo shadow again, this time with full resource shadow. When we
                            // have searched before we might have only some identifiers. The shadow
                            // might still be there, but it may be renamed
                            entitlementRepoShadow = acquireRepositoryShadow(ctxEntitlement, entitlementShadow, false, isDoDiscovery, parentResult);
                        }
                    } catch (ObjectNotFoundException e) {
                        // The entitlement to which we point is not there.
                        // Simply ignore this association value.
                        parentResult.muteLastSubresultError();
                        LOGGER.warn("The entitlement identified by {} referenced from {} does not exist. Skipping.", associationCVal, resourceShadow);
                        continue;
                    } catch (SchemaException e) {
                        // The entitlement to which we point is not bad.
                        // Simply ignore this association value.
                        parentResult.muteLastSubresultError();
                        LOGGER.warn("The entitlement identified by {} referenced from {} violates the schema. Skipping. Original error: {}-{}",
                                    associationCVal, resourceShadow, e.getMessage(), e);
                        continue;
                    }
                } else {
                    entitlementRepoShadow = acquireRepositoryShadow(ctxEntitlement,
                            entitlementShadow, false, isDoDiscovery, parentResult);
                }
                if (doesAssociationMatch(rEntitlementAssociationDef, entitlementRepoShadow)) {
                    ObjectReferenceType shadowRefType = ObjectTypeUtil.createObjectRef(entitlementRepoShadow, prismContext);
                    shadowAssociationType.setShadowRef(shadowRefType);
                } else {
                    // We have association value that does not match its definition. This may happen because the association attribute
                    // may be shared among several associations. The EntitlementConverter code has no way to tell them apart.
                    // We can do that only if we have shadow or full resource object. And that is available at this point only.
                    // Therefore just silently filter out the association values that do not belong here.
                    // See MID-5790
                    iterator.remove();
                }
            }
        }
    }

    private boolean doesAssociationMatch(RefinedAssociationDefinition rEntitlementAssociationDef, PrismObject<ShadowType> entitlementRepoShadow) {
        ShadowKindType shadowKind = ShadowUtil.getKind(entitlementRepoShadow.asObjectable());
        String shadowIntent = ShadowUtil.getIntent(entitlementRepoShadow.asObjectable());
        if (shadowKind == null || shadowKind == ShadowKindType.UNKNOWN || shadowIntent == null) {
            // We have unclassified shadow here. This should not happen in a well-configured system. But the world is a tough place.
            // In case that this happens let's just keep all such shadows in all associations. This is how midPoint worked before,
            // therefore we will get better compatibility. But it is also better for visibility. MidPoint will show data that are
            // wrong. But it will at least show something. The alternative would be to show nothing, which is not really friendly
            // for debugging.
            return true;
        }
        ShadowKindType defKind = rEntitlementAssociationDef.getKind();
        if (defKind == null) {
            defKind = ShadowKindType.ENTITLEMENT;
        }
        if (!defKind.equals(shadowKind)) {
            return false;
        }
        Collection<String> defIntents = rEntitlementAssociationDef.getIntents();
        if (!defIntents.contains(shadowIntent)) {
            return false;
        }
        return true;
    }

    private void transplantPasswordMetadata(ShadowType repoShadowType, ShadowType resultAccountShadow) {
        CredentialsType repoCreds = repoShadowType.getCredentials();
        if (repoCreds == null) {
            return;
        }
        PasswordType repoPassword = repoCreds.getPassword();
        if (repoPassword == null) {
            return;
        }
        MetadataType repoMetadata = repoPassword.getMetadata();
        if (repoMetadata == null) {
            return;
        }
        CredentialsType resultCreds = resultAccountShadow.getCredentials();
        if (resultCreds == null) {
            resultCreds = new CredentialsType();
            resultAccountShadow.setCredentials(resultCreds);
        }
        PasswordType resultPassword = resultCreds.getPassword();
        if (resultPassword == null) {
            resultPassword = new PasswordType();
            resultCreds.setPassword(resultPassword);
        }
        MetadataType resultMetadata = resultPassword.getMetadata();
        if (resultMetadata == null) {
            resultMetadata = repoMetadata.clone();
            resultPassword.setMetadata(resultMetadata);
        }
    }

    // ENTITLEMENTS

    /**
     * Makes sure that all the entitlements have identifiers in them so this is
     * usable by the ResourceObjectConverter.
     */
    private void preprocessEntitlements(final ProvisioningContext ctx, final PrismObject<ShadowType> shadow,
            final OperationResult result) throws SchemaException, ObjectNotFoundException,
                    ConfigurationException, CommunicationException, ExpressionEvaluationException {
        try {
            shadow.accept(
                    (visitable) -> {
                        try {
                            preprocessEntitlement(ctx, (PrismContainerValue<ShadowAssociationType>) visitable,
                                    shadow.toString(), result);
                        } catch (SchemaException | ObjectNotFoundException | ConfigurationException
                                | CommunicationException | ExpressionEvaluationException e) {
                            throw new TunnelException(e);
                        }
                    },
                    ItemPath.create(ShadowType.F_ASSOCIATION, null), false);
        } catch (TunnelException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SchemaException) {
                throw (SchemaException) cause;
            } else if (cause instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) cause;
            } else if (cause instanceof ConfigurationException) {
                throw (ConfigurationException) cause;
            } else if (cause instanceof CommunicationException) {
                throw (CommunicationException) cause;
            } else if (cause instanceof ExpressionEvaluationException) {
                throw (ExpressionEvaluationException) cause;
            } else {
                throw new SystemException("Unexpected exception " + cause, cause);
            }
        }
    }

    /**
     * Makes sure that all the entitlements have identifiers in them so this is
     * usable by the ResourceObjectConverter.
     */
    private void preprocessEntitlements(final ProvisioningContext ctx,
            Collection<? extends ItemDelta> modifications, final String desc, final OperationResult result)
                    throws SchemaException, ObjectNotFoundException, ConfigurationException,
                    CommunicationException, ExpressionEvaluationException {
        try {
            ItemDeltaCollectionsUtil.accept(modifications,
                    (visitable) -> {
                        try {
                            preprocessEntitlement(ctx, (PrismContainerValue<ShadowAssociationType>) visitable, desc,
                                    result);
                        } catch (SchemaException | ObjectNotFoundException | ConfigurationException
                                | CommunicationException | ExpressionEvaluationException e) {
                            throw new TunnelException(e);
                        }
                    },
                    ItemPath.create(ShadowType.F_ASSOCIATION, null), false);
        } catch (TunnelException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SchemaException) {
                throw (SchemaException) cause;
            } else if (cause instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) cause;
            } else if (cause instanceof ConfigurationException) {
                throw (ConfigurationException) cause;
            } else if (cause instanceof CommunicationException) {
                throw (CommunicationException) cause;
            } else if (cause instanceof ExpressionEvaluationException) {
                throw (ExpressionEvaluationException) cause;
            } else {
                throw new SystemException("Unexpected exception " + cause, cause);
            }
        }
    }

    private void preprocessEntitlement(ProvisioningContext ctx,
            PrismContainerValue<ShadowAssociationType> association, String desc, OperationResult result)
                    throws SchemaException, ObjectNotFoundException, ConfigurationException,
                    CommunicationException, ExpressionEvaluationException {
        PrismContainer<Containerable> identifiersContainer = association
                .findContainer(ShadowAssociationType.F_IDENTIFIERS);
        if (identifiersContainer != null && !identifiersContainer.isEmpty()) {
            // We already have identifiers here
            return;
        }
        ShadowAssociationType associationType = association.asContainerable();
        LOGGER.info("###Shadow association: {}, class: {}", associationType.getName(), associationType.getName().getClass());
        if (associationType.getShadowRef() == null
                || StringUtils.isEmpty(associationType.getShadowRef().getOid())) {
            throw new SchemaException(
                    "No identifiers and no OID specified in entitlements association " + association);
        }
        PrismObject<ShadowType> repoShadow;
        try {
            repoShadow = repositoryService.getObject(ShadowType.class,
                    associationType.getShadowRef().getOid(), null, result);
        } catch (ObjectNotFoundException e) {
            throw new ObjectNotFoundException(e.getMessage()
                    + " while resolving entitlement association OID in " + association + " in " + desc, e);
        }
        shadowCaretaker.applyAttributesDefinition(ctx, repoShadow);
        transplantIdentifiers(association, repoShadow);
    }

    private void transplantIdentifiers(PrismContainerValue<ShadowAssociationType> association,
            PrismObject<ShadowType> repoShadow) throws SchemaException {
        PrismContainer<ShadowAttributesType> identifiersContainer = association
                .findContainer(ShadowAssociationType.F_IDENTIFIERS);
        if (identifiersContainer == null) {
            ResourceAttributeContainer origContainer = ShadowUtil.getAttributesContainer(repoShadow);
            identifiersContainer = ObjectFactory.createResourceAttributeContainer(ShadowAssociationType.F_IDENTIFIERS,
                    origContainer.getDefinition(), prismContext);
            association.add(identifiersContainer);
        }
        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(repoShadow);
        for (ResourceAttribute<?> identifier : identifiers) {
            identifiersContainer.add(identifier.clone());
        }
        Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil
                .getSecondaryIdentifiers(repoShadow);
        for (ResourceAttribute<?> identifier : secondaryIdentifiers) {
            identifiersContainer.add(identifier.clone());
        }
    }

    public void propagateOperations(PrismObject<ResourceType> resource, PrismObject<ShadowType> shadow, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, GenericFrameworkException, ObjectAlreadyExistsException, SecurityViolationException, PolicyViolationException, EncryptionException {
        ResourceConsistencyType resourceConsistencyType = resource.asObjectable().getConsistency();
        if (resourceConsistencyType == null) {
            LOGGER.warn("Skipping propagation of {} because no there is no consistency definition in resource", shadow);
            return;
        }
        Duration operationGroupingInterval = resourceConsistencyType.getOperationGroupingInterval();
        if (operationGroupingInterval == null) {
            LOGGER.warn("Skipping propagation of {} because no there is no operationGroupingInterval defined in resource", shadow);
            return;
        }
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        List<PendingOperationType> pendingExecutionOperations = new ArrayList<>();
        boolean triggered = false;
        for (PendingOperationType pendingOperation: shadow.asObjectable().getPendingOperation()) {
            PendingOperationExecutionStatusType executionStatus = pendingOperation.getExecutionStatus();
            if (executionStatus == PendingOperationExecutionStatusType.EXECUTION_PENDING) {
                pendingExecutionOperations.add(pendingOperation);
                if (isPropagationTriggered(pendingOperation, operationGroupingInterval, now)) {
                    triggered = true;
                }
            }
        }
        if (!triggered) {
            LOGGER.debug("Skipping propagation of {} because no pending operation triggered propagation", shadow);
            return;
        }
        if (pendingExecutionOperations.isEmpty()) {
            LOGGER.debug("Skipping propagation of {} because there are no pending executions", shadow);
            return;
        }
        LOGGER.debug("Propagating {} pending operations in {} ", pendingExecutionOperations.size(), shadow);

        ObjectDelta<ShadowType> operationDelta = null;
        List<PendingOperationType> sortedOperations = shadowCaretaker.sortPendingOperations(pendingExecutionOperations);
        for (PendingOperationType pendingOperation: sortedOperations) {
            ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
            ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);
            applyDefinition(pendingDelta, shadow.asObjectable(), result);
            if (operationDelta == null) {
                operationDelta = pendingDelta;
            } else {
                operationDelta.merge(pendingDelta);
            }
        }

        ProvisioningContext ctx = ctxFactory.create(shadow, task, result);
        ctx.setPropagation(true);
        shadowCaretaker.applyAttributesDefinition(ctx, shadow);
        shadowCaretaker.applyAttributesDefinition(ctx, operationDelta);
        LOGGER.trace("Merged operation for {}:\n{} ", shadow, operationDelta.debugDumpLazily(1));

        if (operationDelta.isAdd()) {
            PrismObject<ShadowType> shadowToAdd = operationDelta.getObjectToAdd();
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState =
                    ProvisioningOperationState.fromPendingOperations(shadow, sortedOperations);
            shadowToAdd.setOid(shadow.getOid());
            addShadowAttempt(ctx, shadowToAdd, null, opState, null, task, result);
            opState.determineExecutionStatusFromResult();

            shadowManager.updatePendingOperations(ctx, shadow, opState, pendingExecutionOperations, now, result);

            notifyAfterAdd(ctx, opState.getAsyncResult().getReturnValue(), opState, task, result);

        } else if (operationDelta.isModify()) {
            Collection<? extends ItemDelta<?,?>> modifications = operationDelta.getModifications();
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState =
                    executeResourceModify(ctx, shadow, modifications, null, null, now, task, result);
            opState.determineExecutionStatusFromResult();

            shadowManager.updatePendingOperations(ctx, shadow, opState, pendingExecutionOperations, now, result);

            notifyAfterModify(ctx, shadow, modifications, opState, task, result);

        } else if (operationDelta.isDelete()) {
            ProvisioningOperationState<AsynchronousOperationResult> opState = executeResourceDelete(ctx, shadow, null, null, task, result);
            opState.determineExecutionStatusFromResult();

            shadowManager.updatePendingOperations(ctx, shadow, opState, pendingExecutionOperations, now, result);

            notifyAfterDelete(ctx, shadow, opState, task, result);

        } else {
            throw new IllegalStateException("Delta from outer space: "+operationDelta);
        }

        // do we need to modify exists/dead flags?

    }

    private boolean isPropagationTriggered(PendingOperationType pendingOperation, Duration operationGroupingInterval, XMLGregorianCalendar now) {
        XMLGregorianCalendar requestTimestamp = pendingOperation.getRequestTimestamp();
        if (requestTimestamp == null) {
            return false;
        }
        return XmlTypeConverter.isAfterInterval(requestTimestamp, operationGroupingInterval, now);
    }

    public <T> ItemComparisonResult compare(PrismObject<ShadowType> repositoryShadow, ItemPath path, T expectedValue, Task task, OperationResult parentResult)
                throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {

        if (!path.equivalent(SchemaConstants.PATH_PASSWORD_VALUE)) {
            throw new UnsupportedOperationException("Only password comparison is supported");
        }

        ProvisioningContext ctx = ctxFactory.create(repositoryShadow, task, parentResult);
        try {
            ctx.assertDefinition();
            shadowCaretaker.applyAttributesDefinition(ctx, repositoryShadow);
        } catch (ObjectNotFoundException | SchemaException | CommunicationException
                | ConfigurationException | ExpressionEvaluationException e) {
            throw e;
        }

        ResourceType resource = ctx.getResource();

        PasswordCompareStrategyType passwordCompareStrategy = getPasswordCompareStrategy(ctx.getObjectClassDefinition());
        if (passwordCompareStrategy == PasswordCompareStrategyType.ERROR) {
            throw new UnsupportedOperationException("Password comparison is not supported on "+resource);
        }

        PrismProperty<T> repoProperty = repositoryShadow.findProperty(path);
        if (repoProperty == null) {
            if (passwordCompareStrategy == PasswordCompareStrategyType.CACHED) {
                if (expectedValue == null) {
                    return ItemComparisonResult.MATCH;
                } else {
                    return ItemComparisonResult.MISMATCH;
                }
            } else {
                // AUTO
                return ItemComparisonResult.NOT_APPLICABLE;
            }
        }

        ProtectedStringType repoProtectedString = (ProtectedStringType) repoProperty.getRealValue();
        ProtectedStringType expectedProtectedString;
        if (expectedValue instanceof ProtectedStringType) {
            expectedProtectedString = (ProtectedStringType) expectedValue;
        } else {
            expectedProtectedString = new ProtectedStringType();
            expectedProtectedString.setClearValue((String) expectedValue);
        }
        if (protector.compareCleartext(repoProtectedString, expectedProtectedString)) {
            return ItemComparisonResult.MATCH;
        } else {
            return ItemComparisonResult.MISMATCH;
        }
    }

    private PasswordCompareStrategyType getPasswordCompareStrategy(RefinedObjectClassDefinition objectClassDefinition) {
        ResourcePasswordDefinitionType passwordDefinition = objectClassDefinition.getPasswordDefinition();
        if (passwordDefinition == null) {
            return null;
        }
        return passwordDefinition.getCompareStrategy();
    }

    private ConnectorOperationOptions createConnectorOperationOptions(ProvisioningContext ctx, ProvisioningOperationOptions options, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        if (options == null) {
            return null;
        }
        String runAsAccountOid = options.getRunAsAccountOid();
        if (runAsAccountOid == null) {
            return null;
        }
        RunAsCapabilityType capRunAs = ctx.getEffectiveCapability(RunAsCapabilityType.class);
        if (capRunAs == null) {
            LOGGER.trace("Operation runAs requested, but resource does not have the capability. Ignoring runAs");
            return null;
        }
        PrismObject<ShadowType> runAsShadow;
        try {
            runAsShadow = shadowManager.getRepoShadow(runAsAccountOid, result);
        } catch (ObjectNotFoundException e) {
            throw new ConfigurationException("Requested non-existing 'runAs' shadow", e);
        }
        ProvisioningContext runAsCtx = ctxFactory.create(runAsShadow, null, ctx.getTask(), result);
        shadowCaretaker.applyAttributesDefinition(runAsCtx, runAsShadow);
        ResourceObjectIdentification runAsIdentification = ResourceObjectIdentification.createFromShadow(runAsCtx.getObjectClassDefinition(), runAsShadow.asObjectable());
        ConnectorOperationOptions connOptions = new ConnectorOperationOptions();
        LOGGER.trace("RunAs identification: {}", runAsIdentification);
        connOptions.setRunAsIdentification(runAsIdentification);
        return connOptions;
    }

    private String getAdditionalOperationDesc(OperationProvisioningScriptsType scripts,
            ProvisioningOperationOptions options) {
        if (scripts == null && options == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" (");
        if (options != null) {
            sb.append("options:");
            options.shortDump(sb);
            if (scripts != null) {
                sb.append("; ");
            }
        }
        if (scripts != null) {
            sb.append("scripts");
        }
        sb.append(")");
        return sb.toString();
    }

}
