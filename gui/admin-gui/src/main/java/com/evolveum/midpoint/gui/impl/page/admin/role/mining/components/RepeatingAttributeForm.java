/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributeAnalysisDto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.MembersDetailsPanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

/**
 * Represents a form containing attribute analysis results for role or user objects.
 * It displays collapsible sections for attributes and objects.
 * <p>
 * The form consists of:
 * <p> - Attribute section: Displays attribute analysis results with collapsible functionality.
 * <p> - Object section: Displays details of analyzed members with collapsible functionality.
 */
public class RepeatingAttributeForm extends BasePanel<String> {

    private static final String ID_CONTAINER_FIRST_GROUP = "containerFirstGroup";
    private static final String ID_REPEATING_VIEW_FIRST_GROUP = "progressBarFormFirstGroup";
    private static final String ID_OBJECT_TABLE = "objectTable";
    private static final String ID_COLLAPSE_TITLE_TABLE = "collapseTitleTable";
    private static final String ID_COLLAPSE_TITLE_ATTRIBUTE = "collapseTitleAttribute";
    private static final String ID_COLLAPSE_ICON_ATTRIBUTE = "collapseIconAttribute";
    private static final String ID_COLLAPSE_ICON_TABLE = "collapseIconTable";
    private static final String ID_FIRST_GROUP_CARD = "firstGroupCard";
    private static final String ID_SECOND_GROUP_CARD = "secondGroupCard";
    private static final String ID_SHOW_MORE_PROGRESS_BAR = "showMoreProgressForm";

    boolean isResultExpanded = false;

    public RepeatingAttributeForm(String id,
            @NotNull RoleAnalysisAttributeAnalysisResultType attributeAnalysisResult,
            @NotNull Set<String> objectsOid,
            @NotNull RoleAnalysisProcessModeType processMode) {
        super(id);

        WebMarkupContainer firstGroupCard = createContainer(ID_FIRST_GROUP_CARD);
        WebMarkupContainer secondGroupCard = createContainer(ID_SECOND_GROUP_CARD);
        add(firstGroupCard);
        add(secondGroupCard);

        AjaxLinkPanel collapseTitleAttribute = createAjaxTitleLink(ID_COLLAPSE_TITLE_ATTRIBUTE, ID_CONTAINER_FIRST_GROUP,
                ID_COLLAPSE_ICON_ATTRIBUTE, getAttributeTitle(processMode));
        AjaxLinkPanel collapseTitleTable = createAjaxTitleLink(ID_COLLAPSE_TITLE_TABLE, ID_OBJECT_TABLE,
                ID_COLLAPSE_ICON_TABLE, getTableTitle(processMode));
        firstGroupCard.add(collapseTitleAttribute);
        secondGroupCard.add(collapseTitleTable);

        AjaxButton collapseIconAttribute = createAjaxButton(ID_COLLAPSE_ICON_ATTRIBUTE, ID_CONTAINER_FIRST_GROUP);
        AjaxButton collapseIconTable = createAjaxButton(ID_COLLAPSE_ICON_TABLE, ID_OBJECT_TABLE);
        firstGroupCard.add(collapseIconAttribute);
        secondGroupCard.add(collapseIconTable);

        WebMarkupContainer containerFirstGroup = createContainer(ID_CONTAINER_FIRST_GROUP);
        if (isHide()) {
            containerFirstGroup.add(AttributeModifier.replace("class", "row card-body p-0 collapse"));
        } else {
            containerFirstGroup.add(AttributeModifier.replace("class", "row card-body p-0 collapse show"));
        }
        firstGroupCard.add(containerFirstGroup);

        RepeatingView repeatingProgressBar = new RepeatingView(ID_REPEATING_VIEW_FIRST_GROUP);
        repeatingProgressBar.setOutputMarkupId(true);
        containerFirstGroup.add(repeatingProgressBar);

        initProgressBars(attributeAnalysisResult, repeatingProgressBar, containerFirstGroup, getPathToMark());

        initTablePanel(objectsOid, processMode, secondGroupCard);
    }

    protected Set<String> getPathToMark() {
        return null;
    }

    public boolean isHide() {
        return false;
    }

    private void initProgressBars(
            @NotNull RoleAnalysisAttributeAnalysisResultType attributeAnalysisResult,
            @NotNull RepeatingView repeatingProgressBar,
            @NotNull WebMarkupContainer containerFirstGroup, Set<String> pathToMark) {
        int maxVisibleBars = 3;
        int totalBars = 0;

        List<RoleAnalysisAttributeAnalysisType> attributeAnalysis = attributeAnalysisResult.getAttributeAnalysis();
        List<RoleAnalysisAttributeAnalysisType> toSort = new ArrayList<>();
        for (RoleAnalysisAttributeAnalysisType roleAnalysisAttributeAnalysis : attributeAnalysis) {
            List<RoleAnalysisAttributeStatisticsType> attributeStatistics = roleAnalysisAttributeAnalysis.getAttributeStatistics();
            if (attributeStatistics != null && !attributeStatistics.isEmpty()) {
                toSort.add(roleAnalysisAttributeAnalysis);
            }
        }
        toSort.sort((analysis1, analysis2) -> {
            Double density1 = analysis1.getDensity();
            Double density2 = analysis2.getDensity();
            return Double.compare(density2, density1);
        });

        for (RoleAnalysisAttributeAnalysisType roleAnalysisAttributeAnalysis : toSort) {

            ProgressBarForm progressBarForm = new ProgressBarForm(
                    repeatingProgressBar.newChildId(),
                    () -> new RoleAnalysisAttributeAnalysisDto(roleAnalysisAttributeAnalysis, UserType.class),
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

    private void initTablePanel(
            @NotNull Set<String> objectsOid,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull WebMarkupContainer secondGroupCard) {

        if (isTableSupported()) {
            MembersDetailsPanel detailsPanel = new MembersDetailsPanel(ID_OBJECT_TABLE,
                    Model.of("Analyzed members details panel"), objectsOid, processMode);
            detailsPanel.setOutputMarkupId(true);
            detailsPanel.add(AttributeModifier.replace("class", "card-body collapse"));
            detailsPanel.add(AttributeModifier.replace("style", "display: none;"));
            secondGroupCard.add(detailsPanel);
        } else {
            WebMarkupContainer detailsPanel = new WebMarkupContainer(ID_OBJECT_TABLE);
            detailsPanel.setOutputMarkupId(true);
            detailsPanel.add(AttributeModifier.replace("class", "card-body collapse"));
            detailsPanel.add(AttributeModifier.replace("style", "display: none;"));
            secondGroupCard.add(detailsPanel);
            secondGroupCard.setVisible(false);
        }
    }

    @NotNull
    private WebMarkupContainer createContainer(@NotNull String idFirstGroupCard) {
        WebMarkupContainer container = new WebMarkupContainer(idFirstGroupCard);
        container.setOutputMarkupId(true);
        return container;
    }

    private @NotNull AjaxLinkPanel createAjaxTitleLink(
            @NotNull String id,
            @NotNull String collapseContainerId,
            @NotNull String collapseIconId,
            @NotNull IModel<String> title) {
        AjaxLinkPanel collapseTitleAttribute = new AjaxLinkPanel(id, title) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                MarkupContainer parent = this.getParent();
                target.appendJavaScript(
                        getCollapseIconScript(parent.get(collapseContainerId), parent.get(collapseIconId)));
            }
        };
        collapseTitleAttribute.setOutputMarkupId(true);
        return collapseTitleAttribute;
    }

    @NotNull
    private AjaxButton createAjaxButton(
            @NotNull String id,
            @NotNull String collapseContainerId) {
        AjaxButton collapseIconAttribute = new AjaxButton(id) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                target.appendJavaScript(getCollapseIconScript(this.getParent().get(collapseContainerId), this));
            }
        };
        if (isHide()) {
            collapseIconAttribute.add(new AttributeAppender("class", " fas fa-chevron-down"));
        } else {
            collapseIconAttribute.add(new AttributeAppender("class", " fas fa-chevron-up"));
        }
        collapseIconAttribute.setOutputMarkupId(true);
        return collapseIconAttribute;
    }

    protected boolean isTableSupported() {
        return true;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    private StringResourceModel getAttributeTitle(@NotNull RoleAnalysisProcessModeType processMode) {
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return createStringResource("RepeatingAttributeForm.title.collapseTitleAttributes.role");
        } else {
            return createStringResource("RepeatingAttributeForm.title.collapseTitleAttributes.user");
        }
    }

    private StringResourceModel getTableTitle(@NotNull RoleAnalysisProcessModeType processMode) {
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return createStringResource("RepeatingAttributeForm.title.collapseTitle.role");
        } else {
            return createStringResource("RepeatingAttributeForm.title.collapseTitle.user");
        }
    }

    private @NotNull String getCollapseIconScript(@NotNull Component webMarkupContainer, @NotNull Component collapseIconAttribute) {
        return "var collapseContainer = document.getElementById('" + webMarkupContainer.getMarkupId() + "');" +
                "var icon = document.getElementById('" + collapseIconAttribute.getMarkupId() + "');" +
                "if (collapseContainer.classList.contains('collapse') && collapseContainer.classList.contains('show')) {" +
                "    collapseContainer.classList.remove('show');" +
                "    collapseContainer.style.display = 'none';" +
                "    icon.classList.remove('fa-chevron-up');" +
                "    icon.classList.add('fa-chevron-down');" +
                "} else {" +
                "    collapseContainer.classList.add('show');" +
                "    collapseContainer.style.display = '';" +
                "    collapseContainer.style.pointerEvents = 'none !important';" +
                "    icon.classList.remove('fa-chevron-down');" +
                "    icon.classList.add('fa-chevron-up');" +
                "}";
    }

}
