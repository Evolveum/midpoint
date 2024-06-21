/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.io.Serial;
import java.util.List;

public class StatisticListBoxPanel extends BasePanel<List<StatisticBoxDto>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TITLE_ICON = "titleIcon";
    private static final String ID_TITLE_LABEL = "titleLabel";
    private static final String ID_RIGHT_SIDE_HEADER_COMPONENT = "rightSideHeaderComponent";
    private static final String ID_STATISTIC_PANEL = "statisticsPanel";
    private static final String ID_STATISTIC_BOX = "statisticBox";
    private static final String ID_STATISTIC_CONTAINER = "statisticContainer";
    private static final String ID_VIEW_ALL_LINK = "viewAllLink";

    private IModel<DisplayType> boxDisplayModel;

    public StatisticListBoxPanel(String id, IModel<DisplayType> boxDisplayModel, IModel<List<StatisticBoxDto>> modelObject) {
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

        ListView<StatisticBoxDto> statisticPanel = new ListView<>(ID_STATISTIC_PANEL, getModel()) {
            @Override
            protected void populateItem(ListItem<StatisticBoxDto> listItem) {
                listItem.add(new StatisticBoxPanel(ID_STATISTIC_BOX, listItem.getModel()));
            }
        };
        add(statisticPanel);

        AjaxLink<Void> viewAllLink = new AjaxLink<>(ID_VIEW_ALL_LINK) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                // TODO
            }
        };
        add(viewAllLink);
    }

    protected Component createRightSideHeaderComponent(String id) {
        String additionalInfo = GuiDisplayTypeUtil.getHelp(boxDisplayModel.getObject());
        Label additionalInfoLabel = new Label(id, additionalInfo);
        additionalInfoLabel.add(new VisibleBehaviour(() -> additionalInfo != null && !additionalInfo.isEmpty()));
        return additionalInfoLabel;
    }
}
