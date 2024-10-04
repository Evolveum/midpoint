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

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.MappingEvaluationRequestsMap;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.InboundMappingConfigItem;
import com.evolveum.midpoint.schema.config.MappingConfigItem;

import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.schema.processor.ResourceObjectInboundDefinition.ItemInboundDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

        var sourceInboundDefinition = inboundsSource.getInboundDefinition();

        for (var simpleAttributeDef : inboundsSource.getSimpleAttributeDefinitions()) {
            createMappedItemForSimpleAttribute(
                    simpleAttributeDef,
                    sourceInboundDefinition.getSimpleAttributeInboundDefinition(simpleAttributeDef.getItemName()));
        }

        for (var refAttributeDef : inboundsSource.getObjectReferenceAttributeDefinitions()) {
            createMappedItemForReferenceAttribute(
                    refAttributeDef,
                    sourceInboundDefinition.getReferenceAttributeInboundDefinition(refAttributeDef.getItemName()));
        }

        for (var assocDef : inboundsSource.getAssociationDefinitions()) {
            createMappedItemForAssociation(assocDef);
        }

        // Note that associations are treated later

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
     * Creates a mapping creation request for mapping(s) for given attribute.
     *
     * @param <TA> type of the attribute
     * @see #createMappedItemForReferenceAttribute(ShadowReferenceAttributeDefinition, ItemInboundDefinition)
     * @see #createMappedItemForAuxObjectClasses()
     */
    private <TA> void createMappedItemForSimpleAttribute(
            @NotNull ShadowSimpleAttributeDefinition<TA> attributeDefinition,
            ItemInboundDefinition attributeInboundDefinition)
            throws SchemaException, ConfigurationException {

        if (attributeInboundDefinition == null) {
            return;
        }

        List<InboundMappingType> inboundMappingBeans = attributeInboundDefinition.getInboundMappingBeans();
        if (inboundMappingBeans.isEmpty()) {
            return;
        }

        var attributeName = attributeDefinition.getItemName();
        var itemPath = ItemPath.create(ShadowType.F_ATTRIBUTES, attributeName);

        List<InboundMappingConfigItem> applicableMappings = // [EP:M:IM] DONE beans are really from the resource
                inboundsSource.selectMappingBeansForEvaluationPhase(
                        itemPath,
                        createMappingCIs(inboundMappingBeans),
                        attributeInboundDefinition.getCorrelatorDefinition() != null,
                        inboundsContext.getCorrelationItemPaths());
        if (applicableMappings.isEmpty()) {
            LOGGER.trace("{}: No applicable beans for this phase", itemPath);
            return;
        }

        if (inboundsSource.isItemNotProcessable(
                itemPath,
                attributeDefinition.isVisible(inboundsContext),
                attributeDefinition.isIgnored(LayerType.MODEL),
                attributeDefinition.getLimitations(LayerType.MODEL))) {
            return;
        }

        mappedSourceItems.add(
                new MappedSourceItem<>(
                        inboundsSource,
                        inboundsTarget,
                        inboundsContext,
                        applicableMappings, // [EP:M:IM] DONE beans are from the resource
                        itemPath,
                        attributeDefinition,
                        () -> inboundsSource.sourceData.getSimpleAttribute(attributeName),
                        () -> inboundsSource.isAttributeAvailable(attributeDefinition.getItemName())
                ));
    }

    /**
     * Creates a {@link MappedSourceItem} for given association.
     *
     * @see #createMappedItemForSimpleAttribute(ShadowSimpleAttributeDefinition, ItemInboundDefinition)
     * @see #createMappedItemForAuxObjectClasses()
     */
    private void createMappedItemForReferenceAttribute(
            @NotNull ShadowReferenceAttributeDefinition refAttrDef,
            ItemInboundDefinition attributeInboundDefinition)
            throws SchemaException, ConfigurationException {

        if (attributeInboundDefinition == null) {
            return;
        }

        var inboundMappingBeans = attributeInboundDefinition.getInboundMappingBeans();
        if (inboundMappingBeans.isEmpty()) {
            return;
        }

        var refAttrName = refAttrDef.getItemName();
        var itemPath = ShadowType.F_ATTRIBUTES.append(refAttrName);

        List<InboundMappingConfigItem> applicableMappings =
                inboundsSource.selectMappingBeansForEvaluationPhase(
                        itemPath,
                        createMappingCIs(inboundMappingBeans),
                        attributeInboundDefinition.getCorrelatorDefinition() != null,
                        inboundsContext.getCorrelationItemPaths());
        if (applicableMappings.isEmpty()) {
            LOGGER.trace("{}: No applicable beans for this phase", itemPath);
            return;
        }

        if (inboundsSource.isItemNotProcessable(
                itemPath,
                refAttrDef.isVisible(inboundsContext),
                refAttrDef.isIgnored(LayerType.MODEL),
                refAttrDef.getLimitations(LayerType.MODEL))) {
            return;
        }

        //noinspection rawtypes,unchecked
        mappedSourceItems.add(
                new MappedSourceItem<>(
                        inboundsSource,
                        inboundsTarget,
                        inboundsContext,
                        applicableMappings, // [EP:M:IM] DONE mappings are from the resource
                        itemPath, // source path (cannot point to specified association name!)
                        refAttrDef,
                        () -> (Item) inboundsSource.sourceData.getReferenceAttribute(refAttrName),
                        () -> inboundsSource.isAttributeAvailable(refAttrName)));
    }

    /**
     * Creates a {@link MappedSourceItem} for given (legacy) association.
     *
     * @see #createMappedItemForSimpleAttribute(ShadowSimpleAttributeDefinition, ItemInboundDefinition)
     * @see #createMappedItemForAuxObjectClasses()
     */
    private void createMappedItemForAssociation(@NotNull ShadowAssociationDefinition assocDef)
            throws SchemaException, ConfigurationException {

        var inboundMappingBeans = assocDef.getInboundMappingBeans();
        if (inboundMappingBeans.isEmpty()) {
            return;
        }

        var assocName = assocDef.getItemName();
        var itemPath = ShadowType.F_ASSOCIATIONS.append(assocName);

        List<InboundMappingConfigItem> applicableMappings =
                inboundsSource.selectMappingBeansForEvaluationPhase(
                        itemPath,
                        createMappingCIs(inboundMappingBeans),
                        false, // association cannot be a correlator (currently)
                        inboundsContext.getCorrelationItemPaths());
        if (applicableMappings.isEmpty()) {
            LOGGER.trace("{}: No applicable beans for this phase", itemPath);
            return;
        }

        MappedSourceItem.ItemProvider<ShadowAssociationValue, ShadowAssociationDefinition>
                associationProvider = () -> {
            //noinspection unchecked,rawtypes
            return (Item) MappedSourceItems.this.inboundsSource.sourceData.getAssociation(assocName);
        };

        if (inboundsSource.isItemNotProcessable(
                itemPath,
                assocDef.isVisible(inboundsContext),
                assocDef.getReferenceAttributeDefinition().isIgnored(LayerType.MODEL),
                assocDef.getReferenceAttributeDefinition().getLimitations(LayerType.MODEL))) {
            return;
        }

        mappedSourceItems.add(
                new MappedSourceItem<>(
                        inboundsSource,
                        inboundsTarget,
                        inboundsContext,
                        applicableMappings, // [EP:M:IM] DONE mappings are from the resource
                        itemPath, // source path (cannot point to specified association name!)
                        assocDef,
                        associationProvider,
                        () -> inboundsSource.isAssociationAvailable(assocName)));
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
     * @see #createMappedItemForSimpleAttribute(ShadowSimpleAttributeDefinition, ItemInboundDefinition)
     * @see #createMappedItemForReferenceAttribute(ShadowReferenceAttributeDefinition, ItemInboundDefinition)
     */
    private void createMappedItemForAuxObjectClasses() throws SchemaException, ConfigurationException {

        ItemName itemPath = ShadowType.F_AUXILIARY_OBJECT_CLASS;

        var auxiliaryObjectClassMappings = inboundsSource.inboundDefinition.getAuxiliaryObjectClassMappings();
        if (auxiliaryObjectClassMappings == null) {
            return;
        }
        // TODO add filtering after these mappings are promoted to InboundMappingType
        List<MappingType> mappingBeans = auxiliaryObjectClassMappings.getInbound();
        if (mappingBeans.isEmpty()) {
            return;
        }

        var mappings = MappingConfigItem.ofList(
                mappingBeans,
                item -> ConfigurationItemOrigin.inResourceOrAncestor(inboundsSource.getResource()),
                MappingConfigItem.class);

        MappedSourceItem.ItemProvider<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>> itemProvider =
                () -> inboundsSource.sourceData.getAuxiliaryObjectClasses();

        mappedSourceItems.add(
                new MappedSourceItem<>(
                        inboundsSource,
                        inboundsTarget,
                        inboundsContext,
                        mappings, // [EP:M:IM] DONE mappings are from the resource
                        itemPath,
                        lazyAuxiliaryObjectClassPropertyDefinition.get(),
                        itemProvider,
                        () -> inboundsSource.isAuxiliaryObjectClassPropertyLoaded()
                ));
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
            if (i.isRequiringCurrentValue() && !i.hasCurrentValue()) {
                rv.add(i);
            }
        }
        return rv;
    }
}
