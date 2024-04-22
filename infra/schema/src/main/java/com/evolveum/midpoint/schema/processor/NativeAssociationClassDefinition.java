/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.DebugDumpable;

/**
 * Definition of an association class as seen by the connector (or defined in the simulated associations capability).
 */
public interface NativeAssociationClassDefinition
        extends Cloneable, Serializable, DebugDumpable {

    String getName();

    /**
     * {@link ShadowAssociationParticipantRole#SUBJECT} participants in this association. Never empty.
     */
    @NotNull Collection<NativeParticipant> getSubjects();

    /**
     * {@link ShadowAssociationParticipantRole#OBJECT} participants in this association. Never empty.
     */
    @NotNull Collection<NativeParticipant> getObjects();

    void addParticipant(
            @NotNull String objectClassName,
            @NotNull ItemName associationName,
            @NotNull ShadowAssociationParticipantRole role);

    record NativeParticipant(@NotNull String objectClassName, @NotNull ItemName associationName) implements Serializable {
    }
}
