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

import com.evolveum.midpoint.util.SingleLocalizableMessage;

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
    public @NotNull String resolveSecretString(@NotNull String provider, @NotNull String key) throws EncryptionException {
        ExternalDataType data = new ExternalDataType();
        data.setProvider(provider);
        data.setKey(key);

        return resolveExternalData(data, String.class);
    }

    @Override
    public @NotNull ByteBuffer resolveSecretBinary(@NotNull String provider, @NotNull String key) throws EncryptionException {
        ExternalDataType data = new ExternalDataType();
        data.setProvider(provider);
        data.setKey(key);

        return resolveExternalData(data, ByteBuffer.class);
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
            SingleLocalizableMessage message = new SingleLocalizableMessage(
                    "ConfigurableProtector.noProvider",
                    new Object[] { key },
                    "No provider specified for key " + key);
            throw new EncryptionException(message);
        }

        if (key == null) {
            SingleLocalizableMessage message = new SingleLocalizableMessage(
                    "ConfigurableProtector.noKey",
                    new Object[] { provider },
                    "No key specified for provider " + provider);
            throw new EncryptionException(message);
        }

        SecretsProvider<?> secretsProvider = providers.get(provider);
        if (secretsProvider == null) {
            SingleLocalizableMessage message = new SingleLocalizableMessage(
                    "ConfigurableProtector.unknownProviderIdentifier",
                    new Object[] { provider },
                    "No secrets provider with identifier " + provider + " found");
            throw new EncryptionException(message);
        }

        T value;
        if (type == String.class) {
            value = (T) secretsProvider.getSecretString(key);
        } else if (type == ByteBuffer.class) {
            value = (T) secretsProvider.getSecretBinary(key);
        } else {
            SingleLocalizableMessage message = new SingleLocalizableMessage(
                    "ConfigurableProtector.unsupportedExternalDataType",
                    new Object[] { type },
                    "Unsupported external data type " + type);
            throw new EncryptionException(message);
        }

        if (value == null) {
            SingleLocalizableMessage message = new SingleLocalizableMessage(
                    "ConfigurableProtector.noSecretWithKey",
                    new Object[] { key, provider },
                    "No secret with key " + key + " found in provider " + provider);
            throw new EncryptionException(message);
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
