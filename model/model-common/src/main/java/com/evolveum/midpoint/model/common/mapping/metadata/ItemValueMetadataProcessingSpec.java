/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata;

import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.common.util.ObjectTemplateIncludeProcessor;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.MetadataMappingConfigItem;
import com.evolveum.midpoint.schema.config.OriginProvider;
import com.evolveum.midpoint.schema.metadata.DefaultValueMetadataProcessing;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ItemRefinedDefinitionTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.model.common.mapping.metadata.ProcessingUtil.getProcessingOfItem;

/**
 * Specification of value metadata processing for a given data item: mappings that should be applied
 * and item definitions driving e.g. storage and applicability of built-in processing of individual
 * metadata items.
 *
 * Information present here is derived from e.g. object templates and metadata mappings attached to data mappings.
 * It is already processed regarding applicability of metadata processing to individual data items.
 */
public class ItemValueMetadataProcessingSpec implements ShortDumpable, DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(ItemValueMetadataProcessingSpec.class);

    /** [EP:M:MM] DONE 2/2 (two places at which elements are added to this collection) */
    @NotNull private final Collection<MetadataMappingConfigItem> mappings = new ArrayList<>();
    @NotNull private final Collection<MetadataItemDefinitionType> itemDefinitions = new ArrayList<>();
    @NotNull private final Collection<ItemPath> metadataPathsToIgnore = new ArrayList<>();

    /**
     * This processing spec will contain mappings targeted at this scope.
     */
    @NotNull private final MetadataMappingScopeType scope;
    final boolean useDefaults;

    /**
     * Item processing for given metadata items. Lazily evaluated.
     */
    private Map<ItemPath, ItemProcessingType> itemProcessingMap;

    private ItemValueMetadataProcessingSpec(@NotNull MetadataMappingScopeType scope, boolean useDefaults) {
        this.useDefaults = useDefaults;
        this.scope = scope;
    }

    public static ItemValueMetadataProcessingSpec forScope(@NotNull MetadataMappingScopeType scope) {
        return new ItemValueMetadataProcessingSpec(scope, true);
    }

    public static ItemValueMetadataProcessingSpec forScope(@NotNull MetadataMappingScopeType scope, boolean useDefaults) {
        return new ItemValueMetadataProcessingSpec(scope, useDefaults);
    }

    public void populateFromCurrentFocusTemplate(
            @NotNull ItemPath dataPath, ItemDefinition<?> dataDefinition, ObjectResolver objectResolver, String contextDesc,
            Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        populateFromCurrentFocusTemplate(
                ModelExpressionThreadLocalHolder.getLensContext(), dataPath,  dataDefinition, objectResolver, contextDesc, task, result);
    }

    public void populateFromCurrentFocusTemplate(
            ModelContext<?> lensContext, @NotNull ItemPath dataPath, ItemDefinition<?> dataDefinition, ObjectResolver objectResolver, String contextDesc,
            Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        if (lensContext != null) {
            ObjectTemplateType focusTemplate = lensContext.getFocusTemplate();




            if (focusTemplate != null) {
                addFromObjectTemplate(focusTemplate, dataPath, objectResolver, contextDesc, task, result);
            } else {
                LOGGER.trace("No focus template for {}, using default provenance handling", lensContext);
            }
            // should be provenance enabled by default?

            if (useDefaults &&
                    DefaultValueMetadataProcessing.forMetadataItem(ValueMetadataType.F_PROVENANCE).isEnabledFor(dataPath, dataDefinition)) {
                itemDefinitions.add(defaultProvenanceBehaviour());
            }
        } else {
            LOGGER.trace("No current lens context, no metadata handling");
        }
    }

    private MetadataItemDefinitionType defaultProvenanceBehaviour() {
        return new MetadataItemDefinitionType()
                .ref(new ItemPathType(ValueMetadataType.F_PROVENANCE))
                .limitations(new PropertyLimitationsType().processing(ItemProcessingType.FULL));
    }

    public boolean isEmpty() {
        return mappings.isEmpty() && itemDefinitions.isEmpty();
    }

    private void addFromObjectTemplate(
            @NotNull ObjectTemplateType rootTemplate, @NotNull ItemPath dataPath,
            ObjectResolver objectResolver, String contextDesc,
            Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        LOGGER.trace("Obtaining metadata handling instructions from {}", rootTemplate);

        // TODO resolve conflicts in included templates
        try {
            new ObjectTemplateIncludeProcessor(objectResolver)
                    .processThisAndIncludedTemplates(rootTemplate, contextDesc, task, result,
                            template -> addFromObjectTemplate(template, dataPath));
        } catch (TunnelException e) {
            if (e.getCause() instanceof SchemaException) {
                throw (SchemaException) e.getCause();
            } else {
                MiscUtil.unwrapTunnelledExceptionToRuntime(e);
            }
        }
    }

    private void addFromObjectTemplate(@NotNull ObjectTemplateType template, @NotNull ItemPath dataPath) {
        try {
            addHandling(template.getMeta(), dataPath);

            for (ObjectTemplateItemDefinitionType itemDef : template.getItem()) {
                ItemPathType ref = itemDef.getRef();
                if (ref != null && ref.getItemPath().equivalent(dataPath)) {
                    addHandling(itemDef.getMeta(), dataPath);
                }
            }
        } catch (SchemaException e) {
            throw new TunnelException(e);
        }
    }

    /** [EP:M:MM] DONE 2/2 Checked that `handling` is embedded in its originating object. */
    private void addHandling(@Nullable MetadataHandlingType handling, @NotNull ItemPath dataPath)
            throws SchemaException {
        if (isHandlingApplicable(handling, dataPath)) {
            addMetadataMappings(handling.getMapping(), ConfigurationItemOrigin::embedded);
            addMetadataItems(handling.getItem(), dataPath);
        }
    }

    private boolean isHandlingApplicable(MetadataHandlingType handling, ItemPath dataPath) throws SchemaException {
        return handling != null
                && ProcessingUtil.doesApplicabilityMatch(handling.getApplicability(), dataPath);
    }

    /** [EP:M:MM] Assumes that each item is embedded in its originating object. */
    private void addMetadataItems(List<MetadataItemDefinitionType> items, ItemPath dataPath)
            throws SchemaException {
        for (MetadataItemDefinitionType item : items) {
            if (isMetadataItemDefinitionApplicable(item, dataPath)) {
                itemDefinitions.add(item);
                for (MetadataMappingType itemMapping : item.getMapping()) {
                    if (isMetadataMappingApplicable(itemMapping)) {
                        mappings.add(
                                MetadataMappingConfigItem.of(
                                        provideDefaultTarget(itemMapping, item),
                                        ConfigurationItemOrigin.embedded(itemMapping))); // [EP:M:MM] DONE
                    }
                }
            }
        }
    }

    private boolean isMetadataItemDefinitionApplicable(MetadataItemDefinitionType item, ItemPath dataPath) throws SchemaException {
        return !isMetadataItemIgnored(item.getRef()) && ProcessingUtil.doesApplicabilityMatch(item.getApplicability(), dataPath);
    }

    private MetadataMappingType provideDefaultTarget(MetadataMappingType itemMapping, MetadataItemDefinitionType item) {
        VariableBindingDefinitionType targetSpec = itemMapping.getTarget();
        if (targetSpec != null && targetSpec.getPath() != null) {
            return itemMapping;
        } else {
            checkRefNotNull(item);
            MetadataMappingType clone = itemMapping.clone();
            if (clone.getTarget() == null) {
                clone.beginTarget().path(item.getRef());
            } else {
                clone.getTarget().path(item.getRef());
            }
            return clone;
        }
    }

    private void checkRefNotNull(MetadataItemDefinitionType item) {
        if (item.getRef() == null) {
            // todo better error handling
            throw new IllegalArgumentException("No `ref` value in item definition");
        }
    }

    public void addMetadataMappings(
            @NotNull List<MetadataMappingType> mappingsToAdd, @NotNull OriginProvider<MetadataMappingType> originProvider) {
        for (MetadataMappingType mapping : mappingsToAdd) {
            if (isMetadataMappingApplicable(mapping)) {
                mappings.add(
                        MetadataMappingConfigItem.of(mapping, originProvider)); // [EP:M:MM] DONE 2/2
            }
        }
    }

    public @NotNull Collection<MetadataMappingConfigItem> getMappings() {
        return Collections.unmodifiableCollection(mappings);
    }

    private void computeItemProcessingMapIfNeeded() throws SchemaException, ConfigurationException {
        if (itemProcessingMap == null) {
            computeItemProcessingMap();
        }
    }

    private void computeItemProcessingMap() throws SchemaException, ConfigurationException {
        itemProcessingMap = new HashMap<>();
        for (MetadataItemDefinitionType item : itemDefinitions) {
            ItemProcessingType processing = getProcessingOfItem(item);
            if (processing != null) {
                itemProcessingMap.put(
                        ItemRefinedDefinitionTypeUtil.getRef(item),
                        processing);
            }
        }
    }

    /**
     * Looks up processing information for given path. Proceeds from the most specific to most abstract (empty) path.
     */
    private ItemProcessingType getProcessing(ItemPath itemPath) throws SchemaException, ConfigurationException {
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

    public boolean isFullProcessing(ItemPath itemPath) throws SchemaException, ConfigurationException {
        return getProcessing(itemPath) == ItemProcessingType.FULL;
    }

    Collection<ItemPath> getTransientPaths() {
        PathSet transientPaths = new PathSet();
        PathSet persistentPaths = new PathSet();
        for (MetadataItemDefinitionType item : itemDefinitions) {
            ItemPersistenceType persistence = item.getPersistence();
            if (persistence != null) {
                ItemPath path = item.getRef().getItemPath();
                if (persistence == ItemPersistenceType.PERSISTENT) {
                    persistentPaths.add(path);
                } else {
                    transientPaths.add(path);
                }
            }
        }
        for (ItemPath persistentPath : persistentPaths) {
            transientPaths.remove(persistentPath);
        }
        return transientPaths;
    }

    // For item-contained mappings the target exclusion can be checked twice
    private boolean isMetadataMappingApplicable(MetadataMappingType mapping) {
        return hasCompatibleScope(mapping) &&
                (mapping.getTarget() == null || mapping.getTarget().getPath() == null
                    || isMetadataItemIgnored(mapping.getTarget().getPath()));
    }

    private boolean hasCompatibleScope(MetadataMappingType metadataMapping) {
        MetadataMappingScopeType mappingScope = metadataMapping.getScope();
        return mappingScope == null || mappingScope == scope;
    }

    public @NotNull MetadataMappingScopeType getScope() {
        return scope;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(String.format("%d mapping(s), %d item definition(s)", mappings.size(), itemDefinitions.size()));
        if (!itemDefinitions.isEmpty()) {
            sb.append(" for ");
            sb.append(itemDefinitions.stream()
                    .map(ItemRefinedDefinitionType::getRef)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ")));
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.debugDumpWithLabelLn(sb, "Mappings defined",
                mappings.stream()
                        .map(m -> getTargetPath(m.value()))
                        .collect(Collectors.toList()), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Items defined",
                itemDefinitions.stream()
                        .map(ItemRefinedDefinitionType::getRef)
                        .collect(Collectors.toList()), indent);
        DebugUtil.debugDumpWithLabel(sb, "Paths that were ignored",
                metadataPathsToIgnore, indent);
        return sb.toString();
    }

    private ItemPathType getTargetPath(@NotNull MetadataMappingType mapping) {
        VariableBindingDefinitionType targetSpec = mapping.getTarget();
        return targetSpec != null ? targetSpec.getPath() : null;
    }

    public void addPathsToIgnore(@NotNull List<ItemPathType> pathsToIgnore) {
        pathsToIgnore.forEach(path -> this.metadataPathsToIgnore.add(path.getItemPath()));
    }

    private boolean isMetadataItemIgnored(ItemPathType pathBean) {
        if (metadataPathsToIgnore.isEmpty()) {
            return false;
        } else {
            ItemPath path = Objects.requireNonNull(pathBean, "'ref' is null").getItemPath();
            return ItemPathCollectionsUtil.containsSubpathOrEquivalent(metadataPathsToIgnore, path);
        }
    }
}
