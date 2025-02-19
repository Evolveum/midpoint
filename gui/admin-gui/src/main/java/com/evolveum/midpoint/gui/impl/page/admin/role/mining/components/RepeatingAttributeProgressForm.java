/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributeAnalysisDto;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

/**
 * Represents a form containing attribute analysis results for role or user objects.
 * It displays collapsible sections for attributes and objects.
 * <p>
 * The form consists of:
 * <p> - Attribute section: Displays attribute analysis results with collapsible functionality.
 * <p> - Object section: Displays details of analyzed members with collapsible functionality.
 */
public class RepeatingAttributeProgressForm extends BasePanel<List<RoleAnalysisAttributeAnalysisDto>> {
    private static final String ID_CONTAINER_FIRST_GROUP = "containerFirstGroup";
    private static final String ID_REPEATING_VIEW_FIRST_GROUP = "progressBarFormFirstGroup";
    private static final String ID_SHOW_MORE_PROGRESS_BAR = "showMoreProgressForm";

    private static final String ID_ATTRIBUTE_STATISTICS = "attributeStatistics";

    private static final int MAX_VISIBLE_BARS = 3;
    boolean isResultExpanded = false;

    public RepeatingAttributeProgressForm(String id,
            @NotNull IModel<List<RoleAnalysisAttributeAnalysisDto>> attributeAnalysisResult) {
        super(id, attributeAnalysisResult);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initProgressBars(getPathToMark());
    }

    protected Set<String> getPathToMark() {
        return null;
    }

    public boolean isHide() {
        return false;
    }

    private void initProgressBars(
            Set<String> pathToMark) {
        WebMarkupContainer containerFirstGroup = createContainer();
        add(containerFirstGroup);
        int totalBars = 0;

//        //TODO incorrect model unwrapping
//        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = getModelObject().getAttributeAnalysis();
//        List<RoleAnalysisAttributeAnalysis> toSort = new ArrayList<>();
//        for (RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis : attributeAnalysis) {
//            List<RoleAnalysisAttributeStatistics> attributeStatistics = roleAnalysisAttributeAnalysis.getAttributeStatistics();
//            if (attributeStatistics != null && !attributeStatistics.isEmpty()) {
//                toSort.add(roleAnalysisAttributeAnalysis);
//            }
//        }
//        toSort.sort((analysis1, analysis2) -> {
//            Double density1 = analysis1.getDensity();
//            Double density2 = analysis2.getDensity();
//            return Double.compare(density2, density1);
//        });

//        RepeatingView repeatingProgressBar = new RepeatingView(ID_REPEATING_VIEW_FIRST_GROUP);
//        repeatingProgressBar.setOutputMarkupId(true);
//        containerFirstGroup.add(repeatingProgressBar);

        ListView<RoleAnalysisAttributeAnalysisDto> attributeAnalysis = new ListView<>(ID_REPEATING_VIEW_FIRST_GROUP, getModel()) {
            @Override
            protected void populateItem(ListItem<RoleAnalysisAttributeAnalysisDto> item) {
                //TODO what is pathToMark?
                ProgressBarForm progressBarForm = new ProgressBarForm(
                        ID_ATTRIBUTE_STATISTICS,
                        item.getModel(),
                        pathToMark);

                progressBarForm.setOutputMarkupId(true);
                progressBarForm.add(new VisibleBehaviour(() -> item.getModelObject().isSelected()));
                item.add(progressBarForm);

//                RoleAnalysisAttributeAnalysisDto analysis = item.getModelObject();
//                ProgressBarForm progressBarForm = new ProgressBarForm(
//                        repeatingProgressBar.newChildId(),
//                        analysis,
//                        pathToMark);
//
//                progressBarForm.setOutputMarkupId(true);
//
//                if (totalBars >= MAX_VISIBLE_BARS) {
//                    progressBarForm.setVisible(false);
//                }
//
//                repeatingProgressBar.add(progressBarForm);
//                totalBars++;
            }
        };
        containerFirstGroup.add(attributeAnalysis);

//        for (RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis : toSort) {
//
//            ProgressBarForm progressBarForm = new ProgressBarForm(
//                    repeatingProgressBar.newChildId(),
//                    roleAnalysisAttributeAnalysis,
//                    pathToMark);
//
//            progressBarForm.setOutputMarkupId(true);
//
//            if (totalBars >= MAX_VISIBLE_BARS) {
//                progressBarForm.setVisible(false);
//            }
//
//            repeatingProgressBar.add(progressBarForm);
//            totalBars++;
//        }
//
//        if (totalBars > MAX_VISIBLE_BARS) {
//
//            add(createShowAllStatistics());
//        } else {
//            WebMarkupContainer showAllButton = new WebMarkupContainer(ID_SHOW_MORE_PROGRESS_BAR);
//            showAllButton.setVisible(false);
//            add(showAllButton);
//        }

    }

    @NotNull
    private WebMarkupContainer createContainer() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER_FIRST_GROUP);
        container.add(AttributeModifier.replace("class", "row p-0 card-body collapse show"));
        container.setOutputMarkupId(true);
        return container;
    }

    private AjaxLinkPanel createShowAllStatistics() {
//        new LoadableModel() {
//            @Override
//            protected Object load() {
//                if (!isResultExpanded) {
//                    return Model.of("Show more results");
//                }
//                return Model.of("Collapse results");
//            }
//        }

        return new AjaxLinkPanel(ID_SHOW_MORE_PROGRESS_BAR, createStringResource("RepeatingAttributeProgressForm.show.results." + (isResultExpanded ? "collapse" : "expand"))) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                setVisibilityForStatisticsPanels(ajaxRequestTarget);
            }
        };
    }

    private void setVisibilityForStatisticsPanels(AjaxRequestTarget ajaxRequestTarget) {
        isResultExpanded = !isResultExpanded;
        int counter = 0;
        RepeatingView repeatingProgressBar = (RepeatingView) get(createComponentPath(ID_CONTAINER_FIRST_GROUP, ID_REPEATING_VIEW_FIRST_GROUP));
        for (Component component : repeatingProgressBar) {
            counter++;
            if (!component.isVisible()) {
                component.setVisible(true);
            } else if (counter > MAX_VISIBLE_BARS) {
                if (component.isVisible()) {
                    component.setVisible(false);
                }
            }
        }
        ajaxRequestTarget.add(get(ID_CONTAINER_FIRST_GROUP));
    }
}
