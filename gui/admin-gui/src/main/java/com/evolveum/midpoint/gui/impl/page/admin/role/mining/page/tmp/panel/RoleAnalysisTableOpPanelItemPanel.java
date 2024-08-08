/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import static com.evolveum.midpoint.gui.impl.util.DetailsPageUtil.dispatchToObjectDetailsPage;

import java.io.Serial;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconTextPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.OperationPanelModel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class RoleAnalysisTableOpPanelItemPanel extends BasePanel<OperationPanelModel> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER = "header";
    private static final String ID_HEADER_ITEM = "header-item";
    private static final String ID_SUB_HEADER = "sub-header";
    private static final String ID_SUB_HEADER_ITEM = "sub-header-item";
    private static final String ID_BODY = "body";
    private static final String ID_FOOTER = "footer";
    private static final String ID_FOOTER_ITEM = "footer-item";

    private static final String ID_PATTERNS = "patterns";
    private static final String ID_PATTERN = "pattern";


    public RoleAnalysisTableOpPanelItemPanel(String id, LoadableDetachableModel<OperationPanelModel> model) {
        super(id, model);
        initLayout();
    }

    WebMarkupContainer container;

    private void initLayout() {
        container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        header.setOutputMarkupId(true);
        container.add(header);

        RepeatingView headerItems = new RepeatingView(ID_HEADER_ITEM);
        headerItems.setOutputMarkupId(true);
        header.add(headerItems);

        WebMarkupContainer subHeader = new WebMarkupContainer(ID_SUB_HEADER);
        subHeader.setOutputMarkupId(true);
        container.add(subHeader);

        RepeatingView subHeaderItems = new RepeatingView(ID_SUB_HEADER_ITEM);
        subHeaderItems.setOutputMarkupId(true);
        subHeader.add(subHeaderItems);

        WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.setOutputMarkupId(true);
        container.add(body);

        WebMarkupContainer footer = new WebMarkupContainer(ID_FOOTER);
        footer.setOutputMarkupId(true);
        container.add(footer);

        RepeatingView footerItems = new RepeatingView(ID_FOOTER_ITEM);
        footerItems.setOutputMarkupId(true);
        footer.add(footerItems);

        initHeaderItem(headerItems);
        initSubHeaderItem(subHeaderItems);
        initBodyItem(body);
        initFooterItem(footerItems);

        add(AttributeAppender.append("class", () -> getModelObject().isPanelExpanded() ? "" : "op-panel-collapsed"));
    }

    protected void initHeaderItem(RepeatingView headerItems) {

    }

    protected void initSubHeaderItem(RepeatingView subHeaderItems) {
        addCompareButtonItem(subHeaderItems);
        addToggleModeItem(subHeaderItems);
    }

    private IModel<List<DetectedPattern>> createPatternsModel() {
        return () -> {
            if (getModelObject().isOutlierView()) {
                return getModelObject().getOutlierPatterns();
            }
            if (getModelObject().isCandidateRoleView()) {
                return getModelObject().getCandidatesRoles();
            } else {
                return getModelObject().getPatterns();
            }
        };
    }

    protected void initBodyItem(WebMarkupContainer bodyItems) {
        ListView<DetectedPattern> patterns = new ListView<>(ID_PATTERNS, createPatternsModel()) {
            @Override
            protected void populateItem(ListItem<DetectedPattern> listItem) {

                RoleAnalysisTableOpPanelPatternItem bodyItem = new RoleAnalysisTableOpPanelPatternItem(ID_PATTERN, listItem.getModel()) {
                    @Override
                    protected void onConfigure() {
                        super.onConfigure();
                    }

                    @Override
                    protected void performOnClick(AjaxRequestTarget ajaxRequestTarget) {
                        handlePatternClick(ajaxRequestTarget, getModelObject());
                    }

                    @Contract(pure = true)
                    @Override
                    public @NotNull String appendIconPanelCssClass() {
                        return " elevation-1";
                    }

                    @Contract(pure = true)
                    @Override
                    public @NotNull String replaceIconCssStyle() {
                        return "width: 27px; height: 27px;";
                    }

                };
                listItem.add(bodyItem);
            }
        };
        bodyItems.add(patterns);
//        addPatternButtonItems(bodyItems);
    }

    private void initFooterItem(RepeatingView footerItems) {
        RoleAnalysisTableOpPanelItem footerComponent = addFooterButtonItem(footerItems);
        footerComponent.setOutputMarkupId(true);
        footerComponent.add(AttributeAppender.replace("class", "btn btn-outline-dark border-0 d-flex"
                + " align-self-stretch "));
//        footerComponent.add(AttributeAppender.append("style", "height: 60px;"));
        footerItems.add(footerComponent);
    }

    private void addCompareButtonItem(
            @NotNull RepeatingView subHeaderItems) {
        RoleAnalysisTableOpPanelItem compareButtonItem = new RoleAnalysisTableOpPanelItem(subHeaderItems.newChildId(), getModel()) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Contract(pure = true)
            @Override
            public @NotNull String appendIconPanelCssClass() {
                return "bg-white";
            }

            @Override
            protected void performOnClick(AjaxRequestTarget ajaxRequestTarget) {
                handleCompareModeClick(ajaxRequestTarget);
            }

            @Override
            public @NotNull String replaceIconCssClass() {
                return getCompareModeIconCssClass();
            }

            @Override
            public @NotNull Component getDescriptionTitleComponent(String id) {
                Label label = new Label(id, "Exploration view");
                label.setOutputMarkupId(true);
                return label;
            }

            @Override
            protected void addDescriptionComponents() {
                appendText("Switch explore mode", null);
            }
        };

        compareButtonItem.setOutputMarkupId(true);
        compareButtonItem.add(AttributeAppender.replace("class", "btn btn-outline-dark border-0 d-flex align-self-stretch"));
        subHeaderItems.add(compareButtonItem);
    }

    private void addToggleModeItem(
            @NotNull RepeatingView subHeaderItems) {
        RoleAnalysisTableOpPanelItem toggleModeItem = new RoleAnalysisTableOpPanelItem(subHeaderItems.newChildId(), getModel()) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Contract(pure = true)
            @Override
            public @NotNull String appendIconPanelCssClass() {
                return "bg-white";
            }

            @Override
            protected void performOnClick(AjaxRequestTarget ajaxRequestTarget) {
                handleCandidateRoleViewClick(ajaxRequestTarget);
            }

            @Override
            public @NotNull String replaceIconCssClass() {
                return getCandidateRoleViewIconCssClass();
            }

            @Override
            public @NotNull Component getDescriptionTitleComponent(String id) {
                Label label = new Label(id, "Object view");
                label.setOutputMarkupId(true);
                return label;
            }

            @Override
            protected void addDescriptionComponents() {
                appendText("Switch object view", null);
            }
        };

        toggleModeItem.setOutputMarkupId(true);
        toggleModeItem.add(AttributeAppender.replace("class", "btn btn-outline-dark border-0 d-flex align-self-stretch"));
        subHeaderItems.add(toggleModeItem);
    }

    private @NotNull RoleAnalysisTableOpPanelItem addFooterButtonItem(
            @NotNull RepeatingView footerItems) {
        return new RoleAnalysisTableOpPanelItem(footerItems.newChildId(), getModel()) {
            @Override
            protected void performOnClick(AjaxRequestTarget ajaxRequestTarget) {
                handleExpandedStateClick(ajaxRequestTarget);
            }

            @Override
            public String appendIconPanelCssClass() {
                return "bg-white";
            }

            @Override
            public Component getDescriptionTitleComponent(String id) {
                Label label = new Label(id, "Panel view"); //TODO localization
                label.setOutputMarkupId(true);
                return label;
            }

            @Override
            protected void addDescriptionComponents() {
                appendText("Switch panel view.", null);
            } // TODO localization

            @Serial
            private static final long serialVersionUID = 1L;
        };
    }

    public void onPatternSelectionPerform(@NotNull AjaxRequestTarget ajaxRequestTarget) {

    }

    private void handleCompareModeClick(
            @NotNull AjaxRequestTarget ajaxRequestTarget) {
        OperationPanelModel modelObject = getModelObject();
        modelObject.clearSelectedPatterns();
        boolean compareMode = modelObject.isCompareMode();
        modelObject.setCompareMode(!compareMode);
        ajaxRequestTarget.add(RoleAnalysisTableOpPanelItemPanel.this);
    }

    //TODO refresh whole table
    private void handleCandidateRoleViewClick(
            @NotNull AjaxRequestTarget ajaxRequestTarget) {
        OperationPanelModel modelObject = getModelObject();
        if (modelObject.isOutlierView()) {
            return;
        }
        boolean candidateRoleView = modelObject.isCandidateRoleView();
        modelObject.clearSelectedPatterns();
        modelObject.setCandidateRoleView(!candidateRoleView);
        ajaxRequestTarget.add(RoleAnalysisTableOpPanelItemPanel.this);
    }

    private void handleExpandedStateClick(
            @NotNull AjaxRequestTarget ajaxRequestTarget) {
        OperationPanelModel modelObject = getModelObject();
        boolean panelExpanded = modelObject.isPanelExpanded();
        modelObject.setPanelExpanded(!panelExpanded);
        ajaxRequestTarget.add(this);
        ajaxRequestTarget.add(container);
    }

    private @NotNull String getCompareModeIconCssClass() {
        OperationPanelModel modelObject = RoleAnalysisTableOpPanelItemPanel.this.getModelObject();
        return modelObject.isCompareMode() ? "fa-2x fa fa-clone text-secondary" : "fa-2x fas fa-search text-dark";
    }

    private @NotNull String getCandidateRoleViewIconCssClass() {
        OperationPanelModel modelObject = RoleAnalysisTableOpPanelItemPanel.this.getModelObject();
        if (modelObject.isOutlierView()) {
            return "fa-2x fa fa-user-circle text-danger";
        }
        return modelObject.isCandidateRoleView() ? "fa-2x fe fe-role text-secondary" : "fa-2x fa fa-cube text-dark";
    }

    private void handlePatternClick(
            @NotNull AjaxRequestTarget ajaxRequestTarget,
            @NotNull DetectedPattern pattern) {

        boolean patternSelected = pattern.isPatternSelected();
        OperationPanelModel modelObject = getModelObject();
        if (!modelObject.isCompareMode()) {
            modelObject.clearSelectedPatterns();
        } else {
            modelObject.removeFromPalette(pattern);
        }

        pattern.setPatternSelected(!patternSelected);
        ajaxRequestTarget.add(this);
        ajaxRequestTarget.add(container);
        onPatternSelectionPerform(ajaxRequestTarget);
    }

}
