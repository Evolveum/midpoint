/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.io.Serializable;
import java.util.Collection;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.processor.NativeAssociationClassDefinition.NativeParticipant;
import com.evolveum.midpoint.schema.processor.ShadowAssociationClassDefinition.Participant;
import com.evolveum.midpoint.util.DebugDumpable;

/** "Source" of shadow association type/class: either native or simulated. */
interface ShadowAssociationClassImplementation extends DebugDumpable, Serializable {

    @NotNull String getName();

    default @NotNull QName getQName() {
        return new QName(NS_RI, getName());
    }

    /** Returns the definitions of the subjects participating on this association class. */
    @NotNull Collection<Participant> getParticipatingSubjects();

    /** Returns the definitions of the objects participating on this association class. */
    @NotNull Collection<Participant> getParticipatingObjects();

    class Native implements ShadowAssociationClassImplementation {

        @NotNull private final NativeAssociationClassDefinition nativeClassDef;
        @NotNull private final Collection<Participant> subjects;
        @NotNull private final Collection<Participant> objects;

        private Native(
                @NotNull NativeAssociationClassDefinition nativeClassDef,
                @NotNull Collection<Participant> subjects,
                @NotNull Collection<Participant> objects) {
            this.nativeClassDef = nativeClassDef;
            this.subjects = subjects;
            this.objects = objects;
        }

        public static Native of(@NotNull NativeAssociationClassDefinition nativeClassDef, @NotNull ResourceSchema schema) {
            return new Native(
                    nativeClassDef,
                    convertParticipants(nativeClassDef.getSubjects(), schema),
                    convertParticipants(nativeClassDef.getObjects(), schema));
        }

        @NotNull
        private static Collection<Participant> convertParticipants(
                @NotNull Collection<NativeParticipant> nativeParticipants, @NotNull ResourceSchema schema) {
            return nativeParticipants.stream()
                    .map(nativeParticipant ->
                            new Participant(
                                    null,
                                    resolveObjectClass(nativeParticipant.objectClassName(), schema),
                                    nativeParticipant.associationName()))
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
        public @NotNull String getName() {
            return nativeClassDef.getName();
        }

        @Override
        public @NotNull Collection<Participant> getParticipatingSubjects() {
            return subjects;
        }

        @Override
        public @NotNull Collection<Participant> getParticipatingObjects() {
            return objects;
        }
    }
}
