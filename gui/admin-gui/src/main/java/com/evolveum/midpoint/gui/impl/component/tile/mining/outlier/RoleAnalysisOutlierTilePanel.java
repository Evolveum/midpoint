/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.outlier;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.confidenceBasedTwoColor;
import static com.evolveum.midpoint.gui.impl.util.DetailsPageUtil.dispatchToObjectDetailsPage;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBarSecondStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisCluster;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisOutlier;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisSession;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisPartitionOverviewPanel;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierPartitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisPartitionAnalysisType;

public class RoleAnalysisOutlierTilePanel<T extends Serializable> extends BasePanel<RoleAnalysisOutlierTileModel<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "objectTitle";
    private static final String ID_STATUS_BAR = "status";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_PROGRESS_BAR = "progress-bar";
    private static final String ID_ANOMALY_COUNT = "anomaly-count";
    private static final String ID_ANOMALY_CONFIDENCE = "anomaly-confidence";
    private static final String ID_LOCATION = "location";
    private static final String ID_ACTION_BUTTON = "action-button";

    public RoleAnalysisOutlierTilePanel(String id, IModel<RoleAnalysisOutlierTileModel<T>> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {
        initDefaultStyle();

        initStatusBar();

        buildExploreButton();

        initToolBarPanel();

        initNamePanel();

        initProgressBar();

        initLocationButtons();

        initFirstCountPanel();
    }

    public void initStatusBar() {
        RoleAnalysisOutlierTileModel<T> modelObject = getModelObject();
        String status = modelObject.getStatus();

        Label statusBar = new Label(ID_STATUS_BAR, Model.of(status));
        statusBar.add(AttributeModifier.append(CLASS_CSS, "badge badge-pill badge-info text-truncate"));
        statusBar.add(AttributeModifier.append(STYLE_CSS, "width: 120px"));
        statusBar.setOutputMarkupId(true);
        add(statusBar);
    }

    private void initLocationButtons() {
        ObjectReferenceType clusterRef = getModelObject().getClusterRef();
        ObjectReferenceType sessionRef = getModelObject().getSessionRef();

        String clusterName = "unknown";
        String sessionName = "unknown";
        String clusterOid = null;
        String sessionOid = null;
        if (clusterRef != null) {
            clusterOid = clusterRef.getOid();
            if (clusterRef.getTargetName() != null) {
                clusterName = clusterRef.getTargetName().getOrig();
            }
        }

        if (sessionRef != null) {
            sessionOid = sessionRef.getOid();
            if (sessionRef.getTargetName() != null) {
                sessionName = sessionRef.getTargetName().getOrig();
            }
        }

        String finalSessionName = sessionName;
        String finalClusterName = clusterName;

        String finalClusterOid = clusterOid;
        String finalSessionOid = sessionOid;
        MetricValuePanel panel = new MetricValuePanel(ID_LOCATION) {
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                Label label = new Label(id, createStringResource("RoleAnalysis.title.panel.location"));
                label.setOutputMarkupId(true);
                label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                RepeatingView view = new RepeatingView(id);
                view.setOutputMarkupId(true);
                AjaxLinkPanel sessionLink = new AjaxLinkPanel(view.newChildId(), Model.of(finalSessionName)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, finalSessionOid);
                        getPageBase().navigateToNext(PageRoleAnalysisSession.class, parameters);
                    }
                };

                sessionLink.setOutputMarkupId(true);
                sessionLink.add(AttributeModifier.append(STYLE_CSS, "max-width:100px"));
                sessionLink.add(AttributeModifier.append(CLASS_CSS, TEXT_TRUNCATE));
                view.add(sessionLink);

                Label separator = new Label(view.newChildId(), "/");
                separator.setOutputMarkupId(true);
                view.add(separator);

                AjaxLinkPanel clusterLink = new AjaxLinkPanel(view.newChildId(), Model.of(finalClusterName)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, finalClusterOid);
                        getPageBase().navigateToNext(PageRoleAnalysisCluster.class, parameters);
                    }
                };
                clusterLink.setOutputMarkupId(true);
                clusterLink.add(AttributeModifier.append(STYLE_CSS, "max-width:100px"));
                clusterLink.add(AttributeModifier.append(CLASS_CSS, TEXT_TRUNCATE));
                view.add(clusterLink);
                return view;
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);

    }

    private void initProgressBar() {
        RoleAnalysisOutlierTileModel<T> modelObject = getModelObject();
        RoleAnalysisOutlierPartitionType partition = modelObject.getPartition();
        RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
        Double overallConfidence = partitionAnalysis.getOverallConfidence();
        if (overallConfidence == null) {
            overallConfidence = 0.0;
        }

        BigDecimal confidence = BigDecimal.valueOf(overallConfidence);
        confidence = confidence.setScale(2, RoundingMode.HALF_UP);
        double finalProgress = confidence.doubleValue();

        String colorClass = confidenceBasedTwoColor(finalProgress);

        ProgressBarSecondStyle progressBar = new ProgressBarSecondStyle(ID_PROGRESS_BAR) {

            @Override
            public double getActualValue() {
                return finalProgress;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getProgressBarContainerCssStyle() {
                return "border-radius: 3px; height:13px;";
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getProgressBarContainerCssClass() {
                return "col-12 pl-0 pr-0";
            }

            @Override
            public String getProgressBarColor() {
                return colorClass;
            }

            @Contract(pure = true)
            @Override
            public @NotNull String getBarTitle() {
                return createStringResource("RoleAnalysis.tile.panel.partition.confidence").getString();
            }
        };
        progressBar.setOutputMarkupId(true);
        progressBar.add(AttributeModifier.replace(TITLE_CSS, () -> "Partition confidence: " + finalProgress + "%"));
        progressBar.add(new TooltipBehavior());
        add(progressBar);
    }

    private void buildExploreButton() {
        RoleAnalysisOutlierTileModel<T> modelObject = getModelObject();
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(
                RoleAnalysisOutlierTilePanel.ID_ACTION_BUTTON,
                iconBuilder.build(),
                createStringResource("RoleAnalysis.title.panel.explore.partition.details")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                RoleAnalysisPartitionOverviewPanel detailsPanel = new RoleAnalysisPartitionOverviewPanel(
                        ((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of(modelObject.getPartition()),
                        Model.of(modelObject.getOutlier()));
                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeModifier.append(CLASS_CSS, "btn btn-default btn-sm"));
        migrationButton.setOutputMarkupId(true);
        add(migrationButton);
    }

    private void initFirstCountPanel() {

        RoleAnalysisOutlierPartitionType partition = getModelObject().getPartition();
        int anomaliesCount = partition.getDetectedAnomalyResult().size();
        Double anomalyObjectsConfidence = partition.getPartitionAnalysis().getAnomalyObjectsConfidence();
        if (anomalyObjectsConfidence == null) {
            anomalyObjectsConfidence = 0.0;
        }

        BigDecimal confidence = BigDecimal.valueOf(anomalyObjectsConfidence);
        confidence = confidence.setScale(2, RoundingMode.HALF_UP);
        anomalyObjectsConfidence = confidence.doubleValue();
        MetricValuePanel anomaliesCountPanel = new MetricValuePanel(ID_ANOMALY_COUNT) {
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                Label label = new Label(id, createStringResource("RoleAnalysis.tile.panel.anomaly.count"));
                label.setOutputMarkupId(true);
                label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
                label.add(AttributeModifier.append(STYLE_CSS, "font-size: 16px"));

                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                Label memberPanel = new Label(id, () -> anomaliesCount) {
                };

                memberPanel.setOutputMarkupId(true);
                memberPanel.add(AttributeModifier.replace(TITLE_CSS, () -> "Anomalies count: " + anomaliesCount));
                memberPanel.add(new TooltipBehavior());
                return memberPanel;
            }
        };
        anomaliesCountPanel.setOutputMarkupId(true);
        add(anomaliesCountPanel);

        Double finalAnomalyObjectsConfidence = anomalyObjectsConfidence;
        MetricValuePanel anomaliesConfidencePanel = new MetricValuePanel(ID_ANOMALY_CONFIDENCE) {
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                Label label = new Label(id, createStringResource("RoleAnalysis.tile.panel.anomaly.confidence"));
                label.setOutputMarkupId(true);
                label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
                label.add(AttributeModifier.append(STYLE_CSS, "font-size: 16px"));

                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                Label memberPanel = new Label(id, () -> finalAnomalyObjectsConfidence + "%");
                memberPanel.setOutputMarkupId(true);
                memberPanel.add(AttributeModifier.replace(TITLE_CSS, () -> "Anomalies confidence: "
                        + finalAnomalyObjectsConfidence));
                memberPanel.add(new TooltipBehavior());
                return memberPanel;
            }
        };
        anomaliesConfidencePanel.setOutputMarkupId(true);
        add(anomaliesConfidencePanel);

    }

    private void initNamePanel() {
        AjaxLinkPanel objectTitle = new AjaxLinkPanel(ID_TITLE, () -> getModelObject().getName()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                RoleAnalysisOutlierTileModel<T> modelObject = RoleAnalysisOutlierTilePanel.this.getModelObject();
                String oid = modelObject.getOutlier().getOid();
                dispatchToObjectDetailsPage(RoleAnalysisOutlierType.class, oid, getPageBase(), true);
            }
        };
        objectTitle.setOutputMarkupId(true);
        objectTitle.add(AttributeModifier.replace(STYLE_CSS, "font-size:18px"));
        objectTitle.add(AttributeModifier.replace(TITLE_CSS, () -> getModelObject().getName()));
        objectTitle.add(new TooltipBehavior());
        add(objectTitle);
    }

    private void initToolBarPanel() {
        DropdownButtonPanel barMenu = new DropdownButtonPanel(ID_BUTTON_BAR, new DropdownButtonDto(
                null, "fa fa-ellipsis-v", null, createMenuItems())) {
            @Override
            protected boolean hasToggleIcon() {
                return false;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getSpecialButtonClass() {
                return " p-0 ";
            }

        };
        barMenu.setOutputMarkupId(true);
        barMenu.add(AttributeModifier.replace(TITLE_CSS,
                createStringResource("RoleAnalysis.menu.moreOptions")));
        barMenu.add(new TooltipBehavior());
        add(barMenu);
    }

    private void initDefaultStyle() {
        setOutputMarkupId(true);
        add(AttributeModifier.append(CLASS_CSS,
                "bg-white d-flex flex-column align-items-center elevation-1 rounded w-100 h-100 p-0"));
    }

    public List<InlineMenuItem> createMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("RoleAnalysis.tile.panel.details.view")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        navigateToOutlierDetails();
                    }
                };
            }

        });

        return items;
    }

    private void navigateToOutlierDetails() {
        RoleAnalysisOutlierTileModel<T> modelObject = getModelObject();
        RoleAnalysisOutlierType outlier = modelObject.getOutlier();

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, outlier.getOid());
        getPageBase().navigateToNext(PageRoleAnalysisOutlier.class, parameters);

    }

}
