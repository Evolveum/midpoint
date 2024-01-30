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

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.SecretsProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractSecretsProviderType;

public abstract class CachedSecretsProvider<T extends AbstractSecretsProviderType> implements SecretsProvider {

    private final T configuration;

    private final Map<String, CacheValue<?>> cache = new ConcurrentHashMap<>();

    private long ttl;

    public CachedSecretsProvider(@NotNull T configuration) {
        this.configuration = configuration;
    }

    public @NotNull T getConfiguration() {
        return configuration;
    }

    public void init() {
        Duration duration = configuration.getTtl();

        ttl = duration != null ? duration.getTimeInMillis(new Date()) : 0;
    }

    public void destroy() {
        cache.clear();
    }

    @Override
    public @NotNull String getIdentifier() {
        return configuration.getIdentifier();
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
            if (System.currentTimeMillis() <= ttl) {
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

    protected abstract <ST> ST resolveSecret(@NotNull String key, Class<ST> type) throws EncryptionException;

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
