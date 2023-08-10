/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * For internal use. TODO better name
 */
interface ConfigurationItemable<T extends Serializable & Cloneable> {

    /** See {@link ConfigurationItem#value}. */
    @NotNull T value();

    /** See {@link ConfigurationItem#origin}. */
    @NotNull ConfigurationItemOrigin origin();

    <X extends ConfigurationItem<T>> @NotNull X as(@NotNull Class<X> clazz);

    @NotNull String fullDescription();
}
