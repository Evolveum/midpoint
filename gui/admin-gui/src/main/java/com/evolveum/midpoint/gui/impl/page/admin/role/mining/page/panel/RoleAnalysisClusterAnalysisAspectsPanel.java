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
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateRoleOutlierResultModel;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateUserOutlierResultModel;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
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
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierHeaderResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierItemResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.InfoBoxModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.*;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
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

        ObjectDetailsModels<RoleAnalysisClusterType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisClusterType cluster = objectDetailsModels.getObjectType();
        ObjectReferenceType targetSessionRef = cluster.getRoleAnalysisSessionRef();
        PageBase pageBase = RoleAnalysisClusterAnalysisAspectsPanel.this.getPageBase();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("Load session type object");

        RepeatingView headerItems = new RepeatingView(ID_HEADER_ITEMS);
        headerItems.setOutputMarkupId(true);
        container.add(headerItems);

        PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService
                .getSessionTypeObject(targetSessionRef.getOid(), task, task.getResult());
        if (sessionTypeObject == null) {
            Label label = new Label(ID_PANEL, "No session found");
            container.add(label);
            EmptyPanel emptyPanel = new EmptyPanel(ID_PATTERNS);
            container.add(emptyPanel);
            return;
        }

        RoleAnalysisSessionType session = sessionTypeObject.asObjectable();
        RoleAnalysisCategoryType analysisCategory = session.getAnalysisOption().getAnalysisCategory();

        if (RoleAnalysisCategoryType.OUTLIERS.equals(analysisCategory)) {
            initInfoOutlierPanel(container);
            initOutlierAnalysisHeaderPanel(headerItems);
        } else {
            initInfoPatternPanel(container);
            initRoleMiningHeaderPanel(headerItems);
        }

        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        if (clusterStatistics != null) {
            RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = clusterStatistics.getUserAttributeAnalysisResult();
            RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = clusterStatistics.getRoleAttributeAnalysisResult();
            RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(ID_PANEL,
                    Model.of("Cluster attributes analysis"), roleAttributeAnalysisResult, userAttributeAnalysisResult) {
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
        }
    }

    private void initRoleMiningHeaderPanel(RepeatingView headerItems) {
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

    private void initOutlierAnalysisHeaderPanel(RepeatingView headerItems) {
        ObjectDetailsModels<RoleAnalysisClusterType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisClusterType cluster = objectDetailsModels.getObjectType();
        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        InfoBoxModel infoBoxResolvedPatterns = new InfoBoxModel(GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON + " text-white",
                "User outliers",
                String.valueOf(outliersCount),
                100,
                "Number of user outlier for cluster");

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
                "Assignments anomaly",
                String.valueOf(anomalyAssignmentCount),
                100,
                "Number of assignment anomaly for cluster");

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
                Model.of("Cluster role suggestions")) {
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
                            WebMarkupContainer container = new WebMarkupContainer(getRepeatedView().newChildId());
                            container.add(AttributeAppender.append("class", "d-flex"));
                            appendComponent(container);
                            appendText(" Involves ");
                            appendIcon("fe fe-assignment", "color: red;");
                            appendText(" " + formattedReductionFactorConfidence + " relations ");
                            appendText("with ");
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
                return " height:34vh;";
            }

            @Override
            public String replaceBtnToolCssClass() {
                return " position-relative  ml-auto btn btn-primary btn-sm";
            }
        };
        roleAnalysisInfoPatternPanel.setOutputMarkupId(true);
        container.add(roleAnalysisInfoPatternPanel);
    }

    int outliersCount = 0;
    int anomalyAssignmentCount = 0;

    private void initInfoOutlierPanel(WebMarkupContainer container) {
        RoleAnalysisItemPanel roleAnalysisInfoPatternPanel = new RoleAnalysisItemPanel(ID_PATTERNS,
                Model.of("Discovered cluster outliers")) {
            @Override
            protected void addItem(RepeatingView repeatingView) {
                RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
                PageBase pageBase = RoleAnalysisClusterAnalysisAspectsPanel.this.getPageBase();
                ModelService modelService = pageBase.getModelService();
                Task task = pageBase.createSimpleTask("loadRoleAnalysisInfo");
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                OperationResult result = task.getResult();
                //TODO replace after db schema change
//                SearchResultList<PrismObject<RoleAnalysisOutlierType>> searchResultList;
//                try {
//                    searchResultList = modelService
//                            .searchObjects(RoleAnalysisOutlierType.class, getQuery(cluster), null, task, result);
//                } catch (SchemaException | ObjectNotFoundException | SecurityViolationException |
//                        CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
//                    throw new RuntimeException(e);
//                }


                //TODO remove hack after db schema change
                List<PrismObject<RoleAnalysisOutlierType>> searchResultList = new ArrayList<>();
                String clusterOid = cluster.getOid();
                ResultHandler<RoleAnalysisOutlierType> resultHandler = (outlier, lResult) -> {

                    RoleAnalysisOutlierType outlierObject = outlier.asObjectable();
                    ObjectReferenceType targetClusterRef = outlierObject.getTargetClusterRef();
                    String oid = targetClusterRef.getOid();
                    if (clusterOid.equals(oid)) {
                        searchResultList.add(outlier);
                    }
                    return true;
                };

                try {
                    modelService.searchObjectsIterative(RoleAnalysisOutlierType.class, null, resultHandler,
                            null, task, result);
                } catch (Exception ex) {
                    throw new RuntimeException("Couldn't search outliers", ex);
                }


                if (searchResultList == null || searchResultList.isEmpty()) {
                    return;
                }

                outliersCount = searchResultList.size();
                for (int i = 0; i < searchResultList.size(); i++) {
                    PrismObject<RoleAnalysisOutlierType> outlierTypePrismObject = searchResultList.get(i);
                    RoleAnalysisOutlierType outlierObject = outlierTypePrismObject.asObjectable();
                    List<RoleAnalysisOutlierDescriptionType> outlierStatResult = outlierObject.getResult();
                    anomalyAssignmentCount = outlierStatResult.size();
                    Double clusterConfidence = outlierObject.getClusterConfidence();
                    String formattedConfidence = String.format("%.2f", clusterConfidence);
                    String label;

                    ObjectReferenceType targetClusterRef = outlierObject.getTargetClusterRef();
                    PrismObject<RoleAnalysisClusterType> prismCluster = roleAnalysisService
                            .getClusterTypeObject(targetClusterRef.getOid(), task, result);
                    String clusterName = "unknown";
                    if (prismCluster != null && prismCluster.getName() != null) {
                        clusterName = prismCluster.getName().getOrig();
                    }

                    if (outlierStatResult.size() > 1) {
                        label =  outlierStatResult.size() + " anomalies "
                                + "with confidence of " + formattedConfidence + "% (" + clusterName.toLowerCase() + ").";
                    } else {
                        label = "1 anomalies with confidence of " + formattedConfidence
                                + "% (" + clusterName.toLowerCase() + ").";
                    }

                    int finalI = i;
                    String finalLabel = label;
                    repeatingView.add(new RoleAnalysisInfoItem(repeatingView.newChildId(), Model.of(finalLabel)) {

                        @Override
                        protected String getIconBoxText() {
//                            return "#" + (finalI + 1);
                            return null;
                        }

                        @Override
                        protected String getIconClass() {
                            return "fa-2x " + GuiStyleConstants.CLASS_OUTLIER_ICON;
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
                            appendText(finalLabel);
                        }

                        @Override
                        protected IModel<String> getDescriptionModel() {
                            return Model.of(finalLabel);
                        }

                        @Override
                        protected IModel<String> getLinkModel() {
                            IModel<String> linkModel = super.getLinkModel();
                            return Model.of(linkModel.getObject() + " outlier #" + (finalI + 1));
                        }

                        @Override
                        protected void onClickLinkPerform(AjaxRequestTarget target) {
                            PageParameters parameters = new PageParameters();
                            String outlierOid = outlierObject.getOid();
                            parameters.add(OnePageParameterEncoder.PARAMETER, outlierOid);
                            StringValue fullTableSetting = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
                            if (fullTableSetting != null && fullTableSetting.toString() != null) {
                                parameters.add(PARAM_TABLE_SETTING, fullTableSetting.toString());
                            }

                            Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                    .getObjectDetailsPage(RoleAnalysisOutlierType.class);
                            getPageBase().navigateToNext(detailsPageClass, parameters);

                        }

                        @Override
                        protected void onClickIconPerform(AjaxRequestTarget target) {
                            OutlierObjectModel outlierObjectModel;

                            PageBase pageBase = getPageBase();
                            RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                            Task task = pageBase.createSimpleTask("loadOutlierDetails");
                            ObjectReferenceType targetSessionRef = outlierObject.getTargetSessionRef();
                            PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService
                                    .getSessionTypeObject(targetSessionRef.getOid(), task, task.getResult());
                            if (sessionTypeObject == null) {
                                return;
                            }
                            RoleAnalysisSessionType sessionType = sessionTypeObject.asObjectable();
                            RoleAnalysisProcessModeType processMode = sessionType.getAnalysisOption().getProcessMode();

                            ObjectReferenceType targetClusterRef = outlierObject.getTargetClusterRef();
                            PrismObject<RoleAnalysisClusterType> clusterTypeObject = roleAnalysisService
                                    .getClusterTypeObject(targetClusterRef.getOid(), task, task.getResult());
                            if (clusterTypeObject == null) {
                                return;
                            }
                            RoleAnalysisClusterType cluster = clusterTypeObject.asObjectable();
                            if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
                                outlierObjectModel = generateUserOutlierResultModel(
                                        roleAnalysisService, outlierObject, task, task.getResult(), cluster);
                            } else {
                                outlierObjectModel = generateRoleOutlierResultModel(
                                        roleAnalysisService, outlierObject, task, task.getResult(), cluster);
                            }

                            if (outlierObjectModel == null) {
                                return;
                            }
                            String outlierName = outlierObjectModel.getOutlierName();
                            double outlierConfidence = outlierObjectModel.getOutlierConfidence();
                            String outlierDescription = outlierObjectModel.getOutlierDescription();
                            String timeCreated = outlierObjectModel.getTimeCreated();

                            OutlierResultPanel detailsPanel = new OutlierResultPanel(
                                    ((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of("Outlier details")) {

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
                            ((PageBase) getPage()).showMainPopup(detailsPanel, target);
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
                return " height:34vh;";
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

