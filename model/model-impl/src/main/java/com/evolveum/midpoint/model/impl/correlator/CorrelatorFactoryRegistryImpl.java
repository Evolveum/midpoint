/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.correlator.*;

import com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration.TypedCorrelationConfiguration;
import com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration.UntypedCorrelationConfiguration;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

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
    public <CB extends AbstractCorrelatorType> @NotNull Correlator instantiateCorrelator(
            @NotNull CorrelatorContext<CB> correlatorContext,
            @NotNull Task task,
            @NotNull OperationResult result) throws ConfigurationException {
        CorrelatorFactory<?, CB> factory = getCorrelatorFactory(correlatorContext);
        return factory.instantiate(correlatorContext, task, result);
    }

    private <CB extends AbstractCorrelatorType> CorrelatorFactory<?, CB> getCorrelatorFactory(
            @NotNull CorrelatorContext<CB> correlatorContext) {
        CorrelatorConfiguration configuration = correlatorContext.getConfiguration();
        if (configuration instanceof TypedCorrelationConfiguration) {
            // Actually, configurationBean == configuration.configurationBean!
            //noinspection unchecked
            return (CorrelatorFactory<?, CB>)
                    getFactoryByConfigurationBeanType(configuration.getConfigurationBean().getClass());
        } else if (configuration instanceof UntypedCorrelationConfiguration) {
            //noinspection unchecked
            return (CorrelatorFactory<?, CB>)
                    getFactoryByConfigurationItemName(
                            ((UntypedCorrelationConfiguration) configuration).getConfigurationItemName());
        } else {
            throw new IllegalArgumentException("Unsupported correlator configuration: "
                    + MiscUtil.getValueWithClass(configuration));
        }
    }

    private <CB extends AbstractCorrelatorType> @NotNull CorrelatorFactory<?, CB> getFactoryByConfigurationBeanType(
            @NotNull Class<CB> type) {
        //noinspection unchecked
        return MiscUtil.extractSingletonRequired(
                factories.values().stream()
                        .filter(factory -> factory.getConfigurationBeanType().isAssignableFrom(type))
                        .map(factory -> (CorrelatorFactory<?, CB>) factory)
                        .collect(Collectors.toList()),
                () -> new IllegalStateException("Multiple correlator factories for configuration " + type),
                () -> new IllegalArgumentException("No correlator factory for configuration " + type));
    }

    private CorrelatorFactory<?, ?> getFactoryByConfigurationItemName(@NotNull QName name) {
        argCheck(QNameUtil.isQualified(name), "Unqualified configuration item name: " + name);
        return MiscUtil.requireNonNull(
                factories.get(name),
                () -> new IllegalStateException("No correlator factory for configuration item '" + name + "'"));
    }
}
