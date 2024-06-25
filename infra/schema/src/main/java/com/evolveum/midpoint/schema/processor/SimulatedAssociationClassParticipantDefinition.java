/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.schema.config.AssociationConfigItem.AttributeBinding;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

/** Defines the simulated association class subject or object. Only partially supported for now! */
public class SimulatedAssociationClassParticipantDefinition implements Serializable, DebugDumpable {

    /** What resource objects may participate in the association as the subject or object? */
    @NotNull private final ResourceObjectSetDelineation delineation;

    /**
     * The type or class of the participant. Although the type information belongs to the upper layers, it's convenient
     * to have it here as well.
     */
    @NotNull private final ShadowRelationParticipantType participantType;

    /** Supported only for subject. */
    @Nullable private final QName auxiliaryObjectClassName;

    SimulatedAssociationClassParticipantDefinition(
            @NotNull QName objectClassName,
            @Nullable ResourceObjectReferenceType baseContext,
            @Nullable SearchHierarchyScope searchHierarchyScope,
            @NotNull ShadowRelationParticipantType participantType,
            @Nullable QName auxiliaryObjectClassName) {
        this.delineation = new ResourceObjectSetDelineation(objectClassName, baseContext, searchHierarchyScope, List.of());
        this.participantType = participantType;
        this.auxiliaryObjectClassName = auxiliaryObjectClassName;
    }

    static SimulatedAssociationClassParticipantDefinition fromObjectTypeDefinition(
            @NotNull ResourceObjectTypeDefinition typeDefinition,
            @Nullable QName auxiliaryObjectClassName) {
        return new SimulatedAssociationClassParticipantDefinition(
                typeDefinition.getTypeName(),
                typeDefinition.getDelineation().getBaseContext(),
                typeDefinition.getDelineation().getSearchHierarchyScope(),
                ShadowRelationParticipantType.forObjectType(typeDefinition),
                auxiliaryObjectClassName);
    }

    @NotNull
    ShadowRelationParticipantType getParticipantType() {
        return participantType;
    }

    public @Nullable QName getAuxiliaryObjectClassName() {
        return auxiliaryObjectClassName;
    }

    public @NotNull ResourceObjectDefinition getObjectDefinition() {
        return participantType.getObjectDefinition();
    }

    public <T> ShadowSimpleAttributeDefinition<T> getObjectAttributeDefinition(@NotNull AttributeBinding binding) {
        return getObjectAttributeDefinition(binding.objectSide());
    }

    public @NotNull ResourceObjectSetDelineation getDelineation() {
        return delineation;
    }

    private <T> ShadowSimpleAttributeDefinition<T> getObjectAttributeDefinition(QName attrName) {
        try {
            return getObjectDefinition().findSimpleAttributeDefinitionRequired(attrName);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "(already checked at schema parse time)");
        }
    }

    public @Nullable ResourceObjectTypeIdentification getTypeIdentification() {
        return participantType.getTypeIdentification();
    }

    public boolean matches(@NotNull ResourceObjectDefinition definition) {
        return participantType.matches(definition);
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "delineation", delineation, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "auxiliaryObjectClassName", auxiliaryObjectClassName, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "participantType", String.valueOf(participantType), indent + 1);
        return sb.toString();
    }
}
