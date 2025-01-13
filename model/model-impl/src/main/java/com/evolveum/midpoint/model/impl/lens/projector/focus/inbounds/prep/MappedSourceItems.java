/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectInboundProcessingDefinition.CompleteItemInboundDefinition;

import com.evolveum.midpoint.schema.processor.ShadowItemDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.MappingEvaluationRequestsMap;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.InboundMappingConfigItem;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Collection of {@link MappedSourceItem}s: they will produce prepared {@link MappingImpl}s.
 */
class MappedSourceItems<T extends Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(MappedSourceItems.class);

    @NotNull private final InboundsSource inboundsSource;

    @NotNull private final InboundsTarget<T> inboundsTarget;

    @NotNull private final InboundsContext inboundsContext;

    @NotNull private final List<MappedSourceItem<?, ?, T>> mappedSourceItems = new ArrayList<>();

    /**
     * Definition of `auxiliaryObjectClass` property in shadows.
     */
    private final Lazy<PrismPropertyDefinition<QName>> lazyAuxiliaryObjectClassPropertyDefinition =
            Lazy.from(() ->
                    Objects.requireNonNull(
                            PrismContext.get().getSchemaRegistry()
                                    .findObjectDefinitionByCompileTimeClass(ShadowType.class)
                                    .findPropertyDefinition(ShadowType.F_AUXILIARY_OBJECT_CLASS)));

    MappedSourceItems(
            @NotNull InboundsSource inboundsSource,
            @NotNull InboundsTarget<T> inboundsTarget,
            @NotNull InboundsContext inboundsContext) {
        this.inboundsSource = inboundsSource;
        this.inboundsTarget = inboundsTarget;
        this.inboundsContext = inboundsContext;
    }

    /**
     * Here we create all the mapped items.
     *
     * This excludes special mappings. They are _evaluated_ later. (This is planned to be changed!)
     */
    void collectMappedItems() throws SchemaException, ConfigurationException {

        var sourceInboundDefinition = inboundsSource.getInboundProcessingDefinition();
        for (var shadowItemInboundDefinition : sourceInboundDefinition.getItemInboundDefinitions()) {
            createMappedItemForShadowItem(shadowItemInboundDefinition);
        }

        // FIXME Remove this temporary check
        if (!inboundsSource.isClockwork()) {
            LOGGER.trace("Skipping processing of aux object classes mappings because of limited stage");
            return;
        }

        if (!inboundsSource.isProjectionBeingDeleted()) {
            createMappedItemForAuxObjectClasses();
        } else {
            // TODO why we are skipping evaluation of aux OCs below?
            LOGGER.trace("Skipping application of aux object class mappings because of projection DELETE delta");
        }
    }

    /**
     * Creates a {@link MappedSourceItem} for given attribute or association.
     */
    private void createMappedItemForShadowItem(
            @NotNull CompleteItemInboundDefinition completeItemInboundDefinition)
            throws SchemaException, ConfigurationException {

        var itemPath = completeItemInboundDefinition.path();
        var processingDefinition = completeItemInboundDefinition.inboundProcessingDefinition();
        var itemDefinition = completeItemInboundDefinition.itemDefinition();

        var inboundMappingBeans = processingDefinition.getInboundMappingBeans();
        if (inboundMappingBeans.isEmpty()) {
            return;
        }

        var applicableMappingCIs = // [EP:M:IM] DONE beans are really from the resource
                inboundsSource.selectMappingBeansForEvaluationPhase(
                        itemPath,
                        createMappingCIs(inboundMappingBeans),
                        processingDefinition.getCorrelatorDefinition() != null,
                        inboundsContext.getCorrelationItemPaths());
        if (applicableMappingCIs.isEmpty()) {
            LOGGER.trace("{}: No applicable beans for this phase", itemPath);
            return;
        }
        if (!isItemProcessable(itemPath, itemDefinition)) {
            return;
        }

        mappedSourceItems.add(
                new MappedSourceItem<>(
                        inboundsSource,
                        inboundsTarget,
                        inboundsContext,
                        applicableMappingCIs, // [EP:M:IM] DONE beans are from the resource
                        itemPath,
                        (ItemDefinition<?>) itemDefinition));
    }

    /**
     * Returns true if the mapping(s) for given item on this source should be skipped because of item restrictions
     * or obviously missing data.
     */
    private boolean isItemProcessable(ItemPath itemPath, ShadowItemDefinition itemDefinition) {

        if (!itemDefinition.isVisible(inboundsContext)) {
            LOGGER.trace("Inbound mapping(s) for {} will be skipped because the item is not visible in current execution mode",
                    itemPath);
            return false;
        }
        if (itemDefinition.isIgnored(LayerType.MODEL)) {
            LOGGER.trace("Inbound mapping(s) for {} will be skipped because the item is ignored", itemPath);
            return false;
        }
        var limitations = itemDefinition.getLimitations(LayerType.MODEL);
        if (limitations != null && !limitations.canRead()) {
            LOGGER.warn("Skipping inbound mapping(s) for {} in {} because it is not readable",
                    itemPath, inboundsSource.getProjectionHumanReadableName());
            return false;
        }
        if (inboundsSource.sourceData.isNoShadow() && inboundsSource.sourceData.getEffectiveItemDelta(itemPath) == null) {
            LOGGER.trace("Inbound mapping(s) for {} will be skipped because there is no shadow (not even repo version),"
                    + "and no delta for the item", itemPath);
            return false;
        }
        return true;
    }

    private List<InboundMappingConfigItem> createMappingCIs(Collection<InboundMappingType> inboundMappingBeans) {
        return InboundMappingConfigItem.ofList(
                List.copyOf(inboundMappingBeans),
                item -> ConfigurationItemOrigin.inResourceOrAncestor(inboundsSource.getResource()),
                InboundMappingConfigItem.class);
    }

    /**
     * Creates a {@link MappedSourceItem} for "auxiliary object classes" property.
     *
     * @see #createMappedItemForShadowItem(CompleteItemInboundDefinition)
     */
    private void createMappedItemForAuxObjectClasses() throws SchemaException, ConfigurationException {

        // TODO add filtering after these mappings are promoted to InboundMappingType
        List<MappingType> mappingBeans = inboundsSource.inboundProcessingDefinition.getAuxiliaryObjectClassInboundMappings();
        if (mappingBeans.isEmpty()) {
            return;
        }

        var mappingCIs = MappingConfigItem.ofList(
                mappingBeans,
                item -> ConfigurationItemOrigin.inResourceOrAncestor(inboundsSource.getResource()),
                MappingConfigItem.class);

        mappedSourceItems.add(
                new MappedSourceItem<>(
                        inboundsSource,
                        inboundsTarget,
                        inboundsContext,
                        mappingCIs, // [EP:M:IM] DONE mappings are from the resource
                        ShadowType.F_AUXILIARY_OBJECT_CLASS,
                        lazyAuxiliaryObjectClassPropertyDefinition.get()));
    }

    void createMappings(MappingEvaluationRequestsMap evaluationRequestsBeingCollected, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        for (var mappedItem : mappedSourceItems) {
            mappedItem.createMappings(evaluationRequestsBeingCollected, result);
        }
    }

    @NotNull List<MappedSourceItem<?, ?, T>> getItemsRequiringCurrentValue() {
        return mappedSourceItems.stream()
                .filter(i -> i.isRequiringCurrentValue())
                .toList();
    }

    @NotNull List<MappedSourceItem<?, ?, T>> getItemsRequiringCurrentValueAndNotHavingIt()
            throws SchemaException, ConfigurationException {
        var rv = new ArrayList<MappedSourceItem<?, ?, T>>();
        for (var i : mappedSourceItems) {
            if (i.isRequiringCurrentValue() && !i.isLoaded()) {
                rv.add(i);
            }
        }
        return rv;
    }
}
