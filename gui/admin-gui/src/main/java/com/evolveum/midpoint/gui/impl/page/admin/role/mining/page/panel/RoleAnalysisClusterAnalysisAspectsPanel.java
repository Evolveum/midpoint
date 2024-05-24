/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.InfoBoxModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisInfoBox;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributePanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@PanelType(name = "clusterOverview", defaultContainerPath = "empty")
@PanelInstance(identifier = "clusterOverview",
        applicableForType = RoleAnalysisClusterType.class,
        display = @PanelDisplay(
                label = "PageRoleAnalysis.analysis.aspects.panel",
                icon = GuiStyleConstants.CLASS_LINE_CHART_ICON,
                order = 50))
public class RoleAnalysisClusterAnalysisAspectsPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER_ITEMS = "header-items";
    private static final String ID_PANEL = "panelId";

    public RoleAnalysisClusterAnalysisAspectsPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        ObjectDetailsModels<RoleAnalysisClusterType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisClusterType cluster = objectDetailsModels.getObjectType();
        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        if (clusterStatistics != null) {
            RepeatingView headerItems = new RepeatingView(ID_HEADER_ITEMS);
            headerItems.setOutputMarkupId(true);
            container.add(headerItems);
            initHeaderPanel(headerItems);

            RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = clusterStatistics.getUserAttributeAnalysisResult();
            RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = clusterStatistics.getRoleAttributeAnalysisResult();
            RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(ID_PANEL,
                    Model.of("Role analysis attribute panel"), roleAttributeAnalysisResult, userAttributeAnalysisResult){
                @Override
                protected @NotNull String getChartContainerStyle() {
                    return "height:25vh;";
                }
            };
            roleAnalysisAttributePanel.setOutputMarkupId(true);
            container.add(roleAnalysisAttributePanel);
        } else {
            Label label = new Label(ID_PANEL, "No data available");
            label.setOutputMarkupId(true);
            container.add(label);

            WebMarkupContainer headerItems = new WebMarkupContainer(ID_HEADER_ITEMS);
            headerItems.setOutputMarkupId(true);
            container.add(headerItems);
        }
    }

    private void initHeaderPanel(RepeatingView headerItems) {
        ObjectDetailsModels<RoleAnalysisClusterType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisClusterType cluster = objectDetailsModels.getObjectType();
        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
        List<ObjectReferenceType> resolvedPattern = cluster.getResolvedPattern();
        String resolvedPatternCount = "0";
        if (resolvedPattern != null) {
            resolvedPatternCount = String.valueOf(resolvedPattern.size());
        }
        List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();
        String candidateRolesCount = "0";
        if (candidateRoles != null) {
            candidateRolesCount = String.valueOf(candidateRoles.size());
        }

        InfoBoxModel infoBoxResolvedPatterns = new InfoBoxModel(GuiStyleConstants.ARROW_LONG_DOWN + " text-dark",
                "Optimized roles",
                resolvedPatternCount,
                100,
                "Number of optimized roles for cluster");

        RoleAnalysisInfoBox resolvedPatternLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxResolvedPatterns)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-light";
            }

        };
        resolvedPatternLabel.add(AttributeModifier.replace("class", "col-md-6"));
        resolvedPatternLabel.setOutputMarkupId(true);
        headerItems.add(resolvedPatternLabel);

        InfoBoxModel infoBoxCandidateRoles = new InfoBoxModel(GuiStyleConstants.ARROW_LONG_DOWN + " text-dark",
                "Candidate roles",
                candidateRolesCount,
                100,
                "Number of candidate roles for cluster");

        RoleAnalysisInfoBox candidateRolesLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxCandidateRoles)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-light";
            }
        };
        candidateRolesLabel.add(AttributeModifier.replace("class", "col-md-6"));
        candidateRolesLabel.setOutputMarkupId(true);
        headerItems.add(candidateRolesLabel);

        InfoBoxModel infoBoxRoles = new InfoBoxModel(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON + " text-dark",
                "Roles",
                String.valueOf(clusterStatistics.getRolesCount()),
                100,
                "Number of roles in the cluster");

        RoleAnalysisInfoBox rolesLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxRoles)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-light";
            }
        };
        rolesLabel.add(AttributeModifier.replace("class", "col-md-6"));
        rolesLabel.setOutputMarkupId(true);
        headerItems.add(rolesLabel);

        InfoBoxModel infoBoxUsers = new InfoBoxModel(GuiStyleConstants.CLASS_OBJECT_USER_ICON + " text-dark",
                "Users",
                String.valueOf(clusterStatistics.getUsersCount()),
                100,
                "Number of users in the cluster");

        RoleAnalysisInfoBox usersLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxUsers)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-light";
            }
        };
        usersLabel.add(AttributeModifier.replace("class", "col-md-6"));
        usersLabel.setOutputMarkupId(true);
        headerItems.add(usersLabel);

    }
}

