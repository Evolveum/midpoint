/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import static com.evolveum.midpoint.provisioning.impl.resources.ResourceCompletionOperation.*;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
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
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
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
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SchemaCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;

@Component
public class ResourceManager {

    @Autowired @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private ResourceCache resourceCache;
    @Autowired private ConnectorManager connectorManager;
    @Autowired private PrismContext prismContext;
    @Autowired private ResourceOperationalStateManager operationalStateManager;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired private CommonBeans beans;
    @Autowired private ResourceSchemaHelper schemaHelper;
    @Autowired private ResourceCapabilitiesHelper capabilitiesHelper;

    private static final Trace LOGGER = TraceManager.getTrace(ResourceManager.class);

    /**
     * Completes a resource that has been just retrieved from the repository, usually by a search operation.
     * (If the up-to-date cached version of the resource is available, it is used immediately.)
     */
    public @NotNull PrismObject<ResourceType> completeResource(
            @NotNull PrismObject<ResourceType> repositoryObject,
            @Nullable GetOperationOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, ConfigurationException {

        String oid = repositoryObject.getOid();
        boolean readonly = GetOperationOptions.isReadOnly(options);

        PrismObject<ResourceType> cachedResource = resourceCache.get(oid, repositoryObject.getVersion(), readonly);
        if (cachedResource != null) {
            LOGGER.trace("Returning resource from cache:\n{}", cachedResource.debugDumpLazily());
            return cachedResource;
        } else {
            return completeAndCacheResource(repositoryObject, options, task, result);
        }
    }

    /**
     * Gets a resource. We try the cache first. If it's not there, then we fetch, complete, and cache it.
     */
    @NotNull public PrismObject<ResourceType> getResource(
            @NotNull String oid, @Nullable GetOperationOptions options, @NotNull Task task, @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, ConfigurationException {
        boolean readonly = GetOperationOptions.isReadOnly(options);
        PrismObject<ResourceType> cachedResource = resourceCache.getIfLatest(oid, readonly, result);
        if (cachedResource != null) {
            LOGGER.trace("Returning resource from cache:\n{}", cachedResource.debugDumpLazily());
            return cachedResource;
        } else {
            // We must obviously NOT fetch resource from repo as read-only. We are going to modify it.
            PrismObject<ResourceType> repositoryObject = readResourceFromRepository(oid, result);
            return completeAndCacheResource(repositoryObject, options, task, result);
        }
    }

    private @NotNull PrismObject<ResourceType> completeAndCacheResource(
            @NotNull PrismObject<ResourceType> repositoryObject,
            @Nullable GetOperationOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, ConfigurationException {
        String oid = repositoryObject.getOid();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Completing and caching fetched resource {}, version {} to cache "
                            + "(previously cached version {}, options={})",
                    repositoryObject, repositoryObject.getVersion(), beans.resourceCache.getVersion(oid), options);
        }

        ResourceCompletionOperation completionOperation = new ResourceCompletionOperation(
                repositoryObject, options, null, false, null, task, beans);
        PrismObject<ResourceType> completedResource =
                completionOperation.execute(result);

        logResourceAfterCompletion(completedResource);

        // TODO fix this diagnostics using member methods of the completion operation
        if (!isComplete(completedResource)) {
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

    private void logResourceAfterCompletion(PrismObject<ResourceType> completedResource) {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }
        LOGGER.trace("Resource after completion, before putting into cache:\n{}", completedResource.debugDump());
        Element xsdSchemaElement = ResourceTypeUtil.getResourceXsdSchema(completedResource);
        if (xsdSchemaElement == null) {
            LOGGER.trace("Schema: null");
        } else {
            LOGGER.trace("Schema:\n{}",
                    DOMUtil.serializeDOMToString(ResourceTypeUtil.getResourceXsdSchema(completedResource)));
        }
    }

    @NotNull PrismObject<ResourceType> readResourceFromRepository(String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        InternalMonitor.recordCount(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT);
        return repositoryService.getObject(ResourceType.class, oid, null, parentResult);
    }

    public void deleteResource(@NotNull String oid, OperationResult parentResult) throws ObjectNotFoundException {
        resourceCache.invalidateSingle(oid);
        repositoryService.deleteObject(ResourceType.class, oid, parentResult);
    }

    public SystemConfigurationType getSystemConfiguration() {
        return provisioningService.getSystemConfiguration();
    }

    ResourceSchema fetchResourceSchema(
            PrismObject<ResourceType> resource,
            Map<String, Collection<Object>> capabilityMap,
            OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, ObjectNotFoundException,
            SchemaException {
        ConnectorSpec connectorSpec = selectConnectorSpec(resource, capabilityMap, SchemaCapabilityType.class);
        if (connectorSpec == null) {
            LOGGER.debug("No connector has schema capability, cannot fetch resource schema");
            return null;
        }
        InternalMonitor.recordCount(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        List<QName> generateObjectClasses = ResourceTypeUtil.getSchemaGenerationConstraints(resource);
        ConnectorInstance connectorInstance = connectorManager.getConfiguredConnectorInstance(connectorSpec, false, parentResult);

        LOGGER.debug("Trying to get schema from {}, objectClasses to generate: {}", connectorSpec, generateObjectClasses);
        ResourceSchema rawResourceSchema = connectorInstance.fetchResourceSchema(parentResult);

        if (ResourceTypeUtil.isValidateSchema(resource.asObjectable())) {
            ResourceTypeUtil.validateSchema(rawResourceSchema, resource);
        }
        return rawResourceSchema;
    }

    /**
     * Test the connection.
     *
     * @param resource Resource object as fetched from the repository. Must NOT be immutable!
     *
     * @throws ObjectNotFoundException If the resource object cannot be found in repository (e.g. when trying to set its
     *                                 availability status).
     */
    public void testConnection(PrismObject<ResourceType> resource, Task task, OperationResult parentResult)
            throws ObjectNotFoundException {
        new TestConnectionOperation(resource, task, beans)
                .execute(parentResult);
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
     *                        the current resource availability status. This is to be used in situations where we expect that
     *                        the resource might not be successfully retrievable (e.g. if it's broken).
     *
     * @throws ObjectNotFoundException If the resource object does not exist in repository.
     */
    public void modifyResourceAvailabilityStatus(String resourceOid, AvailabilityStatusType newStatus, String statusChangeReason,
            Task task, OperationResult result, boolean skipGetResource) throws ObjectNotFoundException {

        AvailabilityStatusType currentStatus;
        String resourceDesc;
        PrismObject<ResourceType> resource;
        if (skipGetResource) {
            resource = null;
            currentStatus = null;
            resourceDesc = "resource " + resourceOid;
        } else {
            try {
                resource = getResource(resourceOid, GetOperationOptions.createNoFetch(), task, result);
            } catch (ConfigurationException | SchemaException | ExpressionEvaluationException e) {
                // We actually do not expect any of these exceptions here. The resource is most probably in use
                result.recordFatalError("Unexpected exception: " + e.getMessage(), e);
                throw new SystemException("Unexpected exception: " + e.getMessage(), e);
            }
            ResourceType resourceBean = resource.asObjectable();
            currentStatus = ResourceTypeUtil.getLastAvailabilityStatus(resourceBean);
            resourceDesc = resource.toString();
        }

        if (newStatus != currentStatus) {
            try {
                List<ItemDelta<?, ?>> modifications = operationalStateManager.createAndLogOperationalStateDeltas(
                        currentStatus, newStatus, resourceDesc, statusChangeReason, resource);
                repositoryService.modifyObject(ResourceType.class, resourceOid, modifications, result);
                result.computeStatusIfUnknown();
                InternalMonitor.recordCount(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT);
            } catch (SchemaException | ObjectAlreadyExistsException e) {
                throw new SystemException("Unexpected exception while recording operation state change: " + e.getMessage(), e);
            }
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

    public void applyDefinition(PrismObject<ResourceType> resource, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, ConfigurationException {
        schemaHelper.applyConnectorSchemasToResource(resource, task, parentResult);
    }

    public void applyDefinition(ObjectQuery query, OperationResult result) {
        // TODO: not implemented yet
    }

    /**
     * Applies proper definition (connector schema) to the resource.
     */
    void applyConnectorSchemasToResource(PrismObject<ResourceType> resource, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException {
        schemaHelper.applyConnectorSchemasToResource(resource, task, result);
    }

    /**
     * Apply proper definition (connector schema) to the resource.
     */
    void applyConnectorSchemaToResource(ConnectorSpec connectorSpec, PrismObjectDefinition<ResourceType> resourceDefinition,
            PrismObject<ResourceType> resource, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        schemaHelper.applyConnectorSchemaToResource(connectorSpec, resourceDefinition, resource, task, result);
    }

    public Object executeScript(String resourceOid, ProvisioningScriptType script, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        PrismObject<ResourceType> resource = getResource(resourceOid, null, task, result);
        ConnectorSpec connectorSpec = selectConnectorSpec(resource, ScriptCapabilityType.class);
        if (connectorSpec == null) {
            throw new UnsupportedOperationException("No connector supports script capability");
        }
        ConnectorInstance connectorInstance = connectorManager.getConfiguredConnectorInstance(connectorSpec, false, result);
        ExecuteProvisioningScriptOperation scriptOperation = ProvisioningUtil.convertToScriptOperation(script, "script on "+resource, prismContext);
        try {
            UcfExecutionContext ucfCtx = new UcfExecutionContext(
                    lightweightIdentifierGenerator, resource.asObjectable(), task);
            return connectorInstance.executeScript(scriptOperation, ucfCtx, result);
        } catch (GenericFrameworkException e) {
            // Not expected. Transform to system exception
            result.recordFatalError("Generic provisioning framework error", e);
            throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
        }
    }

    public List<ConnectorOperationalStatus> getConnectorOperationalStatus(
            PrismObject<ResourceType> resource, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
        List<ConnectorOperationalStatus> statuses = new ArrayList<>();
        for (ConnectorSpec connectorSpec: getAllConnectorSpecs(resource)) {
            ConnectorInstance connectorInstance = connectorManager.getConfiguredConnectorInstance(connectorSpec, false, result);
            ConnectorOperationalStatus operationalStatus = connectorInstance.getOperationalStatus();
            if (operationalStatus != null) {
                operationalStatus.setConnectorName(connectorSpec.getConnectorName());
                statuses.add(operationalStatus);
            }
        }
        return statuses;
    }

    List<ConnectorSpec> getAllConnectorSpecs(PrismObject<ResourceType> resource) throws SchemaException, ConfigurationException {
        List<ConnectorSpec> connectorSpecs = new ArrayList<>();
        connectorSpecs.add(getDefaultConnectorSpec(resource));
        for (ConnectorInstanceSpecificationType additionalConnector: resource.asObjectable().getAdditionalConnector()) {
            connectorSpecs.add(getConnectorSpec(resource, additionalConnector));
        }
        return connectorSpecs;
    }

    // Should be used only internally (private). But it is public, because it is accessed from the tests.
    @VisibleForTesting
    public <T extends CapabilityType> ConnectorInstance getConfiguredConnectorInstance(
            PrismObject<ResourceType> resource,
            Class<T> operationCapabilityClass,
            boolean forceFresh,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        ConnectorSpec connectorSpec = selectConnectorSpec(resource, operationCapabilityClass);
        if (connectorSpec == null) {
            return null;
        }
        return connectorManager.getConfiguredConnectorInstance(connectorSpec, forceFresh, parentResult);
    }

    // Used by the tests. Does not change anything.
    @SuppressWarnings("SameParameterValue")
    @VisibleForTesting
    public <T extends CapabilityType> ConnectorInstance getConfiguredConnectorInstanceFromCache(
            PrismObject<ResourceType> resource, Class<T> operationCapabilityClass) throws SchemaException, ConfigurationException {
        ConnectorSpec connectorSpec = selectConnectorSpec(resource, operationCapabilityClass);
        return connectorSpec != null ? connectorManager.getConfiguredConnectorInstanceFromCache(connectorSpec) : null;
    }

    /**
     * Returns connector capabilities merged with capabilities defined at object type level.
     */
    public <T extends CapabilityType> CapabilitiesType getConnectorCapabilities(
            ResourceType resource, ResourceObjectTypeDefinition objectTypeDefinition, Class<T> operationCapabilityClass) {
        return capabilitiesHelper.getConnectorCapabilities(resource, objectTypeDefinition, operationCapabilityClass);
    }

    @SuppressWarnings("SameParameterValue")
    private <T extends CapabilityType> ConnectorSpec selectConnectorSpec(
            PrismObject<ResourceType> resource, Map<String,Collection<Object>> capabilityMap, Class<T> capabilityClass)
            throws SchemaException, ConfigurationException {
        if (capabilityMap == null) {
            return selectConnectorSpec(resource, capabilityClass);
        }
        for (ConnectorInstanceSpecificationType additionalConnectorType: resource.asObjectable().getAdditionalConnector()) {
            if (capabilitiesHelper.supportsCapability(
                    additionalConnectorType, capabilityMap.get(additionalConnectorType.getName()), capabilityClass)) {
                return getConnectorSpec(resource, additionalConnectorType);
            }
        }
        return getDefaultConnectorSpec(resource);
    }

    private <T extends CapabilityType> ConnectorSpec selectConnectorSpec(
            PrismObject<ResourceType> resource, Class<T> operationCapabilityClass) throws SchemaException, ConfigurationException {
        for (ConnectorInstanceSpecificationType additionalConnectorType: resource.asObjectable().getAdditionalConnector()) {
            if (capabilitiesHelper.supportsCapability(additionalConnectorType, operationCapabilityClass)) {
                return getConnectorSpec(resource, additionalConnectorType);
            }
        }
        return getDefaultConnectorSpec(resource);
    }

    ConnectorSpec getDefaultConnectorSpec(PrismObject<ResourceType> resource) {
        return new ConnectorSpec(
                resource,
                null,
                ResourceTypeUtil.getConnectorOid(resource),
                resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION));
    }

    ConnectorSpec getConnectorSpec(
            PrismObject<ResourceType> resource, ConnectorInstanceSpecificationType additionalConnectorSpecBean)
            throws ConfigurationException {
        String connectorName = additionalConnectorSpecBean.getName();
        configCheck(StringUtils.isNotBlank(connectorName), "No connector name in additional connector in %s", resource);

        // connector OID is not required here, as it may come from the super-resource
        String connectorOid = getConnectorOid(additionalConnectorSpecBean);

        //noinspection unchecked
        PrismContainer<ConnectorConfigurationType> connectorConfiguration =
                additionalConnectorSpecBean.asPrismContainerValue().findContainer(
                        ConnectorInstanceSpecificationType.F_CONNECTOR_CONFIGURATION);

        return new ConnectorSpec(resource, connectorName, connectorOid, connectorConfiguration);
    }

    private String getConnectorOid(@NotNull ConnectorInstanceSpecificationType bean) {
        ObjectReferenceType ref = bean.getConnectorRef();
        return ref != null ? ref.getOid() : null;
    }
}
