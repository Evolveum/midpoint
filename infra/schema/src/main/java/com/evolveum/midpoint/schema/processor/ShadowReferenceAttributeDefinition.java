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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.MiscUtil;

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

    /**
     * Returns a filter that provides all shadows eligible as the target value for this reference attribute.
     *
     * If `resourceSafe` is `true`, the filter is safe for the execution on the resource, i.e., it does not contain
     * multiple values for kind and intent. The filtering by object class is used in such cases; that requires post-processing
     * of returned values that filters out those shadows that do not match those kind/intent values.
     *
     * Note that currently provisioning module require at most one kind/intent even with `noFetch` option being present.
     */
    default @NotNull ObjectFilter createTargetObjectsFilter(boolean resourceSafe) {
        return ObjectQueryUtil.createObjectTypesFilter(
                getResourceOid(), getTargetParticipantTypes(), resourceSafe, this);
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

    default @NotNull String getResourceOid() {
        return stateNonNull(
                getRepresentativeTargetObjectDefinition().getResourceOid(),
                "No resource OID in %s", this);
    }

    @NotNull ShadowReferenceAttributeDefinition clone();

    @NotNull ShadowReferenceAttributeDefinition cloneWithNewCardinality(int newMinOccurs, int newMaxOccurs);
}
