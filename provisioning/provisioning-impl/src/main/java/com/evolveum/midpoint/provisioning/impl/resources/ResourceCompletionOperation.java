/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType.UP;

/**
 * Responsible for "completing" a resource object, i.e. transforming the raw value fetched from the repository
 * into fully operational version - resolving super-resources, fetching schema and capabilities, and so on.
 *
 * To be used only from the local package only. All external access should be through {@link ResourceManager}.
 */
class ResourceCompletionOperation {

    private static final String OP_COMPLETE_RESOURCE = ResourceCompletionOperation.class.getName() + ".completeResource";

    private static final Trace LOGGER = TraceManager.getTrace(ResourceCompletionOperation.class);

    /**
     * The resource being completed. Must be mutable. This object is used throughout the operation,
     * except for final stages when the reloaded one is used instead.
     */
    @NotNull private final PrismObject<ResourceType> resource;

    /** Root options used in the request to obtain the resource definition. We look e.g. after `noFetch` here. */
    @Nullable private final GetOperationOptions options;

    /** Resource schema. May be provided by the client. Updated by the operation. */
    private ResourceSchema rawResourceSchema;

    /**
     * True if the schema was just loaded. This also means that it is "extra raw",
     * i.e. it is not adjusted for e.g. simulated capabilities.
     */
    private boolean isSchemaFreshlyLoaded;

    /** TODO */
    private final Map<String, Collection<Object>> capabilityMap;

    @NotNull private final Task task;
    @NotNull private final CommonBeans beans;

    @NotNull private final ResourceExpansionOperation expansionOperation;

    /**
     * Operation result for the operation itself. It is quite unusual to store the operation result
     * like this, but we need it to provide the overall success/failure of the operation.
     */
    private OperationResult result;

    ResourceCompletionOperation(
            @NotNull PrismObject<ResourceType> resource,
            @Nullable GetOperationOptions options,
            @Nullable ResourceSchema rawResourceSchema,
            boolean isSchemaFreshlyLoaded,
            @Nullable Map<String, Collection<Object>> capabilityMap,
            @NotNull Task task,
            @NotNull CommonBeans beans) {

        this.resource = resource.cloneIfImmutable();
        this.options = options;
        this.rawResourceSchema = rawResourceSchema;
        this.isSchemaFreshlyLoaded = isSchemaFreshlyLoaded;
        this.capabilityMap = capabilityMap;
        this.task = task;
        this.beans = beans;
        this.expansionOperation = new ResourceExpansionOperation(resource.asObjectable(), beans);
    }

    /**
     * TODO review/update this javadoc
     *
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
     * @return completed resource
     */
    public @NotNull PrismObject<ResourceType> execute(OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, ConfigurationException {

        result = parentResult.createMinorSubresult(OP_COMPLETE_RESOURCE);
        try {
            expansionOperation.execute(result);
            applyConnectorSchema();
            PrismObject<ResourceType> reloaded = completeAndReload();
            parseSchema(reloaded);
            return reloaded;
        } catch (StopException e) {
            LOGGER.trace("Completion operation was stopped");
            return resource;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private void applyConnectorSchema() throws StopException {
        try {
            beans.resourceManager.applyConnectorSchemasToResource(resource, task, result);
        } catch (Throwable t) {
            String message =
                    "An error occurred while applying connector schema to connector configuration of " + resource + ": "
                            + t.getMessage();
            result.recordPartialError(message, t); // Maybe fatal is more appropriate
            LOGGER.warn(message, t);
            throw new StopException();
        }
    }

    private @NotNull PrismObject<ResourceType> completeAndReload()
            throws StopException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        if (isComplete(resource)) {
            LOGGER.trace("The resource is complete.");
            return resource;
        }

        LOGGER.trace("The resource is NOT complete. Trying to fetch schema and capabilities.");

        if (GetOperationOptions.isNoFetch(options)) {
            LOGGER.trace("We need to fetch schema, but the noFetch option is specified. Therefore returning whatever we have.");
            throw new StopException();
        }

        try {
            completeSchemaAndCapabilities();
        } catch (Throwable t) {
            // Catch the exceptions. There are not critical. We need to catch them all because the connector may
            // throw even undocumented runtime exceptions.
            // Even non-complete resource may still be usable. The fetchResult indicates that there was an error
            result.recordPartialError("Cannot complete resource schema and capabilities: " + t.getMessage(), t);
            throw new StopException();
        }

        // Now we need to re-read the resource from the repository and re-apply the schemas. This ensures that we will
        // cache the correct version and that we avoid race conditions, etc.
        PrismObject<ResourceType> reloaded = beans.resourceManager.readResourceFromRepository(resource.getOid(), result);
        beans.resourceManager.applyConnectorSchemasToResource(reloaded, task, result);
        return reloaded;
    }

    private void parseSchema(PrismObject<ResourceType> reloaded) {
        try {
            // Make sure the schema is parseable. We are going to cache the resource, so we want to cache it
            // with the parsed schemas.
            ResourceSchemaFactory.getRawSchema(reloaded);
            ResourceSchemaFactory.getCompleteSchema(reloaded);
        } catch (Throwable e) {
            String message = "Error while processing schemaHandling section of " + reloaded + ": " + e.getMessage();
            result.recordPartialError(message, e);
            LOGGER.warn(message, e);
        }
    }

    private void completeSchemaAndCapabilities()
            throws SchemaException, CommunicationException, ObjectNotFoundException, GenericFrameworkException,
            ConfigurationException {

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();

        // Capabilities
        // we need to process capabilities first. Schema is one of the connector capabilities.
        // We need to determine this capability to select the right connector for schema retrieval.
        completeCapabilities(capabilityMap != null, capabilityMap, modifications, result);

        if (rawResourceSchema == null) {
            // Try to get existing schema from resource. We do not want to override this if it exists
            // (but we still want to refresh the capabilities, that happens below)
            rawResourceSchema = ResourceSchemaFactory.getRawSchema(resource);
        }

        if (rawResourceSchema == null || rawResourceSchema.isEmpty()) {
            fetchResourceSchema();
        }

        if (rawResourceSchema != null) {
            if (isSchemaFreshlyLoaded) {
                adjustSchemaForSimulatedCapabilities();
                modifications.add(
                        createSchemaUpdateDelta());

                // Update the operational state (we know we are up, as the schema was freshly loaded).
                AvailabilityStatusType previousStatus = ResourceTypeUtil.getLastAvailabilityStatus(resource.asObjectable());
                if (previousStatus != UP) {
                    modifications.addAll(
                            beans.operationalStateManager.createAndLogOperationalStateDeltas(
                                    previousStatus,
                                    UP,
                                    resource.toString(),
                                    "resource schema was successfully fetched",
                                    resource));
                } else {
                    // just for sure (if the status changed in the meanwhile)
                    modifications.add(
                            beans.operationalStateManager.createAvailabilityStatusDelta(UP));
                }
            } else {
                CachingMetadataType schemaCachingMetadata = getCurrentCachingMetadata();
                if (schemaCachingMetadata == null) {
                    modifications.add(
                            createMetadataUpdateDelta());
                }
            }
        }

        if (!modifications.isEmpty()) {
            try {
                LOGGER.trace("Applying completion modifications to {}:\n{}",
                        resource, DebugUtil.debugDumpLazily(modifications, 1));
                beans.cacheRepositoryService.modifyObject(ResourceType.class, resource.getOid(), modifications, result);
                InternalMonitor.recordCount(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT);
            } catch (ObjectAlreadyExistsException ex) {
                throw SystemException.unexpected(ex, "when updating resource during completion");
            }
        }
    }

    private CachingMetadataType getCurrentCachingMetadata() {
        XmlSchemaType schema = resource.asObjectable().getSchema();
        return schema != null ? schema.getCachingMetadata() : null;
    }

    private void fetchResourceSchema()
            throws CommunicationException, GenericFrameworkException, ConfigurationException, ObjectNotFoundException,
            SchemaException {
        LOGGER.trace("Fetching resource schema for {}", resource);
        rawResourceSchema = beans.resourceManager.fetchResourceSchema(resource, capabilityMap, result);
        if (rawResourceSchema == null) {
            LOGGER.warn("No resource schema fetched from {}", resource);
        } else if (rawResourceSchema.isEmpty()) {
            LOGGER.warn("Empty resource schema fetched from {}", resource);
        } else {
            LOGGER.debug("Fetched resource schema for {}: {} definitions", resource, rawResourceSchema.getDefinitions().size());
            isSchemaFreshlyLoaded = true;
        }
    }

    private void completeCapabilities(boolean forceRefresh, Map<String,Collection<Object>> capabilityMap, Collection<ItemDelta<?, ?>> modifications,
            OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        ResourceType resourceType = resource.asObjectable();
        ConnectorSpec defaultConnectorSpec = beans.resourceManager.getDefaultConnectorSpec(resource);
        CapabilitiesType resourceCapType = resourceType.getCapabilities();
        if (resourceCapType == null) {
            resourceCapType = new CapabilitiesType();
            resourceType.setCapabilities(resourceCapType);
        }
        completeConnectorCapabilities(defaultConnectorSpec, resourceCapType, ResourceType.F_CAPABILITIES, forceRefresh,
                capabilityMap==null?null:capabilityMap.get(null),
                modifications, result);

        for (ConnectorInstanceSpecificationType additionalConnectorType: resource.asObjectable().getAdditionalConnector()) {
            ConnectorSpec connectorSpec = beans.resourceManager.getConnectorSpec(resource, additionalConnectorType);
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
                            PrismContext.get().deltaFactory().property().createModificationReplaceProperty(
                                    ItemPath.create(ResourceType.F_CAPABILITIES, CapabilitiesType.F_CACHING_METADATA),
                                    connectorSpec.getResource().getDefinition(),
                                    cachingMetadata)
                    );
                }
                return;
            }
        }

        if (retrievedCapabilities == null) {
            try {

                InternalMonitor.recordCount(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);

                ConnectorInstance connector = beans.connectorManager.getConfiguredConnectorInstance(connectorSpec, false, result);
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
        ObjectDelta<ResourceType> capabilitiesReplaceDelta = PrismContext.get().deltaFactory().object()
                .createModificationReplaceContainer(ResourceType.class, connectorSpec.getResource().getOid(),
                        itemPath, capType.asPrismContainerValue().clone());

        modifications.addAll(capabilitiesReplaceDelta.getModifications());
    }

    private ContainerDelta<XmlSchemaType> createSchemaUpdateDelta() throws SchemaException {
        Document xsdDoc;
        try {
            xsdDoc = rawResourceSchema.serializeToXsd();
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

        PrismContext prismContext = PrismContext.get();
        ContainerDelta<XmlSchemaType> schemaContainerDelta =
                prismContext.deltaFactory().container().createDelta(ResourceType.F_SCHEMA, ResourceType.class);
        PrismContainerValue<XmlSchemaType> cval = prismContext.itemFactory().createContainerValue();
        schemaContainerDelta.setValueToReplace(cval);
        PrismProperty<CachingMetadataType> cachingMetadataProperty = cval.createProperty(XmlSchemaType.F_CACHING_METADATA);
        cachingMetadataProperty.setRealValue(
                MiscSchemaUtil.generateCachingMetadata());
        List<QName> objectClasses = ResourceTypeUtil.getSchemaGenerationConstraints(resource);
        if (objectClasses != null) {
            PrismProperty<SchemaGenerationConstraintsType> generationConstraints =
                    cval.createProperty(XmlSchemaType.F_GENERATION_CONSTRAINTS);
            SchemaGenerationConstraintsType constraints = new SchemaGenerationConstraintsType();
            constraints.getGenerateObjectClass().addAll(objectClasses);
            generationConstraints.setRealValue(constraints);
        }
        PrismProperty<SchemaDefinitionType> definitionProperty = cval.createProperty(XmlSchemaType.F_DEFINITION);
        ObjectTypeUtil.setXsdSchemaDefinition(definitionProperty, xsdElement);

        return schemaContainerDelta;
    }

    private PropertyDelta<CachingMetadataType> createMetadataUpdateDelta() {
        return PrismContext.get().deltaFactory().property().createModificationReplaceProperty(
                ItemPath.create(ResourceType.F_SCHEMA, CapabilitiesType.F_CACHING_METADATA),
                resource.getDefinition(),
                MiscSchemaUtil.generateCachingMetadata());
    }

    /**
     * Adjust scheme with respect to capabilities. E.g. disable attributes that
     * are used for special purpose (such as account activation simulation).
     *
     * TODO treat also objectclass-specific capabilities here
     */
    private void adjustSchemaForSimulatedCapabilities() {
        ResourceType resourceBean = resource.asObjectable();
        if (resourceBean.getCapabilities() == null || resourceBean.getCapabilities().getConfigured() == null) {
            return;
        }
        ActivationCapabilityType activationCapability =
                CapabilityUtil.getCapability(
                        resourceBean.getCapabilities().getConfigured().getAny(), ActivationCapabilityType.class);
        if (CapabilityUtil.getEnabledActivationStatus(activationCapability) != null) {
            QName attributeName = activationCapability.getStatus().getAttribute();
            Boolean ignore = activationCapability.getStatus().isIgnoreAttribute();
            if (attributeName != null && !Boolean.FALSE.equals(ignore)) {
                setAttributeIgnored(attributeName);
            }
        }
    }

    /**
     * Sets the attribute with a given name as ignored - in all object classes.
     *
     * The attribute used for enable/disable simulation should be ignored in the schema
     * otherwise strange things may happen, such as changing the same attribute both from
     * activation/enable and from the attribute using its native name.
     *
     * TODO Is it OK that we update the attribute in all the object classes?
     */
    private void setAttributeIgnored(QName attributeName) {
        if (rawResourceSchema.isImmutable()) {
            rawResourceSchema = rawResourceSchema.clone();
        }
        assert rawResourceSchema.isRaw();

        for (ResourceObjectClassDefinition objectClassDefinition : rawResourceSchema.getObjectClassDefinitions()) {
            ResourceAttributeDefinition<?> attributeDefinition = objectClassDefinition.findAttributeDefinition(attributeName);
            if (attributeDefinition != null) {
                objectClassDefinition.toMutable().replaceDefinition(
                        attributeDefinition.getItemName(),
                        attributeDefinition.spawnModifyingRaw(def -> def.setProcessing(ItemProcessing.IGNORE)));
            } else {
                // TODO is the following description OK even if we consider multiple object classes?
                //  For example, the attribute may be present in inetOrgPerson but may be missing in
                //  organizationalUnit.
                //
                // Simulated activation attribute points to something that is not in the schema
                // technically, this is an error. But it looks to be quite common in connectors.
                // The enable/disable is using operational attributes that are not exposed in the
                // schema, but they work if passed to the connector.
                // Therefore we don't want to break anything. We could log an warning here, but the
                // warning would be quite frequent. Maybe a better place to warn user would be import
                // of the object.
                LOGGER.debug("Simulated activation attribute {} for objectclass {} in {}  does not exist in "
                        + "the resource schema. This may work well, but it is not clean. Connector exposing "
                        + "such schema should be fixed.", attributeName, objectClassDefinition.getTypeName(), resource);
            }
        }
    }

    static boolean isComplete(PrismObject<ResourceType> resource) {
        return hasSchema(resource) && hasCapabilitiesCached(resource);
    }

    static boolean hasCapabilitiesCached(PrismObject<ResourceType> resource) {
        CapabilitiesType capabilities = resource.asObjectable().getCapabilities();
        return capabilities != null && capabilities.getCachingMetadata() != null;
    }

    static boolean hasSchema(PrismObject<ResourceType> resource) {
        return ResourceTypeUtil.getResourceXsdSchema(resource) != null;
    }

    public OperationResultStatus getOperationResultStatus() {
        return result.getStatus();
    }

    /** Returns OIDs of objects that are ancestors to the current resource. Used e.g. for cache invalidation. */
    public @NotNull Collection<String> getAncestorsOids() {
        return expansionOperation.getAncestorsOids();
    }

    /** Stopping the evaluation, and returning the {@link #resource}. */
    private static class StopException extends Exception {
    }
}
