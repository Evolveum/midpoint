/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning.targets.JmsProvisioningTarget;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArtemisProvisioningTargetType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.JmsProvisioningTargetType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningTarget;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning.targets.ArtemisProvisioningTarget;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncProvisioningTargetType;

/**
 * Creates AsyncProvisioningTarget objects based on their configurations ({@link AsyncProvisioningTargetType} objects).
 */
class TargetManager {

    private static final Trace LOGGER = TraceManager.getTrace(TargetManager.class);

    @NotNull private final AsyncProvisioningConnectorInstance connectorInstance;

    TargetManager(@NotNull AsyncProvisioningConnectorInstance connectorInstance) {
        this.connectorInstance = connectorInstance;
    }

    @NotNull
    List<AsyncProvisioningTarget> createTargets(List<AsyncProvisioningTargetType> targetConfigurations) {
        if (targetConfigurations.isEmpty()) {
            throw new IllegalStateException("No asynchronous provisioning targets are configured");
        }
        return targetConfigurations.stream()
                .map(this::createTarget)
                .collect(Collectors.toList());
    }

    @NotNull
    private AsyncProvisioningTarget createTarget(AsyncProvisioningTargetType targetConfiguration) {
        LOGGER.trace("Creating source from configuration: {}", targetConfiguration);
        Class<? extends AsyncProvisioningTarget> targetClass = determineTargetClass(targetConfiguration);
        try {
            Method createMethod = targetClass.getMethod("create", AsyncProvisioningTargetType.class, AsyncProvisioningConnectorInstance.class);
            AsyncProvisioningTarget target = (AsyncProvisioningTarget) createMethod.invoke(null, targetConfiguration, connectorInstance);
            if (target == null) {
                throw new SystemException("Asynchronous update source was not created for " + targetClass);
            }
            return target;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
            throw new SystemException("Couldn't instantiate asynchronous update source class " + targetClass + ": " + e.getMessage(), e);
        }
    }

    private Class<? extends AsyncProvisioningTarget> determineTargetClass(AsyncProvisioningTargetType cfg) {
        if (cfg.getClassName() != null) {
            try {
                //noinspection unchecked
                return ((Class<? extends AsyncProvisioningTarget>) Class.forName(cfg.getClassName()));
            } catch (ClassNotFoundException e) {
                throw new SystemException("Couldn't find async target implementation class: " + cfg.getClassName());
            }
        } else if (cfg instanceof ArtemisProvisioningTargetType) {
            return ArtemisProvisioningTarget.class;
        } else if (cfg instanceof JmsProvisioningTargetType) {
            return JmsProvisioningTarget.class;
        } else {
            throw new SystemException("Couldn't find async provisioning target class for configuration: " + cfg.getClass());
        }
    }
}
