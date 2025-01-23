/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.outlier;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier.OutlierPartitionPage;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

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
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translateMessage;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.*;

public class RoleAnalysisOutlierPartitionTilePanel<T extends Serializable> extends BasePanel<RoleAnalysisOutlierPartitionTileModel<T>> {

    @Serial private static final long serialVersionUID = 1L;
    private static final String ID_TITLE = "title";
    private static final String ID_LEFT_BATCH = "leftBatch";
    private static final String ID_RIGHT_BATCH = "rightBatch";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_ITEMS = "items";
    private static final String ID_EXAMINE = "examine";
    private static final String ID_CONFIDENCE = "confidence";
    private static final String ID_EXPLANATION = "explanation";

    public RoleAnalysisOutlierPartitionTilePanel(String id, IModel<RoleAnalysisOutlierPartitionTileModel<T>> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {
        initDefaultCssStyle();

        initTitle();
        initLeftBatch();
        initRightBatch();
        initToolBarPanel();

        initItems();

        initExamineButton();
        initConfidencePanel();
    }

    private void initTitle() {
        RoleAnalysisOutlierPartitionType partition = getModelObject().getPartition();

        //TODO check
        String sessionName = "unknown";
        String clusterName = "unknown";

        if (partition.getTargetSessionRef() != null && partition.getTargetSessionRef().getTargetName() != null) {
            sessionName = partition.getTargetSessionRef().getTargetName().toString();
        }

        if (partition.getClusterRef() != null && partition.getClusterRef().getTargetName() != null) {
            clusterName = partition.getClusterRef().getTargetName().toString();
        }

        String title = sessionName + " / " + clusterName;

        IconWithLabel titlePanel = new IconWithLabel(ID_TITLE, () -> title) {
            @Contract(pure = true)
            @Override
            public @NotNull String getIconCssClass() {
                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON + " p-1 text-muted";
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getIconContainerCssClass() {
                return "p-1 rounded";
            }

            @Override
            protected String getIconContainerCssStyle() {
                return "background-color:#6C757D26; border-radius: 25%";
            }

            @Override
            protected boolean isLink() {
                return true;
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, partition.getClusterRef().getOid());
                parameters.add("panelId", "clusterDetails");
                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisClusterType.class);
                getPageBase().navigateToNext(detailsPageClass, parameters);
            }
        };
        titlePanel.add(AttributeModifier.append(CLASS_CSS, "gap-2"));

        titlePanel.setOutputMarkupId(true);
        titlePanel.add(new TooltipBehavior());
        add(titlePanel);
    }

    private void initLeftBatch() {
        Label label = buildCategoryPanel();
        label.add(AttributeModifier.append(CLASS_CSS, "badge bg-info"));
        label.add(new VisibleBehaviour(() -> false));
        add(label);

        Model<String> explanationTranslatedModel = getModelObject().getExplanationTranslatedModel();

        Label explanationField = new Label(ID_EXPLANATION, explanationTranslatedModel);
        explanationField.setOutputMarkupId(true);
        explanationField.add(AttributeModifier.append(CLASS_CSS, "gap-2"));
        add(explanationField);
    }

    private @NotNull Label buildCategoryPanel() {
        RoleAnalysisOutlierPartitionType partition = getModelObject().getPartition();
        RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
        OutlierCategoryType outlierCategory = partitionAnalysis.getOutlierCategory();
        String labelValue = "N/A";
        if (outlierCategory != null) {
            OutlierNoiseCategoryType outlierNoiseCategory = outlierCategory.getOutlierNoiseCategory();
            if (outlierNoiseCategory != null) {
                labelValue = outlierNoiseCategory.value();
            }
        }
        Label label = new Label(ID_LEFT_BATCH, labelValue);
        label.setOutputMarkupId(true);
        return label;
    }

    private void initRightBatch() {
        String labelValue = "MOST IMPACTFUL";
        Label label = new Label(ID_RIGHT_BATCH, labelValue);
        label.setOutputMarkupId(true);
        label.add(AttributeModifier.append(CLASS_CSS, "badge bg-danger"));
        label.add(new VisibleBehaviour(() -> getModelObject().isMostImpactful));
        add(label);
    }

    private void initItems() {
        RepeatingView items = new RepeatingView(ID_ITEMS);
        add(items);

        RoleAnalysisOutlierPartitionType partition = getModelObject().getPartition();
        RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
        Double anomalyObjectsConfidence = partitionAnalysis.getAnomalyObjectsConfidence();
        List<DetectedAnomalyResult> detectedAnomalyResult = partition.getDetectedAnomalyResult();

        BigDecimal bd = new BigDecimal(Double.toString(anomalyObjectsConfidence));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        double averageAnomalyConfidence = bd.doubleValue();
        String anomalyValue = averageAnomalyConfidence + "% (" + detectedAnomalyResult.size() + ")";

        MetricValuePanel assignmentAnomalyPanel = new MetricValuePanel(items.newChildId()) {

            @Override
            protected @NotNull Component getTitleComponent(String id) {
                LabelWithHelpPanel label = new LabelWithHelpPanel(id,
                        createStringResource("RoleAnalysisOutlierPartitionTilePanel.assignmentAnomaly")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisOutlierPartitionTilePanel.assignmentAnomaly.help");
                    }
                };
                label.setOutputMarkupId(true);
                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                Label label = new Label(id, anomalyValue);
                label.setOutputMarkupId(true);
                label.add(AttributeModifier.append(CLASS_CSS, FONT_WEIGHT_BOLD));
                return label;
            }
        };
        assignmentAnomalyPanel.setOutputMarkupId(true);
        items.add(assignmentAnomalyPanel);

        RoleAnalysisPatternAnalysis patternAnalysis = partitionAnalysis.getPatternAnalysis();
        Double confidence = patternAnalysis.getConfidence();
        if (confidence == null) {
            confidence = 0.0;
        }
        BigDecimal bdConfidence = new BigDecimal(Double.toString(confidence));
        bdConfidence = bdConfidence.setScale(2, RoundingMode.HALF_UP);
        double patternCoverageConfidence = bdConfidence.doubleValue();
        String confidenceValue = patternCoverageConfidence + "%";

        MetricValuePanel patternPanel = new MetricValuePanel(items.newChildId()) {

            @Override
            protected @NotNull Component getTitleComponent(String id) {
                LabelWithHelpPanel label = new LabelWithHelpPanel(id,
                        createStringResource("RoleAnalysisOutlierPartitionTilePanel.pattern.coverage")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisOutlierPartitionTilePanel.pattern.help");
                    }
                };
                label.setOutputMarkupId(true);
                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                Label label = new Label(id, confidenceValue);
                label.setOutputMarkupId(true);
                label.add(AttributeModifier.append(CLASS_CSS, FONT_WEIGHT_BOLD));
                return label;
            }
        };
        patternPanel.setOutputMarkupId(true);
        items.add(patternPanel);

        RoleAnalysisOutlierSimilarObjectsAnalysisResult similarObjectAnalysis = partitionAnalysis.getSimilarObjectAnalysis();
        Integer similarObjectsCount = similarObjectAnalysis.getSimilarObjectsCount();

        if (similarObjectsCount == null) {
            similarObjectsCount = 0;
        }

        Double similarObjectsConfidence = partitionAnalysis.getSimilarObjectsConfidence();
        if (similarObjectsConfidence == null) {
            similarObjectsConfidence = 0.0;
        }

        BigDecimal bdSimilarObjectsConfidence = new BigDecimal(Double.toString(similarObjectsConfidence));
        bdSimilarObjectsConfidence = bdSimilarObjectsConfidence.setScale(2, RoundingMode.HALF_UP);
        double similarObjectsConfidenceValue = bdSimilarObjectsConfidence.doubleValue();
        String similarObjectsValue = similarObjectsCount + " (" + similarObjectsConfidenceValue + "%)";

        MetricValuePanel similarObjectPanel = new MetricValuePanel(items.newChildId()) {

            @Override
            protected @NotNull Component getTitleComponent(String id) {
                LabelWithHelpPanel label = new LabelWithHelpPanel(id,
                        createStringResource("RoleAnalysisOutlierPartitionTilePanel.similarObjects")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisOutlierPartitionTilePanel.similarObjects.help");
                    }
                };
                label.setOutputMarkupId(true);
                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                Label label = new Label(id, similarObjectsValue);
                label.setOutputMarkupId(true);
                label.add(AttributeModifier.append(CLASS_CSS, FONT_WEIGHT_BOLD));
                return label;
            }
        };
        similarObjectPanel.setOutputMarkupId(true);
        items.add(similarObjectPanel);

        Double outlierAssignmentFrequencyConfidence = partitionAnalysis.getOutlierAssignmentFrequencyConfidence();
        if (outlierAssignmentFrequencyConfidence == null) {
            outlierAssignmentFrequencyConfidence = 0.0;
        }
        BigDecimal bdOutlierAssignmentFrequencyConfidence = new BigDecimal(Double.toString(outlierAssignmentFrequencyConfidence));
        bdOutlierAssignmentFrequencyConfidence = bdOutlierAssignmentFrequencyConfidence.setScale(2, RoundingMode.HALF_UP);
        double outlierAssignmentFrequencyConfidenceValue = bdOutlierAssignmentFrequencyConfidence.doubleValue();
        String outlierAssignmentFrequencyValue = outlierAssignmentFrequencyConfidenceValue + "%";
        MetricValuePanel outlierAssignmentFrequencyPanel = new MetricValuePanel(items.newChildId()) {

            @Override
            protected @NotNull Component getTitleComponent(String id) {
                LabelWithHelpPanel label = new LabelWithHelpPanel(id,
                        createStringResource("RoleAnalysisOutlierPartitionTilePanel.outlierAssignmentFrequency")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisOutlierPartitionTilePanel.outlierAssignmentFrequency.help");
                    }
                };
                label.setOutputMarkupId(true);
                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                Label label = new Label(id, outlierAssignmentFrequencyValue);
                label.setOutputMarkupId(true);
                label.add(AttributeModifier.append(CLASS_CSS, FONT_WEIGHT_BOLD));
                return label;
            }
        };
        outlierAssignmentFrequencyPanel.setOutputMarkupId(true);
        items.add(outlierAssignmentFrequencyPanel);

        MetricValuePanel attributeAnalysisPanel = new MetricValuePanel(items.newChildId()) {

            @Override
            protected @NotNull Component getTitleComponent(String id) {
                LabelWithHelpPanel label = new LabelWithHelpPanel(id,
                        createStringResource("RoleAnalysisOutlierPartitionTilePanel.attributeAnalysis")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisOutlierPartitionTilePanel.attributeAnalysis.help");
                    }
                };
                label.setOutputMarkupId(true);
                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                AttributeAnalysis attributeAnalysis = partition.getPartitionAnalysis().getAttributeAnalysis();
                RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = attributeAnalysis.getUserAttributeAnalysisResult();
                List<RoleAnalysisAttributeAnalysis> attributeAnalysisCluster = userAttributeAnalysisResult.getAttributeAnalysis();

                double totalDensity = 0.0;
                int totalCount = 0;
                if (attributeAnalysisCluster != null) {
                    totalDensity += calculateDensity(attributeAnalysisCluster);
                    totalCount += attributeAnalysisCluster.size();
                }

                int itemCount = (attributeAnalysisCluster != null ? attributeAnalysisCluster.size() : 0);

                double itemsConfidence = (totalCount > 0 && totalDensity > 0.0 && itemCount > 0) ? totalDensity / itemCount : 0.0;

                BigDecimal bd = BigDecimal.valueOf(itemsConfidence);
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                itemsConfidence = bd.doubleValue();

                Label label = new Label(id, itemsConfidence + "%");
                label.setOutputMarkupId(true);
                label.add(AttributeModifier.append(CLASS_CSS, FONT_WEIGHT_BOLD));
                return label;
            }
        };
        attributeAnalysisPanel.setOutputMarkupId(true);
        items.add(attributeAnalysisPanel);

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

    private void initDefaultCssStyle() {
        setOutputMarkupId(true);

        add(AttributeModifier.append(CLASS_CSS,
                "catalog-tile-panel d-flex flex-column align-items-center w-100 h-100 p-0 elevation-1"));

        add(AttributeModifier.append(STYLE_CSS, "width:25%"));
    }

    protected Label getTitle() {
        return (Label) get(ID_TITLE);
    }

    private void initExamineButton() {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                "fa fa-eye", IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton examineButton = new AjaxCompositedIconSubmitButton(
                ID_EXAMINE,
                iconBuilder.build(),
                createStringResource("Examine")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                RoleAnalysisOutlierType outlier = getModelObject().getOutlierParent();
                RoleAnalysisOutlierPartitionType partitionType = getModelObject().getPartition();
                PageParameters parameters = new PageParameters();
                parameters.add(OutlierPartitionPage.PARAM_OUTLIER_OID, outlier.getOid());
                parameters.add(OutlierPartitionPage.PARAM_SESSION_OID, partitionType.getTargetSessionRef().getOid());
                getPageBase().navigateToNext(OutlierPartitionPage.class, parameters);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        examineButton.titleAsLabel(true);
        examineButton.setOutputMarkupId(true);
        examineButton.add(AttributeModifier.append(CLASS_CSS, "btn btn-default btn-sm p-2"));

        examineButton.setOutputMarkupId(true);
        add(examineButton);
    }

    public List<InlineMenuItem> createMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("Details view")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        //TODO
                    }
                };
            }

        });

        return items;
    }

    private void initConfidencePanel() {
        RoleAnalysisOutlierPartitionType partition = getModelObject().getPartition();
        Double overallConfidence = partition.getPartitionAnalysis().getOverallConfidence();
        BigDecimal bdOverallConfidence = new BigDecimal(Double.toString(overallConfidence));
        bdOverallConfidence = bdOverallConfidence.setScale(2, RoundingMode.HALF_UP);
        double overallConfidenceValue = bdOverallConfidence.doubleValue();
        String overallValue = overallConfidenceValue + "%";
        MetricValuePanel confidencePanel = new MetricValuePanel(ID_CONFIDENCE) {

            @Override
            protected @NotNull Component getTitleComponent(String id) {
                Label label = new Label(id,
                        createStringResource("RoleAnalysisOutlierPartitionTilePanel.confidence.score"));
                label.setOutputMarkupId(true);
                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                IconWithLabel panel = new IconWithLabel(id, () -> overallValue) {
                    @Override
                    public String getIconCssClass() {
                        return "fa fa-circle text-danger";
                    }

                    @Override
                    protected String getIconComponentCssStyle() {
                        return "font-size:10px";
                    }
                };
                panel.add(AttributeModifier.append(CLASS_CSS, "h5 text-danger justify-content-end"));
                panel.setOutputMarkupId(true);
                return panel;
            }
        };
        confidencePanel.setOutputMarkupId(true);
        add(confidencePanel);
    }

    private double calculateDensity(@NotNull List<RoleAnalysisAttributeAnalysis> attributeAnalysisList) {
        double totalDensity = 0.0;
        for (RoleAnalysisAttributeAnalysis attributeAnalysis : attributeAnalysisList) {
            Double density = attributeAnalysis.getDensity();
            if (density != null) {
                totalDensity += density;
            }
        }
        return totalDensity;
    }
}
