/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EnvironmentVariablesSecretsProviderType;

public class EnvironmentVariablesSecretsProvider extends CachedSecretsProvider<EnvironmentVariablesSecretsProviderType> {

    public EnvironmentVariablesSecretsProvider(EnvironmentVariablesSecretsProviderType configuration) {
        super(configuration);
    }

    @Override
    protected <ST> ST resolveSecret(@NotNull String key, @NotNull Class<ST> type) throws EncryptionException {
        String prefix = getConfiguration().getPrefix();

        if (prefix != null && !key.startsWith(prefix)) {
            throw new EncryptionException("Key not available in provider " + getIdentifier() + ": " + key);
        }

        String value = System.getenv(key);

        return mapValue(value, type);
    }
}
