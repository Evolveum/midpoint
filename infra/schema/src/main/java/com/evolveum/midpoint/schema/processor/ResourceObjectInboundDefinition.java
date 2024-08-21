/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.schema.config.AbstractAttributeMappingsDefinitionConfigItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.processor.SynchronizationReactionDefinition.ItemSynchronizationReactionDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Defines "complex inbound processing": correlation, synchronization reactions, inbounds for attributes and associations.
 *
 * There are three main flavors:
 *
 * . standard {@link ResourceObjectDefinition}
 * . for associations, based on {@link AssociationSynchronizationExpressionEvaluatorType}
 *
 * Currently, the processing assumes that we have a shadow as an input. It is either the regular shadow coming from
 * a resource, or a fake shadow in the case of associations (TODO).
 *
 * TEMPORARY
 */
public interface ResourceObjectInboundDefinition extends Serializable, DebugDumpable {

    static ResourceObjectInboundDefinition empty() {
        return new EmptyImplementation();
    }

    static ResourceObjectInboundDefinition forAssociationSynchronization(
            @NotNull ShadowAssociationDefinition associationDefinition,
            @NotNull AssociationSynchronizationExpressionEvaluatorType bean,
            @Nullable VariableBindingDefinitionType targetBean) throws ConfigurationException {
        return new AssociationSynchronizationImplementation(associationDefinition, bean, targetBean);
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
        public String debugDump(int indent) {
            return "EMPTY"; // FIXME
        }
    }

    class AssociationSynchronizationImplementation implements ResourceObjectInboundDefinition {

        @NotNull private final AssociationSynchronizationExpressionEvaluatorType definitionBean;
        @Nullable private final VariableBindingDefinitionType targetBean;

        @NotNull private final PathKeyedMap<ItemInboundDefinition> itemDefinitionsMap = new PathKeyedMap<>();

        @NotNull private final Collection<ItemSynchronizationReactionDefinition> synchronizationReactionDefinitions;

        AssociationSynchronizationImplementation(
                @NotNull ShadowAssociationDefinition associationDefinition,
                @NotNull AssociationSynchronizationExpressionEvaluatorType definitionBean,
                @Nullable VariableBindingDefinitionType targetBean) throws ConfigurationException {
            this.definitionBean = definitionBean;
            this.targetBean = targetBean;

            var origin = ConfigurationItemOrigin.undeterminedSafe(); // TODO connect to the resource
            for (var attrDefBean : definitionBean.getAttribute()) {
                var attrDefCI = AbstractAttributeMappingsDefinitionConfigItem.of(attrDefBean, origin);
                var itemName = attrDefCI.getRef();
                itemDefinitionsMap.put(itemName, new AssociationBasedItemImplementation(attrDefBean));
            }
            Collection<ItemInboundDefinition> defaultObjectRefDefinitions = new ArrayList<>();
            for (var objectRefDefBean : definitionBean.getObjectRef()) {
                var objectRefDefCI = AbstractAttributeMappingsDefinitionConfigItem.of(objectRefDefBean, origin);
                var itemName = objectRefDefCI.getObjectRefOrDefault(associationDefinition);
                itemDefinitionsMap.put(itemName, new AssociationBasedItemImplementation(objectRefDefBean));
            }
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
                public String getAssignmentSubtype() {
                    return targetBean != null ? targetBean.getAssignmentSubtype() : null;
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
        public String debugDump(int indent) {
            return DebugUtil.debugDump(definitionBean, indent);
        }
    }

    class AssociationBasedItemImplementation implements ItemInboundDefinition {

        private final AttributeInboundMappingsDefinitionType definitionBean;

        AssociationBasedItemImplementation(AttributeInboundMappingsDefinitionType definitionBean) {
            this.definitionBean = definitionBean;
        }

        @Override
        public @NotNull List<InboundMappingType> getInboundMappingBeans() {
            return definitionBean.getMapping();
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
                public String getAssignmentSubtype() {
                    return null;
                }

                @Override
                public String getArchetypeOid() {
                    return null;
                }
            };
        }
        String getAssignmentSubtype();
        String getArchetypeOid();
    }
}
