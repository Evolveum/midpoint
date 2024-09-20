/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.SecretsProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EnvironmentVariablesSecretsProviderType;

/**
 * Custom implementation of {@link SecretsProvider} that reads secrets from environment variables.
 *
 * Note: This implementation doesn't read secrets from system properties (e.g. -Dkey=value parameters).
 */
public class EnvironmentVariablesSecretsProvider extends SecretsProviderImpl<EnvironmentVariablesSecretsProviderType> {

    public EnvironmentVariablesSecretsProvider(EnvironmentVariablesSecretsProviderType configuration) {
        super(configuration);
    }

    @Override
    protected <ST> ST resolveSecret(@NotNull String key, @NotNull Class<ST> type) {
        String value = System.getenv(key);

        if (value == null && BooleanUtils.isTrue(getConfiguration().isUseSystemProperties())) {
            value = System.getProperty(key);
        }

        byte[] data = value != null ? value.getBytes() : null;

        return mapValue(data, type);
    }
}
