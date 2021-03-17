/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.wicket.chartjs.PieChartConfiguration;

import org.apache.commons.collections4.CollectionUtils;
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
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.ProcessedItemDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskInfoBoxType;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskIterativeProgressType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskPartItemsProcessingInformationType;
import com.evolveum.wicket.chartjs.ChartJsPanel;

/**
 * TODO MID-6850 (whole class)
 */
public class TaskIterativeInformationPanel extends BasePanel<IterativeTaskInformationType> {

    private static final String ID_PARTS = "parts";
    private static final String ID_SUCCESS_ITEM = "successItem";
    private static final String ID_FAILED_ITEM = "failedItem";
    private static final String ID_SKIPPED_ITEM = "skippedItem";

    private static final String ID_CURRENT_ITEMS = "currentItems";

    private static final String ID_CHART = "chart";
    private static final String ID_PROGRESS_SUMMARY = "progressSummary";
    private static final String ID_WALLCLOCK_THROUGHPUT = "wallClockThroughput";

    public TaskIterativeInformationPanel(String id, IModel<IterativeTaskInformationType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayoutNew();
    }

    private TaskInfoBoxPanel createInfoBoxPanel(IModel<TaskInfoBoxType> boxModel, String boxId) {

        TaskInfoBoxPanel infoBoxPanel = new TaskInfoBoxPanel(boxId, boxModel);
        infoBoxPanel.setOutputMarkupId(true);
        infoBoxPanel.add(new VisibleBehaviour(() -> boxModel.getObject() != null));
        return infoBoxPanel;
    }

    private List<IColumn<ProcessedItemDto, String>> createColumns() {
        List<IColumn<ProcessedItemDto, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<>(createStringResource("ProcessedItemType.currentItem")) {

            @Override
            public void populateItem(Item<ICellPopulator<ProcessedItemDto>> item, String s, IModel<ProcessedItemDto> iModel) {
                ProcessedItemDto processed = iModel.getObject();
                String label = processed.getDisplayName() + " (" + WebComponentUtil.formatDurationWordsForLocal(processed.getDuration(), true, true, getPageBase()) + ")";
                item.add(new Label(s, label));
            }
        });
        return columns;
    }
    private void initLayoutNew() {

        ListView<IterativeTaskPartItemsProcessingInformationType> partsView = new ListView<>(ID_PARTS, new PropertyModel<>(getModel(), IterativeTaskInformationType.F_PART.getLocalPart())) {

            @Override
            protected void populateItem(ListItem<IterativeTaskPartItemsProcessingInformationType> item) {
                IModel<TaskIterativeProgressType> progressModel = createProgressModel(item);

                Label summary = new Label(ID_PROGRESS_SUMMARY, new PropertyModel<>(progressModel, TaskIterativeProgressType.F_TITLE));
                summary.setOutputMarkupId(true);
                item.add(summary);

                Label wallClockThroughput = new Label(ID_WALLCLOCK_THROUGHPUT, new PropertyModel<>(progressModel, TaskIterativeProgressType.F_WALLCLOCK_THROUGHPUT));
                summary.setOutputMarkupId(true);
                item.add(wallClockThroughput);

                ChartJsPanel<PieChartConfiguration> chartPanel = new ChartJsPanel<>(ID_CHART, new PropertyModel<>(progressModel, TaskIterativeProgressType.F_PROGRESS));
                item.add(chartPanel);

                PropertyModel<List<ProcessedItemDto>> currentItemsModel = new PropertyModel<>(progressModel, TaskIterativeProgressType.F_CURRENT_ITEMS);
                BoxedTablePanel<ProcessedItemDto> currentItems = new BoxedTablePanel<>(ID_CURRENT_ITEMS, new ListDataProvider<>(TaskIterativeInformationPanel.this, currentItemsModel), createColumns()) {

                    @Override
                    protected boolean hideFooterIfSinglePage() {
                        return true;
                    }

                    @Override
                    public String getAdditionalBoxCssClasses() {
                        return " box-info ";
                    }
                };
                currentItems.setOutputMarkupId(true);
                currentItems.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(currentItemsModel.getObject())));
                item.add(currentItems);

                item.add(createInfoBoxPanel(new PropertyModel<>(progressModel, TaskIterativeProgressType.F_SUCCESS_BOX), ID_SUCCESS_ITEM));
                item.add(createInfoBoxPanel(new PropertyModel<>(progressModel, TaskIterativeProgressType.F_FAILED_BOX), ID_FAILED_ITEM));
                item.add(createInfoBoxPanel(new PropertyModel<>(progressModel, TaskIterativeProgressType.F_SKIP_BOX), ID_SKIPPED_ITEM));

            }
        };
        partsView.setOutputMarkupId(true);
        add(partsView);
    }

    protected IModel<TaskIterativeProgressType> createProgressModel(ListItem<IterativeTaskPartItemsProcessingInformationType> item) {
        throw new UnsupportedOperationException("Not supported. Should be impelemnted in panel, which uses it");
    }

}
