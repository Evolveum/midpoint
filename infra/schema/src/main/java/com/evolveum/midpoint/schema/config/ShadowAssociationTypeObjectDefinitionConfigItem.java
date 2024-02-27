/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeObjectDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeSubjectDefinitionType;

public class ShadowAssociationTypeObjectDefinitionConfigItem
        extends ConfigurationItem<ShadowAssociationTypeObjectDefinitionType>
        implements ShadowAssociationTypeParticipantDefinitionConfigItem<ShadowAssociationTypeObjectDefinitionType> {

    public ShadowAssociationTypeObjectDefinitionConfigItem(
            @NotNull ConfigurationItem<ShadowAssociationTypeObjectDefinitionType> original) {
        super(original);
    }

    @Override
    public @NotNull String localDescription() {
        return "object definition";
    }

}
