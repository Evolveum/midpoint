/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.icf.dummy.resource;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

public class HookRegistry {

    private final Set<ConnectorOperationHook> hooks = ConcurrentHashMap.newKeySet();

    public void reset() {
        hooks.clear();
    }

    void registerHook(@NotNull ConnectorOperationHook hook) {
        hooks.add(hook);
    }

    void invokeHooks(@NotNull Consumer<ConnectorOperationHook> invoker) {
        hooks.forEach(invoker);
    }
}
