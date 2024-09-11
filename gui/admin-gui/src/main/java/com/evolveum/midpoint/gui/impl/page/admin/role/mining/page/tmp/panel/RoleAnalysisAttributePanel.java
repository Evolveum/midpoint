/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
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

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;

public class RoleAnalysisAttributePanel extends BasePanel<String> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CARD_CONTAINER = "card-container";
    private static final String ID_CARD_HEADER_REPEATING_BUTTONS = "analysisAttributesButtons";
    private static final String ID_CARD_BODY_COMPONENT = "cardBodyComponent";

    private static final String ID_CARD_HEADER_CONTAINER = "cardHeaderContainer";
    private static final String ID_CARD_HEADER_TITLE = "cardHeaderTitle";

    private static final String ID_CARD_BODY = "cardBody";

    private static final String STATUS_ACTIVE = " active ";

    RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult;
    RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult;

    boolean isCompared = false;

    transient List<String> userPath = new ArrayList<>();
    transient List<String> rolePath = new ArrayList<>();

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
        cardContainer.add(AttributeModifier.replace(CLASS_CSS, getCssClassForCardContainer()));
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
        return Collections.emptySet();
    }

    private void initCardHeaderButtons(WebMarkupContainer cardContainer) {
        RepeatingView repeatingView = new RepeatingView(ID_CARD_HEADER_REPEATING_BUTTONS);

        List<String> localUserPath = new ArrayList<>();
        List<String> localRolePath = new ArrayList<>();
        if (userAttributeAnalysisResult != null) {

            List<RoleAnalysisAttributeAnalysis> attributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();
            for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysis) {
                String itemDescription = resolveItemDescription(analysis, localUserPath);
                String classObjectIcon = GuiStyleConstants.CLASS_OBJECT_USER_ICON;

                int badge = analysis.getAttributeStatistics().size();
                initRepeatingChildButtons(classObjectIcon, repeatingView, itemDescription, badge);
            }
        }

        if (roleAttributeAnalysisResult != null) {
            for (RoleAnalysisAttributeAnalysis analysis : roleAttributeAnalysisResult.getAttributeAnalysis()) {
                String itemDescription = resolveItemDescription(analysis, localRolePath);
                String classObjectIcon = GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;

                int badge = analysis.getAttributeStatistics().size();
                initRepeatingChildButtons(classObjectIcon, repeatingView, itemDescription, badge);
            }
        }

        if (!localUserPath.isEmpty() || !localRolePath.isEmpty()) {
            initOveralResultButton(
                    repeatingView, localUserPath, localRolePath);
        }

        repeatingView.setOutputMarkupId(true);
        cardContainer.add(repeatingView);
    }

    private static @Nullable String resolveItemDescription(
            @NotNull RoleAnalysisAttributeAnalysis analysis,
            List<String> localUserPath) {
        String itemDescription = analysis.getItemPath();
        if (itemDescription != null && !itemDescription.isEmpty()) {
            itemDescription = Character.toUpperCase(itemDescription.charAt(0)) + itemDescription.substring(1);
            localUserPath.add(itemDescription.toLowerCase());
        }
        return itemDescription;
    }

    private void initRepeatingChildButtons(String classObjectIcon, @NotNull RepeatingView repeatingView,
            String itemDescription, int badge) {

        IconAjaxButtonBadge button = new IconAjaxButtonBadge(repeatingView.newChildId(), Model.of(itemDescription), true) {

            @Override
            protected void onLoadComponent() {
                add(AttributeModifier.replace(CLASS_CSS, isClicked
                        ? getButtonCssClass() + STATUS_ACTIVE
                        : getButtonCssClass()));

                if (this.isClicked()) {
                    if (classObjectIcon.contains("user")) {
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
                    if (classObjectIcon.contains("user")) {
                        userPath.add(this.getModelObject().toLowerCase());
                    } else {
                        rolePath.add(this.getModelObject().toLowerCase());
                    }
                } else {
                    if (classObjectIcon.contains("user")) {
                        userPath.remove(this.getModelObject().toLowerCase());
                    } else {
                        rolePath.remove(this.getModelObject().toLowerCase());
                    }
                }

                for (Component component : repeatingView) {
                    IconAjaxButtonBadge btn = (IconAjaxButtonBadge) component;
                    if (!btn.isUnique) {
                        continue;
                    }
                    boolean clicked = btn.isClicked();
                    if (clicked && this.isClicked()) {
                        btn.setClicked(false);
                    } else if (!clicked && !this.isClicked() && userPath.isEmpty() && rolePath.isEmpty()) {
                        btn.setClicked(true);
                    }

                    btn.add(AttributeModifier.replace(CLASS_CSS, btn.isClicked()
                            ? getButtonCssClass() + STATUS_ACTIVE
                            : getButtonCssClass()));

                    target.add(btn);
                }

                switchCardBodyComponent(target, userPath, rolePath);

                this.add(AttributeModifier.replace(CLASS_CSS, isClicked
                        ? getButtonCssClass() + STATUS_ACTIVE
                        : getButtonCssClass()));
                target.add(this);
            }

            @Override
            public String getBadgeValue() {
                return "(" + badge + ")";
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getBadgeCssClass() {
                return "ml-auto mr-1";
            }

            @Override
            public @NotNull String getIconCssClass() {
                if (this.isClicked()) {
                    return "fa fa-check ml-1";
                } else {
                    return classObjectIcon + " ml-1";
                }
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getLabelCssClass() {
                return " pill-label";
            }

        };
        button.setOutputMarkupId(true);
        repeatingView.add(button);
    }

    private void initOveralResultButton(@NotNull RepeatingView repeatingView,
            List<String> userPath,
            List<String> rolePath) {

        IconAjaxButtonBadge button = new IconAjaxButtonBadge(repeatingView.newChildId(), createStringResource(
                "RoleAnalysisAttributePanel.title.overal.result"), false) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                boolean tmp = isClicked;
                isClicked = !tmp;

                RoleAnalysisAttributePanel.this.userPath.clear();
                RoleAnalysisAttributePanel.this.rolePath.clear();
                if (this.isClicked()) {

                    for (Component component : repeatingView) {
                        IconAjaxButtonBadge btn = (IconAjaxButtonBadge) component;
                        if (btn.isUnique()) {
                            continue;
                        }
                        btn.setClicked(false);
                        btn.add(AttributeModifier.replace(CLASS_CSS, getButtonCssClass()));
                        target.add(btn);
                    }
                } else {
                    RoleAnalysisAttributePanel.this.userPath.addAll(userPath);
                    RoleAnalysisAttributePanel.this.rolePath.addAll(rolePath);
                    for (Component component : repeatingView) {
                        IconAjaxButtonBadge btn = (IconAjaxButtonBadge) component;
                        if (btn.isUnique()) {
                            continue;
                        }
                        btn.setClicked(true);
                        btn.add(AttributeModifier.replace(CLASS_CSS, getButtonCssClass() + STATUS_ACTIVE));
                        target.add(btn);
                    }
                }

                switchCardBodyComponent(target, RoleAnalysisAttributePanel.this.userPath, RoleAnalysisAttributePanel.this.rolePath);

                this.add(AttributeModifier.replace(CLASS_CSS, isClicked
                        ? getButtonCssClass() + STATUS_ACTIVE
                        : getButtonCssClass()));
                target.add(this);
            }

            @Override
            public @NotNull String getBadgeValue() {
                int count = userPath.size() + rolePath.size();
                return "(" + count + ")";
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getBadgeCssClass() {
                return "ml-auto";
            }

            @Override
            public String getIconCssClass() {
                if (this.isClicked()) {
                    return "fa fa-check ml-1";
                } else {
                    return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON + " ml-1";
                }
            }

        };
        button.setOutputMarkupId(true);
        button.setUnique(true);
        button.add(AttributeModifier.replace(CLASS_CSS, getButtonCssClass()));
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

    private void initCardHeaderTitle(@NotNull WebMarkupContainer cardContainer) {
        WebMarkupContainer cardHeaderContainer = new WebMarkupContainer(ID_CARD_HEADER_CONTAINER);
        cardHeaderContainer.setOutputMarkupId(true);
        cardHeaderContainer.add(new VisibleBehaviour(this::isCardTitleVisible));

        cardContainer.add(cardHeaderContainer);

        IconWithLabel label = new IconWithLabel(ID_CARD_HEADER_TITLE, getModel()) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-area-chart";
            }
        };
        label.setOutputMarkupId(true);
        cardHeaderContainer.add(label);
    }

    public String getIconCssClass() {
        return "";
    }

    public String getButtonCssClass() {
        return "d-flex align-items-center gap-1 btn btn-sm btn-pill rounded-pill";
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

    protected boolean isCardTitleVisible() {
        return false;
    }

}
