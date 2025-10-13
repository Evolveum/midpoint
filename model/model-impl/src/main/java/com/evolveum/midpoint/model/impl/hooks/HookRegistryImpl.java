/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.hooks;

import java.util.*;

import com.evolveum.midpoint.model.api.hooks.ReadHook;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;

/**
 * @author semancik
 */
@Component
public class HookRegistryImpl implements HookRegistry {

    private final Map<String, ChangeHook> changeHookMap = new HashMap<>();
    private final Map<String, ReadHook> readHookMap = new HashMap<>();

    @Override
    public void registerChangeHook(String url, ChangeHook changeHook) {
        changeHookMap.put(url, changeHook);
    }

    @Override
    public List<ChangeHook> getAllChangeHooks() {
        List<ChangeHook> rv = new ArrayList<>(changeHookMap.values());
        rv.sort(Comparator.comparing(ChangeHook::getPriority));
        return rv;
    }

    @Override
    public void registerReadHook(String url, ReadHook searchHook) {
        readHookMap.put(url, searchHook);
    }

    @Override
    public Collection<ReadHook> getAllReadHooks() {
        return readHookMap.values();
    }
}
