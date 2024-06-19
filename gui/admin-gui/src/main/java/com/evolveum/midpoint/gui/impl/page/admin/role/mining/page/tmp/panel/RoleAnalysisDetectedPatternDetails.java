/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.InfoBoxModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResult;

import org.jetbrains.annotations.NotNull;

public class RoleAnalysisDetectedPatternDetails extends BasePanel<DetectedPattern> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER_ITEMS_CONTAINER = "header-items-container";
    private static final String ID_HEADER_ITEMS = "header-items";
    private static final String ID_STATISTICS_PANEL_CONTAINER = "statistics-panel-container";
    private static final String ID_STATISTICS_PANEL = "statistics-panel";

    public RoleAnalysisDetectedPatternDetails(String id, IModel<DetectedPattern> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        WebMarkupContainer headerItemsContainer = new WebMarkupContainer(ID_HEADER_ITEMS_CONTAINER);
        headerItemsContainer.setOutputMarkupId(true);
        headerItemsContainer.add(AttributeModifier.replace("class", getCssClassForHeaderItemsContainer()));
        container.add(headerItemsContainer);

        RepeatingView headerItems = new RepeatingView(ID_HEADER_ITEMS);
        headerItems.setOutputMarkupId(true);
        headerItemsContainer.add(headerItems);

        initHeaderPanel(headerItems);

        WebMarkupContainer statisticsPanelContainer = new WebMarkupContainer(ID_STATISTICS_PANEL_CONTAINER);
        statisticsPanelContainer.setOutputMarkupId(true);
        statisticsPanelContainer.add(AttributeModifier.replace("class", getCssClassForStatisticsPanelContainer()));
        container.add(statisticsPanelContainer);

        initStatisticsPanel(statisticsPanelContainer);
    }

    protected String getCssClassForHeaderItemsContainer() {
        return "row";
    }

    protected String getCssClassForStatisticsPanelContainer() {
        return "col-12 p-0";
    }

    private void initHeaderPanel(RepeatingView headerItems) {
        DetectedPattern pattern = getModel().getObject();
        if (getModel().getObject() == null) {
            return;
        }

        Double patternMetric = pattern.getMetric();
        int relationCount = patternMetric != null ? patternMetric.intValue() : 0;

        IModel<String> reduction = Model.of(String.valueOf(relationCount));
        IModel<String> confidence = Model.of(String.format("%.2f", pattern.getItemsConfidence()) + "%");
        IModel<String> roleObjectCount = Model.of(String.valueOf(pattern.getRoles().size()));
        IModel<String> userObjectCount = Model.of(String.valueOf(pattern.getUsers().size()));

        InfoBoxModel infoBoxModelReduction = new InfoBoxModel(GuiStyleConstants.ARROW_LONG_DOWN,
                "Reduction",
                reduction.getObject(),
                pattern.getReductionFactorConfidence(),
                "Reduction factor");

        RoleAnalysisInfoBox reductionLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxModelReduction)) {
            @Override
            protected String getInfoBoxCssClass() {
                return super.getInfoBoxCssClass();
            }

            @Override
            public String getIconBoxContainerCssStyle() {
                return RoleAnalysisDetectedPatternDetails.this.getIconBoxContainerCssStyle();
            }
        };
        reductionLabel.add(AttributeModifier.replace("class", getInfoBoxClass()));
        reductionLabel.setOutputMarkupId(true);
        headerItems.add(reductionLabel);

        InfoBoxModel infoBoxModelConfidence = new InfoBoxModel(GuiStyleConstants.THUMBS_UP,
                "Confidence",
                confidence.getObject(),
                pattern.getItemsConfidence(),
                "Confidence of the suggested role");

        RoleAnalysisInfoBox confidenceLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxModelConfidence)) {
            @Override
            protected String getInfoBoxCssClass() {
                return super.getInfoBoxCssClass();
            }

            @Override
            public String getIconBoxContainerCssStyle() {
                return RoleAnalysisDetectedPatternDetails.this.getIconBoxContainerCssStyle();
            }
        };
        confidenceLabel.add(AttributeModifier.replace("class", getInfoBoxClass()));
        confidenceLabel.setOutputMarkupId(true);
        headerItems.add(confidenceLabel);

        InfoBoxModel infoBoxModelRoles = new InfoBoxModel(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON,
                "Roles",
                roleObjectCount.getObject(),
                100,
                "Number of roles in the suggested role");

        RoleAnalysisInfoBox roleObjectCountLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxModelRoles)) {
            @Override
            protected String getInfoBoxCssClass() {
                return super.getInfoBoxCssClass();
            }

            @Override
            public String getIconBoxContainerCssStyle() {
                return RoleAnalysisDetectedPatternDetails.this.getIconBoxContainerCssStyle();
            }
        };
        roleObjectCountLabel.add(AttributeModifier.replace("class", getInfoBoxClass()));
        roleObjectCountLabel.setOutputMarkupId(true);
        headerItems.add(roleObjectCountLabel);

        InfoBoxModel infoBoxModelUsers = new InfoBoxModel(GuiStyleConstants.CLASS_OBJECT_USER_ICON,
                "Users",
                userObjectCount.getObject(),
                100,
                "Number of users in the suggested role");

        RoleAnalysisInfoBox userObjectCountLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxModelUsers)) {
            @Override
            protected String getInfoBoxCssClass() {
                return super.getInfoBoxCssClass();
            }

            @Override
            public String getIconBoxContainerCssStyle() {
                return RoleAnalysisDetectedPatternDetails.this.getIconBoxContainerCssStyle();
            }
        };
        userObjectCountLabel.add(AttributeModifier.replace("class", getInfoBoxClass()));
        userObjectCountLabel.setOutputMarkupId(true);
        headerItems.add(userObjectCountLabel);
    }

    private void initStatisticsPanel(WebMarkupContainer container) {

        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = null;
        RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = null;
        if (getModel().getObject() != null) {
            DetectedPattern pattern = getModel().getObject();
            userAttributeAnalysisResult = pattern.getUserAttributeAnalysisResult();
            roleAttributeAnalysisResult = pattern.getRoleAttributeAnalysisResult();
        }

        if (userAttributeAnalysisResult != null || roleAttributeAnalysisResult != null) {
            RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(ID_STATISTICS_PANEL,
                    getCardTitleModel(), roleAttributeAnalysisResult, userAttributeAnalysisResult) {
                @Override
                protected String getCssClassForCardContainer() {
                    String cssClassForCardContainer = RoleAnalysisDetectedPatternDetails.this.getCssClassForCardContainer();
                    if (cssClassForCardContainer != null) {
                        return cssClassForCardContainer;
                    }

                    return super.getCssClassForCardContainer();
                }

                @Override
                protected @NotNull String getChartContainerStyle() {
                    return "height:30vh;";
                }
            };
            roleAnalysisAttributePanel.setOutputMarkupId(true);
            roleAnalysisAttributePanel.add(AttributeModifier.replace("class", getCssClassForStatisticsPanel()));
            container.add(roleAnalysisAttributePanel);
        } else {
            Label label = new Label(ID_STATISTICS_PANEL, "No data available");
            label.setOutputMarkupId(true);
            container.add(label);
        }
    }

    protected String getCssClassForCardContainer() {
        return null;
    }

    protected String getInfoBoxClass() {
        return "col-md-3";
    }

    protected IModel<String> getCardTitleModel() {
        return Model.of("Role suggestion attributes analysis result");
    }

    protected String getCssClassForStatisticsPanel() {
        return null;
    }

    protected String getIconBoxContainerCssStyle() {
        return null;
    }

}
