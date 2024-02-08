/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.datatype.Duration;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.SecretsProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecretsProviderType;

public abstract class SecretsProviderImpl<T extends SecretsProviderType> implements SecretsProvider<T> {

    private static final String[] EMPTY_DEPENDENCIES = new String[0];

    private static final long DEFAULT_TTL = 0;

    private final T configuration;

    private final Map<String, CacheValue<?>> cache = new ConcurrentHashMap<>();

    private long ttl;

    public SecretsProviderImpl(@NotNull T configuration) {
        this.configuration = configuration;
    }

    @Override
    public @NotNull T getConfiguration() {
        return configuration;
    }

    @Override
    public void initialize() {
        Duration duration = configuration.getCache();

        ttl = duration != null ? duration.getTimeInMillis(new Date()) : DEFAULT_TTL;
    }

    @Override
    public void destroy() {
        cache.clear();
    }

    @Override
    public @NotNull String getIdentifier() {
        return configuration.getIdentifier();
    }

    @Override
    public @NotNull String[] getDependencies() {
        return EMPTY_DEPENDENCIES;
    }

    @Override
    public String getSecretString(@NotNull String key) throws EncryptionException {
        return getOrResolveSecret(key, String.class);
    }

    @Override
    public ByteBuffer getSecretBinary(@NotNull String key) throws EncryptionException {
        return getOrResolveSecret(key, ByteBuffer.class);
    }

    private <ST> ST getOrResolveSecret(String key, Class<ST> type) throws EncryptionException {
        if (ttl <= 0) {
            return resolveSecret(key, type);
        }

        CacheValue<?> value = cache.get(key);
        if (value != null) {
            if (Clock.get().currentTimeMillis() <= ttl) {
                if (value.value == null) {
                    return null;
                }

                Class<?> clazz = value.value().getClass();
                if (!(type.isAssignableFrom(clazz))) {
                    throw new IllegalStateException(
                            "Secret value for key " + key + " is not a " + type + ", but " + clazz);
                }
                return (ST) value.value();
            } else {
                cache.remove(key);
            }
        }

        ST secret = resolveSecret(key, type);
        cache.put(key, new CacheValue<>(secret, System.currentTimeMillis() + ttl));

        return secret;
    }

    /**
     * TODO document, should it return null or throw exception?
     */
    protected abstract <ST> ST resolveSecret(@NotNull String key, @NotNull Class<ST> type) throws EncryptionException;

    protected <ST> ST mapValue(String value, Class<ST> type) {
        if (value == null) {
            return null;
        }

        if (type == String.class) {
            return (ST) value;
        } else if (type == ByteBuffer.class) {
            return (ST) ByteBuffer.wrap(value.getBytes());
        }

        throw new IllegalStateException("Unsupported type " + type);
    }

    private record CacheValue<T>(T value, long ttl) {

        @Override
        public String toString() {
            return "CacheKey{" +
                    "key='" + value + '\'' +
                    ", ttl=" + ttl +
                    '}';
        }
    }
}
