/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

public class ResourceAttributeDefinitionConfigItem extends ConfigurationItem<ResourceAttributeDefinitionType> {

    @SuppressWarnings("unused") // called dynamically
    public ResourceAttributeDefinitionConfigItem(@NotNull ConfigurationItem<ResourceAttributeDefinitionType> original) {
        super(original);
    }

    public ResourceAttributeDefinitionConfigItem(
            @NotNull ResourceAttributeDefinitionType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public @NotNull QName getAttributeName() throws ConfigurationException {
        return configNonNull(
                ItemPathTypeUtil.asSingleNameOrFailNullSafe(value().getRef()),
                "No attribute name (ref) in %s");
    }

    public boolean hasInbounds() {
        return !value().getInbound().isEmpty();
    }

    @Override
    public @NotNull String localDescription() {
        return "resource attribute definition for '" + value().getRef() + "'";
    }

    public @Nullable MappingConfigItem getOutbound() {
        MappingType val = value().getOutbound();
        if (val != null) {
            return MappingConfigItem.of(val, origin().child(ResourceAttributeDefinitionType.F_OUTBOUND));
        } else {
            return null;
        }
    }
}
