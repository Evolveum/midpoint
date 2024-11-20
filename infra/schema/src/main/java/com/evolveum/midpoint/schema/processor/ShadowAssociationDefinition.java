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

import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.MiscUtil;

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

import org.jetbrains.annotations.Nullable;

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
     * Provides information on acceptable types of shadows participating in this association as (the one) object.
     *
     * Fails if there are no or multiple objects.
     *
     * @see #getObjectParticipants()
     */
    default @NotNull Collection<ShadowRelationParticipantType> getObjectParticipant() {
        var objectParticipantsMap = getObjectParticipants();
        var objectName = MiscUtil.extractSingletonRequired(
                objectParticipantsMap.keySet(),
                () -> new IllegalStateException("Multiple object participants in " + this + ": " + objectParticipantsMap.keySet()),
                () -> new IllegalStateException("No object participants in " + this));
        return getObjectParticipants().get(objectName);
    }

    /**
     * Returns the name(s) of participant(s) playing the role of association object:
     *
     * - Exactly one for simple associations.
     * - Zero, one, or more for complex associations.
     */
    default @NotNull Collection<QName> getObjectParticipantNames() {
        return getObjectParticipants().keySet();
    }

    /** Call only on simple associations! */
    default @NotNull QName getSingleObjectParticipantName() throws SchemaException {
        assert !isComplex();
        var participantNames = getObjectParticipantNames();
        return MiscUtil.extractSingletonRequired(
                participantNames,
                () -> new SchemaException("Multiple object participants in " + this + ": " + participantNames),
                () -> new SchemaException("No object participant in " + this + ": " + participantNames));
    }

    // FIXME is this really correct? Why don't we look at our own participant types?
    default boolean matches(@NotNull ShadowType potentialTarget) {
        return getReferenceAttributeDefinition().getTargetParticipantTypes().stream()
                .anyMatch(participantType -> participantType.matches(potentialTarget));
    }

    /**
     * Creates a filter that provides all shadows eligible as the target value for this association.
     *
     * For complex associations, a filter for association data objects is returned.
     *
     * @see ShadowReferenceAttributeDefinition#createTargetObjectsFilter(boolean)
     */
    default ObjectFilter createTargetObjectsFilter(boolean resourceSafe) {
        var resourceOid = getResourceOid();
        if (isComplex()) {
            // This is a preliminary implementation
            return getAssociationDataObjectDefinition()
                    .createShadowSearchQuery(resourceOid)
                    .getFilter();
        } else {
            return ObjectQueryUtil.createObjectTypesFilter(resourceOid, getObjectParticipant(), resourceSafe, this);
        }
    }

    /** TODO reconsider this: which definition should we provide as the representative one? There can be many. */
    @Deprecated
    default ResourceObjectDefinition getRepresentativeTargetObjectDefinition() {
        return getReferenceAttributeDefinition().getRepresentativeTargetObjectDefinition();
    }

    @Override
    @NotNull ShadowAssociation instantiate() throws SchemaException;

    /** Call only on simple associations! */
    default ShadowAssociationValue createValueFromFullDefaultObject(@NotNull AbstractShadow object) throws SchemaException {
        return createValueFromDefaultObject(object, true);
    }

    /** Call only on simple associations! */
    default ShadowAssociationValue createValueFromDefaultObject(@NotNull AbstractShadow object, boolean full)
            throws SchemaException {
        assert !isComplex();
        var newValue = instantiate().createNewValue();
        newValue.getOrCreateObjectsContainer()
                .addReferenceAttribute(getSingleObjectParticipantName(), object, full);
        return newValue.clone(); // to make it parent-less
    }

    /** Call only on simple associations! */
    default ShadowAssociationValue createValueFromDefaultObjectRef(@NotNull ShadowReferenceAttributeValue refAttrValue)
            throws SchemaException {
        assert !isComplex();
        var newValue = instantiate().createNewValue();
        newValue.getOrCreateObjectsContainer()
                .addReferenceAttribute(getSingleObjectParticipantName(), refAttrValue);
        return newValue.clone(); // to make it parent-less
    }

    ContainerDelta<ShadowAssociationValueType> createEmptyDelta();

    boolean isEntitlement();

    default @NotNull String getResourceOid() {
        return getReferenceAttributeDefinition().getResourceOid();
    }

    @NotNull
    ShadowAssociationDefinition clone();

    @Override
    default <T extends ItemDefinition<?>> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
        return ShadowItemDefinition.super.findItemDefinition(path, clazz);
    }

    @NotNull Collection<MappingType> getOutboundMappingBeans();

    @NotNull Collection<InboundMappingType> getInboundMappingBeans();

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

    /** Use with care. Please do not modify the returned value. */
    @Nullable ShadowAssociationDefinitionType getModernAssociationDefinitionBean();

    /** Use with care. Please do not modify the returned value. */
    @Nullable ShadowAssociationTypeDefinitionType getModernAssociationTypeDefinitionBean();

    @NotNull QName getAssociationTypeName();
}
