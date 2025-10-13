/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;

public class ConstructionConfigItem extends ConfigurationItem<ConstructionType> {

    @SuppressWarnings("unused") // called dynamically
    public ConstructionConfigItem(@NotNull ConfigurationItem<ConstructionType> original) {
        super(original);
    }

    public @NotNull List<ResourceAttributeDefinitionConfigItem> getAttributes() {
        return children(
                value().getAttribute(),
                ResourceAttributeDefinitionConfigItem.class,
                ConstructionType.F_ATTRIBUTE);
    }

    public @NotNull List<ResourceObjectAssociationConfigItem> getAssociations() {
        return children(
                value().getAssociation(),
                ResourceObjectAssociationConfigItem.class,
                ConstructionType.F_ASSOCIATION);
    }

    @Override
    public @NotNull String localDescription() {
        return "construction for " + value().getResourceRef(); // TODO more human readable
    }
}
