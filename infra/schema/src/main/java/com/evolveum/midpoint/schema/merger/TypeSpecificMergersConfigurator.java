/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import static java.util.Map.entry;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.merger.assignment.AssignmentMerger;
import com.evolveum.midpoint.schema.merger.key.DefaultNaturalKeyImpl;
import com.evolveum.midpoint.schema.merger.objdef.LimitationsMerger;
import com.evolveum.midpoint.schema.merger.resource.ObjectTypeDefinitionMerger;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Separate class to hold the configuration of type-specific item mergers.
 */
class TypeSpecificMergersConfigurator {

    record TypedMergerSupplier(Class<?> type, Supplier<ItemMerger> supplier) {
    }

    static Map<String, TypedMergerSupplier> createMergersMap(@Nullable OriginMarker marker) {
        return Map.ofEntries(
                entry(
                        "ResourceObjectTypeDefinitionType",
                        new TypedMergerSupplier(
                                ResourceObjectTypeDefinitionType.class,
                                () -> new ObjectTypeDefinitionMerger(marker))),
                entry(
                        "PropertyLimitationsType",
                        new TypedMergerSupplier(
                                PropertyLimitationsType.class,
                                () -> new LimitationsMerger(marker))),
                entry(
                        "AssignmentType",
                        new TypedMergerSupplier(
                                AssignmentType.class,
                                () -> new AssignmentMerger(marker)))
        );
    }

    @Deprecated
    static Map<Class<?>, Supplier<ItemMerger>> createStandardTypeSpecificMergersMap(@Nullable OriginMarker marker) {
        return Map.ofEntries(
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
                        VirtualContainersSpecificationType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(
                                        VirtualContainersSpecificationType.F_IDENTIFIER,
                                        VirtualContainersSpecificationType.F_PATH))),
                entry(
                        GuiResourceDetailsPageType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(
                                GuiResourceDetailsPageType.F_TYPE,
                                GuiResourceDetailsPageType.F_CONNECTOR_REF))),
                entry(
                        ExpressionEvaluatorProfileType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(ExpressionEvaluatorProfileType.F_TYPE))),
                entry(
                        ScriptLanguageExpressionProfileType.class,
                        () -> new GenericItemMerger(
                                marker, DefaultNaturalKeyImpl.of(ScriptLanguageExpressionProfileType.F_LANGUAGE))),
                entry(
                        TracingTypeProfileType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(
                                        TracingTypeProfileType.F_LEVEL, TracingTypeProfileType.F_OPERATION_TYPE))),
                entry(
                        ClassLoggerLevelOverrideType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(ClassLoggerLevelOverrideType.F_LOGGER))),
                entry(
                        AuthorizationType.class,
                        () -> new GenericItemMerger(
                                marker, DefaultNaturalKeyImpl.of(AuthorizationType.F_NAME, AuthorizationType.F_ACTION))),
                entry(
                        ObjectSelectorType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(ObjectSelectorType.F_NAME, ObjectSelectorType.F_TYPE))),
                entry(
                        CollectionSpecificationType.class,
                        () -> new GenericItemMerger(
                                marker, DefaultNaturalKeyImpl.of(CollectionSpecificationType.F_INTERPRETATION))),
                entry(
                        DashboardWidgetDataFieldType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(DashboardWidgetDataFieldType.F_FIELD_TYPE))),
                entry(
                        DashboardWidgetVariationType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(
                                        DashboardWidgetVariationType.F_DISPLAY, DashboardWidgetVariationType.F_CONDITION))),
                entry(
                        AssignmentRelationType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(
                                        AssignmentRelationType.F_HOLDER_TYPE,
                                        AssignmentRelationType.F_RELATION,
                                        AssignmentRelationType.F_HOLDER_ARCHETYPE_REF))),
                entry(
                        ItemConstraintType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(ItemConstraintType.F_PATH))),
                entry(
                        ModificationPolicyConstraintType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(
                                        ModificationPolicyConstraintType.F_NAME,
                                        ModificationPolicyConstraintType.F_OPERATION))),
                entry(
                        AuthenticationSequenceModuleType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(AuthenticationSequenceModuleType.F_IDENTIFIER))),
                entry(
                        CredentialPolicyType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(CredentialPolicyType.F_NAME))),
                entry(
                        SecurityQuestionDefinitionType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(SecurityQuestionDefinitionType.F_IDENTIFIER))),
                entry(
                        GuiObjectListViewType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(
                                GuiObjectListViewType.F_IDENTIFIER,
                                GuiObjectListViewType.F_TYPE))),
                entry(
                        GuiShadowListViewType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(
                                GuiShadowListViewType.F_IDENTIFIER,
                                GuiShadowListViewType.F_TYPE,
                                GuiShadowListViewType.F_RESOURCE_REF,
                                GuiShadowListViewType.F_KIND,
                                GuiShadowListViewType.F_INTENT))),
                entry(
                        AbstractObjectTypeConfigurationType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(AbstractObjectTypeConfigurationType.F_TYPE))),
                entry(
                        GuiShadowDetailsPageType.class,
                        () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(
                                GuiShadowDetailsPageType.F_TYPE,
                                GuiShadowDetailsPageType.F_RESOURCE_REF,
                                GuiShadowDetailsPageType.F_KIND,
                                GuiShadowDetailsPageType.F_INTENT))),
                entry(
                        SelectorQualifiedGetOptionType.class,
                        () -> new GenericItemMerger(
                                marker,
                                DefaultNaturalKeyImpl.of(
                                        SelectorQualifiedGetOptionType.F_OPTIONS,
                                        SelectorQualifiedGetOptionType.F_SELECTOR)))
        );
    }
}
