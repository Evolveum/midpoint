/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum Associations {

    /** Simulated associations, no shortcut (secondary) binding. */
    BASIC("basic"),

    /** Simulated associations, using secondary binding. */
    SHORTCUT("shortcut"),

    /** Native references (only for 4.9+). */
    NATIVE("native");

    private static final Associations DEFAULT = NATIVE;

    @NotNull private final String value;

    Associations(@NotNull String value) {
        this.value = value;
    }

    static @NotNull Associations fromValue(@Nullable String value) {
        if (value == null) {
            return DEFAULT;
        }
        for (Associations style : Associations.values()) {
            if (style.value.equalsIgnoreCase(value)) {
                return style;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    public @NotNull String getValue() {
        return value;
    }

    boolean isAssociationShortcut() {
        return this == SHORTCUT;
    }

    boolean isNativeReferences() {
        return this == NATIVE;
    }

    @Override
    public String toString() {
        return value;
    }
}
