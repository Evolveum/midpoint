/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.config.*;

import com.evolveum.midpoint.util.QNameUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.DESC;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Definition of an association class: a named type of association (linkage) between two shadows.
 *
 * It can be either native or simulated.
 * Each participant can be a set of object definitions: classes or types, potentially mixing both.
 *
 * TODO Do we need a separate concept of association types? (As we have object class and object type.)
 *  Currently, this one serves as both - for associations.
 */
public class ShadowAssociationClassDefinition implements DebugDumpable, Serializable {

    /** Name of this association class, e.g. `ri:membership`. */
    @NotNull private final QName className;

    /** How is this association type implemented? Natively or is it simulated? */
    @NotNull private final ShadowAssociationClassImplementation implementation;

    /** Definition of the subjects that can take part in this association. Must be at least one. */
    @NotNull private final Collection<Participant> subjects;

    /** Definition of the objects that can take part in this association. Must be at least one. */
    @NotNull private final Collection<Participant> objects;

    /** Representative definition of an object. TODO clarify! */
    @NotNull private final ResourceObjectDefinition representativeObjectDefinition;

    private ShadowAssociationClassDefinition(
            @NotNull QName className,
            @NotNull ShadowAssociationClassImplementation implementation,
            @NotNull Collection<Participant> subjects,
            @NotNull Collection<Participant> objects) {
        argCheck(NS_RI.equals(className.getNamespaceURI()), "Wrong namespace in association type name: %s", className);
        this.className = className;
        this.implementation = implementation;
        this.subjects = List.copyOf(subjects);
        this.objects = List.copyOf(objects);
        this.representativeObjectDefinition = objects.iterator().next().objectDefinition;
    }

    static @NotNull ShadowAssociationClassDefinition fromAssociationType(
            @NotNull ShadowAssociationTypeDefinitionConfigItem definitionCI,
            @NotNull ShadowAssociationClassImplementation implementation,
            @NotNull ResourceSchema resourceSchema)
            throws ConfigurationException {
        return new ShadowAssociationClassDefinition(
                definitionCI.getAssociationClassName(),
                implementation,
                getParticipants(definitionCI.getSubject(), implementation.getParticipatingSubjects(), definitionCI, resourceSchema),
                getParticipants(definitionCI.getObject(), implementation.getParticipatingObjects(), definitionCI, resourceSchema));
    }

    static ShadowAssociationClassDefinition fromImplementation(@NotNull ShadowAssociationClassImplementation implementation) {
        return new ShadowAssociationClassDefinition(
                implementation.getQName(),
                implementation,
                implementation.getParticipatingSubjects(),
                implementation.getParticipatingObjects());
    }

    private static Collection<Participant> getParticipants(
            @Nullable ShadowAssociationTypeParticipantDefinitionConfigItem<?> participantCI,
            @NotNull Collection<Participant> fromImplementation,
            @NotNull ShadowAssociationTypeDefinitionConfigItem errorCtxCI,
            @NotNull ResourceSchema resourceSchema) throws ConfigurationException {
        Collection<? extends ResourceObjectTypeIdentification> typeIdentifiers =
                participantCI != null ? participantCI.getTypeIdentifiers() : List.of();
        if (!typeIdentifiers.isEmpty()) {
            // We use this information to override the one coming from the implementation.
            Collection<Participant> refinedParticipants = new ArrayList<>();
            for (var typeIdentifier : typeIdentifiers) {
                var typeDef =
                        errorCtxCI.configNonNull(
                                resourceSchema.getObjectTypeDefinition(typeIdentifier),
                                "No definition for object type %s in %s as used in %s",
                                typeIdentifier, resourceSchema, DESC);
                var participantInImplementation =
                        errorCtxCI.configNonNull(
                                findMatchingParticipant(fromImplementation, typeDef.getObjectClassName()),
                                "No participant for object class %s found; in %s", typeDef.getObjectClassName(), DESC);
                refinedParticipants.add(
                        new Participant(typeDef, participantInImplementation.itemName));
            }
            return refinedParticipants;
        } else {
            // Just take information from the native/simulated definition
            return fromImplementation;
        }
    }

    private static Participant findMatchingParticipant(Collection<Participant> participants, QName objectClassName) {
        return participants.stream()
                .filter(participant -> QNameUtil.match(participant.objectDefinition.getObjectClassName(), objectClassName))
                .findFirst()
                .orElse(null);
    }

    static ShadowAssociationClassDefinition parseLegacy(
            @NotNull ResourceObjectAssociationConfigItem definitionCI,
            @NotNull ShadowAssociationClassSimulationDefinition simulationDefinition,
            @NotNull ResourceObjectTypeDefinition referentialSubjectDefinition, // is currently being built!
            @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions) throws ConfigurationException {
        // The "ref" serves as an association type name; not very nice but harmless. We don't put these definitions to any schema.
        return new ShadowAssociationClassDefinition(
                definitionCI.getAssociationName(),
                simulationDefinition,
                List.of(
                        new Participant(
                                referentialSubjectDefinition.getTypeIdentification(),
                                referentialSubjectDefinition,
                                definitionCI.getAssociationName())),
                Participant.fromObjectTypeDefinitions(objectTypeDefinitions));
    }

    public @NotNull QName getClassName() {
        return className;
    }

    public String getName() {
        return className.getLocalPart();
    }

    @SuppressWarnings("WeakerAccess")
    public @NotNull ResourceObjectDefinition getRepresentativeObjectDefinition() {
        return representativeObjectDefinition;
    }

    public @NotNull Collection<Participant> getObjects() {
        return objects;
    }

    public @NotNull Collection<? extends ResourceObjectDefinition> getObjectObjectDefinitions() {
        return objects.stream()
                .map(participant -> participant.objectDefinition)
                .toList();
    }

    public @Nullable ShadowAssociationClassSimulationDefinition getSimulationDefinition() {
        return implementation instanceof ShadowAssociationClassSimulationDefinition simulationDefinition ?
                simulationDefinition : null;
    }

    /** Requires consistent definition of the association target objects (all are entitlements, or none of them is). */
    public boolean isEntitlement() {
        ResourceObjectTypeIdentification typeIdentification = representativeObjectDefinition.getTypeIdentification();
        return typeIdentification != null && typeIdentification.getKind() == ShadowKindType.ENTITLEMENT;
    }

    public @Nullable String getResourceOid() {
        return representativeObjectDefinition.getResourceOid();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "name=" + className +
                ", implementation=" + implementation +
                ", targetObjectDefinition=" + representativeObjectDefinition +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "name", className, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "implementation", implementation, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "targetObjectDefinition", String.valueOf(representativeObjectDefinition), indent + 1);
        return sb.toString();
    }

    /**
     * What kinds of objects can participate in this association class and under what item name?
     *
     * Some participants are object types, others are object classes. The former is obligatory for legacy simulated associations.
     * Modern simulated associations and native associations can have class-scoped participants.
     */
    public static class Participant implements Serializable {

        /** Identification of the object type of the participant, if it's type-scoped. Null for class-scoped ones. */
        @Nullable final ResourceObjectTypeIdentification typeIdentification;

        /**
         * Definition of the object type of the participant. It may be a genuine type even for class-scoped participants,
         * if the object type is a default one for the object class.
         */
        @NotNull final ResourceObjectDefinition objectDefinition;

        /** Item representing the association on that participant, if it's visible. Must be qualified. */
        @Nullable final ItemName itemName;

        Participant(
                @NotNull ResourceObjectTypeDefinition objectDefinition,
                @Nullable ItemName itemName) {
            this.typeIdentification = objectDefinition.getTypeIdentification();
            this.objectDefinition = objectDefinition;
            this.itemName = itemName;
        }

        Participant(
                @Nullable ResourceObjectTypeIdentification typeIdentification,
                @NotNull ResourceObjectDefinition objectDefinition,
                @Nullable ItemName itemName) {
            this.typeIdentification = typeIdentification;
            this.objectDefinition = objectDefinition;
            this.itemName = itemName;
        }

        static Collection<Participant> fromObjectTypeDefinitions(Collection<? extends ResourceObjectTypeDefinition> definitions) {
            return definitions.stream()
                    .map(def -> new Participant(def, null))
                    .toList();
        }

        public @Nullable ResourceObjectTypeIdentification getTypeIdentification() {
            return typeIdentification;
        }

        public @NotNull ResourceObjectDefinition getObjectDefinition() {
            return objectDefinition;
        }
    }
}
