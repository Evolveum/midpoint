/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.schema.processor.SynchronizationReactionDefinition.ItemSynchronizationReactionDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Defines "complex inbound processing": correlation, synchronization reactions, inbounds for attributes and associations.
 *
 * There are three main flavors:
 *
 * . standard {@link ResourceObjectDefinition}
 * . for associations, based on {@link ShadowAssociationDefinitionType}
 * . for structured attributes, based on {@link ComplexProcessingType}
 *
 * Currently, the processing assumes that we have a shadow as an input. It is either the regular shadow coming from
 * a resource, or an embedded shadow in the case of associations.
 *
 * TEMPORARY
 */
public interface ResourceObjectInboundDefinition extends Serializable, DebugDumpable {

    static ResourceObjectInboundDefinition empty() {
        return new EmptyImplementation();
    }

    static ResourceObjectInboundDefinition forComplexProcessing(@Nullable ComplexProcessingType bean) {
        return bean != null ? new ComplexProcessingImplementation(bean) : empty();
    }

    static ResourceObjectInboundDefinition forAssociation(@Nullable ShadowAssociationDefinitionType bean) {
        return bean != null ? new AssociationProcessingImplementation(bean) : empty();
    }

    Collection<? extends ItemInboundDefinition> getAttributeDefinitions();

    ItemInboundDefinition getSimpleAttributeInboundDefinition(ItemName itemName) throws SchemaException;

    ItemInboundDefinition getReferenceAttributeInboundDefinition(ItemName itemName) throws SchemaException;

    ResourceBidirectionalMappingType getActivationBidirectionalMappingType(ItemName itemName);

    ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings();

    DefaultInboundMappingEvaluationPhasesType getDefaultInboundMappingEvaluationPhases();

    @NotNull List<MappingType> getPasswordInbound();

    @NotNull FocusSpecification getFocusSpecification();

    @NotNull Collection<? extends SynchronizationReactionDefinition> getSynchronizationReactions();

    @Nullable CorrelationDefinitionType getCorrelation();

    // TEMPORARY FIXME define the semantics
    boolean hasAnyInbounds();

    default @Nullable ItemInboundDefinition getDefaultObjectRefDefinition() {
        return null;
    }

    interface ItemInboundDefinition extends Serializable {

        @NotNull List<InboundMappingType> getInboundMappingBeans();

        ItemCorrelatorDefinitionType getCorrelatorDefinition();
    }

    class EmptyImplementation implements ResourceObjectInboundDefinition {

        @Override
        public Collection<? extends ItemInboundDefinition> getAttributeDefinitions() {
            return List.of();
        }

        @Override
        public ItemInboundDefinition getSimpleAttributeInboundDefinition(ItemName itemName) {
            return null;
        }

        @Override
        public ItemInboundDefinition getReferenceAttributeInboundDefinition(ItemName itemName) {
            return null;
        }

        @Override
        public ResourceBidirectionalMappingType getActivationBidirectionalMappingType(ItemName itemName) {
            return null;
        }

        @Override
        public ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings() {
            return null;
        }

        @Override
        public DefaultInboundMappingEvaluationPhasesType getDefaultInboundMappingEvaluationPhases() {
            return null;
        }

        @Override
        public @NotNull List<MappingType> getPasswordInbound() {
            return List.of();
        }

        @Override
        public @NotNull FocusSpecification getFocusSpecification() {
            return FocusSpecification.empty();
        }

        @Override
        public @NotNull Collection<? extends SynchronizationReactionDefinition> getSynchronizationReactions() {
            return List.of();
        }

        @Override
        public CorrelationDefinitionType getCorrelation() {
            return null;
        }

        @Override
        public boolean hasAnyInbounds() {
            return false;
        }

        @Override
        public String debugDump(int indent) {
            return "EMPTY"; // FIXME
        }
    }

    class ComplexProcessingImplementation implements ResourceObjectInboundDefinition {

        @NotNull private final ComplexProcessingType definitionBean;

        @NotNull private final PathKeyedMap<ItemInboundDefinition> itemDefinitionsMap = new PathKeyedMap<>();

        @NotNull private final Collection<ItemSynchronizationReactionDefinition> synchronizationReactionDefinitions;

        /** This is the inbound provided by "ref = '.'", i.e., related to the association value itself. */
        @Nullable private final ItemInboundDefinition associationValueInboundDefinition;

        ComplexProcessingImplementation(@NotNull ComplexProcessingType definitionBean) {
            this.definitionBean = definitionBean;
            for (var itemDefBean : definitionBean.getAttribute()) {
                var itemName = ItemPathTypeUtil.asSingleNameOrFail(itemDefBean.getRef()); // TODO error handling
                itemDefinitionsMap.put(itemName, new BeanBasedItemImplementation(itemDefBean));
            }
            Collection<ItemInboundDefinition> associationValueInbounds = new ArrayList<>();
            for (var itemDefBean : definitionBean.getAssociation()) {
                // TODO error handling
                var itemPath = itemDefBean.getRef().getItemPath();
                BeanBasedItemImplementation value = new BeanBasedItemImplementation(itemDefBean);
                if (itemPath.isEmpty()) {
                    associationValueInbounds.add(value);
                } else {
                    itemDefinitionsMap.put(itemPath.asSingleNameOrFail(), value);
                }
            }
            associationValueInboundDefinition = MiscUtil.extractSingleton(associationValueInbounds);
            synchronizationReactionDefinitions =
                    SynchronizationReactionDefinition.itemLevel(
                            definitionBean.getSynchronization());
        }

        @Override
        public Collection<? extends ItemInboundDefinition> getAttributeDefinitions() {
            return itemDefinitionsMap.values();
        }

        @Override
        public ItemInboundDefinition getSimpleAttributeInboundDefinition(ItemName itemName) {
            return itemDefinitionsMap.get(itemName);
        }

        @Override
        public ItemInboundDefinition getReferenceAttributeInboundDefinition(ItemName itemName) {
            return itemDefinitionsMap.get(itemName);
        }

        @Override
        public ResourceBidirectionalMappingType getActivationBidirectionalMappingType(ItemName itemName) {
            return ResourceObjectDefinitionUtil.getActivationBidirectionalMappingType(
                    definitionBean.getActivation(), itemName);
        }

        @Override
        public ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings() {
            return null; // probably makes no sense for associated objects
        }

        @Override
        public DefaultInboundMappingEvaluationPhasesType getDefaultInboundMappingEvaluationPhases() {
            return null; // currently not implemented
        }

        @Override
        public @NotNull List<MappingType> getPasswordInbound() {
            return List.of(); // currently not implemented
        }

        @Override
        public @NotNull FocusSpecification getFocusSpecification() {
            return new FocusSpecification() {
                @Override
                public ItemPath getFocusItemPath() {
                    return FocusType.F_ASSIGNMENT;
                }

                @Override
                public String getAssignmentSubtype() {
                    var focusBean = definitionBean.getFocus();
                    return focusBean != null ? focusBean.getAssignmentSubtype() : null;
                }

                @Override
                public QName getAssignmentTargetTypeName() {
                    var focusBean = definitionBean.getFocus();
                    return focusBean != null ? focusBean.getAssignmentTargetType() : null;
                }

                @Override
                public String getArchetypeOid() {
                    return null; // TODO implement if needed
                }
            };
        }

        @Override
        public @NotNull Collection<? extends SynchronizationReactionDefinition> getSynchronizationReactions() {
            return synchronizationReactionDefinitions;
        }

        @Override
        public CorrelationDefinitionType getCorrelation() {
            return definitionBean.getCorrelation();
        }

        @Override
        public boolean hasAnyInbounds() {
            return associationValueInboundDefinition != null
                    || itemDefinitionsMap.values().stream()
                    .anyMatch(def -> !def.getInboundMappingBeans().isEmpty());
        }

        @Override
        public @Nullable ItemInboundDefinition getDefaultObjectRefDefinition() {
            return associationValueInboundDefinition;
        }

        @Override
        public String debugDump(int indent) {
            return definitionBean.debugDump(indent);
        }
    }

    class BeanBasedItemImplementation implements ItemInboundDefinition {

        private final ComplexProcessingResourceItemDefinitionType definitionBean;

        BeanBasedItemImplementation(ComplexProcessingResourceItemDefinitionType definitionBean) {
            this.definitionBean = definitionBean;
        }

        @Override
        public @NotNull List<InboundMappingType> getInboundMappingBeans() {
            return definitionBean.getInbound();
        }

        @Override
        public ItemCorrelatorDefinitionType getCorrelatorDefinition() {
            return definitionBean.getCorrelator();
        }
    }

    class AssociationProcessingImplementation implements ResourceObjectInboundDefinition {

        @NotNull private final ShadowAssociationDefinitionType definitionBean;

        @NotNull private final PathKeyedMap<ItemInboundDefinition> itemDefinitionsMap = new PathKeyedMap<>();

        @NotNull private final Collection<ItemSynchronizationReactionDefinition> synchronizationReactionDefinitions;

        /** This is the inbound provided by unnamed `objectRef` definition. */
        @Nullable private final ItemInboundDefinition defaultObjectRefDefinition;

        AssociationProcessingImplementation(@NotNull ShadowAssociationDefinitionType definitionBean) {
            this.definitionBean = definitionBean;
            for (var itemDefBean : definitionBean.getAttribute()) {
                var itemName = ItemPathTypeUtil.asSingleNameOrFail(itemDefBean.getRef()); // TODO error handling
                itemDefinitionsMap.put(itemName, new AssociationBasedItemImplementation(itemDefBean));
            }
            Collection<ItemInboundDefinition> defaultObjectRefDefinitions = new ArrayList<>();
            for (var objectRefBean : definitionBean.getObjectRef()) {
                // TODO error handling
                var value = new AssociationBasedItemImplementation(objectRefBean);
                var refBean = objectRefBean.getRef();
                if (refBean == null) {
                    defaultObjectRefDefinitions.add(value);
                } else {
                    itemDefinitionsMap.put(refBean.getItemPath().asSingleNameOrFail(), value);
                }
            }
            defaultObjectRefDefinition = MiscUtil.extractSingleton(defaultObjectRefDefinitions);
            synchronizationReactionDefinitions =
                    SynchronizationReactionDefinition.itemLevel(
                            definitionBean.getSynchronization());
        }

        @Override
        public Collection<? extends ItemInboundDefinition> getAttributeDefinitions() {
            if (defaultObjectRefDefinition == null) {
                return itemDefinitionsMap.values();
            } else {
                var rv = new ArrayList<>(itemDefinitionsMap.values());
                rv.add(defaultObjectRefDefinition);
                return rv;
            }
        }

        @Override
        public ItemInboundDefinition getSimpleAttributeInboundDefinition(ItemName itemName) {
            return itemDefinitionsMap.get(itemName);
        }

        @Override
        public ItemInboundDefinition getReferenceAttributeInboundDefinition(ItemName itemName) {
            return itemDefinitionsMap.get(itemName);
        }

        @Override
        public ResourceBidirectionalMappingType getActivationBidirectionalMappingType(ItemName itemName) {
            return ResourceObjectDefinitionUtil.getActivationBidirectionalMappingType(
                    definitionBean.getActivation(), itemName);
        }

        @Override
        public ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings() {
            return null; // probably makes no sense for associated objects
        }

        @Override
        public DefaultInboundMappingEvaluationPhasesType getDefaultInboundMappingEvaluationPhases() {
            return null; // currently not implemented
        }

        @Override
        public @NotNull List<MappingType> getPasswordInbound() {
            return List.of(); // currently not implemented
        }

        @Override
        public @NotNull FocusSpecification getFocusSpecification() {
            return new FocusSpecification() {
                @Override
                public ItemPath getFocusItemPath() {
                    return FocusType.F_ASSIGNMENT;
                }

                @Override
                public String getAssignmentSubtype() {
                    var focusBean = definitionBean.getFocus();
                    return focusBean != null ? focusBean.getAssignmentSubtype() : null;
                }

                @Override
                public QName getAssignmentTargetTypeName() {
                    var focusBean = definitionBean.getFocus();
                    return focusBean != null ? focusBean.getAssignmentTargetType() : null;
                }

                @Override
                public String getArchetypeOid() {
                    return null; // TODO implement if needed
                }
            };
        }

        @Override
        public @NotNull Collection<? extends SynchronizationReactionDefinition> getSynchronizationReactions() {
            return synchronizationReactionDefinitions;
        }

        @Override
        public CorrelationDefinitionType getCorrelation() {
            return definitionBean.getCorrelation();
        }

        @Override
        public boolean hasAnyInbounds() {
            return defaultObjectRefDefinition != null
                    || itemDefinitionsMap.values().stream()
                    .anyMatch(def -> !def.getInboundMappingBeans().isEmpty());
        }

        @Override
        public @Nullable ItemInboundDefinition getDefaultObjectRefDefinition() {
            return defaultObjectRefDefinition;
        }

        @Override
        public String debugDump(int indent) {
            return definitionBean.debugDump(indent);
        }
    }

    class AssociationBasedItemImplementation implements ItemInboundDefinition {

        private final ResourceAttributeDefinitionType definitionBean;

        AssociationBasedItemImplementation(ResourceAttributeDefinitionType definitionBean) {
            this.definitionBean = definitionBean;
        }

        @Override
        public @NotNull List<InboundMappingType> getInboundMappingBeans() {
            return definitionBean.getInbound();
        }

        @Override
        public ItemCorrelatorDefinitionType getCorrelatorDefinition() {
            return definitionBean.getCorrelator();
        }
    }

    interface FocusSpecification {

        static FocusSpecification empty() {

            return new FocusSpecification() {
                @Override
                public ItemPath getFocusItemPath() {
                    return null;
                }

                @Override
                public String getAssignmentSubtype() {
                    return null;
                }

                @Override
                public QName getAssignmentTargetTypeName() {
                    return null;
                }

                @Override
                public String getArchetypeOid() {
                    return null;
                }
            };
        }
        ItemPath getFocusItemPath();
        String getAssignmentSubtype();
        QName getAssignmentTargetTypeName();
        String getArchetypeOid();
    }
}
