/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.provisioning.impl.sync.AsyncUpdater;
import com.evolveum.midpoint.provisioning.impl.sync.SynchronizationOperationResult;
import com.evolveum.midpoint.provisioning.impl.sync.LiveSynchronizer;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.LabeledString;
import com.evolveum.midpoint.schema.ProvisioningDiag;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Implementation of provisioning service.
 *
 * It is just a "dispatcher" that routes interface calls to appropriate places.
 * E.g. the operations regarding resource definitions are routed directly to the
 * repository, operations of shadow objects are routed to the shadow cache and
 * so on.
 *
 * @author Radovan Semancik
 */
@Service(value = "provisioningService")
@Primary
public class ProvisioningServiceImpl implements ProvisioningService, SystemConfigurationChangeListener {

    private static final String OPERATION_REFRESH_SHADOW = ProvisioningServiceImpl.class.getName() +".refreshShadow";
    private static final String OPERATION_DETERMINE_SHADOW_STATE = ProvisioningServiceImpl.class.getName() +".determineShadowState";

    @Autowired ShadowCache shadowCache;
    @Autowired ResourceManager resourceManager;
    @Autowired ConnectorManager connectorManager;
    @Autowired ProvisioningContextFactory ctxFactory;
    @Autowired PrismContext prismContext;
    @Autowired CacheConfigurationManager cacheConfigurationManager;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;
    @Autowired private LiveSynchronizer liveSynchronizer;
    @Autowired private AsyncUpdater asyncUpdater;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired private Clock clock;
    @Autowired private TaskManager taskManager;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    private PrismObjectDefinition<ShadowType> resourceObjectShadowDefinition;

    private SystemConfigurationType systemConfiguration;

    private static final Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImpl.class);

    private static final String DETAILS_CONNECTOR_FRAMEWORK_VERSION = "ConnId framework version";       // TODO generalize

    /**
     * Get the value of repositoryService.
     *
     * @return the value of repositoryService
     */
    public RepositoryService getCacheRepositoryService() {
        return cacheRepositoryService;
    }

    /**
     * Set the value of repositoryService
     *
     * Expected to be injected.
     *
     * @param repositoryService
     *            new value of repositoryService
     */
    public void setCacheRepositoryService(RepositoryService repositoryService) {
        this.cacheRepositoryService = repositoryService;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        Validate.notNull(oid, "Oid of object to get must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        // Result type for this operation
        OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".getObject");
        result.addParam(OperationResult.PARAM_OID, oid);
        result.addParam(OperationResult.PARAM_TYPE, type);
        result.addArbitraryObjectCollectionAsParam("options", options);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        PrismObject<T> resultingObject;

        if (ResourceType.class.isAssignableFrom(type)) {
            if (GetOperationOptions.isRaw(rootOptions)) {
                try {
                    resultingObject = (PrismObject<T>) cacheRepositoryService.getObject(ResourceType.class, oid, options, result);
                } catch (ObjectNotFoundException|SchemaException ex) {
                    // catching an exception is important because otherwise the result is UNKNOWN
                    result.recordFatalError(ex);
                    throw ex;
                }
                try {
                    applyDefinition(resultingObject, task, result);
                } catch (ObjectNotFoundException ex) {
                    // this is almost OK, we use raw for debug pages, so we want
                    // to return resource and it can be fixed
                    result.muteLastSubresultError();
                    ProvisioningUtil.logWarning(LOGGER, result,
                            "Bad connector reference defined for resource:  " + ex.getMessage(), ex);
                } catch (SchemaException ex) {
                    result.muteLastSubresultError();
                    ProvisioningUtil.logWarning(LOGGER, result, "Schema violation:  " + ex.getMessage(), ex);
                } catch (ConfigurationException ex) {
                    result.muteLastSubresultError();
                    ProvisioningUtil.logWarning(LOGGER, result, "Configuration problem:  " + ex.getMessage(), ex);
                }
            } else {
                // We need to handle resource specially. This is usually cached
                // and we do not want to get the repository
                // object if it is in the cache.

                // Make sure that the object is complete, e.g. there is a
                // (fresh)
                // schema
                try {
                    resultingObject = (PrismObject<T>) resourceManager.getResource(oid, SelectorOptions.findRootOptions(options), task, result);
                } catch (ObjectNotFoundException ex) {
                    ProvisioningUtil.recordFatalError(LOGGER, result, "Resource object not found", ex);
                    throw ex;
                } catch (SchemaException ex) {
                    ProvisioningUtil.recordFatalError(LOGGER, result, "Schema violation", ex);
                    throw ex;
                } catch (CommunicationException ex) {
                    ProvisioningUtil.recordFatalError(LOGGER, result, "Error communicating with resource", ex);
                    throw ex;
                } catch (ConfigurationException ex) {
                    ProvisioningUtil.recordFatalError(LOGGER, result, "Bad resource configuration", ex);
                    throw ex;
                } catch (ExpressionEvaluationException ex) {
                    ProvisioningUtil.recordFatalError(LOGGER, result, "Expression error", ex);
                    throw ex;
                }
            }

        } else {
            // Not resource

            PrismObject<T> repositoryObject = getRepoObject(type, oid, options, result);
            LOGGER.trace("Retrieved repository object:\n{}", repositoryObject.debugDumpLazily());

            if (repositoryObject.canRepresent(ShadowType.class)) {
                try {
                    resultingObject = (PrismObject<T>) shadowCache.getShadow(oid,
                            (PrismObject<ShadowType>) (repositoryObject), options, task, result);
                } catch (ObjectNotFoundException e) {
                    if (!GetOperationOptions.isAllowNotFound(rootOptions)){
                        ProvisioningUtil.recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
                    } else{
                        result.muteLastSubresultError();
                        result.computeStatus();
                    }
                    throw e;
                } catch (CommunicationException | SchemaException | ConfigurationException | SecurityViolationException | RuntimeException | Error e) {
                    ProvisioningUtil.recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
                    throw e;
                } catch (EncryptionException e){
                    ProvisioningUtil.recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
                    throw new SystemException(e);
                }
            } else {
                resultingObject = repositoryObject;
            }
        }

        result.computeStatusIfUnknown();
        if (!GetOperationOptions.isRaw(rootOptions)) {
            resultingObject = resultingObject.cloneIfImmutable();
            resultingObject.asObjectable().setFetchResult(result.createOperationResultType());
        }
        result.cleanupResult();
        return resultingObject;
    }

    @Override
    public <T extends ObjectType> String addObject(PrismObject<T> object, OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options,
            Task task, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException, CommunicationException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        Validate.notNull(object, "Object to add must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        if (InternalsConfig.encryptionChecks) {
            CryptoUtil.checkEncrypted(object);
        }

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".addObject");
        result.addParam("object", object);
        result.addArbitraryObjectAsParam("scripts", scripts);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        String oid = null;
        if (object.canRepresent(ShadowType.class)) {
            try {
                // calling shadow cache to add object
                oid = shadowCache.addShadow((PrismObject<ShadowType>) object, scripts,
                        null, options, task, result);
                LOGGER.trace("Added shadow object {}", oid);
                // Status might be set already (e.g. by consistency mechanism)
                result.computeStatusIfUnknown();
            } catch (GenericFrameworkException ex) {
                ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't add object " + object + ". Reason: " + ex.getMessage(), ex);
                throw new ConfigurationException(ex.getMessage(), ex);
            } catch (ObjectAlreadyExistsException ex) {
                result.computeStatus();
                if (!result.isSuccess() && !result.isHandledError()) {
                    ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't add object. Object already exist: " + ex.getMessage(), ex);
                } else {
                    result.recordSuccess();
                }
                result.cleanupResult(ex);
                throw new ObjectAlreadyExistsException("Couldn't add object. Object already exists: " + ex.getMessage(),
                        ex);
            } catch (EncryptionException e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
                throw new SystemException(e.getMessage(), e);
            } catch (SchemaException | ConfigurationException | SecurityViolationException | PolicyViolationException | ExpressionEvaluationException | RuntimeException | Error e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
                throw e;
            }
        } else {
            RepoAddOptions addOptions = null;
            if (ProvisioningOperationOptions.isOverwrite(options)){
                addOptions = RepoAddOptions.createOverwrite();
            }
            try {
                oid = cacheRepositoryService.addObject(object, addOptions, result);
            } catch (Throwable t) {
                result.recordFatalError(t);
                throw t;
            } finally {
                result.computeStatusIfUnknown();
            }
        }

        result.cleanupResult();
        return oid;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public int synchronize(ResourceShadowDiscriminator shadowCoordinates, Task task, TaskPartitionDefinitionType taskPartition, OperationResult parentResult) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {

        Validate.notNull(shadowCoordinates, "Coordinates oid must not be null.");
        String resourceOid = shadowCoordinates.getResourceOid();
        Validate.notNull(resourceOid, "Resource oid must not be null.");
        Validate.notNull(task, "Task must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".synchronize");
        result.addParam(OperationResult.PARAM_OID, resourceOid);
        result.addParam(OperationResult.PARAM_TASK, task.toString());

        SynchronizationOperationResult liveSyncResult;

        try {
            // Resolve resource
            PrismObject<ResourceType> resource = getObject(ResourceType.class, resourceOid, null, task, result);
            ResourceType resourceType = resource.asObjectable();

            LOGGER.trace("Start synchronization of resource {} ", resourceType);

            liveSyncResult = liveSynchronizer.synchronize(shadowCoordinates, task, taskPartition, result);
            LOGGER.debug("Synchronization of {} done, result: {}", resource, liveSyncResult);

        } catch (ObjectNotFoundException | CommunicationException | SchemaException | SecurityViolationException | ConfigurationException | ExpressionEvaluationException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            result.summarize(true);
            throw e;
        } catch (ObjectAlreadyExistsException e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            result.summarize(true);
            throw new SystemException(e);
        } catch (GenericFrameworkException e) {
            ProvisioningUtil.recordFatalError(LOGGER, result,
                    "Synchronization error: generic connector framework error: " + e.getMessage(), e);
            result.summarize(true);
            throw new GenericConnectorException(e.getMessage(), e);
        }

        if (liveSyncResult.isHaltingErrorEncountered()) {
            // TODO MID-5514
            result.recordPartialError("Object could not be processed and 'retryLiveSyncErrors' is true");
        } else if (liveSyncResult.getErrors() > 0) {
            result.recordPartialError("Errors while processing: " + liveSyncResult.getErrors() + " out of " + liveSyncResult.getChangesProcessed());
        } else {
            result.recordSuccess();
        }
        result.cleanupResult();
        return liveSyncResult.getChangesProcessed();
    }

    @Override
    public String startListeningForAsyncUpdates(@NotNull ResourceShadowDiscriminator shadowCoordinates, @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        String resourceOid = shadowCoordinates.getResourceOid();
        Validate.notNull(resourceOid, "Resource oid must not be null.");

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".startListeningForAsyncUpdates");
        result.addParam(OperationResult.PARAM_OID, resourceOid);
        result.addParam(OperationResult.PARAM_TASK, task.toString());

        String listeningActivityHandle;
        try {
            LOGGER.trace("Starting listening for async updates for {}", shadowCoordinates);
            listeningActivityHandle = asyncUpdater.startListeningForAsyncUpdates(shadowCoordinates, task, result);
        } catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException | ExpressionEvaluationException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            result.summarize(true);
            throw e;
        }
        result.addReturn("listeningActivityHandle", listeningActivityHandle);
        result.recordSuccess();
        result.cleanupResult();
        return listeningActivityHandle;
    }

    @Override
    public void stopListeningForAsyncUpdates(@NotNull String listeningActivityHandle, Task task, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".stopListeningForAsyncUpdates");
        result.addParam("listeningActivityHandle", listeningActivityHandle);

        try {
            LOGGER.trace("Stopping listening for async updates for {}", listeningActivityHandle);
            asyncUpdater.stopListeningForAsyncUpdates(listeningActivityHandle, task, result);
        } catch (RuntimeException | Error e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            result.summarize(true);
            throw e;
        }
        result.recordSuccess();
        result.cleanupResult();
    }

    @Override
    public AsyncUpdateListeningActivityInformationType getAsyncUpdatesListeningActivityInformation(@NotNull String listeningActivityHandle, Task task, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".getAsyncUpdatesListeningActivityInformation");
        result.addParam("listeningActivityHandle", listeningActivityHandle);

        try {
            AsyncUpdateListeningActivityInformationType rv = asyncUpdater
                    .getAsyncUpdatesListeningActivityInformation(listeningActivityHandle, task, result);
            result.recordSuccess();
            result.cleanupResult();
            return rv;
        } catch (RuntimeException | Error e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            result.summarize(true);
            throw e;
        }
    }

    @NotNull
    @Override
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
                    throws SchemaException, ObjectNotFoundException, CommunicationException,
                        ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".searchObjects");
        result.addParam("objectType", type);
        result.addParam("query", query);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        LOGGER.trace("Start of (non-iterative) search objects. Query:\n{}", query != null ? query.debugDumpLazily(1) : "  (null)");

        query = simplifyQueryFilter(query);
        ObjectFilter filter = query != null ? query.getFilter() : null;

        if (InternalsConfig.consistencyChecks && filter != null) {
            // We may not have all the definitions here. We will apply the definitions later
            filter.checkConsistence(false);
        }

        if (filter instanceof NoneFilter) {
            result.recordSuccessIfUnknown();
            result.cleanupResult();
            SearchResultMetadata metadata = new SearchResultMetadata();
            metadata.setApproxNumberOfAllResults(0);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Finished searching. Nothing to do. Filter is NONE. Metadata: {}", metadata.shortDump());
            }
            SearchResultList<PrismObject<T>> objListType = new SearchResultList<>(new ArrayList<>());
            objListType.setMetadata(metadata);
            return objListType;
        }

        SearchResultList<PrismObject<T>> objects;
        if (ShadowType.class.isAssignableFrom(type)) {
            try {
                //noinspection unchecked
                objects = (SearchResultList) shadowCache.searchObjects(query, options, task, result);
            } catch (Throwable e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, "Could not search objects: " + e.getMessage(), e);
                throw e;
            }
        } else {
            objects = searchRepoObjects(type, query, options, task, result);
        }

        result.computeStatus();
        result.cleanupResult();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finished searching. Metadata: {}", DebugUtil.shortDump(objects.getMetadata()));
        }
        // validateObjects(objListType);
        return objects;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <T extends ObjectType> SearchResultList<PrismObject<T>> searchRepoObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws SchemaException {

        List<PrismObject<T>> repoObjects;

        // TODO: should searching connectors trigger rediscovery?

        Collection<SelectorOptions<GetOperationOptions>> repoOptions = null;
        if (GetOperationOptions.isReadOnly(SelectorOptions.findRootOptions(options))) {
            repoOptions = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
        }
        repoObjects = getCacheRepositoryService().searchObjects(type, query, repoOptions, result);

        SearchResultList<PrismObject<T>> newObjListType = new SearchResultList(new ArrayList<PrismObject<T>>());
        for (PrismObject<T> repoObject : repoObjects) {
            OperationResult objResult = new OperationResult(ProvisioningService.class.getName() + ".searchObjects.object");

            try {

                PrismObject<T> completeResource = completeObject(type, repoObject, options, task, objResult);
                objResult.computeStatusIfUnknown();
                if (!objResult.isSuccess()) {
                    completeResource.asObjectable().setFetchResult(objResult.createOperationResultType());      // necessary e.g. to skip validation for resources that had issues when checked
                    result.addSubresult(objResult);
                }
                newObjListType.add(completeResource);

                // TODO: what else do to with objResult??

            } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                LOGGER.error("Error while completing {}: {}-{}. Using non-complete object.", repoObject, e.getMessage(), e);
                objResult.recordFatalError(e);
                repoObject.asObjectable().setFetchResult(objResult.createOperationResultType());
                newObjListType.add(repoObject);
                result.addSubresult(objResult);
                result.recordPartialError(e);

            } catch (RuntimeException e) {
                // FIXME: Strictly speaking, the runtime exception should not be handled here.
                //  The runtime exceptions should be considered fatal anyway ... but some of the
                //  ICF exceptions are still translated to system exceptions. So this provides
                //  a better robustness now.
                LOGGER.error("System error while completing {}: {}-{}. Using non-complete object.", repoObject, e.getMessage(), e);
                objResult.recordFatalError(e);
                repoObject.asObjectable().setFetchResult(objResult.createOperationResultType());
                newObjListType.add(repoObject);
                result.addSubresult(objResult);
                result.recordPartialError(e);
            }
        }
        return newObjListType;

    }

    // TODO: move to ResourceManager and ConnectorManager
    // the shadow-related code is already in the ShadowCache
    private <T extends ObjectType> PrismObject<T> completeObject(Class<T> type, PrismObject<T> inObject,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        if (ResourceType.class.equals(type)) {

                PrismObject<ResourceType> completeResource = resourceManager.getResource((PrismObject<ResourceType>) inObject,
                        SelectorOptions.findRootOptions(options), task, result);
                return (PrismObject<T>) completeResource;
        } else if (ShadowType.class.equals(type)) {
            // should not happen, the shadow-related code is already in the ShadowCache
            throw new IllegalStateException("BOOOM!");
        } else {
            //TODO: connectors etc..

        }
        return inObject;

    }

    public <T extends ObjectType> Integer countObjects(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".countObjects");
        result.addParam("objectType", type);
        result.addParam("query", query);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        query = simplifyQueryFilter(query);
        ObjectFilter filter = query != null ? query.getFilter() : null;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Start of counting objects. Query:\n{}", query != null ? query.debugDump(1) : "  (null)");
        }

        if (filter != null && filter instanceof NoneFilter) {
            result.recordSuccessIfUnknown();
            result.cleanupResult();
            LOGGER.trace("Finished counting. Nothing to do. Filter is NONE");
            return 0;
        }

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

        if (!ShadowType.class.isAssignableFrom(type) || GetOperationOptions.isNoFetch(rootOptions) || GetOperationOptions.isRaw(rootOptions)) {
            int count = getCacheRepositoryService().countObjects(type, query, options, parentResult);
            result.computeStatus();
            result.recordSuccessIfUnknown();
            result.cleanupResult();
            return count;
        }

        Integer count;
        try {

            count = shadowCache.countObjects(query, task, result);

            result.computeStatus();

        } catch (ConfigurationException | CommunicationException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            throw e;
        } finally {
            result.cleanupResult();
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finished counting objects: {}", count);
        }

        return count;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public <T extends ObjectType> String modifyObject(Class<T> type, String oid,
            Collection<? extends ItemDelta> modifications, OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        Validate.notNull(oid, "OID must not be null.");
        Validate.notNull(modifications, "Modifications must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        if (InternalsConfig.encryptionChecks) {
            CryptoUtil.checkEncrypted(modifications);
        }

        if (InternalsConfig.consistencyChecks) {
            ItemDeltaCollectionsUtil.checkConsistence(modifications);
        }

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".modifyObject");
        result.addArbitraryObjectCollectionAsParam("modifications", modifications);
        result.addParam(OperationResult.PARAM_OID, oid);
        result.addArbitraryObjectAsParam("scripts", scripts);
        result.addArbitraryObjectAsParam("options", options);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("modifyObject: object modifications:\n{}", DebugUtil.debugDump(modifications));
        }

        // getting object to modify
        PrismObject<T> repoShadow = getRepoObject(type, oid, null, result);

        LOGGER.trace("modifyObject: object to modify (repository):\n{}.", repoShadow.debugDumpLazily());

        try {

            if (ShadowType.class.isAssignableFrom(type)) {
                // calling shadow cache to modify object
                //noinspection unchecked
                oid = shadowCache.modifyShadow((PrismObject<ShadowType>)repoShadow, modifications, scripts, options, task,
                    result);
            } else {
                cacheRepositoryService.modifyObject(type, oid, modifications, result);
            }
            if (!result.isInProgress()) {
                // This is the case when there is already a conflicting pending operation.
                result.computeStatus();
            }

        } catch (CommunicationException | SchemaException | ObjectNotFoundException | ConfigurationException | SecurityViolationException
                | PolicyViolationException | ExpressionEvaluationException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            throw e;
        } catch (GenericFrameworkException e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            throw new CommunicationException(e.getMessage(), e);
        } catch (EncryptionException e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            throw new SystemException(e.getMessage(), e);
        } catch (ObjectAlreadyExistsException e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't modify object: object after modification would conflict with another existing object: " + e.getMessage(), e);
            throw e;
        }

        result.cleanupResult();
        return oid;
    }

    @Override
    public <T extends ObjectType> PrismObject<T> deleteObject(Class<T> type, String oid, ProvisioningOperationOptions options, OperationProvisioningScriptsType scripts,
            Task task, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        Validate.notNull(oid, "Oid of object to delete must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.trace("Start to delete object with oid {}", oid);

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".deleteObject");
        result.addParam("oid", oid);
        result.addArbitraryObjectAsParam("scripts", scripts);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        //TODO: is critical when shadow does not exits anymore?? do we need to log it?? if not, change null to allowNotFound options
        PrismObject<T> object = getRepoObject(type, oid, null, result);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Object from repository to delete:\n{}", object.debugDump(1));
        }

        PrismObject<T> deadShadow = null;

        if (object.canRepresent(ShadowType.class) && !ProvisioningOperationOptions.isRaw(options)) {

            try {

                deadShadow = (PrismObject<T>) shadowCache.deleteShadow((PrismObject<ShadowType>)object, options, scripts, task, result);

            } catch (CommunicationException e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: communication problem: " + e.getMessage(), e);
                throw new CommunicationException(e.getMessage(), e);
            } catch (GenericFrameworkException e) {
                ProvisioningUtil.recordFatalError(LOGGER, result,
                        "Couldn't delete object: generic error in the connector: " + e.getMessage(), e);
                throw new CommunicationException(e.getMessage(), e);
            } catch (SchemaException e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: schema problem: " + e.getMessage(), e);
                throw new SchemaException(e.getMessage(), e);
            } catch (ConfigurationException e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: configuration problem: " + e.getMessage(), e);
                throw e;
            } catch (SecurityViolationException e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: security violation: " + e.getMessage(), e);
                throw e;
            } catch (ExpressionEvaluationException e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: expression errror: " + e.getMessage(), e);
                throw e;
            } catch (PolicyViolationException | RuntimeException | Error e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: " + e.getMessage(), e);
                throw e;
            }

        } else if (object.canRepresent(ResourceType.class)) {

            resourceManager.deleteResource(oid, options, task, result);

        } else {

            try {

                getCacheRepositoryService().deleteObject(type, oid, result);

            } catch (ObjectNotFoundException ex) {
                result.recordFatalError(ex);
                result.cleanupResult(ex);
                throw ex;
            }

        }
        LOGGER.trace("Finished deleting object.");

        if (!result.isInProgress()) {
            // This is the case when there is already a conflicting pending operation.
            result.computeStatus();
        }
        result.cleanupResult();

        return deadShadow;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.provisioning.api.ProvisioningService#executeScript(java.lang.Class, java.lang.String, com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptType, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public <T extends ObjectType> Object executeScript(String resourceOid, ProvisioningScriptType script,
            Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
        Validate.notNull(resourceOid, "Oid of object for script execution must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".executeScript");
        result.addParam("oid", resourceOid);
        result.addArbitraryObjectAsParam("script", script);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        Object scriptResult;
        try {

            scriptResult = resourceManager.executeScript(resourceOid, script, task, result);

        } catch (CommunicationException | SchemaException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | Error  e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            throw e;
        }

        result.computeStatus();
        result.cleanupResult();

        return scriptResult;
    }


    @Override
    public OperationResult testResource(String resourceOid, Task task) throws ObjectNotFoundException {
        // We are not going to create parent result here. We don't want to
        // pollute the result with
        // implementation details, as this will be usually displayed in the
        // table of "test resource" results.

        Validate.notNull(resourceOid, "Resource OID to test is null.");

        LOGGER.trace("Start testing resource with oid {} ", resourceOid);

        OperationResult testResult = new OperationResult(ConnectorTestOperation.TEST_CONNECTION.getOperation());
        testResult.addParam("resourceOid", resourceOid);
        testResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        PrismObject<ResourceType> resource;

        try {
            resource = getRepoObject(ResourceType.class, resourceOid, null, testResult);
            resourceManager.testConnection(resource, task, testResult);

        } catch (SchemaException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
        testResult.computeStatus("Test resource has failed");

        LOGGER.debug("Finished testing {}, result: {} ", resource,
                testResult.getStatus());
        return testResult;
    }

    @Deprecated
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<PrismObject<? extends ShadowType>> listResourceObjects(String resourceOid,
                                                                       QName objectClass, ObjectPaging paging, Task task, OperationResult parentResult) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        final OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
                + ".listResourceObjects");
        result.addParam("resourceOid", resourceOid);
        result.addParam("objectClass", objectClass);
        result.addArbitraryObjectAsParam("paging", paging);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        if (resourceOid == null) {
            throw new IllegalArgumentException("Resource not defined in a search query");
        }
        if (objectClass == null) {
            throw new IllegalArgumentException("Objectclass not defined in a search query");
        }

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resourceOid, objectClass, prismContext);

        final List<PrismObject<? extends ShadowType>> objectList = new ArrayList<>();
        final ResultHandler<ShadowType> shadowHandler = (shadow, objResult) -> {
            LOGGER.trace("listResourceObjects: processing shadow: {}", SchemaDebugUtil.prettyPrintLazily(shadow));
            objectList.add(shadow);
            return true;
        };

        try {

            shadowCache.searchObjectsIterative(query, null, shadowHandler, false, task, result);

        } catch (SchemaException | ObjectNotFoundException | CommunicationException |
                ConfigurationException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | Error  ex) {
            result.recordFatalError(ex.getMessage(), ex);
            result.cleanupResult(ex);
            throw ex;
        }
        result.cleanupResult();
        return objectList;
    }

    @Override
    public void refreshShadow(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ObjectAlreadyExistsException, SecurityViolationException, ExpressionEvaluationException {
        Validate.notNull(shadow, "Shadow for refresh must not be null.");
        OperationResult result = parentResult.createSubresult(OPERATION_REFRESH_SHADOW);

        LOGGER.debug("Refreshing shadow {}", shadow);

        try {

            shadowCache.refreshShadow(shadow, options, task, result);

        } catch (CommunicationException | SchemaException | ObjectNotFoundException | ConfigurationException | ExpressionEvaluationException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't refresh shadow: " + e.getClass().getSimpleName() + ": "+ e.getMessage(), e);
            throw e;

        } catch (EncryptionException e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            throw new SystemException(e.getMessage(), e);
        }

        result.computeStatus();
        result.cleanupResult();

        LOGGER.debug("Finished refreshing shadow {}: {}", shadow, result);
    }

    // TODO: move to ResourceManager and ConnectorManager
    // the shadow-related code is already in the ShadowCache
    private <T extends ObjectType> boolean handleRepoObject(final Class<T> type, PrismObject<T> object,
              final Collection<SelectorOptions<GetOperationOptions>> options,
              final ResultHandler<T> handler, Task task, final OperationResult objResult) {

        PrismObject<T> completeObject;
        try {

            completeObject = completeObject(type, object, options, task, objResult);

        } catch (Throwable e) {
            LOGGER.error("Error while completing {}: {}-{}. Using non-complete object.", object, e.getMessage(), e);
            objResult.recordFatalError(e);
            object.asObjectable().setFetchResult(objResult.createOperationResultType());
            completeObject = object;
        }

        objResult.computeStatus();
        objResult.recordSuccessIfUnknown();

        if (!objResult.isSuccess()) {
            OperationResultType resultType = objResult.createOperationResultType();
            completeObject.asObjectable().setFetchResult(resultType);
        }

        return handler.handle(completeObject, objResult);

    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<T> handler, Task task,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        Validate.notNull(parentResult, "Operation result must not be null.");
        Validate.notNull(handler, "Handler must not be null.");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Start of (iterative) search objects. Query:\n{}", query != null ? query.debugDump(1) : "  (null)");
        }

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".searchObjectsIterative");
        result.setSummarizeSuccesses(true);
        result.setSummarizeErrors(true);
        result.setSummarizePartialErrors(true);

        result.addParam("query", query);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        query = simplifyQueryFilter(query);
        ObjectFilter filter = query != null ? query.getFilter() : null;

        if (InternalsConfig.consistencyChecks && filter != null) {
            // We may not have all the definitions here. We will apply the definitions later
            filter.checkConsistence(false);
        }

        if (filter instanceof NoneFilter) {
            result.recordSuccessIfUnknown();
            result.cleanupResult();
            SearchResultMetadata metadata = new SearchResultMetadata();
            metadata.setApproxNumberOfAllResults(0);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Finished searching. Nothing to do. Filter is NONE. Metadata: {}", metadata.shortDump());
            }
            return metadata;
        }

        final GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

        SearchResultMetadata metadata = null;
        if (ShadowType.class.isAssignableFrom(type)) {

            try {

                metadata = shadowCache.searchObjectsIterative(query, options, (ResultHandler<ShadowType>)handler, true, task, result);

                result.computeStatus();
                result.cleanupResult();

            } catch (Throwable e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
                throw e;
            }

        } else {

            ResultHandler<T> internalHandler = (object, objResult) -> handleRepoObject(type, object, options, handler, task, objResult);

            Collection<SelectorOptions<GetOperationOptions>> repoOptions = null;
            if (GetOperationOptions.isReadOnly(rootOptions)) {
                repoOptions = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
            }

            try {

                metadata = getCacheRepositoryService().searchObjectsIterative(type, query, internalHandler, repoOptions, true, result);

                result.computeStatus();
                result.recordSuccessIfUnknown();
                result.cleanupResult();

            } catch (Throwable e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finished searching. Metadata: {}", metadata != null ? metadata.shortDump() : "(null)");
        }

        return metadata;


    }

    private ObjectQuery simplifyQueryFilter(ObjectQuery query) {
        if (query != null) {
            ObjectFilter filter = ObjectQueryUtil.simplify(query.getFilter(), prismContext);
            ObjectQuery clone = query.cloneEmpty();
            clone.setFilter(filter);
            return clone;
        } else {
            return null;
        }
    }

    @Override
    public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult)
            throws CommunicationException {
        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
                + ".discoverConnectors");
        result.addParam("host", hostType);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        Set<ConnectorType> discoverConnectors;
        try {
            discoverConnectors = connectorManager.discoverConnectors(hostType, result);
        } catch (CommunicationException ex) {
            ProvisioningUtil.recordFatalError(LOGGER, result, "Discovery failed: "+ex.getMessage(), ex);
            throw ex;
        }

        result.computeStatus("Connector discovery failed");
        result.cleanupResult();
        return discoverConnectors;
    }

    @Override
    public List<ConnectorOperationalStatus> getConnectorOperationalStatus(String resourceOid, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException  {
        OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName()
                + ".getConnectorOperationalStatus");
        result.addParam("resourceOid", resourceOid);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        PrismObject<ResourceType> resource;
        try {

            resource = resourceManager.getResource(resourceOid, null, task, result);

        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException ex) {
            ProvisioningUtil.recordFatalError(LOGGER, result, ex.getMessage(), ex);
            throw ex;
        }

        List<ConnectorOperationalStatus> stats;
        try {
            stats = resourceManager.getConnectorOperationalStatus(resource, result);
        } catch (Throwable ex) {
            ProvisioningUtil.recordFatalError(LOGGER, result, "Getting operations status from connector for resource "+resourceOid+" failed: "+ex.getMessage(), ex);
            throw ex;
        }

        result.computeStatus();
        result.cleanupResult();
        return stats;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ObjectType> void applyDefinition(ObjectDelta<T> delta, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

            applyDefinition(delta, null, task, parentResult);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ObjectType> void applyDefinition(ObjectDelta<T> delta, Objectable object, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".applyDefinition");
        result.addParam("delta", delta);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        try {

            if (ShadowType.class.isAssignableFrom(delta.getObjectTypeClass())){
                shadowCache.applyDefinition((ObjectDelta<ShadowType>) delta, (ShadowType) object, result);
            } else if (ResourceType.class.isAssignableFrom(delta.getObjectTypeClass())){
                resourceManager.applyDefinition((ObjectDelta<ResourceType>) delta, (ResourceType) object, null, task, result);
            } else {
                throw new IllegalArgumentException("Could not apply definition to deltas for object type: " + delta.getObjectTypeClass());
            }

            result.recordSuccessIfUnknown();
            result.cleanupResult();

        } catch (Throwable e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            throw e;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ObjectType> void applyDefinition(PrismObject<T> object, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".applyDefinition");
        result.addParam(OperationResult.PARAM_OBJECT, object);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        try {

            if (ShadowType.class.isAssignableFrom(object.getCompileTimeClass())){
                shadowCache.applyDefinition((PrismObject<ShadowType>) object, result);
            } else if (ResourceType.class.isAssignableFrom(object.getCompileTimeClass())){
                resourceManager.applyDefinition((PrismObject<ResourceType>) object, task, result);
            } else {
                throw new IllegalArgumentException("Could not apply definition to object type: " + object.getCompileTimeClass());
            }

            result.computeStatus();
            result.recordSuccessIfUnknown();

        } catch (Throwable e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            throw e;
        } finally {
            result.cleanupResult();
        }
    }

    private void setProtectedShadow(PrismObject<ShadowType> shdaow, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        shadowCache.setProtectedShadow(shdaow, result);
    }

    @Override
    public <T extends ObjectType> void applyDefinition(Class<T> type, ObjectQuery query, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".applyDefinition");
        result.addParam(OperationResult.PARAM_TYPE, type);
        result.addParam(OperationResult.PARAM_QUERY, query);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        try {

            if (ObjectQueryUtil.hasAllDefinitions(query)) {
                result.recordNotApplicableIfUnknown();
                return;
            }

            if (ShadowType.class.isAssignableFrom(type)){
                shadowCache.applyDefinition(query, result);
            } else if (ResourceType.class.isAssignableFrom(type)){
                resourceManager.applyDefinition(query, result);
            } else {
                throw new IllegalArgumentException("Could not apply definition to query for object type: " + type);
            }

            result.computeStatus();
            result.recordSuccessIfUnknown();

        } catch (Throwable e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            throw e;
        } finally {
            result.cleanupResult();
        }
    }

    @Override
    public void provisioningSelfTest(OperationResult parentTestResult, Task task) {
        CryptoUtil.securitySelfTest(parentTestResult);
        connectorManager.connectorFrameworkSelfTest(parentTestResult, task);
    }

    @Override
    public ProvisioningDiag getProvisioningDiag() {
        ProvisioningDiag provisioningDiag = new ProvisioningDiag();

        String frameworkVersion = connectorManager.getFrameworkVersion();
        if (frameworkVersion == null) {
            frameworkVersion = "unknown";
        }
        provisioningDiag.getAdditionalDetails().add(new LabeledString(DETAILS_CONNECTOR_FRAMEWORK_VERSION, frameworkVersion));
        return provisioningDiag;
    }

    @Override
    public void postInit(OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".initialize");
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        // Discover local connectors
        Set<ConnectorType> discoverLocalConnectors = connectorManager.discoverLocalConnectors(result);
        for (ConnectorType connector : discoverLocalConnectors) {
            LOGGER.info("Discovered local connector {}", ObjectTypeUtil.toShortString(connector));
        }

        result.computeStatus("Provisioning post-initialization failed");
        result.cleanupResult();
    }

    @PostConstruct
    public void init() {
        systemConfigurationChangeDispatcher.registerListener(this);
    }

    @PreDestroy
    public void shutdown() {
        connectorManager.shutdown();
        systemConfigurationChangeDispatcher.unregisterListener(this);
    }

    @Override
    public ConstraintsCheckingResult checkConstraints(RefinedObjectClassDefinition shadowDefinition,
            PrismObject<ShadowType> shadowObject,
            PrismObject<ShadowType> shadowObjectOld,
            ResourceType resourceType, String shadowOid,
            ResourceShadowDiscriminator resourceShadowDiscriminator, ConstraintViolationConfirmer constraintViolationConfirmer,
            ConstraintsCheckingStrategyType strategy,
            Task task, OperationResult parentResult) throws CommunicationException, SchemaException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".checkConstraints");
        try {
            ConstraintsChecker checker = new ConstraintsChecker();
            checker.setRepositoryService(cacheRepositoryService);
            checker.setCacheConfigurationManager(cacheConfigurationManager);
            checker.setShadowCache(shadowCache);
            checker.setShadowObjectOld(shadowObjectOld);
            checker.setPrismContext(prismContext);
            ProvisioningContext ctx = ctxFactory.create(shadowObject, task, result);
            ctx.setObjectClassDefinition(shadowDefinition);
            ctx.setResource(resourceType);
            ctx.setShadowCoordinates(resourceShadowDiscriminator);
            checker.setProvisioningContext(ctx);
            checker.setShadowObject(shadowObject);
            checker.setShadowOid(shadowOid);
            checker.setConstraintViolationConfirmer(constraintViolationConfirmer);
            checker.setStrategy(strategy);
            if (checker.canSkipChecking()) {
                return ConstraintsCheckingResult.createOk();
            } else {
                return checker.check(task, result);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void enterConstraintsCheckerCache() {
        ConstraintsChecker.enterCache(cacheConfigurationManager.getConfiguration(CacheType.LOCAL_SHADOW_CONSTRAINT_CHECKER_CACHE));
    }

    @Override
    public void exitConstraintsCheckerCache() {
        ConstraintsChecker.exitCache();
    }

    private PrismObjectDefinition<ShadowType> getResourceObjectShadowDefinition() {
        if (resourceObjectShadowDefinition == null) {
            resourceObjectShadowDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(
                    ShadowType.class);
        }
        return resourceObjectShadowDefinition;
    }

    private <T extends ObjectType> PrismObject<T> getRepoObject(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        try {
            return getCacheRepositoryService().getObject(type, oid, options, result);
        } catch (ObjectNotFoundException e) {
            GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
            if (!GetOperationOptions.isAllowNotFound(rootOptions)) {
                ProvisioningUtil.recordFatalError(LOGGER, result, "Can't get object with oid " + oid + ". Reason " + e.getMessage(), e);
            } else {
                result.muteLastSubresultError();
                result.computeStatus();
            }
            throw e;
        } catch (SchemaException ex) {
            ProvisioningUtil.recordFatalError(LOGGER, result, "Can't get object with oid " + oid + ". Reason " + ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public <O extends ObjectType, T> ItemComparisonResult compare(Class<O> type, String oid, ItemPath path,
            T expectedValue, Task task, OperationResult parentResult)
                throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {
        Validate.notNull(oid, "Oid of object to get must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        if (!ShadowType.class.isAssignableFrom(type)) {
            throw new UnsupportedOperationException("Only shadow compare is supported");
        }

        // Result type for this operation
        OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".compare");
        result.addParam(OperationResult.PARAM_OID, oid);
        result.addParam(OperationResult.PARAM_TYPE, type);

        ItemComparisonResult comparisonResult;
        try {

            PrismObject<O> repositoryObject = getRepoObject(type, oid, null, result);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Retrieved repository object:\n{}", repositoryObject.debugDump());
            }

            comparisonResult = shadowCache.compare((PrismObject<ShadowType>) (repositoryObject), path, expectedValue, task, result);

        } catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | EncryptionException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            throw e;
        }


        result.computeStatus();
        result.cleanupResult();

        return comparisonResult;
    }

    @Override
    public boolean update(@Nullable SystemConfigurationType value) {
        systemConfiguration = value;
        return true;
    }

    @Override
    public SystemConfigurationType getSystemConfiguration() {
        return systemConfiguration;
    }

    @Override
    public ShadowState determineShadowState(PrismObject<ShadowType> shadow, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        OperationResult result = parentResult.createMinorSubresult(OPERATION_DETERMINE_SHADOW_STATE);
        try {
            ProvisioningContext ctx = ctxFactory.create(shadow, task, result);
            ShadowState state = shadowCaretaker.determineShadowState(ctx, shadow, clock.currentTimeXMLGregorianCalendar());
            LOGGER.trace("Shadow state for {} is determined to be {}", shadow, state);
            return state;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public TaskManager getTaskManager() {
        return taskManager;
    }
}
