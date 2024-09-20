/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.reactions;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.sync.action.BaseAction;
import com.evolveum.midpoint.model.impl.sync.action.SynchronizationAction;
import com.evolveum.midpoint.schema.processor.SynchronizationActionDefinition;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractSynchronizationActionType;

/**
 * Instantiates {@link SynchronizationAction} objects.
 */
@Component
public class SynchronizationActionFactory {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationActionFactory.class);

    @NotNull private final Map<String, ActionProvider> providersByActionUri = new HashMap<>();
    @NotNull private final Map<Class<? extends AbstractSynchronizationActionType>, ActionProvider>
            providersByDefinitionBeanClass = new HashMap<>();

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
        return MiscUtil.requireNonNull(
                        providersByDefinitionBeanClass.get(beanClass),
                        () -> new ConfigurationException("No synchronization action class found for definition " + beanClass))
                .instantiate(context);
    }

    private SynchronizationAction getByLegacyUri(String actionUri, ActionInstantiationContext<?> context)
            throws ConfigurationException {
        return MiscUtil.requireNonNull(
                        providersByActionUri.get(actionUri),
                        () -> new ConfigurationException("No synchronization action class found for URI " + actionUri))
                .instantiate(context);
    }

    public <T extends BaseAction<?>> void register(Class<T> actionClass, ActionProvider provider) {
        ActionUris urisAnnotation = actionClass.getAnnotation(ActionUris.class);
        if (urisAnnotation != null) {
            for (String uri : urisAnnotation.value()) {
                providersByActionUri.put(uri, provider);
            }
        }
        ActionDefinitionClass definitionClassAnnotation = actionClass.getAnnotation(ActionDefinitionClass.class);
        if (definitionClassAnnotation != null) {
            providersByDefinitionBeanClass.put(definitionClassAnnotation.value(), provider);
        }
        LOGGER.trace("Registered action class {}", actionClass);
    }

    public interface ActionProvider {
        @NotNull BaseAction<?> instantiate(ActionInstantiationContext<?> ctx);
    }
}
