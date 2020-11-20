package com.evolveum.midpoint.web.page.admin.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class TaskOperationStatisticsPanel extends BasePanel<PrismObjectWrapper<TaskType>> implements RefreshableTabPanel {

    private static final String ID_PROCESSING_INFO = "processingInfo";
    private static final String ID_SYNCHORNIZATION_SITUATIONS = "synchronizationSituation";
    private static final String ID_ACTION_ENTRY = "actionEntry";
    private static final String ID_RESULTING_ENTRY = "resultingEntry";

    private static final Collection<String> WALL_CLOCK_AVG_CATEGORIES = Arrays.asList(
            TaskCategory.BULK_ACTIONS, TaskCategory.IMPORTING_ACCOUNTS, TaskCategory.RECOMPUTATION, TaskCategory.RECONCILIATION,
            TaskCategory.UTIL       // this is a megahack: only utility tasks that count objects are DeleteTask and ShadowIntegrityCheck
    );

    private LoadableModel<OperationStatsType> statisticsModel;

    public TaskOperationStatisticsPanel(String id, IModel<PrismObjectWrapper<TaskType>> model) {
        super(id, model);

        statisticsModel = createOperationStatsModel();
    }

    private LoadableModel<OperationStatsType> createOperationStatsModel() {

       return new LoadableModel<OperationStatsType>(true) {

           @Override
           protected OperationStatsType load() {
               PrismObject<TaskType> task = getModelObject().getObject();
               return TaskTypeUtil.getAggregatedOperationStats(task.asObjectable(), getPrismContext());
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

        TaskIterativeInformationPanel infoPanel = new TaskIterativeInformationPanel(ID_PROCESSING_INFO, new PropertyModel<>(statisticsModel, OperationStatsType.F_ITERATIVE_TASK_INFORMATION.getLocalPart())) {

            @Override
            protected Long getWallClockAverage(int objectsTotal) {
                if (objectsTotal == 0) {
                    return null;
                }

                TaskType task = TaskOperationStatisticsPanel.this.getModelObject().getObject().asObjectable();
                if (!WALL_CLOCK_AVG_CATEGORIES.contains(task.getCategory())) {
                    return null;
                }

                Long executionTime = TaskDisplayUtil.getExecutionTime(task);
                return executionTime != null ? executionTime / objectsTotal : null;
            }
        };
        infoPanel.setOutputMarkupId(true);
        add(infoPanel);


        PropertyModel<List<SynchronizationInformationDto>> syncInfoModel = new PropertyModel<List<SynchronizationInformationDto>>(statisticsModel, "") {

            @Override
            public List<SynchronizationInformationDto> getObject() {
                OperationStatsType statsType = statisticsModel.getObject();
                if (statsType == null) {
                    return null;
                }

                SynchronizationInformationType syncInfo = statsType.getSynchronizationInformation();
                if(syncInfo == null) {
                    return null;
                }

                List<SynchronizationInformationDto> infos = new ArrayList<>();
                infos.add(new SynchronizationInformationDto("Unmatched",syncInfo.getCountUnmatched(), syncInfo.getCountUnmatchedAfter()));
                infos.add(new SynchronizationInformationDto("Unlinked",syncInfo.getCountUnlinked(), syncInfo.getCountUnlinkedAfter()));
                infos.add(new SynchronizationInformationDto("Linked",syncInfo.getCountLinked(), syncInfo.getCountLinkedAfter()));
                infos.add(new SynchronizationInformationDto("Deleted",syncInfo.getCountDeleted(), syncInfo.getCountDeletedAfter()));
                infos.add(new SynchronizationInformationDto("Disputed",syncInfo.getCountDisputed(), syncInfo.getCountDisputedAfter()));
                infos.add(new SynchronizationInformationDto("Protected",syncInfo.getCountProtected(), syncInfo.getCountProtectedAfter()));
                infos.add(new SynchronizationInformationDto("No sync policy",syncInfo.getCountNoSynchronizationPolicy(), syncInfo.getCountNoSynchronizationPolicyAfter()));
                infos.add(new SynchronizationInformationDto("Sync disabled",syncInfo.getCountSynchronizationDisabled(), syncInfo.getCountSynchronizationDisabledAfter()));
                infos.add(new SynchronizationInformationDto("Not applicable for task",syncInfo.getCountNotApplicableForTask(), syncInfo.getCountNotApplicableForTaskAfter()));

                return infos;
            }
        };

        ListDataProvider<SynchronizationInformationDto> syncDataProvider = new ListDataProvider<SynchronizationInformationDto>(this, syncInfoModel) {
            @Override
            public boolean isUseCache() {
                return false;
            }
        };

        List<IColumn<SynchronizationInformationDto, String>> syncColumns = new ArrayList<>();
        syncColumns.add(new PropertyColumn<>(createStringResource("TaskOperationStatisticsPanel.situation"), SynchronizationInformationDto.F_ACTION_NAME));
        syncColumns.add(new PropertyColumn<>(createStringResource("TaskOperationStatisticsPanel.countBefore"), SynchronizationInformationDto.F_COUNT_BEFORE));
        syncColumns.add(new PropertyColumn<>(createStringResource("TaskOperationStatisticsPanel.countAfter"), SynchronizationInformationDto.F_COUNT_AFTER));

        BoxedTablePanel<SynchronizationInformationDto> table = new BoxedTablePanel<SynchronizationInformationDto>(ID_SYNCHORNIZATION_SITUATIONS, syncDataProvider, syncColumns) {
            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };

        table.setOutputMarkupId(true);
        add(table);

        ListDataProvider<ObjectActionsExecutedEntryType> objectActionsEntry = createActionsEntryProvider(OperationStatsType.F_ACTIONS_EXECUTED_INFORMATION.getLocalPart() + "." + ActionsExecutedInformationType.F_RESULTING_OBJECT_ACTIONS_ENTRY);
        BoxedTablePanel<ObjectActionsExecutedEntryType> actionTable = new BoxedTablePanel<ObjectActionsExecutedEntryType>(ID_ACTION_ENTRY, objectActionsEntry, createActionEntryColumns()) {
            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };

        actionTable.setOutputMarkupId(true);
        add(actionTable);


        BoxedTablePanel<ObjectActionsExecutedEntryType> resultingEntry = new BoxedTablePanel<ObjectActionsExecutedEntryType>(ID_RESULTING_ENTRY, createActionsEntryProvider(OperationStatsType.F_ACTIONS_EXECUTED_INFORMATION.getLocalPart() + "." + ActionsExecutedInformationType.F_OBJECT_ACTIONS_ENTRY), createActionEntryColumns()) {
            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };

        resultingEntry.setOutputMarkupId(true);
        add(resultingEntry);

    }

    private ListDataProvider<ObjectActionsExecutedEntryType> createActionsEntryProvider(String expression) {
        return new ListDataProvider<>(this,
                new PropertyModel<>(statisticsModel, expression));
    }

    private List<IColumn<ObjectActionsExecutedEntryType, String>> createActionEntryColumns() {
        List<IColumn<ObjectActionsExecutedEntryType, String>> resultingEntryColumns = new ArrayList<>();
        resultingEntryColumns.add(new AbstractColumn<ObjectActionsExecutedEntryType, String>(createStringResource("ObjectActionsExecutedEntryType.objectType")) {
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
        resultingEntryColumns.add(new EnumPropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType.operation"), ObjectActionsExecutedEntryType.F_OPERATION.getLocalPart()));
        resultingEntryColumns.add(new AbstractColumn<ObjectActionsExecutedEntryType, String>(createStringResource("ObjectActionsExecutedEntryType.chanel")) {
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
        resultingEntryColumns.add(new PropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType.totalSuccessCount"), ObjectActionsExecutedEntryType.F_TOTAL_SUCCESS_COUNT.getLocalPart()));
        resultingEntryColumns.add(new PropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType.lastSuccessDisplayName"), ObjectActionsExecutedEntryType.F_LAST_SUCCESS_OBJECT_DISPLAY_NAME.getLocalPart()));
        resultingEntryColumns.add(new AbstractColumn<ObjectActionsExecutedEntryType, String>(createStringResource("ObjectActionsExecutedEntryType.lastSuccessTimestamp")) {
            @Override
            public void populateItem(Item<ICellPopulator<ObjectActionsExecutedEntryType>> item, String id, IModel<ObjectActionsExecutedEntryType> iModel) {
                XMLGregorianCalendar timestamp = iModel.getObject().getLastSuccessTimestamp();
                item.add(new Label(id, WebComponentUtil.formatDate(timestamp)));
            }
        });
        resultingEntryColumns.add(new PropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType.totalFailureCount"), ObjectActionsExecutedEntryType.F_TOTAL_FAILURE_COUNT.getLocalPart()));
        return resultingEntryColumns;
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        statisticsModel.reset();
        List<Component> components = new ArrayList<>();
        components.add(get(ID_ACTION_ENTRY));
        components.add(get(ID_PROCESSING_INFO));
        components.add(get(ID_RESULTING_ENTRY));
        components.add(get(ID_SYNCHORNIZATION_SITUATIONS));
        return components;
    }

    class SynchronizationInformationDto implements Serializable {

        public static final String F_ACTION_NAME = "actionName";
        public static final String F_COUNT_BEFORE = "countBefore";
        public static final String F_COUNT_AFTER = "countAfter";

        private String actionName;
        private int countBefore;
        private int countAfter;

        public SynchronizationInformationDto(String actionName, int countBefore, int countAfter) {
            this.actionName = actionName;
            this.countBefore = countBefore;
            this.countAfter = countAfter;
        }

    }

}
