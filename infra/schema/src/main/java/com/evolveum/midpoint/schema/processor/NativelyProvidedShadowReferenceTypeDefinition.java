/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.processor.NativeReferenceTypeDefinition.NativeParticipant;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Shadow reference type that is backed by a native implementation.
 */
public class NativelyProvidedShadowReferenceTypeDefinition extends AbstractShadowReferenceTypeDefinition {

    @NotNull private final NativeReferenceTypeDefinition nativeTypeDef;
    @NotNull private final Collection<ShadowRelationParticipantType> subjectTypes;
    @NotNull private final Collection<ShadowRelationParticipantType> objectTypes;

    private NativelyProvidedShadowReferenceTypeDefinition(
            @NotNull NativeReferenceTypeDefinition nativeTypeDef,
            @NotNull Collection<ShadowRelationParticipantType> subjectTypes,
            @NotNull Collection<ShadowRelationParticipantType> objectTypes,
            @NotNull ResourceSchema schema) throws ConfigurationException {
        super(
                nativeTypeDef.getName(),
                computeGeneralizedObjectSideObjectDefinition(
                        objectTypes.stream()
                                .map(ShadowRelationParticipantType::getObjectDefinition)
                                .toList(),
                        List.of(),
                        ConfigurationItem.embedded(new ResourceType()),
                        schema));
        this.nativeTypeDef = nativeTypeDef;
        this.subjectTypes = subjectTypes;
        this.objectTypes = objectTypes;
    }

    public static NativelyProvidedShadowReferenceTypeDefinition create(
            @NotNull NativeReferenceTypeDefinition nativeRefTypeDef, @NotNull ResourceSchema schema)
            throws ConfigurationException {
        return new NativelyProvidedShadowReferenceTypeDefinition(
                nativeRefTypeDef,
                convertParticipants(nativeRefTypeDef.getSubjects(), schema),
                convertParticipants(nativeRefTypeDef.getObjects(), schema),
                schema);
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
        return nativeTypeDef.debugDump(indent);
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
