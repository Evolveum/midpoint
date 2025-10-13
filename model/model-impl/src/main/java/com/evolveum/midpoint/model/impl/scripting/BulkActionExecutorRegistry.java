/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.model.api.BulkAction;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.AbstractActionExpressionType;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BulkActionExecutorRegistry {

    private final Map<String, ActionExecutor> executorsByTypeName = new ConcurrentHashMap<>();
    private final Map<Class<? extends AbstractActionExpressionType>, ActionExecutor> executorsByBeanClass = new ConcurrentHashMap<>();

    public void register(ActionExecutor executor) {
        BulkAction actionType = executor.getActionType();
        executorsByTypeName.put(actionType.getName(), executor);
        var beanClass = actionType.getBeanClass();
        if (beanClass != null) {
            executorsByBeanClass.put(beanClass, executor);
        }
    }

    @NotNull ActionExecutor getExecutor(AbstractActionExpressionType action) {
        String type = action instanceof ActionExpressionType dynamic ? dynamic.getType() : null;
        if (type != null) {
            ActionExecutor executor = executorsByTypeName.get(type);
            if (executor != null) {
                return executor;
            } else {
                throw new IllegalStateException("Unknown action executor for action type '" + type + "'");
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
