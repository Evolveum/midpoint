/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class UcfSyncToken {

    @NotNull private final Object value;

    private UcfSyncToken(@NotNull Object value) {
        this.value = value;
    }

    public static UcfSyncToken of(@NotNull Object value) {
        return new UcfSyncToken(value);
    }

    public @NotNull Object getValue() {
        return value;
    }

    public static Object getValue(UcfSyncToken token) {
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
        UcfSyncToken that = (UcfSyncToken) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
