/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table;

import java.util.HashSet;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;

import org.apache.wicket.model.IModel;

/**
 * Utility class for resolving cell colors and status in the context of role analysis tables.
 * <p>
 * This class provides utility methods for resolving cell colors, updating mining status, and initializing detection patterns
 * for user-based and role-based role analysis tables.
 */
public class RoleAnalysisTableCellFillResolver {

    /**
     * Update the mining status of a row based on frequency for user-based analysis.
     *
     * @param rowModel      The model of the row to update.
     * @param minFrequency  The minimum frequency threshold.
     * @param maxFrequency  The maximum frequency threshold.
     */
    public static void updateFrequencyUserBased(IModel<MiningRoleTypeChunk> rowModel, double minFrequency, double maxFrequency) {
        if (minFrequency > rowModel.getObject().getFrequency() && rowModel.getObject().getFrequency() < maxFrequency) {
            rowModel.getObject().setStatus(RoleAnalysisOperationMode.DISABLE);
        } else if (maxFrequency < rowModel.getObject().getFrequency()) {
            rowModel.getObject().setStatus(RoleAnalysisOperationMode.DISABLE);
        }
    }

    /**
     * Update the mining status of a row based on frequency for role-based analysis.
     *
     * @param rowModel      The model of the row to update.
     * @param minFrequency  The minimum frequency threshold.
     * @param maxFrequency  The maximum frequency threshold.
     */
    public static void updateFrequencyRoleBased(IModel<MiningUserTypeChunk> rowModel, double minFrequency, double maxFrequency) {
        if (minFrequency > rowModel.getObject().getFrequency() && rowModel.getObject().getFrequency() < maxFrequency) {
            rowModel.getObject().setStatus(RoleAnalysisOperationMode.DISABLE);
        } else if (maxFrequency < rowModel.getObject().getFrequency()) {
            rowModel.getObject().setStatus(RoleAnalysisOperationMode.DISABLE);
        }
    }

    /**
     * Resolve the cell color for user-based analysis.
     *
     * @param rowModel  The row model.
     * @param colModel  The column model.
     * @return The CSS class representing the cell color.
     */
    public static String resolveUserBasedCellColor(MiningRoleTypeChunk rowModel, MiningUserTypeChunk colModel) {
        boolean isCandidate = new HashSet<>(rowModel.getUsers()).containsAll(colModel.getUsers());
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
     * Resolve the cell color for role-based analysis.
     *
     * @param colModel  The column model.
     * @param rowModel  The row model.
     * @return The CSS class representing the cell color.
     */
    public static String resolveRoleBasedCellColor(MiningRoleTypeChunk colModel, MiningUserTypeChunk rowModel) {
        boolean isCandidate = new HashSet<>(rowModel.getRoles()).containsAll(colModel.getRoles());

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
     * @param users             The list of user models.
     * @param roles             The list of role models.
     * @param detectedPattern   The detected pattern.
     * @param minFrequency      The minimum frequency threshold.
     * @param maxFrequency      The maximum frequency threshold.
     */
    public static void initUserBasedDetectionPattern(List<MiningUserTypeChunk> users, List<MiningRoleTypeChunk> roles,
            DetectedPattern detectedPattern, double minFrequency, double maxFrequency) {

        for (MiningUserTypeChunk user : users) {
            if (detectedPattern.getUsers().containsAll(user.getUsers())) {
                user.setStatus(RoleAnalysisOperationMode.INCLUDE);
            } else {
                user.setStatus(RoleAnalysisOperationMode.EXCLUDE);
            }
        }

        for (MiningRoleTypeChunk role : roles) {
            double frequency = role.getFrequency();
            if (minFrequency > frequency && frequency < maxFrequency) {
                role.setStatus(RoleAnalysisOperationMode.DISABLE);
            } else if (detectedPattern.getRoles().containsAll(role.getRoles())) {
                role.setStatus(RoleAnalysisOperationMode.INCLUDE);
            } else {
                role.setStatus(RoleAnalysisOperationMode.EXCLUDE);
            }
        }

    }

    /**
     * Initialize detection patterns for role-based analysis table.
     *
     * @param users             The list of user models.
     * @param roles             The list of role models.
     * @param detectedPattern   The detected pattern.
     * @param minFrequency      The minimum frequency threshold.
     * @param maxFrequency      The maximum frequency threshold.
     */
    public static void initRoleBasedDetectionPattern(List<MiningUserTypeChunk> users, List<MiningRoleTypeChunk> roles,
            DetectedPattern detectedPattern, double minFrequency, double maxFrequency) {

        for (MiningUserTypeChunk user : users) {
            double frequency = user.getFrequency();
            if (minFrequency > frequency && frequency < maxFrequency) {
                user.setStatus(RoleAnalysisOperationMode.DISABLE);
            } else if (detectedPattern.getUsers().containsAll(user.getUsers())) {
                user.setStatus(RoleAnalysisOperationMode.INCLUDE);
            } else {
                user.setStatus(RoleAnalysisOperationMode.EXCLUDE);
            }
        }

        for (MiningRoleTypeChunk role : roles) {
            if (detectedPattern.getRoles().containsAll(role.getRoles())) {
                role.setStatus(RoleAnalysisOperationMode.INCLUDE);
            } else {
                role.setStatus(RoleAnalysisOperationMode.EXCLUDE);
            }
        }

    }

    /**
     * Update cell mining status (color).
     *
     * @param cellItem     The cell item.
     * @param componentId  The component ID.
     * @param cellColor    The CSS class representing the cell color.
     * @param <T>          The cell item type.
     */
    public static <T> void updateCellMiningStatus(Item<ICellPopulator<T>> cellItem, String componentId, String cellColor) {
        cellItem.add(AttributeModifier.append("class", cellColor));
        cellItem.add(new EmptyPanel(componentId));
    }

}
