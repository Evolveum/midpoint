/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationDefinitionType;

/**
 * TEMPORARY
 */
public class ShadowAssociationDefinitionConfigItem
        extends ConfigurationItem<ShadowAssociationDefinitionType>
        implements AssociationConfigItem {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public ShadowAssociationDefinitionConfigItem(@NotNull ConfigurationItem<ShadowAssociationDefinitionType> original) {
        super(original);
    }

//    public @NotNull ItemName getItemName() throws ConfigurationException {
//        return singleNameRequired(value().getRef(), "association name (ref)");
//    }
//
//    public boolean hasInbounds() {
//        return !value().getInbound().isEmpty();
//    }
//
//    public @Nullable MappingConfigItem getOutboundMapping() {
//        return child(
//                value().getOutbound(),
//                MappingConfigItem.class,
//                ResourceObjectAssociationType.F_OUTBOUND);
//    }
//
//    public @NotNull MappingConfigItem getOutboundMappingRequired() throws ConfigurationException {
//        return nonNull(getOutboundMapping(), "outbound mapping");
//    }
//
//    public @NotNull List<InboundMappingConfigItem> getInboundMappings() {
//        return children(
//                value().getInbound(),
//                InboundMappingConfigItem.class,
//                ResourceObjectAssociationType.F_INBOUND);
//    }
//
//    public boolean isExclusiveStrong() {
//        return Boolean.TRUE.equals(value().isExclusiveStrong());
//    }
//
//    public boolean isTolerant() {
//        return BooleanUtils.isNotFalse(value().isTolerant());
//    }
//
//    public @NotNull List<String> getTolerantValuePatterns() {
//        return value().getTolerantValuePattern();
//    }
//
//    public @NotNull List<String> getIntolerantValuePatterns() {
//        return value().getIntolerantValuePattern();
//    }

    @Override
    public String debugDump(int indent) {
        return value().debugDump(indent);
    }

    @Override
    public @NotNull String localDescription() {
        return "resource association definition for '" + value().getRef() + "'";
    }

//    public QName getMatchingRule() {
//        return value().getMatchingRule();
//    }
}
