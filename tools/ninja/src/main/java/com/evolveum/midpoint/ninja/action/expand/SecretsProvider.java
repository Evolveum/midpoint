/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.expand;

import org.jetbrains.annotations.NotNull;

public interface SecretsProvider {

    default void init() {}

    default void destroy() {}

    String getValue(@NotNull String key);
}
