/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResultType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * The RoleAnalysisModel class stores role analysis data, count of roles and users that are used in the histogram chart.
 * It displays the number of grouped roles with same number of users and the number of users that are assigned to the roles.
 */
public class RoleAnalysisSimpleModel implements Serializable {

    double density;
    double comparedDensity;
    String description;

    boolean isCompared = false;

    private boolean selected = false;

    public RoleAnalysisSimpleModel(double density, String description) {
        this.density = density;
        this.description = description;
    }

    public double getDensity() {
        return density;
    }

    public String getDescription() {
        return description;
    }

    public double getComparedPercentagePart() {
        return (density * 0.01) * comparedDensity;
    }

    public double getComparedDensity() {
        return comparedDensity;
    }

    public void setComparedDensity(double comparedDensity) {
        this.comparedDensity = comparedDensity;
    }

    public boolean isCompared() {
        return isCompared;
    }

    public void setCompared(boolean compared) {
        isCompared = compared;
    }

    public static @NotNull List<RoleAnalysisSimpleModel> getRoleAnalysisSimpleModel(
            @Nullable RoleAnalysisAttributeAnalysisResultType roleAttributeAnalysisResult,
            @Nullable RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult) {
        List<RoleAnalysisSimpleModel> roleAnalysisSimpleModel = new ArrayList<>();

        if (roleAttributeAnalysisResult != null) {
            loadAttributeModel(roleAttributeAnalysisResult, roleAnalysisSimpleModel, "(Role) ");
        }

        if (userAttributeAnalysisResult != null) {
            loadAttributeModel(userAttributeAnalysisResult, roleAnalysisSimpleModel, "(User) ");
        }

        roleAnalysisSimpleModel.sort(Comparator.comparingDouble(RoleAnalysisSimpleModel::getDensity));
        return roleAnalysisSimpleModel;
    }

    private static void loadAttributeModel(@NotNull RoleAnalysisAttributeAnalysisResultType roleAttributeAnalysisResult,
            @NotNull List<RoleAnalysisSimpleModel> roleAnalysisSimpleModel,
            @NotNull String prefix) {
        List<RoleAnalysisAttributeAnalysisType> roleAttributeAnalysis = roleAttributeAnalysisResult.getAttributeAnalysis();
        for (RoleAnalysisAttributeAnalysisType attributeAnalysis : roleAttributeAnalysis) {
            ItemPathType itemDescriptionType = attributeAnalysis.getItemPath();
            if (itemDescriptionType == null) {
                continue;
            }
            ItemPath itemDescription = itemDescriptionType.getItemPath();
            roleAnalysisSimpleModel.add(
                    new RoleAnalysisSimpleModel(attributeAnalysis.getDensity(),
                            prefix + itemDescription));
        }
    }

    public static @NotNull List<RoleAnalysisSimpleModel> getRoleAnalysisSimpleComparedModel(
            @Nullable RoleAnalysisAttributeAnalysisResultType roleAttributeAnalysisResult,
            @Nullable RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult,
            @Nullable RoleAnalysisAttributeAnalysisResultType roleAttributeAnalysisResultCompared,
            @Nullable RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResultCompared) {
        List<RoleAnalysisSimpleModel> roleAnalysisSimpleModel = new ArrayList<>();

        if (roleAttributeAnalysisResult == null && userAttributeAnalysisResult == null) {
            return roleAnalysisSimpleModel;
        }

        if (roleAttributeAnalysisResult != null) {
            loadComparedAttributeModel(
                    roleAttributeAnalysisResult, "(Role) ", roleAttributeAnalysisResultCompared, roleAnalysisSimpleModel);
        }

        if (userAttributeAnalysisResult != null) {
            loadComparedAttributeModel(
                    userAttributeAnalysisResult, "(User) ", userAttributeAnalysisResultCompared, roleAnalysisSimpleModel);
        }

        roleAnalysisSimpleModel.sort(Comparator.comparingDouble(RoleAnalysisSimpleModel::getDensity));
        return roleAnalysisSimpleModel;
    }

    private static void loadComparedAttributeModel(
            @NotNull RoleAnalysisAttributeAnalysisResultType roleAttributeAnalysisResult,
            @NotNull String prefix,
            @Nullable RoleAnalysisAttributeAnalysisResultType attributeAnalysisResultCompared,
            @NotNull List<RoleAnalysisSimpleModel> roleAnalysisSimpleModel) {
        Map<ItemPath, RoleAnalysisAttributeAnalysisType> map = new HashMap<>();

        if (attributeAnalysisResultCompared != null) {
            for (RoleAnalysisAttributeAnalysisType attributeAnalysis : attributeAnalysisResultCompared.getAttributeAnalysis()) {
                map.put(attributeAnalysis.getItemPath().getItemPath(), attributeAnalysis);
            }
        }

        List<RoleAnalysisAttributeAnalysisType> roleAttributeAnalysis = roleAttributeAnalysisResult.getAttributeAnalysis();
        for (RoleAnalysisAttributeAnalysisType attributeAnalysis : roleAttributeAnalysis) {
            ItemPathType itemDescriptionType = attributeAnalysis.getItemPath();
            if (itemDescriptionType == null) {
                continue;
            }

            ItemPath itemPath = itemDescriptionType.getItemPath();

            RoleAnalysisSimpleModel roleAnalysisSimpleModelPreparation = new RoleAnalysisSimpleModel(attributeAnalysis.getDensity(),
                    prefix + itemPath);
            roleAnalysisSimpleModelPreparation.setCompared(true);

            roleAnalysisSimpleModelPreparation.setComparedDensity(map.get(itemPath) != null ?
                    map.get(itemPath).getDensity() : 0.0);
            roleAnalysisSimpleModel.add(roleAnalysisSimpleModelPreparation);
        }
    }

}
