/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResult;

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
            @Nullable RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult,
            @Nullable RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult) {
        List<RoleAnalysisSimpleModel> roleAnalysisSimpleModel = new ArrayList<>();

        if (roleAttributeAnalysisResult == null || userAttributeAnalysisResult == null) {
            return roleAnalysisSimpleModel;
        }
        List<RoleAnalysisAttributeAnalysis> roleAttributeAnalysis = roleAttributeAnalysisResult.getAttributeAnalysis();
        List<RoleAnalysisAttributeAnalysis> userAttributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();

        for (RoleAnalysisAttributeAnalysis attributeAnalysis : roleAttributeAnalysis) {
            ItemPathType itemDescriptionType = attributeAnalysis.getItemPath();
            if (itemDescriptionType == null) {
                continue;
            }
            ItemPath itemDescription = itemDescriptionType.getItemPath();
            roleAnalysisSimpleModel.add(
                    new RoleAnalysisSimpleModel(attributeAnalysis.getDensity(),
                            "(Role) " + itemDescription));
        }

        for (RoleAnalysisAttributeAnalysis attributeAnalysis : userAttributeAnalysis) {
            ItemPathType itemDescriptionType = attributeAnalysis.getItemPath();
            if (itemDescriptionType == null) {
                continue;
            }
            ItemPath itemDescription = itemDescriptionType.getItemPath();
//            if (itemDescription != null && !itemDescription.isEmpty()) {
//                itemDescription = Character.toUpperCase(itemDescription.charAt(0)) + itemDescription.substring(1);
//            }
            roleAnalysisSimpleModel.add(
                    new RoleAnalysisSimpleModel(attributeAnalysis.getDensity(),
                            "(User) " + itemDescription));
        }

        roleAnalysisSimpleModel.sort(Comparator.comparingDouble(RoleAnalysisSimpleModel::getDensity));
        return roleAnalysisSimpleModel;
    }

    public static @NotNull List<RoleAnalysisSimpleModel> getRoleAnalysisSimpleComparedModel(
            @Nullable RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult,
            @Nullable RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult,
            @Nullable RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResultCompared,
            @Nullable RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResultCompared) {
        List<RoleAnalysisSimpleModel> roleAnalysisSimpleModel = new ArrayList<>();

        if (roleAttributeAnalysisResult == null && userAttributeAnalysisResult == null) {
            return roleAnalysisSimpleModel;
        }

        Map<ItemPath, RoleAnalysisAttributeAnalysis> roleAttributeAnalysisComparedMap = new HashMap<>();
        Map<ItemPath, RoleAnalysisAttributeAnalysis> userAttributeAnalysisComparedMap = new HashMap<>();

        if (roleAttributeAnalysisResultCompared != null) {
            for (RoleAnalysisAttributeAnalysis attributeAnalysis : roleAttributeAnalysisResultCompared.getAttributeAnalysis()) {
                roleAttributeAnalysisComparedMap.put(attributeAnalysis.getItemPath().getItemPath(), attributeAnalysis);
            }
        }

        if (userAttributeAnalysisResultCompared != null) {
            for (RoleAnalysisAttributeAnalysis attributeAnalysis : userAttributeAnalysisResultCompared.getAttributeAnalysis()) {
                userAttributeAnalysisComparedMap.put(attributeAnalysis.getItemPath().getItemPath(), attributeAnalysis);
            }
        }

        if (roleAttributeAnalysisResult != null) {
            List<RoleAnalysisAttributeAnalysis> roleAttributeAnalysis = roleAttributeAnalysisResult.getAttributeAnalysis();
            for (RoleAnalysisAttributeAnalysis attributeAnalysis : roleAttributeAnalysis) {
                ItemPathType itemDescriptionType = attributeAnalysis.getItemPath();
                if (itemDescriptionType == null) {
                    continue;
                }

                ItemPath itemPath = itemDescriptionType.getItemPath();
//                if (itemDescription != null && !itemDescription.isEmpty()) {
//                    itemDescription = Character.toUpperCase(itemDescription.charAt(0)) + itemDescription.substring(1);
//                }
                RoleAnalysisSimpleModel roleAnalysisSimpleModelPreparation = new RoleAnalysisSimpleModel(attributeAnalysis.getDensity(),
                        "(Role) " + itemPath);
                roleAnalysisSimpleModelPreparation.setCompared(true);
//                String itemPath = attributeAnalysis.getItemPath();

                roleAnalysisSimpleModelPreparation.setComparedDensity(roleAttributeAnalysisComparedMap.get(itemPath) != null ?
                        roleAttributeAnalysisComparedMap.get(itemPath).getDensity() : 0.0);
                roleAnalysisSimpleModel.add(roleAnalysisSimpleModelPreparation);
            }
        }

        if (userAttributeAnalysisResult != null) {
            List<RoleAnalysisAttributeAnalysis> userAttributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();
            for (RoleAnalysisAttributeAnalysis attributeAnalysis : userAttributeAnalysis) {
                ItemPathType itemDescriptionType = attributeAnalysis.getItemPath();
                if (itemDescriptionType == null) {
                    continue;
                }
                ItemPath itemDescription = itemDescriptionType.getItemPath();
//                if (itemDescription != null && !itemDescription.isEmpty()) {
//                    itemDescription = Character.toUpperCase(itemDescription.charAt(0)) + itemDescription.substring(1);
//                }

                RoleAnalysisSimpleModel roleAnalysisSimpleModelPreparation = new RoleAnalysisSimpleModel(attributeAnalysis.getDensity(),
                        "(User) " + itemDescription);
                roleAnalysisSimpleModelPreparation.setCompared(true);
//                String itemPath = attributeAnalysis.getItemPath();
                roleAnalysisSimpleModelPreparation.setComparedDensity(userAttributeAnalysisComparedMap.get(itemDescription) != null ?
                        userAttributeAnalysisComparedMap.get(itemDescription).getDensity() : 0.0);
                roleAnalysisSimpleModel.add(roleAnalysisSimpleModelPreparation);
            }
        }

        roleAnalysisSimpleModel.sort(Comparator.comparingDouble(RoleAnalysisSimpleModel::getDensity));
        return roleAnalysisSimpleModel;
    }

}
