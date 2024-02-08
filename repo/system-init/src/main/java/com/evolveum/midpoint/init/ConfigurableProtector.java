/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.ProtectedData;
import com.evolveum.midpoint.prism.crypto.SecretsProvider;
import com.evolveum.midpoint.prism.crypto.SecretsResolver;
import com.evolveum.midpoint.prism.impl.crypto.KeyStoreBasedProtectorImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ExternalDataType;

/**
 * TODO better name (also for factory)
 */
public class ConfigurableProtector extends KeyStoreBasedProtectorImpl implements SecretsResolver {

    private final Map<String, SecretsProvider<?>> providers = new ConcurrentHashMap<>();

    @Override
    public void addSecretsProvider(@NotNull SecretsProvider<?> provider) {
        providers.put(provider.getIdentifier(), provider);
    }

    @Override
    public void removeSecretsProvider(@NotNull SecretsProvider<?> provider) {
        providers.remove(provider.getIdentifier());
    }

    @NotNull
    @Override
    public List<SecretsProvider<?>> getSecretsProviders() {
        return List.copyOf(providers.values());
    }

    @Override
    public <T> void decrypt(ProtectedData<T> protectedData) throws EncryptionException, SchemaException {
        ExternalDataType external = protectedData.getExternalData();
        if (external == null) {
            super.decrypt(protectedData);
            return;
        }

        ByteBuffer value = resolveExternalData(external, ByteBuffer.class);
        protectedData.setClearBytes(value.array());
    }

    @Override
    public String decryptString(ProtectedData<String> protectedString) throws EncryptionException {
        ExternalDataType external = protectedString.getExternalData();
        if (external == null) {
            return super.decryptString(protectedString);
        }

        return resolveExternalData(external, String.class);
    }

    @Override
    protected <T> byte[] decryptBytes(ProtectedData<T> protectedData) throws SchemaException, EncryptionException {
        ExternalDataType external = protectedData.getExternalData();
        if (external == null) {
            return super.decryptBytes(protectedData);
        }

        ByteBuffer buffer = resolveExternalData(external, ByteBuffer.class);
        return buffer.array();
    }

    private <T> T resolveExternalData(ExternalDataType external, Class<T> type) throws EncryptionException {
        String provider = external.getProvider();
        String key = external.getKey();
        if (provider == null) {
            throw new EncryptionException("No provider specified for key " + key);
        }

        if (key == null) {
            throw new EncryptionException("No key specified for provider " + provider);
        }

        SecretsProvider<?> secretsProvider = providers.get(provider);
        if (secretsProvider == null) {
            throw new EncryptionException("No secrets provider with identifier " + provider + " found");
        }

        T value;
        if (type == String.class) {
            value = (T) secretsProvider.getSecretString(key);
        } else if (type == ByteBuffer.class) {
            value = (T) secretsProvider.getSecretBinary(key);
        } else {
            throw new EncryptionException("Unsupported external data type " + type);
        }

        if (value == null) {
            throw new EncryptionException("No secret with key " + key + " found in provider " + provider);
        }

        return value;
    }

    @Override
    public <T> void encrypt(ProtectedData<T> protectedData) throws EncryptionException {
        ExternalDataType external = protectedData.getExternalData();
        if (external == null) {
            super.encrypt(protectedData);
            return;
        }

        // nothing to encrypt (data are stored externally), just remove clear text data

        protectedData.destroyCleartext();
    }
}
