/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table;

import java.util.*;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.google.common.collect.ListMultimap;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;

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
            IModel<T> rowModel,
            double minFrequency,
            double maxFrequency) {
        T rowModelObject = rowModel.getObject();
        double frequency = rowModelObject.getFrequency();
        boolean isInclude = rowModelObject.getStatus().isInclude();

        if (!isInclude && (minFrequency > frequency || maxFrequency < frequency)) {
            rowModel.getObject().setStatus(RoleAnalysisOperationMode.DISABLE);
        }
    }

    /**
     * Resolve the cell color for role analysis table.
     *
     * @param rowModel The row model (properties to compare).
     * @param colModel The column model (members to compare).
     * @return The CSS class representing the cell color.
     */
    public static <T extends MiningBaseTypeChunk> String resolveCellColor(
            T rowModel, T colModel) {
        boolean isCandidate = new HashSet<>(rowModel.getProperties()).containsAll(colModel.getMembers());
        RoleAnalysisOperationMode rowStatus = rowModel.getStatus();
        RoleAnalysisOperationMode colStatus = colModel.getStatus();

        if (rowStatus.isDisable() || colStatus.isDisable()) {
            if (isCandidate) {
                return "bg-danger";
            }
            return "";
        }

        if (rowStatus.isInclude() && colStatus.isInclude()) {
            if (isCandidate) {
                return "bg-success";
            } else {
                return "bg-warning";
            }
        }

        if (isCandidate) {
            return "table-dark";
        } else {
            return "";
        }
    }

    /**
     * Initialize detection patterns for user-based analysis table.
     *
     * @param users The list of user models.
     * @param roles The list of role models.
     * @param detectedPattern The detected pattern.
     * @param minFrequency The minimum frequency threshold.
     * @param maxFrequency The maximum frequency threshold.
     */
    public static void initUserBasedDetectionPattern(List<MiningUserTypeChunk> users,
            List<MiningRoleTypeChunk> roles,
            DetectedPattern detectedPattern,
            double minFrequency,
            double maxFrequency,
            PageBase pageBase,
            Task task,
            OperationResult result) {

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        List<String> detectedPatternRoles = new ArrayList<>(detectedPattern.getRoles());
        List<String> detectedPatternUsers = new ArrayList<>(detectedPattern.getUsers());

        for (MiningRoleTypeChunk role : roles) {
            double frequency = role.getFrequency();
            if (detectedPattern.getRoles().containsAll(role.getRoles())) {
                role.setStatus(RoleAnalysisOperationMode.INCLUDE);
                detectedPatternRoles.removeAll(role.getRoles());
            } else if (minFrequency > frequency && frequency < maxFrequency) {
                role.setStatus(RoleAnalysisOperationMode.DISABLE);
            } else {
                role.setStatus(RoleAnalysisOperationMode.EXCLUDE);
            }
        }

        for (MiningUserTypeChunk user : users) {
            if (detectedPattern.getUsers().containsAll(user.getUsers())) {
                user.setStatus(RoleAnalysisOperationMode.INCLUDE);
                detectedPatternUsers.removeAll(user.getUsers());
            } else {
                user.setStatus(RoleAnalysisOperationMode.EXCLUDE);
            }
        }
        addAdditionalObject(
                roleAnalysisService, detectedPatternUsers, detectedPatternRoles, users,
                roles,
                task,
                result
        );

    }

    private static void addAdditionalObject(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<String> detectedPatternUsers,
            @NotNull List<String> detectedPatternRoles,
            @NotNull List<MiningUserTypeChunk> users,
            @NotNull List<MiningRoleTypeChunk> roles,
            @NotNull Task task,
            @NotNull OperationResult result) {
        if (!detectedPatternRoles.isEmpty()) {
            Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
            ListMultimap<String, String> mappedMembers = roleAnalysisService.extractUserTypeMembers(
                    userExistCache, null, new HashSet<>(detectedPatternRoles), task, result);

            for (String detectedPatternRole : detectedPatternRoles) {
                List<String> properties = new ArrayList<>(mappedMembers.get(detectedPatternRole));
                PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(detectedPatternRole, task, result);
                String chunkName = "Unknown";
                if (roleTypeObject != null) {
                    chunkName = roleTypeObject.getName().toString();
                }
                MiningRoleTypeChunk miningRoleTypeChunk = new MiningRoleTypeChunk(
                        Collections.singletonList(detectedPatternRole),
                        properties,
                        chunkName,
                        100.0,
                        RoleAnalysisOperationMode.INCLUDE);
                roles.add(miningRoleTypeChunk);
            }

        }

        if (!detectedPatternUsers.isEmpty()) {
            for (String detectedPatternUser : detectedPatternUsers) {
                PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(detectedPatternUser, task, result);
                List<String> properties = new ArrayList<>();
                String chunkName = "Unknown";
                if (userTypeObject != null) {
                    chunkName = userTypeObject.getName().toString();
                    properties = getRolesOidAssignment(userTypeObject.asObjectable());
                }

                MiningUserTypeChunk miningUserTypeChunk = new MiningUserTypeChunk(
                        Collections.singletonList(detectedPatternUser),
                        properties,
                        chunkName,
                        100.0,
                        RoleAnalysisOperationMode.INCLUDE);
                users.add(miningUserTypeChunk);
            }
        }
    }

    /**
     * Initialize detection patterns for role-based analysis table.
     *
     * @param users The list of user models.
     * @param roles The list of role models.
     * @param detectedPattern The detected pattern.
     * @param minFrequency The minimum frequency threshold.
     * @param maxFrequency The maximum frequency threshold.
     */
    public static void initRoleBasedDetectionPattern(List<MiningUserTypeChunk> users,
            List<MiningRoleTypeChunk> roles,
            DetectedPattern detectedPattern,
            double minFrequency,
            double maxFrequency,
            PageBase pageBase,
            Task task,
            OperationResult result) {

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        List<String> detectedPatternRoles = new ArrayList<>(detectedPattern.getRoles());
        List<String> detectedPatternUsers = new ArrayList<>(detectedPattern.getUsers());

        for (MiningUserTypeChunk user : users) {
            double frequency = user.getFrequency();
            if (detectedPattern.getUsers().containsAll(user.getUsers())) {
                user.setStatus(RoleAnalysisOperationMode.INCLUDE);
                detectedPatternUsers.removeAll(user.getUsers());
            } else if (minFrequency > frequency && frequency < maxFrequency) {
                user.setStatus(RoleAnalysisOperationMode.DISABLE);
            } else {
                user.setStatus(RoleAnalysisOperationMode.EXCLUDE);
            }
        }

        for (MiningRoleTypeChunk role : roles) {
            if (detectedPattern.getRoles().containsAll(role.getRoles())) {
                role.setStatus(RoleAnalysisOperationMode.INCLUDE);
                detectedPatternRoles.removeAll(role.getRoles());
            } else {
                role.setStatus(RoleAnalysisOperationMode.EXCLUDE);
            }
        }

        addAdditionalObject(
                roleAnalysisService, detectedPatternUsers, detectedPatternRoles, users,
                roles,
                task,
                result
        );
    }

    /**
     * Update cell mining status (color).
     *
     * @param cellItem The cell item.
     * @param componentId The component ID.
     * @param cellColor The CSS class representing the cell color.
     * @param <T> The cell item type.
     */
    public static <T> void updateCellMiningStatus(Item<ICellPopulator<T>> cellItem, String componentId, String cellColor) {
        cellItem.add(AttributeModifier.append("class", cellColor));
        cellItem.add(new EmptyPanel(componentId));
    }

}
