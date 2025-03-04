/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.Status.*;

import java.util.*;

import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectStatus;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * Utility class for resolving cell colors and status in the context of role analysis tables.
 * <p>
 * This class provides utility methods for resolving cell colors, updating mining status, and initializing detection patterns
 * for user-based and role-based role analysis tables.
 */
public class RoleAnalysisTableCellFillResolver {

    /**
     * Update the mining DISABLE status for role-based analysis.
     *
     * @param rowModel The model of the row to update.
     * @param minFrequency The minimum frequency threshold.
     * @param maxFrequency The maximum frequency threshold.
     */
    public static <T extends MiningBaseTypeChunk> void updateFrequencyBased(
            @NotNull IModel<T> rowModel,
            double minFrequency,
            double maxFrequency,
            boolean isOutlier) {

        T rowObject = rowModel.getObject();
        FrequencyItem frequencyItem = rowObject.getFrequencyItem();

        if (isOutlier) {
            applyOutlierFqStatus(rowObject);
            return;
        }

        //TODO i think there is a bug (100 fq) check it
        double frequency = frequencyItem.getFrequency();
        boolean isInclude = rowObject.getStatus().isInclude();

        if (!isInclude && (minFrequency > frequency || maxFrequency < frequency)) {
            rowModel.getObject().setStatus(RoleAnalysisOperationMode.DISABLE);
        }
    }

    private static <T extends MiningBaseTypeChunk> void applyOutlierFqStatus(@NotNull T rowModel) {
        FrequencyItem frequencyItem = rowModel.getFrequencyItem();
        FrequencyItem.Status status = frequencyItem.getStatus();

        if (status == null) {
            return;
        }

        if (status == FrequencyItem.Status.NEGATIVE_EXCLUDE) {
            rowModel.setStatus(RoleAnalysisOperationMode.NEGATIVE_EXCLUDE);
        } else if (status == FrequencyItem.Status.POSITIVE_EXCLUDE) {
            rowModel.setStatus(RoleAnalysisOperationMode.POSITIVE_EXCLUDE);
        }
    }

    public enum Status {
        RELATION_INCLUDE,
        RELATION_EXCLUDE,
        RELATION_DISABLE,
        RELATION_NONE;
    }

    /**
     * Resolve the cell color for role analysis table.
     *
     * @param rowModel The row model (properties to compare).
     * @param colModel The column model (members to compare).
     */
    public static <B extends MiningBaseTypeChunk, A extends MiningBaseTypeChunk> Status resolveCellTypeUserTable(
            @NotNull String componentId,
            @NotNull Item<ICellPopulator<A>> cellItem,
            @NotNull A rowModel,
            @NotNull B colModel,
            boolean isOutlierMode,
            @NotNull IModel<Map<String, String>> colorLoadableMap) {
        Map<String, String> colorMap = colorLoadableMap.getObject();
        RoleAnalysisObjectStatus rowObjectStatus = rowModel.getObjectStatus();
        RoleAnalysisObjectStatus colObjectStatus = colModel.getObjectStatus();

        Set<String> duplicatedElements = getDuplicatedElements(rowObjectStatus, colObjectStatus);

        boolean firstStage = isFirstStage(rowModel, colModel);
        boolean secondStage = isSecondStage(rowObjectStatus, colObjectStatus, duplicatedElements);
        boolean isCandidate = firstStage && secondStage;

        if (!firstStage && !secondStage) {
            emptyCell(componentId, cellItem);
            return RELATION_NONE;
        }

        RoleAnalysisOperationMode rowStatus = rowObjectStatus.getRoleAnalysisOperationMode();
        RoleAnalysisOperationMode colStatus = colObjectStatus.getRoleAnalysisOperationMode();

        if (rowStatus.isNegativeExclude() || colStatus.isNegativeExclude()) {
            return handleExcludeStatus(componentId, cellItem, isOutlierMode, isCandidate, true);
        }

        if (rowStatus.isPositiveExclude() || colStatus.isPositiveExclude()) {
            return handleExcludeStatus(componentId, cellItem, isOutlierMode, isCandidate, false);
        }

        if (rowStatus.isDisable() || colStatus.isDisable()) {
            return handleDisableStatus(componentId, cellItem, isOutlierMode, firstStage);
        }

        if (rowStatus.isInclude() && colStatus.isInclude()) {
            if (isCandidate) {
                return handleIncludeCandidateStatus(componentId, cellItem, duplicatedElements, colorMap);
            } else if (secondStage) {
                return handleIncludeDuplicateStatus(componentId, cellItem, duplicatedElements, colorMap);
            }
        }

        if (firstStage) {
            relationCell(componentId, cellItem);
            return RELATION_INCLUDE;
        } else {
            emptyCell(componentId, cellItem);
            return RELATION_NONE;
        }
    }

    private static @NotNull Set<String> getDuplicatedElements(
            @NotNull RoleAnalysisObjectStatus rowStatus,
            @NotNull RoleAnalysisObjectStatus colStatus) {
        Set<String> rowContainerId = rowStatus.getContainerId();
        Set<String> colContainerId = colStatus.getContainerId();
        if (rowContainerId.isEmpty() || colContainerId.isEmpty()) {
            return new HashSet<>();
        }
        Set<String> duplicatedElements = new HashSet<>(rowContainerId);
        duplicatedElements.retainAll(colContainerId);
        return duplicatedElements;
    }

    private static boolean isSecondStage(
            @NotNull RoleAnalysisObjectStatus rowStatus,
            @NotNull RoleAnalysisObjectStatus colStatus, Set<String> duplicatedElements) {
        Set<String> rowContainerId = rowStatus.getContainerId();
        Set<String> colContainerId = colStatus.getContainerId();

        if (rowContainerId.isEmpty() && colContainerId.isEmpty()) {
            return true;
        }

        return !duplicatedElements.isEmpty();
    }

    private static <B extends MiningBaseTypeChunk, A extends MiningBaseTypeChunk> boolean isFirstStage(
            @NotNull A rowModel,
            @NotNull B colModel) {
        List<String> properties = rowModel.getProperties();
        List<String> members = colModel.getMembers();
        for (String member : members) {
            if (!properties.contains(member)) {
                return false;
            }
        }
        return true;
    }

    private static <A extends MiningBaseTypeChunk> @NotNull Status handleIncludeDuplicateStatus(
            @NotNull String componentId,
            Item<ICellPopulator<A>> cellItem,
            @NotNull Set<String> duplicatedElements,
            Map<String, String> colorMap) {
        int duplicatedElementsCount = duplicatedElements.size();

        if (duplicatedElementsCount > 1) {
            additionalDuplicateCell(componentId, cellItem, duplicatedElements);
            return RELATION_EXCLUDE;
        } else if (duplicatedElementsCount == 1) {
            ArrayList<String> element = new ArrayList<>(duplicatedElements);
            additionalCell(componentId, cellItem, colorMap.get(element.get(0)), duplicatedElements);
            return RELATION_INCLUDE;
        }
        additionalCell(componentId, cellItem, "#28A745", duplicatedElements);
        return RELATION_INCLUDE;
    }

    private static <A extends MiningBaseTypeChunk> @NotNull Status handleIncludeCandidateStatus(
            @NotNull String componentId,
            Item<ICellPopulator<A>> cellItem,
            @NotNull Set<String> duplicatedElements,
            Map<String, String> colorMap) {
        int duplicatedElementsCount = duplicatedElements.size();

        if (duplicatedElementsCount > 1) {
            reducedDuplicateCell(componentId, cellItem, duplicatedElements);
            return RELATION_EXCLUDE;
        } else if (duplicatedElementsCount == 1) {
            ArrayList<String> element = new ArrayList<>(duplicatedElements);
            reducedCell(componentId, cellItem, colorMap.get(element.get(0)), duplicatedElements);
            return RELATION_INCLUDE;
        }
        reducedCell(componentId, cellItem, "#28A745", duplicatedElements);
        return RELATION_INCLUDE;
    }

    private static <A extends MiningBaseTypeChunk> @NotNull Status handleDisableStatus(
            @NotNull String componentId,
            Item<ICellPopulator<A>> cellItem,
            boolean isOutlierMode,
            boolean firstStage) {
        if (firstStage) {
            if (isOutlierMode) {
                relationCell(componentId, cellItem);
            } else {
                disabledCell(componentId, cellItem);
            }
            return RELATION_DISABLE;
        }
        emptyCell(componentId, cellItem);
        return RELATION_NONE;
    }

    private static <A extends MiningBaseTypeChunk> @NotNull Status handleExcludeStatus(
            @NotNull String componentId, Item<ICellPopulator<A>> cellItem,
            boolean isOutlierMode,
            boolean isCandidate,
            boolean isNegative) {
        if (isCandidate) {

            if (isOutlierMode) {
                relationCell(componentId, cellItem);
            } else if (isNegative) {
                negativeDisabledCell(componentId, cellItem);
            } else {
                positiveDisabledCell(componentId, cellItem);
            }

            return RELATION_DISABLE;
        }
        emptyCell(componentId, cellItem);
        return RELATION_NONE;
    }

    public static void refreshCells(
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull List<MiningUserTypeChunk> users,
            @NotNull List<MiningRoleTypeChunk> roles,
            double minFrequency,
            double maxFrequency) {

        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            resetCellStatusByCondition(roles, minFrequency, maxFrequency);
            excludeAll(users);
        } else {
            resetCellStatusByCondition(users, minFrequency, maxFrequency);
            excludeAll(roles);
        }
    }

    private static <M extends MiningBaseTypeChunk> void resetCellStatusByCondition(
            @NotNull List<M> chunk, double minFrequency, double maxFrequency) {
        chunk.forEach(role -> {
            FrequencyItem frequencyItem = role.getFrequencyItem();
            double frequency = frequencyItem.getFrequency();
            RoleAnalysisOperationMode status = role.getStatus();

            role.setStatus(determineStatus(minFrequency, maxFrequency, frequency, status.isInclude()));
        });
    }

    private static <T extends MiningBaseTypeChunk> void excludeAll(@NotNull List<T> elements) {
        elements.forEach(element -> element.setStatus(RoleAnalysisOperationMode.EXCLUDE));
    }

    private static RoleAnalysisOperationMode determineStatus(double minFrequency, double maxFrequency, double frequency,
            boolean isIncluded) {
        return (minFrequency > frequency && maxFrequency > frequency && !isIncluded) ?
                RoleAnalysisOperationMode.DISABLE :
                RoleAnalysisOperationMode.EXCLUDE;
    }

    protected static <T> void emptyCell(@NotNull String componentId, @NotNull Item<ICellPopulator<T>> cellItem) {
        cellItem.add(new EmptyPanel(componentId));
    }

    protected static <T> void disabledCell(@NotNull String componentId, @NotNull Item<ICellPopulator<T>> cellItem) {
        cellItem.add(AttributeModifier.append("class", "bg-danger"));
        cellItem.add(new EmptyPanel(componentId));
    }

    protected static <T> void negativeDisabledCell(@NotNull String componentId, @NotNull Item<ICellPopulator<T>> cellItem) {
        cellItem.add(AttributeModifier.append("class", "bg-danger"));
        cellItem.add(new EmptyPanel(componentId));
    }

    protected static <T> void positiveDisabledCell(@NotNull String componentId, @NotNull Item<ICellPopulator<T>> cellItem) {
        cellItem.add(AttributeModifier.append("class", "bg-info"));
        cellItem.add(new EmptyPanel(componentId));
    }

    protected static <T> void relationCell(@NotNull String componentId, @NotNull Item<ICellPopulator<T>> cellItem) {
        cellItem.add(AttributeModifier.append("class", "bg-dark"));
        cellItem.add(new EmptyPanel(componentId));
    }

    protected static <T> void reducedDuplicateCell(@NotNull String componentId, @NotNull Item<ICellPopulator<T>> cellItem,
            Set<String> duplicatedElements) {
        cellItem.add(AttributeModifier.append("class", "corner-hashed-bg"));

        EmptyPanel components = buildReducedCellComponent(componentId, duplicatedElements);
        cellItem.add(components);
    }

    protected static <T> void reducedCell(@NotNull String componentId, @NotNull Item<ICellPopulator<T>> cellItem, String color,
            Set<String> duplicatedElements) {
        cellItem.add(AttributeModifier.append("style", "background-color: " + color + ";"));

        EmptyPanel components = buildReducedCellComponent(componentId, duplicatedElements);
        cellItem.add(components);
    }

    protected static <T> void additionalDuplicateCell(@NotNull String componentId, @NotNull Item<ICellPopulator<T>> cellItem,
            @NotNull Set<String> duplicatedElements) {
        String cssIconClass = getCssIconClass();
        String cssIconColorClass = getCssIconColorClass();

        cellItem.add(AttributeModifier.append("class", "corner-hashed-bg"));

        ImagePanel components = buildAdditionalCellComponent(componentId, duplicatedElements, cssIconClass, cssIconColorClass);
        cellItem.add(components);
    }

    protected static <T> void additionalCell(@NotNull String componentId, @NotNull Item<ICellPopulator<T>> cellItem, String color,
            @NotNull Set<String> duplicatedElements) {
        String cssIconClass = getCssIconClass();
        String cssIconColorClass = getCssIconColorClass();

        cellItem.add(AttributeModifier.append("style", "background-color: " + color + ";"));

        ImagePanel components = buildAdditionalCellComponent(componentId, duplicatedElements, cssIconClass, cssIconColorClass);

        cellItem.add(components);
    }

    private static @NotNull ImagePanel buildAdditionalCellComponent(
            @NotNull String componentId,
            @NotNull Set<String> duplicatedElements,
            String cssIconClass,
            String cssIconColorClass) {
        String joinedIds = String.join("\n ", duplicatedElements);
        DisplayType warning = GuiDisplayTypeUtil.createDisplayType(cssIconClass, cssIconColorClass, joinedIds);

        ImagePanel components = new ImagePanel(componentId, Model.of(warning));
        components.add(new InfoTooltipBehavior() {
            @Override
            public String getCssClass() {
                return " ";
            }
        });

        components.add(AttributeModifier.replace("title", joinedIds));
        return components;
    }

    private static @NotNull EmptyPanel buildReducedCellComponent(@NotNull String componentId, Set<String> duplicatedElements) {
        String joinedIds = String.join("\n ", duplicatedElements);
        EmptyPanel components = new EmptyPanel(componentId);
        components.add(AttributeModifier.append("style", "width: 100%;height: 100%;"));
        components.add(new InfoTooltipBehavior() {
            @Override
            public String getCssClass() {
                return " ";
            }
        });
        components.add(AttributeModifier.replace("title", joinedIds));
        return components;
    }

    protected static @NotNull String getCssIconClass() {
        // return " fa fa-warning"
        // return " fas fa-plus-circle";
        return " fa fa-plus fa-lg";
    }

    protected static @NotNull String getCssIconColorClass() {
        // return " fa fa-warning"
        return " black";
    }

}
