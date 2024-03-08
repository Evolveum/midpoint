/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @SuppressWarnings("WeakerAccess")
    public @Nullable MappingConfigItem getOutboundMapping() {
        return child(
                value().getOutbound(),
                MappingConfigItem.class,
                ShadowAssociationTypeSubjectDefinitionType.F_OUTBOUND);
    }

    @SuppressWarnings("WeakerAccess")
    public @NotNull List<InboundMappingConfigItem> getInboundMappings() {
        return children(
                value().getInbound(),
                InboundMappingConfigItem.class,
                ShadowAssociationTypeSubjectDefinitionType.F_INBOUND);
    }
}
