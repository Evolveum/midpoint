/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart;

import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import static com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure.extractAttributeAnalysis;

public class RoleAnalysisAttributeChartPopupPanel extends BasePanel<String> implements Popupable {

    private static final String ID_CHART_PANEL_ROLE = "panel";

    List<AttributeAnalysisStructure> attributeAnalysisStructureList;
    RoleAnalysisClusterType cluster;
    RoleAnalysisProcessModeType processMode;

    public RoleAnalysisAttributeChartPopupPanel(
            String id,
            IModel<String> messageModel,
            @NotNull RoleAnalysisClusterType cluster) {
        super(id, messageModel);
        this.cluster = cluster;
        this.attributeAnalysisStructureList = extractAttributeAnalysis(cluster);

        this.attributeAnalysisStructureList.sort((model1, model2) -> Double.compare(model2.getDensity(), model1.getDensity()));
    }

    public RoleAnalysisAttributeChartPopupPanel(
            String id,
            IModel<String> messageModel,
            @NotNull List<AttributeAnalysisStructure> attributeAnalysisStructureList,
            @NotNull RoleAnalysisProcessModeType processMode) {
        super(id, messageModel);
        this.processMode = processMode;
        this.attributeAnalysisStructureList = attributeAnalysisStructureList;
        this.attributeAnalysisStructureList.sort((model1, model2) -> Double.compare(model2.getDensity(), model1.getDensity()));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        RoleAnalysisAttributeChartPanel roleAnalysisRoleChartPanel = new RoleAnalysisAttributeChartPanel(
                ID_CHART_PANEL_ROLE, attributeAnalysisStructureList, cluster) {
            @Override
            public StringResourceModel getChartTitle() {
                return getPageBase().createStringResource("RoleAnalysisAttributeChartPopupPanel.roleChartTitle");
            }

            @Override
            public List<AttributeAnalysisStructure> getStackedNegativeValue() {
                return RoleAnalysisAttributeChartPopupPanel.this.getStackedNegativeValue();
            }

            @Override
            protected Set<String> getRolePathToMark() {
                return RoleAnalysisAttributeChartPopupPanel.this.getRolePathToMark();
            }

            @Override
            protected Set<String> getUserPathToMark() {
                return RoleAnalysisAttributeChartPopupPanel.this.getUserPathToMark();
            }

            @Override
            protected RoleAnalysisProcessModeType getProcessMode() {
                return RoleAnalysisAttributeChartPopupPanel.this.getProcessMode();
            }

            @Contract(pure = true)
            @Override
            public @NotNull String getColor() {
                return "#00a65a";
            }
        };
        roleAnalysisRoleChartPanel.setOutputMarkupId(true);
        add(roleAnalysisRoleChartPanel);
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return null;
    }

    protected Set<String> getRolePathToMark() {
        return null;
    }

    protected Set<String> getUserPathToMark() {
        return null;
    }

    public RoleAnalysisProcessModeType getProcessMode() {
        return processMode;
    }

    public void setProcessMode(RoleAnalysisProcessModeType processMode) {
        this.processMode = processMode;
    }

    public List<AttributeAnalysisStructure> getStackedNegativeValue() {
        return null;
    }

}
