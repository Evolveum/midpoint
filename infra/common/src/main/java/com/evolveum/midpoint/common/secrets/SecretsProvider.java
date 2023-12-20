/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import java.nio.ByteBuffer;

import org.jetbrains.annotations.NotNull;

public interface SecretsProvider {

    default void init() {
    }

    default void destroy() {
    }

    String getSecretString(@NotNull String key);

    default ByteBuffer getSecretBinary(@NotNull String key) {
        String secretString = getSecretString(key);
        if (secretString == null) {
            return null;
        }

        return ByteBuffer.wrap(secretString.getBytes());
    }
}
