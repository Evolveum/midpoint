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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecretsProviderType;

public abstract class SecretsProviderImpl<T extends SecretsProviderType> implements SecretsProvider<T> {

    private static final Trace LOGGER = TraceManager.getTrace(SecretsProviderImpl.class);

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
            LOGGER.trace("Cache hit for key {}", key);

            if (Clock.get().currentTimeMillis() - value.ttl <= ttl) {
                LOGGER.trace("Cache entry for key {} is still valid, using cached value", key);

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
                LOGGER.trace("Cache entry for key {} expired", key);

                cache.remove(key);
            }
        } else {
            LOGGER.trace("Cache miss for key {}", key);
        }

        ST secret = resolveSecret(key, type);

        if (ttl > 0) {
            LOGGER.trace("Caching secret for key {}", key);
            cache.put(key, new CacheValue<>(secret, Clock.get().currentTimeMillis() + ttl));
        }

        return secret;
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
