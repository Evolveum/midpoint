/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest;

import java.util.LinkedHashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.secrets.SecretsProviderImpl;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomSecretsProviderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomSecretsProviderType;
import com.evolveum.prism.xml.ns._public.types_3.ExternalDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Sample custom secrets provider used by {@link TestSecretProviders}.
 *
 * Demonstrates how a custom provider implementation reads a {@link ProtectedStringType} value
 * from its own custom configuration and decrypts it using the default {@link Protector}.
 * The protected value may itself point to another secrets provider via external data,
 * therefore such providers are reported as dependencies so that they are available first.
 *
 * The value is decrypted when requested, not during initialization, so the clear value
 * (e.g. a connection token to a remote vault) is never cached in the provider instance.
 * It is exposed under {@link #KEY_CONNECTION_TOKEN} so that tests can verify what the provider has read.
 */
public class MyCustomSecretsProvider extends SecretsProviderImpl<CustomSecretsProviderType> {

    public static final String NS_CUSTOM = "http://midpoint.evolveum.com/xml/ns/public/custom/custom-3";

    public static final ItemName F_CONNECTION_TOKEN = new ItemName(NS_CUSTOM, "customProtectedStringConfigurationAttribute");

    public static final String KEY_CONNECTION_TOKEN = "connection-token";

    public MyCustomSecretsProvider(@NotNull CustomSecretsProviderType configuration) {
        super(configuration);
    }

    @Override
    public @NotNull String[] getDependencies() {
        Set<String> dependencies = new LinkedHashSet<>();

        CustomSecretsProviderConfigurationType configuration = getConfiguration().getConfiguration();
        if (configuration == null) {
            return EMPTY_DEPENDENCIES;
        }

        PrismContainerValue<?> value = configuration.asPrismContainerValue();
        for (Item<?, ?> item : value.getItems()) {
            for (Object realValue : item.getRealValues()) {
                if (realValue instanceof ProtectedStringType ps) {
                    ExternalDataType externalData = ps.getExternalData();
                    if (externalData != null && externalData.getProvider() != null) {
                        dependencies.add(externalData.getProvider());
                    }
                }
            }
        }

        return dependencies.toArray(String[]::new);
    }

    private ProtectedStringType getProtectedConfigurationValue(ItemName name) {
        CustomSecretsProviderConfigurationType configuration = getConfiguration().getConfiguration();
        if (configuration == null) {
            return null;
        }

        PrismContainerValue<?> value = configuration.asPrismContainerValue();
        Item<?, ?> item = value.findItem(name);
        if (item == null) {
            return null;
        }

        return item.getRealValue(ProtectedStringType.class);
    }

    @Override
    protected <ST> ST resolveSecret(@NotNull String key, @NotNull Class<ST> type) throws EncryptionException {
        if (!KEY_CONNECTION_TOKEN.equals(key)) {
            return null;
        }

        ProtectedStringType protectedToken = getProtectedConfigurationValue(F_CONNECTION_TOKEN);
        if (protectedToken == null) {
            throw new EncryptionException("No " + F_CONNECTION_TOKEN + " configured for provider " + getIdentifier());
        }

        // decrypted on every request on purpose, the clear value is never kept in this instance
        Protector protector = PrismContext.get().getDefaultProtector();
        String connectionToken = protector.decryptString(protectedToken);

        // Implementation would use connectionToken to authenticate against custom service to obtain real value.
        // This one just returns value for test to assert.

        return mapValue(connectionToken.getBytes(), type);
    }
}
