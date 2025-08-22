/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;

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


    /**
     * ONLY FOR SUBJECT-SIDE PARTICIPANT (and always non-null for it)
     *
     * Returns generalized definition for objects at the object (target) side. It should cover all possible objects;
     * so it contains definitions of all possible auxiliary object classes.
     *
     * May be:
     *
     * - embedded (for references used to implement complex associations)
     * - or standalone (for references used to implement simple associations, or references without associations).
     *
     * May be:
     *
     * - genuine {@link ResourceObjectClassDefinition}
     * - or {@link ResourceObjectTypeDefinition} (if there's a default type for that object class;
     * hopefully removed soon, see MID-10309)
     * - or {@link CompositeObjectDefinition} if there are auxiliary object classes.
     *
     * Immutable after the resource schema is frozen.
     *
     * @see SimulatedShadowReferenceTypeDefinition#generalizedObjectSideObjectDefinition
     */
    @NotNull ResourceObjectDefinition getGeneralizedObjectSideObjectDefinition();

    /**
     * Returns `true` if the reference points to an embedded object class. This indicates a complex association.
     *
     * Callable only on the subject-side reference definitions!
     */
    default boolean isTargetingSingleEmbeddedObjectClass() {
        checkSubjectSide();
        return getGeneralizedObjectSideObjectDefinition().getObjectClassDefinition().isEmbedded();
    }

    /**
     * Returns the target object class name.
     *
     * Callable only on the subject-side reference definitions!
     */
    default @NotNull QName getTargetObjectClassName() {
        checkSubjectSide();
        return getGeneralizedObjectSideObjectDefinition().getTypeName();
    }

    default void checkSubjectSide() {
        stateCheck(
                getParticipantRole() == ShadowReferenceParticipantRole.SUBJECT,
                "Only subject-side reference definition can have target object class definition: %s", this);
    }

    /** Returns `true` if the provided shadow is a legal target for this reference (according to the definition). */
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

    ReferenceDelta createEmptyDelta();

    SimulatedShadowReferenceTypeDefinition getSimulationDefinition();

    SimulatedShadowReferenceTypeDefinition getSimulationDefinitionRequired();

    default @NotNull String getResourceOid() {
        // Callable only on the subject-side reference definition.
        return stateNonNull(
                getGeneralizedObjectSideObjectDefinition().getResourceOid(),
                "No resource OID in %s", this);
    }

    @NotNull ShadowReferenceAttributeDefinition clone();

    @NotNull ShadowReferenceAttributeDefinition cloneWithNewCardinality(int newMinOccurs, int newMaxOccurs);

    /** @see NativeShadowReferenceAttributeDefinition#isComplexAttribute() */
    boolean isComplexAttribute();
}
