/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.component;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.component.RoleAnalysisDonutChartUtils.createDoughnutChartConfigFor;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.table.ChartedHeaderDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.model.IdentifyWidgetItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.wicket.chartjs.DoughnutChartConfiguration;

public class RoleAnalysisIdentifyWidgetPanel extends BasePanel<List<IdentifyWidgetItem>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CARD = "card";
    private static final String ID_CARD_HEADER = "card-header";
    private static final String ID_HEADER_ITEM = "header-item";
    private static final String ID_CARD_BODY = "card-body";
    private static final String ID_BODY_ITEM_CONTAINER = "body-item-container";
    private static final String ID_CARD_FOOTER = "card-footer";
    private static final String ID_FOOTER_ITEM = "footer-item";

    private static final String ID_BODY_HEADER_PANEL_CONTAINER = "body-header-panel-container";
    private static final String ID_BODY_HEADER_PANEL = "body-header-panel";

    private static final String ID_IMAGE = "image";
    private static final String ID_TITLE = "title";
    private static final String ID_VALUE = "value";

    private static final String ID_VALUE_TITLE = "value-title";
    private static final String ID_SCORE = "value-score";
    private static final String ID_SCORE_ACTION = "value-action";

    IModel<String> title;

    public RoleAnalysisIdentifyWidgetPanel(String id, IModel<String> title, IModel<List<IdentifyWidgetItem>> model) {
        super(id, model);
        this.title = title;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "div");
    }

    private void initLayout() {
        add(AttributeModifier.append(CLASS_CSS, initDefaultCssClass()));

        IModel<String> titleModel = getTitleModel();

        WebMarkupContainer card = new WebMarkupContainer(ID_CARD);
        card.add(AttributeModifier.append(CLASS_CSS, " m-0"));
        card.setOutputMarkupId(true);
        add(card);

        initHeaderItems(card, titleModel);

        initBodyItems(card);

        initFooterItems(card);

    }

    private void initFooterItems(@NotNull WebMarkupContainer card) {
        WebMarkupContainer footerContainer = new WebMarkupContainer(ID_CARD_FOOTER);
        footerContainer.setOutputMarkupId(true);
        card.add(footerContainer);

        AjaxLinkPanel footerItem = new AjaxLinkPanel(ID_FOOTER_ITEM, getFooterButtonLabelModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickFooter(target);
            }
        };

        footerItem.setOutputMarkupId(true);
        footerContainer.add(footerItem);
    }

    protected IModel<String> getFooterButtonLabelModel() {
        return createStringResource("RoleAnalysisIdentifyWidgetPanel.button.label");
    }

    protected void onClickFooter(AjaxRequestTarget target) {
        // do nothing
    }

    //TODO
    protected LoadableModel<List<ChartedHeaderDto<DoughnutChartConfiguration>>> getChartedHeaderDtoModel() {
        return new LoadableModel<>() {
            @Override
            protected List<ChartedHeaderDto<DoughnutChartConfiguration>> load() {

                return List.of(

                        new ChartedHeaderDto<>(createDoughnutChartConfigFor(0, 0, "#dff2e3", "#28a745"),
                                createStringResource("RoleAnalysisIdentifyWidgetPanel.recertified").getString(),
                                String.valueOf(0), "0%"),

                        new ChartedHeaderDto<>(createDoughnutChartConfigFor(0, 0, "#dcf1f4", "#18a2b8"),
                                createStringResource("RoleAnalysisIdentifyWidgetPanel.pending").getString(),
                                String.valueOf(0), "0%"),

                        new ChartedHeaderDto<>(createDoughnutChartConfigFor(0, 0, "#e9eaec", "#6c757d"),
                                createStringResource("RoleAnalysisIdentifyWidgetPanel.idle").getString(),
                                String.valueOf(0), "0%")

                );
            }
        };
    }

    protected Component getBodyHeaderPanel(String id) {
        RepeatingView repeatingView = new RepeatingView(id);
        repeatingView.setOutputMarkupId(true);
        LoadableModel<List<ChartedHeaderDto<DoughnutChartConfiguration>>> chartedHeaderDtoModel = getChartedHeaderDtoModel();
        List<ChartedHeaderDto<DoughnutChartConfiguration>> object = chartedHeaderDtoModel.getObject();
        if (object == null) {
            return repeatingView;
        }

        object.forEach(dto -> {
            WidgetRmChartComponent<DoughnutChartConfiguration> header = new WidgetRmChartComponent<>(
                    repeatingView.newChildId(), Model.of(), Model.of(dto));
            header.setOutputMarkupId(true);
            header.add(AttributeAppender.append(CLASS_CSS, "col-auto p-0"));
            repeatingView.add(header);
        });

        return repeatingView;
    }

    private void initBodyItems(@NotNull WebMarkupContainer card) {
        WebMarkupContainer bodyContainer = new WebMarkupContainer(ID_CARD_BODY);
        bodyContainer.setOutputMarkupId(true);
        card.add(bodyContainer);

        WebMarkupContainer bodyHeaderPanelContainer = new WebMarkupContainer(ID_BODY_HEADER_PANEL_CONTAINER);
        bodyHeaderPanelContainer.setOutputMarkupId(true);
        bodyHeaderPanelContainer.add(AttributeModifier.append("style", getBodyHeaderPanelStyle()));
        bodyContainer.add(bodyHeaderPanelContainer);

        Component bodyHeaderPanel = getBodyHeaderPanel(ID_BODY_HEADER_PANEL);
        bodyHeaderPanelContainer.add(new VisibleBehaviour(this::isHeaderVisible));
        bodyHeaderPanelContainer.add(bodyHeaderPanel);

        if (getModel() == null) {
            WebMarkupContainer details = new WebMarkupContainer(ID_BODY_ITEM_CONTAINER);
            details.setOutputMarkupId(true);
            details.add(new VisibleBehaviour(() -> false));
            bodyContainer.add(details);

            details.add(new EmptyPanel(ID_IMAGE));
            details.add(new EmptyPanel(ID_TITLE));
            details.add(new EmptyPanel(ID_VALUE));
            details.add(new EmptyPanel(ID_SCORE));
            details.add(new EmptyPanel(ID_VALUE_TITLE));
            details.add(new EmptyPanel(ID_SCORE_ACTION));
            return;
        }

        ListView<IdentifyWidgetItem> details = new ListView<>(ID_BODY_ITEM_CONTAINER, getModel()) {

            @Override
            protected void populateItem(@NotNull ListItem<IdentifyWidgetItem> item) {
                initBodyComponents(item);
            }
        };
        bodyContainer.add(details);
    }

    private static void initBodyComponents(@NotNull ListItem<IdentifyWidgetItem> item) {
        IdentifyWidgetItem data = item.getModelObject();
        item.add(data.createImageComponent(ID_IMAGE));
        item.add(data.createTitleComponent(ID_TITLE));
        item.add(data.createDescriptionComponent(ID_VALUE));
        item.add(data.createScoreComponent(ID_SCORE));
        item.add(data.createValueTitleComponent(ID_VALUE_TITLE));
        item.add(data.createActionComponent(ID_SCORE_ACTION));

        if (data.isVisible() != null) {
            item.add(data.isVisible());
        }
    }

    private void initHeaderItems(@NotNull WebMarkupContainer card, IModel<String> titleModel) {
        WebMarkupContainer headerContainer = new WebMarkupContainer(ID_CARD_HEADER);
        headerContainer.setOutputMarkupId(true);
        card.add(headerContainer);
        IconWithLabel headerItem = new IconWithLabel(ID_HEADER_ITEM, titleModel) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return RoleAnalysisIdentifyWidgetPanel.this.getIconCssClass();
            }
        };

        headerItem.setOutputMarkupId(true);
        headerContainer.add(headerItem);
    }

    @Contract(pure = true)
    protected @NotNull String getIconCssClass() {
        return GuiStyleConstants.CLASS_ICON_OUTLIER;
    }

    @Contract(pure = true)
    private @NotNull IModel<String> getTitleModel() {
        return () -> {
            if (title == null || title.getObject() == null) {
                return null;
            }
            return title.getObject();
        };

    }

    protected boolean isHeaderVisible() {
        return true;
    }

    protected String initDefaultCssClass() {
        return "";
    }

    protected String getBodyHeaderPanelStyle() {
        return null;
    }
}
