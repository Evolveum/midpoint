/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisInfoPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisPartitionOverviewPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.model.IdentifyWidgetItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetailsPopup;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisSessionTileTable;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DetectedAnomalyResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierPartitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

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

    public List<RoleAnalysisOutlierType> getTopFiveOutliers(@NotNull List<PrismObject<RoleAnalysisOutlierType>> searchResultList) {
        List<RoleAnalysisOutlierType> outlierList = new ArrayList<>();
        for (PrismObject<RoleAnalysisOutlierType> roleAnalysisOutlierTypePrismObject : searchResultList) {
            RoleAnalysisOutlierType roleAnalysisOutlierType = roleAnalysisOutlierTypePrismObject.asObjectable();
            outlierList.add(roleAnalysisOutlierType);
        }

        List<RoleAnalysisOutlierType> sortedOutliers = outlierList.stream()
                .sorted(Comparator.comparingDouble(RoleAnalysisOutlierType::getOverallConfidence).reversed())
                .toList();

        return sortedOutliers.stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    public List<DetectedPattern> getTopFivePatterns(@NotNull PageBase pageBase) {
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("loadRoleAnalysisInfo");
        OperationResult result = task.getResult();
        List<DetectedPattern> patternList = roleAnalysisService.findTopPatters(task, result);

        List<DetectedPattern> sortedPatterns = patternList.stream()
                .sorted(Comparator.comparingDouble(DetectedPattern::getMetric).reversed())
                .toList();

        return sortedPatterns.stream()
                .limit(3)
                .collect(Collectors.toList());
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

        PageBase pageBase = (PageBase) getPage();
        ModelService modelService = pageBase.getModelService();
        Task task = pageBase.createSimpleTask("loadRoleAnalysisInfo");
        OperationResult result = task.getResult();
        SearchResultList<PrismObject<RoleAnalysisOutlierType>> searchResultList;
        try {
            searchResultList = modelService
                    .searchObjects(RoleAnalysisOutlierType.class, null, null, task, result);
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException |
                CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
            throw new SystemException("Couldn't load role analysis outliers", e);
        }

        List<RoleAnalysisOutlierType> topFiveOutliers = getTopFiveOutliers(searchResultList);

        RoleAnalysisInfoPanel roleAnalysisInfoPanel = new RoleAnalysisInfoPanel(ID_CHART_PANEL) {
            @Override
            protected @NotNull IModel<List<IdentifyWidgetItem>> getModelOutliers() {

                if (searchResultList.isEmpty()) {
                    return Model.ofList(List.of());
                }

                List<IdentifyWidgetItem> detailsModel = new ArrayList<>();

                for (RoleAnalysisOutlierType topFiveOutlier : topFiveOutliers) {

                    Set<String> anomalies = new HashSet<>();
                    List<RoleAnalysisOutlierPartitionType> outlierPartitions = topFiveOutlier.getOutlierPartitions();
                    RoleAnalysisOutlierPartitionType topPartition = null;
                    for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                        Double overallConfidence = outlierPartition.getPartitionAnalysis().getOverallConfidence();
                        if (overallConfidence == null) {
                            overallConfidence = 0.0;
                        }

                        if (topPartition == null
                                || overallConfidence > topPartition.getPartitionAnalysis().getOverallConfidence()) {
                            topPartition = outlierPartition;
                        }

                        List<DetectedAnomalyResult> detectedAnomalyResult = outlierPartition.getDetectedAnomalyResult();
                        for (DetectedAnomalyResult detectedAnomaly : detectedAnomalyResult) {
                            if (detectedAnomaly.getTargetObjectRef() != null) {
                                anomalies.add(detectedAnomaly.getTargetObjectRef().getOid());
                            }
                        }
                    }
                    Double overallConfidence = topFiveOutlier.getOverallConfidence();
                    if (overallConfidence == null) {
                        overallConfidence = 0.0;
                    }
                    BigDecimal bd = new BigDecimal(overallConfidence);
                    bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                    overallConfidence = bd.doubleValue();
                    String description = anomalies.size() + " anomalies were detected within " + outlierPartitions.size() + " session";
                    RoleAnalysisOutlierPartitionType finalTopPartition = topPartition;
                    IdentifyWidgetItem identifyWidgetItem = new IdentifyWidgetItem(
                            IdentifyWidgetItem.ComponentType.OUTLIER,
                            Model.of(GuiStyleConstants.CLASS_ICON_OUTLIER),
                            Model.of(topFiveOutlier.getName().getOrig()),
                            Model.of(description),
                            Model.of(overallConfidence + "%"),
                            Model.of("name")) {
                        @Override
                        public void onActionComponentClick(AjaxRequestTarget target) {
                            RoleAnalysisPartitionOverviewPanel panel = new RoleAnalysisPartitionOverviewPanel(
                                    ((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of(finalTopPartition), Model.of(topFiveOutlier)) {
                                @Override
                                public IModel<String> getTitle() {
                                    return createStringResource(
                                            "RoleAnalysisPartitionOverviewPanel.title.most.impact.partition");
                                }
                            };
                            panel.setOutputMarkupId(true);
                            ((PageBase) getPage()).showMainPopup(panel, target);
                        }

                        @Override
                        public Component createTitleComponent(String id) {
                            AjaxLinkPanel linkPanel = new AjaxLinkPanel(id, Model.of(topFiveOutlier.getName())) {
                                @Override
                                public void onClick(AjaxRequestTarget target) {
                                    PageParameters parameters = new PageParameters();
                                    String outlierOid = topFiveOutlier.getOid();
                                    parameters.add(OnePageParameterEncoder.PARAMETER, outlierOid);
                                    Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                            .getObjectDetailsPage(RoleAnalysisOutlierType.class);
                                    pageBase.navigateToNext(detailsPageClass, parameters);
                                }
                            };
                            linkPanel.setOutputMarkupId(true);
                            return linkPanel;

                        }
                    };
                    detailsModel.add(identifyWidgetItem);
                }

                return Model.ofList(detailsModel);
            }

            @Override
            protected IModel<List<IdentifyWidgetItem>> getModelPatterns() {
                PageBase pageBase = (PageBase) getPage();
                OperationResult result = new OperationResult("loadRoleAnalysisInfo");
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                int allUserOwnedRoleAssignments = roleAnalysisService.countUserOwnedRoleAssignment(result);

                List<DetectedPattern> topPatterns = getTopFivePatterns(pageBase);
                List<IdentifyWidgetItem> detailsModel = new ArrayList<>();

                for (int i = 0; i < topPatterns.size(); i++) {
                    DetectedPattern pattern = topPatterns.get(i);
                    double relationsMetric = pattern.getMetric();
                    double percentagePart = 0;
                    if (relationsMetric != 0 && allUserOwnedRoleAssignments != 0) {
                        percentagePart = (relationsMetric / allUserOwnedRoleAssignments) * 100;
                    }
                    String formattedReductionFactorConfidence = String.format("%.2f", percentagePart);
                    double itemsConfidence = pattern.getItemsConfidence();
                    String formattedItemConfidence = String.format("%.1f", itemsConfidence);
                    String description =
                            relationsMetric +
                                    "x relationships with a confidence of  " +
                                    formattedItemConfidence + "%";

                    String patternName = pageBase.createStringResource(
                            "RoleAnalysis.role.suggestion.title", (i + 1)).getString();
                    IdentifyWidgetItem identifyWidgetItem = new IdentifyWidgetItem(
                            IdentifyWidgetItem.ComponentType.PATTERN,
                            Model.of(GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON),
                            Model.of(),
                            Model.of(description),
                            Model.of(formattedReductionFactorConfidence + "%"),
                            Model.of("name")) {

                        public Component createValueTitleComponent(String id) {
                            Label label = new Label(id,
                                    createStringResource("RoleAnalysisIdentifyWidgetPanel.value.title.system.reduction"));
                            label.setOutputMarkupId(true);
                            label.add(new VisibleBehaviour(() -> getDescription() != null));
                            return label;
                        }

                        @Override
                        public void onActionComponentClick(AjaxRequestTarget target) {
                            RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
                                    ((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of(pattern));
                            ((PageBase) getPage()).showMainPopup(component, target);
                        }

                        @Override
                        public Component createTitleComponent(String id) {
                            AjaxLinkPanel linkPanel = new AjaxLinkPanel(id, Model.of(patternName)) {
                                @Override
                                public void onClick(AjaxRequestTarget target) {
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
                            };
                            linkPanel.setOutputMarkupId(true);
                            return linkPanel;

                        }
                    };
                    detailsModel.add(identifyWidgetItem);
                }

                return Model.ofList(detailsModel);
            }
        };
        roleAnalysisInfoPanel.setOutputMarkupId(true);
        infoForm.add(roleAnalysisInfoPanel);

        RoleAnalysisSessionTileTable roleAnalysisSessionTileTable = new RoleAnalysisSessionTileTable(ID_TABLE, (PageBase) getPage());
        roleAnalysisSessionTileTable.setOutputMarkupId(true);
        mainForm.add(roleAnalysisSessionTileTable);

    }

}
