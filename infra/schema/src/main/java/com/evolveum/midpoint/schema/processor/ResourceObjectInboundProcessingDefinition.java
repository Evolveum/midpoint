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

import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.config.AbstractAttributeMappingsDefinitionConfigItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.processor.SynchronizationReactionDefinition.ItemSynchronizationReactionDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

/**
 * Defines "inbound processing" for resource object type or an association: correlation, synchronization reactions,
 * inbounds for attributes and associations.
 *
 * There are two main flavors:
 *
 * . for regular shadows: standard {@link ResourceObjectDefinition} which extends this interface;
 * . for association values: the implementation based on {@link AssociationSynchronizationExpressionEvaluatorType}.
 */
public interface ResourceObjectInboundProcessingDefinition extends Serializable, DebugDumpable {

    static ResourceObjectInboundProcessingDefinition forAssociationSynchronization(
            @NotNull ShadowAssociationDefinition associationDefinition,
            @NotNull AssociationSynchronizationExpressionEvaluatorType bean,
            @Nullable VariableBindingDefinitionType targetBean) throws ConfigurationException {
        return new AssociationSynchronizationImplementation(associationDefinition, bean, targetBean);
    }

    static ResourceObjectInboundProcessingDefinition withCorrelationDefinition(
            @NotNull ResourceObjectTypeDefinition typeDef, @NotNull List<QName> businessKeyItems) {

        return new Delegator() {

            @Override
            public ResourceObjectInboundProcessingDefinition delegate() {
                return typeDef;
            }

            @Override
            public @Nullable CorrelationDefinitionType getCorrelation() {
                var itemCorrelator = new ItemsSubCorrelatorType();
                businessKeyItems.forEach(key -> itemCorrelator.item(
                        new CorrelationItemType()
                                .ref(ItemName.fromQName(key).toBean())));
                return new CorrelationDefinitionType()
                        .correlators(new CompositeCorrelatorType()
                                .items(itemCorrelator));
            }
        };
    }

    /** Returns all inbound definitions for attributes and associations. */
    @NotNull Collection<CompleteItemInboundDefinition> getItemInboundDefinitions();

    /** Returns the inbound mappings for given activation item (administrativeStatus, validFrom, validTo, ...). */
    @NotNull List<MappingType> getActivationInboundMappings(ItemName itemName);

    /** Returns inbound mappings for the password. */
    @NotNull List<MappingType> getPasswordInboundMappings();

    /** Returns inbound mappings for the auxiliary object class(es) property. */
    @NotNull List<MappingType> getAuxiliaryObjectClassInboundMappings();

    /** What are the default evaluation phases for inbound mappings? Normally, it's the clockwork only. */
    DefaultInboundMappingEvaluationPhasesType getDefaultInboundMappingEvaluationPhases();

    @NotNull FocusSpecification getFocusSpecification();

    @NotNull Collection<? extends SynchronizationReactionDefinition> getSynchronizationReactions();

    @Nullable CorrelationDefinitionType getCorrelation();

    /**
     * Defines inbound processing for given attribute-like item, e.g., a real attribute or objectRef (in association).
     *
     * - Either the standard {@link ShadowAttributeDefinition} (based on {@link ResourceAttributeDefinitionType})
     * - or on association-specific, based on {@link AttributeInboundMappingsDefinitionType} in
     * {@link AssociationSynchronizationExpressionEvaluatorType}.
     */
    interface ItemInboundProcessingDefinition extends Serializable {

        @NotNull List<InboundMappingType> getInboundMappingBeans();

        ItemCorrelatorDefinitionType getCorrelatorDefinition();
    }

    /** Inbound processing for the `associationSynchronization` expression evaluator. */
    class AssociationSynchronizationImplementation implements ResourceObjectInboundProcessingDefinition {

        @NotNull private final AssociationSynchronizationExpressionEvaluatorType definitionBean;

        @Nullable private final VariableBindingDefinitionType targetBean;

        @NotNull private final List<CompleteItemInboundDefinition> completeItemInboundDefinitions;

        @NotNull private final Collection<ItemSynchronizationReactionDefinition> synchronizationReactionDefinitions;

        AssociationSynchronizationImplementation(
                @NotNull ShadowAssociationDefinition associationDefinition,
                @NotNull AssociationSynchronizationExpressionEvaluatorType definitionBean,
                @Nullable VariableBindingDefinitionType targetBean) throws ConfigurationException {
            this.definitionBean = definitionBean;
            this.targetBean = targetBean;

            try {
                var shadowItemInboundDefinitions = new ArrayList<CompleteItemInboundDefinition>();
                var origin = ConfigurationItemOrigin.undeterminedSafe(); // TODO connect to the resource
                for (var attrDefBean : definitionBean.getAttribute()) {
                    var attrDefCI = AbstractAttributeMappingsDefinitionConfigItem.of(attrDefBean, origin);
                    var itemName = attrDefCI.getRef();
                    shadowItemInboundDefinitions.add(
                            new CompleteItemInboundDefinition(
                                    ShadowAssociationValueType.F_ATTRIBUTES.append(itemName),
                                    associationDefinition.findSimpleAttributeDefinitionRequired(itemName),
                                    new AssociationBasedItemImplementation(attrDefBean)));
                }
                for (var objectRefDefBean : definitionBean.getObjectRef()) {
                    var objectRefDefCI = AbstractAttributeMappingsDefinitionConfigItem.of(objectRefDefBean, origin);
                    var itemName = objectRefDefCI.getObjectRefOrDefault(associationDefinition);
                    shadowItemInboundDefinitions.add(
                            new CompleteItemInboundDefinition(
                                    ShadowAssociationValueType.F_OBJECTS.append(itemName),
                                    associationDefinition.findObjectRefDefinitionRequired(itemName),
                                    new AssociationBasedItemImplementation(objectRefDefBean)));
                }
                this.completeItemInboundDefinitions = List.copyOf(shadowItemInboundDefinitions);
                synchronizationReactionDefinitions =
                        SynchronizationReactionDefinition.itemLevel(
                                definitionBean.getSynchronization());
            } catch (SchemaException e) {
                throw new ConfigurationException(e.getMessage(), e); // TODO improve error handling here
            }
        }

        @Override
        public @NotNull Collection<CompleteItemInboundDefinition> getItemInboundDefinitions() {
            return completeItemInboundDefinitions;
        }

        public @NotNull List<MappingType> getActivationInboundMappings(ItemName itemName) {
            return ResourceObjectDefinitionUtil.getActivationInboundMappings(
                    ResourceObjectDefinitionUtil.getActivationBidirectionalMappingType(definitionBean.getActivation(), itemName));
        }

        @Override
        public @NotNull List<MappingType> getAuxiliaryObjectClassInboundMappings() {
            return List.of(); // probably makes no sense for associated objects
        }

        @Override
        public DefaultInboundMappingEvaluationPhasesType getDefaultInboundMappingEvaluationPhases() {
            return null; // currently not implemented
        }

        @Override
        public @NotNull List<MappingType> getPasswordInboundMappings() {
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

    class AssociationBasedItemImplementation implements ItemInboundProcessingDefinition {

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

    /**
     * What focus objects correspond to the given resource object?
     *
     * - If the resource object is a regular shadow, we assume the focus objects will be {@link FocusType} or its subtypes.
     * - If the resource object is an association value, we assume the focus objects will be {@link AssignmentType}.
     *
     * Experimental. This interface is only a single access channel to that information. Other places access it directly,
     * like using {@link ResourceObjectFocusSpecificationType#getType()} etc.
     *
     * TODO Consider whether it is useful at all.
     */
    @Experimental
    interface FocusSpecification {

        /** No focus objects are expected. */
        static FocusSpecification none() {

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

        /** OID of the structural archetype for the focus objects. Not applicable for the assignments. */
        String getArchetypeOid();

        /** Subtype of the assignments. Not applicable for real focus objects. */
        String getAssignmentSubtype();
    }

    /**
     * Complete inbound definition of an item: attribute (simple/reference) or an association.
     *
     * - standard definition of the item (name, type, cardinality, lifecycle state, etc),
     * - inbound processing: mappings and correlator.
     *
     * Normally, both
     *
     * Other shadow items (activation, credentials, ...) can come later.
     */
    record CompleteItemInboundDefinition(
            @NotNull ItemPath path,
            @NotNull ShadowItemDefinition itemDefinition,
            @NotNull ItemInboundProcessingDefinition inboundProcessingDefinition) implements Serializable {
    }

    interface Delegator extends ResourceObjectInboundProcessingDefinition {

        ResourceObjectInboundProcessingDefinition delegate();

        @Override
        default @NotNull Collection<CompleteItemInboundDefinition> getItemInboundDefinitions() {
            return delegate().getItemInboundDefinitions();
        }

        @Override
        default @NotNull List<MappingType> getActivationInboundMappings(ItemName itemName) {
            return delegate().getActivationInboundMappings(itemName);
        }

        @Override
        default @NotNull List<MappingType> getPasswordInboundMappings() {
            return delegate().getPasswordInboundMappings();
        }

        @Override
        default @NotNull List<MappingType> getAuxiliaryObjectClassInboundMappings() {
            return delegate().getAuxiliaryObjectClassInboundMappings();
        }

        @Override
        default DefaultInboundMappingEvaluationPhasesType getDefaultInboundMappingEvaluationPhases() {
            return delegate().getDefaultInboundMappingEvaluationPhases();
        }

        @Override
        default @NotNull FocusSpecification getFocusSpecification() {
            return delegate().getFocusSpecification();
        }

        @Override
        default @NotNull Collection<? extends SynchronizationReactionDefinition> getSynchronizationReactions() {
            return delegate().getSynchronizationReactions();
        }

        @Override
        default @Nullable CorrelationDefinitionType getCorrelation() {
            return delegate().getCorrelation();
        }

        @Override
        default String debugDump(int indent) {
            return delegate().debugDump(indent);
        }
    }
}
