/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.crypto.SecretsProvider;
import com.evolveum.midpoint.prism.crypto.SecretsResolver;
import com.evolveum.midpoint.util.DependencyGraph;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Manages secrets providers instances.
 * Wraps all new secrets providers with {@link CacheableSecretsProviderDelegate} to handle caching and key validation.
 *
 * It's used to handle configuration changes in {@link SystemConfigurationType} related to secrets providers.
 */
@Component
public class SecretsProviderManager {

    private static final Trace LOGGER = TraceManager.getTrace(SecretsProviderManager.class);

    private static final Map<Class<? extends SecretsProviderType>, Class<? extends SecretsProvider<? extends SecretsProviderType>>> PROVIDER_TYPES =
            Map.ofEntries(
                    Map.entry(DockerSecretsProviderType.class, DockerSecretsProvider.class),
                    Map.entry(PropertiesSecretsProviderType.class, PropertiesSecretsProvider.class),
                    Map.entry(EnvironmentVariablesSecretsProviderType.class, EnvironmentVariablesSecretsProvider.class),
                    Map.entry(FileSecretsProviderType.class, FileSecretsProvider.class)
            );

    public synchronized void configure(SecretsResolver consumer, SecretsProvidersType configuration) {
        if (configuration == null) {
            configuration = new SecretsProvidersType();
        }

        Map<String, SecretsProvider<?>> existingProviders = consumer.getSecretsProviders().stream()
                .collect(Collectors.toMap(SecretsProvider::getIdentifier, p -> p));

        LOGGER.debug("Existing providers: {}", existingProviders.keySet());

        List<SecretsProviderType> configurations = new ArrayList<>();
        configurations.add(configuration.getDocker());
        configurations.addAll(configuration.getEnvironmentVariables());
        configurations.addAll(configuration.getFile());
        configurations.addAll(configuration.getProperties());
        configurations.addAll(configuration.getCustom());

        Map<String, SecretsProvider<?>> newProviders = configurations.stream()
                .map(c -> createProvider(c))
                .filter(p -> p != null)
                .collect(Collectors.toMap(SecretsProvider::getIdentifier, p -> p));

        LOGGER.debug("Preparing new providers: {}", newProviders.keySet());

        Map<String, Collection<String>> dependencies = newProviders.values().stream()
                .collect(Collectors.toMap(
                        p -> p.getIdentifier(),
                        p -> Arrays.asList(p.getDependencies())));

        List<String> sorted = DependencyGraph.ofMap(dependencies).getSortedItems();

        LOGGER.debug("Sorted providers by dependencies: {}", sorted);

        for (String identifier : sorted) {
            LOGGER.trace("Initializing secrets provider: {}", identifier);

            SecretsProvider<?> provider = newProviders.get(identifier);
            provider.initialize();

            LOGGER.trace("Adding secrets provider: {} to resolver", identifier);

            consumer.addSecretsProvider(provider);
            existingProviders.remove(provider.getIdentifier());
        }

        LOGGER.debug("Removing remaining old providers: {}", existingProviders.keySet());
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

    private void destroyProvider(SecretsResolver consumer, SecretsProvider<?> provider) {
        try {
            LOGGER.trace("Removing secrets provider: {} from resolver", provider.getIdentifier());
            consumer.removeSecretsProvider(provider);

            LOGGER.trace("Destroying secrets provider: {}", provider.getIdentifier());
            provider.destroy();
        } catch (Exception ex) {
            throw new SystemException("Couldn't destroy secrets provider: " + provider.getIdentifier(), ex);
        }
    }

    private <C extends SecretsProviderType> SecretsProvider<?> createProvider(C configuration) {
        if (configuration == null) {
            return null;
        }

        SecretsProvider<?> provider = createProviderImpl(configuration);

        return new CacheableSecretsProviderDelegate<>(provider, configuration.getCache());
    }

    @SuppressWarnings("unchecked")
    private <C extends SecretsProviderType> SecretsProvider<?> createProviderImpl(C configuration) {
        if (configuration == null) {
            return null;
        }

        Class<? extends SecretsProvider<?>> providerClass;
        if (configuration instanceof CustomSecretsProviderType custom) {
            String className = custom.getClassName();
            if (className == null) {
                throw new SystemException("No class name specified for custom secrets provider");
            }

            try {
                providerClass = (Class<? extends SecretsProvider<?>>) Class.forName(className);
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
            return providerClass
                    .getConstructor(configuration.getClass())
                    .newInstance(configuration);
        } catch (Exception ex) {
            throw new SystemException(
                    "Couldn't create secrets provider instance for configuration of type: " + configuration.getClass(), ex);
        }
    }
}
