/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatistics;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

/**
 * Represents a form containing attribute analysis results for role or user objects.
 * It displays collapsible sections for attributes and objects.
 * <p>
 * The form consists of:
 * <p> - Attribute section: Displays attribute analysis results with collapsible functionality.
 * <p> - Object section: Displays details of analyzed members with collapsible functionality.
 */
public class RepeatingAttributeProgressForm extends BasePanel<String> {
    private static final String ID_CONTAINER_FIRST_GROUP = "containerFirstGroup";
    private static final String ID_REPEATING_VIEW_FIRST_GROUP = "progressBarFormFirstGroup";
    private static final String ID_SHOW_MORE_PROGRESS_BAR = "showMoreProgressForm";

    boolean isResultExpanded = false;

    public RepeatingAttributeProgressForm(String id,
            @NotNull RoleAnalysisAttributeAnalysisResult attributeAnalysisResult) {
        super(id);

        WebMarkupContainer containerFirstGroup = createContainer();
        if (isHide()) {
            containerFirstGroup.add(AttributeModifier.replace("class", "row card-body collapse"));
        } else {
            containerFirstGroup.add(AttributeModifier.replace("class", "row card-body collapse show"));
        }
        add(containerFirstGroup);

        RepeatingView repeatingProgressBar = new RepeatingView(ID_REPEATING_VIEW_FIRST_GROUP);
        repeatingProgressBar.setOutputMarkupId(true);
        containerFirstGroup.add(repeatingProgressBar);

        initProgressBars(attributeAnalysisResult, repeatingProgressBar, containerFirstGroup, getPathToMark());
    }

    protected Set<String> getPathToMark() {
        return null;
    }

    public boolean isHide() {
        return false;
    }

    private void initProgressBars(
            @NotNull RoleAnalysisAttributeAnalysisResult attributeAnalysisResult,
            @NotNull RepeatingView repeatingProgressBar,
            @NotNull WebMarkupContainer containerFirstGroup, Set<String> pathToMark) {
        int maxVisibleBars = 3;
        int totalBars = 0;

        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = attributeAnalysisResult.getAttributeAnalysis();
        List<RoleAnalysisAttributeAnalysis> toSort = new ArrayList<>();
        for (RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis : attributeAnalysis) {
            List<RoleAnalysisAttributeStatistics> attributeStatistics = roleAnalysisAttributeAnalysis.getAttributeStatistics();
            if (attributeStatistics != null && !attributeStatistics.isEmpty()) {
                toSort.add(roleAnalysisAttributeAnalysis);
            }
        }
        toSort.sort((analysis1, analysis2) -> {
            Double density1 = analysis1.getDensity();
            Double density2 = analysis2.getDensity();
            return Double.compare(density2, density1);
        });

        for (RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis : toSort) {

            ProgressBarForm progressBarForm = new ProgressBarForm(
                    repeatingProgressBar.newChildId(),
                    roleAnalysisAttributeAnalysis,
                    pathToMark);

            progressBarForm.setOutputMarkupId(true);

            if (totalBars >= maxVisibleBars) {
                progressBarForm.setVisible(false);
            }

            repeatingProgressBar.add(progressBarForm);
            totalBars++;
        }

        if (totalBars > maxVisibleBars) {
            AjaxLinkPanel showAllButton = new AjaxLinkPanel(ID_SHOW_MORE_PROGRESS_BAR, new LoadableModel() {
                @Override
                protected Object load() {
                    if (!isResultExpanded) {
                        return Model.of("Show more results");
                    }
                    return Model.of("Collapse results");
                }
            }) {

                @Override
                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                    isResultExpanded = !isResultExpanded;
                    int counter = 0;
                    for (Component component : repeatingProgressBar) {
                        counter++;
                        if (!component.isVisible()) {
                            component.setVisible(true);
                        } else if (counter > maxVisibleBars) {
                            if (component.isVisible()) {
                                component.setVisible(false);
                            }
                        }
                    }
                    ajaxRequestTarget.add(containerFirstGroup);
                }
            };
            containerFirstGroup.add(showAllButton);
        } else {
            WebMarkupContainer showAllButton = new WebMarkupContainer(ID_SHOW_MORE_PROGRESS_BAR);
            showAllButton.setVisible(false);
            containerFirstGroup.add(showAllButton);
        }

    }

    @NotNull
    private WebMarkupContainer createContainer() {
        WebMarkupContainer container = new WebMarkupContainer(RepeatingAttributeProgressForm.ID_CONTAINER_FIRST_GROUP);
        container.setOutputMarkupId(true);
        return container;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

}
