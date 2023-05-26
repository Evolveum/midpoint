/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.impl.NinjaContext;

public class UpgradeStepsContext {

    private final NinjaContext context;

    private final UpgradeOptions options;

    private final Map<Class<?>, Object> result = new HashMap<>();

    public UpgradeStepsContext(NinjaContext context, UpgradeOptions options) {
        this.context = context;
        this.options = options;
    }

    public void addResult(@NotNull Class<?> step, Object result) {
        this.result.put(step, result);
    }

    public <T> T getResult(@NotNull Class<?> step, @NotNull Class<T> type) {
        Object o = result.get(type);

        if (o != null && !type.isAssignableFrom(o.getClass())) {
            throw new IllegalStateException("Result for step " + step.getName() + " is not type of " + type.getName());
        }

        return (T) o;
    }

    public <T> T getResult(@NotNull Class<T> type) {
        return (T) result.values().stream()
                .filter(r -> r != null && type.isAssignableFrom(r.getClass())).findFirst().orElse(null);
    }

    public NinjaContext getContext() {
        return context;
    }
}
