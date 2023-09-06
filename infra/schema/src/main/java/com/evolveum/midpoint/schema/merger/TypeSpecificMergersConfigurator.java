/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import com.evolveum.midpoint.schema.merger.key.DefaultNaturalKeyImpl;
import com.evolveum.midpoint.schema.merger.key.ItemPathNaturalKeyImpl;
import com.evolveum.midpoint.schema.merger.objdef.LimitationsMerger;
import com.evolveum.midpoint.schema.merger.resource.ObjectTypeDefinitionMerger;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

import static java.util.Map.entry;

/**
 * Separate class to hold the configuration of type-specific item mergers.
 */
class TypeSpecificMergersConfigurator {

    static Map<Class<?>, Supplier<ItemMerger>> createStandardTypeSpecificMergersMap(@Nullable OriginMarker marker) {
        return Map.ofEntries(
                // for ResourceType
                entry(
                        ConnectorInstanceSpecificationType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(ConnectorInstanceSpecificationType.F_NAME))),
                entry(
                        ResourceObjectTypeDefinitionType.class,
                        () -> new ObjectTypeDefinitionMerger(marker)),

                // for ObjectTemplateType
                entry(
                        ObjectTemplateItemDefinitionType.class,
                        () -> new GenericItemMerger(
                                marker, ItemPathNaturalKeyImpl.of(ItemRefinedDefinitionType.F_REF))),

                // for ResourceObjectTypeDefinitionType (object type definitions and embedded structures)
                entry(
                        ResourceItemDefinitionType.class,
                        () -> new GenericItemMerger(
                                marker, ItemPathNaturalKeyImpl.of(ResourceAttributeDefinitionType.F_REF))),
                entry(
                        PropertyLimitationsType.class,
                        () -> new LimitationsMerger(marker)),
                entry(
                        MappingType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(MappingType.F_NAME))),
                entry(
                        AbstractCorrelatorType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(AbstractCorrelatorType.F_NAME))),
                entry(
                        SynchronizationReactionType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(SynchronizationReactionType.F_NAME))),
                entry(
                        AbstractSynchronizationActionType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(AbstractSynchronizationActionType.F_NAME))),
                entry(
                        LookupTableRowType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(LookupTableRowType.F_KEY)))
        );
    }
}
