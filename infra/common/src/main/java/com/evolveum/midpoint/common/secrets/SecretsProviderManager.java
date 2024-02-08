/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.crypto.SecretsProvider;
import com.evolveum.midpoint.prism.crypto.SecretsResolver;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Manages secrets providers instances.
 *
 * It's used to handle configuration changes in {@link SystemConfigurationType} related to secrets providers.
 */
@Component
public class SecretsProviderManager {

    private static final Trace LOGGER = TraceManager.getTrace(SecretsProviderManager.class);

    private static final Map<Class<? extends SecretsProviderType>, Class<? extends SecretsProvider>> PROVIDER_TYPES =
            Map.ofEntries(
                    Map.entry(DockerSecretsProviderType.class, DockerSecretsProvider.class),
                    Map.entry(PropertiesSecretsProviderType.class, PropertiesSecretsProvider.class),
                    Map.entry(EnvironmentVariablesSecretsProviderType.class, EnvironmentVariablesSecretsProvider.class)
            );

    public synchronized void configure(SecretsResolver consumer, SecretsProvidersType configuration) {
        if (configuration == null) {
            configuration = new SecretsProvidersType();
        }

        Map<String, SecretsProvider> existingProviders = consumer.getSecretsProviders().stream()
                .collect(Collectors.toMap(SecretsProvider::getIdentifier, p -> p));

        List<SecretsProviderType> configurations = new ArrayList<>();
        configurations.add(configuration.getEnvironmentVariablesSecretsProvider());
        configurations.add(configuration.getDockerSecretsProvider());
        configurations.addAll(configuration.getKubernetesSecretsProvider());
        configurations.addAll(configuration.getPropertiesSecretsProvider());
        configurations.addAll(configuration.getCustomSecretsProvider());

        List<SecretsProvider> newProviders = configurations.stream()
                .map(c -> createProvider(c))
                .filter(p -> p != null)
                .toList();

        // todo sort based on dependencies

        for (SecretsProvider provider : newProviders) {
            provider.initialize();

            consumer.addSecretsProvider(provider);
            existingProviders.remove(provider.getIdentifier());
        }

        // we'll just clear existing providers
        existingProviders.values().forEach(p -> destroyProvider(consumer, p));
    }

    public Map<String, DisplayType> getSecretsProviderDescriptions(SecretsResolver consumer) {
        return consumer.getSecretsProviders().stream()
                .collect(Collectors.toMap(
                        SecretsProvider::getIdentifier,
                        p -> {
                            DisplayType display = null;
                            if (p.getConfiguration() instanceof SecretsProviderType spConfig) {
                                display = spConfig.getDisplay();
                            }

                            if (display == null) {
                                display = new DisplayType();
                                display.setLabel(new PolyStringType(p.getIdentifier()));
                            }

                            return display;
                        }));
    }

    private void destroyProvider(SecretsResolver consumer, SecretsProvider provider) {
        try {
            consumer.removeSecretsProvider(provider);

            provider.destroy();
        } catch (Exception ex) {
            throw new SystemException("Couldn't destroy secrets provider: " + provider.getIdentifier(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <C extends SecretsProviderType> SecretsProvider createProvider(C configuration) {
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

            return provider;
        } catch (Exception ex) {
            throw new SystemException(
                    "Couldn't create secrets provider instance for configuration of type: " + configuration.getClass(), ex);
        }
    }
}
