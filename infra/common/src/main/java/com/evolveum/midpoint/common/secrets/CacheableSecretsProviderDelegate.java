/*
 * Copyright (C) 2010-2024 Evolveum and contributors
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

public class CacheableSecretsProviderDelegate<C> implements SecretsProvider<C> {

    private static final Trace LOGGER = TraceManager.getTrace(CacheableSecretsProviderDelegate.class);

    private static final long DEFAULT_TTL = 0;

    private final SecretsProvider<C> delegate;

    private final Map<String, CacheValue<?>> cache = new ConcurrentHashMap<>();

    private final long ttl;

    public CacheableSecretsProviderDelegate(@NotNull SecretsProvider<C> delegate, @NotNull Duration duration) {
        this.delegate = delegate;

        ttl = duration != null ? duration.getTimeInMillis(new Date()) : DEFAULT_TTL;
    }

    @Override
    public void initialize() {
        delegate.initialize();
    }

    @Override
    public void destroy() {
        delegate.destroy();

        cache.clear();
    }

    @Override
    public @NotNull String getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public @NotNull String[] getDependencies() {
        return delegate.getDependencies();
    }

    @Override
    public C getConfiguration() {
        return delegate.getConfiguration();
    }

    @Override
    public ByteBuffer getSecretBinary(@NotNull String key) throws EncryptionException {
        return getOrResolveSecret(key, ByteBuffer.class);
    }

    @Override
    public String getSecretString(@NotNull String key) throws EncryptionException {
        return getOrResolveSecret(key, String.class);
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

    protected <ST> ST resolveSecret(@NotNull String key, @NotNull Class<ST> type) throws EncryptionException {
        if (type == String.class) {
            return (ST) delegate.getSecretString(key);
        } else if (type == ByteBuffer.class) {
            return (ST) delegate.getSecretBinary(key);
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
