/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.InfoBoxModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.*;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "clusterOverview", defaultContainerPath = "empty")
@PanelInstance(identifier = "clusterOverview",
        applicableForType = RoleAnalysisClusterType.class,
        defaultPanel = true,
        display = @PanelDisplay(
                label = "RoleAnalysis.overview.panel",
                icon = GuiStyleConstants.CLASS_LINE_CHART_ICON,
                order = 10))
public class RoleAnalysisClusterAnalysisAspectsPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER_ITEMS = "header-items";
    private static final String ID_PANEL = "panelId";
    private static final String ID_PATTERNS = "patterns";

    public RoleAnalysisClusterAnalysisAspectsPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        initInfoPatternPanel(container);

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
                    Model.of("Cluster attributes analysis"), roleAttributeAnalysisResult, userAttributeAnalysisResult) {
                @Override
                protected @NotNull String getChartContainerStyle() {
                    return "height:25vh;";
                }

//                @Contract(pure = true)
//                @Override
//                protected @NotNull String getCssClassForCardContainer() {
//                    return "";
//                }
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

        //TODO check why is there empty ObjectReferenceType
        if (resolvedPattern != null && resolvedPattern.size() == 1) {
            ObjectReferenceType objectReferenceType = resolvedPattern.get(0);
            if (objectReferenceType == null || objectReferenceType.getOid() == null) {
                resolvedPatternCount = "0";
            }
        }

        List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();
        String candidateRolesCount = "0";
        if (candidateRoles != null) {
            candidateRolesCount = String.valueOf(candidateRoles.size());
        }

        InfoBoxModel infoBoxResolvedPatterns = new InfoBoxModel(GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON + " text-white",
                "Optimized roles",
                resolvedPatternCount,
                100,
                "Number of optimized roles for cluster");

        RoleAnalysisInfoBox resolvedPatternLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxResolvedPatterns)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }

        };
        resolvedPatternLabel.add(AttributeModifier.replace("class", "col-md-6"));
        resolvedPatternLabel.setOutputMarkupId(true);
        headerItems.add(resolvedPatternLabel);

        InfoBoxModel infoBoxCandidateRoles = new InfoBoxModel(GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " text-white",
                "Candidate roles",
                candidateRolesCount,
                100,
                "Number of candidate roles for cluster");

        RoleAnalysisInfoBox candidateRolesLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxCandidateRoles)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        candidateRolesLabel.add(AttributeModifier.replace("class", "col-md-6"));
        candidateRolesLabel.setOutputMarkupId(true);
        headerItems.add(candidateRolesLabel);

        InfoBoxModel infoBoxRoles = new InfoBoxModel(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON + " text-white",
                "Roles",
                String.valueOf(clusterStatistics.getRolesCount()),
                100,
                "Number of roles in the cluster");

        RoleAnalysisInfoBox rolesLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxRoles)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        rolesLabel.add(AttributeModifier.replace("class", "col-md-6"));
        rolesLabel.setOutputMarkupId(true);
        headerItems.add(rolesLabel);

        InfoBoxModel infoBoxUsers = new InfoBoxModel(GuiStyleConstants.CLASS_OBJECT_USER_ICON + " text-white",
                "Users",
                String.valueOf(clusterStatistics.getUsersCount()),
                100,
                "Number of users in the cluster");

        RoleAnalysisInfoBox usersLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxUsers)) {
            @Override
            protected String getInfoBoxCssClass() {
                return "bg-primary";
            }
        };
        usersLabel.add(AttributeModifier.replace("class", "col-md-6"));
        usersLabel.setOutputMarkupId(true);
        headerItems.add(usersLabel);

    }

    private void initInfoPatternPanel(WebMarkupContainer container) {
        RoleAnalysisItemPanel roleAnalysisInfoPatternPanel = new RoleAnalysisItemPanel(ID_PATTERNS,
                Model.of("Top role suggestions for cluster")) {
            @Override
            protected void addItem(RepeatingView repeatingView) {
                List<DetectedPattern> topPatters = transformDefaultPattern(getObjectDetailsModels().getObjectType());
                for (int i = 0; i < topPatters.size(); i++) {
                    DetectedPattern pattern = topPatters.get(i);
                    double reductionFactorConfidence = pattern.getMetric();
                    String formattedReductionFactorConfidence = String.format("%.0f", reductionFactorConfidence);
                    double itemsConfidence = pattern.getItemsConfidence();
                    String formattedItemConfidence = String.format("%.1f", itemsConfidence);
                    String label = "Detected a potential reduction of " +
                            formattedReductionFactorConfidence +
                            "x relationships with a confidence of  " +
                            formattedItemConfidence + "%";
                    int finalI = i;
                    repeatingView.add(new RoleAnalysisInfoItem(repeatingView.newChildId(), Model.of(label)) {

                        @Override
                        protected String getIconBoxText() {
//                            return "#" + (finalI + 1);
                            return null;
                        }

                        @Override
                        protected String getIconBoxIconStyle() {
                            return super.getIconBoxIconStyle();
                        }

                        @Override
                        protected String getIconContainerCssClass() {
                            return "btn btn-outline-dark";
                        }

                        @Override
                        protected void addDescriptionComponents() {
                            appendText("A potential reduction has been detected. ");
                            WebMarkupContainer container = new WebMarkupContainer(getRepeatedView().newChildId());
                            container.add(AttributeAppender.append("class", "d-flex"));
                            appendComponent(container);
                            appendText(" Involves ");
                            appendIcon("fe fe-assignment", "color: red;");
                            appendText(" " + formattedReductionFactorConfidence + " relations ");
                            appendText("and ");
                            appendIcon("fa fa-leaf", "color: green");
                            appendText(" " + formattedItemConfidence + "% confidence.");
                        }

                        @Override
                        protected IModel<String> getDescriptionModel() {
                            String description = "A potential reduction has been detected. The reduction involves " +
                                    formattedReductionFactorConfidence + " assignments and is associated with "
                                    + "an attribute confidence of " +
                                    formattedItemConfidence + "%.";
                            return Model.of(description);
                        }

                        @Override
                        protected IModel<String> getLinkModel() {
                            IModel<String> linkModel = super.getLinkModel();
                            return Model.of(linkModel.getObject() + " role suggestion #" + (finalI + 1));
                        }

                        @Override
                        protected void onClickLinkPerform(AjaxRequestTarget target) {
                            PageParameters parameters = new PageParameters();
                            String clusterOid = pattern.getClusterRef().getOid();
                            parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
                            parameters.add("panelId", "clusterDetails");
                            parameters.add(PARAM_DETECTED_PATER_ID, pattern.getId());
                            StringValue fullTableSetting = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
                            if (fullTableSetting != null && fullTableSetting.toString() != null) {
                                parameters.add(PARAM_TABLE_SETTING, fullTableSetting.toString());
                            }

                            Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                    .getObjectDetailsPage(RoleAnalysisClusterType.class);
                            getPageBase().navigateToNext(detailsPageClass, parameters);
                        }

                        @Override
                        protected void onClickIconPerform(AjaxRequestTarget target) {
                            RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
                                    ((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of(pattern));
                            ((PageBase) getPage()).showMainPopup(component, target);
                        }
                    });
                }
            }

            @Override
            public String getCardBodyCssClass() {
                return " overflow-auto ";
            }

            @Override
            public String replaceCardCssClass() {
                return "card p-0";
            }

            @Override
            public String getCardBodyStyle() {
                return " height:58vh;";
            }

            @Override
            public String replaceBtnToolCssClass() {
                return " position-relative  ml-auto btn btn-primary btn-sm";
            }
        };
        roleAnalysisInfoPatternPanel.setOutputMarkupId(true);
        container.add(roleAnalysisInfoPatternPanel);
    }
}

