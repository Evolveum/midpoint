/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.model.api.correlator.CorrelatorFactory;
import com.evolveum.midpoint.model.api.correlator.CorrelatorFactoryRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

@Component
public class CorrelatorFactoryRegistryImpl implements CorrelatorFactoryRegistry {

    /**
     * Registered factories. Keyed by the configuration item name (must be qualified!)
     */
    private final Map<QName, CorrelatorFactory<?, ?>> factories = new ConcurrentHashMap<>();

    @Override
    public void registerFactory(@NotNull QName name, @NotNull CorrelatorFactory<?, ?> factory) {
        argCheck(QNameUtil.isQualified(name), "Correlator factory name is not qualified: %s for %s", name, factory);
        factories.put(name, factory);
    }

    @Override
    public CorrelatorFactory<?, ?> getFactoryByConfigurationItemName(@NotNull QName name) {
        argCheck(QNameUtil.isQualified(name), "Unqualified configuration item name: " + name);
        return MiscUtil.requireNonNull(
                factories.get(name),
                () -> new IllegalStateException("No correlator factory for configuration item '" + name + "'"));
    }

    @Override
    public <CB extends AbstractCorrelatorType> @NotNull Correlator instantiateCorrelator(
            @NotNull CB correlatorConfiguration,
            @NotNull Task task,
            @NotNull OperationResult result) throws ConfigurationException {
        //noinspection unchecked
        CorrelatorFactory<?, CB> factory = (CorrelatorFactory<?, CB>)
                findByConfigurationBeanType(correlatorConfiguration.getClass());
        return factory.instantiate(correlatorConfiguration, task, result);
    }

    private <CB extends AbstractCorrelatorType> @NotNull CorrelatorFactory<?, CB> findByConfigurationBeanType(
            @NotNull Class<CB> type) {
        //noinspection unchecked
        return MiscUtil.extractSingletonRequired(
                factories.values().stream()
                        .filter(factory -> factory.getConfigurationBeanType().equals(type))
                        .map(factory -> (CorrelatorFactory<?, CB>) factory)
                        .collect(Collectors.toList()),
                () -> new IllegalStateException("Multiple correlator factories for configuration " + type),
                () -> new IllegalArgumentException("No correlator factory for configuration " + type));
    }

    @Override
    public @NotNull Correlator instantiateCorrelator(
            @NotNull AbstractCorrelatorType correlatorConfiguration,
            @NotNull QName configurationItemName,
            @NotNull Task task,
            @NotNull OperationResult result) throws ConfigurationException {
        //noinspection unchecked
        return ((CorrelatorFactory<?, AbstractCorrelatorType>) getFactoryByConfigurationItemName(configurationItemName))
                .instantiate(correlatorConfiguration, task, result);
    }
}
