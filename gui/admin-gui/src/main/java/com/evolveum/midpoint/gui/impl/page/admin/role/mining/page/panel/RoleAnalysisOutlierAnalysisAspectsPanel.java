/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateRoleOutlierResultModel;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateUserOutlierResultModel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierHeaderResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierItemResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierResultPanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "outlierOverView", defaultContainerPath = "empty")
@PanelInstance(identifier = "outlierOverView",
        applicableForType = RoleAnalysisOutlierType.class,
        defaultPanel = true,
        display = @PanelDisplay(
                label = "RoleAnalysis.overview.panel",
                icon = GuiStyleConstants.CLASS_LINE_CHART_ICON,
                order = 20))
public class RoleAnalysisOutlierAnalysisAspectsPanel extends AbstractObjectMainPanel<RoleAnalysisOutlierType, ObjectDetailsModels<RoleAnalysisOutlierType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER_ITEMS = "header-items";


    public RoleAnalysisOutlierAnalysisAspectsPanel(String id, ObjectDetailsModels<RoleAnalysisOutlierType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected void initLayout(){

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        OutlierObjectModel outlierObjectModel;

        PageBase pageBase = getPageBase();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("loadOutlierDetails");
        RoleAnalysisOutlierType outlierObject = getObjectDetailsModels().getObjectType();
        ObjectReferenceType targetSessionRef = outlierObject.getTargetSessionRef();
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService
                .getSessionTypeObject(targetSessionRef.getOid(), task, task.getResult());
        if (sessionTypeObject == null) {
            Label label = new Label(ID_HEADER_ITEMS, "No session found");
            container.add(label);
            return;
        }
        RoleAnalysisSessionType sessionType = sessionTypeObject.asObjectable();
        RoleAnalysisProcessModeType processMode = sessionType.getAnalysisOption().getProcessMode();

        ObjectReferenceType targetClusterRef = outlierObject.getTargetClusterRef();
        PrismObject<RoleAnalysisClusterType> clusterTypeObject = roleAnalysisService
                .getClusterTypeObject(targetClusterRef.getOid(), task, task.getResult());
        if (clusterTypeObject == null) {
            Label label = new Label(ID_HEADER_ITEMS, "No cluster found");
            container.add(label);
            return;
        }
        RoleAnalysisClusterType cluster = clusterTypeObject.asObjectable();
        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            outlierObjectModel = generateUserOutlierResultModel(roleAnalysisService, outlierObject, task, task.getResult(), cluster);
        } else {
            outlierObjectModel = generateRoleOutlierResultModel(roleAnalysisService,outlierObject, task, task.getResult(), cluster);
        }

        if (outlierObjectModel == null) {
            Label label = new Label(ID_HEADER_ITEMS, "No outlier model found");
            container.add(label);
            return;
        }
        String outlierName = outlierObjectModel.getOutlierName();
        double outlierConfidence = outlierObjectModel.getOutlierConfidence();
        String outlierDescription = outlierObjectModel.getOutlierDescription();
        String timeCreated = outlierObjectModel.getTimeCreated();

        OutlierResultPanel detailsPanel = new OutlierResultPanel(
                ID_HEADER_ITEMS,
                Model.of("Analyzed members details panel")) {

            @Override
            public String getCardCssClass() {
                return "";
            }

            @Override
            public Component getCardHeaderBody(String componentId) {
                OutlierHeaderResultPanel components = new OutlierHeaderResultPanel(componentId, outlierName,
                        outlierDescription, String.valueOf(outlierConfidence), timeCreated);
                components.setOutputMarkupId(true);
                return components;
            }

            @Override
            public Component getCardBodyComponent(String componentId) {
                //TODO just for testing
                RepeatingView cardBodyComponent = (RepeatingView) super.getCardBodyComponent(componentId);
                outlierObjectModel.getOutlierItemModels()
                        .forEach(outlierItemModel
                                -> cardBodyComponent.add(
                                        new OutlierItemResultPanel(cardBodyComponent.newChildId(), outlierItemModel)));
                return cardBodyComponent;
            }

            @Override
            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                super.onClose(ajaxRequestTarget);
            }

        };
        detailsPanel.setOutputMarkupId(true);
        container.add(detailsPanel);
    }

}

