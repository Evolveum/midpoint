/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.web.component.dialog.Popupable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.RepeatingAttributeProgressForm;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisSimpleModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisAttributeResultChartPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResult;

public class RoleAnalysisAttributePanel extends BasePanel<String> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CARD_CONTAINER = "card-container";
    private static final String ID_CARD_HEADER_TITLE = "cardHeaderTitle";
    private static final String ID_CARD_HEADER_REPEATING_BUTTONS = "analysisAttributesButtons";
    private static final String ID_CARD_BODY_COMPONENT = "cardBodyComponent";
    private static final String ID_CARD_BODY = "cardBody";

    RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult;
    RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult;

    boolean isCompared = false;

    List<String> userPath = new ArrayList<>();
    List<String> rolePath = new ArrayList<>();

    LoadableDetachableModel<List<RoleAnalysisSimpleModel>> chartModel = new LoadableDetachableModel<>() {
        @Override
        protected @Nullable List<RoleAnalysisSimpleModel> load() {
            return null;
        }
    };

    public RoleAnalysisAttributePanel(
            @NotNull String id,
            @NotNull IModel<String> model,
            @Nullable RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult,
            @Nullable RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult) {
        super(id, model);
        this.roleAttributeAnalysisResult = roleAttributeAnalysisResult;
        this.userAttributeAnalysisResult = userAttributeAnalysisResult;

        chartModel.setObject(RoleAnalysisSimpleModel
                .getRoleAnalysisSimpleModel(roleAttributeAnalysisResult, userAttributeAnalysisResult));
        initLayout();
    }

    public RoleAnalysisAttributePanel(
            @NotNull String id,
            @NotNull IModel<String> model,
            @Nullable RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult,
            @Nullable RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult,
            @Nullable RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResultCompared,
            @Nullable RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResultCompared) {
        super(id, model);
        this.roleAttributeAnalysisResult = roleAttributeAnalysisResult;
        this.userAttributeAnalysisResult = userAttributeAnalysisResult;
        this.isCompared = true;
        chartModel.setObject(RoleAnalysisSimpleModel
                .getRoleAnalysisSimpleComparedModel(roleAttributeAnalysisResult, userAttributeAnalysisResult,
                        roleAttributeAnalysisResultCompared, userAttributeAnalysisResultCompared));
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer cardContainer = new WebMarkupContainer(ID_CARD_CONTAINER);
        cardContainer.setOutputMarkupId(true);
        cardContainer.add(AttributeAppender.replace("class", getCssClassForCardContainer()));
        add(cardContainer);

        initCardHeaderTitle(cardContainer);

        initCardHeaderButtons(cardContainer);

        WebMarkupContainer cardBody = new WebMarkupContainer(ID_CARD_BODY);
        cardBody.setOutputMarkupId(true);
        cardContainer.add(cardBody);

        if (!userPath.isEmpty() || !rolePath.isEmpty()) {
            cardBody.add(initCardBodyComponentRp(userPath, rolePath));
        } else {
            cardBody.add(initCardBodyComponentChart());
        }

    }

    protected String getCssClassForCardContainer() {
        return "card m-0";
    }

    private @NotNull RoleAnalysisAttributeResultChartPanel initCardBodyComponentChart() {
        RoleAnalysisAttributeResultChartPanel roleAnalysisChartPanel = new RoleAnalysisAttributeResultChartPanel(ID_CARD_BODY_COMPONENT) {

            @Override
            public boolean isCompare() {
                return isCompared;
            }

            @Override
            public @NotNull List<RoleAnalysisSimpleModel> prepareRoleAnalysisData() {
                if (chartModel == null || chartModel.getObject() == null) {
                    return new ArrayList<>();
                }

                return chartModel.getObject();
            }

            @Override
            protected String getChartContainerStyle() {
                if (RoleAnalysisAttributePanel.this.getChartContainerStyle() != null) {
                    return RoleAnalysisAttributePanel.this.getChartContainerStyle();
                }
                return super.getChartContainerStyle();
            }
        };
        roleAnalysisChartPanel.setOutputMarkupId(true);
        return roleAnalysisChartPanel;
    }

    private @NotNull RepeatingAttributeProgressForm initCardBodyComponentRp(
            @NotNull List<String> userPath,
            @NotNull List<String> rolePath) {

        RoleAnalysisAttributeAnalysisResult analysisAttributeToDisplay = new RoleAnalysisAttributeAnalysisResult();

        if (roleAttributeAnalysisResult != null) {
            List<RoleAnalysisAttributeAnalysis> attributeAnalysis = roleAttributeAnalysisResult.getAttributeAnalysis();
            for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysis) {
                if (rolePath.contains(analysis.getItemPath())) {
                    analysisAttributeToDisplay.getAttributeAnalysis().add(analysis.clone());
                }
            }
        }

        if (userAttributeAnalysisResult != null) {
            List<RoleAnalysisAttributeAnalysis> userAttributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();
            for (RoleAnalysisAttributeAnalysis analysis : userAttributeAnalysis) {
                if (userPath.contains(analysis.getItemPath().toLowerCase())) {
                    analysisAttributeToDisplay.getAttributeAnalysis().add(analysis.clone());
                }
            }
        }
        RepeatingAttributeProgressForm component = new RepeatingAttributeProgressForm(ID_CARD_BODY_COMPONENT, analysisAttributeToDisplay) {
            @Override
            protected Set<String> getPathToMark() {
                return RoleAnalysisAttributePanel.this.getPathToMark();
            }
        };
        component.setOutputMarkupId(true);
        return component;
    }

    public Set<String> getPathToMark() {
        return null;
    }

    private void initCardHeaderButtons(WebMarkupContainer cardContainer) {
        RepeatingView repeatingView = new RepeatingView(ID_CARD_HEADER_REPEATING_BUTTONS);

        List<String> userPath = new ArrayList<>();
        List<String> rolePath = new ArrayList<>();
        if (userAttributeAnalysisResult != null) {

            List<RoleAnalysisAttributeAnalysis> attributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();
            for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysis) {
                String itemDescription = analysis.getItemPath();
                if (itemDescription != null && !itemDescription.isEmpty()) {
                    itemDescription = Character.toUpperCase(itemDescription.charAt(0)) + itemDescription.substring(1);
                    userPath.add(itemDescription.toLowerCase());
                }
                String classObjectIcon = GuiStyleConstants.CLASS_OBJECT_USER_ICON + " object-user-color";

                int badge = analysis.getAttributeStatistics().size();
                initRepeatingChildButtons(classObjectIcon, repeatingView, itemDescription, badge);
            }
        }

        if (roleAttributeAnalysisResult != null) {
            for (RoleAnalysisAttributeAnalysis analysis : roleAttributeAnalysisResult.getAttributeAnalysis()) {
                String itemDescription = analysis.getItemPath();
                if (itemDescription != null && !itemDescription.isEmpty()) {
                    itemDescription = Character.toUpperCase(itemDescription.charAt(0)) + itemDescription.substring(1);
                    rolePath.add(itemDescription.toLowerCase());
                }
                String classObjectIcon = GuiStyleConstants.CLASS_OBJECT_ROLE_ICON + " object-role-color";

                int badge = analysis.getAttributeStatistics().size();
                initRepeatingChildButtons(classObjectIcon, repeatingView, itemDescription, badge);
            }
        }

        if (!userPath.isEmpty() || !rolePath.isEmpty()) {
            initOveralResultButton(
                    repeatingView, userPath, rolePath);
        }

        repeatingView.setOutputMarkupId(true);
        cardContainer.add(repeatingView);
    }

    private void initRepeatingChildButtons(String classObjectIcon, RepeatingView repeatingView,
            String itemDescription, int badge) {

        IconAjaxButtonBadge button = new IconAjaxButtonBadge(repeatingView.newChildId(), Model.of(itemDescription), true) {

            @Override
            protected void onLoadComponent() {
                add(AttributeAppender.replace("class", isClicked
                        ? getButtonCssClass() + " active "
                        : getButtonCssClass()));

                if (this.isClicked()) {
                    if (this.getIconCssClass().contains("user")) {
                        userPath.add(this.getModelObject().toLowerCase());
                    } else {
                        rolePath.add(this.getModelObject().toLowerCase());
                    }
                }
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                boolean tmp = isClicked;
                isClicked = !tmp;

                if (this.isClicked()) {
                    if (this.getIconCssClass().contains("user")) {
                        userPath.add(this.getModelObject().toLowerCase());
                    } else {
                        rolePath.add(this.getModelObject().toLowerCase());
                    }
                } else {
                    if (this.getIconCssClass().contains("user")) {
                        userPath.remove(this.getModelObject().toLowerCase());
                    } else {
                        rolePath.remove(this.getModelObject().toLowerCase());
                    }
                }

                for (Component component : repeatingView) {
                    IconAjaxButtonBadge btn = (IconAjaxButtonBadge) component;
                    String buttonTitle = btn.getModelObject().toLowerCase();
                    if (!buttonTitle.equals("overal result")) {
                        continue;
                    }
                    boolean clicked = btn.isClicked();
                    if (clicked && this.isClicked()) {
                        btn.setClicked(false);
                    } else if (!clicked && !this.isClicked() && userPath.isEmpty() && rolePath.isEmpty()) {
                        btn.setClicked(true);
                    }

                    btn.add(AttributeAppender.replace("class", btn.isClicked()
                            ? getButtonCssClass() + " active "
                            : getButtonCssClass()));

                    target.add(btn);
                }

                switchCardBodyComponent(target, userPath, rolePath);

                this.add(AttributeAppender.replace("class", isClicked
                        ? getButtonCssClass() + " active "
                        : getButtonCssClass()));
                target.add(this);
            }

            @Override
            public Integer getBadgeValue() {
                return badge;
            }

            @Override
            public String getIconCssClass() {
                return classObjectIcon;
            }

        };
        button.setOutputMarkupId(true);
        button.add(AttributeAppender.append("class", getButtonCssClass()));
        repeatingView.add(button);
    }

    private void initOveralResultButton(RepeatingView repeatingView,
            List<String> userPath,
            List<String> rolePath) {

        IconAjaxButtonBadge button = new IconAjaxButtonBadge(repeatingView.newChildId(), Model.of("Overal result"), false) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                boolean tmp = isClicked;
                isClicked = !tmp;

                RoleAnalysisAttributePanel.this.userPath.clear();
                RoleAnalysisAttributePanel.this.rolePath.clear();
                if (this.isClicked()) {

                    for (Component component : repeatingView) {
                        IconAjaxButtonBadge btn = (IconAjaxButtonBadge) component;
                        String buttonTitle = btn.getModelObject().toLowerCase();
                        if (buttonTitle.equals("overal result")) {
                            continue;
                        }
                        btn.setClicked(false);
                        btn.add(AttributeAppender.replace("class", getButtonCssClass()));
                        target.add(btn);
                    }
                } else {
                    RoleAnalysisAttributePanel.this.userPath.addAll(userPath);
                    RoleAnalysisAttributePanel.this.rolePath.addAll(rolePath);
                    for (Component component : repeatingView) {
                        IconAjaxButtonBadge btn = (IconAjaxButtonBadge) component;
                        String buttonTitle = btn.getModelObject().toLowerCase();
                        if (buttonTitle.equals("overal result")) {
                            continue;
                        }
                        btn.setClicked(true);
                        btn.add(AttributeAppender.replace("class", getButtonCssClass() + " active "));
                        target.add(btn);
                    }
                }

                switchCardBodyComponent(target, RoleAnalysisAttributePanel.this.userPath, RoleAnalysisAttributePanel.this.rolePath);

                this.add(AttributeAppender.replace("class", isClicked
                        ? getButtonCssClass() + " active "
                        : getButtonCssClass()));
                target.add(this);
            }

            @Override
            public Integer getBadgeValue() {
                return userPath.size() + rolePath.size();
            }

            @Override
            public String getIconCssClass() {
                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON;
            }

        };
        button.setOutputMarkupId(true);
        button.add(AttributeAppender.append("class", getButtonCssClass()));
        repeatingView.add(button);
    }

    private void switchCardBodyComponent(
            @NotNull AjaxRequestTarget target,
            @NotNull List<String> userPath,
            @NotNull List<String> rolePath) {
        Component component = RoleAnalysisAttributePanel.this.get(createComponentPath(ID_CARD_CONTAINER, ID_CARD_BODY, ID_CARD_BODY_COMPONENT));
        if (!userPath.isEmpty() || !rolePath.isEmpty()) {
            component.replaceWith(initCardBodyComponentRp(userPath, rolePath));
            component.setOutputMarkupId(true);
        } else {
            component.replaceWith(initCardBodyComponentChart());
        }
        target.add(RoleAnalysisAttributePanel.this.get(createComponentPath(ID_CARD_CONTAINER, ID_CARD_BODY, ID_CARD_BODY_COMPONENT)).getParent());
    }

    private void initCardHeaderTitle(WebMarkupContainer cardContainer) {
        Label label = new Label(ID_CARD_HEADER_TITLE, getModel());
        label.setOutputMarkupId(true);
        cardContainer.add(label);
    }

    public String getIconCssClass() {
        return "";
    }

    public String getButtonCssClass() {
        return "d-flex align-items-center gap-2 btn btn-sm btn-outline-primary rounded-pill";
    }

    @Contract(pure = true)
    protected @Nullable String getChartContainerStyle() {
        return null;
    }

    @Override
    public int getWidth() {
        return 70;
    }

    @Override
    public int getHeight() {
        return 70;
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
        return Model.of("Analysis");
    }

    @Override
    public Component getContent() {
        return this;
    }
}
