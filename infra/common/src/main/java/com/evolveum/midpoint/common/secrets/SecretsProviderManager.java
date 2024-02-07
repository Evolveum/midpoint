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
import com.evolveum.midpoint.prism.crypto.SecretsProviderConsumer;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Manages secrets providers instances.
 *
 * It's used to handle configuration changes in {@link SystemConfigurationType} related to secrets providers.
 */
@Component
public class SecretsProviderManager {

    private static final Map<Class<? extends AbstractSecretsProviderType>, Class<? extends SecretsProvider>> PROVIDER_TYPES =
            Map.ofEntries(
                    Map.entry(DockerSecretsProviderType.class, DockerSecretsProvider.class),
                    Map.entry(PropertiesSecretsProviderType.class, PropertiesSecretsProvider.class),
                    Map.entry(EnvironmentVariablesSecretsProviderType.class, EnvironmentVariablesSecretsProvider.class)
            );

    public synchronized void configure(SecretsProviderConsumer consumer, SecretsProvidersType configuration) {
        if (configuration == null) {
            configuration = new SecretsProvidersType();
        }

        List<AbstractSecretsProviderType> configurations = new ArrayList<>();
        configurations.add(configuration.getEnvironmentVariablesSecretsProvider());
        configurations.add(configuration.getDockerSecretsProvider());
        configurations.addAll(configuration.getKubernetesSecretsProvider());
        configurations.addAll(configuration.getPropertiesSecretsProvider());
        configurations.addAll(configuration.getCustomSecretsProvider());

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
            existingProviders.remove(newProvider.getIdentifier());
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

    @SuppressWarnings("unchecked")
    private <C extends AbstractSecretsProviderType> SecretsProvider createProvider(C configuration) {
        if (configuration == null) {
            return null;
        }

        Class<? extends SecretsProvider> providerClass;
        if (configuration instanceof CustomSecretsProviderType custom) {
            String className = custom.getClassName();
            if (className == null) {
                throw new SystemException("No class name specified for custom secrets provider");
            }

            try {
                providerClass = (Class<? extends SecretsProvider>) Class.forName(className);
            } catch (Exception ex) {
                throw new SystemException("Couldn't find custom secrets provider class: " + className, ex);
            }
        } else {
            providerClass = PROVIDER_TYPES.get(configuration.getClass());

            if (providerClass == null) {
                throw new SystemException(
                        "Unknown secrets provider type for configuration of type: " + configuration.getClass());
            }
        }

        try {
            SecretsProvider provider = providerClass
                    .getConstructor(configuration.getClass())
                    .newInstance(configuration);
            provider.init();

            return provider;
        } catch (Exception ex) {
            throw new SystemException(
                    "Couldn't create secrets provider instance for configuration of type: " + configuration.getClass(), ex);
        }
    }
}
