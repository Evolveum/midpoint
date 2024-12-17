/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReferencesCapabilityType;

public class ReferencesCapabilityConfigItem
        extends ConfigurationItem<ReferencesCapabilityType> {

    @SuppressWarnings("unused") // called dynamically
    public ReferencesCapabilityConfigItem(@NotNull ConfigurationItem<ReferencesCapabilityType> original) {
        super(original);
    }

    @SuppressWarnings("WeakerAccess")
    public @NotNull List<SimulatedReferenceTypeConfigItem> getReferenceTypes() {
        return children(
                value().getType(),
                SimulatedReferenceTypeConfigItem.class,
                ReferencesCapabilityType.F_TYPE);
    }

    public boolean isEnabled() {
        return BooleanUtils.isNotFalse(value().isEnabled());
    }

    @Override
    public @NotNull String localDescription() {
        return "simulated associations capability configuration";
    }
}
