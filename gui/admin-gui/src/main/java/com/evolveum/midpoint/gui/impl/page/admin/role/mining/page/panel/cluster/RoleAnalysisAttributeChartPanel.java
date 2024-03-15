/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import static com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributePathResolver.getRolePathLabels;
import static com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributePathResolver.getUserPathLabels;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionPatternType;

public class RoleAnalysisAttributeChartPanel extends BasePanel<String> {

    private static final String ID_CHART_PANEL_USER = "chartPanelUser";
    private static final String ID_CHART_PANEL_ROLE = "chartPanelRole";

    List<AttributeAnalysisStructure> attributeAnalysisUserStructureList = new ArrayList<>();
    List<AttributeAnalysisStructure> attributeAnalysisRoleStructureList = new ArrayList<>();

    LoadableDetachableModel<RoleAnalysisAttributeAnalysisResult> roleAnalysisResult;

    LoadableDetachableModel<ObjectDetailsModels<RoleAnalysisClusterType>> clusterModel;
    DetectedPattern detectedPattern;

    public RoleAnalysisAttributeChartPanel(String id,
            RoleAnalysisAttributeAnalysisResult roleAnalysisResult,
            RoleAnalysisAttributeAnalysisResult userAnalysisResult,
            LoadableDetachableModel<ObjectDetailsModels<RoleAnalysisClusterType>> clusterModel,
            DetectedPattern detectedPattern) {
        super(id);

        this.clusterModel = clusterModel;
        this.detectedPattern = detectedPattern;
        this.roleAnalysisResult = new LoadableDetachableModel<>() {
            @Override
            protected RoleAnalysisAttributeAnalysisResult load() {
                return roleAnalysisResult;
            }
        };

        Set<String> rolePathLabels = getRolePathLabels();
        if (roleAnalysisResult != null) {
            List<RoleAnalysisAttributeAnalysis> attributeAnalysis = roleAnalysisResult.getAttributeAnalysis();
            for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysis) {
                AttributeAnalysisStructure attributeAnalysisStructure = new AttributeAnalysisStructure(analysis);
                rolePathLabels.remove(analysis.getItemPath());
                attributeAnalysisRoleStructureList.add(attributeAnalysisStructure);
            }

            for (String path : rolePathLabels) {
                RoleAnalysisAttributeAnalysis analysis = new RoleAnalysisAttributeAnalysis();
                analysis.setItemPath(path);
                analysis.setDensity(0.0);
                AttributeAnalysisStructure attributeAnalysisStructure = new AttributeAnalysisStructure(analysis);
                attributeAnalysisRoleStructureList.add(attributeAnalysisStructure);
            }
        }

        Set<String> userPathLabels = getUserPathLabels();

        if (userAnalysisResult != null) {
            List<RoleAnalysisAttributeAnalysis> attributeAnalysis = userAnalysisResult.getAttributeAnalysis();
            for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysis) {
                userPathLabels.remove(analysis.getItemPath());
                AttributeAnalysisStructure attributeAnalysisStructure = new AttributeAnalysisStructure(analysis);
                attributeAnalysisUserStructureList.add(attributeAnalysisStructure);
            }

            for (String path : userPathLabels) {
                RoleAnalysisAttributeAnalysis analysis = new RoleAnalysisAttributeAnalysis();
                analysis.setItemPath(path);
                analysis.setDensity(0.0);
                AttributeAnalysisStructure attributeAnalysisStructure = new AttributeAnalysisStructure(analysis);
                attributeAnalysisUserStructureList.add(attributeAnalysisStructure);
            }
        }

        IModel<? extends PrismContainerWrapper<RoleAnalysisAttributeAnalysisResult>> userContainerFormModel = getUserContainerFormModel();
        if (userContainerFormModel != null) {
            userContainerFormModel.getObject().setExpanded(false);
            PrismContainerPanel<RoleAnalysisDetectionPatternType, ?> containerPanel = new PrismContainerPanel(
                    "containerPanelUser", userContainerFormModel, null);
            containerPanel.setOutputMarkupId(true);
            add(containerPanel);
        } else {
            WebMarkupContainer container = new WebMarkupContainer("containerPanelUser");
            container.setOutputMarkupId(true);
            add(container);
        }

        IModel<? extends PrismContainerWrapper<RoleAnalysisAttributeAnalysisResult>> rolesContainerFormModel = getRolesContainerFormModel();
        if (rolesContainerFormModel != null) {
            rolesContainerFormModel.getObject().setExpanded(false);
            PrismContainerPanel<RoleAnalysisDetectionPatternType, ?> containerPanelRole = new PrismContainerPanel(
                    "containerPanelRole", rolesContainerFormModel, null);
            containerPanelRole.setOutputMarkupId(true);
            add(containerPanelRole);
        } else {
            WebMarkupContainer container = new WebMarkupContainer("containerPanelRole");
            container.setOutputMarkupId(true);
            add(container);
        }

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisAttributeChartPanel roleAnalysisUserChartPanel = new com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisAttributeChartPanel(
                ID_CHART_PANEL_USER, attributeAnalysisUserStructureList, clusterModel.getObject().getObjectType()) {
            @Override
            public StringResourceModel getChartTitle() {
                return getPageBase().createStringResource("RoleAnalysisAttributeChartPopupPanel.userChartTitle");
            }

            @Override
            public String getColor() {
                return "#dd4b39";
            }
        };
        roleAnalysisUserChartPanel.setOutputMarkupId(true);
        add(roleAnalysisUserChartPanel);

        com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisAttributeChartPanel roleAnalysisRoleChartPanel = new com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisAttributeChartPanel(
                ID_CHART_PANEL_ROLE, attributeAnalysisRoleStructureList,clusterModel.getObject().getObjectType()) {
            @Override
            public StringResourceModel getChartTitle() {
                return getPageBase().createStringResource("RoleAnalysisAttributeChartPopupPanel.roleChartTitle");
            }

            @Override
            public String getColor() {
                return "#00a65a";
            }
        };
        roleAnalysisRoleChartPanel.setOutputMarkupId(true);
        add(roleAnalysisRoleChartPanel);
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    protected IModel<? extends PrismContainerWrapper<RoleAnalysisAttributeAnalysisResult>> getUserContainerFormModel() {

        if (detectedPattern == null || detectedPattern.getId() == null) {
            return null;
        }

        if (clusterModel == null || clusterModel.getObject() == null) {
            return null;
        }

        LoadableModel<PrismObjectWrapper<RoleAnalysisClusterType>> objectWrapperModel = clusterModel.getObject().getObjectWrapperModel();

        PrismContainerWrapperModel<RoleAnalysisClusterType, Containerable> pattern = PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel,
                ItemPath.create(RoleAnalysisClusterType.F_DETECTED_PATTERN));

        PrismContainerValueWrapper<Containerable> containerablePrismContainerValueWrapper = null;

        List<PrismContainerValueWrapper<Containerable>> values = pattern.getObject().getValues();
        for (PrismContainerValueWrapper<Containerable> valueWrapper : values) {
            Containerable realValue = valueWrapper.getRealValue();
            if (realValue instanceof RoleAnalysisDetectionPatternType) {
                RoleAnalysisDetectionPatternType detectionPatternType = (RoleAnalysisDetectionPatternType) realValue;
                if (detectionPatternType.getId() == null) {
                    continue;
                }
                if (detectionPatternType.getId().equals(detectedPattern.getId())) {
                    containerablePrismContainerValueWrapper = valueWrapper;
                    break;
                }
            }
        }

        try {
            PrismContainerWrapper<RoleAnalysisAttributeAnalysisResult> container = containerablePrismContainerValueWrapper != null
                    ? containerablePrismContainerValueWrapper
                    .findContainer(RoleAnalysisDetectionPatternType.F_USER_ATTRIBUTE_ANALYSIS_RESULT)
                    : null;
            return Model.of(container);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

    }

    //TODO brutal ugly just for fast solution
    protected IModel<? extends PrismContainerWrapper<RoleAnalysisAttributeAnalysisResult>> getRolesContainerFormModel() {

        if (detectedPattern == null || detectedPattern.getId() == null) {
            return null;
        }

        if (clusterModel == null || clusterModel.getObject() == null) {
            return null;
        }
        LoadableModel<PrismObjectWrapper<RoleAnalysisClusterType>> objectWrapperModel = clusterModel.getObject().getObjectWrapperModel();

        PrismContainerWrapperModel<RoleAnalysisClusterType, Containerable> pattern = PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel,
                ItemPath.create(RoleAnalysisClusterType.F_DETECTED_PATTERN));

        PrismContainerWrapper<Containerable> object = pattern.getObject();
        if (object == null) {
            return null;
        }
        List<PrismContainerValueWrapper<Containerable>> values = object.getValues();
        if (values == null || values.isEmpty()) {
            return null;
        }

        PrismContainerValueWrapper<Containerable> containerablePrismContainerValueWrapper = values.get(0);

        try {
            PrismContainerWrapper<RoleAnalysisAttributeAnalysisResult> container = containerablePrismContainerValueWrapper
                    .findContainer(RoleAnalysisDetectionPatternType.F_ROLE_ATTRIBUTE_ANALYSIS_RESULT);
            return Model.of(container);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

    }
}
