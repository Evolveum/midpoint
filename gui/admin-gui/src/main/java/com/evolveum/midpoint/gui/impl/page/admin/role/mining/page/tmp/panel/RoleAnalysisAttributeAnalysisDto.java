/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBarForm;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatistics;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RoleAnalysisAttributeAnalysisDto implements Serializable {

    private String displayNameKey;
    private boolean selected = true;
    private int attributeValuesSize;
    private Class<?> type;
    private List<RoleAnalysisAttributeStatistics> attributeStatistics;

    public RoleAnalysisAttributeAnalysisDto(RoleAnalysisAttributeAnalysis analysis, Class<? extends FocusType> type) {
        this.attributeStatistics = analysis.getAttributeStatistics();
        this.attributeValuesSize = analysis.getAttributeStatistics().size();
        this.type = type;
        ItemPath itemPath = resolveItemPath(analysis);
        if (itemPath != null) {
            PrismObjectDefinition<?> userDefinition = PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
            ItemDefinition<?> def = userDefinition.findItemDefinition(itemPath);
            if (def != null) {
                this.displayNameKey = def.getDisplayName();
            } else {
                this.displayNameKey = itemPath.toString();
            }
        }



//            List<RoleAnalysisAttributeStatistics> attributeStatistics = analysis.getAttributeStatistics();
//            if (attributeStatistics != null && !attributeStatistics.isEmpty()) {
//                toSort.add(analysis);
//            }
//
//        toSort.sort((analysis1, analysis2) -> {
//            Double density1 = analysis1.getDensity();
//            Double density2 = analysis2.getDensity();
//            return Double.compare(density2, density1);
//        });
//
//        int totalBars = toSort.size();



//        if (roleAttributeAnalysisResult != null) {
//            List<RoleAnalysisAttributeAnalysis> attributeAnalysis = roleAttributeAnalysisResult.getAttributeAnalysis();
//            for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysis) {
////                        if (rolePath.contains(analysis.getItemPath().getItemPath())) {
//                analysisAttributeToDisplay.getAttributeAnalysis().add(analysis.clone());
////                        }
//            }
//        }
//
//        //TODO duplicate?
//        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = detectedPattern.getUserAttributeAnalysisResult();
//        if (userAttributeAnalysisResult != null) {
//            List<RoleAnalysisAttributeAnalysis> userAttributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();
//            for (RoleAnalysisAttributeAnalysis analysis : userAttributeAnalysis) {
////                        if (userPath.contains(analysis.getItemPath().getItemPath())) {
//                analysisAttributeToDisplay.getAttributeAnalysis().add(analysis.clone());
////                        }
//            }
//        }
//
//        return analysisAttributeToDisplay;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    private static @Nullable ItemPath resolveItemPath(@NotNull RoleAnalysisAttributeAnalysis analysis) {

        ItemPathType itemDescription = analysis.getItemPath();
        if (itemDescription == null) {
            return null;
        }
        return itemDescription.getItemPath();
    }

    public int getAttributeValuesSize() {
        return attributeValuesSize;
    }

    public Class<?> getType() {
        return type;
    }

    public List<RoleAnalysisAttributeStatistics> getAttributeStatistics() {
        return attributeStatistics;
    }

    //        String itemDescription = analysis.getItemPath();
//        if (itemDescription != null && !itemDescription.isEmpty()) {
//            itemDescription = Character.toUpperCase(itemDescription.charAt(0)) + itemDescription.substring(1);
//            localUserPath.add(itemDescription.toLowerCase());
//        }
//        return itemDescription;
//    }
}
