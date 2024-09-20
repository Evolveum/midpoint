/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.schema.processor.NativeReferenceTypeDefinition.NativeParticipant;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Association class that is backed by a native implementation.
 */
public class NativelyProvidedShadowReferenceTypeDefinition extends AbstractShadowReferenceTypeDefinition {

    @NotNull private final NativeReferenceTypeDefinition nativeClassDef;
    @NotNull private final Collection<ShadowRelationParticipantType> subjectTypes;
    @NotNull private final Collection<ShadowRelationParticipantType> objectTypes;

    private NativelyProvidedShadowReferenceTypeDefinition(
            @NotNull NativeReferenceTypeDefinition nativeClassDef,
            @NotNull Collection<ShadowRelationParticipantType> subjectTypes,
            @NotNull Collection<ShadowRelationParticipantType> objectTypes) {
        super(nativeClassDef.getName(), objectTypes.iterator().next().objectDefinition);
        this.nativeClassDef = nativeClassDef;
        this.subjectTypes = subjectTypes;
        this.objectTypes = objectTypes;
    }

    public static NativelyProvidedShadowReferenceTypeDefinition create(
            @NotNull NativeReferenceTypeDefinition nativeClassDef, @NotNull ResourceSchema schema) {
        return new NativelyProvidedShadowReferenceTypeDefinition(
                nativeClassDef,
                convertParticipants(nativeClassDef.getSubjects(), schema),
                convertParticipants(nativeClassDef.getObjects(), schema));
    }

    @NotNull
    private static Collection<ShadowRelationParticipantType> convertParticipants(
            @NotNull Collection<NativeParticipant> nativeParticipants, @NotNull ResourceSchema schema) {
        return nativeParticipants.stream()
                .map(nativeParticipant ->
                        ShadowRelationParticipantType.forObjectClass(
                                resolveObjectClass(nativeParticipant.objectClassName(), schema)))
                .toList();
    }

    private static ResourceObjectDefinition resolveObjectClass(String name, ResourceSchema schema) {
        return stateNonNull(
                schema.findDefinitionForObjectClass(new QName(NS_RI, name)),
                "No object class definition for '%s' in %s", name, schema);
    }

    @Override
    public String debugDump(int indent) {
        return nativeClassDef.debugDump(indent);
    }

    @Override
    public @NotNull Collection<ShadowRelationParticipantType> getSubjectTypes() {
        return subjectTypes;
    }

    @Override
    public @NotNull Collection<ShadowRelationParticipantType> getObjectTypes() {
        return objectTypes;
    }
}
