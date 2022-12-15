/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.ExpressionConstants;

import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.MutablePrismSchema;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SchemaCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import static com.evolveum.midpoint.prism.Referencable.getOid;

@Component
public class ResourceManager {

    @Autowired @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private ResourceCache resourceCache;
    @Autowired private ConnectorManager connectorManager;
    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ResourceOperationalStateManager operationalStateManager;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;

    private static final Trace LOGGER = TraceManager.getTrace(ResourceManager.class);

    private static final String OP_COMPLETE_RESOURCE = ResourceManager.class.getName() + ".completeResource";
    private static final String OP_APPLY_DEFINITION_TO_DELTA = ResourceManager.class + ".applyDefinitionToDelta";

    /**
     * Completes a resource that has been - we expect - just retrieved from the repository, usually by a search operation.
     */
    PrismObject<ResourceType> completeResource(PrismObject<ResourceType> repositoryObject, GetOperationOptions options, Task task,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        String oid = repositoryObject.getOid();
        boolean readonly = GetOperationOptions.isReadOnly(options);

        PrismObject<ResourceType> cachedResource = resourceCache.get(oid, repositoryObject.getVersion(), readonly);
        if (cachedResource != null) {
            return cachedResource;
        } else {
            LOGGER.debug("Storing fetched resource {}, version {} to cache (previously cached version {})",
                    oid, repositoryObject.getVersion(), resourceCache.getVersion(oid));
            PrismObject<ResourceType> mutableRepositoryObject = repositoryObject.cloneIfImmutable();
            return completeAndCacheResource(mutableRepositoryObject, options, task, parentResult);
        }
    }

    /**
     * Gets a resource.
     */
    @NotNull public PrismObject<ResourceType> getResource(String oid, GetOperationOptions options, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
        boolean readonly = GetOperationOptions.isReadOnly(options);
        PrismObject<ResourceType> cachedResource = resourceCache.getIfLatest(oid, readonly, parentResult);
        if (cachedResource != null) {
            LOGGER.trace("Returning resource from cache:\n{}", cachedResource.debugDumpLazily());
            return cachedResource;
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Fetching resource {} and storing to cache (previously cached version {}) (options={})",
                        oid, resourceCache.getVersion(oid), options);
            }
            // We must obviously NOT fetch resource from repo as read-only. We are going to modify it.
            PrismObject<ResourceType> repositoryObject = readResourceFromRepository(oid, parentResult);
            return completeAndCacheResource(repositoryObject, options, task, parentResult);
        }
    }

    @NotNull private PrismObject<ResourceType> readResourceFromRepository(String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        InternalMonitor.recordCount(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT);
        return repositoryService.getObject(ResourceType.class, oid, null, parentResult);
    }

    /**
     * Here we complete the resource and cache it.
     *
     * @param repositoryObject Up-to-date repository object. Must be mutable.
     */
    @NotNull private PrismObject<ResourceType> completeAndCacheResource(@NotNull PrismObject<ResourceType> repositoryObject,
            GetOperationOptions options, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        checkMutable(repositoryObject);

        PrismObject<ResourceType> completedResource = completeResourceInternal(repositoryObject, null,
                false, null, options, task, parentResult);

        logResourceAfterCompletion(completedResource);
        if (!isComplete(completedResource)) {
            // No not cache non-complete resources (e.g. those retrieved with noFetch)
            LOGGER.debug("Not putting {} into cache because it's not complete: hasSchema={}, hasCapabilitiesCached={}",
                    repositoryObject, hasSchema(completedResource), hasCapabilitiesCached(completedResource));
        } else {
            OperationResult completeResourceResult = parentResult.findSubresult(OP_COMPLETE_RESOURCE);
            if (!completeResourceResult.isSuccess()) {
                LOGGER.debug("Not putting {} into cache because the completeResource operation status is {}",
                        ObjectTypeUtil.toShortString(repositoryObject), completeResourceResult.getStatus());
            } else {
                LOGGER.debug("Putting {} into cache", repositoryObject);
                // Cache only resources that are completely OK
                resourceCache.put(completedResource);
            }
        }
        return completedResource;
    }

    private void logResourceAfterCompletion(PrismObject<ResourceType> completedResource) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resource after completion, before putting into cache:\n{}", completedResource.debugDump());
            Element xsdSchemaElement = ResourceTypeUtil.getResourceXsdSchema(completedResource);
            if (xsdSchemaElement == null) {
                LOGGER.trace("Schema: null");
            } else {
                LOGGER.trace("Schema:\n{}",
                        DOMUtil.serializeDOMToString(ResourceTypeUtil.getResourceXsdSchema(completedResource)));
            }
        }
    }

    void deleteResource(String oid, OperationResult parentResult) throws ObjectNotFoundException {
        resourceCache.remove(oid);
        repositoryService.deleteObject(ResourceType.class, oid, parentResult);
    }

    /**
     * Make sure that the resource is complete.
     *
     * It will check if the resource has a sufficiently fresh schema, etc.
     *
     * Returned resource may be the same or may be a different instance, but it
     * is guaranteed that it will be "fresher" and will correspond to the
     * repository state (assuming that the provided resource also corresponded
     * to the repository state).
     *
     * The connector schema that was fetched before can be supplied to this
     * method. This is just an optimization. It comes handy e.g. in test
     * connection case.
     *
     * Note: This is not really the best place for this method. Need to figure
     * out correct place later.
     *
     * @param repoResource
     *            Resource to check
     * @param resourceSchema
     *            schema that was freshly pre-fetched (or null)
     *
     * @return completed resource
     */
    @NotNull private PrismObject<ResourceType> completeResourceInternal(@NotNull PrismObject<ResourceType> repoResource,
            ResourceSchema resourceSchema, boolean fetchedSchema, Map<String,Collection<Object>> capabilityMap,
            GetOperationOptions options, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        checkMutable(repoResource);

        OperationResult result = parentResult.createMinorSubresult(OP_COMPLETE_RESOURCE);
        try {
            try {
                applyConnectorSchemasToResource(repoResource, task, result);
            } catch (Throwable t) {
                String message =
                        "An error occurred while applying connector schema to connector configuration of " + repoResource + ": "
                                + t.getMessage();
                result.recordPartialError(message, t);
                LOGGER.warn(message, t);
                return repoResource;
            }

            PrismObject<ResourceType> newResource;

            if (isComplete(repoResource)) {
                // The resource is complete.
                newResource = repoResource;

            } else {
                // The resource is NOT complete. Try to fetch schema and capabilities

                if (GetOperationOptions.isNoFetch(options)) {
                    // We need to fetch schema, but the noFetch option is specified. Therefore return whatever we have.
                    result.recordSuccessIfUnknown();
                    return repoResource;
                }

                try {

                    completeSchemaAndCapabilities(repoResource, resourceSchema, fetchedSchema, capabilityMap, result);

                } catch (Exception ex) {
                    // Catch the exceptions. There are not critical. We need to catch them all because the connector may
                    // throw even undocumented runtime exceptions.
                    // Even non-complete resource may still be usable. The fetchResult indicates that there was an error
                    result.recordPartialError("Cannot complete resource schema and capabilities: " + ex.getMessage(), ex);
                    return repoResource;
                }

                try {
                    // Now we need to re-read the resource from the repository and re-apply the schemas. This ensures that we will
                    // cache the correct version and that we avoid race conditions, etc.

                    newResource = readResourceFromRepository(repoResource.getOid(), result);
                    applyConnectorSchemasToResource(newResource, task, result);

                } catch (SchemaException | ObjectNotFoundException | RuntimeException e) {
                    result.recordFatalError(e);
                    throw e;
                }
            }

            try {
                // make sure it has parsed resource and refined schema. We are going to cache
                // it, so we want to cache it with the parsed schemas
                RefinedResourceSchemaImpl.getResourceSchema(newResource, prismContext);
                RefinedResourceSchemaImpl.getRefinedSchema(newResource);

            } catch (SchemaException e) {
                String message = "Schema error while processing schemaHandling section of " + newResource + ": " + e.getMessage();
                result.recordPartialError(message, e);
                LOGGER.warn(message, e);
                return newResource;
            } catch (RuntimeException e) {
                String message =
                        "Unexpected error while processing schemaHandling section of " + newResource + ": " + e.getMessage();
                result.recordPartialError(message, e);
                LOGGER.warn(message, e);
                return newResource;
            }

            result.recordSuccessIfUnknown();

            return newResource;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private boolean isComplete(PrismObject<ResourceType> resource) {
        return hasSchema(resource) && hasCapabilitiesCached(resource);
    }

    private boolean hasCapabilitiesCached(PrismObject<ResourceType> resource) {
        CapabilitiesType capabilities = resource.asObjectable().getCapabilities();
        return capabilities != null && capabilities.getCachingMetadata() != null;
    }

    private boolean hasSchema(PrismObject<ResourceType> resource) {
        return ResourceTypeUtil.getResourceXsdSchema(resource) != null;
    }

    private void completeSchemaAndCapabilities(PrismObject<ResourceType> resource, ResourceSchema resourceSchema, boolean fetchedSchema,
            Map<String, Collection<Object>> capabilityMap, OperationResult result)
                    throws SchemaException, CommunicationException, ObjectNotFoundException, GenericFrameworkException, ConfigurationException {

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();

        // Capabilities
        // we need to process capabilities first. Schema is one of the connector capabilities.
        // We need to determine this capability to select the right connector for schema retrieval.
        completeCapabilities(resource, capabilityMap != null, capabilityMap, modifications, result);

        if (resourceSchema == null) {
            // Try to get existing schema from resource. We do not want to override this if it exists
            // (but we still want to refresh the capabilities, that happens below)
            resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
        }

        if (resourceSchema == null || resourceSchema.isEmpty()) {

            LOGGER.trace("Fetching resource schema for {}", resource);

            resourceSchema = fetchResourceSchema(resource, capabilityMap, result);

            if (resourceSchema == null) {
                LOGGER.warn("No resource schema fetched from {}", resource);
            } else if (resourceSchema.isEmpty()) {
                LOGGER.warn("Empty resource schema fetched from {}", resource);
            } else {
                LOGGER.debug("Fetched resource schema for {}: {} definitions", resource, resourceSchema.getDefinitions().size());
                fetchedSchema = true;
            }
        }

        if (resourceSchema != null) {
            if (fetchedSchema) {
                adjustSchemaForSimulatedCapabilities(resource, resourceSchema);
                modifications.add(createSchemaUpdateDelta(resource, resourceSchema));
                AvailabilityStatusType previousStatus = ResourceTypeUtil.getLastAvailabilityStatus(resource.asObjectable());
                if (previousStatus != AvailabilityStatusType.UP) {
                    modifications.addAll(operationalStateManager.createAndLogOperationalStateDeltas(previousStatus,
                            AvailabilityStatusType.UP, resource.toString(),
                            "resource schema was successfully fetched", resource));
                } else {
                    // just for sure (if the status changed in the meanwhile)
                    modifications.add(operationalStateManager.createAvailabilityStatusDelta(AvailabilityStatusType.UP));
                }
            } else {
                CachingMetadataType schemaCachingMetadata = resource.asObjectable().getSchema().getCachingMetadata();
                if (schemaCachingMetadata == null) {
                    schemaCachingMetadata = MiscSchemaUtil.generateCachingMetadata();
                    modifications.add(
                            prismContext.deltaFactory().property().createModificationReplaceProperty(
                                    ItemPath.create(ResourceType.F_SCHEMA, CapabilitiesType.F_CACHING_METADATA),
                                    resource.getDefinition(),
                                    schemaCachingMetadata)
                    );
                }
            }
        }

        if (!modifications.isEmpty()) {
            try {
                LOGGER.trace("Completing {}:\n{}", resource, DebugUtil.debugDumpLazily(modifications, 1));
                repositoryService.modifyObject(ResourceType.class, resource.getOid(), modifications, result);
                InternalMonitor.recordCount(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT);
            } catch (ObjectAlreadyExistsException ex) {
                // This should not happen
                throw new SystemException(ex);
            }
        }
    }

    private void completeCapabilities(PrismObject<ResourceType> resource, boolean forceRefresh, Map<String,Collection<Object>> capabilityMap, Collection<ItemDelta<?, ?>> modifications,
            OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        ResourceType resourceType = resource.asObjectable();
        ConnectorSpec defaultConnectorSpec = getDefaultConnectorSpec(resource);
        CapabilitiesType resourceCapType = resourceType.getCapabilities();
        if (resourceCapType == null) {
            resourceCapType = new CapabilitiesType();
            resourceType.setCapabilities(resourceCapType);
        }
        completeConnectorCapabilities(defaultConnectorSpec, resourceCapType, ResourceType.F_CAPABILITIES, forceRefresh,
                capabilityMap==null?null:capabilityMap.get(null),
                modifications, result);

        for (ConnectorInstanceSpecificationType additionalConnectorType: resource.asObjectable().getAdditionalConnector()) {
            ConnectorSpec connectorSpec = getConnectorSpec(resource, additionalConnectorType);
            CapabilitiesType connectorCapType = additionalConnectorType.getCapabilities();
            if (connectorCapType == null) {
                connectorCapType = new CapabilitiesType();
                additionalConnectorType.setCapabilities(connectorCapType);
            }
            ItemPath itemPath = additionalConnectorType.asPrismContainerValue().getPath().append(ConnectorInstanceSpecificationType.F_CAPABILITIES);
            completeConnectorCapabilities(connectorSpec, connectorCapType, itemPath, forceRefresh,
                    capabilityMap==null?null:capabilityMap.get(additionalConnectorType.getName()),
                    modifications, result);
        }
    }

    private void completeConnectorCapabilities(ConnectorSpec connectorSpec, CapabilitiesType capType, ItemPath itemPath, boolean forceRefresh,
            Collection<Object> retrievedCapabilities, Collection<ItemDelta<?, ?>> modifications, OperationResult result)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {

        if (capType.getNative() != null && !capType.getNative().getAny().isEmpty()) {
            if (!forceRefresh) {
                CachingMetadataType cachingMetadata = capType.getCachingMetadata();
                if (cachingMetadata == null) {
                    cachingMetadata = MiscSchemaUtil.generateCachingMetadata();
                    modifications.add(
                            prismContext.deltaFactory().property().createModificationReplaceProperty(
                                ItemPath.create(ResourceType.F_CAPABILITIES, CapabilitiesType.F_CACHING_METADATA),
                                connectorSpec.getResource().asPrismObject().getDefinition(),
                                cachingMetadata)
                        );
                }
                return;
            }
        }

        if (retrievedCapabilities == null) {
            try {

                InternalMonitor.recordCount(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);

                ConnectorInstance connector = connectorManager.getConfiguredConnectorInstance(connectorSpec, false, result);
                retrievedCapabilities = connector.fetchCapabilities(result);

            } catch (GenericFrameworkException e) {
                throw new GenericConnectorException("Generic error in connector " + connectorSpec + ": "
                        + e.getMessage(), e);
            }
        }

        CapabilityCollectionType nativeCapType = new CapabilityCollectionType();
        capType.setNative(nativeCapType);
        nativeCapType.getAny().addAll(retrievedCapabilities);

        CachingMetadataType cachingMetadata = MiscSchemaUtil.generateCachingMetadata();
        capType.setCachingMetadata(cachingMetadata);

        //noinspection unchecked
        ObjectDelta<ResourceType> capabilitiesReplaceDelta = prismContext.deltaFactory().object()
                .createModificationReplaceContainer(ResourceType.class, connectorSpec.getResource().getOid(),
                itemPath, capType.asPrismContainerValue().clone());

        modifications.addAll(capabilitiesReplaceDelta.getModifications());
    }

    private ContainerDelta<XmlSchemaType> createSchemaUpdateDelta(PrismObject<ResourceType> resource, ResourceSchema resourceSchema) throws SchemaException {
        Document xsdDoc;
        try {
            xsdDoc = resourceSchema.serializeToXsd();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Serialized XSD resource schema for {}:\n{}", resource, DOMUtil.serializeDOMToString(xsdDoc));
            }
        } catch (SchemaException e) {
            throw new SchemaException("Error processing resource schema for " + resource + ": " + e.getMessage(), e);
        }

        Element xsdElement = DOMUtil.getFirstChildElement(xsdDoc);
        if (xsdElement == null) {
            throw new SchemaException("No schema was generated for " + resource);
        }
        CachingMetadataType cachingMetadata = MiscSchemaUtil.generateCachingMetadata();

        ContainerDelta<XmlSchemaType> schemaContainerDelta = prismContext.deltaFactory().container().createDelta(
                ResourceType.F_SCHEMA, ResourceType.class);
        PrismContainerValue<XmlSchemaType> cval = prismContext.itemFactory().createContainerValue();
        schemaContainerDelta.setValueToReplace(cval);
        PrismProperty<CachingMetadataType> cachingMetadataProperty = cval
                .createProperty(XmlSchemaType.F_CACHING_METADATA);
        cachingMetadataProperty.setRealValue(cachingMetadata);
        List<QName> objectClasses = ResourceTypeUtil.getSchemaGenerationConstraints(resource);
        if (objectClasses != null) {
            PrismProperty<SchemaGenerationConstraintsType> generationConstraints = cval
                    .createProperty(XmlSchemaType.F_GENERATION_CONSTRAINTS);
            SchemaGenerationConstraintsType constraints = new SchemaGenerationConstraintsType();
            constraints.getGenerateObjectClass().addAll(objectClasses);
            generationConstraints.setRealValue(constraints);
        }
        PrismProperty<SchemaDefinitionType> definitionProperty = cval.createProperty(XmlSchemaType.F_DEFINITION);
        ObjectTypeUtil.setXsdSchemaDefinition(definitionProperty, xsdElement);

        return schemaContainerDelta;
    }

    /**
     * Apply proper definition (connector schema) to the resource.
     */
    private void applyConnectorSchemasToResource(PrismObject<ResourceType> resource, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
        checkMutable(resource);
        PrismObjectDefinition<ResourceType> newResourceDefinition = resource.getDefinition().clone();
        for (ConnectorSpec connectorSpec : getAllConnectorSpecs(resource)) {
            try {
                applyConnectorSchemaToResource(connectorSpec, newResourceDefinition, resource, task, result);
            } catch (CommunicationException | ConfigurationException | SecurityViolationException e) {
                throw new IllegalStateException("Unexpected exception: " + e.getMessage(), e);      // fixme temporary solution
            }
        }
        newResourceDefinition.freeze();
        resource.setDefinition(newResourceDefinition);
    }

    /**
     * Apply proper definition (connector schema) to the resource.
     */
    private void applyConnectorSchemaToResource(ConnectorSpec connectorSpec, PrismObjectDefinition<ResourceType> resourceDefinition,
            PrismObject<ResourceType> resource, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        ConnectorType connectorType = connectorManager.getConnector(connectorSpec, result);
        PrismSchema connectorSchema = connectorManager.getAttachedConnectorSchema(connectorType);
        PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefinition = ConnectorTypeUtil
                .findConfigurationContainerDefinition(connectorType, connectorSchema);
        if (configurationContainerDefinition == null) {
            throw new SchemaException("No configuration container definition in schema of " + connectorType);
        }

        configurationContainerDefinition = configurationContainerDefinition.clone();
        PrismContainer<ConnectorConfigurationType> configurationContainer = connectorSpec.getConnectorConfiguration();
        // We want element name, minOccurs/maxOccurs and similar definition to be taken from the original, not the schema
        // the element is global in the connector schema. therefore it does not have correct maxOccurs
        if (configurationContainer != null) {
            configurationContainerDefinition.adoptElementDefinitionFrom(configurationContainer.getDefinition());
            configurationContainer.applyDefinition(configurationContainerDefinition, true);

            try {
                //noinspection unchecked
                configurationContainer.accept(visitable -> {
                    if ((visitable instanceof PrismProperty<?>)) {
                        try {
                            evaluateExpression((PrismProperty<?>)visitable, resource, task, result);
                        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
                            throw new TunnelException(e);
                        }
                    }
                    // TODO treat configuration items that are containers themselves
                });
            } catch (TunnelException te) {
                Throwable e = te.getCause();
                if (e instanceof SchemaException) {
                    throw (SchemaException)e;
                } else if (e instanceof ObjectNotFoundException) {
                    throw (ObjectNotFoundException)e;
                } else if (e instanceof ExpressionEvaluationException) {
                    throw (ExpressionEvaluationException)e;
                } else if (e instanceof CommunicationException) {
                    throw (CommunicationException)e;
                } else if (e instanceof ConfigurationException) {
                    throw (ConfigurationException)e;
                } else if (e instanceof SecurityViolationException) {
                    throw (SecurityViolationException)e;
                } else if (e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                } else if (e instanceof Error) {
                    throw (Error)e;
                } else {
                    throw new SystemException(e);
                }
            }

        } else {
            configurationContainerDefinition.adoptElementDefinitionFrom(
                    resourceDefinition.findContainerDefinition(ResourceType.F_CONNECTOR_CONFIGURATION));
        }

        if (connectorSpec.getConnectorName() == null) {
            // Default connector, for compatibility
            // It does not make sense to update this for any other connectors.
            // We cannot have one definition for additionalConnector[1]/connectorConfiguration and
            // different definition for additionalConnector[2]/connectorConfiguration in the object definition.
            // The way to go is to set up definitions on the container level.
            resourceDefinition.replaceDefinition(ResourceType.F_CONNECTOR_CONFIGURATION, configurationContainerDefinition);
        }
    }

    private <T> void evaluateExpression(PrismProperty<T> configurationProperty, PrismObject<ResourceType> resource, Task task,
            OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        PrismPropertyDefinition<T> propDef = configurationProperty.getDefinition();
        String shortDesc = "connector configuration property "+configurationProperty+" in "+resource;
        List<PrismPropertyValue<T>> extraValues = new ArrayList<>();
        for (PrismPropertyValue<T> configurationPropertyValue: configurationProperty.getValues()) {
            ExpressionWrapper expressionWrapper = configurationPropertyValue.getExpression();
            if (expressionWrapper == null) {
                return;
            }
            Object expressionObject = expressionWrapper.getExpression();
            if (!(expressionObject instanceof ExpressionType)) {
                throw new IllegalStateException("Expected that expression in "+configurationPropertyValue+" will be ExpressionType, but it was "+expressionObject);
            }
            ExpressionType expressionType = (ExpressionType) expressionWrapper.getExpression();

            Expression<PrismPropertyValue<T>, PrismPropertyDefinition<T>> expression = expressionFactory.makeExpression(expressionType, propDef, MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);
            VariablesMap variables = new VariablesMap();

            SystemConfigurationType systemConfiguration = getSystemConfiguration();
            variables.put(ExpressionConstants.VAR_CONFIGURATION, PrismObject.asPrismObject(systemConfiguration),
                    SystemConfigurationType.class);
            variables.put(ExpressionConstants.VAR_RESOURCE, resource, ResourceType.class);

            // TODO: are there other variables that should be populated?

            ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(null, variables, shortDesc, task);
            PrismValueDeltaSetTriple<PrismPropertyValue<T>> expressionOutputTriple = expression.evaluate(expressionContext,
                    result);
            Collection<PrismPropertyValue<T>> expressionOutputValues = expressionOutputTriple.getNonNegativeValues();
            if (!expressionOutputValues.isEmpty()) {
                Iterator<PrismPropertyValue<T>> iterator = expressionOutputValues.iterator();
                PrismPropertyValue<T> firstValue = iterator.next();
                configurationPropertyValue.setValue(firstValue.getValue());
                while (iterator.hasNext()) {
                    extraValues.add(iterator.next());
                }
            }
        }
        for (PrismPropertyValue<T> extraValue: extraValues) {
            configurationProperty.add(extraValue);
        }
    }

    public SystemConfigurationType getSystemConfiguration() {
        return provisioningService.getSystemConfiguration();
    }

    private ResourceSchema fetchResourceSchema(PrismObject<ResourceType> resource, Map<String, Collection<Object>> capabilityMap, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, ObjectNotFoundException, SchemaException {
        ConnectorSpec connectorSpec = selectConnectorSpec(resource, capabilityMap, SchemaCapabilityType.class);
        if (connectorSpec == null) {
            LOGGER.debug("No connector has schema capability, cannot fetch resource schema");
            return null;
        }
        InternalMonitor.recordCount(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        List<QName> generateObjectClasses = ResourceTypeUtil.getSchemaGenerationConstraints(resource);
        ConnectorInstance connectorInstance = connectorManager.getConfiguredConnectorInstance(connectorSpec, false, parentResult);

        LOGGER.debug("Trying to get schema from {}, objectClasses to generate: {}", connectorSpec, generateObjectClasses);
        ResourceSchema resourceSchema = connectorInstance.fetchResourceSchema(parentResult);

        if (ResourceTypeUtil.isValidateSchema(resource.asObjectable())) {
            ResourceTypeUtil.validateSchema(resourceSchema, resource);
        }
        return resourceSchema;

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

        checkMutable(resource);

        String resourceOid = resource.getOid();

        String operationDesc = "test resource " + resourceOid + "connection";

        List<ConnectorSpec> allConnectorSpecs;
        try {
            allConnectorSpecs = getAllConnectorSpecs(resource);
        } catch (SchemaException e) {
            String statusChangeReason = operationDesc + ", getting all connectors failed: " + e.getMessage();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.error("Configuration error: {}", e.getMessage(), e);
            }
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, statusChangeReason, task, parentResult, true);
            parentResult.recordFatalError("Configuration error: " + e.getMessage(), e);
            return;
        }

        Map<String,Collection<Object>> capabilityMap = new HashMap<>();
        for (ConnectorSpec connectorSpec: allConnectorSpecs) {

            OperationResult connectorTestResult = parentResult
                    .createSubresult(ConnectorTestOperation.CONNECTOR_TEST.getOperation());
            connectorTestResult.addParam(OperationResult.PARAM_NAME, connectorSpec.getConnectorName());
            connectorTestResult.addParam(OperationResult.PARAM_OID, connectorSpec.getConnectorOid());

            testConnectionConnector(connectorSpec, capabilityMap, task, connectorTestResult);

            connectorTestResult.computeStatus();

            if (!connectorTestResult.isAcceptable()) {
                //nothing more to do.. if it failed while testing connection, status is set.
                // we do not need to continue and waste the time.
                return;
            }
        }

        // === test SCHEMA ===

        OperationResult schemaResult = parentResult.createSubresult(ConnectorTestOperation.RESOURCE_SCHEMA.getOperation());

        ResourceSchema schema;
        try {

            schema = fetchResourceSchema(resource, capabilityMap, schemaResult);

        } catch (CommunicationException e) {
            String statusChangeReason = operationDesc + " failed while fetching schema: " + e.getMessage();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.error("Communication error: {}", e.getMessage(), e);
            }
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.DOWN, statusChangeReason, task, parentResult, true);
            schemaResult.recordFatalError("Communication error: " + e.getMessage(), e);
            return;
        } catch (GenericFrameworkException | ConfigurationException | ObjectNotFoundException | SchemaException | RuntimeException e) {
            String statusChangeReason = operationDesc + " failed while fetching schema: " + e.getMessage();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.error("Error: {}", e.getMessage(), e);
            }
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, statusChangeReason, task, parentResult, true);
            schemaResult.recordFatalError("Error: " + e.getMessage(), e);
            return;
        }

        if (schema == null || schema.isEmpty()) {
            // Resource does not support schema
            // If there is a static schema in resource definition this may still be OK
            try {
                schema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
            } catch (SchemaException e) {
                String statusChangeReason = operationDesc + " failed while parsing refined schema: " + e.getMessage();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.error("Error: {}", e.getMessage(), e);
                }
                modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, statusChangeReason, task, parentResult, true);
                schemaResult.recordFatalError(e);
                return;
            }

            if (schema == null || schema.isEmpty()) {
                String msg = "Connector does not support schema and no static schema available";
                String statusChangeReason = operationDesc + ". " + msg;
                modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, statusChangeReason, task, parentResult, true);
                schemaResult.recordFatalError(msg);
                return;
            }
        }

        // Invoke completeResource(). This will store the fetched schema to the ResourceType if there is no <schema>
        // definition already. Therefore the testResource() can be used to generate the resource schema - until we
        // have full schema caching capability.
        PrismObject<ResourceType> completedResource;
        try {
            // Re-fetching from repository to get up-to-date availability status (to avoid phantom state change records).
            PrismObject<ResourceType> repoResource = repositoryService.getObject(ResourceType.class, resourceOid, null, schemaResult);
            completedResource = completeResourceInternal(repoResource, schema, true, capabilityMap, null, task, schemaResult);
        } catch (ObjectNotFoundException e) {
            String msg = "Object not found (unexpected error, probably a bug): " + e.getMessage();
            String statusChangeReason = operationDesc + " failed while completing resource. " + msg;
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, statusChangeReason, task, parentResult, true);
            schemaResult.recordFatalError(msg, e);
            return;
        } catch (SchemaException e) {
            String msg = "Schema processing error (probably connector bug): " + e.getMessage();
            String statusChangeReason = operationDesc + " failed while completing resource. " + msg;
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, statusChangeReason, task, parentResult, true);
            schemaResult.recordFatalError(msg, e);
            return;
        } catch (ExpressionEvaluationException e) {
            String msg = "Expression error: " + e.getMessage();
            String statusChangeReason = operationDesc + " failed while completing resource. " + msg;
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, statusChangeReason, task, parentResult, true);
            schemaResult.recordFatalError(msg, e);
            return;
        } catch (RuntimeException e) {
            String msg = "Unspecified exception: " + e.getMessage();
            String statusChangeReason = operationDesc + " failed while completing resource. " + msg;
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, statusChangeReason, task, parentResult, true);
            schemaResult.recordFatalError(msg, e);
            return;
        }

        schemaResult.recordSuccess();

        try {
            updateResourceSchema(allConnectorSpecs, parentResult, completedResource);
        } catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | RuntimeException e) {
            String statusChangeReason = operationDesc + " failed while updating resource schema: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, statusChangeReason, task, parentResult, true);
            parentResult.recordFatalError("Couldn't update resource schema: " + e.getMessage(), e);
            //noinspection UnnecessaryReturnStatement
            return;
        }

        // TODO: connector sanity (e.g. refined schema, at least one account type, identifiers
        // in schema, etc.)

    }

    private void checkMutable(PrismObject<ResourceType> resource) {
        if (resource.isImmutable()) {
            throw new IllegalArgumentException("Got immutable resource object, while expecting mutable one: " + resource);
        }
    }

    private void updateResourceSchema(List<ConnectorSpec> allConnectorSpecs, OperationResult parentResult,
            PrismObject<ResourceType> resource)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
        if (resourceSchema != null) {
            for (ConnectorSpec connectorSpec : allConnectorSpecs) {
                ConnectorInstance instance = connectorManager.getConfiguredConnectorInstance(connectorSpec, false, parentResult);
                instance.updateSchema(resourceSchema);
            }
        }
    }

    private void testConnectionConnector(ConnectorSpec connectorSpec, Map<String, Collection<Object>> capabilityMap, Task task,
            OperationResult parentResult) throws ObjectNotFoundException {

        // === test INITIALIZATION ===

        OperationResult initResult = parentResult
                .createSubresult(ConnectorTestOperation.CONNECTOR_INITIALIZATION.getOperation());

        LOGGER.debug("Testing connection using {}", connectorSpec);
        String resourceOid = connectorSpec.getResource().getOid();

        String operationCtx = "testing connection using " + connectorSpec;

        ConfiguredConnectorInstanceEntry connectorInstanceCacheEntry;
        try {
            // Make sure we are getting non-configured instance.
            connectorInstanceCacheEntry = connectorManager.getOrCreateConnectorInstanceCacheEntry(connectorSpec, initResult);
            initResult.recordSuccess();
        } catch (ObjectNotFoundException e) {
            // The connector was not found. The resource definition is either
            // wrong or the connector is not installed.
            String msg = "The connector was not found: "+e.getMessage();
            operationCtx += " failed while getting connector instance. " + msg;
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            initResult.recordFatalError(msg, e);
            return;
        } catch (SchemaException e) {
            String msg = "Schema error while dealing with the connector definition: "+e.getMessage();
            operationCtx += " failed while getting connector instance. " + msg;
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            initResult.recordFatalError(msg, e);
            return;
        } catch (RuntimeException | Error e) {
            String msg = "Unexpected runtime error: "+e.getMessage();
            operationCtx += " failed while getting connector instance. " + msg;
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            initResult.recordFatalError(msg, e);
            return;
        }

        ConnectorInstance connector = connectorInstanceCacheEntry.getConnectorInstance();


        // === test CONFIGURATION ===

        OperationResult configResult = parentResult
                .createSubresult(ConnectorTestOperation.CONNECTOR_CONFIGURATION.getOperation());

        try {
            PrismObject<ResourceType> resource = connectorSpec.getResource().asPrismObject();
            PrismObjectDefinition<ResourceType> newResourceDefinition = resource.getDefinition().clone();
            applyConnectorSchemaToResource(connectorSpec, newResourceDefinition, resource, task, configResult);
            PrismContainerValue<ConnectorConfigurationType> connectorConfiguration = connectorSpec.getConnectorConfiguration().getValue();

            InternalMonitor.recordCount(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT);

            connector.configure(connectorConfiguration, ResourceTypeUtil.getSchemaGenerationConstraints(resource), configResult);

            // We need to explicitly initialize the instance, e.g. in case that the schema and capabilities
            // cannot be detected by the connector and therefore are provided in the resource
            //
            // NOTE: the capabilities and schema that are used here are NOT necessarily those that are detected by the resource.
            //       The detected schema will come later. The schema here is the one that is stored in the resource
            //       definition (ResourceType). This may be schema that was detected previously. But it may also be a schema
            //       that was manually defined. This is needed to be passed to the connector in case that the connector
            //       cannot detect the schema and needs schema/capabilities definition to establish a connection.
            //       Most connectors will just ignore the schema and capabilities that are provided here.
            //       But some connectors may need it (e.g. CSV connector working with CSV file without a header).
            //
            ResourceSchema previousResourceSchema = RefinedResourceSchemaImpl.getResourceSchema(connectorSpec.getResource(), prismContext);
            Collection<Object> previousCapabilities = ResourceTypeUtil.getNativeCapabilitiesCollection(connectorSpec.getResource());
            connector.initialize(previousResourceSchema, previousCapabilities,
                    ResourceTypeUtil.isCaseIgnoreAttributeNames(connectorSpec.getResource()), configResult);

            configResult.recordSuccess();
        } catch (CommunicationException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.DOWN, operationCtx, task, parentResult, true);
            configResult.recordFatalError("Communication error", e);
            return;
        } catch (GenericFrameworkException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            configResult.recordFatalError("Generic error", e);
            return;
        } catch (SchemaException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            configResult.recordFatalError("Schema error", e);
            return;
        } catch (ConfigurationException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            configResult.recordFatalError("Configuration error", e);
            return;
        } catch (ObjectNotFoundException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            configResult.recordFatalError("Object not found", e);
            return;
        } catch (ExpressionEvaluationException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            configResult.recordFatalError("Expression error", e);
            return;
        } catch (SecurityViolationException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            configResult.recordFatalError("Security violation", e);
            return;
        } catch (RuntimeException | Error e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            configResult.recordFatalError("Unexpected runtime error", e);
            return;
        }

        // === test CONNECTION ===

        // delegate the main part of the test to the connector
        connector.test(parentResult);

        parentResult.computeStatus();
        if (!parentResult.isAcceptable()) {
            operationCtx += ". Connector test failed: " + parentResult.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.DOWN, operationCtx, task, parentResult, true);
            // No point in going on. Following tests will fail anyway, they will
            // just produce misleading
            // messages.
            return;
        } else {
            operationCtx += ". Connector test successful.";
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.UP, operationCtx, task, parentResult, false);
        }

        // === test CAPABILITIES ===

        OperationResult capabilitiesResult = parentResult
                .createSubresult(ConnectorTestOperation.CONNECTOR_CAPABILITIES.getOperation());
        try {
            InternalMonitor.recordCount(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
            Collection<Object> retrievedCapabilities = connector.fetchCapabilities(capabilitiesResult);

            capabilityMap.put(connectorSpec.getConnectorName(), retrievedCapabilities);
            capabilitiesResult.recordSuccess();
        } catch (CommunicationException e) {
            operationCtx += " failed while testing capabilities: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.DOWN, operationCtx, task, parentResult, true);
            capabilitiesResult.recordFatalError("Communication error", e);
            return;
        } catch (GenericFrameworkException e) {
            operationCtx += " failed while testing capabilities: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            capabilitiesResult.recordFatalError("Generic error", e);
            return;
        } catch (ConfigurationException e) {
            operationCtx += " failed while testing capabilities: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            capabilitiesResult.recordFatalError("Configuration error", e);
            return;
        } catch (SchemaException e) {
            operationCtx += " failed while testing capabilities: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            capabilitiesResult.recordFatalError("Schema error", e);
            return;
        } catch (RuntimeException | Error e) {
            operationCtx += " failed while testing capabilities: " + e.getMessage();
            modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.BROKEN, operationCtx, task, parentResult, true);
            capabilitiesResult.recordFatalError("Unexpected runtime error", e);
            return;
        }

        // Connector instance is fully configured at this point.
        // But the connector cache entry may not be set up properly and it is not yet placed into the cache.
        // Therefore make sure the caching bit is completed.
        // Place the connector to cache even if it was configured at the beginning. The connector is reconfigured now.
        connectorManager.cacheConfiguredConnector(connectorInstanceCacheEntry, connectorSpec);
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
            } catch (SchemaException | ExpressionEvaluationException e) {
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
                List<ItemDelta<?, ?>> modifications = operationalStateManager.createAndLogOperationalStateDeltas(currentStatus, newStatus,
                        resourceDesc, statusChangeReason, resource);
                repositoryService.modifyObject(ResourceType.class, resourceOid, modifications, result);
                result.computeStatusIfUnknown();
                InternalMonitor.recordCount(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT);
            } catch (SchemaException | ObjectAlreadyExistsException e) {
                throw new SystemException("Unexpected exception while recording operation state change: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Adjust scheme with respect to capabilities. E.g. disable attributes that
     * are used for special purpose (such as account activation simulation).
     *
     * TODO treat also objectclass-specific capabilities here
     */
    private void adjustSchemaForSimulatedCapabilities(PrismObject<ResourceType> resource, ResourceSchema resourceSchema) {
        ResourceType resourceType = resource.asObjectable();
        if (resourceType.getCapabilities() == null || resourceType.getCapabilities().getConfigured() == null) {
            return;
        }
        ActivationCapabilityType activationCapability = CapabilityUtil.getCapability(resourceType
                .getCapabilities().getConfigured().getAny(), ActivationCapabilityType.class);
        if (CapabilityUtil.getEnabledActivationStatus(activationCapability) != null) {
            QName attributeName = activationCapability.getStatus().getAttribute();
            Boolean ignore = activationCapability.getStatus().isIgnoreAttribute();
            if (attributeName != null) {
                // The attribute used for enable/disable simulation should be ignored in the schema
                // otherwise strange things may happen, such as changing the same attribute both from
                // activation/enable and from the attribute using its native name.
                for (ObjectClassComplexTypeDefinition objectClassDefinition : resourceSchema
                        .getDefinitions(ObjectClassComplexTypeDefinition.class)) {
                    ResourceAttributeDefinition<?> attributeDefinition =
                            objectClassDefinition.findAttributeDefinition(attributeName);
                    if (attributeDefinition != null) {
                        if (ignore == null || ignore) {
                            ((MutableItemDefinition<?>) attributeDefinition).setProcessing(ItemProcessing.IGNORE);
                        }
                    } else {
                        // simulated activation attribute points to something that is not in the schema
                        // technically, this is an error. But it looks to be quite common in connectors.
                        // The enable/disable is using operational attributes that are not exposed in the
                        // schema, but they work if passed to the connector.
                        // Therefore we don't want to break anything. We could log an warning here, but the
                        // warning would be quite frequent. Maybe a better place to warn user would be import
                        // of the object.
                        LOGGER.debug("Simulated activation attribute "
                                + attributeName
                                + " for objectclass "
                                + objectClassDefinition.getTypeName()
                                + " in "
                                + resource
                                + " does not exist in the resource schema. This may work well, but it is not clean. Connector exposing such schema should be fixed.");
                    }
                }
            }
        }
    }

    public void applyDefinition(
            ObjectDelta<ResourceType> delta,
            ResourceType resourceWhenNoOid,
            GetOperationOptions options,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException {

        if (delta.isAdd()) {
            PrismObject<ResourceType> resource = delta.getObjectToAdd();
            applyConnectorSchemasToResource(resource, task, result);
            return;

        } else if (delta.isModify()) {
            // Go on
        } else {
            return;
        }

        if (delta.hasCompleteDefinition()) {
            //nothing to do, all modifications has definitions..just aplly this deltas..
            return;
        }


        PrismObject<ResourceType> resource;
        String resourceOid = delta.getOid();
        if (resourceOid == null) {
            Validate.notNull(resourceWhenNoOid, "Resource oid not specified in the object delta, and resource is not specified as well. Could not apply definition.");
            resource = resourceWhenNoOid.asPrismObject();
        } else {
            resource = getResource(resourceOid, options, task, result);
        }

        for (ConnectorSpec connectorSpec : ConnectorSpec.all(resource.asObjectable())) {
            applyDefinitionToDeltaForConnector(delta, connectorSpec, result);
        }
        applyDefinitionsForNewConnectors(delta, result);
    }

    private void applyDefinitionToDeltaForConnector(
            @NotNull ObjectDelta<ResourceType> delta, @NotNull ConnectorSpec connectorSpec, @NotNull OperationResult parentResult)
            throws SchemaException {
        OperationResult result = parentResult.subresult(OP_APPLY_DEFINITION_TO_DELTA)
                .addArbitraryObjectAsParam("connector", connectorSpec)
                .setMinor()
                .build();
        try {
            String connectorOid = getConnectorOid(delta, connectorSpec);
            if (connectorOid == null) {
                result.recordFatalError("Connector OID is missing");
                return;
            }

            PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDef =
                    getConfigurationContainerDefinition(connectorOid, result);
            if (configurationContainerDef == null) {
                return;
            }

            PrismContainer<ConnectorConfigurationType> connectorConfiguration = connectorSpec.getConnectorConfiguration();
            if (connectorConfiguration != null) {
                connectorConfiguration.applyDefinition(configurationContainerDef);
            }

            for (ItemDelta<?,?> itemDelta : delta.getModifications()){
                applyItemDefinition(itemDelta, connectorSpec, configurationContainerDef, result);
            }

        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private PrismContainerDefinition<ConnectorConfigurationType> getConfigurationContainerDefinition(
            String connectorOid, OperationResult result)
            throws SchemaException {

        ConnectorType connector;
        try {
            connector =
                    repositoryService
                            .getObject(ConnectorType.class, connectorOid, null, result)
                            .asObjectable();
        } catch (ObjectNotFoundException e) {
            // No connector, no fun. We can't apply the definition.
            result.recordFatalError(
                    "Connector (OID:" + connectorOid + ") referenced from the resource is not in the repository", e);
            return null;
        } catch (SchemaException e) {
            // Probably a malformed connector.
            result.recordPartialError("Connector (OID:" + connectorOid + ") referenced from the resource "
                    + "has schema problems: " + e.getMessage(), e);
            LOGGER.error("Connector (OID:{}) has schema problems: {}-{}", connectorOid, e.getMessage(), e);
            return null;
        }

        Element connectorSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(connector);
        if (connectorSchemaElement == null) {
            // No schema to apply to
            return null;
        }
        MutablePrismSchema connectorSchema;
        try {
            connectorSchema = prismContext.schemaFactory().createPrismSchema(
                    DOMUtil.getSchemaTargetNamespace(connectorSchemaElement));
            connectorSchema.parseThis(
                    connectorSchemaElement, true, "schema for " + connector, prismContext);
        } catch (SchemaException e) {
            throw new SchemaException("Error parsing connector schema for " + connector + ": " + e.getMessage(), e);
        }
        QName configContainerQName = new QName(connector.getNamespace(), ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
        PrismContainerDefinition<ConnectorConfigurationType> configContainerDef =
                connectorSchema.findContainerDefinitionByElementName(configContainerQName);
        if (configContainerDef == null) {
            throw new SchemaException("Definition of configuration container " + configContainerQName
                    + " not found in the schema of of " + connector);
        }
        return configContainerDef;
    }

    private String getConnectorOid(ObjectDelta<ResourceType> delta, ConnectorSpec connectorSpec) throws SchemaException {

        // Note: strict=true means that we are looking for this delta defined straight for the property.
        // We do so because here we don't try to apply definitions for additional connectors that are being added.
        ReferenceDelta connectorRefDelta =
                ItemDeltaCollectionsUtil.findItemDelta(
                        delta.getModifications(),
                        connectorSpec.getBasePath().append(ResourceType.F_CONNECTOR_REF),
                        ReferenceDelta.class,
                        true);

        if (connectorRefDelta != null) {
            Item<PrismReferenceValue,PrismReferenceDefinition> connectorRefNew =
                    connectorRefDelta.getItemNewMatchingPath(null);
            if (connectorRefNew.getValues().size() == 1) {
                PrismReferenceValue connectorRefValue = connectorRefNew.getValues().iterator().next();
                if (connectorRefValue.getOid() != null) {
                    // TODO what if there is a dynamic reference?
                    return connectorRefValue.getOid();
                }
            }
        }

        return connectorSpec.getConnectorOid();
    }


    private void applyDefinitionsForNewConnectors(ObjectDelta<ResourceType> delta, OperationResult result)
            throws SchemaException {
        List<PrismValue> newConnectors = delta.getNewValuesFor(ResourceType.F_ADDITIONAL_CONNECTOR);
        for (PrismValue newConnectorPcv : newConnectors) {
            //noinspection unchecked
            ConnectorInstanceSpecificationType newConnector =
                    ((PrismContainerValue<ConnectorInstanceSpecificationType>) newConnectorPcv).asContainerable();
            String connectorOid = getOid(newConnector.getConnectorRef());
            if (connectorOid == null) {
                continue;
            }
            PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDef =
                    getConfigurationContainerDefinition(connectorOid, result);
            if (configurationContainerDef == null) {
                continue;
            }
            ConnectorConfigurationType connectorConfiguration = newConnector.getConnectorConfiguration();
            if (connectorConfiguration != null) {
                connectorConfiguration.asPrismContainerValue().applyDefinition(configurationContainerDef);
            }
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void applyItemDefinition(
            ItemDelta<V,D> itemDelta,
            ConnectorSpec connectorSpec,
            PrismContainerDefinition<ConnectorConfigurationType> configContainerDef,
            OperationResult result) throws SchemaException {
        if (itemDelta.getDefinition() != null) {
            return;
        }
        ItemPath changedItemPath = itemDelta.getPath();
        ItemPath configurationContainerPath = connectorSpec.getConfigurationItemPath();
        if (!changedItemPath.startsWith(configurationContainerPath)) {
            return;
        }

        ItemPath remainder = changedItemPath.remainder(configurationContainerPath);
        if (remainder.isEmpty()) {
            // The delta is for the whole configuration container
            //noinspection unchecked
            itemDelta.applyDefinition((D) configContainerDef);
        } else {
            // The delta is for individual configuration property
            D itemDef = configContainerDef.findItemDefinition(remainder);
            if (itemDef == null) {
                LOGGER.warn("No definition found for item {}. Check your namespaces?", changedItemPath);
                result.recordWarning("No definition found for item delta: " + itemDelta +". Check your namespaces?" );
            } else {
                itemDelta.applyDefinition(itemDef);
            }
        }
    }

    public void applyDefinition(PrismObject<ResourceType> resource, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
        applyConnectorSchemasToResource(resource, task, parentResult);
    }

    @SuppressWarnings("unused")
    public void applyDefinition(ObjectQuery query, OperationResult result) {
        // TODO: not implemented yet
    }

    public Object executeScript(String resourceOid, ProvisioningScriptType script, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ResourceType> resource = getResource(resourceOid, null, task, result);
        ConnectorSpec connectorSpec = selectConnectorSpec(resource, ScriptCapabilityType.class);
        if (connectorSpec == null) {
            throw new UnsupportedOperationException("No connector supports script capability");
        }
        ConnectorInstance connectorInstance = connectorManager.getConfiguredConnectorInstance(connectorSpec, false, result);
        ExecuteProvisioningScriptOperation scriptOperation = ProvisioningUtil.convertToScriptOperation(script, "script on "+resource, prismContext);
        try {
            StateReporter reporter = new StateReporter(lightweightIdentifierGenerator, resourceOid, task);
            return connectorInstance.executeScript(scriptOperation, reporter, result);
        } catch (GenericFrameworkException e) {
            // Not expected. Transform to system exception
            result.recordFatalError("Generic provisioning framework error", e);
            throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
        }
    }

    public List<ConnectorOperationalStatus> getConnectorOperationalStatus(PrismObject<ResourceType> resource, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
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

    private List<ConnectorSpec> getAllConnectorSpecs(PrismObject<ResourceType> resource) throws SchemaException {
        List<ConnectorSpec> connectorSpecs = new ArrayList<>();
        connectorSpecs.add(getDefaultConnectorSpec(resource));
        for (ConnectorInstanceSpecificationType additionalConnectorType: resource.asObjectable().getAdditionalConnector()) {
            connectorSpecs.add(getConnectorSpec(resource, additionalConnectorType));
        }
        return connectorSpecs;
    }

    // Should be used only internally (private). But it is public, because it is accessed from the tests.
    public <T extends CapabilityType> ConnectorInstance getConfiguredConnectorInstance(PrismObject<ResourceType> resource,
            Class<T> operationCapabilityClass, boolean forceFresh, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        ConnectorSpec connectorSpec = selectConnectorSpec(resource, operationCapabilityClass);
        if (connectorSpec == null) {
            return null;
        }
        return connectorManager.getConfiguredConnectorInstance(connectorSpec, forceFresh, parentResult);
    }

    // Used by the tests. Does not change anything.
    @SuppressWarnings("SameParameterValue")
    <T extends CapabilityType> ConnectorInstance getConfiguredConnectorInstanceFromCache(PrismObject<ResourceType> resource,
            Class<T> operationCapabilityClass) throws SchemaException {
        ConnectorSpec connectorSpec = selectConnectorSpec(resource, operationCapabilityClass);
        return connectorSpec != null ? connectorManager.getConfiguredConnectorInstanceFromCache(connectorSpec) : null;
    }

    /**
     * Returns connector capabilities merged with capabilities defined at object class level.
     */
    <T extends CapabilityType> CapabilitiesType getConnectorCapabilities(ResourceType resource,
            RefinedObjectClassDefinition objectClassDefinition, Class<T> operationCapabilityClass) {
        if (resource == null) {
            return null;
        }

        CapabilitiesType connectorCapabilities = null;
        for (ConnectorInstanceSpecificationType additionalConnectorType : resource.getAdditionalConnector()) {
            if (supportsCapability(additionalConnectorType, operationCapabilityClass)) {
                connectorCapabilities = additionalConnectorType.getCapabilities();
            }
        }

        if (connectorCapabilities == null) {
            connectorCapabilities = resource.getCapabilities();
        }

        CapabilitiesType finalCapabilities = applyObjectClassCapabilities(connectorCapabilities, objectClassDefinition);
        LOGGER.trace("Returning final capabilities:\n{} ", finalCapabilities);
        return finalCapabilities;
    }

    /**
     * Merges object class specific capabilities with capabilities defined at connector level.
     * The specific capabilities take precedence over the connector-level ones.
     * (A unit of comparison is the whole capability, identified by its root level element.)
     */
    private CapabilitiesType applyObjectClassCapabilities(CapabilitiesType connectorCapabilities,
            RefinedObjectClassDefinition objectClassDefinition) {

        if (objectClassDefinition == null) {
            LOGGER.trace("No object class definition, skipping merge.");
            return connectorCapabilities;
        }

        CapabilitiesType objectClassCapabilities = objectClassDefinition.getCapabilities();
        if (objectClassCapabilities == null) {
            LOGGER.trace("No capabilities for {} specified, skipping merge.", objectClassDefinition);
            return connectorCapabilities;
        }

        CapabilityCollectionType configuredObjectClassCapabilities = objectClassCapabilities.getConfigured();
        if (configuredObjectClassCapabilities == null) {
            LOGGER.trace("No configured capabilities in {} specified, skipping merge", objectClassDefinition);
            return connectorCapabilities;
        }

        CapabilitiesType finalCapabilities = new CapabilitiesType(prismContext);
        if (connectorCapabilities.getNative() != null) {
            finalCapabilities.setNative(connectorCapabilities.getNative());
        }

        if (!hasConfiguredCapabilities(connectorCapabilities)) {
            LOGGER.trace("No configured capabilities found for connector, replacing with capabilities defined for {}", objectClassDefinition);
            finalCapabilities.setConfigured(configuredObjectClassCapabilities);
            return finalCapabilities;
        }

        for (Object capability : connectorCapabilities.getConfigured().getAny()) {
            if (!CapabilityUtil.containsCapabilityWithSameElementName(configuredObjectClassCapabilities.getAny(), capability)) {
                configuredObjectClassCapabilities.getAny().add(capability);
            }
        }

        finalCapabilities.setConfigured(configuredObjectClassCapabilities);
        return finalCapabilities;
    }

    private boolean hasConfiguredCapabilities(CapabilitiesType supportedCapabilities) {
        CapabilityCollectionType configured = supportedCapabilities.getConfigured();
        return configured != null && !configured.getAny().isEmpty();
    }

    @SuppressWarnings("SameParameterValue")
    private <T extends CapabilityType> ConnectorSpec selectConnectorSpec(PrismObject<ResourceType> resource, Map<String,Collection<Object>> capabilityMap, Class<T> capabilityClass) throws SchemaException {
        if (capabilityMap == null) {
            return selectConnectorSpec(resource, capabilityClass);
        }
        for (ConnectorInstanceSpecificationType additionalConnectorType: resource.asObjectable().getAdditionalConnector()) {
            if (supportsCapability(additionalConnectorType, capabilityMap.get(additionalConnectorType.getName()), capabilityClass)) {
                return getConnectorSpec(resource, additionalConnectorType);
            }
        }
        return getDefaultConnectorSpec(resource);
    }

    private <T extends CapabilityType> ConnectorSpec selectConnectorSpec(PrismObject<ResourceType> resource, Class<T> operationCapabilityClass) throws SchemaException {
        for (ConnectorInstanceSpecificationType additionalConnectorType: resource.asObjectable().getAdditionalConnector()) {
            if (supportsCapability(additionalConnectorType, operationCapabilityClass)) {
                return getConnectorSpec(resource, additionalConnectorType);
            }
        }
        return getDefaultConnectorSpec(resource);
    }

    private <T extends CapabilityType> boolean supportsCapability(ConnectorInstanceSpecificationType additionalConnectorType, Class<T> capabilityClass) {
        T cap = CapabilityUtil.getEffectiveCapability(additionalConnectorType.getCapabilities(), capabilityClass);
        return CapabilityUtil.isCapabilityEnabled(cap);
    }

    private <T extends CapabilityType> boolean supportsCapability(ConnectorInstanceSpecificationType additionalConnectorType, Collection<Object> nativeCapabilities, Class<T> capabilityClass) {
        CapabilitiesType specifiedCapabilitiesType = additionalConnectorType.getCapabilities();
        if (specifiedCapabilitiesType != null) {
            CapabilityCollectionType configuredCapCollectionType = specifiedCapabilitiesType.getConfigured();
            if (configuredCapCollectionType != null) {
                T configuredCap = CapabilityUtil.getCapability(configuredCapCollectionType.getAny(), capabilityClass);
                if (configuredCap != null && !CapabilityUtil.isCapabilityEnabled(configuredCap)) {
                    return false;
                }
            }

        }
        T cap = CapabilityUtil.getCapability(nativeCapabilities, capabilityClass);
        return CapabilityUtil.isCapabilityEnabled(cap);
    }

    private ConnectorSpec getDefaultConnectorSpec(PrismObject<ResourceType> resource) {
        return ConnectorSpec.main(resource.asObjectable());
    }

    private ConnectorSpec getConnectorSpec(
            PrismObject<ResourceType> resource, ConnectorInstanceSpecificationType additionalConnectorType)
            throws SchemaException {
        if (additionalConnectorType.getConnectorRef() == null) {
            throw new SchemaException("No connector reference in additional connector in "+resource);
        }
        String connectorOid = additionalConnectorType.getConnectorRef().getOid();
        if (StringUtils.isBlank(connectorOid)) {
            throw new SchemaException("No connector OID in additional connector in "+resource);
        }
        return ConnectorSpec.additional(resource.asObjectable(), additionalConnectorType);
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public @NotNull LightweightIdentifierGenerator getLightweightIdentifierGenerator() {
        return lightweightIdentifierGenerator;
    }
}
