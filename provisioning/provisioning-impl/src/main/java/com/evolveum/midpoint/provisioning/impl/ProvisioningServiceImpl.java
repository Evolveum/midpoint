/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.evolveum.midpoint.provisioning.impl.shadows.RepoShadowWithState.ShadowState;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowModifyOperation;
import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.provisioning.impl.operations.OperationsHelper;
import com.evolveum.midpoint.provisioning.impl.operations.ProvisioningGetOperation;
import com.evolveum.midpoint.provisioning.impl.operations.ProvisioningSearchLikeOperation;
import com.evolveum.midpoint.provisioning.impl.resources.ConnectorManager;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceManager;
import com.evolveum.midpoint.provisioning.impl.shadows.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;

import com.evolveum.midpoint.provisioning.impl.shadows.classification.ResourceObjectClassifier;
import com.evolveum.midpoint.provisioning.impl.shadows.classification.ShadowTagGenerator;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
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
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Implementation of provisioning service.
 *
 * It is just a "dispatcher" that routes interface calls to appropriate places.
 * E.g. the operations regarding resource definitions are routed directly to the
 * repository, operations of shadow objects are routed to the shadow cache and
 * so on.
 *
 * To keep the functionality transparent and maintainable, some of the processing is delegated to specialized
 * "operation" classes, like {@link ProvisioningGetOperation}, {@link ProvisioningSearchLikeOperation}, and so on.
 *
 * General responsibilities:
 *
 * * Creating and closing the operation-level {@link OperationResult} (and recording any exceptions in the standard manner).
 * More complex methods (e.g. `getObject`, `searchObjects`, `searchObjectsIterative`) may *clean up* the result before returning.
 * (Except for auxiliary methods like {@link #classifyResourceObject(ShadowType, ResourceType,
 * ObjectSynchronizationDiscriminatorType, Task, OperationResult)} and {@link ProvisioningService#generateShadowTag(ShadowType,
 * ResourceType, ResourceObjectDefinition, Task, OperationResult)}).
 *
 * * Logging at the operation level.
 *
 * @author Radovan Semancik
 */
@Service(value = "provisioningService")
@Primary
public class ProvisioningServiceImpl implements ProvisioningService, SystemConfigurationChangeListener {

    private static final String OP_GET_OBJECT = ProvisioningService.class.getName() + ".getObject";
    private static final String OP_SEARCH_OBJECTS = ProvisioningService.class.getName() + ".searchObjects";
    private static final String OP_COUNT_OBJECTS = ProvisioningService.class.getName() + ".countObjects";
    private static final String OP_REFRESH_SHADOW = ProvisioningServiceImpl.class.getName() + ".refreshShadow";
    private static final String OP_DELETE_OBJECT = ProvisioningService.class.getName() + ".deleteObject";
    private static final String OP_DISCOVER_CONFIGURATION = ProvisioningService.class.getName() + ".discoverConfiguration";
    private static final String OP_EXPAND_CONFIGURATION_OBJECT = ProvisioningService.class.getName()
            + ".expandConfigurationObject";
    // TODO reconsider names of these operations
    private static final String OP_TEST_RESOURCE = ProvisioningService.class.getName() + ".testResource";

    private static final String OP_GET_NATIVE_CAPABILITIES = ProvisioningService.class.getName() + ".getNativeCapabilities";

    @Autowired ShadowsFacade shadowsFacade;
    @Autowired ResourceManager resourceManager;
    @Autowired ConnectorManager connectorManager;
    @Autowired ProvisioningContextFactory ctxFactory;
    @Autowired PrismContext prismContext;
    @Autowired CacheConfigurationManager cacheConfigurationManager;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;
    @Autowired private LiveSynchronizer liveSynchronizer;
    @Autowired private AsyncUpdater asyncUpdater;
    @Autowired private CommonBeans beans;
    @Autowired private OperationsHelper operationsHelper;
    @Autowired private ResourceObjectClassifier resourceObjectClassifier;
    @Autowired private ShadowTagGenerator shadowTagGenerator;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    private volatile SynchronizationSorterEvaluator synchronizationSorterEvaluator;
    private volatile SystemConfigurationType systemConfiguration;

    private static final Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImpl.class);

    @Override
    public @NotNull <T extends ObjectType> PrismObject<T> getObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        Preconditions.checkNotNull(type, "type");
        Preconditions.checkNotNull(oid, "oid");
        Preconditions.checkNotNull(task, "task");
        Preconditions.checkNotNull(parentResult, "parentResult");

        ProvisioningGetOperation<T> operation = new ProvisioningGetOperation<>(type, oid, options, context, task, beans, operationsHelper);

        OperationResult result = parentResult.createSubresult(OP_GET_OBJECT);
        result.addParam(OperationResult.PARAM_OID, oid);
        result.addParam(OperationResult.PARAM_TYPE, type);
        result.addArbitraryObjectCollectionAsParam(OperationResult.PARAM_OPTIONS, options);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);
        T resultingObject;
        try {
            resultingObject = operation.execute(result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
            result.cleanup();
        }

        LOGGER.trace("Retrieved object {} with the status of {}", resultingObject, result.getStatus());
        //noinspection unchecked
        return (PrismObject<T>) storeFetchResultIfApplicable(resultingObject, result)
                .asPrismObject();
    }

    private static <T extends ObjectType> T storeFetchResultIfApplicable(T object, OperationResult result) {
        if (result.isSuccess()) {
            // Let's avoid storing SUCCESS result. This is an optimization in case of read-only object retrieval,
            // because it avoids cloning the object.
            return object;
        } else {
            // This must be done after the result is closed.
            //
            // There may be fetch result stored in the object by lower layers. We overwrite it. We assume that
            // this parent result contains its value as one of the children (hence the overwriting does not cause loss
            // of information).
            //
            //noinspection unchecked
            T clone = (T) object.asPrismObject().cloneIfImmutable().asObjectable();
            clone.setFetchResult(result.createBeanReduced());
            return clone;
        }
    }

    @Override
    public <T extends ObjectType> String addObject(
            @NotNull PrismObject<T> object,
            @Nullable OperationProvisioningScriptsType scripts,
            @Nullable ProvisioningOperationOptions options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectAlreadyExistsException, SchemaException, CommunicationException, ObjectNotFoundException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        Validate.notNull(object, "Object to add must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        if (InternalsConfig.encryptionChecks) {
            CryptoUtil.checkEncrypted(object);
        }

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".addObject");
        result.addParam("object", object);
        result.addArbitraryObjectAsParam("scripts", scripts);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);
        try {
            if (object.canRepresent(ShadowType.class)) {
                try {
                    String oid = shadowsFacade.addResourceObject(
                            (ShadowType) object.asObjectable(), scripts, options, context, task, result);
                    LOGGER.trace("Added shadow object {}", oid);
                    return oid;
                } catch (GenericFrameworkException ex) {
                    throw new ConfigurationException("Couldn't add object " + object + ": " + ex.getMessage(), ex);
                } catch (EncryptionException e) {
                    throw new SystemException(e.getMessage(), e);
                }
            } else {
                RepoAddOptions addOptions;
                if (ProvisioningOperationOptions.isOverwrite(options)) {
                    addOptions = RepoAddOptions.createOverwrite();
                } else {
                    addOptions = null;
                }
                return repositoryService.addObject(object, addOptions, result);
            }

        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
            result.cleanup();
        }
    }

    @Override
    public @NotNull SynchronizationResult synchronize(
            @NotNull ResourceOperationCoordinates coordinates,
            LiveSyncOptions options,
            @NotNull LiveSyncTokenStorage tokenStorage,
            @NotNull LiveSyncEventHandler handler,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {

        Validate.notNull(coordinates, "Coordinates must not be null.");
        String resourceOid = coordinates.getResourceOid();
        Validate.notNull(resourceOid, "Resource oid must not be null.");
        Validate.notNull(task, "Task must not be null.");
        Validate.notNull(tokenStorage, "Token storage must not be null.");
        Validate.notNull(handler, "Handler must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".synchronize");
        result.addParam(OperationResult.PARAM_OID, resourceOid);
        result.addParam(OperationResult.PARAM_TASK, task.toString());
        result.addArbitraryObjectAsParam(OperationResult.PARAM_OPTIONS, options);

        SynchronizationOperationResult liveSyncResult;

        try {
            // TODO avoid double fetching the resource
            PrismObject<ResourceType> resource = getObject(ResourceType.class, resourceOid, null, task, result);
            ResourceTypeUtil.checkNotInMaintenance(resource.asObjectable());

            LOGGER.debug("Start synchronization of {}", resource);
            liveSyncResult = liveSynchronizer.synchronize(coordinates, options, tokenStorage, handler, context, task, result);
            LOGGER.debug("Synchronization of {} done, result: {}", resource, liveSyncResult);

        } catch (ObjectNotFoundException | CommunicationException | SchemaException | SecurityViolationException |
                ConfigurationException | ExpressionEvaluationException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, null, e);
            result.summarize(true);
            throw e;
        } catch (ObjectAlreadyExistsException e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, null, e);
            result.summarize(true);
            throw new SystemException(e);
        } catch (GenericFrameworkException e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result,
                    "Synchronization error: generic connector framework error: " + e.getMessage(), e);
            result.summarize(true);
            throw new GenericConnectorException(e.getMessage(), e);
        }
        // TODO clean up the above exception and operation result processing

        return new SynchronizationResult(); // TODO
    }

    @Override
    public void processAsynchronousUpdates(@NotNull ResourceOperationCoordinates coordinates,
            @NotNull AsyncUpdateEventHandler handler, @NotNull Task task, @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        String resourceOid = coordinates.getResourceOid();
        Validate.notNull(resourceOid, "Resource oid must not be null.");

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".startListeningForAsyncUpdates");
        result.addParam(OperationResult.PARAM_OID, resourceOid);
        result.addParam(OperationResult.PARAM_TASK, task.toString());

        try {
            LOGGER.trace("Starting processing async updates for {}", coordinates);
            asyncUpdater.processAsynchronousUpdates(coordinates, handler, task, result);
            result.recordSuccess();
        } catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException | ExpressionEvaluationException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, null, e);
            throw e;
        } finally {
            // todo ok?
            result.summarize(true);
            result.cleanupResult();
        }
    }

    @NotNull
    @Override
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        Preconditions.checkNotNull(type, "type");
        Preconditions.checkNotNull(task, "task");
        Preconditions.checkNotNull(parentResult, "parentResult");

        LOGGER.trace("Start of (non-iterative) search objects. Query:\n{}", DebugUtil.debugDumpLazily(query, 1));

        OperationResult result = parentResult.createSubresult(OP_SEARCH_OBJECTS);
        result.addParam(OperationResult.PARAM_TYPE, type);
        result.addParam(OperationResult.PARAM_QUERY, query);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        try {
            SearchResultList<PrismObject<T>> objects =
                    new ProvisioningSearchLikeOperation<>(type, query, options, context, task, beans)
                            .executeSearch(result);

            LOGGER.trace("Finished searching. Metadata: {}", DebugUtil.shortDumpLazily(objects.getMetadata()));
            return objects;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
            result.cleanup();
        }
    }

    public <T extends ObjectType> Integer countObjects(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        Preconditions.checkNotNull(type, "type");
        Preconditions.checkNotNull(task, "task");
        Preconditions.checkNotNull(parentResult, "parentResult");

        LOGGER.trace("Start of counting objects. Query:\n{}", DebugUtil.debugDumpLazily(query, 1));

        OperationResult result = parentResult.createMinorSubresult(OP_COUNT_OBJECTS);
        result.addParam(OperationResult.PARAM_TYPE, type);
        result.addParam(OperationResult.PARAM_QUERY, query);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        try {
            Integer count =
                    new ProvisioningSearchLikeOperation<>(type, query, options, context, task, beans)
                            .executeCount(result);

            LOGGER.trace("Result of the counting: {}", count);
            result.addReturn(OperationResult.RETURN_COUNT, count);
            return count;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
            result.cleanup();
        }
    }

    @Override
    public <T extends ObjectType> String modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable OperationProvisioningScriptsType scripts,
            @Nullable ProvisioningOperationOptions options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
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

        OperationResult result = parentResult.subresult(ProvisioningService.class.getName() + ".modifyObject")
                .addArbitraryObjectCollectionAsParam("modifications", modifications)
                .addParam(OperationResult.PARAM_OID, oid)
                .addArbitraryObjectAsParam("scripts", scripts)
                .addArbitraryObjectAsParam("options", options)
                .addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class)
                .build();

        try {

            LOGGER.trace("modifyObject: object modifications:\n{}", DebugUtil.debugDumpLazily(modifications));

            T repoShadow = operationsHelper.getRepoObject(type, oid, null, result);
            LOGGER.trace("modifyObject: object to modify (repository):\n{}.", repoShadow.debugDumpLazily());

            if (ShadowType.class.isAssignableFrom(type)) {
                oid = ShadowModifyOperation.executeDirectly(
                        RawRepoShadow.of((ShadowType) repoShadow), modifications, scripts, options, context, task, result);
            } else {
                repositoryService.modifyObject(type, oid, modifications, result);
            }
            if (!result.isInProgress()) {
                // This is the case when there is already a conflicting pending operation.
                result.computeStatus();
            }

        } catch (CommunicationException | SchemaException | ObjectNotFoundException | ConfigurationException | SecurityViolationException
                | PolicyViolationException | ExpressionEvaluationException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, null, e);
            throw e;
        } catch (GenericFrameworkException e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, null, e);
            throw new CommunicationException(e.getMessage(), e);
        } catch (ObjectAlreadyExistsException e) {
            ProvisioningUtil.recordExceptionWhileRethrowing(LOGGER, result, "Couldn't modify object: object"
                    + " after modification would conflict with another existing object: " + e.getMessage(), e);
            throw e;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
            result.cleanup();
        }
        return oid;
    }

    @Override
    public <T extends ObjectType> PrismObject<T> deleteObject(Class<T> type, String oid, ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts, ProvisioningOperationContext context, Task task, OperationResult parentResult) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException {

        Validate.notNull(oid, "Oid of object to delete must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.trace("Start to delete object with oid {}", oid);

        OperationResult result = parentResult.subresult(OP_DELETE_OBJECT)
                .addParam("oid", oid)
                .addArbitraryObjectAsParam("scripts", scripts)
                .addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class)
                .build();
        try {

            // TODO: is it critical when shadow does not exist anymore? E.g. do we need to log it?
            //  If it's not critical, change null to allowNotFound options below.

            T object = operationsHelper.getRepoObject(type, oid, null, result);
            LOGGER.trace("Object from repository to delete:\n{}", object.debugDumpLazily(1));

            if (object instanceof ShadowType shadow && !ProvisioningOperationOptions.isRaw(options)) {
                return deleteShadow(RawRepoShadow.of(shadow), options, scripts, context, task, result);
            } else if (object instanceof ResourceType) {
                resourceManager.deleteResource(oid, result);
                return null;
            } else {
                repositoryService.deleteObject(type, oid, result);
                return null;
            }

        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
            result.cleanup();
        }
    }

    private <T extends ObjectType> PrismObject<T> deleteShadow(
            RawRepoShadow rawRepoShadow,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationContext context,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {

        try {

            //noinspection unchecked
            return (PrismObject<T>)
                    asPrismObject(
                            shadowsFacade.deleteShadow(rawRepoShadow, options, scripts, context, task, result));

            // TODO improve the error reporting. It is good that we want to provide some context for the error ("Couldn't delete
            //  object: ... problem: ...") but this is just too verbose in code. We need a better approach.
        } catch (CommunicationException e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, "Couldn't delete object: communication problem: " + e.getMessage(), e);
            throw e;
        } catch (GenericFrameworkException e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result,
                    "Couldn't delete object: generic error in the connector: " + e.getMessage(), e);
            throw new CommunicationException(e.getMessage(), e);
        } catch (SchemaException e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, "Couldn't delete object: schema problem: " + e.getMessage(), e);
            throw e;
        } catch (ConfigurationException e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, "Couldn't delete object: configuration problem: " + e.getMessage(), e);
            throw e;
        } catch (SecurityViolationException e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, "Couldn't delete object: security violation: " + e.getMessage(), e);
            throw e;
        } catch (ExpressionEvaluationException e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, "Couldn't delete object: expression error: " + e.getMessage(), e);
            throw e;
        } catch (EncryptionException e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, "Couldn't delete object: " + e.getMessage(), e);
            throw new SystemException(e.getMessage(), e);
        } catch (Throwable e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, "Couldn't delete object: " + e.getMessage(), e);
            throw e;
        }
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
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, null, e);
            throw e;
        }

        result.computeStatus();
        result.cleanupResult();

        return scriptResult;
    }

    @Override
    public @NotNull OperationResult testResource(
            @NotNull String resourceOid,
            @Nullable ResourceTestOptions options,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        Validate.notNull(resourceOid, "Resource OID to test is null.");

        OperationResult result = parentResult.subresult(OP_TEST_RESOURCE)
                .addParam("resourceOid", resourceOid)
                .addArbitraryObjectAsParam(OperationResult.PARAM_OPTIONS, options)
                .addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class)
                .build();
        try {
            ResourceType resource =
                    operationsHelper.getRepoObject(ResourceType.class, resourceOid, null, result);
            return testResourceInternal(resource, options, task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public @NotNull OperationResult testResource(
            @NotNull PrismObject<ResourceType> resource,
            @Nullable ResourceTestOptions options,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        OperationResult result = parentResult.subresult(OP_TEST_RESOURCE)
                .addParam(OperationResult.PARAM_RESOURCE, resource)
                .addArbitraryObjectAsParam(OperationResult.PARAM_OPTIONS, options)
                .addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class)
                .build();
        try {
            // The default for doing repository updates here is "false", even if the resource has an OID.
            // We update the repository object only if explicitly requested by the client.
            if (options == null) {
                options = new ResourceTestOptions().updateInRepository(false);
            } else if (options.isUpdateInRepository() == null) {
                options = options.updateInRepository(false);
            }

            options = options.doNotCacheConnector(); // MID-8020
            return testResourceInternal(resource.asObjectable(), options, task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private OperationResult testResourceInternal(
            @NotNull ResourceType resource, @Nullable ResourceTestOptions options, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        LOGGER.trace("Starting testing {}", resource);
        OperationResult testResult = resourceManager.testResource(resource, options, task, result);
        LOGGER.debug("Finished testing {}, result: {}", resource, testResult.getStatus());
        return testResult;
    }

    @Override
    public @NotNull DiscoveredConfiguration discoverConfiguration(
            @NotNull PrismObject<ResourceType> resource, @NotNull OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_DISCOVER_CONFIGURATION)
                .addParam("resource", resource)
                .addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class)
                .build();
        try {
            LOGGER.trace("Start discovering configuration of {}", resource);
            DiscoveredConfiguration discoveredConfiguration = resourceManager.discoverConfiguration(resource, result);
            LOGGER.debug("Finished discovering configuration of {}:\n{}",
                    resource, discoveredConfiguration.debugDumpLazily(1));
            return discoveredConfiguration;
        } catch (Exception e) {
            LOGGER.warn("Failed while discovering connector configuration: {}", e.getMessage(), e);
            result.recordFatalError("Failed while discovering connector configuration: " + e.getMessage(), e);
            return DiscoveredConfiguration.empty();
        } catch (Throwable t) {
            // This is more serious, like OutOfMemoryError and the like. We won't pretend it's OK.
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    public @Nullable BareResourceSchema fetchSchema(
            @NotNull PrismObject<ResourceType> resource, @NotNull OperationResult result) {
        LOGGER.trace("Fetching resource schema for {}", resource);
        try {
            return resourceManager.fetchSchema(resource.asObjectable(), result);
        } catch (Exception e) {
            LOGGER.warn("Failed while fetch schema: {}", e.getMessage(), e);
            result.recordFatalError("Failed while fetch schema: " + e.getMessage(), e);
            return null;
        } catch (Throwable t) {
            // This is more serious, like OutOfMemoryError and the like. We won't pretend it's OK.
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public void refreshShadow(
            @NotNull PrismObject<ShadowType> shadow,
            ProvisioningOperationOptions options,
            ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, EncryptionException {
        OperationResult result = parentResult.createSubresult(OP_REFRESH_SHADOW);

        LOGGER.debug("Refreshing shadow {}", shadow);

        try {

            shadowsFacade.refreshShadow(shadow.getOid(), options, context, task, result);

        } catch (CommonException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(
                    LOGGER, result,
                    "Couldn't refresh shadow: %s: %s".formatted(
                            e.getClass().getSimpleName(), e.getMessage()),
                    e);
            throw e;

        }

        result.computeStatus();
        result.cleanup();

        LOGGER.debug("Finished refreshing shadow {}: {}", shadow, result);
    }

    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ResultHandler<T> handler,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        Preconditions.checkNotNull(type, "type");
        Preconditions.checkNotNull(handler, "handler");
        Preconditions.checkNotNull(task, "task");
        Preconditions.checkNotNull(parentResult, "parentResult");

        LOGGER.trace("Start of (iterative) search objects. Query:\n{}", DebugUtil.debugDumpLazily(query, 1));

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".searchObjectsIterative");
        result.setSummarizeSuccesses(true);
        result.setSummarizeErrors(true);
        result.setSummarizePartialErrors(true);

        result.addParam(OperationResult.PARAM_TYPE, type);
        result.addParam(OperationResult.PARAM_QUERY, query);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

        try {
            SearchResultMetadata metadata =
                    new ProvisioningSearchLikeOperation<>(type, query, options, context, task, beans)
                            .executeIterativeSearch(handler, result);

            LOGGER.trace("Finished iterative searching. Metadata: {}", DebugUtil.shortDumpLazily(metadata));
            return metadata;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
            result.cleanup();
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
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, "Discovery failed: " + ex.getMessage(), ex);
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

        ResourceType resource;
        try {

            resource = resourceManager.getCompletedResource(resourceOid, null, task, result);

        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException ex) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, ex.getMessage(), ex);
            throw ex;
        }

        List<ConnectorOperationalStatus> stats;
        try {
            stats = resourceManager.getConnectorOperationalStatus(resource, result);
        } catch (Throwable ex) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(
                    LOGGER, result, "Getting operations status from connector for resource " + resourceOid + " failed: " + ex.getMessage(), ex);
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
                shadowsFacade.applyDefinition((ObjectDelta<ShadowType>) delta, (ShadowType) object, task, result);
            } else if (ResourceType.class.isAssignableFrom(delta.getObjectTypeClass())) {
                resourceManager.applyDefinition(
                        (ObjectDelta<ResourceType>) delta, (ResourceType) object, null, task, result);
            } else {
                throw new IllegalArgumentException("Could not apply definition to deltas for object type: " + delta.getObjectTypeClass());
            }

            result.recordSuccessIfUnknown();
            result.cleanupResult();

        } catch (Throwable e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, null, e);
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
                shadowsFacade.applyDefinition((PrismObject<ShadowType>) object, task, result);
            } else if (object.isOfType(ResourceType.class)) {
                resourceManager.applyDefinition((ResourceType) object.asObjectable(), result);
            } else {
                throw new IllegalArgumentException("Could not apply definition to object type: " + object.getCompileTimeClass());
            }

            result.computeStatus();
            result.recordSuccessIfUnknown();

        } catch (Throwable e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, null, e);
            throw e;
        } finally {
            result.cleanupResult();
        }
    }

    @Override
    public void determineShadowState(PrismObject<ShadowType> shadow, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".determineShadowState");
        result.addParam("shadow", shadow);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);
        try {
            ProvisioningContext ctx = ctxFactory.createForShadow(shadow, task, result);
            ctx.updateShadowState(shadow.asObjectable());
        } catch (Throwable e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, null, e);
            throw e;
        } finally {
            result.close();
            result.cleanup();
        }
    }

    @Override
    public void updateShadowMarksAndPolicies(PrismObject<ShadowType> shadow, boolean isNew, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {
        OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".updateShadowMarksAndPolicies");
        result.addParam("shadow", shadow);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);
        try {
            ProvisioningContext ctx = ctxFactory.createForShadow(shadow, task, result);
            ctx.computeAndUpdateEffectiveMarksAndPolicies(
                    AbstractShadow.of(shadow),
                    isNew ? ShadowState.TO_BE_CREATED : ShadowState.EXISTING,
                    result);
        } catch (Throwable e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, null, e);
            throw e;
        } finally {
            result.close();
            result.cleanup();
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
                shadowsFacade.applyDefinition(query, task, result);
            } else if (ResourceType.class.isAssignableFrom(type)) {
                resourceManager.applyDefinition(query, result); // beware: no implementation yet
            } else {
                throw new IllegalArgumentException("Could not apply definition to query for object type: " + type);
            }

            result.computeStatus();
            result.recordSuccessIfUnknown();

        } catch (Throwable e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, null, e);
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
        provisioningDiag.setConnectorFrameworkVersion(connectorManager.getConnIdFrameworkVersion());
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
    public ConstraintsCheckingResult checkConstraints(
            ResourceObjectDefinition objectDefinition,
            PrismObject<ShadowType> shadowObject,
            PrismObject<ShadowType> shadowObjectOld,
            ResourceType resource, String shadowOid,
            ConstraintViolationConfirmer constraintViolationConfirmer,
            ConstraintsCheckingStrategyType strategy,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws CommunicationException, SchemaException, SecurityViolationException, ConfigurationException,
            ObjectNotFoundException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".checkConstraints");
        try {
            ConstraintsChecker checker = new ConstraintsChecker();
            checker.setShadowObjectOld(shadowObjectOld);
            // "Whole class": we should not need it here. We do not invoke the search on resource here.
            ProvisioningContext ctx = ctxFactory.createForDefinition(resource, objectDefinition, null, task);
            ctx.setOperationContext(context);

            checker.setProvisioningContext(ctx);
            checker.setShadowObject(shadowObject);
            checker.setShadowOid(shadowOid);
            checker.setConstraintViolationConfirmer(constraintViolationConfirmer);
            checker.setStrategy(strategy);
            if (checker.canSkipChecking()) {
                return ConstraintsCheckingResult.createOk();
            } else {
                return checker.check(result);
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

    @Override
    public <O extends ObjectType, T> ItemComparisonResult compare(Class<O> type, String oid, ItemPath path,
            T expectedValue, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, EncryptionException {
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

            O repositoryObject = operationsHelper.getRepoObject(type, oid, null, result);
            LOGGER.trace("Retrieved repository object:\n{}", repositoryObject.debugDumpLazily());

            comparisonResult = shadowsFacade.compare((ShadowType) repositoryObject, path, expectedValue, task, result);

        } catch (Throwable t) {
            ProvisioningUtil.recordExceptionWhileRethrowing(LOGGER, result, null, t);
            throw t;
        } finally {
            result.close();
            result.cleanup();
        }
        return comparisonResult;
    }

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        systemConfiguration = value;
        resourceManager.updateSystemConfiguration(value);
    }

    @Override
    public SystemConfigurationType getSystemConfiguration() {
        return systemConfiguration;
    }

    @Override
    public void setSynchronizationSorterEvaluator(SynchronizationSorterEvaluator evaluator) {
        synchronizationSorterEvaluator = evaluator;
    }

    public @NotNull SynchronizationSorterEvaluator getSynchronizationSorterEvaluator() {
        return Objects.requireNonNullElse(
                synchronizationSorterEvaluator,
                NullSynchronizationSorterEvaluatorImpl.INSTANCE);
    }

    @Override
    public @NotNull ResourceObjectClassification classifyResourceObject(
            @NotNull ShadowType combinedObject,
            @NotNull ResourceType resource,
            @Nullable ObjectSynchronizationDiscriminatorType existingSorterResult,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return resourceObjectClassifier.classify(combinedObject, resource, existingSorterResult, task, result);
    }

    @Override
    public @Nullable String generateShadowTag(
            @NotNull ShadowType combinedObject,
            @NotNull ResourceType resource,
            @NotNull ResourceObjectDefinition definition,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        return shadowTagGenerator.generateTag(combinedObject, resource, definition, task, result);
    }

    @Override
    public void expandConfigurationObject(
            @NotNull PrismObject<? extends ObjectType> configurationObject,
            @NotNull Task task,
            @NotNull OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException {
        OperationResult result = parentResult.subresult(OP_EXPAND_CONFIGURATION_OBJECT)
                .addParam("configurationObject", configurationObject)
                .addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class)
                .build();
        try {
            ObjectType objectBean = configurationObject.asObjectable();
            if (objectBean instanceof ResourceType resource) {
                LOGGER.trace("Starting expanding {}", configurationObject);
                resourceManager.expandResource(resource, result);
                LOGGER.trace("Finished expanding {}", configurationObject);
            } else {
                // Nothing to do here; intentionally not throwing an exception
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public @NotNull CapabilityCollectionType getNativeCapabilities(@NotNull String connOid, OperationResult parentResult)
            throws SchemaException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        OperationResult result = parentResult.subresult(OP_GET_NATIVE_CAPABILITIES)
                .addParam("connectorOid", connOid)
                .addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class)
                .build();
        try {
            LOGGER.trace("Start getting native capabilities by connector oid {}", connOid);
            CapabilityCollectionType capabilities = resourceManager.getNativeCapabilities(connOid, result);
            LOGGER.debug("Finished getting native capabilities by connector oid {}:\n{}",
                    connOid, capabilities.debugDumpLazily(1));
            return capabilities;
        } catch (Throwable t) {
            // This is more serious, like OutOfMemoryError and the like. We won't pretend it's OK.
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public @Nullable ObjectOperationPolicyType getDefaultOperationPolicy(
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeIdentification typeIdentification,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var resource = getObject(ResourceType.class, resourceOid, null, task, result);
        var objectDef = ResourceSchemaFactory
                .getCompleteSchemaRequired(resource)
                .getObjectTypeDefinitionRequired(typeIdentification);
        return ObjectOperationPolicyHelper.get().getDefaultPolicyForResourceObjectType(objectDef, task.getExecutionMode(), result);
    }
}
