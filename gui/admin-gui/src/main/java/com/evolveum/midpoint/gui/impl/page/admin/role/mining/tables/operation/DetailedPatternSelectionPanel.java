/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.InfoBoxModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetails;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisInfoBox;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;

public class DetailedPatternSelectionPanel extends BasePanel<PatternStatistics<?>> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER_ITEMS = "header-items";
    private static final String ID_PANEL = "panelId";
    private static final String ID_CARD_TITLE = "card-title";
    private static final String ID_EXPLORE_PATTERN_BUTTON = "explore-pattern-button";

    public DetailedPatternSelectionPanel(String id, IModel<PatternStatistics<?>> model) {
        super(id, model);
//        initLayout();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        initRoleMiningPart(container);
    }

    private void initRoleMiningPart(WebMarkupContainer container) {

        if (getPattern() != null) {
            DetectedPattern pattern = getPattern();
            RepeatingView headerItems = new RepeatingView(ID_HEADER_ITEMS);
            headerItems.setOutputMarkupId(true);
            container.add(headerItems);

            initRoleMiningHeaders(headerItems);

            Label cardTitle = new Label(ID_CARD_TITLE, "Top suggested role");
            cardTitle.setOutputMarkupId(true);
            container.add(cardTitle);

            AjaxCompositedIconSubmitButton components = buildExplorePatternButton(pattern);
            container.add(components);

            RoleAnalysisDetectedPatternDetails statisticsPanel = new RoleAnalysisDetectedPatternDetails(ID_PANEL,
                    Model.of(pattern)) {

                @Contract(pure = true)
                @Override
                protected @NotNull String getCssClassForCardContainer() {
                    return "m-0 border-0";
                }

                @Override
                protected String getIconBoxContainerCssStyle() {
                    return "width:40px";
                }

                @Contract(pure = true)
                @Override
                protected @NotNull String getCssClassForHeaderItemsContainer() {
                    return "d-flex flex-wrap p-2";
                }

                @Contract(pure = true)
                @Override
                protected @NotNull String getCssClassForStatisticsPanelContainer() {
                    return "col-12 p-0 border-top";
                }

                @Contract(pure = true)
                @Override
                protected @NotNull String getCssClassForStatisticsPanel() {
                    return "col-12 p-0";
                }

            };
            statisticsPanel.setOutputMarkupId(true);
            container.add(statisticsPanel);

        } else {
            Label label = new Label(ID_PANEL, "No data available");
            label.setOutputMarkupId(true);
            container.add(label);

            WebMarkupContainer headerItems = new WebMarkupContainer(ID_HEADER_ITEMS);
            headerItems.setOutputMarkupId(true);
            container.add(headerItems);

            Label cardTitle = new Label(ID_CARD_TITLE, "No data available");
            cardTitle.setOutputMarkupId(true);
            headerItems.add(cardTitle);

            WebMarkupContainer exploreButton = new WebMarkupContainer(ID_EXPLORE_PATTERN_BUTTON);
            exploreButton.setOutputMarkupId(true);
            container.add(exploreButton);
        }
    }

    public DetectedPattern getPattern() {
        return getModelObject().getDetectedPattern();
    }

    @Override
    public int getWidth() {
        return 50;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public IModel<String> getTitle() {
        return null;
    }

    @Override
    public Component getContent() {
        return this;
    }

    private void initRoleMiningHeaders(RepeatingView headerItems) {

        InfoBoxModel infoBoxReduction = new InfoBoxModel(GuiStyleConstants.ARROW_LONG_DOWN + " text-white",
                "Detected patterns",
                String.valueOf(getModelObject().getDetectedPatternCount()),
                100,
                "Number of detected patterns");

        RoleAnalysisInfoBox infoBoxReductionLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxReduction));
        infoBoxReductionLabel.add(AttributeModifier.replace(CLASS_CSS, "col-md-6 p-2"));
        infoBoxReductionLabel.setOutputMarkupId(true);
        headerItems.add(infoBoxReductionLabel);

        InfoBoxModel infoBoxOutliers = new InfoBoxModel(GuiStyleConstants.EVO_ASSIGNMENT_ICON + " text-white",
                "Top pattern relations",
                String.valueOf(getModelObject().getTopPatternRelations()),
                100,
                "Number of top pattern relations");

        RoleAnalysisInfoBox outliersLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxOutliers));
        outliersLabel.add(AttributeModifier.replace(CLASS_CSS, "col-md-6 p-2"));
        outliersLabel.setOutputMarkupId(true);
        headerItems.add(outliersLabel);

        InfoBoxModel infoBoxResolvedPattern = new InfoBoxModel(
                GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON + " text-white",
                "Max coverage",
                String.format("%.2f", getModelObject().getMaxCoverage()),
                100,
                "Max coverage of the detected pattern");

        RoleAnalysisInfoBox resolvedPatternLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxResolvedPattern));
        resolvedPatternLabel.add(AttributeModifier.replace(CLASS_CSS, "col-md-6 p-2"));
        resolvedPatternLabel.setOutputMarkupId(true);
        headerItems.add(resolvedPatternLabel);

        double averageRelationPerPattern = getModelObject().getDetectedPatternCount() > 0 ? (double) getModelObject().getTotalRelations() / getModelObject().getDetectedPatternCount() : 0;

        InfoBoxModel infoBoxCandidateRoles = new InfoBoxModel(
                GuiStyleConstants.EVO_ASSIGNMENT_ICON + " text-white",
                "Average relations",
                String.format("%.2f", averageRelationPerPattern),
                100,
                "Average relations per detected pattern");

        RoleAnalysisInfoBox candidateRolesLabel = new RoleAnalysisInfoBox(
                headerItems.newChildId(), Model.of(infoBoxCandidateRoles));
        candidateRolesLabel.add(AttributeModifier.replace(CLASS_CSS, "col-md-6 p-2"));
        candidateRolesLabel.setOutputMarkupId(true);
        headerItems.add(candidateRolesLabel);
    }

    private @NotNull AjaxCompositedIconSubmitButton buildExplorePatternButton(DetectedPattern pattern) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton explorePatternButton = new AjaxCompositedIconSubmitButton(
                ID_EXPLORE_PATTERN_BUTTON,
                iconBuilder.build(),
                createStringResource("RoleAnalysis.explore.button.title")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                explorePatternPerform(pattern, target);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        explorePatternButton.titleAsLabel(true);
        explorePatternButton.setOutputMarkupId(true);
        explorePatternButton.add(AttributeModifier.append(CLASS_CSS, "ml-auto btn btn-primary btn-sm"));
        explorePatternButton.setOutputMarkupId(true);
        return explorePatternButton;
    }

    protected void explorePatternPerform(@NotNull DetectedPattern pattern, AjaxRequestTarget target) {
        //override in subclass
    }

}
