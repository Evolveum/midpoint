/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serializable;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatistics;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class RoleAnalysisAttributeAnalysisDto implements Serializable {

    private String displayNameKey;
    private boolean selected = true;
    private int attributeValuesSize;
    private Class<?> type;
    private List<RoleAnalysisAttributeStatistics> attributeStatistics;

    private RoleAnalysisAttributeAnalysisDto() {

    }

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

        //TODO what to do whit this?

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
    }

    public static RoleAnalysisAttributeAnalysisDto forOverallResult(int attributeSize) {
        RoleAnalysisAttributeAnalysisDto overallAttribute = new RoleAnalysisAttributeAnalysisDto();
        overallAttribute.attributeValuesSize = attributeSize;
        overallAttribute.displayNameKey = "RoleAnalysisAttributePanel.title.overal.result";
        overallAttribute.selected = false;
        return overallAttribute;
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

}
