/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.checkNotInMaintenance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.provisioning.impl.shadows.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;

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
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.AsyncUpdater;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.LiveSynchronizer;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.SynchronizationOperationResult;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Implementation of provisioning service.
 * <p>
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

    private static final String OPERATION_REFRESH_SHADOW = ProvisioningServiceImpl.class.getName() + ".refreshShadow";

    @Autowired
    ShadowsFacade shadowsFacade;
    @Autowired ResourceManager resourceManager;
    @Autowired ConnectorManager connectorManager;
    @Autowired ProvisioningContextFactory ctxFactory;
    @Autowired PrismContext prismContext;
    @Autowired CacheConfigurationManager cacheConfigurationManager;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;
    @Autowired private LiveSynchronizer liveSynchronizer;
    @Autowired private AsyncUpdater asyncUpdater;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    private volatile SystemConfigurationType systemConfiguration;

    private static final Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImpl.class);

    /**
     * Get the value of repositoryService.
     *
     * @return the value of repositoryService
     */
    public RepositoryService getCacheRepositoryService() {
        return cacheRepositoryService;
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
        boolean exceptionRecorded = false;
        try {
            GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
            PrismObject<T> resultingObject;

            if (ResourceType.class.isAssignableFrom(type)) {
                if (GetOperationOptions.isRaw(rootOptions)) {
                    resultingObject = (PrismObject<T>) cacheRepositoryService.getObject(ResourceType.class, oid, options, result);
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
                    // We need to handle resource specially. This is usually cached and we do not want to get the repository
                    // object if it is in the cache.

                    // Make sure that the object is complete, e.g. there is a (fresh) schema
                    try {
                        resultingObject = (PrismObject<T>) resourceManager
                                .getResource(oid, SelectorOptions.findRootOptions(options), task, result);
                    } catch (ObjectNotFoundException ex) {
                        ProvisioningUtil.recordFatalError(LOGGER, result, "Resource object not found", ex);
                        exceptionRecorded = true;
                        throw ex;
                    } catch (SchemaException ex) {
                        ProvisioningUtil.recordFatalError(LOGGER, result, "Schema violation", ex);
                        exceptionRecorded = true;
                        throw ex;
                    } catch (ExpressionEvaluationException ex) {
                        ProvisioningUtil.recordFatalError(LOGGER, result, "Expression error", ex);
                        exceptionRecorded = true;
                        throw ex;
                    }
                }

            } else {
                // Not resource

                PrismObject<T> repositoryObject = getRepoObject(type, oid, options, result);
                LOGGER.trace("Retrieved repository object:\n{}", repositoryObject.debugDumpLazily());

                if (repositoryObject.canRepresent(ShadowType.class)) {
                    try {
                        resultingObject = (PrismObject<T>) shadowsFacade.getShadow(oid,
                                (PrismObject<ShadowType>) (repositoryObject), null, options, task, result);
                    } catch (ObjectNotFoundException e) {
                        if (!GetOperationOptions.isAllowNotFound(rootOptions)) {
                            ProvisioningUtil
                                    .recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
                        } else {
                            result.muteLastSubresultError();
                            result.computeStatus();
                        }
                        exceptionRecorded = true;
                        throw e;
                    } catch (MaintenanceException e) {
                        LOGGER.trace(e.getMessage(), e);
                        result.cleanupResult(e);
                        exceptionRecorded = true;
                        throw e;
                    } catch (CommunicationException | SchemaException | ConfigurationException | SecurityViolationException | RuntimeException | Error e) {
                        ProvisioningUtil
                                .recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
                        //noinspection UnusedAssignment - IDEA is wrong here
                        exceptionRecorded = true;
                        throw e;
                    } catch (EncryptionException e) {
                        ProvisioningUtil
                                .recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
                        exceptionRecorded = true;
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
        } catch (Throwable t) {
            // We use this strange construction because in some occasions we record the exception in a custom way
            // (e.g. using custom message). Therefore let's record the fatal error only if it was not recorded previously.
            // An alternative would be to check if the result has a status of FATAL_ERROR already set. But there might be
            // cases (in the future) when we want to record status other than FATAL_ERROR and still throw the exception.
            if (!exceptionRecorded) {
                result.recordFatalError(t);
            }
            throw t;
        }
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

        String oid;
        if (object.canRepresent(ShadowType.class)) {
            try {
                // calling shadow cache to add object
                //noinspection unchecked
                oid = shadowsFacade.addResourceObject((PrismObject<ShadowType>) object, scripts, options, task, result);
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
            } catch (Exception | Error e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
                throw e;
            }
        } else {
            RepoAddOptions addOptions = null;
            if (ProvisioningOperationOptions.isOverwrite(options)) {
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

    @Override
    public @NotNull SynchronizationResult synchronize(ResourceShadowDiscriminator shadowCoordinates, Task task,
            TaskPartitionDefinitionType taskPartition, LiveSyncEventHandler handler, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {

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
            PrismObject<ResourceType> resource = getResource(resourceOid, task, result); // TODO avoid double fetching the resource

            LOGGER.debug("Start synchronization of {}", resource);
            liveSyncResult = liveSynchronizer.synchronize(shadowCoordinates, task, taskPartition, handler, result);
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
        // TODO clean up the above exception and operation result processing

        return new SynchronizationResult(liveSyncResult.getChangesProcessed());
    }

    private PrismObject<ResourceType> getResource(String resourceOid, Task task, OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        PrismObject<ResourceType> resource = getObject(ResourceType.class, resourceOid, null, task, result);
        checkNotInMaintenance(resource);
        return resource;
    }

    @Override
    public void processAsynchronousUpdates(@NotNull ResourceShadowDiscriminator shadowCoordinates,
            @NotNull AsyncUpdateEventHandler handler, @NotNull Task task, @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        String resourceOid = shadowCoordinates.getResourceOid();
        Validate.notNull(resourceOid, "Resource oid must not be null.");

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".startListeningForAsyncUpdates");
        result.addParam(OperationResult.PARAM_OID, resourceOid);
        result.addParam(OperationResult.PARAM_TASK, task.toString());

        try {
            LOGGER.trace("Starting processing async updates for {}", shadowCoordinates);
            asyncUpdater.processAsynchronousUpdates(shadowCoordinates, handler, task, result);
            result.recordSuccess();
        } catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException | ExpressionEvaluationException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            throw e;
        } finally {
            // todo ok?
            result.summarize(true);
            result.cleanupResult();
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
                //noinspection unchecked,rawtypes
                objects = (SearchResultList) shadowsFacade.searchObjects(query, options, task, result);
            } catch (Throwable e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, "Could not search objects: " + e.getMessage(), e);
                throw e;
            }
        } else {
            objects = searchRepoObjects(type, query, options, task, result);
        }

        result.computeStatus();
        result.cleanupResult();
        LOGGER.trace("Finished searching. Metadata: {}", DebugUtil.shortDumpLazily(objects.getMetadata()));
        // validateObjects(objListType);
        return objects;
    }

    @NotNull
    private <T extends ObjectType> SearchResultList<PrismObject<T>> searchRepoObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws SchemaException {

        List<PrismObject<T>> repoObjects;

        // TODO: should searching connectors trigger rediscovery?

        Collection<SelectorOptions<GetOperationOptions>> repoOptions;
        if (GetOperationOptions.isReadOnly(SelectorOptions.findRootOptions(options))) {
            repoOptions = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
        } else {
            repoOptions = null;
        }
        repoObjects = getCacheRepositoryService().searchObjects(type, query, repoOptions, result);

        SearchResultList<PrismObject<T>> newObjListType = new SearchResultList<>(new ArrayList<>());
        for (PrismObject<T> repoObject : repoObjects) {
            OperationResult objResult = new OperationResult(ProvisioningService.class.getName() + ".searchObjects.object");

            try {

                PrismObject<T> completeResource = completeObject(type, repoObject, options, task, objResult);
                objResult.computeStatusIfUnknown();
                if (!objResult.isSuccess()) {
                    // necessary e.g. to skip validation for resources that had issues when checked
                    completeResource.asObjectable().setFetchResult(objResult.createOperationResultType());
                    result.addSubresult(objResult);
                }
                newObjListType.add(completeResource);

                // TODO: what else do to with objResult??

            } catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException e) {
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
    private <T extends ObjectType> PrismObject<T> completeObject(
            Class<T> type, PrismObject<T> inObject, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

        if (ResourceType.class.equals(type)) {
            //noinspection unchecked
            return (PrismObject<T>) resourceManager.completeResource((PrismObject<ResourceType>) inObject,
                    SelectorOptions.findRootOptions(options), task, result);
        } else if (ShadowType.class.equals(type)) {
            throw new IllegalStateException(
                    "Should not happen, the shadow-related code is already in the ShadowCache");
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

        LOGGER.trace("Start of counting objects. Query:\n{}", DebugUtil.debugDumpLazily(query, 1));

        if (filter instanceof NoneFilter) {
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
            count = shadowsFacade.countObjects(query, task, result);

            result.computeStatus();
        } catch (ConfigurationException | CommunicationException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            throw e;
        } finally {
            result.cleanupResult();
        }

        LOGGER.trace("Finished counting objects: {}", count);
        return count;
    }

    @Override
    public <T extends ObjectType> String modifyObject(Class<T> type, String oid,
            Collection<? extends ItemDelta<?, ?>> modifications, OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
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

        LOGGER.trace("modifyObject: object modifications:\n{}", DebugUtil.debugDumpLazily(modifications));

        // getting object to modify
        PrismObject<T> repoShadow = getRepoObject(type, oid, null, result);

        LOGGER.trace("modifyObject: object to modify (repository):\n{}.", repoShadow.debugDumpLazily());

        try {

            if (ShadowType.class.isAssignableFrom(type)) {
                // calling shadow cache to modify object
                //noinspection unchecked
                oid = shadowsFacade.modifyShadow((PrismObject<ShadowType>) repoShadow,
                        modifications, scripts, options, task, result);
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

        //TODO: is critical when shadow does not exist anymore?? do we need to log it?? if not, change null to allowNotFound options
        PrismObject<T> object = getRepoObject(type, oid, null, result);
        LOGGER.trace("Object from repository to delete:\n{}", object.debugDumpLazily(1));

        PrismObject<T> deadShadow = null;

        if (object.canRepresent(ShadowType.class) && !ProvisioningOperationOptions.isRaw(options)) {

            try {

                //noinspection unchecked
                deadShadow = (PrismObject<T>) shadowsFacade.deleteShadow((PrismObject<ShadowType>) object, options, scripts, task, result);

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
                ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: expression error: " + e.getMessage(), e);
                throw e;
            } catch (PolicyViolationException | RuntimeException | Error e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: " + e.getMessage(), e);
                throw e;
            }

        } else if (object.canRepresent(ResourceType.class)) {

            resourceManager.deleteResource(oid, result);

        } else {

            try {
                cacheRepositoryService.deleteObject(type, oid, result);
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

    @Override
    public Object executeScript(String resourceOid, ProvisioningScriptType script, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Validate.notNull(resourceOid, "Oid of object for script execution must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".executeScript");
        result.addParam("oid", resourceOid);
        result.addArbitraryObjectAsParam("script", script);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        Object scriptResult;
        try {

            scriptResult = resourceManager.executeScript(resourceOid, script, task, result);

        } catch (CommunicationException | SchemaException | ConfigurationException | ExpressionEvaluationException | RuntimeException | Error e) {
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

    @Override
    public void refreshShadow(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        Validate.notNull(shadow, "Shadow for refresh must not be null.");
        OperationResult result = parentResult.createSubresult(OPERATION_REFRESH_SHADOW);

        LOGGER.debug("Refreshing shadow {}", shadow);

        try {

            shadowsFacade.refreshShadow(shadow, options, task, result);

        } catch (CommunicationException | SchemaException | ObjectNotFoundException | ConfigurationException | ExpressionEvaluationException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't refresh shadow: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
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

        LOGGER.trace("Start of (iterative) search objects. Query:\n{}", DebugUtil.debugDumpLazily(query, 1));

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
            LOGGER.trace("Finished searching. Nothing to do. Filter is NONE. Metadata: {}", metadata.shortDumpLazily());
            return metadata;
        }

        final GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

        SearchResultMetadata metadata;
        if (ShadowType.class.isAssignableFrom(type)) {

            try {

                metadata = shadowsFacade.searchObjectsIterative(query, options, (ResultHandler<ShadowType>) handler, task, result);

                result.computeStatus();
                result.cleanupResult();

            } catch (Throwable e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
                throw e;
            }

        } else {

            ResultHandler<T> internalHandler = (object, objResult) -> handleRepoObject(type, object, options, handler, task, objResult);

            Collection<SelectorOptions<GetOperationOptions>> repoOptions;
            if (GetOperationOptions.isReadOnly(rootOptions)) {
                repoOptions = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
            } else {
                repoOptions = null;
            }

            try {

                metadata = cacheRepositoryService.searchObjectsIterative(type, query, internalHandler, repoOptions, true, result);

                result.computeStatus();
                result.recordSuccessIfUnknown();
                result.cleanupResult();

            } catch (Throwable e) {
                ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
                throw e;
            }
        }

        LOGGER.trace("Finished searching. Metadata: {}", DebugUtil.shortDumpLazily(metadata));
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
            ProvisioningUtil.recordFatalError(LOGGER, result, "Discovery failed: " + ex.getMessage(), ex);
            throw ex;
        }

        result.computeStatus("Connector discovery failed");
        result.cleanupResult();
        return discoverConnectors;
    }

    @Override
    public List<ConnectorOperationalStatus> getConnectorOperationalStatus(String resourceOid, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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
            ProvisioningUtil.recordFatalError(LOGGER, result, "Getting operations status from connector for resource " + resourceOid + " failed: " + ex.getMessage(), ex);
            throw ex;
        }

        result.computeStatus();
        result.cleanupResult();
        return stats;
    }

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

            if (ShadowType.class.isAssignableFrom(delta.getObjectTypeClass())) {
                shadowsFacade.applyDefinition((ObjectDelta<ShadowType>) delta, (ShadowType) object, result);
            } else if (ResourceType.class.isAssignableFrom(delta.getObjectTypeClass())) {
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

            if (object.isOfType(ShadowType.class)) {
                shadowsFacade.applyDefinition((PrismObject<ShadowType>) object, result);
            } else if (object.isOfType(ResourceType.class)) {
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

            if (ShadowType.class.isAssignableFrom(type)) {
                shadowsFacade.applyDefinition(query, result);
            } else if (ResourceType.class.isAssignableFrom(type)) {
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
        provisioningDiag.setConnectorFrameworkVersion(connectorManager.getFrameworkVersion());
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
            checker.setCacheConfigurationManager(cacheConfigurationManager);
            checker.setShadowsFacade(shadowsFacade);
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

    @NotNull
    private <T extends ObjectType> PrismObject<T> getRepoObject(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        try {
            return cacheRepositoryService.getObject(type, oid, options, result);
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

        OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".compare");
        result.addParam(OperationResult.PARAM_OID, oid);
        result.addParam(OperationResult.PARAM_TYPE, type);

        ItemComparisonResult comparisonResult;
        try {

            PrismObject<O> repositoryObject = getRepoObject(type, oid, null, result);
            LOGGER.trace("Retrieved repository object:\n{}", repositoryObject.debugDumpLazily());

            //noinspection unchecked
            comparisonResult = shadowsFacade.compare((PrismObject<ShadowType>) (repositoryObject), path, expectedValue, task, result);

        } catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | EncryptionException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
            throw e;
        }

        result.computeStatus();
        result.cleanupResult();

        return comparisonResult;
    }

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        systemConfiguration = value;
    }

    @Override
    public SystemConfigurationType getSystemConfiguration() {
        return systemConfiguration;
    }

    @Override
    public void setResourceObjectClassifier(ResourceObjectClassifier classifier) {
        shadowsFacade.setResourceObjectClassifier(classifier);
    }
}
