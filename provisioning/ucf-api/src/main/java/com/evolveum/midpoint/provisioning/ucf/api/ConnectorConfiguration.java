/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Describes the configuration of a connector instance.
 *
 * @param configuration Configuration of the connector instance.
 * Corresponds to the {@link ResourceType#F_CONNECTOR_CONFIGURATION} item.
 *
 * @param objectClassesToGenerate List of object classes that should be generated in the schema. Empty list means "all".
 */
public record ConnectorConfiguration(
        @Nullable PrismContainerValue<?> configuration,
        @NotNull Collection<QName> objectClassesToGenerate) implements Serializable, DebugDumpable, Cloneable {

    @Override
    public String debugDump(int indent) {
        return DebugUtil.standardDebugDump(
                this, indent,
                "configuration", configuration,
                "objectClassesToGenerate", objectClassesToGenerate);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ConnectorConfiguration clone() {
        return new ConnectorConfiguration(
                configuration != null ? configuration.clone() : null,
                List.copyOf(objectClassesToGenerate));
    }

    public boolean equivalent(@NotNull ConnectorConfiguration other) {
        if (!MiscUtil.unorderedCollectionEquals(objectClassesToGenerate, other.objectClassesToGenerate)) {
            return false;
        }
        if (configuration == null) {
            return other.configuration == null;
        } else {
            if (configuration.isEmpty() && other.configuration == null) {
                return true;
            }
            return other.configuration != null && configuration.equivalent(other.configuration);
        }
    }
}
