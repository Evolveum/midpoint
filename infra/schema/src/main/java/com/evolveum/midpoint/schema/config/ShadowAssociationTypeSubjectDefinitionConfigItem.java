/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeSubjectDefinitionType;

public class ShadowAssociationTypeSubjectDefinitionConfigItem
        extends ConfigurationItem<ShadowAssociationTypeSubjectDefinitionType>
        implements ShadowAssociationTypeParticipantDefinitionConfigItem<ShadowAssociationTypeSubjectDefinitionType> {

    public ShadowAssociationTypeSubjectDefinitionConfigItem(
            @NotNull ConfigurationItem<ShadowAssociationTypeSubjectDefinitionType> original) {
        super(original);
    }

    @Override
    public @NotNull String localDescription() {
        return "subject definition";
    }
}
