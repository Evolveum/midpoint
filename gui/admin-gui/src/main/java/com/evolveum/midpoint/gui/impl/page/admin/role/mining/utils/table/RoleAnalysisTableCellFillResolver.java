/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.Status.*;

import java.util.*;
import java.util.stream.IntStream;

import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ListMultimap;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectStatus;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
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

        if (isOutlier) {
            MiningBaseTypeChunk object = rowModel.getObject();
            FrequencyItem frequencyItem = object.getFrequencyItem();
            FrequencyItem.Status status = frequencyItem.getStatus();

            if (status.equals(FrequencyItem.Status.NEGATIVE_EXCLUDE)) {
                object.setStatus(RoleAnalysisOperationMode.NEGATIVE_EXCLUDE);
            } else if (status.equals(FrequencyItem.Status.POSITIVE_EXCLUDE)) {
                object.setStatus(RoleAnalysisOperationMode.POSITIVE_EXCLUDE);
            }

            return;
        }

        T rowModelObject = rowModel.getObject();
        FrequencyItem frequencyItem = rowModelObject.getFrequencyItem();
        double frequency = frequencyItem.getFrequency();
        boolean isInclude = rowModelObject.getStatus().isInclude();

        if (!isInclude && (minFrequency > frequency || maxFrequency < frequency)) {
            rowModel.getObject().setStatus(RoleAnalysisOperationMode.DISABLE);
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
    public static <T extends MiningBaseTypeChunk> Status resolveCellTypeUserTable(@NotNull String componentId,
            Item<ICellPopulator<MiningRoleTypeChunk>> cellItem,
            @NotNull MiningRoleTypeChunk rowModel,
            @NotNull MiningUserTypeChunk colModel,
            @NotNull LoadableDetachableModel<Map<String, String>> colorLoadableMap) {
        Map<String, String> colorMap = colorLoadableMap.getObject();
        RoleAnalysisObjectStatus rowObjectStatus = rowModel.getObjectStatus();
        RoleAnalysisObjectStatus colObjectStatus = colModel.getObjectStatus();
        Set<String> rowContainerId = rowObjectStatus.getContainerId();
        Set<String> colContainerId = colObjectStatus.getContainerId();
        Set<String> duplicatedElements = new HashSet<>();

        boolean secondStage;
        if (rowContainerId.isEmpty() && colContainerId.isEmpty()) {
            secondStage = true;
        } else {
            duplicatedElements = new HashSet<>(rowContainerId);
            duplicatedElements.retainAll(colContainerId);
            secondStage = !duplicatedElements.isEmpty();
        }

        ArrayList<String> element = new ArrayList<>(duplicatedElements);

        boolean firstStage = new HashSet<>(rowModel.getProperties()).containsAll(colModel.getMembers());
        boolean isCandidate = firstStage && secondStage;

        RoleAnalysisOperationMode rowStatus = rowObjectStatus.getRoleAnalysisOperationMode();
        RoleAnalysisOperationMode colStatus = colObjectStatus.getRoleAnalysisOperationMode();

        if (rowStatus.isNegativeExclude() || colStatus.isNegativeExclude()) {
            if (isCandidate) {
                negativeDisabledCell(componentId, cellItem);
                return RELATION_DISABLE;
            }
            emptyCell(componentId, cellItem);
            return RELATION_NONE;
        }

        if (rowStatus.isPositiveExclude() || colStatus.isPositiveExclude()) {
            if (isCandidate) {
                positiveDisabledCell(componentId, cellItem);
                return RELATION_DISABLE;
            }
            emptyCell(componentId, cellItem);
            return RELATION_NONE;
        }

        if (rowStatus.isDisable() || colStatus.isDisable()) {
            if (firstStage) {
                disabledCell(componentId, cellItem);
                return RELATION_DISABLE;
            }
            emptyCell(componentId, cellItem);
            return RELATION_NONE;
        }
        int size = duplicatedElements.size();

        if (rowStatus.isInclude() && colStatus.isInclude()) {
            if (isCandidate) {
                if (size > 1) {
                    reducedDuplicateCell(componentId, cellItem, duplicatedElements);
                    return RELATION_EXCLUDE;
                } else if (size == 1) {
                    reducedCell(componentId, cellItem, colorMap.get(element.get(0)), duplicatedElements);
                    return RELATION_INCLUDE;
                }
                reducedCell(componentId, cellItem, "#28A745", duplicatedElements);
                return RELATION_INCLUDE;
            } else if (secondStage) {
                if (size > 1) {
                    additionalDuplicateCell(componentId, cellItem, duplicatedElements);
                    return RELATION_EXCLUDE;
                } else if (size == 1) {
                    additionalCell(componentId, cellItem, colorMap.get(element.get(0)), duplicatedElements);
                    return RELATION_INCLUDE;
                }
                additionalCell(componentId, cellItem, "#28A745", duplicatedElements);
                return RELATION_INCLUDE;
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

    public static <T extends MiningBaseTypeChunk> boolean resolveCellTypeRoleTable(@NotNull String componentId,
            Item<ICellPopulator<MiningUserTypeChunk>> cellItem,
            @NotNull T rowModel,
            @NotNull T colModel,
            @NotNull LoadableDetachableModel<Map<String, String>> colorLoadableMap) {
        Map<String, String> colorMap = colorLoadableMap.getObject();
        RoleAnalysisObjectStatus rowObjectStatus = rowModel.getObjectStatus();
        RoleAnalysisObjectStatus colObjectStatus = colModel.getObjectStatus();
        Set<String> rowContainerId = rowObjectStatus.getContainerId();
        Set<String> colContainerId = colObjectStatus.getContainerId();
        Set<String> duplicatedElements = new HashSet<>();
        boolean secondStage;
        if (rowContainerId.isEmpty() && colContainerId.isEmpty()) {
            secondStage = true;
        } else {
            duplicatedElements = new HashSet<>(rowContainerId);
            duplicatedElements.retainAll(colContainerId);
            secondStage = !duplicatedElements.isEmpty();
        }

        ArrayList<String> element = new ArrayList<>(duplicatedElements);

        boolean firstStage = new HashSet<>(rowModel.getProperties()).containsAll(colModel.getMembers());
        boolean isCandidate = firstStage && secondStage;

        RoleAnalysisOperationMode rowStatus = rowObjectStatus.getRoleAnalysisOperationMode();
        RoleAnalysisOperationMode colStatus = colObjectStatus.getRoleAnalysisOperationMode();

        if (rowStatus.isNegativeExclude() || colStatus.isNegativeExclude()) {
            if (isCandidate) {
                negativeDisabledCell(componentId, cellItem);
                return false;
            }
            emptyCell(componentId, cellItem);
            return false;
        }

        if (rowStatus.isPositiveExclude() || colStatus.isPositiveExclude()) {
            if (isCandidate) {
                positiveDisabledCell(componentId, cellItem);
                return false;
            }
            emptyCell(componentId, cellItem);
            return false;
        }

        if (rowStatus.isDisable() || colStatus.isDisable()) {
            if (firstStage) {
                disabledCell(componentId, cellItem);
                return false;
            }
            emptyCell(componentId, cellItem);
            return false;
        }
        int size = duplicatedElements.size();

        if (rowStatus.isInclude() && colStatus.isInclude()) {
            if (isCandidate) {
                if (size > 1) {
                    reducedDuplicateCell(componentId, cellItem, duplicatedElements);
                    return false;
                } else if (size == 1) {

                    reducedCell(componentId, cellItem, colorMap.get(element.get(0)), duplicatedElements);
                    return true;
                }
                reducedCell(componentId, cellItem, "#28A745", duplicatedElements);
                return true;
            } else if (secondStage) {
                if (size > 1) {
                    additionalDuplicateCell(componentId, cellItem, duplicatedElements);
                    return false;
                } else if (size == 1) {
                    additionalCell(componentId, cellItem, colorMap.get(element.get(0)), duplicatedElements);
                    return true;
                }
                additionalCell(componentId, cellItem, "#28A745", duplicatedElements);
                return true;
            }
        }

        if (firstStage) {
            relationCell(componentId, cellItem);
        } else {
            emptyCell(componentId, cellItem);
        }
        return false;
    }

    /**
     * Initialize detection patterns for user-based analysis table.
     *
     * @param users The list of user models.
     * @param roles The list of role models.
     * @param detectedPatterns The detected pattern.
     * @param minFrequency The minimum frequency threshold.
     * @param maxFrequency The maximum frequency threshold.
     */
    public static void initUserBasedDetectionPattern(
            @NotNull PageBase pageBase,
            @NotNull List<MiningUserTypeChunk> users,
            @NotNull List<MiningRoleTypeChunk> roles,
            @NotNull List<DetectedPattern> detectedPatterns,
            double minFrequency,
            double maxFrequency,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

        List<List<String>> detectedPatternsRoles = new ArrayList<>();
        List<List<String>> detectedPatternsUsers = new ArrayList<>();
        List<String> candidateRolesIds = new ArrayList<>();
        detectedPatterns.forEach(detectedPattern -> {
            detectedPatternsRoles.add(new ArrayList<>(detectedPattern.getRoles()));
            detectedPatternsUsers.add(new ArrayList<>(detectedPattern.getUsers()));
            candidateRolesIds.add(detectedPattern.getIdentifier());
        });

        for (MiningRoleTypeChunk role : roles) {
            FrequencyItem frequencyItem = role.getFrequencyItem();
            double frequency = frequencyItem.getFrequency();

            IntStream.range(0, detectedPatternsRoles.size()).forEach(i -> {
                List<String> detectedPatternsRole = detectedPatternsRoles.get(i);
                List<String> chunkRoles = role.getRoles();
                if (new HashSet<>(detectedPatternsRole).containsAll(chunkRoles)) {
                    RoleAnalysisObjectStatus objectStatus = role.getObjectStatus();
                    objectStatus.setRoleAnalysisOperationMode(RoleAnalysisOperationMode.INCLUDE);
                    objectStatus.addContainerId(candidateRolesIds.get(i));
                    detectedPatternsRole.removeAll(chunkRoles);
                } else if (minFrequency > frequency && frequency < maxFrequency && !role.getStatus().isInclude()) {
                    role.setStatus(RoleAnalysisOperationMode.DISABLE);
                } else if (!role.getStatus().isInclude()) {
                    role.setStatus(RoleAnalysisOperationMode.EXCLUDE);
                }
            });
        }

        for (MiningUserTypeChunk user : users) {
            IntStream.range(0, detectedPatternsUsers.size()).forEach(i -> {
                List<String> detectedPatternsUser = detectedPatternsUsers.get(i);
                List<String> chunkUsers = user.getUsers();
                if (new HashSet<>(detectedPatternsUser).containsAll(chunkUsers)) {
                    RoleAnalysisObjectStatus objectStatus = user.getObjectStatus();
                    objectStatus.setRoleAnalysisOperationMode(RoleAnalysisOperationMode.INCLUDE);
                    objectStatus.addContainerId(candidateRolesIds.get(i));
                    detectedPatternsUser.removeAll(chunkUsers);
                } else if (!user.getStatus().isInclude()) {
                    user.setStatus(RoleAnalysisOperationMode.EXCLUDE);
                }
            });
        }

        int size = detectedPatternsUsers.size();

        IntStream.range(0, size).forEach(i -> {
            List<String> detectedPatternRoles = detectedPatternsRoles.get(i);
            List<String> detectedPatternUsers = detectedPatternsUsers.get(i);
            String candidateRoleId = candidateRolesIds.get(i);
            addAdditionalObject(
                    roleAnalysisService, candidateRoleId, detectedPatternUsers, detectedPatternRoles, users,
                    roles,
                    task,
                    result);
        });
    }

    public static void refreshCells(
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull List<MiningUserTypeChunk> users,
            @NotNull List<MiningRoleTypeChunk> roles,
            double minFrequency,
            double maxFrequency) {

        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {

            for (MiningUserTypeChunk user : users) {
                user.setStatus(RoleAnalysisOperationMode.EXCLUDE);
            }

            for (MiningRoleTypeChunk role : roles) {
                FrequencyItem frequencyItem = role.getFrequencyItem();
                double frequency = frequencyItem.getFrequency();
                if (minFrequency > frequency && frequency < maxFrequency && !role.getStatus().isInclude()) {
                    role.setStatus(RoleAnalysisOperationMode.DISABLE);
                } else {
                    role.setStatus(RoleAnalysisOperationMode.EXCLUDE);
                }
            }
        } else {
            for (MiningUserTypeChunk user : users) {
                FrequencyItem frequencyItem = user.getFrequencyItem();
                double frequency = frequencyItem.getFrequency();
                if (minFrequency > frequency && frequency < maxFrequency && !user.getStatus().isInclude()) {
                    user.setStatus(RoleAnalysisOperationMode.DISABLE);
                } else {
                    user.setStatus(RoleAnalysisOperationMode.EXCLUDE);
                }
            }

            for (MiningRoleTypeChunk role : roles) {
                role.setStatus(RoleAnalysisOperationMode.EXCLUDE);
            }
        }
    }

    /**
     * Initialize detection patterns for role-based analysis table.
     *
     * @param users The list of user models.
     * @param roles The list of role models.
     * @param detectedPatterns The detected pattern.
     * @param minFrequency The minimum frequency threshold.
     * @param maxFrequency The maximum frequency threshold.
     */
    public static void initRoleBasedDetectionPattern(
            @NotNull PageBase pageBase,
            @NotNull List<MiningUserTypeChunk> users,
            @NotNull List<MiningRoleTypeChunk> roles,
            @NotNull List<DetectedPattern> detectedPatterns,
            double minFrequency,
            double maxFrequency,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

        List<List<String>> detectedPatternsRoles = new ArrayList<>();
        List<List<String>> detectedPatternsUsers = new ArrayList<>();
        List<String> candidateRolesIds = new ArrayList<>();
        detectedPatterns.forEach(detectedPattern -> {
            detectedPatternsRoles.add(new ArrayList<>(detectedPattern.getRoles()));
            detectedPatternsUsers.add(new ArrayList<>(detectedPattern.getUsers()));
            candidateRolesIds.add(detectedPattern.getIdentifier());
        });

        for (MiningUserTypeChunk user : users) {
            FrequencyItem frequencyItem = user.getFrequencyItem();
            double frequency = frequencyItem.getFrequency();

            IntStream.range(0, detectedPatternsUsers.size()).forEach(i -> {
                List<String> detectedPatternsUser = detectedPatternsUsers.get(i);
                List<String> chunkUsers = user.getUsers();
                if (new HashSet<>(detectedPatternsUser).containsAll(chunkUsers)) {
                    RoleAnalysisObjectStatus objectStatus = user.getObjectStatus();
                    objectStatus.setRoleAnalysisOperationMode(RoleAnalysisOperationMode.INCLUDE);
                    objectStatus.addContainerId(candidateRolesIds.get(i));
                    detectedPatternsUser.removeAll(chunkUsers);
                } else if (minFrequency > frequency && frequency < maxFrequency && !user.getStatus().isInclude()) {
                    user.setStatus(RoleAnalysisOperationMode.DISABLE);
                } else if (!user.getStatus().isInclude()) {
                    user.setStatus(RoleAnalysisOperationMode.EXCLUDE);
                }
            });
        }

        for (MiningRoleTypeChunk role : roles) {
            IntStream.range(0, detectedPatternsRoles.size()).forEach(i -> {
                List<String> detectedPatternsRole = detectedPatternsRoles.get(i);
                List<String> chunkRoles = role.getRoles();
                if (new HashSet<>(detectedPatternsRole).containsAll(chunkRoles)) {
                    RoleAnalysisObjectStatus objectStatus = role.getObjectStatus();
                    objectStatus.setRoleAnalysisOperationMode(RoleAnalysisOperationMode.INCLUDE);
                    objectStatus.addContainerId(candidateRolesIds.get(i));
                    detectedPatternsRole.removeAll(chunkRoles);
                } else if (!role.getStatus().isInclude()) {
                    role.setStatus(RoleAnalysisOperationMode.EXCLUDE);
                }
            });
        }

        int size = detectedPatternsUsers.size();

        IntStream.range(0, size).forEach(i -> {
            List<String> detectedPatternRoles = detectedPatternsRoles.get(i);
            List<String> detectedPatternUsers = detectedPatternsUsers.get(i);
            String candidateRoleId = candidateRolesIds.get(i);
            addAdditionalObject(
                    roleAnalysisService, candidateRoleId, detectedPatternUsers, detectedPatternRoles, users,
                    roles,
                    task,
                    result);
        });
    }

    private static void addAdditionalObject(
            @NotNull RoleAnalysisService roleAnalysisService,
            String candidateRoleId,
            @NotNull List<String> detectedPatternUsers,
            @NotNull List<String> detectedPatternRoles,
            @NotNull List<MiningUserTypeChunk> users,
            @NotNull List<MiningRoleTypeChunk> roles,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisObjectStatus roleAnalysisObjectStatus = new RoleAnalysisObjectStatus(RoleAnalysisOperationMode.INCLUDE);
        roleAnalysisObjectStatus.setContainerId(Collections.singleton(candidateRoleId));

        if (!detectedPatternRoles.isEmpty()) {
            Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
            ListMultimap<String, String> mappedMembers = roleAnalysisService.extractUserTypeMembers(
                    userExistCache, null, new HashSet<>(detectedPatternRoles), task, result);

            for (String detectedPatternRole : detectedPatternRoles) {
                List<String> properties = new ArrayList<>(mappedMembers.get(detectedPatternRole));
                PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(detectedPatternRole, task, result);
                String chunkName = "Unknown";
                String iconColor = null;
                if (roleTypeObject != null) {
                    chunkName = roleTypeObject.getName().toString();
                    iconColor = roleAnalysisService.resolveFocusObjectIconColor(roleTypeObject.asObjectable(), task, result);
                }

                MiningRoleTypeChunk miningRoleTypeChunk = new MiningRoleTypeChunk(
                        Collections.singletonList(detectedPatternRole),
                        properties,
                        chunkName,
                        new FrequencyItem(100.0),
                        roleAnalysisObjectStatus);
                if (iconColor != null) {
                    miningRoleTypeChunk.setIconColor(iconColor);
                }
                roles.add(miningRoleTypeChunk);
            }

        }

        if (!detectedPatternUsers.isEmpty()) {
            for (String detectedPatternUser : detectedPatternUsers) {
                PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(detectedPatternUser, task, result);
                List<String> properties = new ArrayList<>();
                String chunkName = "Unknown";
                String iconColor = null;
                if (userTypeObject != null) {
                    chunkName = userTypeObject.getName().toString();
                    properties = getRolesOidAssignment(userTypeObject.asObjectable());
                    iconColor = roleAnalysisService.resolveFocusObjectIconColor(userTypeObject.asObjectable(), task, result);
                }

                MiningUserTypeChunk miningUserTypeChunk = new MiningUserTypeChunk(
                        Collections.singletonList(detectedPatternUser),
                        properties,
                        chunkName,
                        new FrequencyItem(100.0),
                        roleAnalysisObjectStatus);

                if (iconColor != null) {
                    miningUserTypeChunk.setIconColor(iconColor);
                }

                users.add(miningUserTypeChunk);
            }
        }
    }

    public static @NotNull Map<String, String> generateObjectColors(List<String> containerIds) {
        if (containerIds == null || containerIds.isEmpty()) {
            return Collections.emptyMap();
        }

//        Collections.sort(containerIds);

        int numberOfObjects = containerIds.size();

        Map<String, String> objectColorMap = new HashMap<>();

        int baseGreen = 0x00A65A;
        objectColorMap.put(containerIds.get(0), "#00A65A");
        if (numberOfObjects == 1) {
            return objectColorMap;
        }

        int brightnessStep = 255 / numberOfObjects;

        if (numberOfObjects < 3) {
            brightnessStep = 30;
        } else if (numberOfObjects < 5) {
            brightnessStep = 40;
        }

        for (int i = 1; i < numberOfObjects; i++) {
            int brightness = 255 - (i * brightnessStep);
            int greenValue = (baseGreen & 0xFF0000) | (brightness << 8) | (baseGreen & 0x0000FF);
            String hexColor = String.format("#%06X", greenValue);
            objectColorMap.put(containerIds.get(i), hexColor);
        }

        return objectColorMap;
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

        cellItem.add(components);
    }

    protected static <T> void reducedCell(@NotNull String componentId, @NotNull Item<ICellPopulator<T>> cellItem, String color,
            Set<String> duplicatedElements) {

        cellItem.add(AttributeModifier.append("style", "background-color: " + color + ";"));

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

        cellItem.add(components);
    }

    protected static <T> void additionalDuplicateCell(@NotNull String componentId, @NotNull Item<ICellPopulator<T>> cellItem,
            Set<String> duplicatedElements) {
        String cssIconClass = getCssIconClass();
        String cssIconColorClass = getCssIconColorClass();

        cellItem.add(AttributeModifier.append("class", "corner-hashed-bg"));

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

        cellItem.add(components);
    }

    protected static <T> void additionalCell(@NotNull String componentId,
            @NotNull Item<ICellPopulator<T>> cellItem,
            String color, Set<String> duplicatedElements) {
        String cssIconClass = getCssIconClass();
        String cssIconColorClass = getCssIconColorClass();

        cellItem.add(AttributeModifier.append("style", "background-color: " + color + ";"));

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

        cellItem.add(components);
    }

    protected static String getCssIconClass() {
        // return " fa fa-warning"
        // return " fas fa-plus-circle";
        return " fa fa-plus fa-lg";
    }

    protected static String getCssIconColorClass() {
        // return " fa fa-warning"
        return " black";
    }

}
