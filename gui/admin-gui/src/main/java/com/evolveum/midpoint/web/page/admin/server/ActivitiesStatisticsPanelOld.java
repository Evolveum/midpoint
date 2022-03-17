/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.ActivitiesStatisticsDto;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Panel for activity-level statistics (item processing, synchronization, actions executed) for a list of activities.
 *
 * Some of the statistics are displayed per activity, others are aggregated. (This is perhaps temporary. We plan to
 * provide detailed information later.)
 */
public class ActivitiesStatisticsPanelOld extends BasePanel<PrismObjectWrapper<TaskType>> implements RefreshableTabPanel {

    private static final String ID_ITEM_PROCESSING = "itemProcessing";
    private static final String ID_SYNCHRONIZATION_STATISTICS = "synchronizationStatistics";
    private static final String ID_SYNCHRONIZATION_SITUATIONS_TRANSITIONS = "synchronizationSituationTransitions";
    private static final String ID_RESULTING_ACTIONS_EXECUTED = "resultingActionsExecuted";
    private static final String ID_RESULTING_ACTIONS_EXECUTED_TITLE = "resultingActionsExecutedTitle";
    private static final String ID_ALL_ACTIONS_EXECUTED = "allActionsExecuted";
    private static final String ID_ALL_ACTIONS_EXECUTED_TITLE = "allActionsExecutedTitle";

    private final LoadableModel<ActivitiesStatisticsDto> statisticsModel;

    ActivitiesStatisticsPanelOld(String id, IModel<PrismObjectWrapper<TaskType>> model) {
        super(id, model);

        statisticsModel = LoadableModel.create(
                () -> ActivitiesStatisticsDto.fromTaskTree(getTask()),
                true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {
        addItemProcessingPanel();
        addSynchronizationTransitionPanel();
        addActionsExecutedTablePanel(ID_RESULTING_ACTIONS_EXECUTED, ID_RESULTING_ACTIONS_EXECUTED_TITLE,
                ActivitiesStatisticsDto.F_RESULTING_ACTIONS_EXECUTED);
        addActionsExecutedTablePanel(ID_ALL_ACTIONS_EXECUTED, ID_ALL_ACTIONS_EXECUTED_TITLE,
                ActivitiesStatisticsDto.F_ALL_ACTIONS_EXECUTED);
    }

    private void addItemProcessingPanel() {
        ActivitiesItemProcessingPanel infoPanel = new ActivitiesItemProcessingPanel(ID_ITEM_PROCESSING,
                new PropertyModel<>(statisticsModel, ActivitiesStatisticsDto.F_ITEM_PROCESSING));
        infoPanel.setOutputMarkupId(true);
        add(infoPanel);
    }

    private void addSynchronizationTransitionPanel() {
        WebMarkupContainer syncTransitionParent = new WebMarkupContainer(ID_SYNCHRONIZATION_STATISTICS);
        syncTransitionParent.setOutputMarkupId(true);
        add(syncTransitionParent);

        PropertyModel<List<SynchronizationSituationTransitionType>> syncInfoModel =
                PropertyModel.of(statisticsModel, ActivitiesStatisticsDto.F_SYNCHRONIZATION_TRANSITIONS);
        SynchronizationSituationTransitionPanel transitions =
                new SynchronizationSituationTransitionPanel(ID_SYNCHRONIZATION_SITUATIONS_TRANSITIONS, syncInfoModel);
        transitions.setOutputMarkupId(true);
        syncTransitionParent.add(transitions);
        transitions.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(syncInfoModel.getObject())));
    }

    private void addActionsExecutedTablePanel(String id, String titleId, String modelField) {
        ListDataProvider<ObjectActionsExecutedEntryType> dataProvider =
                new ListDataProvider<>(this, PropertyModel.of(statisticsModel, modelField));

        BoxedTablePanel<ObjectActionsExecutedEntryType> actionTable =
                new BoxedTablePanel<>(id, dataProvider, createActionEntryColumns()) {
                    @Override
                    protected boolean hideFooterIfSinglePage() {
                        return true;
                    }

                    @Override
                    protected WebMarkupContainer createHeader(String headerId) {
                        return new Fragment(headerId, titleId, ActivitiesStatisticsPanelOld.this);
                    }
                };

        actionTable.setOutputMarkupId(true);
        actionTable.add(new VisibleBehaviour(() -> true));//!dataProvider.getAvailableData().isEmpty()));
        add(actionTable);
    }

    private <T> EnumPropertyColumn<T> createEnumColumn() {
        String columnName = ObjectActionsExecutedEntryType.F_OPERATION.getLocalPart();
        return new EnumPropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType." + columnName), columnName);
    }

    private <T> PropertyColumn<T, String> createPropertyColumn(QName columnItem) {
        String columnName = columnItem.getLocalPart();
        return new PropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType." + columnName), columnName);
    }

    private List<IColumn<ObjectActionsExecutedEntryType, String>> createActionEntryColumns() {
        List<IColumn<ObjectActionsExecutedEntryType, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<>(createStringResource("ObjectActionsExecutedEntryType.objectType")) {
            @Override
            public void populateItem(Item<ICellPopulator<ObjectActionsExecutedEntryType>> item, String id, IModel<ObjectActionsExecutedEntryType> iModel) {
                ObjectActionsExecutedEntryType entry = iModel.getObject();
                ObjectTypes objectType = null;
                if (entry != null) {
                    if (entry.getObjectType() != null) {
                        objectType = ObjectTypes.getObjectTypeFromTypeQName(entry.getObjectType());
                    }
                }
                item.add(new Label(id, createStringResource(objectType)));
            }
        });
        columns.add(createEnumColumn());
        columns.add(new AbstractColumn<>(createStringResource("ObjectActionsExecutedEntryType.chanel")) {
            @Override
            public void populateItem(Item<ICellPopulator<ObjectActionsExecutedEntryType>> item, String id, IModel<ObjectActionsExecutedEntryType> iModel) {
                String channel = iModel.getObject().getChannel();
                String key = "";
                if (channel != null && !channel.isEmpty()) {
                    key = "Channel." + WebComponentUtil.getSimpleChannel(channel);
                }
                item.add(new Label(id, createStringResource(key)));
            }
        });
        columns.add(createPropertyColumn(ObjectActionsExecutedEntryType.F_TOTAL_SUCCESS_COUNT));
        columns.add(createPropertyColumn(ObjectActionsExecutedEntryType.F_LAST_SUCCESS_OBJECT_DISPLAY_NAME));
        columns.add(new AbstractColumn<>(createStringResource("ObjectActionsExecutedEntryType.lastSuccessTimestamp")) {
            @Override
            public void populateItem(Item<ICellPopulator<ObjectActionsExecutedEntryType>> item, String id, IModel<ObjectActionsExecutedEntryType> iModel) {
                XMLGregorianCalendar timestamp = iModel.getObject().getLastSuccessTimestamp();
                item.add(new Label(id, WebComponentUtil.formatDate(timestamp)));
            }
        });
        columns.add(createPropertyColumn(ObjectActionsExecutedEntryType.F_TOTAL_FAILURE_COUNT));
        return columns;
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        statisticsModel.reset();
        List<Component> components = new ArrayList<>();
        components.add(get(ID_ITEM_PROCESSING));
        components.add(get(ID_SYNCHRONIZATION_STATISTICS));
        components.add(get(ID_RESULTING_ACTIONS_EXECUTED));
        components.add(get(ID_ALL_ACTIONS_EXECUTED));
        return components;
    }

    private TaskType getTask() {
        PrismObjectWrapper<TaskType> taskWrapper = getModelObject();
        return taskWrapper != null ? taskWrapper.getObject().asObjectable() : null;
    }
}
