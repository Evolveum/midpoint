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

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.schema.util.task.TaskOperationStatsUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskIterativeProgressType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class TaskOperationStatisticsPanel extends BasePanel<PrismObjectWrapper<TaskType>> implements RefreshableTabPanel {

    private static final String ID_PROCESSING_INFO = "processingInfo";
    private static final String ID_SYNCHRONIZATION_STATISTICS = "synchronizationStatistics";
    private static final String ID_SYNCHORNIZATION_SITUATIONS_TRANSITIONS = "synchronizationSituationTransitions";
    private static final String ID_ACTION_ENTRY = "actionEntry";
    private static final String ID_ACTION_ENTRY_TITLE = "actionEntryTitle";
    private static final String ID_RESULTING_ENTRY = "resultingEntry";
    private static final String ID_RESULTING_ENTRY_TITLE ="resultingEntryTitle";

    private LoadableModel<OperationStatsType> statisticsModel;

    public TaskOperationStatisticsPanel(String id, IModel<PrismObjectWrapper<TaskType>> model) {
        super(id, model);

        statisticsModel = createOperationStatsModel();
    }

    private LoadableModel<OperationStatsType> createOperationStatsModel() {

       return new LoadableModel<>(true) {

           @Override
           protected OperationStatsType load() {
               PrismObject<TaskType> task = getModelObject().getObject();
               return TaskOperationStatsUtil.getOperationStatsFromTree(task.asObjectable(), getPrismContext());
           }
       };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {

        addProcessingInfoPanel();
        addSynchronizationTransitionPanel();
        addActionsTablePanel();
        addResultingEntryPanel();

    }

    private void addProcessingInfoPanel() {

        TaskIterativeInformationPanel infoPanel = new TaskIterativeInformationPanel(ID_PROCESSING_INFO, new PropertyModel<>(statisticsModel, OperationStatsType.F_ITERATIVE_TASK_INFORMATION.getLocalPart())) {

            @Override
            protected IModel<TaskIterativeProgressType> createProgressModel(ListItem<IterativeTaskPartItemsProcessingInformationType> item) {
                return new ReadOnlyModel<>(() -> {
                    IterativeTaskPartItemsProcessingInformationType taskInfo = item.getModelObject();
                    return new TaskIterativeProgressType(taskInfo, getTaskType());
                });
            }
        };
        infoPanel.setOutputMarkupId(true);
        add(infoPanel);
    }

    private void addSynchronizationTransitionPanel() {
        WebMarkupContainer syncTransitionParent = new WebMarkupContainer(ID_SYNCHRONIZATION_STATISTICS);
        syncTransitionParent.setOutputMarkupId(true);
        add(syncTransitionParent);

        PropertyModel<List<SynchronizationSituationTransitionType>> syncInfoModel = new PropertyModel<>(statisticsModel, getSynchronizationTransitionExpression());
        SynchronizationSituationTransitionPanel transitions = new SynchronizationSituationTransitionPanel(ID_SYNCHORNIZATION_SITUATIONS_TRANSITIONS, syncInfoModel);
        transitions.setOutputMarkupId(true);
        syncTransitionParent.add(transitions);
        transitions.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(syncInfoModel.getObject())));

    }

    private void addActionsTablePanel() {
        ListDataProvider<ObjectActionsExecutedEntryType> objectActionsEntry = createActionsEntryProvider(ActionsExecutedInformationType.F_OBJECT_ACTIONS_ENTRY);
        BoxedTablePanel<ObjectActionsExecutedEntryType> actionTable = new BoxedTablePanel<>(ID_ACTION_ENTRY, objectActionsEntry, createActionEntryColumns()) {
            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new Fragment(headerId, ID_ACTION_ENTRY_TITLE, TaskOperationStatisticsPanel.this);
            }
        };

        actionTable.setOutputMarkupId(true);
        actionTable.add(new VisibleBehaviour(() -> hasObjectActionsEntry()));
        add(actionTable);
    }

    private boolean hasObjectActionsEntry() {
        ActionsExecutedInformationType actionsExecutedInformationType = getActionExecutedInformationType();
        if (actionsExecutedInformationType == null) {
            return false;
        }
        return CollectionUtils.isNotEmpty(actionsExecutedInformationType.getObjectActionsEntry());
    }

    private ActionsExecutedInformationType getActionExecutedInformationType() {
        if (statisticsModel == null) {
            return null;
        }

        OperationStatsType stats = statisticsModel.getObject();
        if (stats == null) {
            return null;
        }

        ActionsExecutedInformationType actionsExecutedInformationType = stats.getActionsExecutedInformation();
        return actionsExecutedInformationType;
    }

    private void addResultingEntryPanel() {
        BoxedTablePanel<ObjectActionsExecutedEntryType> resultingEntry =
                new BoxedTablePanel<>(ID_RESULTING_ENTRY, createActionsEntryProvider(ActionsExecutedInformationType.F_RESULTING_OBJECT_ACTIONS_ENTRY), createActionEntryColumns()) {
            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }

                    @Override
                    protected WebMarkupContainer createHeader(String headerId) {
                        return new Fragment(headerId, ID_RESULTING_ENTRY_TITLE, TaskOperationStatisticsPanel.this);
                    }
        };

        resultingEntry.setOutputMarkupId(true);
        resultingEntry.add(new VisibleBehaviour(() -> hasResultingEntry()));
        add(resultingEntry);
    }

    private boolean hasResultingEntry() {
        ActionsExecutedInformationType actionsExecutedInformationType = getActionExecutedInformationType();
        if (actionsExecutedInformationType == null) {
            return false;
        }
        return CollectionUtils.isNotEmpty(actionsExecutedInformationType.getResultingObjectActionsEntry());
    }

    private String getSynchronizationTransitionExpression() {
        return OperationStatsType.F_SYNCHRONIZATION_INFORMATION.getLocalPart() + "." + SynchronizationInformationType.F_TRANSITION.getLocalPart();
    }

    private <T> EnumPropertyColumn<T> createEnumColumn() {
        String columnName = ObjectActionsExecutedEntryType.F_OPERATION.getLocalPart();
        return new EnumPropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType." + columnName), columnName);
    }

    private <T> PropertyColumn<T, String> createPropertyColumn(QName columnItem) {
        String columnName = columnItem.getLocalPart();
        return new PropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType." + columnName), columnName);
    }

    private ListDataProvider<ObjectActionsExecutedEntryType> createActionsEntryProvider(QName item) {
        String expression = OperationStatsType.F_ACTIONS_EXECUTED_INFORMATION.getLocalPart() + "." + item.getLocalPart();
        return new ListDataProvider<>(this,
                new PropertyModel<>(statisticsModel, expression));
    }

    private List<IColumn<ObjectActionsExecutedEntryType, String>> createActionEntryColumns() {
        List<IColumn<ObjectActionsExecutedEntryType, String>> resultingEntryColumns = new ArrayList<>();
        resultingEntryColumns.add(new AbstractColumn<>(createStringResource("ObjectActionsExecutedEntryType.objectType")) {
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
        resultingEntryColumns.add(createEnumColumn());
        resultingEntryColumns.add(new AbstractColumn<>(createStringResource("ObjectActionsExecutedEntryType.chanel")) {
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
        resultingEntryColumns.add(createPropertyColumn(ObjectActionsExecutedEntryType.F_TOTAL_SUCCESS_COUNT));
        resultingEntryColumns.add(createPropertyColumn(ObjectActionsExecutedEntryType.F_LAST_SUCCESS_OBJECT_DISPLAY_NAME));
        resultingEntryColumns.add(new AbstractColumn<>(createStringResource("ObjectActionsExecutedEntryType.lastSuccessTimestamp")) {
            @Override
            public void populateItem(Item<ICellPopulator<ObjectActionsExecutedEntryType>> item, String id, IModel<ObjectActionsExecutedEntryType> iModel) {
                XMLGregorianCalendar timestamp = iModel.getObject().getLastSuccessTimestamp();
                item.add(new Label(id, WebComponentUtil.formatDate(timestamp)));
            }
        });
        resultingEntryColumns.add(createPropertyColumn(ObjectActionsExecutedEntryType.F_TOTAL_FAILURE_COUNT));
        return resultingEntryColumns;
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        statisticsModel.reset();
        List<Component> components = new ArrayList<>();
        components.add(get(ID_ACTION_ENTRY));
        components.add(get(ID_PROCESSING_INFO));
        components.add(get(ID_RESULTING_ENTRY));
        components.add(get(ID_SYNCHRONIZATION_STATISTICS));
        return components;
    }

    private TaskType getTaskType() {
        PrismObjectWrapper<TaskType> taskWrapper = getModelObject();
        if (taskWrapper == null) {
            return null;
        }
        return taskWrapper.getObject().asObjectable();
    }

}
