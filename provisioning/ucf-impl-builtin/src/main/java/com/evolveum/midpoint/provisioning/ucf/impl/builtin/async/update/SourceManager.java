/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncUpdateSource;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update.sources.Amqp091AsyncUpdateSource;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update.sources.JmsAsyncUpdateSource;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.Amqp091SourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateSourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.JmsSourceType;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 *  Creates AsyncUpdateSource objects based on their configurations (AsyncUpdateSourceType objects).
 */
class SourceManager {

    private static final Trace LOGGER = TraceManager.getTrace(SourceManager.class);

    @NotNull private final AsyncUpdateConnectorInstance connectorInstance;

    SourceManager(@NotNull AsyncUpdateConnectorInstance connectorInstance) {
        this.connectorInstance = connectorInstance;
    }

    @NotNull
    Collection<AsyncUpdateSource> createSources(Collection<AsyncUpdateSourceType> sourceConfigurations) {
        if (sourceConfigurations.isEmpty()) {
            throw new IllegalStateException("No asynchronous update sources are configured");
        }
        return sourceConfigurations.stream()
                .map(this::createSource)
                .collect(Collectors.toList());
    }

    @NotNull
    private AsyncUpdateSource createSource(AsyncUpdateSourceType sourceConfiguration) {
        LOGGER.trace("Creating source from configuration: {}", sourceConfiguration);
        Class<? extends AsyncUpdateSource> sourceClass = determineSourceClass(sourceConfiguration);
        try {
            Method createMethod = sourceClass.getMethod("create", AsyncUpdateSourceType.class, AsyncUpdateConnectorInstance.class);
            AsyncUpdateSource source = (AsyncUpdateSource) createMethod.invoke(null, sourceConfiguration, connectorInstance);
            if (source == null) {
                throw new SystemException("Asynchronous update source was not created for " + sourceClass);
            }
            return source;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
            throw new SystemException("Couldn't instantiate asynchronous update source class " + sourceClass + ": " + e.getMessage(), e);
        }
    }

    private Class<? extends AsyncUpdateSource> determineSourceClass(AsyncUpdateSourceType cfg) {
        if (cfg.getClassName() != null) {
            try {
                //noinspection unchecked
                return ((Class<? extends AsyncUpdateSource>) Class.forName(cfg.getClassName()));
            } catch (ClassNotFoundException e) {
                throw new SystemException("Couldn't find async source implementation class: " + cfg.getClassName());
            }
        } else if (cfg instanceof JmsSourceType) {
            return JmsAsyncUpdateSource.class;
        } else if (cfg instanceof Amqp091SourceType) {
            return Amqp091AsyncUpdateSource.class;
        } else {
            throw new SystemException("Couldn't find async update source class for configuration: " + cfg.getClass());
        }
    }
}
