/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.datatype.Duration;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.SecretsProvider;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecretsProviderType;

/**
 * Delegate implementation for secret providers that is able to cache the resolved secrets and check keys for allowed prefixes.
 *
 * @param <C>
 */
public class CacheableSecretsProviderDelegate<C> implements SecretsProvider<C> {

    private static final Trace LOGGER = TraceManager.getTrace(CacheableSecretsProviderDelegate.class);

    private static final long DEFAULT_TTL = 0;

    private final SecretsProvider<C> delegate;

    private final Map<String, CacheValue> cache = new ConcurrentHashMap<>();

    private final long ttl;

    public CacheableSecretsProviderDelegate(@NotNull SecretsProvider<C> delegate, Duration duration) {
        this.delegate = delegate;

        ttl = duration == null || duration.getSign() == -1 ? DEFAULT_TTL : duration.getTimeInMillis(new Date());
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

        CacheValue value = cache.get(key);
        if (value != null) {
            LOGGER.trace("Cache hit for key {}", key);

            if (value.ttl - Clock.get().currentTimeMillis() >= 0) {
                LOGGER.trace("Cache entry for key {} is still valid, using cached value", key);

                return createRealSecretValue(value.value, type);
            } else {
                LOGGER.trace("Cache entry for key {} expired", key);

                cache.remove(key);
            }
        } else {
            LOGGER.trace("Cache miss for key {}", key);
        }

        ST secret = resolveSecret(key, type);

        LOGGER.trace("Caching secret for key {}", key);

        ByteBuffer secretBytes = createCacheableSecretValue(secret);
        cache.put(key, new CacheValue(secretBytes, Clock.get().currentTimeMillis() + ttl));

        return secret;
    }

    private <T> T createRealSecretValue(ByteBuffer secretValue, Class<T> type) {
        if (secretValue == null) {
            return null;
        }

        if (type == String.class) {
            return (T) new String(secretValue.array());
        } else if (type == ByteBuffer.class) {
            return (T) secretValue;
        }

        throw new IllegalStateException(
                "Can't translate cached secret value (" + secretValue.getClass() + ") to type  " + type);

    }

    private <T> ByteBuffer createCacheableSecretValue(@NotNull T secret) {
        if (secret instanceof String str) {
            return ByteBuffer.wrap(str.getBytes());
        } else if (secret instanceof ByteBuffer) {
            return (ByteBuffer) secret;
        }

        throw new IllegalStateException("Unsupported secret type " + secret.getClass());
    }

    protected <ST> ST resolveSecret(@NotNull String key, @NotNull Class<ST> type) throws EncryptionException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resolving secret for key {} of type {}", key, type);
        }

        C config = getConfiguration();
        if (config instanceof SecretsProviderType secretsProvider) {
            List<String> allowKeyPrefixes = secretsProvider.getAllowKeyPrefix();
            boolean matched = allowKeyPrefixes.isEmpty() || allowKeyPrefixes.stream().anyMatch(prefix -> key.startsWith(prefix));

            if (!matched) {
                SingleLocalizableMessage message = new SingleLocalizableMessage(
                        "CacheableSecretsProviderDelegate.noAllowedKey",
                        new Object[] { key },
                        "Key " + key + " is not allowed by the configuration (allowKeyPrefix does not match the key)");
                throw new EncryptionException(message);
            }
        }

        if (type == String.class) {
            return (ST) delegate.getSecretString(key);
        } else if (type == ByteBuffer.class) {
            return (ST) delegate.getSecretBinary(key);
        }

        throw new IllegalStateException("Unsupported type " + type);
    }

    private record CacheValue(ByteBuffer value, long ttl) {

        @Override
        public String toString() {
            return "CacheKey{" +
                    "key='" + value + '\'' +
                    ", ttl=" + ttl +
                    '}';
        }
    }
}
