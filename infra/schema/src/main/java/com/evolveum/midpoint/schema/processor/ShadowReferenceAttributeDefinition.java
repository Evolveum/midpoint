/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.util.exception.SchemaException;

import static com.evolveum.midpoint.util.MiscUtil.*;

/** Definition of an {@link ShadowReferenceAttribute}. */
public interface ShadowReferenceAttributeDefinition
        extends
        PrismReferenceDefinition,
        ShadowAttributeDefinition<
                ShadowReferenceAttributeValue,
                ShadowReferenceAttributeDefinition,
                Referencable,
                ShadowReferenceAttribute> {

    /**
     * What is the role of the object that holds this reference attribute in the subject-object relationship?
     *
     * For example, when considering `ri:group` reference attribute on `ri:user` object class pointing to the `ri:group`
     * object class, the participant role for this attribute is {@link ShadowReferenceParticipantRole#SUBJECT}, because
     * `ri:user` object class participates in this relation as the subject.
     *
     * For the other side of this reference, the `ri:member` reference attribute on `ri:group` object class will have
     * participant role of {@link ShadowReferenceParticipantRole#OBJECT}, because `ri:group` object class participates
     * in this relation as the object.
     */
    @NotNull ShadowReferenceParticipantRole getParticipantRole();

    /** Returns types of the objects on the other side. Always non-empty. */
    @NotNull Collection<ShadowRelationParticipantType> getTargetParticipantTypes();

    default boolean isTargetingSingleEmbeddedObjectClass() {
        var classDefinitions = getTargetParticipantTypes().stream()
                .map(participantType -> participantType.getObjectDefinition().getObjectClassDefinition())
                .collect(Collectors.toSet());
        return classDefinitions.size() == 1
                && classDefinitions.iterator().next().isEmbedded();
    }

    @Override
    <T extends ItemDefinition<?>> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz);

    /**
     * Returns the object class definition of the immediate target object. Fails if there's not exactly one.
     *
     * TEMPORARY IMPLEMENTATION; this should be resolved during definition parsing/creation.
     */
    default @NotNull ResourceObjectClassDefinition getTargetObjectClass() {
        var immediateNeighbors =
                stateNonEmpty(getTargetParticipantTypes(), "No target participant types in %s", this);
        var classDefinitions = immediateNeighbors.stream()
                .map(participantType -> participantType.getObjectDefinition().getObjectClassDefinition())
                .collect(Collectors.toSet());
        return MiscUtil.extractSingletonRequired(
                classDefinitions,
                () -> new IllegalStateException("Multiple object class definitions in " + this + ": " + classDefinitions),
                () -> new IllegalStateException("No object class definition in " + this));
    }

    default @NotNull QName getTargetObjectClassName() {
        return getTargetObjectClass().getTypeName();
    }

    default boolean matches(@NotNull ShadowType potentialTarget) {
        return getTargetParticipantTypes().stream()
                .anyMatch(participantType -> participantType.matches(potentialTarget));
    }

    // FIXME fix this method
    default @NotNull ObjectFilter createTargetObjectsFilter() {
        var resourceOid = stateNonNull(getRepresentativeTargetObjectDefinition().getResourceOid(), "No resource OID in %s", this);
        var targetParticipantTypes = getTargetParticipantTypes();
        assertCheck(!targetParticipantTypes.isEmpty(), "No object type definitions (already checked)");
        var firstObjectType = targetParticipantTypes.iterator().next().getTypeIdentification();
        if (targetParticipantTypes.size() > 1 || firstObjectType == null) {
            var objectClassNames = targetParticipantTypes.stream()
                    .map(def -> def.getObjectDefinition().getObjectClassName())
                    .collect(Collectors.toSet());
            var objectClassName = MiscUtil.extractSingletonRequired(
                    objectClassNames,
                    () -> new UnsupportedOperationException("Multiple object class names in " + this),
                    () -> new IllegalStateException("No object class names in " + this));
            return PrismContext.get().queryFor(ShadowType.class)
                    .item(ShadowType.F_RESOURCE_REF).ref(resourceOid, ResourceType.COMPLEX_TYPE)
                    .and().item(ShadowType.F_OBJECT_CLASS).eq(objectClassName)
                    .buildFilter();
        } else {
            return PrismContext.get().queryFor(ShadowType.class)
                    .item(ShadowType.F_RESOURCE_REF).ref(resourceOid, ResourceType.COMPLEX_TYPE)
                    .and().item(ShadowType.F_KIND).eq(firstObjectType.getKind())
                    .and().item(ShadowType.F_INTENT).eq(firstObjectType.getIntent())
                    .buildFilter();
        }
    }

    /** TODO reconsider this: which definition should we provide as the representative one? There can be many. */
    @Deprecated
    ResourceObjectDefinition getRepresentativeTargetObjectDefinition();

    @TestOnly
    ShadowReferenceAttributeValue instantiateFromIdentifierRealValue(@NotNull QName identifierName, @NotNull Object realValue)
            throws SchemaException;

    ReferenceDelta createEmptyDelta();

    SimulatedShadowReferenceTypeDefinition getSimulationDefinition();

    SimulatedShadowReferenceTypeDefinition getSimulationDefinitionRequired();

    /** Very poorly defined method; TODO reconsider. */
    boolean isEntitlement();

    default String getResourceOid() {
        return getRepresentativeTargetObjectDefinition().getResourceOid();
    }

    @NotNull ShadowReferenceAttributeDefinition clone();

    @NotNull ShadowReferenceAttributeDefinition cloneWithNewCardinality(int newMinOccurs, int newMaxOccurs);
}
