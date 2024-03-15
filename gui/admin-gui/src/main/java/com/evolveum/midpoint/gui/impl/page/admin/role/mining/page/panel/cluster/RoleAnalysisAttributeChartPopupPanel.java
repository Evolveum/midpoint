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

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
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
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisAttributeChartPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

public class RoleAnalysisAttributeChartPopupPanel extends BasePanel<String> implements Popupable {

    private static final String ID_CHART_PANEL_ROLE = "chartPanel";

    List<AttributeAnalysisStructure> attributeAnalysisUserStructureList = new ArrayList<>();
    List<AttributeAnalysisStructure> attributeAnalysisRoleStructureList = new ArrayList<>();

    LoadableDetachableModel<RoleAnalysisAttributeAnalysisResult> roleAnalysisResult;

    LoadableDetachableModel<ObjectDetailsModels<RoleAnalysisClusterType>> clusterModel;
    DetectedPattern detectedPattern;

    public RoleAnalysisAttributeChartPopupPanel(String id, IModel<String> messageModel,
            RoleAnalysisAttributeAnalysisResult roleAnalysisResult,
            RoleAnalysisAttributeAnalysisResult userAnalysisResult,
            LoadableDetachableModel<ObjectDetailsModels<RoleAnalysisClusterType>> clusterModel, DetectedPattern detectedPattern) {
        super(id, messageModel);

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
                attributeAnalysisStructure.setComplexType(RoleType.COMPLEX_TYPE);
                attributeAnalysisRoleStructureList.add(attributeAnalysisStructure);
            }
        }

        Set<String> userPathLabels = getUserPathLabels();

        if (userAnalysisResult != null) {
            List<RoleAnalysisAttributeAnalysis> attributeAnalysis = userAnalysisResult.getAttributeAnalysis();
            for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysis) {
                userPathLabels.remove(analysis.getItemPath());
                AttributeAnalysisStructure attributeAnalysisStructure = new AttributeAnalysisStructure(analysis);
                attributeAnalysisStructure.setComplexType(UserType.COMPLEX_TYPE);
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

        List<AttributeAnalysisStructure> attributeAnalysisStructures = new ArrayList<>();
        attributeAnalysisStructures.addAll(attributeAnalysisRoleStructureList);
        attributeAnalysisStructures.addAll(attributeAnalysisUserStructureList);
        RoleAnalysisAttributeChartPanel roleAnalysisRoleChartPanel = new RoleAnalysisAttributeChartPanel(
                ID_CHART_PANEL_ROLE, attributeAnalysisStructures, clusterModel.getObject().getObjectType()) {
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
