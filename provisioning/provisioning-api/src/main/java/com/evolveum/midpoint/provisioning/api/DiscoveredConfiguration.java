/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.PrismProperty;

import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * Suggested configuration properties of connector.
 * Contains collection of PrismProperties with suggested configuration properties.
 * Collection is empty if connector can't determine any suggestions.
 */
public class DiscoveredConfiguration implements Serializable, DebugDumpable {

    @NotNull private final Collection<PrismProperty<?>> discoveredProperties;

    private DiscoveredConfiguration(@NotNull Collection<PrismProperty<?>> discoveredProperties) {
        this.discoveredProperties = discoveredProperties;
    }

    public static DiscoveredConfiguration of(@NotNull Collection<PrismProperty<?>> discoveredProperties) {
        return new DiscoveredConfiguration(discoveredProperties);
    }

    public static DiscoveredConfiguration empty() {
        return new DiscoveredConfiguration(Set.of());
    }

    public @NotNull Collection<PrismProperty<?>> getDiscoveredProperties() {
        return discoveredProperties;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "properties", discoveredProperties, indent + 1);
        return sb.toString();
    }
}
