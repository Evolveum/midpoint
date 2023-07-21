/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class ResourceObjectAssociationConfigItem extends ConfigurationItem<ResourceObjectAssociationType> {

    @SuppressWarnings("unused") // called dynamically
    public ResourceObjectAssociationConfigItem(@NotNull ConfigurationItem<ResourceObjectAssociationType> original) {
        super(original);
    }

    ResourceObjectAssociationConfigItem(
            @NotNull ResourceObjectAssociationType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public @NotNull QName getAssociationName() throws ConfigurationException {
        return configNonNull(
                ItemPathTypeUtil.asSingleNameOrFailNullSafe(value().getRef()),
                "No association name (ref) in %s", DESC);
    }

    public boolean hasInbounds() {
        return !value().getInbound().isEmpty();
    }

    @Override
    public @NotNull String localDescription() {
        return "resource association definition for '" + value().getRef() + "'";
    }

    public @Nullable MappingConfigItem getOutbound() {
        MappingType val = value().getOutbound();
        if (val != null) {
            return new MappingConfigItem(
                    child(val, ResourceObjectAssociationType.F_OUTBOUND));
        } else {
            return null;
        }
    }
}
