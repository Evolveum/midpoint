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

public class RoleAnalysisAttributePanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CARD_CONTAINER = "card-container";
    private static final String ID_CARD_HEADER_TITLE = "cardHeaderTitle";
    private static final String ID_CARD_HEADER_REPEATING_BUTTONS = "analysisAttributesButtons";
    private static final String ID_CARD_BODY_COMPONENT = "cardBodyComponent";
    private static final String ID_CARD_BODY = "cardBody";

    RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult;
    RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult;

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

        cardBody.add(initCardBodyComponentChart());
    }

    protected String getCssClassForCardContainer() {
        return "card m-0";
    }

    private @NotNull RoleAnalysisAttributeResultChartPanel initCardBodyComponentChart() {

        RoleAnalysisAttributeResultChartPanel roleAnalysisChartPanel = new RoleAnalysisAttributeResultChartPanel(ID_CARD_BODY_COMPONENT) {
            @Override
            public @NotNull List<RoleAnalysisSimpleModel> prepareRoleAnalysisData() {
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

        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = roleAttributeAnalysisResult.getAttributeAnalysis();
        for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysis) {
            if (rolePath.contains(analysis.getItemPath())) {
                analysisAttributeToDisplay.getAttributeAnalysis().add(analysis.clone());
            }
        }

        List<RoleAnalysisAttributeAnalysis> userAttributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();
        for (RoleAnalysisAttributeAnalysis analysis : userAttributeAnalysis) {
            if (userPath.contains(analysis.getItemPath())) {
                analysisAttributeToDisplay.getAttributeAnalysis().add(analysis.clone());
            }
        }
        RepeatingAttributeProgressForm component = new RepeatingAttributeProgressForm(ID_CARD_BODY_COMPONENT, analysisAttributeToDisplay);
        component.setOutputMarkupId(true);
        return component;
    }

    private void initCardHeaderButtons(WebMarkupContainer cardContainer) {
        RepeatingView repeatingView = new RepeatingView(ID_CARD_HEADER_REPEATING_BUTTONS);
        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();
        for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysis) {
            String itemDescription = analysis.getItemPath();
            if (itemDescription != null && !itemDescription.isEmpty()) {
                itemDescription = Character.toUpperCase(itemDescription.charAt(0)) + itemDescription.substring(1);
            }
            String classObjectIcon = GuiStyleConstants.CLASS_OBJECT_USER_ICON + " object-user-color";

            int badge = analysis.getAttributeStatistics().size();
            initRepeatingChildButtons(classObjectIcon, repeatingView, itemDescription, badge, true);
        }

        for (RoleAnalysisAttributeAnalysis analysis : roleAttributeAnalysisResult.getAttributeAnalysis()) {
            String itemDescription = analysis.getItemPath();
            if (itemDescription != null && !itemDescription.isEmpty()) {
                itemDescription = Character.toUpperCase(itemDescription.charAt(0)) + itemDescription.substring(1);
            }
            String classObjectIcon = GuiStyleConstants.CLASS_OBJECT_ROLE_ICON + " object-role-color";

            int badge = analysis.getAttributeStatistics().size();
            initRepeatingChildButtons(classObjectIcon, repeatingView, itemDescription, badge, false);
        }

        repeatingView.setOutputMarkupId(true);
        cardContainer.add(repeatingView);
    }

    private void initRepeatingChildButtons(String classObjectIcon, RepeatingView repeatingView,
            String itemDescription, int badge, boolean isUser) {

        IconAjaxButtonBadge button = new IconAjaxButtonBadge(repeatingView.newChildId(), Model.of(itemDescription)) {
            @Override
            public void onClick(AjaxRequestTarget target) {

                List<String> userPath = new ArrayList<>();
                List<String> rolePath = new ArrayList<>();
                boolean tmp = isClicked;
                isClicked = !tmp;

                for (Component component : repeatingView) {
                    IconAjaxButtonBadge btn = (IconAjaxButtonBadge) component;
                    if (btn.isClicked()) {
                        if (btn.getIconCssClass().contains("user")) {
                            userPath.add(btn.getModelObject().toLowerCase());
                        } else {
                            rolePath.add(btn.getModelObject().toLowerCase());

                        }
                    }
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
}
