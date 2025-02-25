/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.OperationPanelModel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;

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

        add(AttributeModifier.append(CLASS_CSS, () -> getModelObject().isPanelExpanded() ? "" : "op-panel-collapsed"));
    }

    protected void initHeaderItem(RepeatingView headerItems) {
        //override
    }

    protected void initSubHeaderItem(RepeatingView subHeaderItems) {
        addCompareButtonItem(subHeaderItems);
        if (!getModelObject().isOutlierView()) {
            addToggleModeItem(subHeaderItems);
        }
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
                listItem.add(AttributeModifier.append(CLASS_CSS, "d-flex align-items-center rounded"));
            }
        };
        bodyItems.add(patterns);
//        addPatternButtonItems(bodyItems);
    }

    private void initFooterItem(RepeatingView footerItems) {
        RoleAnalysisTableOpPanelItem footerComponent = addFooterButtonItem(footerItems);
        footerComponent.setOutputMarkupId(true);
        footerItems.add(footerComponent);
    }

    private void addCompareButtonItem(
            @NotNull RepeatingView subHeaderItems) {

        IModel<String> operationDescription = new LoadableDetachableModel<>() {
            @Override
            protected @NotNull String load() {
                return getModelObject().isCompareMode() ? "Compare mode" : "Explore mode";
            }
        };

        RoleAnalysisTableOpPanelItem compareButtonItem = new RoleAnalysisTableOpPanelItem(subHeaderItems.newChildId(), getModel()) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Contract(pure = true)
            @Override
            public @NotNull String appendIconPanelCssClass() {
                return " bg-white";
            }

            @Override
            protected void performOnClick(AjaxRequestTarget ajaxRequestTarget) {
                handleCompareModeClick(ajaxRequestTarget, this);
            }

            @Override
            public @NotNull String replaceIconCssClass() {
                return getCompareModeIconCssClass();
            }

            @Override
            public @NotNull Component getDescriptionTitleComponent(String id) {
                Label label = new Label(id,
                        createStringResource("RoleAnalysisTableOpPanelItemPanel.explore"));
                label.setOutputMarkupId(true);
                return label;
            }

            @Override
            protected void addDescriptionComponents() {
                appendText(operationDescription, null);
            }
        };

        compareButtonItem.setOutputMarkupId(true);
        subHeaderItems.add(compareButtonItem);
    }

    private void addToggleModeItem(
            @NotNull RepeatingView subHeaderItems) {

        IModel<String> operationDescription = new LoadableDetachableModel<>() {
            @Override
            protected @NotNull String load() {
                return getModelObject().isCandidateRoleView()
                        ? createStringResource("RoleAnalysisTableOpPanelItemPanel.candidate.role.view").getString()
                        : createStringResource("RoleAnalysisTableOpPanelItemPanel.pattern.view").getString();
            }
        };

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
                Label label = new Label(id, createStringResource("RoleAnalysisTableOpPanelItemPanel.object.view"));
                label.setOutputMarkupId(true);
                return label;
            }

            @Override
            protected void addDescriptionComponents() {
                appendText(operationDescription, null);
            }
        };

        toggleModeItem.setOutputMarkupId(true);
        subHeaderItems.add(toggleModeItem);
    }

    private @NotNull RoleAnalysisTableOpPanelItem addFooterButtonItem(
            @NotNull RepeatingView footerItems) {

        IModel<String> operationDescription = new LoadableDetachableModel<>() {
            @Override
            protected @NotNull String load() {
                return getModelObject().isPanelExpanded()
                        ? createStringResource("RoleAnalysisTableOpPanelItemPanel.expanded").getString()
                        : createStringResource("RoleAnalysisTableOpPanelItemPanel.collapsed").getString();
            }
        };

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
                Label label = new Label(id,
                        createStringResource("RoleAnalysisTableOpPanelItemPanel.panel.view"));
                label.setOutputMarkupId(true);
                return label;
            }

            @Contract(pure = true)
            @Override
            public @NotNull String replaceIconCssClass() {
                if (getModelObject().isPanelExpanded()) {
                    return "fa-2x fa fa-window-maximize text-dark";
                }
                return "fa-2x fa fa-columns text-dark";
            }

            @Override
            protected void addDescriptionComponents() {
                appendText(operationDescription, null);
            } // TODO localization

            @Serial
            private static final long serialVersionUID = 1L;
        };
    }

    public void onPatternSelectionPerform(@NotNull AjaxRequestTarget ajaxRequestTarget) {
        //override
    }

    private void handleCompareModeClick(
            @NotNull AjaxRequestTarget ajaxRequestTarget, RoleAnalysisTableOpPanelItem components) {
        OperationPanelModel modelObject = getModelObject();
        modelObject.clearSelectedPatterns();
        boolean compareMode = modelObject.isCompareMode();
        modelObject.setCompareMode(!compareMode);
        ajaxRequestTarget.add(components);
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
