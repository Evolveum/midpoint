/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

    /** Use with care! See {@link ConfigurationItemOrigin#undetermined()}. */
    static @NotNull <T> OriginProvider<T> undetermined() {
        return item -> ConfigurationItemOrigin.undetermined();
    }
}
