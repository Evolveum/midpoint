/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.reactions;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.sync.action.SynchronizationAction;
import com.evolveum.midpoint.model.impl.sync.action.BaseAction;
import com.evolveum.midpoint.schema.processor.SynchronizationActionDefinition;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractSynchronizationActionType;

/**
 * Instantiates {@link SynchronizationAction} objects.
 */
@Component
public class SynchronizationActionFactory {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationActionFactory.class);

    @NotNull private final Map<String, Class<? extends BaseAction<?>>> classesByActionUri = new HashMap<>();
    @NotNull private final Map<Class<? extends AbstractSynchronizationActionType>, Class<? extends BaseAction<?>>>
            classesByDefinitionBeanClass = new HashMap<>();

    SynchronizationAction getActionInstance(@NotNull ActionInstantiationContext<?> context)
            throws ConfigurationException {
        SynchronizationActionDefinition definition = context.actionDefinition;
        Class<? extends AbstractSynchronizationActionType> beanClass = definition.getNewDefinitionBeanClass();
        String legacyUri = definition.getLegacyActionUri();
        if (beanClass != null) {
            return getByBeanClass(beanClass, context);
        } else if (legacyUri != null) {
            return getByLegacyUri(legacyUri, context);
        } else {
            throw new IllegalArgumentException(
                    "Neither action definition bean class nor legacy action URI present in " + definition);
        }
    }

    private SynchronizationAction getByBeanClass(
            Class<? extends AbstractSynchronizationActionType> beanClass, ActionInstantiationContext<?> context)
            throws ConfigurationException {
        return instantiate(
                MiscUtil.requireNonNull(
                        classesByDefinitionBeanClass.get(beanClass),
                        () -> new ConfigurationException("No synchronization action class found for definition " + beanClass)),
                context);
    }

    private SynchronizationAction getByLegacyUri(String actionUri, ActionInstantiationContext<?> context)
            throws ConfigurationException {
        return instantiate(
                MiscUtil.requireNonNull(
                        classesByActionUri.get(actionUri),
                        () -> new ConfigurationException("No synchronization action class found for URI " + actionUri)),
                context);
    }

    private SynchronizationAction instantiate(
            Class<? extends SynchronizationAction> clazz,
            ActionInstantiationContext<?> context) {
        try {
            return clazz.getDeclaredConstructor(ActionInstantiationContext.class)
                    .newInstance(context);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw SystemException.unexpected(e, "when instantiating synchronization action " + clazz);
        }
    }

    public <T extends BaseAction<?>> void register(Class<T> actionClass) {
        ActionUris urisAnnotation = actionClass.getAnnotation(ActionUris.class);
        if (urisAnnotation != null) {
            for (String uri : urisAnnotation.value()) {
                classesByActionUri.put(uri, actionClass);
            }
        }
        ActionDefinitionClass definitionClassAnnotation = actionClass.getAnnotation(ActionDefinitionClass.class);
        if (definitionClassAnnotation != null) {
            classesByDefinitionBeanClass.put(definitionClassAnnotation.value(), actionClass);
        }
        LOGGER.trace("Registered action class {}", actionClass);
    }
}
