/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What kinds of objects can participate in given {@link ShadowReferenceAttribute} or {@link ShadowAssociation}?
 *
 * Some participants are object types, others are object classes. The former is obligatory for legacy simulated associations.
 * Modern simulated references and native references can have class-scoped participants.
 */
public class ShadowRelationParticipantType implements Serializable {

    /** Identification of the object type of the participant, if it's type-scoped. Null for class-scoped ones. */
    @Nullable private final ResourceObjectTypeIdentification typeIdentification;

    /**
     * Definition of the object type of the participant. It may be a genuine type even for class-scoped participants,
     * if the object type is a default one for the object class.
     */
    @NotNull private final ResourceObjectDefinition objectDefinition;

    private ShadowRelationParticipantType(
            @Nullable ResourceObjectTypeIdentification typeIdentification,
            @NotNull ResourceObjectDefinition objectDefinition) {
        this.typeIdentification = typeIdentification;
        this.objectDefinition = objectDefinition;
    }

    static ShadowRelationParticipantType forObjectClass(@NotNull ResourceObjectDefinition definition) {
        return new ShadowRelationParticipantType(null, definition);
    }

    static ShadowRelationParticipantType forObjectType(@NotNull ResourceObjectTypeDefinition definition) {
        return new ShadowRelationParticipantType(
                definition.getTypeIdentification(),
                definition);
    }

    static Collection<ShadowRelationParticipantType> forObjectTypes(Collection<? extends ResourceObjectTypeDefinition> definitions) {
        return definitions.stream()
                .map(def -> forObjectType(def))
                .toList();
    }

    static @NotNull ShadowRelationParticipantType forObjectDefinition(@NotNull ResourceObjectDefinition definition) {
        var typeDef = definition.getTypeDefinition();
        if (typeDef != null) {
            return forObjectType(typeDef);
        } else {
            return forObjectClass(definition);
        }
    }

    public @Nullable ResourceObjectTypeIdentification getTypeIdentification() {
        return typeIdentification;
    }

    public @NotNull ResourceObjectDefinition getObjectDefinition() {
        return objectDefinition;
    }

    public boolean matches(@NotNull ResourceObjectDefinition definition) {
        var candidateType = definition.getTypeIdentification();
        if (candidateType != null && typeIdentification != null) {
            return candidateType.equals(typeIdentification);
        } else {
            return definition.getObjectClassName().equals(objectDefinition.getObjectClassName());
        }
    }

    public boolean matches(@NotNull ShadowType shadow) {
        if (typeIdentification != null) {
            return typeIdentification.equals(ShadowUtil.getTypeIdentification(shadow));
        } else {
            return objectDefinition.getObjectClassName().equals(shadow.getObjectClass());
        }
    }

    @Override
    public String toString() {
        if (typeIdentification != null) {
            return objectDefinition.getObjectClassName().getLocalPart() + " [" + typeIdentification + "]";
        } else {
            return objectDefinition.getObjectClassName().getLocalPart();
        }
    }

    public boolean isWholeClass() {
        return typeIdentification == null;
    }

    public boolean isEntitlement() {
        return typeIdentification != null && typeIdentification.getKind() == ShadowKindType.ENTITLEMENT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ShadowRelationParticipantType that = (ShadowRelationParticipantType) o;
        return Objects.equals(typeIdentification, that.typeIdentification)
                && Objects.equals(objectDefinition, that.objectDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeIdentification, objectDefinition);
    }
}
