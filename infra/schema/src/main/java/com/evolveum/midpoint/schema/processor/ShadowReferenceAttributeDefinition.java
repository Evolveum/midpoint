/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.google.common.collect.ImmutableSetMultimap.flatteningToImmutableSetMultimap;
import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;

import static com.evolveum.midpoint.util.MiscUtil.stateNonEmpty;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Definition of an association item, e.g., `ri:group`.
 *
 * The association can be native or simulated; it can point right to the target object (like `group` object class),
 * or to an intermediate - a.k.a. "associated" - one (like `groupMembership` object class).
 *
 * @see ShadowAttributeDefinition
 */
public interface ShadowReferenceAttributeDefinition
        extends
        PrismContainerDefinition<ShadowAssociationValueType>,
        ShadowAttributeDefinition<ShadowReferenceAttribute, ShadowAssociationValueType> {

    /** Returns "immediate neighbors". TODO */
    @NotNull Collection<AssociationParticipantType> getImmediateTargetParticipantTypes();

    /** This is used for associations with the association object. */
    default @NotNull AssociationParticipantType getAssociationObjectInformation() {
        var immediateTargets = getImmediateTargetParticipantTypes();
        return MiscUtil.extractSingletonRequired(
                immediateTargets,
                () -> new IllegalStateException("Multiple immediate targets in " + this + ": " + immediateTargets),
                () -> new IllegalStateException("No immediate target in " + this));
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
    default @NotNull Multimap<QName, AssociationParticipantType> getObjectParticipants(
            @NotNull CompleteResourceSchema resourceSchema) {
        if (!hasAssociationObject()) {
            // "empty" association - just target objects, nothing inside, so immediate targets are the objects
            return getImmediateTargetParticipantTypes().stream()
                    .collect(toImmutableSetMultimap(part -> null, part -> part));
        } else {
            // The object types can be defined either by the association type definition, or by the association object
            // class or type definition.
            var associationObjectInfo = getAssociationObjectInformation();
            return associationObjectInfo.getObjectDefinition().getReferenceAttributeDefinitions().stream()
                    .collect(flatteningToImmutableSetMultimap(
                            objectRefDef -> objectRefDef.getItemName(),
                            objectRefDef -> objectRefDef.getObjectDefinitionsFor(objectRefDef.getItemName(), resourceSchema).stream()));
        }
    }

    default Set<AssociationParticipantType> getObjectDefinitionsFor(
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
                        .map(typeDef -> AssociationParticipantType.forObjectType(typeDef))
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
    ObjectFilter createTargetObjectsFilter();

    /** TODO reconsider this: which definition should we provide as the representative one? There can be many. */
    ResourceObjectDefinition getRepresentativeTargetObjectDefinition();

    @TestOnly
    ShadowAssociationValue instantiateFromIdentifierRealValue(@NotNull QName identifierName, @NotNull Object realValue)
            throws SchemaException;

    ContainerDelta<ShadowAssociationValueType> createEmptyDelta();

    SimulatedShadowReferenceTypeDefinition getSimulationDefinition();

    SimulatedShadowReferenceTypeDefinition getSimulationDefinitionRequired();

    boolean isEntitlement();

    default String getResourceOid() {
        return getRepresentativeTargetObjectDefinition().getResourceOid();
    }

    @NotNull ShadowReferenceAttributeDefinition clone();

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
}
