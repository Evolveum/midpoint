/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

public interface OriginProvider<T> {

    /** Provides origin for the given item. */
    @NotNull ConfigurationItemOrigin origin(@NotNull T item);

    static @NotNull <T> OriginProvider<T> embedded() {
        return item -> ConfigurationItemOrigin.embedded(item);
    }

    /** Use with care! See {@link ConfigurationItemOrigin#generated()}. */
    static @NotNull <T> OriginProvider<T> generated() {
        return item -> ConfigurationItemOrigin.generated();
    }
}
