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
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.MappingEvaluationRequests;
import com.evolveum.midpoint.prism.delta.ContainerDelta;

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
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Collection of {@link MappedItem}s: they will produce prepared {@link MappingImpl}s.
 */
class MappedItems<T extends Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(MappedItems.class);

    @NotNull private final MappingSource source;

    @NotNull private final MappingTarget<T> target;

    @NotNull private final MappingContext context;

    @NotNull private final List<MappedItem<?, ?, T>> mappedItems = new ArrayList<>();

    /**
     * Definition of `auxiliaryObjectClass` property in shadows.
     */
    private final Lazy<PrismPropertyDefinition<QName>> lazyAuxiliaryObjectClassPropertyDefinition =
            Lazy.from(() -> PrismContext.get().getSchemaRegistry()
                    .findObjectDefinitionByCompileTimeClass(ShadowType.class)
                    .findPropertyDefinition(ShadowType.F_AUXILIARY_OBJECT_CLASS));

    MappedItems(@NotNull MappingSource source, @NotNull MappingTarget<T> target, @NotNull MappingContext context) {
        this.source = source;
        this.target = target;
        this.context = context;
    }

    /**
     * Here we create all the mapped items.
     *
     * This excludes special mappings. They are _evaluated_ later. (This is planned to be changed!)
     */
    void collectMappedItems() throws SchemaException, ConfigurationException {

        var sourceInboundDefinition = source.getInboundDefinition();

        for (var simpleAttributeDef : source.getSimpleAttributeDefinitions()) {
            createMappedItemForSimpleAttribute(
                    simpleAttributeDef,
                    sourceInboundDefinition.getSimpleAttributeInboundDefinition(simpleAttributeDef.getItemName()));
        }

        for (var refAttributeDef : source.getObjectReferenceAttributeDefinitions()) {
            createMappedItemForReferenceAttribute(
                    refAttributeDef,
                    sourceInboundDefinition.getReferenceAttributeInboundDefinition(refAttributeDef.getItemName()));
        }

        for (var assocDef : source.getAssociationDefinitions()) {
            createMappedItemForAssociationExplicitInbounds(assocDef);
        }

        // Note that associations are treated later

        // FIXME Remove this temporary check
        if (!source.isClockwork()) {
            LOGGER.trace("Skipping processing of aux object classes mappings because of limited stage");
            return;
        }

        if (!source.isProjectionBeingDeleted()) {
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
            ShadowSimpleAttributeDefinition<TA> attributeDefinition,
            ItemInboundDefinition attributeInboundDefinition)
            throws SchemaException, ConfigurationException {

        if (attributeInboundDefinition == null) {
            return;
        }

        // 1. Definitions and mapping beans
        List<InboundMappingType> inboundMappingBeans = attributeInboundDefinition.getInboundMappingBeans();
        if (inboundMappingBeans.isEmpty()) {
            return;
        }

        var attributeName = attributeDefinition.getItemName();
        List<InboundMappingConfigItem> applicableMappings = // [EP:M:IM] DONE beans are really from the resource
                source.selectMappingBeansForEvaluationPhase(
                        createMappingCIs(inboundMappingBeans),
                        attributeInboundDefinition.getCorrelatorDefinition() != null,
                        context.getCorrelationItemPaths());
        if (applicableMappings.isEmpty()) {
            LOGGER.trace("No applicable beans for this phase");
            return;
        }

        ItemPath attributePath = ItemPath.create(ShadowType.F_ATTRIBUTES, attributeName);
        String itemDescription = "attribute " + attributeName;

        // 2. Values

        ItemDelta<PrismPropertyValue<TA>, PrismPropertyDefinition<TA>> attributeAPrioriDelta =
                source.sourceData.getItemAPrioriDelta(attributePath);

        // 3. Processing source

        ProcessingMode processingMode = source.getItemProcessingMode(
                itemDescription,
                attributeAPrioriDelta,
                applicableMappings,
                attributeDefinition.isVisible(context.getTaskExecutionMode()),
                attributeDefinition.isIgnored(LayerType.MODEL),
                attributeDefinition.getLimitations(LayerType.MODEL));
        if (processingMode == ProcessingMode.NONE) {
            return;
        }

        // 4. Mapped item

        mappedItems.add(
                new MappedItem<>(
                        source,
                        target,
                        context,
                        applicableMappings, // [EP:M:IM] DONE beans are from the resource
                        attributePath,
                        itemDescription,
                        attributeAPrioriDelta,
                        attributeDefinition,
                        () -> source.sourceData.getSimpleAttribute(attributeName),
                        null, // postprocessor
                        null, // variable producer
                        processingMode));
    }

    /**
     * Creates a {@link MappedItem} for given association.
     *
     * @see #createMappedItemForSimpleAttribute(ShadowSimpleAttributeDefinition, ItemInboundDefinition)
     * @see #createMappedItemForAuxObjectClasses()
     */
    private void createMappedItemForReferenceAttribute(
            ShadowReferenceAttributeDefinition refAttrDef,
            ItemInboundDefinition attributeInboundDefinition)
            throws SchemaException, ConfigurationException {

        if (attributeInboundDefinition == null) {
            return;
        }

        // 1. Definitions

        var inboundMappingBeans = attributeInboundDefinition.getInboundMappingBeans();
        if (inboundMappingBeans.isEmpty()) {
            return;
        }

        var refAttrName = refAttrDef.getItemName();
        ItemPath itemPath = ShadowType.F_ATTRIBUTES.append(refAttrName);
        String itemDescription = "reference attribute " + refAttrName;
        List<InboundMappingConfigItem> applicableMappings =
                source.selectMappingBeansForEvaluationPhase(
                        createMappingCIs(inboundMappingBeans),
                        attributeInboundDefinition.getCorrelatorDefinition() != null,
                        context.getCorrelationItemPaths());
        if (applicableMappings.isEmpty()) {
            LOGGER.trace("No applicable beans for this phase");
            return;
        }

        // 2. Values

        ItemDelta<ShadowReferenceAttributeValue, ShadowReferenceAttributeDefinition> refAttrAPrioriDelta =
                source.sourceData.getItemAPrioriDelta(itemPath);

        // 3. Processing source

        ProcessingMode processingMode = source.getItemProcessingMode(
                itemDescription,
                refAttrAPrioriDelta,
                applicableMappings,
                refAttrDef.isVisible(context),
                refAttrDef.isIgnored(LayerType.MODEL),
                refAttrDef.getLimitations(LayerType.MODEL));
        if (processingMode == ProcessingMode.NONE) {
            return;
        }

        // 4. Mapped item

        //noinspection rawtypes,unchecked
        mappedItems.add(
                new MappedItem<>(
                        source,
                        target,
                        context,
                        applicableMappings, // [EP:M:IM] DONE mappings are from the resource
                        itemPath, // source path (cannot point to specified association name!)
                        itemDescription,
                        refAttrAPrioriDelta,
                        refAttrDef,
                        () -> (Item) source.sourceData.getReferenceAttribute(refAttrName),
                        null,
                        null,
                        processingMode));
    }

    /**
     * Creates a {@link MappedItem} for given (legacy) association.
     *
     * @see #createMappedItemForSimpleAttribute(ShadowSimpleAttributeDefinition, ItemInboundDefinition)
     * @see #createMappedItemForAuxObjectClasses()
     */
    private void createMappedItemForAssociationExplicitInbounds(ShadowAssociationDefinition assocDef)
            throws SchemaException, ConfigurationException {

        // 1. Definitions

        var inboundMappingBeans = assocDef.getExplicitInboundMappingBeans();
        if (inboundMappingBeans.isEmpty()) {
            return;
        }

        var assocName = assocDef.getItemName();
        ItemPath itemPath = ShadowType.F_ASSOCIATIONS.append(assocName);
        String itemDescription = "association " + assocName;
        List<InboundMappingConfigItem> applicableMappings =
                source.selectMappingBeansForEvaluationPhase(
                        createMappingCIs(inboundMappingBeans),
                        false, // association cannot be a correlator (currently)
                        context.getCorrelationItemPaths());
        if (applicableMappings.isEmpty()) {
            LOGGER.trace("No applicable beans for this phase");
            return;
        }

        // 2. Values

        ItemDelta<ShadowAssociationValue, ShadowAssociationDefinition> assocAPrioriDelta =
                source.sourceData.getItemAPrioriDelta(itemPath);

        MappedItem.ItemProvider<ShadowAssociationValue, ShadowAssociationDefinition>
                associationProvider = () -> {
                    //noinspection unchecked,rawtypes
                    return (Item) MappedItems.this.source.sourceData.getAssociation(assocName);
                };
        MappedItem.PostProcessor<ShadowAssociationValue, ShadowAssociationDefinition> associationPostProcessor =
                (aPrioriDelta, currentItem) -> {
                    // FIXME remove this ugly hacking
                    //noinspection unchecked,rawtypes
                    source.resolveInputEntitlements(
                            (ContainerDelta<ShadowAssociationValueType>) (ContainerDelta) aPrioriDelta,
                            (ShadowAssociation) (Item) currentItem);
                };

        // 3. Processing source

        ProcessingMode processingMode = source.getItemProcessingMode(
                itemDescription,
                assocAPrioriDelta,
                applicableMappings,
                assocDef.isVisible(context),
                assocDef.getReferenceAttributeDefinition().isIgnored(LayerType.MODEL),
                assocDef.getReferenceAttributeDefinition().getLimitations(LayerType.MODEL));
        if (processingMode == ProcessingMode.NONE) {
            return;
        }

        // 4. Mapped item

        mappedItems.add(
                new MappedItem<>(
                        source,
                        target,
                        context,
                        applicableMappings, // [EP:M:IM] DONE mappings are from the resource
                        itemPath, // source path (cannot point to specified association name!)
                        itemDescription,
                        assocAPrioriDelta,
                        assocDef,
                        associationProvider,
                        associationPostProcessor,
                        source::getEntitlementVariableProducer, // so-called variable producer
                        processingMode));
    }

    private List<InboundMappingConfigItem> createMappingCIs(Collection<InboundMappingType> inboundMappingBeans) {
        return InboundMappingConfigItem.ofList(
                List.copyOf(inboundMappingBeans),
                item -> ConfigurationItemOrigin.inResourceOrAncestor(source.getResource()),
                InboundMappingConfigItem.class);
    }

    /**
     * Creates a {@link MappedItem} for "auxiliary object classes" property.
     *
     * @see #createMappedItemForSimpleAttribute(ShadowSimpleAttributeDefinition, ItemInboundDefinition)
     * @see #createMappedItemForReferenceAttribute(ShadowReferenceAttributeDefinition, ItemInboundDefinition)
     */
    private void createMappedItemForAuxObjectClasses() throws SchemaException, ConfigurationException {

        // 1. Definitions

        ItemName itemPath = ShadowType.F_AUXILIARY_OBJECT_CLASS;
        String itemDescription = "auxiliary object classes";

        var auxiliaryObjectClassMappings = source.inboundDefinition.getAuxiliaryObjectClassMappings();
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
                item -> ConfigurationItemOrigin.inResourceOrAncestor(source.getResource()),
                MappingConfigItem.class);

        // 2. Values

        ItemDelta<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>> itemAPrioriDelta =
                source.sourceData.getItemAPrioriDelta(itemPath);

        MappedItem.ItemProvider<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>> itemProvider =
                () -> source.sourceData.getAuxiliaryObjectClasses();

        // 3. Processing source

        ProcessingMode processingMode = source.getItemProcessingMode(
                itemDescription,
                itemAPrioriDelta,
                mappings,
                true, // aux OCs are always visible
                false, // aux OCs are never ignored
                null); // aux OCs are never unreadable
        if (processingMode == ProcessingMode.NONE) {
            return;
        }

        // 4. Mapped item

        // Note that we intentionally ignore a-priori delta for aux OCs. The reason is unknown for me, but
        // this is how it was done before 4.5.
        //
        // We also insist on fetching the full shadow, unless getProcessingSource() has told us that we should not process
        // these mappings.
        //
        // TODO reconsider these irregularities in behavior (comparing with attribute/association mappings).

        mappedItems.add(
                new MappedItem<>(
                        source,
                        target,
                        context,
                        mappings, // [EP:M:IM] DONE mappings are from the resource
                        itemPath,
                        itemDescription,
                        null, // ignoring a priori delta
                        lazyAuxiliaryObjectClassPropertyDefinition.get(),
                        itemProvider,
                        null, // postprocessor
                        null, // variable producer
                        ProcessingMode.ABSOLUTE_STATE));
    }

    boolean isFullStateRequired() {
        for (MappedItem<?, ?, ?> mappedItem : mappedItems) {
            if (mappedItem.doesRequireAbsoluteState()) {
                LOGGER.trace("The mapping(s) for {} require the absolute state, we'll load it if it will be necessary",
                        mappedItem.itemDescription);
                return true;
            }
        }
        return false;
    }

    void createMappings(MappingEvaluationRequests evaluationRequestsBeingCollected, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        for (var mappedItem : mappedItems) {
            mappedItem.createMappings(evaluationRequestsBeingCollected, result);
        }
    }
}
