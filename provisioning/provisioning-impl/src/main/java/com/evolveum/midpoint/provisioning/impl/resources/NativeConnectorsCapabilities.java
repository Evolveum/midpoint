/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.AbstractFreezable;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps information on native capabilities of all resource connectors (unnamed main one + additional ones).
 */
public class NativeConnectorsCapabilities extends AbstractFreezable {

    @NotNull private final Map<String, CapabilityCollectionType> map;

    private NativeConnectorsCapabilities(@NotNull Map<String, CapabilityCollectionType> map) {
        this.map = map;
    }

    public static NativeConnectorsCapabilities of(@NotNull Map<String, CapabilityCollectionType> capabilityMap) {
        return new NativeConnectorsCapabilities(capabilityMap);
    }

    public static NativeConnectorsCapabilities empty() {
        return new NativeConnectorsCapabilities(new HashMap<>());
    }

    public CapabilityCollectionType get(String connectorName) {
        return map.get(connectorName);
    }

    public void put(String connectorName, CapabilityCollectionType capabilities) {
        checkMutable();
        map.put(connectorName, capabilities);
    }
}
