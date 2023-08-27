/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;

public class TableCellFillOperation {

    public static void updateFrequencyUserBased(IModel<MiningRoleTypeChunk> rowModel, double minFrequency, double maxFrequency) {
        if (minFrequency > rowModel.getObject().getFrequency() && rowModel.getObject().getFrequency() < maxFrequency) {
            rowModel.getObject().setStatus(RoleAnalysisOperationMode.DISABLE);
        } else if (maxFrequency < rowModel.getObject().getFrequency()) {
            rowModel.getObject().setStatus(RoleAnalysisOperationMode.DISABLE);
        }
    }

    public static void updateFrequencyRoleBased(IModel<MiningUserTypeChunk> rowModel, double minFrequency, double maxFrequency) {
        if (minFrequency > rowModel.getObject().getFrequency() && rowModel.getObject().getFrequency() < maxFrequency) {
            rowModel.getObject().setStatus(RoleAnalysisOperationMode.DISABLE);
        } else if (maxFrequency < rowModel.getObject().getFrequency()) {
            rowModel.getObject().setStatus(RoleAnalysisOperationMode.DISABLE);
        }
    }

    private static void intersectionModeRoleProcess(Item<ICellPopulator<MiningUserTypeChunk>> cellItem, String componentId,
            IModel<MiningUserTypeChunk> model, List<String> rowRoles, RoleAnalysisOperationMode colRoleAnalysisOperationMode,
            List<String> colRoles, DetectedPattern intersection, MiningRoleTypeChunk roleChunk) {

        RoleAnalysisOperationMode roleAnalysisOperationMode = model.getObject().getStatus();

        if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.DISABLE) || colRoleAnalysisOperationMode.equals(RoleAnalysisOperationMode.DISABLE)) {
            filledCell(cellItem, componentId, rowRoles, colRoles, "bg-danger", "");
        } else if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.NEUTRAL) || colRoleAnalysisOperationMode.equals(RoleAnalysisOperationMode.NEUTRAL)) {
            if (intersection != null && intersection.getUsers() != null) {
                Set<String> roles = intersection.getUsers();
                boolean found = new HashSet<>(roles).containsAll(colRoles) && new HashSet<>(rowRoles).containsAll(roles);

                if (found) {
                    model.getObject().setStatus(RoleAnalysisOperationMode.ADD);
                    roleChunk.setStatus(RoleAnalysisOperationMode.ADD);
                    filledCell(cellItem, componentId, rowRoles, colRoles, "bg-success", "table-dark");
                } else {
                    filledCell(cellItem, componentId, rowRoles, colRoles, "table-dark", "");
                }
            } else {
                filledCell(cellItem, componentId, rowRoles, colRoles, "table-dark", "");
            }
        } else if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.ADD) && colRoleAnalysisOperationMode.equals(RoleAnalysisOperationMode.ADD)) {
            filledCell(cellItem, componentId, rowRoles, colRoles, "bg-success", "bg-warning");
        } else if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.REMOVE) || colRoleAnalysisOperationMode.equals(RoleAnalysisOperationMode.REMOVE)) {
            filledCell(cellItem, componentId, rowRoles, colRoles, "table-dark", "");
        } else {
            filledCell(cellItem, componentId, rowRoles, colRoles, "table-dark", "");
        }
    }

    public static void updateRoleBasedTableData(Item<ICellPopulator<MiningUserTypeChunk>> cellItem, String componentId,
            IModel<MiningUserTypeChunk> model, List<String> rowRoles,
            RoleAnalysisOperationMode colRoleAnalysisOperationMode, List<String> colRoles, DetectedPattern intersection,
            MiningRoleTypeChunk roleChunk) {

        intersectionModeRoleProcess(cellItem, componentId, model, rowRoles, colRoleAnalysisOperationMode, colRoles, intersection, roleChunk);

    }

    public static void updateUserBasedTableData(Item<ICellPopulator<MiningRoleTypeChunk>> cellItem, String componentId,
            IModel<MiningRoleTypeChunk> model, List<String> rowUsers,
            List<String> colUsers, DetectedPattern intersection,
            RoleAnalysisOperationMode colRoleAnalysisOperationMode, MiningUserTypeChunk userChunk) {
        intersectionModeUserProcess(cellItem, componentId, model, rowUsers, colUsers, intersection, colRoleAnalysisOperationMode, userChunk);

    }

    private static void intersectionModeUserProcess(Item<ICellPopulator<MiningRoleTypeChunk>> cellItem, String componentId,
            IModel<MiningRoleTypeChunk> model, List<String> rowUsers, List<String> colUsers, DetectedPattern intersection,
            RoleAnalysisOperationMode colRoleAnalysisOperationMode, MiningUserTypeChunk userChunk) {

        RoleAnalysisOperationMode roleAnalysisOperationMode = model.getObject().getStatus();

        if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.DISABLE) || colRoleAnalysisOperationMode.equals(RoleAnalysisOperationMode.DISABLE)) {
            filledCell(cellItem, componentId, rowUsers, colUsers, "bg-danger", "");
        } else if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.NEUTRAL) || colRoleAnalysisOperationMode.equals(RoleAnalysisOperationMode.NEUTRAL)) {
            if (intersection != null && intersection.getUsers() != null) {
                Set<String> users = intersection.getUsers();
                boolean found = users != null && new HashSet<>(users).containsAll(colUsers) && new HashSet<>(rowUsers).containsAll(users);

                if (found) {
                    model.getObject().setStatus(RoleAnalysisOperationMode.ADD);
                    userChunk.setStatus(RoleAnalysisOperationMode.ADD);
                    Tools.filledCell(cellItem, componentId, "bg-success");
                } else {
                    filledCell(cellItem, componentId, rowUsers, colUsers, "table-dark", "");
                }
            } else {
                filledCell(cellItem, componentId, rowUsers, colUsers, "table-dark", "");
            }
        } else if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.ADD) && colRoleAnalysisOperationMode.equals(RoleAnalysisOperationMode.ADD)) {
            filledCell(cellItem, componentId, rowUsers, colUsers, "bg-success", "bg-warning");
        } else if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.REMOVE) || colRoleAnalysisOperationMode.equals(RoleAnalysisOperationMode.REMOVE)) {
            filledCell(cellItem, componentId, rowUsers, colUsers, "table-dark", "");
        } else {
            filledCell(cellItem, componentId, rowUsers, colUsers, "table-dark", "");
        }
    }

    public static void filledCell(@NotNull Item<?> cellItem, String componentId, List<String> containsAll,
            List<String> contain, String colorTrue, String colorFalse) {
        String cellColor = new HashSet<>(containsAll).containsAll(new HashSet<>(contain)) ? colorTrue : colorFalse;
        cellItem.add(AttributeModifier.append("class", cellColor));
        cellItem.add(new EmptyPanel(componentId));
    }

}
