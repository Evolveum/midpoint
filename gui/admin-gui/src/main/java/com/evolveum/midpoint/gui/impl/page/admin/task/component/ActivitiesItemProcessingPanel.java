/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.box.InfoBox;
import com.evolveum.midpoint.gui.impl.component.box.InfoBoxData;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.ActivitiesItemProcessingDto;
import com.evolveum.midpoint.web.page.admin.server.dto.ActivityItemProcessingDto;
import com.evolveum.midpoint.web.page.admin.server.dto.ProcessedItemDto;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.wicket.chartjs.ChartJsPanel;
import com.evolveum.wicket.chartjs.PieChartConfiguration;

/**
 * Shows "item processing" statistics for a collection of activities.
 */
public class ActivitiesItemProcessingPanel extends BasePanel<ActivitiesItemProcessingDto> {

    private static final String ID_ACTIVITIES = "activities";

    private static final String ID_SUCCESS_ITEM = "successItem";
    private static final String ID_FAILED_ITEM = "failedItem";
    private static final String ID_SKIPPED_ITEM = "skippedItem";

    private static final String ID_CURRENT_ITEMS = "currentItems";

    private static final String ID_CHART = "chart";
    private static final String ID_SUMMARY = "summary";
    private static final String ID_WALL_CLOCK_THROUGHPUT = "wallClockThroughput";

    ActivitiesItemProcessingPanel(String id, IModel<ActivitiesItemProcessingDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayoutNew();
    }

    private void initLayoutNew() {

        ListView<ActivityItemProcessingDto> activitiesView =
                new ListView<>(ID_ACTIVITIES, new PropertyModel<>(getModel(), ActivitiesItemProcessingDto.F_ACTIVITIES)) {

            @Override
            protected void populateItem(ListItem<ActivityItemProcessingDto> listItem) {
                IModel<ActivityItemProcessingDto> itemProcessingModel = listItem.getModel();

                Label summary = new Label(ID_SUMMARY, PropertyModel.of(itemProcessingModel, ActivityItemProcessingDto.F_TITLE));
                summary.setOutputMarkupId(true);
                listItem.add(summary);

                Label wallClockThroughput = new Label(ID_WALL_CLOCK_THROUGHPUT,
                        PropertyModel.of(itemProcessingModel, ActivityItemProcessingDto.F_WALL_CLOCK_THROUGHPUT));
                summary.setOutputMarkupId(true);
                listItem.add(wallClockThroughput);

                ChartJsPanel<PieChartConfiguration> chartPanel =
                        new ChartJsPanel<>(ID_CHART, PropertyModel.of(itemProcessingModel, ActivityItemProcessingDto.F_CHART));
                listItem.add(chartPanel);
                chartPanel.add(new VisibleBehaviour(() -> itemProcessingModel.getObject().getTotalCount() > 0));

                PropertyModel<List<ProcessedItemDto>> currentItemsModel =
                        PropertyModel.of(itemProcessingModel, ActivityItemProcessingDto.F_CURRENT_ITEMS);
                BoxedTablePanel<ProcessedItemDto> currentItems = new BoxedTablePanel<>(
                        ID_CURRENT_ITEMS,
                        new ListDataProvider<>(ActivitiesItemProcessingPanel.this, currentItemsModel),
                        createCurrentItemsColumns()) {

                    @Override
                    protected boolean hideFooterIfSinglePage() {
                        return true;
                    }

                    @Override
                    public String getAdditionalBoxCssClasses() {
                        return " card-info ";
                    }
                };
                currentItems.setOutputMarkupId(true);
                currentItems.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(currentItemsModel.getObject())));
                listItem.add(currentItems);

                listItem.add(createInfoBoxPanel(PropertyModel.of(itemProcessingModel, ActivityItemProcessingDto.F_SUCCESS_BOX), ID_SUCCESS_ITEM));
                listItem.add(createInfoBoxPanel(PropertyModel.of(itemProcessingModel, ActivityItemProcessingDto.F_FAILED_BOX), ID_FAILED_ITEM));
                listItem.add(createInfoBoxPanel(PropertyModel.of(itemProcessingModel, ActivityItemProcessingDto.F_SKIP_BOX), ID_SKIPPED_ITEM));

            }
        };
        activitiesView.setOutputMarkupId(true);
        add(activitiesView);
    }

    private List<IColumn<ProcessedItemDto, String>> createCurrentItemsColumns() {
        List<IColumn<ProcessedItemDto, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<>(createStringResource("ProcessedItemType.currentItem")) {

            @Override
            public void populateItem(Item<ICellPopulator<ProcessedItemDto>> item, String s, IModel<ProcessedItemDto> iModel) {
                ProcessedItemDto processed = iModel.getObject();
                String duration = WebComponentUtil.formatDurationWordsForLocal(processed.getDuration(),
                        true, true, getPageBase());
                String label = processed.getDisplayName() + " (" + duration + ")";
                item.add(new Label(s, label));
            }
        });
        return columns;
    }

    private InfoBox createInfoBoxPanel(IModel<InfoBoxData> model, String boxId) {
        InfoBox infoBox = new InfoBox(boxId, () -> {
            InfoBoxData data = model.getObject();
            return data != null ? data : new InfoBoxData();
        }) {

            @Override
            protected Component createLabel(String id, IModel model) {
                Component comp = super.createLabel(id, model);

                if (!InfoBox.ID_DESCRIPTION_2.equals(id)) {
                    return comp;
                }

                comp.add(AttributeAppender.append("title", model));
                comp.add(new TooltipBehavior() {
                    @Override
                    public String getDataPlacement() {
                        return "bottom";
                    }
                });

                return comp;
            }
        };
        infoBox.setOutputMarkupId(true);
        infoBox.add(new VisibleBehaviour(() -> model.getObject() != null));

        return infoBox;
    }
}
