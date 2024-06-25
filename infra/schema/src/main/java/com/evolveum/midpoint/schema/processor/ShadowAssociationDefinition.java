/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.google.common.collect.ImmutableSetMultimap.flatteningToImmutableSetMultimap;
import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_C;
import static com.evolveum.midpoint.util.MiscUtil.stateNonEmpty;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.schema.util.AbstractShadow;

import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.simulation.ExecutionModeProvider;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Definition of a {@link ShadowAssociation}, e.g., `ri:group`.
 *
 * @see ShadowAttributeDefinition
 */
public interface ShadowAssociationDefinition
        extends
        PrismContainerDefinition<ShadowAssociationValueType>,
        ShadowItemDefinition {

    // TEMPORARY!
    ItemName VALUE = ItemName.from(NS_C, "value");

    /** Returns "immediate neighbors". TODO REMOVE/INLINE */
    default @NotNull Collection<ShadowRelationParticipantType> getImmediateTargetParticipantTypes() {
        return getReferenceAttributeDefinition().getTargetParticipantTypes();
    }

    /** This is used for associations with the association object. */
    default @NotNull ShadowRelationParticipantType getAssociationObjectInformation() {
        var immediateTargets = getReferenceAttributeDefinition().getTargetParticipantTypes();
        return MiscUtil.extractSingletonRequired(
                immediateTargets,
                () -> new IllegalStateException("Multiple immediate targets in " + this + ": " + immediateTargets),
                () -> new IllegalStateException("No immediate target in " + this));
    }

    /** Only for associations with association object! */
    default @NotNull ResourceObjectDefinition getAssociationObjectDefinition() {
        return getAssociationObjectInformation().getObjectDefinition();
    }

    /**
     * Returns the object class definition of the immediate target object. Should be exactly one.
     *
     * TEMPORARY IMPLEMENTATION; this should be resolved during definition parsing/creation.
     */
    default @NotNull ResourceObjectClassDefinition getImmediateTargetObjectClass() {
        var immediateNeighbors =
                stateNonEmpty(getImmediateTargetParticipantTypes(), "No immediate neighbors in %s", this);
        var classDefinitions = immediateNeighbors.stream()
                .map(participantType -> participantType.getObjectDefinition().getObjectClassDefinition())
                .collect(Collectors.toSet());
        return MiscUtil.extractSingletonRequired(
                classDefinitions,
                () -> new IllegalStateException("Multiple object class definitions in " + this + ": " + classDefinitions),
                () -> new IllegalStateException("No object class definition in " + this));
    }

    default boolean hasAssociationObject() {
        return getImmediateTargetObjectClass().isAssociationObject();
    }

    /** TEMPORARY */
    default @NotNull Multimap<QName, ShadowRelationParticipantType> getObjectParticipants(
            @NotNull CompleteResourceSchema resourceSchema) {
        if (!hasAssociationObject()) {
            // "empty" association - just target objects, nothing inside, so immediate targets are the objects
            return getImmediateTargetParticipantTypes().stream()
                    .collect(toImmutableSetMultimap(
                            part -> getReferenceAttributeDefinition().getItemName(),
                            part -> part));
        } else {
            // The object types can be defined either by the association type definition, or by the association object
            // class or type definition.
            var associationObjectInfo = getAssociationObjectInformation();
            return associationObjectInfo.getObjectDefinition().getReferenceAttributeDefinitions().stream()
                    .collect(flatteningToImmutableSetMultimap(
                            objectRefDef -> objectRefDef.getItemName(),
                            objectRefDef -> objectRefDef
                                    .getAssociationDefinition()
                                    .getObjectDefinitionsFor(objectRefDef.getItemName(), resourceSchema).stream()));
        }
    }

    default Set<ShadowRelationParticipantType> getObjectDefinitionsFor(
            @NotNull ItemName refAttrName, @NotNull CompleteResourceSchema resourceSchema) {
        var bean = getAssociationTypeDefinitionBean();
        if (bean != null) {
            var objectDefs = bean.getObject().stream()
                    .filter(objectDef -> objectDef.getRef() == null || QNameUtil.match(refAttrName, objectDef.getRef()))
                    .toList();
            var objectDef = MiscUtil.extractSingleton(objectDefs);
            if (objectDef != null && !objectDef.getObjectType().isEmpty()) {
                return objectDef.getObjectType().stream()
                        .map(typeBean -> ResourceObjectTypeIdentification.of(typeBean))
                        .map(typeId -> resourceSchema.getObjectTypeDefinitionRequired(typeId))
                        .map(typeDef -> ShadowRelationParticipantType.forObjectType(typeDef))
                        .collect(Collectors.toSet());
            }
        }
        return new HashSet<>(getImmediateTargetParticipantTypes());
    }

    default boolean matches(@NotNull ShadowType potentialTarget) {
        return getImmediateTargetParticipantTypes().stream()
                .anyMatch(participantType -> participantType.matches(potentialTarget));
    }

    /**
     * Creates a filter that provides all shadows eligible as the target value for this association.
     *
     * FIXME resolve limitations:
     *  - single object class is allowed for given association
     *  - if multiple object types are there, then the filter is for the whole class
     *  - if type type is the default object type, then it's used as such (even if the whole OC should be returned)
     *
     * TODO are these immediate targets (associated objects, if present), or the "final" targets?
     */
    default ObjectFilter createTargetObjectsFilter() {
        // FIXME remove this hack!
        return getReferenceAttributeDefinition().createTargetObjectsFilter();
    }

    /** TODO reconsider this: which definition should we provide as the representative one? There can be many. */
    default ResourceObjectDefinition getRepresentativeTargetObjectDefinition() {
        throw new UnsupportedOperationException("remove me");
    }

    @Override
    @NotNull ShadowAssociation instantiate() throws SchemaException;

    @TestOnly
    default ShadowAssociationValue createValueFromDefaultObject(@NotNull QName assocName, @NotNull AbstractShadow object)
            throws SchemaException {
        var newValue = instantiate().createNewValue();
        newValue.getOrCreateObjectsContainer()
                .addReferenceAttribute(assocName, object);
        return newValue.clone(); // to make it parent-less
    }

    ContainerDelta<ShadowAssociationValueType> createEmptyDelta();

    boolean isEntitlement();

    default String getResourceOid() {
        return getReferenceAttributeDefinition().getResourceOid();
    }

    @NotNull
    ShadowAssociationDefinition clone();

    @Nullable ShadowAssociationDefinitionType getAssociationDefinitionBean();

    @Nullable ShadowAssociationTypeDefinitionType getAssociationTypeDefinitionBean();

    // TODO find better place
    default boolean hasModernOutbound() {
        var bean = getAssociationDefinitionBean();
        if (bean == null) {
            return false;
        }
        return bean.getAttribute().stream().anyMatch(a -> a.getOutbound() != null)
                || bean.getObjectRef().stream().anyMatch(o -> o.getOutbound() != null);
    }

    @Override
    default <T extends ItemDefinition<?>> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
        return ShadowItemDefinition.super.findItemDefinition(path, clazz);
    }

    @Nullable MappingType getLegacyOutboundMappingBean();

    boolean isVisible(ExecutionModeProvider modeProvider);

    @NotNull ShadowReferenceAttributeDefinition getReferenceAttributeDefinition();
}
