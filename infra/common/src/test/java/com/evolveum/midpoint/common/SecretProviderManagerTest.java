/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.secrets.SecretsProviderManager;
import com.evolveum.midpoint.prism.crypto.SecretsProvider;
import com.evolveum.midpoint.prism.crypto.SecretsResolver;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DockerSecretsProviderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EnvironmentVariablesSecretsProviderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertiesSecretsProviderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecretsProvidersType;

public class SecretProviderManagerTest {

    @Test
    public void test100TestProvidersInitialization() throws Exception {
        DockerSecretsProviderType docker = new DockerSecretsProviderType();
        docker.setIdentifier("docker");

        EnvironmentVariablesSecretsProviderType env = new EnvironmentVariablesSecretsProviderType();
        env.setIdentifier("env");

        PropertiesSecretsProviderType properties = new PropertiesSecretsProviderType();
        properties.setIdentifier("properties");
        properties.setPropertiesFile("./src/test/resources/test-provider.properties");
        properties.setCache(XmlTypeConverter.createDuration("PT30S"));

        SecretsProvidersType providers = new SecretsProvidersType();
        providers.setDockerSecretsProvider(docker);
        providers.setEnvironmentVariablesSecretsProvider(env);
        providers.getPropertiesSecretsProvider().add(properties);

        Map<String, SecretsProvider<?>> map = new ConcurrentHashMap<>();

        SecretsResolver resolver = new SecretsResolver() {

            @Override
            public void addSecretsProvider(@NotNull SecretsProvider<?> provider) {
                map.put(provider.getIdentifier(), provider);
            }

            @Override
            public void removeSecretsProvider(@NotNull SecretsProvider<?> provider) {
                map.remove(provider.getIdentifier());
            }

            @Override
            public @NotNull List<SecretsProvider<?>> getSecretsProviders() {
                return new ArrayList<>(map.values());
            }
        };

        SecretsProviderManager manager = new SecretsProviderManager();
        manager.configure(resolver, providers);

        Assertions.assertThat(map).hasSize(3);

        String value = map.get("properties").getSecretString("sample.key");
        Assertions.assertThat(value).isEqualTo("jdoe");
    }
}
