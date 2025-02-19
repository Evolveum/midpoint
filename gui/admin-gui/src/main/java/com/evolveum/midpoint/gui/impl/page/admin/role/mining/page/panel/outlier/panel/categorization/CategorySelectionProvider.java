/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.categorization;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;

public class CategorySelectionProvider extends ChoiceProvider<RoleAnalysisObjectCategorizationType> {
    @Serial private static final long serialVersionUID = 1L;

    boolean advanced;
    LoadableModel<Boolean> isRoleSelected;
    transient RoleAnalysisOptionType sessionAnalysisOption;

    public CategorySelectionProvider(boolean advanced, LoadableModel<Boolean> isRoleSelected, RoleAnalysisOptionType sessionAnalysisOption) {
        this.advanced = advanced;
        this.isRoleSelected = isRoleSelected;
        this.sessionAnalysisOption = sessionAnalysisOption;
    }

    @Override
    public String getDisplayValue(RoleAnalysisObjectCategorizationType value) {
        return getCategoryValueDisplayString(value, isRoleSelected.getObject());
    }

    @Override
    public String getIdValue(RoleAnalysisObjectCategorizationType value) {
        return value.toString();
    }

    @Override
    public void query(String text, int page, Response<RoleAnalysisObjectCategorizationType> response) {
        List<RoleAnalysisObjectCategorizationType> allowedValues = allowedValues(advanced, isRoleSelected, sessionAnalysisOption);
        if (text == null) {
            response.addAll(allowedValues);
            return;
        }
        for (RoleAnalysisObjectCategorizationType value : allowedValues) {
            if (value.toString().toLowerCase().contains(text)) {
                response.add(value);
            }
        }
    }

    @Override
    public Collection<RoleAnalysisObjectCategorizationType> toChoices(@NotNull Collection<String> values) {
        List<RoleAnalysisObjectCategorizationType> choices = new ArrayList<>();
        values.stream().map(RoleAnalysisObjectCategorizationType::valueOf).collect(Collectors.toCollection(() -> choices));
        return choices;
    }

    public static @NotNull List<RoleAnalysisObjectCategorizationType> allowedValues(
            boolean advanced,
            @NotNull LoadableModel<Boolean> isRoleSelected,
            @NotNull RoleAnalysisOptionType sessionAnalysisOption) {
        RoleAnalysisProcessModeType processMode = sessionAnalysisOption.getProcessMode();
        List<RoleAnalysisObjectCategorizationType> allowedValues = new ArrayList<>(
                BASIC_VALUES.get(isRoleSelected.getObject()).get(processMode));

        if (advanced) {
            allowedValues.addAll(ADVANCED_VALUES.get(isRoleSelected.getObject()).get(processMode));
        }

        filterAllowedValuesByProcedure(allowedValues, sessionAnalysisOption.getAnalysisProcedureType());
        return allowedValues;
    }

    private static void filterAllowedValuesByProcedure(
            @NotNull List<RoleAnalysisObjectCategorizationType> allowedValues,
            RoleAnalysisProcedureType analysisProcedureType) {

        if (analysisProcedureType == RoleAnalysisProcedureType.ROLE_MINING) {
            allowedValues.removeIf(value -> value == RoleAnalysisObjectCategorizationType.OUTLIER
                    || value == RoleAnalysisObjectCategorizationType.ANOMALY
                    || value == RoleAnalysisObjectCategorizationType.OVERALL_ANOMALY);
        }
    }

    public static @NotNull List<CategoryData> allowedCategoryData(
            boolean advanced,
            @NotNull RoleAnalysisIdentifiedCharacteristicsItemsType itemContainer,
            @NotNull LoadableModel<Boolean> isRoleSelected,
            RoleAnalysisOptionType processMode) {

        List<RoleAnalysisObjectCategorizationType> allowedValues = allowedValues(advanced, isRoleSelected, processMode);
        Map<RoleAnalysisObjectCategorizationType, CategoryData> categoryDataMap = resolveClassificationCategoryStr(
                isRoleSelected.getObject(), itemContainer);

        return allowedValues.stream()
                .map(categoryDataMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static String getCategoryValueDisplayString(@NotNull RoleAnalysisObjectCategorizationType value, boolean isRoleSelected) {
        String labelKey = getCategoryModel(value, isRoleSelected, null).labelKey;
        return translate(labelKey, null);
    }

    private static int safeCount(Integer count) {
        return count != null ? count : 0;
    }

    public record CategoryData(String helpKey, int count, ProgressBar.State state, String labelKey, String cssClass) {
    }

    public static CategoryData getCategoryModel(@NotNull RoleAnalysisObjectCategorizationType type, boolean isRoleSelected,
            @Nullable RoleAnalysisIdentifiedCharacteristicsItemsType itemContainer) {
        Map<RoleAnalysisObjectCategorizationType, CategoryData> categoryDataMap = resolveClassificationCategoryStr(
                isRoleSelected, itemContainer);
        return categoryDataMap.get(type);
    }

    public static @NotNull Map<RoleAnalysisObjectCategorizationType, CategoryData> resolveClassificationCategoryStr(
            boolean isRoleSelected,
            @Nullable RoleAnalysisIdentifiedCharacteristicsItemsType itemContainer) {

        Map<RoleAnalysisObjectCategorizationType, CategoryData> categoryDataMap = new HashMap<>();

        addCategory(categoryDataMap, RoleAnalysisObjectCategorizationType.INSUFFICIENT,
                "insufficient.peer.similarity", itemContainer == null ? 0 : safeCount(itemContainer.getInsufficientCount()),
                ProgressBar.State.SECONDARY, isRoleSelected, "text-secondary");

        addCategory(categoryDataMap, RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE_UNPOPULAR,
                "noise_exclusive.and.un_popular", itemContainer == null ? 0 : safeCount(itemContainer.getNoiseExclusiveUnpopular()),
                ProgressBar.State.INFO, isRoleSelected, "text-info");

        addCategory(categoryDataMap, RoleAnalysisObjectCategorizationType.UN_POPULAR,
                "un_popular", itemContainer == null ? 0 : safeCount(itemContainer.getUnPopularCount()),
                ProgressBar.State.PRIMARY, isRoleSelected, "text-primary");

        addCategory(categoryDataMap, RoleAnalysisObjectCategorizationType.ABOVE_POPULAR,
                "above_popular", itemContainer == null ? 0 : safeCount(itemContainer.getAbovePopularCount()),
                ProgressBar.State.SUCCESS, isRoleSelected, "text-success");

        addCategory(categoryDataMap, RoleAnalysisObjectCategorizationType.NOISE,
                "noise", itemContainer == null ? 0 : safeCount(itemContainer.getNoiseCount()),
                ProgressBar.State.PURPLE, isRoleSelected, "text-purple");

        addCategory(categoryDataMap, RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE,
                "noise_exclusive", itemContainer == null ? 0 : safeCount(itemContainer.getNoiseExclusiveCount()),
                ProgressBar.State.OLIVE, isRoleSelected, "text-olive");

        addCategory(categoryDataMap, RoleAnalysisObjectCategorizationType.OUTLIER,
                "outlier", itemContainer == null ? 0 : safeCount(itemContainer.getOutlierCount()),
                ProgressBar.State.DANGER, isRoleSelected, "text-danger");

        addCategory(categoryDataMap, RoleAnalysisObjectCategorizationType.EXCLUDED,
                "excluded", itemContainer == null ? 0 : safeCount(itemContainer.getExcludedCount()),
                ProgressBar.State.DARK, isRoleSelected, "text-dark");

        addCategory(categoryDataMap, RoleAnalysisObjectCategorizationType.EXCLUDED_MISSING_BASE,
                "excluded.missing.base", itemContainer == null ? 0 : safeCount(itemContainer.getExcludedMissingBase()),
                ProgressBar.State.WARNING, isRoleSelected, "text-warning");

        addCategory(categoryDataMap, RoleAnalysisObjectCategorizationType.OVERALL_ANOMALY,
                "overall.anomaly", itemContainer == null ? 0 : safeCount(itemContainer.getOverallAnomalyCount()),
                ProgressBar.State.LIME, isRoleSelected, "text-lime");

        addCategory(categoryDataMap, RoleAnalysisObjectCategorizationType.ANOMALY,
                "anomaly", itemContainer == null ? 0 : safeCount(itemContainer.getAnomalyCount()),
                ProgressBar.State.DANGER, isRoleSelected, "text-danger");

        return categoryDataMap;
    }

    private static void addCategory(@NotNull Map<RoleAnalysisObjectCategorizationType, CategoryData> categoryDataMap,
            RoleAnalysisObjectCategorizationType type, String key, int count,
            ProgressBar.State state, boolean isRoleCategorisation, String textClass) {
        String suffix = isRoleCategorisation ? ".role" : ".user";
        String helpSuffix = ".help" + suffix;

        String helpKey = "RoleAnalysisObjectCategorizationType." + key + helpSuffix;
        String displayKey = "RoleAnalysisObjectCategorizationType." + key + suffix;
        categoryDataMap.put(type, new CategoryData(helpKey, count, state, displayKey, textClass));
    }

    public static boolean skipProvidedObject(
            @NotNull RoleAnalysisIdentifiedCharacteristicsItemType item,
            @NotNull LoadableModel<List<RoleAnalysisObjectCategorizationType>> selectionModel, List<RoleAnalysisObjectCategorizationType> allowedValues) {
        List<RoleAnalysisObjectCategorizationType> category = item.getCategory();
        if (selectionModel.getObject() != null
                && !selectionModel.getObject().isEmpty()
                && (category == null || !new HashSet<>(category).containsAll(selectionModel.getObject()))) {
            return true;
        }

        boolean existSuitableCategory = category.stream().anyMatch(allowedValues::contains);

        return !existSuitableCategory;
    }

    private static final Map<Boolean, Map<RoleAnalysisProcessModeType, List<RoleAnalysisObjectCategorizationType>>> BASIC_VALUES = Map.of(
            true, Map.of(
                    RoleAnalysisProcessModeType.USER, List.of(
                            RoleAnalysisObjectCategorizationType.UN_POPULAR,
                            RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE,
                            RoleAnalysisObjectCategorizationType.EXCLUDED_MISSING_BASE
                    ),
                    RoleAnalysisProcessModeType.ROLE, List.of(
                            RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE_UNPOPULAR,
                            RoleAnalysisObjectCategorizationType.INSUFFICIENT,
                            RoleAnalysisObjectCategorizationType.EXCLUDED_MISSING_BASE
                    )
            ),
            false, Map.of(
                    RoleAnalysisProcessModeType.USER, List.of(
                            RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE_UNPOPULAR,
                            RoleAnalysisObjectCategorizationType.INSUFFICIENT,
                            RoleAnalysisObjectCategorizationType.EXCLUDED_MISSING_BASE
                    ),
                    RoleAnalysisProcessModeType.ROLE, List.of(
                            RoleAnalysisObjectCategorizationType.UN_POPULAR,
                            RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE,
                            RoleAnalysisObjectCategorizationType.EXCLUDED_MISSING_BASE
                    )
            )
    );

    private static final Map<Boolean, Map<RoleAnalysisProcessModeType, List<RoleAnalysisObjectCategorizationType>>> ADVANCED_VALUES = Map.of(
            true, Map.of(
                    RoleAnalysisProcessModeType.USER, List.of(
                            RoleAnalysisObjectCategorizationType.OVERALL_ANOMALY,
                            RoleAnalysisObjectCategorizationType.ABOVE_POPULAR,
                            RoleAnalysisObjectCategorizationType.ANOMALY,
                            RoleAnalysisObjectCategorizationType.EXCLUDED
                    ),
                    RoleAnalysisProcessModeType.ROLE, List.of(
                            RoleAnalysisObjectCategorizationType.OVERALL_ANOMALY,
                            RoleAnalysisObjectCategorizationType.ANOMALY,
                            RoleAnalysisObjectCategorizationType.ABOVE_POPULAR,
                            RoleAnalysisObjectCategorizationType.EXCLUDED
                    )
            ),
            false, Map.of(
                    RoleAnalysisProcessModeType.USER, List.of(
                            RoleAnalysisObjectCategorizationType.ABOVE_POPULAR,
                            RoleAnalysisObjectCategorizationType.OUTLIER,
                            RoleAnalysisObjectCategorizationType.EXCLUDED
                    ),
                    RoleAnalysisProcessModeType.ROLE, List.of(
                            RoleAnalysisObjectCategorizationType.OUTLIER,
                            RoleAnalysisObjectCategorizationType.ABOVE_POPULAR,
                            RoleAnalysisObjectCategorizationType.EXCLUDED
                    )
            )
    );
}
