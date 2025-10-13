/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.processor;

import org.jetbrains.annotations.NotNull;

/** Reference has two sides (participants): subject and object. This enum marks them. */
public enum ShadowReferenceParticipantRole {

    SUBJECT("subject"),
    OBJECT("object");

    @NotNull private final String value;

    ShadowReferenceParticipantRole(@NotNull String value) {
        this.value = value;
    }

    public @NotNull String getValue() {
        return value;
    }

    public @NotNull ShadowReferenceParticipantRole other() {
        return this == SUBJECT ? OBJECT : SUBJECT;
    }
}
