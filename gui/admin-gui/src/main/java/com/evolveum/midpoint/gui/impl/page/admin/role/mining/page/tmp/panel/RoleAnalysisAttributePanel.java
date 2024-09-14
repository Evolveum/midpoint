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

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.RepeatingAttributeProgressForm;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisSimpleModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisAttributeResultChartPanel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;

public class RoleAnalysisAttributePanel extends BasePanel<RoleAnalysisAttributesDto> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CARD_CONTAINER = "card-container";

    private static final String ID_CARD_HEADER_REPEATING_BUTTONS = "analysisAttributesButtons";
    private static final String ID_ATTRIBUTE_HEADER = "attributeHeader";

    private static final String ID_CARD_BODY_COMPONENT = "cardBodyComponent";
    private static final String ID_OVERALL_CARD_BODY_COMPONENT = "overallCardBodyComponent";

    private static final String ID_CARD_HEADER_CONTAINER = "cardHeaderContainer";

    private static final String ID_CARD_HEADER_TITLE = "cardHeaderTitle";

    private static final String ID_CARD_BODY = "cardBody";

    private static final String STATUS_ACTIVE = " active ";
    private static final String ID_OVERAL_HEDAER = "overallHeader";


    public RoleAnalysisAttributePanel(String id, IModel<RoleAnalysisAttributesDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private boolean showPerAttributeStatistics = true;

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

        RepeatingAttributeProgressForm repeatingAttributeProgressForm = initCardBodyComponentRp();
        repeatingAttributeProgressForm.setOutputMarkupId(true);
        repeatingAttributeProgressForm.add(new VisibleBehaviour(() -> showPerAttributeStatistics));
        cardBody.add(repeatingAttributeProgressForm);

        RoleAnalysisAttributeResultChartPanel overallStatus = initCardBodyComponentChart();
        overallStatus.setOutputMarkupId(true);
        overallStatus.add(new VisibleBehaviour(() -> !showPerAttributeStatistics));
        cardBody.add(overallStatus);

    }

    protected String getCssClassForCardContainer() {
        return "card m-0";
    }

    private @NotNull RoleAnalysisAttributeResultChartPanel initCardBodyComponentChart() {
        RoleAnalysisAttributeResultChartPanel roleAnalysisChartPanel = new RoleAnalysisAttributeResultChartPanel(ID_OVERALL_CARD_BODY_COMPONENT) {

            @Override
            public boolean isCompare() {
                return RoleAnalysisAttributePanel.this.getModelObject().isCompared();
            }

            @Override
            public @NotNull List<RoleAnalysisSimpleModel> prepareRoleAnalysisData() {
                return RoleAnalysisAttributePanel.this.getModelObject().getChartModel();
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

    private @NotNull RepeatingAttributeProgressForm initCardBodyComponentRp() {


        RepeatingAttributeProgressForm component = new RepeatingAttributeProgressForm(ID_CARD_BODY_COMPONENT, new PropertyModel<>(getModel(), RoleAnalysisAttributesDto.F_ATTRIBUTES_MODEL)) {
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

        ListView<RoleAnalysisAttributeAnalysisDto> attributeStatisticsHeader = new ListView<>(ID_CARD_HEADER_REPEATING_BUTTONS, new PropertyModel<>(getModel(), RoleAnalysisAttributesDto.F_ATTRIBUTES_MODEL)) {

            @Override
            protected void populateItem(ListItem<RoleAnalysisAttributeAnalysisDto> item) {
                IconAjaxButtonBadgeNew header = initAttributeHeaderButton(ID_ATTRIBUTE_HEADER, item.getModel());
                header.setOutputMarkupId(true);
                item.add(header);
            }
        };
        cardContainer.add(attributeStatisticsHeader);

        cardContainer.add(initOverallResultButton());
    }

    private IconAjaxButtonBadgeNew initAttributeHeaderButton(String id, IModel<RoleAnalysisAttributeAnalysisDto> roleAnalysisAttributeModel) {

        return new IconAjaxButtonBadgeNew(id, roleAnalysisAttributeModel) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                showPerAttributeStatistics = true;
                getModelObject().setSelected(!getModelObject().isSelected());
                target.add(RoleAnalysisAttributePanel.this);
            }
        };
    }

    private IconAjaxButtonBadge initOverallResultButton() {

        IconAjaxButtonBadge button = new IconAjaxButtonBadge(ID_OVERAL_HEDAER, createStringResource(
                "RoleAnalysisAttributePanel.title.overal.result"), false) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showPerAttributeStatistics = !showPerAttributeStatistics;
                for (RoleAnalysisAttributeAnalysisDto attribute : RoleAnalysisAttributePanel.this.getModelObject().getAttributesModel()) {
                    attribute.setSelected(showPerAttributeStatistics);
                }


//                boolean tmp = isClicked;
//                isClicked = !tmp;

//                RoleAnalysisAttributePanel.this.userPath.clear();
//                RoleAnalysisAttributePanel.this.rolePath.clear();
//                if (this.isClicked()) {
//
//                    for (Component component : repeatingView) {
//                        IconAjaxButtonBadge btn = (IconAjaxButtonBadge) component;
//                        if (btn.isUnique()) {
//                            continue;
//                        }
//                        btn.setClicked(false);
//                        btn.add(AttributeModifier.replace(CLASS_CSS, getButtonCssClass()));
//                        target.add(btn);
//                    }
//                } else {
////                    RoleAnalysisAttributePanel.this.userPath.addAll(userPath);
////                    RoleAnalysisAttributePanel.this.rolePath.addAll(rolePath);
//                    for (Component component : repeatingView) {
//                        IconAjaxButtonBadge btn = (IconAjaxButtonBadge) component;
//                        if (btn.isUnique()) {
//                            continue;
//                        }
//                        btn.setClicked(true);
//                        btn.add(AttributeModifier.replace(CLASS_CSS, getButtonCssClass() + STATUS_ACTIVE));
//                        target.add(btn);
//                    }
//                }

//                switchCardBodyComponent(target);

                this.add(AttributeModifier.replace(CLASS_CSS, isClicked
                        ? getButtonCssClass() + STATUS_ACTIVE
                        : getButtonCssClass()));
                target.add(RoleAnalysisAttributePanel.this);
            }

            @Override
            public @NotNull String getBadgeValue() {
                int count = RoleAnalysisAttributePanel.this.getModelObject().getAttributesModel().size();
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
        return button;
//        repeatingView.add(button);
    }

//    private void switchCardBodyComponent(
//            @NotNull AjaxRequestTarget target) {
////        Component component = RoleAnalysisAttributePanel.this.get(createComponentPath(ID_CARD_CONTAINER, ID_CARD_BODY, ID_CARD_BODY_COMPONENT));
////        if (showPerAttributeStatistics) {
////            component.replaceWith(initCardBodyComponentRp());
////            component.setOutputMarkupId(true);
////        } else {
////            component.replaceWith(initCardBodyComponentChart());
////        }
////        target.add(RoleAnalysisAttributePanel.this.get(createComponentPath(ID_CARD_CONTAINER, ID_CARD_BODY, ID_CARD_BODY_COMPONENT)).getParent());
//    }

    private void initCardHeaderTitle(@NotNull WebMarkupContainer cardContainer) {
        WebMarkupContainer cardHeaderContainer = new WebMarkupContainer(ID_CARD_HEADER_CONTAINER);
        cardHeaderContainer.setOutputMarkupId(true);
        cardHeaderContainer.add(new VisibleBehaviour(this::isCardTitleVisible));

        cardContainer.add(cardHeaderContainer);

        IconWithLabel label = new IconWithLabel(ID_CARD_HEADER_TITLE, new PropertyModel<>(getModel(), RoleAnalysisAttributesDto.F_DISPLAY)) {
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
