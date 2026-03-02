/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.schema.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.MergeStrategy;
import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Builder-style class for extending resource schemas with additional definitions.
 *
 * This class provides a way to augment resource schemas by adding custom definitions without modifying the original
 * resource or its parsed schema.
 *
 * The extension process works by:
 *
 * - Collecting all additions through method calls.
 * - Merging definitions if needed, utilizing the {@link BaseMergeOperation} (e.g. when attribute definitions of the
 *   same attribute, but with different mappings are added).
 * - Wrapping all definitions to the schema handling.
 * - Parsing the complete schema of the resource with additional schema handling.
 *
 * @see ResourceSchemaFactory#schemaExtenderFor(ResourceType) for creating instances of this class.
 * @see MergeStrategy#OVERLAY for understanding the used strategy for definition merging.
 */
public final class ResourceSchemaExtender {
    private final NativeResourceSchema nativeSchema;
    private final ResourceType resource;
    private final Map<ResourceObjectTypeIdentification, ResourceObjectTypeDefinitionType> objectTypes;

    public ResourceSchemaExtender(NativeResourceSchema nativeSchema, ResourceType resource) {
        this.nativeSchema = nativeSchema;
        this.resource = resource;
        this.objectTypes = new HashMap<>();
    }

    /**
     * Adds an attribute definition to a specific resource object type.
     *
     * If an attribute with the same reference has already been added, the new definition will be merged with the
     * existing one using {@link BaseMergeOperation}. Otherwise, the attribute definition will be added as a new
     * attribute.
     *
     * @param objectTypeId The identification of the object type (kind and intent) to which the attribute belongs.
     * @param attributeDefinition The attribute definition to add or merge. Must contain a valid attribute reference.
     * @return This instance of the extender.
     * @throws SchemaException When a schema error occurs during merging of existing attribute definitions.
     * @throws ConfigurationException When a config error occurs during merging of existing attribute definitions.
     */
    public ResourceSchemaExtender addAttributeDefinition(ResourceObjectTypeIdentification objectTypeId,
            ResourceAttributeDefinitionType attributeDefinition) throws SchemaException, ConfigurationException {
        final ResourceObjectTypeDefinitionType objectType = this.objectTypes.computeIfAbsent(objectTypeId,
                ResourceSchemaExtender::createObjectType);

        final List<ResourceAttributeDefinitionType> attributes = objectType.getAttribute();
        boolean notFound = true;
        for (int i = 0; i < attributes.size(); i++) {
            final ResourceAttributeDefinitionType attr = attributes.get(i);
            if (attr.getRef().equivalent(attributeDefinition.getRef())) {
                attributes.set(i, BaseMergeOperation.merge(attr, attributeDefinition));
                notFound = false;
                break;
            }
        }
        if (notFound) {
            attributes.add(attributeDefinition);
        }
        return this;
    }

    /**
     * Adds a correlation definition to a specific resource object type.
     *
     * If a correlation definition already exists for the object type, the new definition will be merged with the
     * existing one using {@link BaseMergeOperation}. Otherwise, the correlation definition will be set as the new
     * correlation for the object type.
     *
     * @param objectTypeId The identification of the object type (kind and intent) to which the correlation belongs.
     * @param correlationDefinition The correlation definition to add or merge.
     * @return This instance of the extender.
     * @throws SchemaException When a schema error occurs during merging of existing correlation definitions.
     * @throws ConfigurationException When a config error occurs during merging of existing correlation definitions.
     */
    public ResourceSchemaExtender addCorrelationDefinition(ResourceObjectTypeIdentification objectTypeId,
            CorrelationDefinitionType correlationDefinition) throws SchemaException, ConfigurationException {
        final ResourceObjectTypeDefinitionType objectType = this.objectTypes.computeIfAbsent(objectTypeId,
                ResourceSchemaExtender::createObjectType);

        if (objectType.getCorrelation() != null) {
            objectType.correlation(BaseMergeOperation.merge(objectType.getCorrelation(), correlationDefinition));
        } else {
            objectType.correlation(correlationDefinition);
        }
        return this;
    }

    /**
     * Parses and returns the complete resource schema including all added definitions.
     *
     * This method combines the original resource schema with all the definitions that have been added.
     *
     * NOTE: The parsing itself is delegated to the {@link ResourceSchemaParser}.
     *
     * @return The complete resource schema that includes all added definitions, appearing as if they were part of
     *         the original schema from the beginning.
     * @throws SchemaException When a schema error occurs during parsing of the resource schema.
     * @throws ConfigurationException When a config error occurs during parsing of the resource schema.
     * @see CompleteResourceSchema
     */
    public CompleteResourceSchema extend() throws SchemaException, ConfigurationException {
        final SchemaHandlingType additionalSchemaHandling = wrapInSchemaHandling();
        return ResourceSchemaParser.parseSchemaWithAdditionalSchemaHandling(this.resource, this.nativeSchema,
                additionalSchemaHandling);
    }

    private SchemaHandlingType wrapInSchemaHandling() {
        final SchemaHandlingType schemaHandling = new SchemaHandlingType();
        schemaHandling.getObjectType().addAll(this.objectTypes.values());
        return schemaHandling;
    }

    private static ResourceObjectTypeDefinitionType createObjectType(ResourceObjectTypeIdentification id) {
        return new ResourceObjectTypeDefinitionType()
                .kind(id.getKind())
                .intent(id.getIntent());
    }

}
