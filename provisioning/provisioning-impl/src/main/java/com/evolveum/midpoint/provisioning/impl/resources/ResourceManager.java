/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.*;
import static com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil.getShadowCachingDefaultPolicy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.schema.processor.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.DiscoveredConfiguration;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceTestOptions;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfExecutionContext;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;

@Component
public class ResourceManager {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceManager.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private ResourceCache resourceCache;
    @Autowired private ConnectorManager connectorManager;
    @Autowired private ResourceOperationalStateManager operationalStateManager;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired private CommonBeans beans;

    @Autowired ResourceSchemaHelper schemaHelper;
    @Autowired SchemaFetcher schemaFetcher;
    @Autowired ResourceConnectorsManager connectorSelector;

    /**
     * Used to detect changes in the default caching policy - for forced resource cache invalidation.
     *
     * @see #updateSystemConfiguration(SystemConfigurationType)
     */
    private ShadowCachingPolicyType lastKnownShadowCachingPolicy;

    /**
     * Gets (from cache) or completes a resource that has been just retrieved from the repository.
     *
     * If the up-to-date cached version of the resource is available, it is used immediately.
     * Otherwise, completion is requested.
     *
     * Options:
     *
     * - honored: `readOnly`, `noFetch`
     * - ignored: `raw` (We assume we are not called in this mode.)
     *
     * For requested processing, see {@link ProvisioningService#getObject(Class, String, Collection, Task, OperationResult)}.
     *
     * Typical use case: search operation.
     */
    public @NotNull ResourceType getCompletedResource(
            @NotNull ResourceType repositoryObject,
            @Nullable GetOperationOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, ConfigurationException {

        String oid = repositoryObject.getOid();
        boolean readonly = GetOperationOptions.isReadOnly(options);

        PrismObject<ResourceType> cachedResource = resourceCache.get(oid, repositoryObject.getVersion(), readonly);
        if (cachedResource != null) {
            LOGGER.trace("Returning resource from cache:\n{}", cachedResource.debugDumpLazily());
            return cachedResource.asObjectable();
        } else {
            return completeAndCacheResource(repositoryObject, options, task, result);
        }
    }

    /**
     * Gets (from cache) or gets (from repo) and completes a resource.
     *
     * If a cached version of the resource is available, it is used immediately.
     * Otherwise, resource is obtained from the repository, and its completion is requested.
     *
     * For more information please see {@link #getCompletedResource(ResourceType, GetOperationOptions, Task, OperationResult)}.
     *
     * Typical use case: get operation.
     */
    public @NotNull ResourceType getCompletedResource(
            @NotNull String oid,
            @Nullable GetOperationOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, ConfigurationException {
        boolean readonly = GetOperationOptions.isReadOnly(options);
        PrismObject<ResourceType> cachedResource = resourceCache.getIfLatest(oid, readonly, result);
        if (cachedResource != null) {
            LOGGER.trace("Returning resource from cache:\n{}", cachedResource.debugDumpLazily());
            return cachedResource.asObjectable();
        } else {
            // We must obviously NOT fetch resource from repo as read-only. We are going to modify it.
            ResourceType repositoryObject = readResourceFromRepository(oid, result);
            return completeAndCacheResource(repositoryObject, options, task, result);
        }
    }

    /** The processing is described in {@link ProvisioningService#getObject(Class, String, Collection, Task, OperationResult)}. */
    private @NotNull ResourceType completeAndCacheResource(
            @NotNull ResourceType repositoryObject,
            @Nullable GetOperationOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, ConfigurationException {

        logResourceBeforeCompletion(repositoryObject, options);

        ResourceCompletionOperation completionOperation = new ResourceCompletionOperation(repositoryObject, options, task, beans);
        ResourceType completedResource = completionOperation.execute(result);

        logResourceAfterCompletion(completedResource);

        // TODO fix this diagnostics using member methods of the completion operation
        if (isAbstract(completedResource)) {
            LOGGER.debug("Not putting {} into cache because it's abstract", completedResource);
        } else if (!ResourceTypeUtil.isComplete(completedResource)) {
            // No not cache non-complete resources (e.g. those retrieved with noFetch)
            LOGGER.debug("Not putting {} into cache because it's not complete: hasSchema={}, hasCapabilitiesCached={}",
                    repositoryObject, hasSchema(completedResource), hasCapabilitiesCached(completedResource));
        } else {
            OperationResultStatus completionStatus = completionOperation.getOperationResultStatus();
            if (completionStatus != OperationResultStatus.SUCCESS) {
                LOGGER.debug("Not putting {} into cache because the completeResource operation status is {}",
                        ObjectTypeUtil.toShortString(repositoryObject), completionStatus);
            } else {
                LOGGER.debug("Putting {} into cache", repositoryObject);
                // Cache only resources that are completely OK
                beans.resourceCache.put(completedResource, completionOperation.getAncestorsOids());
            }
        }
        return completedResource;
    }

    private void logResourceBeforeCompletion(
            @NotNull ResourceType repositoryObject, @Nullable GetOperationOptions options) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }
        if (isAbstract(repositoryObject)) {
            LOGGER.debug("Partially completing fetched abstract resource {}, version {}",
                    repositoryObject, repositoryObject.getVersion());
        } else {
            String oid = repositoryObject.getOid();
            LOGGER.debug("Completing and caching fetched resource {}, version {} to cache "
                            + "(previously cached version {}, options={})",
                    repositoryObject, repositoryObject.getVersion(), beans.resourceCache.getVersion(oid), options);
        }
    }

    private void logResourceAfterCompletion(ResourceType completedResource) {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }
        LOGGER.trace("Resource after completion, before (considering) putting into cache:\n{}", completedResource.debugDump());
        Element xsdSchemaElement = ResourceTypeUtil.getResourceXsdSchemaElement(completedResource);
        if (xsdSchemaElement == null) {
            LOGGER.trace("Schema: null");
        } else {
            LOGGER.trace("Schema:\n{}", DOMUtil.serializeDOMToString(xsdSchemaElement));
        }
    }

    @NotNull ResourceType readResourceFromRepository(String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        InternalMonitor.recordCount(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT);
        return repositoryService
                .getObject(ResourceType.class, oid, null, result)
                .asObjectable();
    }

    public void deleteResource(@NotNull String oid, OperationResult parentResult) throws ObjectNotFoundException {
        resourceCache.invalidateSingle(oid);
        repositoryService.deleteObject(ResourceType.class, oid, parentResult);
    }

    public SystemConfigurationType getSystemConfiguration() {
        return provisioningService.getSystemConfiguration();
    }

    /**
     * Tests the connection.
     *
     * @param resource Resource object. Must NOT be immutable!
     */
    public @NotNull OperationResult testResource(
            @NotNull ResourceType resource,
            @Nullable ResourceTestOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        expandResource(resource, result);
        return new ResourceTestOperation(resource, options, task, beans)
                .execute(result);
    }

    public void expandResource(@NotNull ResourceType resource, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        new ResourceExpansionOperation(resource, beans)
                .execute(result);
    }

    public @NotNull DiscoveredConfiguration discoverConfiguration(
            @NotNull PrismObject<ResourceType> resource,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {

        expandResource(resource.asObjectable(), result);

        ConnectorSpec connectorSpec = ConnectorSpec.main(resource.asObjectable());
        result.addParam(OperationResult.PARAM_NAME, connectorSpec.getConnectorName());
        result.addParam(OperationResult.PARAM_OID, connectorSpec.getConnectorOid());

        var connector = connectorManager.getNonProductionConnectorInstance(connectorSpec, result);
        return DiscoveredConfiguration.of(
                connector.discoverConfiguration(result));
    }

    public @NotNull CapabilityCollectionType getNativeCapabilities(@NotNull String connOid, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        try {
            return connectorManager
                    .getUnconfiguredConnectorInstance(connOid, result)
                    .getNativeCapabilities(result);
        } catch (GenericFrameworkException e) {
            // Not expected. Transform to system exception
            throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches the schema from the resource.
     *
     * The appropriate connector instance is not cached, as the configuration can differ from the "official" one.
     */
    public @Nullable BareResourceSchema fetchSchema(@NotNull ResourceType resource, @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, ObjectNotFoundException,
            SchemaException {
        LOGGER.trace("Fetching resource schema for {}", resource);
        var nativeSchema = schemaFetcher.fetchResourceSchema(resource, null, false, result);
        return nativeSchema != null ? ResourceSchemaFactory.nativeToBare(nativeSchema) : null;
    }

    /**
     * Modifies resource availability status in the repository (if needed).
     *
     * The necessity of status modification is determined against the current version of the resource - unless "skipGetResource"
     * is set. The resource is hopefully cached ResourceCache, so the performance impact should be almost ponone.
     *
     * Also note that in-memory representation of the resource is not modified. As a side effect, the cached resource
     * is invalidated because of the modification. But it will be loaded on the next occasion. This should be quite harmless,
     * as we do not expect availability changes to occur frequently.
     *
     * @param statusChangeReason Description of the reason of changing the availability status.
     * @param skipGetResource True if we want to skip "getResource" operation and therefore apply the change regardless of
     * the current resource availability status. This is to be used in situations where we expect that
     * the resource might not be successfully retrievable (e.g. if it's broken).
     * @throws ObjectNotFoundException If the resource object does not exist in repository.
     */
    public void modifyResourceAvailabilityStatus(String resourceOid, AvailabilityStatusType newStatus, String statusChangeReason,
            Task task, OperationResult result, boolean skipGetResource) throws ObjectNotFoundException {

        AvailabilityStatusType currentStatus;
        String resourceDesc;
        ResourceType resource;
        if (skipGetResource) {
            resource = null;
            currentStatus = null;
            resourceDesc = "resource " + resourceOid;
        } else {
            try {
                // TODO consider getting the resource in read-only mode
                resource = getCompletedResource(resourceOid, GetOperationOptions.createNoFetch(), task, result);
            } catch (ConfigurationException | SchemaException | ExpressionEvaluationException e) {
                // We actually do not expect any of these exceptions here. The resource is most probably in use
                throw SystemException.unexpected(e);
            }
            currentStatus = ResourceTypeUtil.getLastAvailabilityStatus(resource);
            resourceDesc = resource.toString();
        }

        if (newStatus != currentStatus && resource != null) {
            try {
                List<ItemDelta<?, ?>> modifications = operationalStateManager.createAndLogOperationalStateDeltas(
                        currentStatus, newStatus, resourceDesc, statusChangeReason, resource);
                repositoryService.modifyObject(ResourceType.class, resourceOid, modifications, result);
                InternalMonitor.recordCount(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT);
            } catch (SchemaException | ObjectAlreadyExistsException e) {
                throw SystemException.unexpected(e, "while recording operation state change");
            }
        }
    }

    public void modifyResourceAvailabilityStatus(
            ResourceType resource,
            AvailabilityStatusType newStatus,
            String statusChangeReason) {

        AvailabilityStatusType currentStatus = ResourceTypeUtil.getLastAvailabilityStatus(resource);
        String resourceDesc = resource.toString();

        if (newStatus != currentStatus) {
            OperationalStateType newState = operationalStateManager.createAndLogOperationalState(
                    currentStatus, newStatus, resourceDesc, statusChangeReason);
            resource.operationalState(newState);
        }
    }

    public void applyDefinition(
            ObjectDelta<ResourceType> delta,
            ResourceType resourceWhenNoOid,
            GetOperationOptions options,
            Task task,
            OperationResult objectResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException {
        schemaHelper.applyDefinition(delta, resourceWhenNoOid, options, task, objectResult);
    }

    /**
     * Applies a definition on a resource coming from the external client - i.e. it is a resource we know nothing about.
     * It may be e.g. unexpanded (deriving from a super-resource and not yet expanded).
     */
    public void applyDefinition(ResourceType resource, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, ConfigurationException {
        schemaHelper.applyConnectorSchemasToResource(resource, result);
    }

    public void applyDefinition(ObjectQuery query, OperationResult result) {
        // TODO: not implemented yet
    }

    public Object executeScript(String resourceOid, ProvisioningScriptType script, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ResourceType resource = getCompletedResource(resourceOid, null, task, result);
        ConnectorSpec connectorSpec = connectorSelector.selectConnectorRequired(resource, ScriptCapabilityType.class);
        try {
            ConnectorInstance connectorInstance =
                    connectorManager.getConfiguredAndInitializedConnectorInstance(connectorSpec, false, result);
            ExecuteProvisioningScriptOperation scriptOperation = ProvisioningUtil.convertToScriptOperation(script, "script on " + resource);
            UcfExecutionContext ucfCtx = new UcfExecutionContext(lightweightIdentifierGenerator, resource, task);
            ucfCtx.checkExecutionFullyPersistent();
            return connectorInstance.executeScript(scriptOperation, ucfCtx, result);
        } catch (GenericFrameworkException e) {
            // Not expected. Transform to system exception
            result.recordFatalError("Generic provisioning framework error", e);
            throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
        }
    }

    public List<ConnectorOperationalStatus> getConnectorOperationalStatus(ResourceType resource, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
        List<ConnectorOperationalStatus> statuses = new ArrayList<>();
        for (ConnectorSpec connectorSpec : ConnectorSpec.all(resource)) {
            ConnectorInstance connectorInstance =
                    connectorManager.getConfiguredAndInitializedConnectorInstance(connectorSpec, false, result);
            ConnectorOperationalStatus operationalStatus = connectorInstance.getOperationalStatus();
            if (operationalStatus != null) {
                operationalStatus.setConnectorName(connectorSpec.getConnectorName());
                statuses.add(operationalStatus);
            }
        }
        return statuses;
    }

    public <T extends CapabilityType> @NotNull ConnectorInstance getConfiguredConnectorInstance(
            ResourceType resource,
            Class<T> capabilityClass,
            boolean forceFresh,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        ConnectorSpec connectorSpec = connectorSelector.selectConnectorRequired(resource, capabilityClass);
        return connectorManager.getConfiguredAndInitializedConnectorInstance(connectorSpec, forceFresh, parentResult);
    }

    // Used by the tests. Does not change anything.
    @SuppressWarnings("SameParameterValue")
    @VisibleForTesting
    public <T extends CapabilityType> ConnectorInstance getConfiguredConnectorInstanceFromCache(
            ResourceType resource, Class<T> operationCapabilityClass) throws ConfigurationException {
        ConnectorSpec connectorSpec = connectorSelector.selectConnectorRequired(resource, operationCapabilityClass);
        return connectorManager.getConfiguredConnectorInstanceFromCache(connectorSpec);
    }

    /**
     * Invalidates all resources when the default caching policy is changed.
     *
     * This may seem harsh, but what are the alternatives?
     *
     * . Either we could avoid pre-computing the caching configuration for individual resource object definitions
     * (and attributes), which would mean that on each attribute access, the effective caching configuration would
     * need to be re-computed. We don't want to slow down the processing.
     *
     * . Or, we could try to select what specific resources to invalidate. But that would mean computing whether the change
     * of the default configuration affects the specific resource and its object classes and object types. This is not trivial,
     * and there's a possibility of having subtle bugs in this logic.
     *
     * Hence, we go with this simple yet (hopefully) effective solution. The assumption is that the default caching policy
     * is changed rarely.
     *
     * NOTE: As this method is invoked on the system startup, it's possible that there is an extra invalidation at that time.
     * But it should be harmless.
     */
    public synchronized void updateSystemConfiguration(@Nullable SystemConfigurationType newSystemConfiguration) {
        var newShadowCachingPolicy = getShadowCachingDefaultPolicy(newSystemConfiguration);
        if (!Objects.equals(lastKnownShadowCachingPolicy, newShadowCachingPolicy)) {
            resourceCache.invalidate(ResourceType.class, null, null);
        }
        lastKnownShadowCachingPolicy = newShadowCachingPolicy;
    }
}
