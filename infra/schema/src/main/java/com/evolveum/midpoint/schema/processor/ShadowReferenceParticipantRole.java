/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
