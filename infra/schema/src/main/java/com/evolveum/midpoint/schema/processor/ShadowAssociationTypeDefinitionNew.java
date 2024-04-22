/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;

import com.evolveum.midpoint.schema.config.ShadowAssociationTypeNewDefinitionConfigItem;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;

import org.jetbrains.annotations.Nullable;

/**
 * Definition of a blob of associations and associated objects, representing a high-level "association type".
 *
 * TODO EXPERIMENTAL; to be improved
 */
public class ShadowAssociationTypeDefinitionNew
        implements DebugDumpable, Serializable {

    @Nullable private final ShadowAssociationTypeNewDefinitionConfigItem configItem;

    @NotNull private final ResourceObjectInboundDefinition inboundDefinition;

    private ShadowAssociationTypeDefinitionNew(
            @Nullable ShadowAssociationTypeNewDefinitionConfigItem configItem,
            @NotNull ResourceObjectInboundDefinition inboundDefinition) {
        this.configItem = configItem;
        this.inboundDefinition = inboundDefinition;
    }

    public static ShadowAssociationTypeDefinitionNew create(
            @Nullable ShadowAssociationTypeNewDefinitionConfigItem definitionConfigItem,
            @NotNull ResourceObjectInboundDefinition inboundDefinition) {
        return new ShadowAssociationTypeDefinitionNew(definitionConfigItem, inboundDefinition);
    }

    public static ShadowAssociationTypeDefinitionNew empty() {
        return new ShadowAssociationTypeDefinitionNew(
                null, ResourceObjectInboundDefinition.empty());
    }

    @Override
    public String debugDump(int indent) {
        return "TODO";
    }

    public boolean needsInboundProcessing() {
        return inboundDefinition.hasAnyInbounds();
    }

    public @NotNull ResourceObjectInboundDefinition getInboundDefinition() {
        return inboundDefinition;
    }

    //    boolean hasInboundMappings() {
//        return getShadowItemDefinitions().stream()
//                .anyMatch(ShadowItemDefinition::hasInboundMapping);
//    }
}
