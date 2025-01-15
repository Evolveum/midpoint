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

import org.jetbrains.annotations.Nullable;

/**
 * Definition of a reference type as seen by the connector (or defined in the simulated references capability).
 */
public interface NativeReferenceTypeDefinition
        extends Cloneable, Serializable, DebugDumpable {

    String getName();

    /**
     * {@link ShadowReferenceParticipantRole#SUBJECT} participants in this reference. Never empty.
     */
    @NotNull Collection<NativeParticipant> getSubjects();

    /**
     * {@link ShadowReferenceParticipantRole#OBJECT} participants in this reference. Never empty.
     */
    @NotNull Collection<NativeParticipant> getObjects();

    void addParticipant(
            @NotNull String objectClassName,
            @Nullable ItemName referenceAttributeName,
            @NotNull ShadowReferenceParticipantRole role);

    void addParticipantIfNotThere(@NotNull String objectClassName, @NotNull ShadowReferenceParticipantRole role);

    record NativeParticipant(@NotNull String objectClassName, @Nullable ItemName referenceAttributeName) implements Serializable {
    }
}
