/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.crypto.SecretsProvider;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractSecretsProviderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DockerSecretsProviderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecretsProvidersType;

@Component
public class SecretsProviderManager {

    private static final Map<Class<? extends AbstractSecretsProviderType>, Class<? extends SecretsProvider>> PROVIDER_TYPES =
            Map.ofEntries(
                    Map.entry(DockerSecretsProviderType.class, DockerSecretsProvider.class)
            );

    public synchronized void configure(SecretsProviderConsumer consumer, SecretsProvidersType configuration) {
        if (configuration == null) {
            configuration = new SecretsProvidersType();
        }

        List<AbstractSecretsProviderType> configurations = new ArrayList<>();
        configurations.add(configuration.getDockerSecretsProvider());
        configurations.add(configuration.getKubernetesSecretsProvider());

        configurations = configurations.stream()
                .filter(c -> c != null)
                .sorted(Comparator.nullsLast(Comparator.comparing(AbstractSecretsProviderType::getOrder)))
                .toList();

        Map<String, SecretsProvider> existingProviders = consumer.getSecretsProviders().stream()
                .collect(Collectors.toMap(SecretsProvider::getIdentifier, p -> p));

        for (AbstractSecretsProviderType config : configurations) {
            SecretsProvider newProvider = createProvider(config);
            if (newProvider == null) {
                continue;
            }

            consumer.addSecretsProvider(newProvider);
        }

        // we'll just clear existing providers
        existingProviders.values().forEach(p -> destroyProvider(consumer, p));
    }

    private void destroyProvider(SecretsProviderConsumer consumer, SecretsProvider provider) {
        try {
            consumer.removeSecretsProvider(provider);

            provider.destroy();
        } catch (Exception ex) {
            throw new SystemException("Couldn't destroy secrets provider: " + provider.getIdentifier(), ex);
        }
    }

    private <C extends AbstractSecretsProviderType> SecretsProvider createProvider(C configuration) {
        if (configuration == null) {
            return null;
        }

        Class<? extends SecretsProvider> providerClass = PROVIDER_TYPES.get(configuration.getClass());
        if (providerClass == null) {
            throw new SystemException(
                    "Unknown secrets provider type for configuration of type: " + configuration.getClass());
        }

        try {
            SecretsProvider provider = providerClass.getConstructor(configuration.getClass()).newInstance(configuration);
            provider.init();

            return provider;
        } catch (Exception ex) {
            throw new SystemException(
                    "Couldn't create secrets provider instance for configuration of type: " + configuration.getClass(), ex);
        }
    }
}
