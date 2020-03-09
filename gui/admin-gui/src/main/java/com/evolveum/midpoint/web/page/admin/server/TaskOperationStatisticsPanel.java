package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ActionsExecutedInformation;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class TaskOperationStatisticsPanel extends BasePanel<PrismObjectWrapper<TaskType>> implements TaskTabPanel {

    private static final String ID_PROCESSING_INFO = "processingInfo";
    private static final String ID_SYNCHORNIZATION_SITUATIONS = "synchronizationSituation";
    private static final String ID_ACTION_ENTRY = "actionEntry";
    private static final String ID_RESULTING_ENTRY = "resultingEntry";

    private static final String DOT_CLASS = TaskOperationStatisticsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_SUBTASKS = DOT_CLASS + "loadSubtasks";

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
//               IterativeTaskInformationType iterativeTaskInformation = new IterativeTaskInformationType();
//               SynchronizationInformationType synchronizationInformation = new SynchronizationInformationType();
//               ActionsExecutedInformationType actionsExecutedInformation = new ActionsExecutedInformationType();
//
//               if (TaskTypeUtil.isPartitionedMaster(task.asObjectable())) {
////                   List<TaskType> subTasks = resolveSubTasks();
//                   Stream<TaskType> subTasks = TaskTypeUtil.getAllTasksStream(task.asObjectable());
//                   subTasks.forEach(subTask -> {
//                       OperationStatsType operationStatsType = subTask.getOperationStats();
//                       if (operationStatsType != null) {
//                           IterativeTaskInformation.addTo(iterativeTaskInformation, operationStatsType.getIterativeTaskInformation(), true);
//                           SynchronizationInformation.addTo(synchronizationInformation, operationStatsType.getSynchronizationInformation());
//                           ActionsExecutedInformation.addTo(actionsExecutedInformation, operationStatsType.getActionsExecutedInformation());
//                       }
//                   });
////                   for (TaskType taskDto : subTasks) {
////                       OperationStatsType operationStats = taskDto.getOperationStats();
////                       if (operationStats != null) {
////                           IterativeTaskInformation.addTo(iterativeTaskInformation, operationStats.getIterativeTaskInformation(), true);
////                           SynchronizationInformation.addTo(synchronizationInformation, operationStats.getSynchronizationInformation());
////                           ActionsExecutedInformation.addTo(actionsExecutedInformation, operationStats.getActionsExecutedInformation());
////                       }
////                   }
//
//                   return new OperationStatsType(getPrismContext())
//                           .iterativeTaskInformation(iterativeTaskInformation)
//                           .synchronizationInformation(synchronizationInformation)
//                           .actionsExecutedInformation(actionsExecutedInformation);
//
//               }
//
//               return task.asObjectable().getOperationStats();

           }
       };
    }

    private List<TaskType> resolveSubTasks() {
        List<TaskType> loadedSubtasks =new ArrayList<>();
        PrismObject<TaskType> taskType = getModelObject().getObject();
        if (taskType == null) {
            return loadedSubtasks; //TODO throw exception?
        }

        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_SUBTASKS);
        OperationResult result = task.getResult();
        resolveSubTasks(taskType.asObjectable(), true, loadedSubtasks, task, result);
        return loadedSubtasks;
    }

    private void resolveSubTasks(TaskType subTask, boolean alreadyLoaded, List<TaskType> allTasks, Task task, OperationResult result) {
        List<TaskType> subTasks = new ArrayList<>();

        PrismObject<TaskType> subTaskWithLoadedSubtasks;
        if (alreadyLoaded) {
            subTaskWithLoadedSubtasks = subTask.asPrismObject();
        } else {
            subTaskWithLoadedSubtasks = WebModelServiceUtils.loadObject(TaskType.class, subTask.getOid(),
                    getSchemaHelper().getOperationOptionsBuilder().item(TaskType.F_SUBTASK).retrieve().build(), getPageBase(), task, result);
        }

        if (subTaskWithLoadedSubtasks == null) {
            return;
        }

        PrismReference subRefs = subTaskWithLoadedSubtasks.findReference(TaskType.F_SUBTASK_REF);
        if (subRefs == null) {
            return;
        }
        for (PrismReferenceValue refVal : subRefs.getValues()) {
            addSubTasks(refVal, subTasks);
        }

        allTasks.addAll(subTasks);

        for (TaskType subLoaded : subTasks) {
            resolveSubTasks(subLoaded, false, allTasks, task, result);
        }

    }

    private void addSubTasks(PrismReferenceValue subTaskRef, List<TaskType> loadedSubtasks) {
        if (subTaskRef == null) {
            return;
        }
        PrismObject<TaskType> subTask = subTaskRef.getObject();
        if (subTask == null) {
            return;
        }

        loadedSubtasks.add(subTask.asObjectable());
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

                Long started = WebComponentUtil.xgc2long(task.getLastRunStartTimestamp());
                if (started == null) {
                    return null;
                }
                Long finished = WebComponentUtil.xgc2long(task.getLastRunFinishTimestamp());
                if (finished == null || finished < started) {
                    finished = System.currentTimeMillis();
                }

                return (finished - started) / objectsTotal;
            }
        };
        infoPanel.setOutputMarkupId(true);
        add(infoPanel);


        LoadableModel<List<SynchronizationInformationDto>> syncInfoModel = new LoadableModel<List<SynchronizationInformationDto>>(true) {
            @Override
            protected List<SynchronizationInformationDto> load() {
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

        ListDataProvider<SynchronizationInformationDto> syncDataProvider = new ListDataProvider<>(this, syncInfoModel);

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



        ListDataProvider<ObjectActionsExecutedEntryType> objectActionsEntry = new ListDataProvider<>(this,
                new PropertyModel<>(statisticsModel, OperationStatsType.F_ACTIONS_EXECUTED_INFORMATION.getLocalPart() + "." + ActionsExecutedInformationType.F_RESULTING_OBJECT_ACTIONS_ENTRY));

        List<IColumn<ObjectActionsExecutedEntryType, String>> actionEntryColumns = new ArrayList<>();
        actionEntryColumns.add(new AbstractColumn<ObjectActionsExecutedEntryType, String>(createStringResource("ObjectActionsExecutedEntryType.objectType")) {
            @Override
            public void populateItem(Item<ICellPopulator<ObjectActionsExecutedEntryType>> item, String id, IModel<ObjectActionsExecutedEntryType> iModel) {
                QName entry = iModel.getObject().getObjectType();
                ObjectTypes objectType = null;
                if (entry != null) {
                    objectType = ObjectTypes.getObjectTypeFromTypeQName(entry);
                }
                item.add(new Label(id, createStringResource(objectType)));
            }
        });
        actionEntryColumns.add(new EnumPropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType.operation"), ObjectActionsExecutedEntryType.F_OPERATION.getLocalPart()));
        actionEntryColumns.add(new AbstractColumn<ObjectActionsExecutedEntryType, String>(createStringResource("ObjectActionsExecutedEntryType.chanel")) {
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
        actionEntryColumns.add(new PropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType.totalSuccessCount"), ObjectActionsExecutedEntryType.F_TOTAL_SUCCESS_COUNT.getLocalPart()));
        actionEntryColumns.add(new PropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType.lastSuccessDisplayName"), ObjectActionsExecutedEntryType.F_LAST_SUCCESS_OBJECT_DISPLAY_NAME.getLocalPart()));
        actionEntryColumns.add(new AbstractColumn<ObjectActionsExecutedEntryType, String>(createStringResource("ObjectActionsExecutedEntryType.lastSuccessTimestamp")) {

            @Override
            public void populateItem(Item<ICellPopulator<ObjectActionsExecutedEntryType>> item, String id, IModel<ObjectActionsExecutedEntryType> iModel) {
                XMLGregorianCalendar timestamp = iModel.getObject().getLastSuccessTimestamp();
                item.add(new Label(id, WebComponentUtil.formatDate(timestamp)));
            }
        });
        actionEntryColumns.add(new PropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType.totalFailureCount"), ObjectActionsExecutedEntryType.F_TOTAL_FAILURE_COUNT.getLocalPart()));

        BoxedTablePanel<ObjectActionsExecutedEntryType> actionTable = new BoxedTablePanel<ObjectActionsExecutedEntryType>(ID_ACTION_ENTRY, objectActionsEntry, actionEntryColumns) {
            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };

        actionTable.setOutputMarkupId(true);
        add(actionTable);

        ListDataProvider<ObjectActionsExecutedEntryType> resultingObjectActionsEntry = new ListDataProvider<>(this,
                new PropertyModel<>(statisticsModel, OperationStatsType.F_ACTIONS_EXECUTED_INFORMATION.getLocalPart() + "." + ActionsExecutedInformationType.F_OBJECT_ACTIONS_ENTRY));

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
        resultingEntryColumns.add(new AbstractColumn<ObjectActionsExecutedEntryType, String>(createStringResource("ObjectActionsExecutedEntryType.channel")) {
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

        BoxedTablePanel<ObjectActionsExecutedEntryType> resultingEntry = new BoxedTablePanel<ObjectActionsExecutedEntryType>(ID_RESULTING_ENTRY, resultingObjectActionsEntry, resultingEntryColumns) {
            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };

        resultingEntry.setOutputMarkupId(true);
        add(resultingEntry);

    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
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

    @Override
    protected void onDetach() {
        super.onDetach();
        statisticsModel.reset();
        IModel<PrismObjectWrapper<TaskType>> taskModel = getModel();
        if (taskModel instanceof LoadableModel) {
            ((LoadableModel<PrismObjectWrapper<TaskType>>) taskModel).reset();
        }
    }
}
