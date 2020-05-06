/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@Component
public class ScriptingActionExecutorRegistry {

    private final Map<String, ActionExecutor> executorsByTypeName = new ConcurrentHashMap<>();
    private final Map<Class<? extends ActionExpressionType>, ActionExecutor> executorsByBeanClass = new ConcurrentHashMap<>();

    public void register(String name, Class<? extends ActionExpressionType> type, ActionExecutor executor) {
        executorsByTypeName.put(name, executor);
        executorsByBeanClass.put(type, executor);
    }

    public void register(String name, ActionExecutor executor) {
        executorsByTypeName.put(name, executor);
    }

    @NotNull ActionExecutor getExecutor(ActionExpressionType action) {
        if (action.getType() != null) {
            ActionExecutor executor = executorsByTypeName.get(action.getType());
            if (executor != null) {
                return executor;
            } else {
                throw new IllegalStateException("Unknown action executor for action type '" + action.getType() + "'");
            }
        } else {
            ActionExecutor executor = executorsByBeanClass.get(action.getClass());
            if (executor != null) {
                return executor;
            } else {
                throw new IllegalStateException("Unknown action executor for bean class '" + action.getClass().getName() + "'");
            }
        }
    }
}
