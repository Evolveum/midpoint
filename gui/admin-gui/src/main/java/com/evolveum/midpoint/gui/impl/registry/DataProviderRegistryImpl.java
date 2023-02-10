/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.registry;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.registry.DataProviderRegistry;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.gui.api.factory.ContainerValueDataProviderFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiListDataProviderType;

/**
 *
 * Spring based Data Provider registry
 *
 * Factories are expected to be instantiated by Spring using {@link Component} annotation
 * and autowired using {@link Autowired}
 *
 */
@Component
public class DataProviderRegistryImpl implements DataProviderRegistry {

    @Autowired
    List<ContainerValueDataProviderFactory<?,?>> containerValueFactories;

    @Override
    public <T extends Containerable, C extends GuiListDataProviderType> ContainerValueDataProviderFactory<T, C> forContainerValue(
            Class<T> dataType, Class<C> configurationType) {
        for (ContainerValueDataProviderFactory<?, ?> factory : containerValueFactories) {
            if (factory.getConfigurationType().isAssignableFrom(configurationType) && factory.isSupported(dataType)) {
                return factory.specializedFor(dataType, configurationType);
            }
        }
        return null;
    }
}
