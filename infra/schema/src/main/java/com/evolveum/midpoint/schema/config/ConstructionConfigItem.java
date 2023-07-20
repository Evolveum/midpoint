/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
        return value().getAttribute().stream()
                .map(val -> new ResourceAttributeDefinitionConfigItem(
                        val,
                        origin().child(ConstructionType.F_ATTRIBUTE.append(val.getId()))))
                .toList();
    }

    public @NotNull List<ResourceObjectAssociationConfigItem> getAssociations() {
        return value().getAssociation().stream()
                .map(val -> new ResourceObjectAssociationConfigItem(
                        val,
                        origin().child(ConstructionType.F_ASSOCIATION.append(val.getId()))))
                .toList();
    }
}
