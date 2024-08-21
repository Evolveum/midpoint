/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.AbstractShadow;

import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.simulation.ExecutionModeProvider;
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

    /** True if this is a "complex" association (with association data object), false if it's a trivial one. */
    boolean isComplex();

    /** Returns the association object definition (for complex associations), or fails (for simple ones). */
    @NotNull ResourceObjectDefinition getAssociationDataObjectDefinition();

    /**
     * Provides information on acceptable types of shadows participating in this association as objects.
     * These come from the underlying reference attribute definition, but can be further restricted by the association
     * type definition.
     */
    @NotNull Multimap<QName, ShadowRelationParticipantType> getObjectParticipants();

    /**
     * Returns the name(s) of participant(s) playing the role of association object:
     *
     * - Exactly one for simple associations.
     * - Zero, one, or more for complex associations.
     */
    default @NotNull Collection<QName> getObjectParticipantNames() {
        return getObjectParticipants().keySet();
    }

    default boolean matches(@NotNull ShadowType potentialTarget) {
        return getReferenceAttributeDefinition().getTargetParticipantTypes().stream()
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
    @Deprecated
    default ResourceObjectDefinition getRepresentativeTargetObjectDefinition() {
        return getReferenceAttributeDefinition().getRepresentativeTargetObjectDefinition();
    }

    @Override
    @NotNull ShadowAssociation instantiate() throws SchemaException;

    default ShadowAssociationValue createValueFromFullDefaultObject(@NotNull AbstractShadow object) throws SchemaException {
        return createValueFromDefaultObject(object, true);
    }

    default ShadowAssociationValue createValueFromDefaultObject(@NotNull AbstractShadow object, boolean full)
            throws SchemaException {
        var newValue = instantiate().createNewValue();
        newValue.getOrCreateObjectsContainer()
                .addReferenceAttribute(getItemName(), object, full);
        return newValue.clone(); // to make it parent-less
    }

    default ShadowAssociationValue createValueFromDefaultObjectRef(@NotNull ShadowReferenceAttributeValue refAttrValue)
            throws SchemaException {
        var newValue = instantiate().createNewValue();
        newValue.getOrCreateObjectsContainer()
                .addReferenceAttribute(getItemName(), refAttrValue);
        return newValue.clone(); // to make it parent-less
    }

    ContainerDelta<ShadowAssociationValueType> createEmptyDelta();

    boolean isEntitlement();

    default String getResourceOid() {
        return getReferenceAttributeDefinition().getResourceOid();
    }

    @NotNull
    ShadowAssociationDefinition clone();

    @Override
    default <T extends ItemDefinition<?>> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
        return ShadowItemDefinition.super.findItemDefinition(path, clazz);
    }

    @NotNull Collection<MappingType> getExplicitOutboundMappingBeans();

    @NotNull Collection<InboundMappingType> getExplicitInboundMappingBeans();

    boolean isVisible(ExecutionModeProvider modeProvider);

    @NotNull ShadowReferenceAttributeDefinition getReferenceAttributeDefinition();

    ItemPath getStandardPath();

    /** To be used only for trivial associations; moreover, replaced by mark-based tolerance. */
    default List<String> getTolerantValuePatterns() {
        return getReferenceAttributeDefinition().getTolerantValuePatterns();
    }

    /** To be used only for trivial associations; moreover, replaced by mark-based tolerance. */
    default List<String> getIntolerantValuePatterns() {
        return getReferenceAttributeDefinition().getIntolerantValuePatterns();
    }

    boolean isTolerant();
}
