/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResult;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The RoleAnalysisModel class stores role analysis data, count of roles and users that are used in the histogram chart.
 * It displays the number of grouped roles with same number of users and the number of users that are assigned to the roles.
 */
public class RoleAnalysisSimpleModel implements Serializable {

    double density;
    String description;

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

    public static @NotNull List<RoleAnalysisSimpleModel> getRoleAnalysisSimpleModel(
            @Nullable RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult,
            @Nullable RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult) {
        List<RoleAnalysisSimpleModel> roleAnalysisSimpleModel = new ArrayList<>();

        if(roleAttributeAnalysisResult == null || userAttributeAnalysisResult == null) {
            return roleAnalysisSimpleModel;
        }
        List<RoleAnalysisAttributeAnalysis> roleAttributeAnalysis = roleAttributeAnalysisResult.getAttributeAnalysis();
        List<RoleAnalysisAttributeAnalysis> userAttributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();

        for (RoleAnalysisAttributeAnalysis attributeAnalysis : roleAttributeAnalysis) {
            String itemDescription = attributeAnalysis.getItemPath();
            if (itemDescription != null && !itemDescription.isEmpty()) {
                itemDescription = Character.toUpperCase(itemDescription.charAt(0)) + itemDescription.substring(1);
            }
            roleAnalysisSimpleModel.add(
                    new RoleAnalysisSimpleModel(attributeAnalysis.getDensity(),
                            "(Role) " + itemDescription));
        }

        for (RoleAnalysisAttributeAnalysis attributeAnalysis : userAttributeAnalysis) {
            String itemDescription = attributeAnalysis.getItemPath();
            if (itemDescription != null && !itemDescription.isEmpty()) {
                itemDescription = Character.toUpperCase(itemDescription.charAt(0)) + itemDescription.substring(1);
            }
            roleAnalysisSimpleModel.add(
                    new RoleAnalysisSimpleModel(attributeAnalysis.getDensity(),
                            "(User) " + itemDescription));
        }

        roleAnalysisSimpleModel.sort(Comparator.comparingDouble(RoleAnalysisSimpleModel::getDensity));
        return roleAnalysisSimpleModel;
    }

}
