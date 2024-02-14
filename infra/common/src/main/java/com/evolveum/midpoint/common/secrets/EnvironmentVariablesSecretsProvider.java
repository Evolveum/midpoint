/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EnvironmentVariablesSecretsProviderType;

public class EnvironmentVariablesSecretsProvider extends SecretsProviderImpl<EnvironmentVariablesSecretsProviderType> {

    private static final Trace LOGGER = TraceManager.getTrace(EnvironmentVariablesSecretsProvider.class);

    public EnvironmentVariablesSecretsProvider(EnvironmentVariablesSecretsProviderType configuration) {
        super(configuration);
    }

    @Override
    protected <ST> ST resolveSecret(@NotNull String key, @NotNull Class<ST> type) throws EncryptionException {
        String prefix = getConfiguration().getPrefix();

        String finalKey = StringUtils.isNotEmpty(prefix) ? prefix + key : key;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Reading secret from environment variable {}", finalKey);
        }

        String value = System.getenv(finalKey);

        return mapValue(value, type);
    }
}
