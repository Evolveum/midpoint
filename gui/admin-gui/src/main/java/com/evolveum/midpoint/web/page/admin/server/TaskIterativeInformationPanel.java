/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProcessedItemType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskInfoBoxType;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskIterativeProgressType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskPartItemsProcessingInformationType;
import com.evolveum.wicket.chartjs.ChartJsPanel;

import org.bouncycastle.asn1.icao.ICAOObjectIdentifiers;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO MID-6850 (whole class)
 */
public class TaskIterativeInformationPanel extends BasePanel<IterativeTaskInformationType> {

    private static final String ID_PROGRESS = "progress";

    private static final String ID_PARTS = "parts";
    private static final String ID_SUCESS_ITEM = "successItem";
    private static final String ID_FAILED_ITEM = "failedItem";
    private static final String ID_SKIPPED_ITEM = "skippedItem";

    private static final String ID_CURRENT_ITEMS = "currentItems";
    private static final String ID_CURRENT_ITEM = "currentItem";

    private static final String ID_CHART = "chart";
    private static final String ID_PROGRESS_SUMMARY = "progressSummary";

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

    private List<IColumn<ProcessedItemType, String>> createColumns() {
        List<IColumn<ProcessedItemType, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<ProcessedItemType, String>(createStringResource("ProcessedItemType.currentItem")) {

            @Override
            public void populateItem(Item<ICellPopulator<ProcessedItemType>> item, String s, IModel<ProcessedItemType> iModel) {
                ProcessedItemType processed = iModel.getObject();
                String label = processed.getName() + "(123ms)";
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

                ChartJsPanel chartpanel = new ChartJsPanel(ID_CHART, new PropertyModel<>(progressModel, TaskIterativeProgressType.F_PROGRESS));
                item.add(chartpanel);

                PropertyModel<List<ProcessedItemType>> currentItemsModel = new PropertyModel<>(item.getModel(), IterativeTaskPartItemsProcessingInformationType.F_CURRENT.getLocalPart());
//                ListView<ProcessedItemType> currentItems = new ListView<>(ID_CURRENT_ITEMS, currentItemsModel) {
//
//                    @Override
//                    protected void populateItem(ListItem<ProcessedItemType> item) {
//                        item.add(createInfoBoxPanel(item.getModel(), ID_CURRENT_ITEM));
//                    }
//                };
                BoxedTablePanel<ProcessedItemType> currentItems = new BoxedTablePanel<>(ID_CURRENT_ITEMS, new ListDataProvider<>(TaskIterativeInformationPanel.this, currentItemsModel), createColumns()) {

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
                currentItems.add(new VisibleBehaviour(() -> currentItemsModel != null && CollectionUtils.isNotEmpty(currentItemsModel.getObject())));
                item.add(currentItems);

                item.add(createInfoBoxPanel(new PropertyModel<>(progressModel, TaskIterativeProgressType.F_SUCCESS_BOX), ID_SUCESS_ITEM));
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
