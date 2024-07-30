/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierHeaderResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierItemResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierResultPanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.panel.RoleAnalysisDetectedPatternTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisOutlierPropertyTileTable;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateUserOutlierResultModel;

@PanelType(name = "outlierPartitions")
@PanelInstance(
        identifier = "outlierPartitions",
        applicableForType = RoleAnalysisOutlierType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.outlierPartitions",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 30
        )
)
public class OutlierPartitionPanel extends AbstractObjectMainPanel<RoleAnalysisOutlierType, ObjectDetailsModels<RoleAnalysisOutlierType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    public OutlierPartitionPanel(String id, ObjectDetailsModels<RoleAnalysisOutlierType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        RepeatingView components = loadTable();
        container.add(components);
    }

    private RepeatingView loadTable() {
        RoleAnalysisOutlierType outlierParent = getObjectDetailsModels().getObjectType();
        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierParent.getOutlierPartitions();

        RepeatingView repeatingView = new RepeatingView(ID_PANEL);
        add(repeatingView);

        for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
            Double overallConfidence = outlierPartition.getPartitionAnalysis().getOverallConfidence();
            OutlierHeaderResultPanel componentsHeader = new OutlierHeaderResultPanel(repeatingView.newChildId(), outlierParent.getName().getOrig(),
                    "Outlier fall under partition due to high confidence level",
                    String.format("%.2f", overallConfidence),
                    outlierPartition.getCreateTimestamp().toString()) {
                @Override
                protected boolean isLink() {
                    return true;
                }

                @Override
                protected boolean isViewAnalyzedClusterEnable() {
                    return true;
                }

                @Override
                protected void performOnAction(AjaxRequestTarget target) {

                    RoleAnalysisPartitionAnalysisType partitionAnalysis = outlierPartition.getPartitionAnalysis();
                    RoleAnalysisOutlierSimilarObjectsAnalysisResult similarObjectAnalysis = partitionAnalysis.getSimilarObjectAnalysis();
                    List<ObjectReferenceType> similarObjects = similarObjectAnalysis.getSimilarObjects();
                    Set<String> similarObjectOids = similarObjects.stream().map(ObjectReferenceType::getOid).collect(Collectors.toSet());
                    OutlierAnalyseActionDetailsPopupPanel detailsPanel = new OutlierAnalyseActionDetailsPopupPanel(
                            ((PageBase) getPage()).getMainPopupBodyId(),
                            Model.of("Analyzed members details panel"), outlierParent.getTargetObjectRef().getOid(),
                            similarObjectOids, outlierPartition.getTargetSessionRef().getOid()) {
                        @Override
                        public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                            super.onClose(ajaxRequestTarget);
                        }
                    };
                    ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                }

                @Override
                protected void performOnClick(AjaxRequestTarget target) {
                    PageBase pageBase = getPageBase();
                    RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                    Task task = pageBase.createSimpleTask("loadOutlierResult");
                    OutlierObjectModel outlierObjectModel = generateUserOutlierResultModel(
                            roleAnalysisService, outlierParent, task, task.getResult(), outlierPartition, getPageBase());

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
            };
            componentsHeader.setOutputMarkupId(true);
            componentsHeader.add(AttributeAppender.append("class", "p-3 bg-light"));

            repeatingView.add(componentsHeader);

            RoleAnalysisOutlierPropertyTileTable components = buildPropertyTable(outlierPartition, repeatingView, outlierParent);
            components.add(AttributeAppender.append("class", "pb-3"));
            repeatingView.add(components);
        }

        return repeatingView;
    }

    @NotNull
    private RoleAnalysisOutlierPropertyTileTable buildPropertyTable(
            @NotNull RoleAnalysisOutlierPartitionType outlierPartition,
            @NotNull RepeatingView repeatingView,
            @NotNull RoleAnalysisOutlierType outlierParent) {
        RoleAnalysisOutlierPropertyTileTable components = new RoleAnalysisOutlierPropertyTileTable(repeatingView.newChildId(), getPageBase(),
                new LoadableDetachableModel<>() {
                    @Override
                    protected RoleAnalysisOutlierPartitionType load() {
                        return outlierPartition;
                    }
                }, outlierParent) {

            @Override
            protected void onRefresh(AjaxRequestTarget target) {
                performOnRefresh();
            }
        };
        components.setOutputMarkupId(true);
        return components;
    }

    private void performOnRefresh() {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, getObjectDetailsModels().getObjectType().getOid());
        parameters.add(ID_PANEL, getPanelConfiguration().getIdentifier());
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisOutlierType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected RoleAnalysisDetectedPatternTable getTable() {
        return (RoleAnalysisDetectedPatternTable) get(((PageBase) getPage()).createComponentPath(ID_CONTAINER, ID_PANEL));
    }

}
