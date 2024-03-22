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
                                () -> new AssignmentMerger(marker))),

                // todo entries below this should be removed and should be handled by annotations in xsd,
                //  natural keys should be reviewed and most probably changed

                entry(
                        "SearchItemType",
                        new TypedMergerSupplier(
                                SearchItemType.class,
                                () -> new GenericItemMerger(
                                        marker,
                                        DefaultNaturalKeyImpl.of(
                                                SearchItemType.F_PATH, SearchItemType.F_FILTER, SearchItemType.F_FILTER_EXPRESSION)))),
                entry(
                        "GuiObjectDetailsPageType",
                        new TypedMergerSupplier(
                                GuiObjectDetailsPageType.class,
                                () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(GuiObjectDetailsPageType.F_TYPE)))),
                entry(
                        "GuiResourceDetailsPageType",
                        new TypedMergerSupplier(
                                GuiResourceDetailsPageType.class,
                                () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(
                                        GuiResourceDetailsPageType.F_TYPE,
                                        GuiResourceDetailsPageType.F_CONNECTOR_REF)))),
                entry(
                        "ExpressionEvaluatorProfileType",
                        new TypedMergerSupplier(
                                ExpressionEvaluatorProfileType.class,
                                () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(ExpressionEvaluatorProfileType.F_TYPE)))),
                entry(
                        "ScriptLanguageExpressionProfileType",
                        new TypedMergerSupplier(
                                ScriptLanguageExpressionProfileType.class,
                                () -> new GenericItemMerger(
                                        marker, DefaultNaturalKeyImpl.of(ScriptLanguageExpressionProfileType.F_LANGUAGE)))),
                entry(
                        "ClassLoggerLevelOverrideType",
                        new TypedMergerSupplier(
                                ClassLoggerLevelOverrideType.class,
                                () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(ClassLoggerLevelOverrideType.F_LOGGER)))),
                entry(
                        "ObjectSelectorType",
                        new TypedMergerSupplier(
                                ObjectSelectorType.class,
                                () -> new GenericItemMerger(
                                        marker,
                                        DefaultNaturalKeyImpl.of(ObjectSelectorType.F_NAME, ObjectSelectorType.F_TYPE)))),
                entry(
                        "CollectionSpecificationType",
                        new TypedMergerSupplier(
                                CollectionSpecificationType.class,
                                () -> new GenericItemMerger(
                                        marker, DefaultNaturalKeyImpl.of(CollectionSpecificationType.F_INTERPRETATION)))),
                entry(
                        "DashboardWidgetDataFieldType",
                        new TypedMergerSupplier(
                                DashboardWidgetDataFieldType.class,
                                () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(DashboardWidgetDataFieldType.F_FIELD_TYPE)))),
                entry(
                        "DashboardWidgetVariationType",
                        new TypedMergerSupplier(
                                DashboardWidgetVariationType.class,
                                () -> new GenericItemMerger(
                                        marker,
                                        DefaultNaturalKeyImpl.of(
                                                DashboardWidgetVariationType.F_DISPLAY, DashboardWidgetVariationType.F_CONDITION)))),
                entry(
                        "AssignmentRelationType",
                        new TypedMergerSupplier(
                                AssignmentRelationType.class,
                                () -> new GenericItemMerger(
                                        marker,
                                        DefaultNaturalKeyImpl.of(
                                                AssignmentRelationType.F_HOLDER_TYPE,
                                                AssignmentRelationType.F_RELATION,
                                                AssignmentRelationType.F_HOLDER_ARCHETYPE_REF)))),
                entry(
                        "ItemConstraintType",
                        new TypedMergerSupplier(
                                ItemConstraintType.class,
                                () -> new GenericItemMerger(marker, DefaultNaturalKeyImpl.of(ItemConstraintType.F_PATH)))),
                entry(
                        "ModificationPolicyConstraintType",
                        new TypedMergerSupplier(
                                ModificationPolicyConstraintType.class,
                                () -> new GenericItemMerger(
                                        marker,
                                        DefaultNaturalKeyImpl.of(
                                                ModificationPolicyConstraintType.F_NAME,
                                                ModificationPolicyConstraintType.F_OPERATION)))),
                entry(
                        "AbstractObjectTypeConfigurationType",
                        new TypedMergerSupplier(
                                AbstractObjectTypeConfigurationType.class,
                                () -> new GenericItemMerger(
                                        marker,
                                        DefaultNaturalKeyImpl.of(AbstractObjectTypeConfigurationType.F_TYPE)))),
                entry(
                        "GuiShadowDetailsPageType",
                        new TypedMergerSupplier(
                                GuiShadowDetailsPageType.class,
                                () -> new GenericItemMerger(
                                        marker, DefaultNaturalKeyImpl.of(
                                        GuiShadowDetailsPageType.F_TYPE,
                                        GuiShadowDetailsPageType.F_RESOURCE_REF,
                                        GuiShadowDetailsPageType.F_KIND,
                                        GuiShadowDetailsPageType.F_INTENT)))),
                entry(
                        "SelectorQualifiedGetOptionType",
                        new TypedMergerSupplier(
                                SelectorQualifiedGetOptionType.class,
                                () -> new GenericItemMerger(
                                        marker,
                                        DefaultNaturalKeyImpl.of(
                                                SelectorQualifiedGetOptionType.F_OPTIONS,
                                                SelectorQualifiedGetOptionType.F_SELECTOR))))
        );
    }
}
