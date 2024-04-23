package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.NativeResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType.UP;

/**
 * Helper class for updating schema and capabilities in a resource.
 *
 * Used during resource completion and resource testing.
 */
class ResourceUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceUpdater.class);

    @NotNull private final ResourceType resource;

    private final boolean updateRepository;
    private final boolean updateInMemory;

    @NotNull private final CommonBeans beans;

    /**
     * Here we collect modifications that are to be applied to the resource in repository and/or in memory.
     * Some special modifications are applied directly (because of missing item paths - no PCV IDs).
     */
    @NotNull private final Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();

    ResourceUpdater(
            @NotNull ResourceType resource,
            boolean updateRepository,
            boolean updateInMemory,
            @NotNull CommonBeans beans) {
        this.resource = resource;
        this.updateRepository = updateRepository;
        this.updateInMemory = updateInMemory;
        this.beans = beans;
    }

    /**
     * Merges native with configured capabilities.
     */
    void updateNativeCapabilities(
            @NotNull ConnectorSpec connectorSpec,
            @Nullable CapabilityCollectionType nativeCapabilities) throws SchemaException {

        if (nativeCapabilities == null) {
            LOGGER.trace("Native capabilities for {} are not known -> not updating", connectorSpec);
            return; // TODO should we clear the caching metadata?
        }

        LOGGER.trace("Going to update native capabilities for {}:\n{}",
                connectorSpec, nativeCapabilities.debugDumpLazily(1));

        CapabilitiesType newCapabilities = new CapabilitiesType();

        CapabilitiesType existingCapabilities = connectorSpec.getCapabilities();
        CapabilityCollectionType existingConfigured = existingCapabilities != null ? existingCapabilities.getConfigured() : null;
        if (existingConfigured != null) {
            newCapabilities.setConfigured(existingConfigured.clone());
        }
        newCapabilities.setNative(CloneUtil.clone(nativeCapabilities));
        newCapabilities.setCachingMetadata(MiscSchemaUtil.generateCachingMetadata());

        if (updateRepository) {
            modifications.add(
                    PrismContext.get().deltaFor(ResourceType.class)
                            .item(connectorSpec.getCapabilitiesItemPath())
                            .replace(newCapabilities.clone())
                            .asItemDelta());
        }
        if (updateInMemory) {
            connectorSpec.setCapabilities(newCapabilities.clone());
        }
    }

    void updateCapabilitiesCachingMetadata(@NotNull ConnectorSpec connectorSpec)
            throws SchemaException {
        CachingMetadataType cachingMetadata = MiscSchemaUtil.generateCachingMetadata();
        if (updateRepository) {
            modifications.add(
                    PrismContext.get().deltaFor(ResourceType.class)
                            .item(connectorSpec.getCapabilitiesItemPath().append(CapabilitiesType.F_CACHING_METADATA))
                            .replace(cachingMetadata.clone())
                            .asItemDelta());
        }
        if (updateInMemory) {
            connectorSpec.setCapabilitiesCachingMetadata(cachingMetadata);
        }
    }

    void updateSchema(NativeResourceSchema schema) throws SchemaException {
        if (updateRepository) {
            modifications.add(
                    PrismContext.get().deltaFor(ResourceType.class)
                            .item(ResourceType.F_SCHEMA)
                            .replace(createSchemaUpdateValue(schema))
                            .asItemDelta());
        }
        if (updateInMemory) {
            resource.schema(createSchemaUpdateValue(schema));
        }
    }

    private XmlSchemaType createSchemaUpdateValue(NativeResourceSchema nativeResourceSchema) throws SchemaException {
        SchemaDefinitionType schemaDefinition = new SchemaDefinitionType();
        schemaDefinition.setSchema(
                getSchemaRootElement(nativeResourceSchema));

        return new XmlSchemaType()
                .cachingMetadata(MiscSchemaUtil.generateCachingMetadata())
                .definition(schemaDefinition)
                .generationConstraints(getCurrentSchemaGenerationConstraints());
    }

    @NotNull
    private Element getSchemaRootElement(NativeResourceSchema nativeResourceSchema) throws SchemaException {
        Document xsdDoc;
        try {
            xsdDoc = nativeResourceSchema.serializeToXsd();
            LOGGER.trace("Serialized XSD resource schema for {}:\n{}",
                    resource, lazy(() -> DOMUtil.serializeDOMToString(xsdDoc)));
        } catch (SchemaException e) {
            throw new SchemaException("Error processing resource schema for " + resource + ": " + e.getMessage(), e);
        }

        return MiscUtil.requireNonNull(
                DOMUtil.getFirstChildElement(xsdDoc),
                () -> "No schema was generated for " + resource);
    }

    private SchemaGenerationConstraintsType getCurrentSchemaGenerationConstraints() {
        XmlSchemaType schema = resource.getSchema();
        return schema != null ? schema.getGenerationConstraints() : null;
    }

    void updateSchemaCachingMetadata() throws SchemaException {
        modifications.add(
                PrismContext.get().deltaFor(ResourceType.class)
                        .item(ResourceType.F_SCHEMA, CapabilitiesType.F_CACHING_METADATA)
                        .replace(MiscSchemaUtil.generateCachingMetadata())
                        .asItemDelta());
    }

    // TODO fix (generalize) this method
    void markResourceUp() throws SchemaException {
        if (updateRepository) {
            // Update the operational state (we know we are up, as the schema was freshly loaded).
            AvailabilityStatusType previousStatus = ResourceTypeUtil.getLastAvailabilityStatus(resource);
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
        }
        if (updateInMemory) {
            // currently always false
        }
    }

    /** Beware, some in-memory modifications do not wait until this method is called. */
    void applyModifications(OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (modifications.isEmpty()) {
            return;
        }
        // TODO what if resource has no OID?
        try {
            LOGGER.trace("Applying modifications to {}:\n{}",
                    resource, DebugUtil.debugDumpLazily(modifications, 1));
            beans.repositoryService.modifyObject(ResourceType.class, resource.getOid(), modifications, result);
            InternalMonitor.recordCount(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT);
        } catch (ObjectAlreadyExistsException ex) {
            throw SystemException.unexpected(ex, "when updating resource during completion");
        }
    }
}
