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
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(LookupTableRowType.F_KEY))),
                entry(
                        AbstractAuthenticationModuleType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(AbstractAuthenticationModuleType.F_IDENTIFIER))),
                entry(
                        AuthenticationSequenceType.class,
                       () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(AuthenticationSequenceType.F_IDENTIFIER))),
                entry(
                        ClassLoggerConfigurationType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(ClassLoggerConfigurationType.F_PACKAGE))),
                entry(
                        AppenderConfigurationType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(AppenderConfigurationType.F_NAME))),
                entry(
                        TracingProfileType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(TracingProfileType.F_NAME))),
                entry(
                        HomePageType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(HomePageType.F_IDENTIFIER))),
                entry(
                        PreviewContainerPanelConfigurationType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(PreviewContainerPanelConfigurationType.F_IDENTIFIER))),
                entry(
                        UserInterfaceFeatureType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(UserInterfaceFeatureType.F_IDENTIFIER))),
                entry(
                        SearchItemType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(
                                        SearchItemType.F_PATH, SearchItemType.F_FILTER, SearchItemType.F_FILTER_EXPRESSION))),
                entry(
                        GuiObjectDetailsPageType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(GuiObjectDetailsPageType.F_TYPE))),
                entry(
                        VirtualContainerItemSpecificationType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(VirtualContainerItemSpecificationType.F_PATH))),
                entry(
                        GuiResourceDetailsPageType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(GuiResourceDetailsPageType.F_CONNECTOR_REF))),
                entry(
                        RoleCollectionViewType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(RoleCollectionViewType.F_IDENTIFIER))),
                entry(
                        RichHyperlinkType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(RichHyperlinkType.F_TARGET_URL))),
                entry(
                        ExpressionProfileType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(ExpressionProfileType.F_IDENTIFIER))),
                entry(
                        ExpressionEvaluatorProfileType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(ExpressionEvaluatorProfileType.F_TYPE))),
                entry(
                        ScriptLanguageExpressionProfileType.class,
                        () -> new GenericItemMerger(
                                marker, DefaultNaturalKeyImpl.of(ScriptLanguageExpressionProfileType.F_LANGUAGE))),
                entry(
                        ExpressionPermissionProfileType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(ExpressionPermissionProfileType.F_IDENTIFIER))),
                entry(
                        ExpressionPermissionPackageProfileType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(ExpressionPermissionPackageProfileType.F_NAME))),
                entry(
                        ExpressionPermissionClassProfileType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(ExpressionPermissionClassProfileType.F_NAME))),
                entry(
                        ExpressionPermissionMethodProfileType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(ExpressionPermissionMethodProfileType.F_NAME))),
                entry(
                        TracingTypeProfileType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(
                                        TracingTypeProfileType.F_LEVEL, TracingTypeProfileType.F_OPERATION_TYPE))),
                entry(
                        ClassLoggerLevelOverrideType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(ClassLoggerLevelOverrideType.F_LOGGER)))
        );
    }
}
