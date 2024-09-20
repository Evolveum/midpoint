/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.List;

/**
 * The panel is planned to be used on the Campaign view page
 * to display Campaign activities, Created reports, Approver status etc.
 * The panel consists of a vertical list of info panels
 * @param <T>
 */
public class StatisticListBoxPanel<T> extends BasePanel<List<StatisticBoxDto<T>>> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TITLE_ICON = "titleIcon";
    private static final String ID_TITLE_LABEL = "titleLabel";
    private static final String ID_RIGHT_SIDE_HEADER_COMPONENT = "rightSideHeaderComponent";
    private static final String ID_STATISTIC_PANEL = "statisticsPanel";
    private static final String ID_STATISTIC_BOX = "statisticBox";
    private static final String ID_VIEW_ALL_LINK = "viewAllLink";
    private static final String ID_VIEW_ALL_LABEL = "viewAllLabel";

    private final IModel<DisplayType> boxDisplayModel;

    public StatisticListBoxPanel(String id, IModel<DisplayType> boxDisplayModel, IModel<List<StatisticBoxDto<T>>> modelObject) {
        super(id, modelObject);
        this.boxDisplayModel = boxDisplayModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer titleIcon = new WebMarkupContainer(ID_TITLE_ICON);
        String iconCssClass = GuiDisplayTypeUtil.getIconCssClass(boxDisplayModel.getObject());
        titleIcon.add(AttributeAppender.append("class", iconCssClass));
        titleIcon.add(new VisibleBehaviour(() -> iconCssClass != null && !iconCssClass.isEmpty()));
        add(titleIcon);

        String title = GuiDisplayTypeUtil.getTranslatedLabel(boxDisplayModel.getObject());
        Label titleLabel = new Label(ID_TITLE_LABEL, title);
        titleLabel.add(new VisibleBehaviour(() -> title != null && !title.isEmpty()));
        add(titleLabel);

        Component rightSideHeaderComponent = createRightSideHeaderComponent(ID_RIGHT_SIDE_HEADER_COMPONENT);
        add(rightSideHeaderComponent);

        ListView<StatisticBoxDto<T>> statisticPanel = new ListView<>(ID_STATISTIC_PANEL, getModel()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<StatisticBoxDto<T>> listItem) {
                listItem.add(new StatisticBoxPanel<>(ID_STATISTIC_BOX, listItem.getModel()) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected Component createRightSideComponent(String id, StatisticBoxDto<T> statisticObject) {
                        return StatisticListBoxPanel.this.createRightSideBoxComponent(id, statisticObject);
                    }

                });
            }
        };
        add(statisticPanel);

        AjaxLink<Void> viewAllLink = new AjaxLink<>(ID_VIEW_ALL_LINK) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.STOP);
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                viewAllActionPerformed(target);
            }
        };
        viewAllLink.add(new VisibleBehaviour(this::isViewAllAllowed));
        add(viewAllLink);

        viewAllLink.add(new Label(ID_VIEW_ALL_LABEL, createStringResource("AjaxIconButton.viewAll")));
    }

    protected boolean isViewAllAllowed() {
        return true;
    }

    protected Component createRightSideHeaderComponent(String id) {
        String additionalInfo = GuiDisplayTypeUtil.getHelp(boxDisplayModel.getObject());
        Label additionalInfoLabel = new Label(id, additionalInfo);
        additionalInfoLabel.add(new VisibleBehaviour(() -> additionalInfo != null && !additionalInfo.isEmpty()));
        return additionalInfoLabel;
    }

    protected void viewAllActionPerformed(AjaxRequestTarget target) {
    }

    protected Component createRightSideBoxComponent(String id, StatisticBoxDto<T> statisticObject) {
        return new WebMarkupContainer(id);
    }

    @Override
    public int getWidth() {
        return 800;
    }

    @Override
    public int getHeight() {
        return 600;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("CampaignStatisticsPanel.label");
    }

    @Override
    public Component getContent() {
        return this;
    }
}
