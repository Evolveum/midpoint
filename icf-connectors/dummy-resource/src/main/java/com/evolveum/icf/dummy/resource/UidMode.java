/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.icf.dummy.resource;

import org.jetbrains.annotations.NotNull;

/**
 * Determines the behavior of the externally visible UID, i.e., ConnId `Uid` attribute.
 */
public enum UidMode {

    /** Externally visible UID is derived from (bound to) the NAME attribute, regardless of {@link DummyObject#id} value. */
    NAME("name"),

    /** Externally visible UID is the same as internal ID ({@link DummyObject#id}) and is generated as random UUID. */
    UUID("uuid"),

    /**
     * Externally visible UID is the same as internal ID ({@link DummyObject#id}) and is provided by the creator
     * of the dummy objects.
     */
    EXTERNAL("external");

    public static final String V_NAME = "name";
    public static final String V_UUID = "uuid";
    public static final String V_EXTERNAL = "external";

    @NotNull private final String stringValue;

    UidMode(@NotNull String stringValue) {
        this.stringValue = stringValue;
    }

    public @NotNull String getStringValue() {
        return stringValue;
    }

    public static @NotNull UidMode of(String stringValue) {
        for (UidMode value : values()) {
            if (value.stringValue.equals(stringValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown UID mode value: " + stringValue);
    }
}
