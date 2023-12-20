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

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractSecretsProviderType;

public abstract class AbstractSecretsProviderImpl<T extends AbstractSecretsProviderType> implements SecretsProvider {

    private final T configuration;

    private final Map<String, CacheValue<?>> cache = new ConcurrentHashMap<>();

    private long ttl;

    public AbstractSecretsProviderImpl(T configuration) {
        this.configuration = configuration;
    }

    public T getConfiguration() {
        return configuration;
    }

    @Override
    public void init() {
        Duration duration = configuration.getTtl();

        ttl = duration != null ? duration.getTimeInMillis(new Date()) : 0;
    }

    @Override
    public void destroy() {
        cache.clear();
    }

    @Override
    public String getSecretString(@NotNull String key) {
        if (ttl <= 0) {
            return resolveSecretString(key);
        }

        CacheValue<?> value = cache.get(key);
        if (value != null) {
            if (System.currentTimeMillis() <= ttl) {
                if (value.value == null) {
                    return null;
                }

                if (!(value.value instanceof String str)) {
                    throw new IllegalStateException(
                            "Secret value for key " + key + " is not a string, but " + value.value.getClass());
                }
                return str;
            } else {
                cache.remove(key);
            }
        }

        String secret = resolveSecretString(key);
        cache.put(key, new CacheValue<>(secret, System.currentTimeMillis() + ttl));

        return secret;
    }

    @Override
    public ByteBuffer getSecretBinary(@NotNull String key) {
        return SecretsProvider.super.getSecretBinary(key);
    }

    protected abstract String resolveSecretString(@NotNull String key);

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
