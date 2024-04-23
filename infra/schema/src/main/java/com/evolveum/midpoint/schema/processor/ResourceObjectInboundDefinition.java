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
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public interface ResourceObjectInboundDefinition extends Serializable {

    static ResourceObjectInboundDefinition empty() {
        return new EmptyImplementation();
    }

    static ResourceObjectInboundDefinition forAssociation(
            @Nullable ShadowAssociationTypeNewDefinitionType bean) {
        return new AssociationBasedImplementation(
                Objects.requireNonNullElseGet(bean, () -> new ShadowAssociationTypeNewDefinitionType())
        );
    }

    ItemInboundDefinition getAttributeInboundDefinition(ItemName itemName) throws SchemaException;

    ItemInboundDefinition getAssociationInboundDefinition(ItemName itemName) throws SchemaException;

    ResourceBidirectionalMappingType getActivationBidirectionalMappingType(ItemName itemName);

    ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings();

    DefaultInboundMappingEvaluationPhasesType getDefaultInboundMappingEvaluationPhases();

    @NotNull List<MappingType> getPasswordInbound();

    @NotNull FocusSpecification getFocusSpecification();

    @NotNull Collection<SynchronizationReactionDefinition> getSynchronizationReactions();

    CorrelationDefinitionType getCorrelation();

    // TEMPORARY FIXME define the semantics
    boolean hasAnyInbounds();

    default @Nullable ItemInboundDefinition getAssociationValueInboundDefinition() {
        return null;
    }

    interface ItemInboundDefinition extends Serializable {

        @NotNull List<InboundMappingType> getInboundMappingBeans();

        ItemCorrelatorDefinitionType getCorrelatorDefinition();
    }

    class EmptyImplementation implements ResourceObjectInboundDefinition {

        @Override
        public ItemInboundDefinition getAttributeInboundDefinition(ItemName itemName) {
            return null;
        }

        @Override
        public ItemInboundDefinition getAssociationInboundDefinition(ItemName itemName) {
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
        public @NotNull Collection<SynchronizationReactionDefinition> getSynchronizationReactions() {
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
    }

    class AssociationBasedImplementation implements ResourceObjectInboundDefinition {

        @NotNull private final ShadowAssociationTypeNewDefinitionType definitionBean;

        @NotNull private final PathKeyedMap<ItemInboundDefinition> itemDefinitionsMap = new PathKeyedMap<>();

        @NotNull private final Collection<SynchronizationReactionDefinition> synchronizationReactionDefinitions;

        /** This is the inbound provided by "ref = '.'", i.e., related to the association value itself. */
        @Nullable private final ItemInboundDefinition associationValueInboundDefinition;

        AssociationBasedImplementation(@NotNull ShadowAssociationTypeNewDefinitionType definitionBean) {
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
                    SynchronizationReactionDefinition.modern(
                            definitionBean.getSynchronization());
        }

        @Override
        public ItemInboundDefinition getAttributeInboundDefinition(ItemName itemName) {
            return itemDefinitionsMap.get(itemName);
        }

        @Override
        public ItemInboundDefinition getAssociationInboundDefinition(ItemName itemName) {
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
                    // TODO reconsider the default value handling
                    var focusBean = definitionBean.getFocus();
                    var itemBean = focusBean != null ? focusBean.getItem() : null;
                    if (itemBean != null) {
                        return itemBean.getItemPath();
                    } else {
                        return FocusType.F_ASSIGNMENT;
                    }
                }

                @Override
                public String getAssignmentSubtype() {
                    var focusBean = definitionBean.getFocus();
                    return focusBean != null ? focusBean.getSubtype() : null;
                }

                @Override
                public String getArchetypeOid() {
                    return null; // TODO implement if needed
                }
            };
        }

        @Override
        public @NotNull Collection<SynchronizationReactionDefinition> getSynchronizationReactions() {
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
        public @Nullable ItemInboundDefinition getAssociationValueInboundDefinition() {
            return associationValueInboundDefinition;
        }
    }

    class BeanBasedItemImplementation implements ItemInboundDefinition {

        private final ResourceItemDefinitionType definitionBean;

        BeanBasedItemImplementation(ResourceItemDefinitionType definitionBean) {
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
                public String getArchetypeOid() {
                    return null;
                }
            };
        }
        ItemPath getFocusItemPath();
        String getAssignmentSubtype();
        String getArchetypeOid();
    }
}