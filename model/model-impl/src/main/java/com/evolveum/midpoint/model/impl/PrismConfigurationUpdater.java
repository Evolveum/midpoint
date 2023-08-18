/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PrismConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import static com.evolveum.midpoint.prism.PrismStaticConfiguration.*;

@Component
public class PrismConfigurationUpdater implements SystemConfigurationChangeListener {

    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        if (value == null) {
            return;
        }
        PrismConfigurationType prismConfig = value.getInternals() != null ? value.getInternals().getPrism() : null;
        setJavaSerializationProxiesEnabled(value(Boolean.class,prismConfig, PrismConfigurationType.F_USE_SERIALIZATION_PROXIES,true));
        setPropertyIndexEnabled(value(Boolean.class,prismConfig, PrismConfigurationType.F_INDEX_PROPERTY_VALUES, false));
        setPropertyIndexThreshold(value(Integer.class,prismConfig, PrismConfigurationType.F_PROPERTY_VALUES_INDEX_THRESHOLD, 50));
    }

    private <V> V value(Class<V> vclass, PrismConfigurationType object, QName name, V defaultValue) {
        if (object == null) {
            return defaultValue;
        }
        @SuppressWarnings("unchecked")
        PrismContainerValue<PrismConfigurationType> pcv = (object.asPrismContainerValue());
        V value = pcv.getPropertyRealValue(name, vclass);
        return value != null ? value : defaultValue;
    }

    @PostConstruct
    public void init() {
        systemConfigurationChangeDispatcher.registerListener(this);
    }

    @PreDestroy
    public void shutdown() {
        systemConfigurationChangeDispatcher.unregisterListener(this);
    }
}
