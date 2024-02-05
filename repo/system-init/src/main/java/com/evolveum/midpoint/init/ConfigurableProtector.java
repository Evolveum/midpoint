/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.evolveum.midpoint.common.secrets.SecretsProviderConsumer;
import com.evolveum.midpoint.prism.crypto.SecretsProvider;
import com.evolveum.midpoint.prism.impl.crypto.KeyStoreBasedProtectorImpl;

public class ConfigurableProtector extends KeyStoreBasedProtectorImpl implements SecretsProviderConsumer {

    private final Map<String, SecretsProvider> providers = new ConcurrentHashMap<>();

    @Override
    public void addSecretsProvider(SecretsProvider provider) {
        providers.put(provider.getIdentifier(), provider);
    }

    @Override
    public void removeSecretsProvider(SecretsProvider provider) {
        providers.remove(provider.getIdentifier());
    }

    @Override
    public List<SecretsProvider> getSecretsProviders() {
        return List.copyOf(providers.values());
    }
}
