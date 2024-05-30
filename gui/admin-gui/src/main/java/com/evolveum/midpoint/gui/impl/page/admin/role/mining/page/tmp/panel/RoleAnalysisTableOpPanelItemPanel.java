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
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
    private static final String ID_BODY_ITEM = "body-item";
    private static final String ID_FOOTER = "footer";
    private static final String ID_FOOTER_ITEM = "footer-item";

    public RoleAnalysisTableOpPanelItemPanel(String id, LoadableDetachableModel<OperationPanelModel> model) {
        super(id, model);
        initLayout();
    }

    LoadableDetachableModel<Boolean> isExpanded = new LoadableDetachableModel<>() {
        @Override
        protected Boolean load() {
            return false;
        }
    };
    WebMarkupContainer container;
    RepeatingView bodyItems;

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

        bodyItems = new RepeatingView(ID_BODY_ITEM);
        bodyItems.setOutputMarkupId(true);
        body.add(bodyItems);

        WebMarkupContainer footer = new WebMarkupContainer(ID_FOOTER);
        footer.setOutputMarkupId(true);
        container.add(footer);

        RepeatingView footerItems = new RepeatingView(ID_FOOTER_ITEM);
        footerItems.setOutputMarkupId(true);
        footer.add(footerItems);

        initHeaderItem(headerItems);
        initSubHeaderItem(subHeaderItems);
        initBodyItem(bodyItems);
        initFooterItem(footerItems);
    }

    protected void initHeaderItem(RepeatingView headerItems) {

    }

    protected void initSubHeaderItem(RepeatingView subHeaderItems) {
        addCompareButtonItem(subHeaderItems);
        addToggleModeItem(subHeaderItems);
    }

    protected void initBodyItem(RepeatingView bodyItems) {
        addPatternButtonItems(bodyItems);
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
        RoleAnalysisTableOpPanelItem compareButtonItem = new RoleAnalysisTableOpPanelItem(subHeaderItems.newChildId(), isExpanded()) {
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
        RoleAnalysisTableOpPanelItem toggleModeItem = new RoleAnalysisTableOpPanelItem(subHeaderItems.newChildId(), isExpanded()) {
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

    private void addPatternButtonItems(
            @NotNull RepeatingView bodyItems) {
        OperationPanelModel modelObject = RoleAnalysisTableOpPanelItemPanel.this.getModelObject();
        boolean candidateRoleView = modelObject.isCandidateRoleView();
        List<DetectedPattern> patterns = candidateRoleView ? modelObject.getCandidatesRoles() : modelObject.getPatterns();

        for (int i = 0; i < patterns.size(); i++) {
            DetectedPattern pattern = patterns.get(i);

            String formattedReductionFactorConfidence = String.format("%.0f", pattern.getMetric());
            String formattedItemConfidence = String.format("%.1f", pattern.getItemsConfidence());
            int patternIndex = i;

            int finalI = i;
            RoleAnalysisTableOpPanelItem bodyItem = new RoleAnalysisTableOpPanelItem(bodyItems.newChildId(), isExpanded()) {
                @Override
                protected void onConfigure() {
                    super.onConfigure();
                }

                @Override
                protected void performOnClick(AjaxRequestTarget ajaxRequestTarget) {
                    handlePatternClick(ajaxRequestTarget, modelObject, pattern);
                }

                @Override
                public String appendIconPanelCssClass() {
                    return null;
                }

                @Override
                public String appendIconPanelStyle() {
                    if (candidateRoleView) {
                        return "background-color: #DFF2E3;";
                    } else {
                        return "background-color: #E2EEF5;";
                    }
                }

                @Override
                public LoadableDetachableModel<String> getBackgroundColorStyle() {
                    List<DetectedPattern> selectedPatterns = modelObject.getSelectedPatterns();
                    for (DetectedPattern selectedPattern : selectedPatterns) {
                        if (selectedPattern.getId().equals(pattern.getId())) {
                            return new LoadableDetachableModel<>() {
                                @Override
                                protected String load() {
                                    return "background-color: " + selectedPattern.getAssociatedColor() + ";";
                                }
                            };
                        }
                    }
                    return super.getBackgroundColorStyle();
                }

                @Override
                public Component generateIconComponent(String idIcon) {
                    String iconClass = modelObject.isCandidateRoleView()
                            ? GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON
                            : GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON;
                    return new CompositedIconTextPanel(idIcon,
                            "fa-2x " + iconClass + " text-secondary",
                            finalI + 1 + "",
                            "text-secondary bg-white border border-white rounded-circle");
                }

                @Override
                public String replaceIconCssClass() {
                    return null;
                }

                @Override
                public @NotNull Component getDescriptionTitleComponent(String id) {
                    LoadableDetachableModel<String> model = new LoadableDetachableModel<>() {
                        @Override
                        protected String load() {
                            if (modelObject.isCandidateRoleView()) {
                                return "Candidate role" + (pattern.getIdentifier());
                            }

                            return "Pattern details #" + (patternIndex + 1);
                        }
                    };

                    RepeatingView repeatingView = new RepeatingView(id);
                    repeatingView.setOutputMarkupId(true);

                    Label label = new Label(repeatingView.newChildId(), model);
                    label.setOutputMarkupId(true);

                    AjaxIconButton iconButton = new AjaxIconButton(repeatingView.newChildId(),
                            Model.of("fa fa-list"), Model.of("")) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            if (modelObject.isCandidateRoleView()) {
                                String roleOid = pattern.getRoleOid();
                                dispatchToObjectDetailsPage(RoleType.class, roleOid, getPageBase(), true);
                            }

                            RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
                                    ((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of(pattern));
                            ((PageBase) getPage()).showMainPopup(component, target);
                        }
                    };
                    iconButton.setOutputMarkupId(true);
                    iconButton.add(AttributeAppender.replace("class", "p-0"));
                    repeatingView.add(label);
                    repeatingView.add(iconButton);
                    return repeatingView;
                }

                @Override
                protected void addDescriptionComponents() {
                    if(modelObject.isCandidateRoleView()){
                        Set<String> users = pattern.getUsers();
                        Set<String> roles = pattern.getRoles();
                        if(users != null && !users.isEmpty() && roles != null && !roles.isEmpty()){
                            appendIcon(GuiStyleConstants.CLASS_OBJECT_USER_ICON_COLORED,null);
                            appendText(" " + pattern.getUsers().size(), null);
                            appendText("users - ", null);
                            appendIcon(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON_COLORED,null);
                            appendText(" " + pattern.getRoles().size(), null);
                            appendText(" roles", null);
                        }
                    }else{
                        appendIcon("fe fe-assignment", "color: red;");
                        appendText(" " + formattedReductionFactorConfidence, null);
                        appendText("reduction - ", null);
                        appendIcon("fa fa-leaf", "color: green");
                        appendText(" " + formattedItemConfidence + "% ", null);
                        appendText(" confidence", null);
                    }
                }

                @Serial
                private static final long serialVersionUID = 1L;
            };

            bodyItem.setOutputMarkupId(true);
            bodyItem.add(AttributeAppender.replace("class", "btn btn-outline-dark border-0 d-flex align-self-stretch"));
            bodyItems.add(bodyItem);
        }
    }

    private @NotNull RoleAnalysisTableOpPanelItem addFooterButtonItem(
            @NotNull RepeatingView footerItems) {
        return new RoleAnalysisTableOpPanelItem(footerItems.newChildId(), isExpanded()) {
            @Override
            protected void performOnClick(AjaxRequestTarget ajaxRequestTarget) {
                handleExpandedStateClick(ajaxRequestTarget);
            }

            @Override
            public String appendIconPanelCssClass() {
                return "bg-white";
            }

            @Override
            public String replaceIconCssClass() {
                return isExpanded().getObject()
                        ? "fa-2x fa fa-align-justify text-secondary"
                        : "fa-2x fa fa-columns text-secondary";
            }

            @Override
            public Component getDescriptionTitleComponent(String id) {
                Label label = new Label(id, "Panel view");
                label.setOutputMarkupId(true);
                return label;
            }

            @Override
            protected void addDescriptionComponents() {
                appendText("Switch panel view.", null);
            }

            @Serial
            private static final long serialVersionUID = 1L;
        };
    }

    private void handlePatternClick(
            @NotNull AjaxRequestTarget ajaxRequestTarget,
            @NotNull OperationPanelModel modelObject,
            @NotNull DetectedPattern pattern) {
        if (!modelObject.isCompareMode()) {
            modelObject.addSelectedPatternSingleAllowed(pattern);
        } else {
            modelObject.addSelectedPattern(pattern);
        }
        ajaxRequestTarget.add(this);
        ajaxRequestTarget.add(container);
        onPatternSelectionPerform(ajaxRequestTarget);
    }

    public void onPatternSelectionPerform(@NotNull AjaxRequestTarget ajaxRequestTarget) {

    }

    private void handleCompareModeClick(
            @NotNull AjaxRequestTarget ajaxRequestTarget) {
        OperationPanelModel modelObject = RoleAnalysisTableOpPanelItemPanel.this.getModelObject();
        modelObject.clearSelectedPatterns();
        boolean compareMode = modelObject.isCompareMode();
        modelObject.setCompareMode(!compareMode);
        ajaxRequestTarget.add(this);
        ajaxRequestTarget.add(container);
    }

    private void handleCandidateRoleViewClick(
            @NotNull AjaxRequestTarget ajaxRequestTarget) {
        OperationPanelModel modelObject = RoleAnalysisTableOpPanelItemPanel.this.getModelObject();
        modelObject.clearSelectedPatterns();
        boolean candidateRoleView = modelObject.isCandidateRoleView();
        modelObject.setCandidateRoleView(!candidateRoleView);

        bodyItems.removeAll();
        initBodyItem(bodyItems);
        ajaxRequestTarget.add(this);
        ajaxRequestTarget.add(container);
    }

    private void handleExpandedStateClick(
            @NotNull AjaxRequestTarget ajaxRequestTarget) {
        isExpanded.setObject(!isExpanded.getObject());
        ajaxRequestTarget.add(this);
        ajaxRequestTarget.add(container);
    }

    private @NotNull String getCompareModeIconCssClass() {
        OperationPanelModel modelObject = RoleAnalysisTableOpPanelItemPanel.this.getModelObject();
        return modelObject.isCompareMode() ? "fa-2x fa fa-clone text-secondary" : "fa-2x fas fa-search text-secondary";
    }

    private @NotNull String getCandidateRoleViewIconCssClass() {
        OperationPanelModel modelObject = RoleAnalysisTableOpPanelItemPanel.this.getModelObject();
        return modelObject.isCandidateRoleView() ? "fa-2x fe fe-role text-secondary" : "fa-2x fa fa-cube text-secondary";
    }

    protected LoadableDetachableModel<Boolean> isExpanded() {
        return isExpanded;
    }

}
