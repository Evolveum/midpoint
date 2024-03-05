/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import java.nio.ByteBuffer;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.SecretsProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecretsProviderType;

/**
 * Base implementation of {@link SecretsProvider} interface.
 */
public abstract class SecretsProviderImpl<T extends SecretsProviderType> implements SecretsProvider<T> {

    private final T configuration;

    public SecretsProviderImpl(@NotNull T configuration) {
        this.configuration = configuration;
    }

    @Override
    public @NotNull T getConfiguration() {
        return configuration;
    }

    @Override
    public @NotNull String getIdentifier() {
        return configuration.getIdentifier();
    }

    @Override
    public String getSecretString(@NotNull String key) throws EncryptionException {
        return resolveSecret(key, String.class);
    }

    @Override
    public ByteBuffer getSecretBinary(@NotNull String key) throws EncryptionException {
        return resolveSecret(key, ByteBuffer.class);
    }

    /**
     * Should return secret value for given key or null if the secret does not exist.
     *
     * @throws EncryptionException if the secret cannot be resolved (e.g. due to network problems, or unforeseen error)
     */
    protected abstract <ST> ST resolveSecret(@NotNull String key, @NotNull Class<ST> type) throws EncryptionException;

    protected <ST> ST mapValue(byte[] value, Class<ST> type) {
        if (value == null) {
            return null;
        }

        if (type == String.class) {
            return (ST) new String(value);
        } else if (type == ByteBuffer.class) {
            return (ST) ByteBuffer.wrap(value);
        }

        throw new IllegalStateException("Unsupported type " + type);
    }
}
