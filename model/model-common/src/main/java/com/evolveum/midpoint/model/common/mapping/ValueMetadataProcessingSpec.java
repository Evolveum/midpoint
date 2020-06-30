/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping;

import java.util.*;

import com.evolveum.midpoint.model.common.util.ObjectTemplateIncludeProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

/**
 * Specification of value metadata processing: mappings that should be applied
 * and item definitions driving e.g. storage and applicability of built-in processing of individual
 * metadata items.
 *
 * Information present here is derived from e.g. object templates and metadata mappings attached to data mappings.
 * It is already processed regarding applicability of metadata processing to individual data items.
 */
public class ValueMetadataProcessingSpec {

    @NotNull private final Collection<MetadataMappingType> mappings = new ArrayList<>();
    @NotNull private final Collection<MetadataItemDefinitionType> itemDefinitions = new ArrayList<>();

    /**
     * Item processing for given metadata items. Lazily evaluated.
     */
    private Map<ItemPath, ItemProcessingType> itemProcessingMap;

    public boolean isEmpty() {
        return false;
    }

    void addFromObjectTemplate(ObjectTemplateType template, ObjectResolver objectResolver, String contextDesc,
            Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        // TODO resolve conflicts in included templates
        new ObjectTemplateIncludeProcessor(objectResolver)
                .processThisAndIncludedTemplates(template, contextDesc, task, result, this::addFromObjectTemplate);
    }

    private void addFromObjectTemplate(ObjectTemplateType template) {
        if (template.getMeta() != null) {
            mappings.addAll(template.getMeta().getMapping());
            for (MetadataItemDefinitionType item : template.getMeta().getItem()) {
                itemDefinitions.add(item);
                for (MetadataMappingType itemMapping : item.getMapping()) {
                    mappings.add(provideDefaultTarget(itemMapping, item));
                }
            }
        }
    }

    private MetadataMappingType provideDefaultTarget(MetadataMappingType itemMapping, MetadataItemDefinitionType item) {
        if (itemMapping.getTarget() != null && itemMapping.getTarget().getPath() != null) {
            return itemMapping;
        } else {
            if (item.getRef() == null) {
                // todo better error handling
                throw new IllegalArgumentException("No `ref` value in item definition");
            }
            MetadataMappingType clone = itemMapping.clone();
            if (clone.getTarget() == null) {
                clone.beginTarget().path(item.getRef());
            } else {
                clone.getTarget().path(item.getRef());
            }
            return clone;
        }
    }

    void addMappings(List<MetadataMappingType> mappings) {
        this.mappings.addAll(mappings);
    }

    public Collection<MetadataMappingType> getMappings() {
        return mappings;
    }

    public Collection<MetadataItemDefinitionType> getItemDefinitions() {
        return itemDefinitions;
    }

    private void computeItemProcessingMapIfNeeded() throws SchemaException {
        if (itemProcessingMap == null) {
            computeItemProcessingMap();
        }
    }

    private void computeItemProcessingMap() throws SchemaException {
        itemProcessingMap = new HashMap<>();
        for (MetadataItemDefinitionType item : itemDefinitions) {
            if (item.getRef() == null) {
                throw new SchemaException("No 'ref' in item definition: " + item);
            }
            ItemProcessingType processing = getProcessingOfItem(item);
            if (processing != null) {
                itemProcessingMap.put(item.getRef().getItemPath(), processing);
            }
        }
    }

    /**
     * Extracts processing information from specified item definition.
     */
    private ItemProcessingType getProcessingOfItem(MetadataItemDefinitionType item) throws SchemaException {
        Set<ItemProcessingType> processing = new HashSet<>();
        for (PropertyLimitationsType limitation : item.getLimitations()) {
            if (limitation.getLayer().isEmpty() || limitation.getLayer().contains(LayerType.MODEL)) {
                if (limitation.getProcessing() != null) {
                    processing.add(limitation.getProcessing());
                }
            }
        }
        return MiscUtil.extractSingleton(processing,
                () -> new SchemaException("Contradicting 'processing' values for " + item + ": " + processing));
    }

    /**
     * Looks up processing information for given path. Proceeds from the most specific to most abstract (empty) path.
     */
    private ItemProcessingType getProcessing(ItemPath itemPath) throws SchemaException {
        computeItemProcessingMapIfNeeded();
        while (!itemPath.isEmpty()) {
            ItemProcessingType processing = ItemPathCollectionsUtil.getFromMap(itemProcessingMap, itemPath);
            if (processing != null) {
                return processing;
            }
            itemPath = itemPath.allExceptLast();
        }
        return null;
    }

    boolean isFullProcessing(ItemPath itemPath) throws SchemaException {
        return getProcessing(itemPath) == ItemProcessingType.FULL;
    }
}
