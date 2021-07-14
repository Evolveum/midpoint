/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class LiveSyncToken {

    @NotNull private final Object value;

    public LiveSyncToken(@NotNull Object value) {
        this.value = value;
    }

    public static @NotNull LiveSyncToken of(@NotNull Object value) {
        return new LiveSyncToken(value);
    }

    public @NotNull Object getValue() {
        return value;
    }

    public static Object getValue(LiveSyncToken token) {
        return token != null ? token.getValue() : null;
    }

    @Override
    public String toString() {
        return "token:" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LiveSyncToken that = (LiveSyncToken) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
