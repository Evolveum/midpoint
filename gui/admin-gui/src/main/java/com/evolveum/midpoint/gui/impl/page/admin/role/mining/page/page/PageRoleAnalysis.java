/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateRoleOutlierResultModel;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateUserOutlierResultModel;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierHeaderResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierItemResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierResultPanel;
import com.evolveum.midpoint.model.api.ModelService;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisInfoPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetailsPopup;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisInfoItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisSessionTileTable;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleAnalysis", matchUrlForSecurity = "/admin/roleAnalysis")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_ALL_URL,
                        label = "PageRoleAnalysis.auth.roleAnalysisAll.label",
                        description = "PageRoleAnalysis.auth.roleAnalysisAll.description")
        })

public class PageRoleAnalysis extends PageAdmin {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_INFO_FORM = "infoForm";
    private static final String ID_CHART_PANEL = "chartPanel";
    private static final String ID_TABLE = "table";

    public PageRoleAnalysis(PageParameters params) {
        super(params);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        Form<?> infoForm = new MidpointForm<>(ID_INFO_FORM);
        add(infoForm);

        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        if (!isNativeRepo()) {
            mainForm.add(new ErrorPanel(ID_TABLE, createStringResource("RoleAnalysis.menu.nonNativeRepositoryWarning")));
            infoForm.add(new EmptyPanel(ID_CHART_PANEL));
            return;
        }

        RoleAnalysisInfoPanel roleAnalysisInfoPanel = new RoleAnalysisInfoPanel(ID_CHART_PANEL) {
            @Override
            public void addPatternItems(RepeatingView repeatingView) {
                PageBase pageBase = getPageBase();
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                Task task = pageBase.createSimpleTask("loadRoleAnalysisInfo");
                OperationResult result = task.getResult();
                List<DetectedPattern> topPatters = roleAnalysisService.findTopPatters(task, result);
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
            public void addOutliersItems(RepeatingView repeatingView) {
                PageBase pageBase = getPageBase();
                ModelService modelService = pageBase.getModelService();
                Task task = pageBase.createSimpleTask("loadRoleAnalysisInfo");
                OperationResult result = task.getResult();
                SearchResultList<PrismObject<RoleAnalysisOutlierType>> searchResultList;
                try {
                    searchResultList = modelService
                            .searchObjects(RoleAnalysisOutlierType.class, null, null, task, result);
                } catch (SchemaException | ObjectNotFoundException | SecurityViolationException |
                        CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                    throw new RuntimeException(e);
                }

                if (searchResultList == null || searchResultList.isEmpty()) {
                    return;
                }

                for (int i = 0; i < searchResultList.size(); i++) {
                    PrismObject<RoleAnalysisOutlierType> outlierTypePrismObject = searchResultList.get(i);
                    RoleAnalysisOutlierType outlierObject = outlierTypePrismObject.asObjectable();
                    List<RoleAnalysisOutlierDescriptionType> outlierStatResult = outlierObject.getResult();
                    Double clusterConfidence = outlierObject.getClusterConfidence();
                    String formattedConfidence = String.format("%.2f", clusterConfidence);
                    String label;

                    ObjectReferenceType targetClusterRef = outlierObject.getTargetClusterRef();
                    PrismObject<RoleAnalysisClusterType> prismCluster = getRoleAnalysisService()
                            .getClusterTypeObject(targetClusterRef.getOid(), task, result);
                    String clusterName = "unknown";
                    if (prismCluster != null && prismCluster.getName() != null) {
                        clusterName = prismCluster.getName().getOrig();
                    }

                    if (outlierStatResult.size() > 1) {
                        label =  + outlierStatResult.size() + " anomalies "
                                + "with confidence of " + formattedConfidence + "% (" + clusterName.toLowerCase() + ").";
                    } else {
                        label = " 1 anomalies with confidence of " + formattedConfidence
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
                            assert sessionTypeObject != null;
                            RoleAnalysisSessionType sessionType = sessionTypeObject.asObjectable();
                            RoleAnalysisProcessModeType processMode = sessionType.getAnalysisOption().getProcessMode();

                            ObjectReferenceType targetClusterRef = outlierObject.getTargetClusterRef();
                            PrismObject<RoleAnalysisClusterType> clusterTypeObject = roleAnalysisService
                                    .getClusterTypeObject(targetClusterRef.getOid(), task, task.getResult());
                            assert clusterTypeObject != null;
                            RoleAnalysisClusterType cluster = clusterTypeObject.asObjectable();
                            if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
                                outlierObjectModel = generateUserOutlierResultModel(
                                        roleAnalysisService, outlierObject, task, task.getResult(), cluster);
                            } else {
                                outlierObjectModel = generateRoleOutlierResultModel(
                                        roleAnalysisService, outlierObject, task, task.getResult(), cluster);
                            }

                            assert outlierObjectModel != null;
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
        };
        roleAnalysisInfoPanel.setOutputMarkupId(true);
        infoForm.add(roleAnalysisInfoPanel);

        RoleAnalysisSessionTileTable roleAnalysisSessionTileTable = new RoleAnalysisSessionTileTable(ID_TABLE, (PageBase) getPage());
        roleAnalysisSessionTileTable.setOutputMarkupId(true);
        mainForm.add(roleAnalysisSessionTileTable);

    }

}
