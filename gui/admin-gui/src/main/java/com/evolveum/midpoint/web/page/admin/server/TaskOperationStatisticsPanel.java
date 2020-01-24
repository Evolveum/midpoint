package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.web.page.admin.server.currentState.IterativeInformationPanelNew;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.transaction.Synchronization;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;

public class TaskOperationStatisticsPanel extends BasePanel<PrismContainerValueWrapper<OperationStatsType>> {

    private static final String ID_PROCESSING_INFO = "processingInfo";
    private static final String ID_SYNCHORNIZATION_SITUATIONS = "synchronizationSituation";
    private static final String ID_ACTION_ENTRY = "actionEntry";
    private static final String ID_RESULTING_ENTRY = "resultingEntry";

    private static final Collection<String> WALL_CLOCK_AVG_CATEGORIES = Arrays.asList(
            TaskCategory.BULK_ACTIONS, TaskCategory.IMPORTING_ACCOUNTS, TaskCategory.RECOMPUTATION, TaskCategory.RECONCILIATION,
            TaskCategory.UTIL       // this is a megahack: only utility tasks that count objects are DeleteTask and ShadowIntegrityCheck
    );

    public TaskOperationStatisticsPanel(String id, IModel<PrismContainerValueWrapper<OperationStatsType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {

//        LoadableModel<IterativeTaskInformationType> iterativeInfoModel = new LoadableModel<IterativeTaskInformationType>(true) {
//
//            @Override
//            protected IterativeTaskInformationType load() {
//                PrismObjectWrapper<TaskType> taskWrapper = getModelObject().getParent().findObjectWrapper();
//                PrismReferenceWrapper<ObjectReferenceType> refWrappers = taskWrapper.findReference(TaskType.F_SUBTASK_REF);
//                List<PrismReferenceValueWrapperImpl<ObjectReferenceType>> refs = refWrappers.getValues();
//                for (PrismReferenceValueWrapperImpl<ObjectReferenceType> ref : refs) {
//
//                }
//
//            }
//        }
        PrismPropertyWrapperModel<OperationStatsType, IterativeTaskInformationType> stats = PrismPropertyWrapperModel.fromContainerValueWrapper(getModel(), ItemName.fromQName(OperationStatsType.F_ITERATIVE_TASK_INFORMATION));

        IterativeInformationPanelNew infoPanel = new IterativeInformationPanelNew(ID_PROCESSING_INFO, new ItemRealValueModel<>(new PropertyModel<>(stats, "value"))) {


//            @Override
//            protected Long getWallClockAverage(int objectsTotal) {
//                if (objectsTotal == 0) {
//                    return null;
//                }
//
//                TaskType task = (TaskType) stats.getObject().findObjectWrapper().getObjectOld().asObjectable();
//                if (!WALL_CLOCK_AVG_CATEGORIES.contains(task.getCategory())) {
//                    return null;
//                }
//
//                Long started = WebComponentUtil.xgc2long(task.getLastRunStartTimestamp());
//                if (started == null) {
//                    return null;
//                }
//                Long finished = WebComponentUtil.xgc2long(task.getLastRunFinishTimestamp());
//                if (finished == null || finished < started) {
//                    finished = System.currentTimeMillis();
//                }
//
//                return (finished - started) / objectsTotal;
//            }
        };
        add(infoPanel);


        LoadableModel<List<SynchronizationInforationDto>> syncInfoModel = new LoadableModel<List<SynchronizationInforationDto>>(true) {
            @Override
            protected List load() {
                PrismContainerValueWrapper<OperationStatsType> stats = TaskOperationStatisticsPanel.this.getModelObject();
                OperationStatsType statsType = stats.getRealValue();
                if (statsType == null) {
                    return null;
                }

                SynchronizationInformationType syncInfo = statsType.getSynchronizationInformation();
                if(syncInfo == null) {
                    return null;
                }

                List<SynchronizationInforationDto> infos = new ArrayList<>();
                infos.add(new SynchronizationInforationDto("Unmatched",syncInfo.getCountUnmatched(), syncInfo.getCountUnmatchedAfter()));
                infos.add(new SynchronizationInforationDto("Unlinked",syncInfo.getCountUnlinked(), syncInfo.getCountUnlinkedAfter()));
                infos.add(new SynchronizationInforationDto("Linked",syncInfo.getCountLinked(), syncInfo.getCountLinkedAfter()));
                infos.add(new SynchronizationInforationDto("Deleted",syncInfo.getCountDeleted(), syncInfo.getCountDeletedAfter()));
                infos.add(new SynchronizationInforationDto("Disputed",syncInfo.getCountDisputed(), syncInfo.getCountDisputedAfter()));
                infos.add(new SynchronizationInforationDto("Protected",syncInfo.getCountProtected(), syncInfo.getCountProtectedAfter()));
                infos.add(new SynchronizationInforationDto("No sync policy",syncInfo.getCountNoSynchronizationPolicy(), syncInfo.getCountNoSynchronizationPolicyAfter()));
                infos.add(new SynchronizationInforationDto("Sync disabled",syncInfo.getCountSynchronizationDisabled(), syncInfo.getCountSynchronizationDisabledAfter()));
                infos.add(new SynchronizationInforationDto("Not applicable for task",syncInfo.getCountNotApplicableForTask(), syncInfo.getCountNotApplicableForTaskAfter()));

                return infos;
            }
        };

        ListDataProvider<SynchronizationInforationDto> syncDataProvider = new ListDataProvider<>(this, syncInfoModel);

        List<IColumn<String, String>> syncColumns = new ArrayList<>();
        syncColumns.add(new PropertyColumn<>(createStringResource("TaskOperationStatisticsPanel.situation"), SynchronizationInforationDto.F_ACTION_NAME));
        syncColumns.add(new PropertyColumn<>(createStringResource("TaskOperationStatisticsPanel.countBefore"), SynchronizationInforationDto.F_COUNT_BEFORE));
        syncColumns.add(new PropertyColumn<>(createStringResource("TaskOperationStatisticsPanel.countAfter"), SynchronizationInforationDto.F_COUNT_AFTER));

        BoxedTablePanel table = new BoxedTablePanel(ID_SYNCHORNIZATION_SITUATIONS, syncDataProvider, syncColumns) {
            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };

        add(table);


        PrismPropertyWrapperModel<OperationStatsType, ActionsExecutedInformationType> executedActions = PrismPropertyWrapperModel.fromContainerValueWrapper(getModel(), ItemName.fromQName(OperationStatsType.F_ACTIONS_EXECUTED_INFORMATION));
        ItemRealValueModel<ActionsExecutedInformationType> executedActionsValue = new ItemRealValueModel<>(new PropertyModel<>(executedActions, "value"));

        ListDataProvider<ObjectActionsExecutedEntryType> objectActionsEntry = new ListDataProvider<>(this, new PropertyModel<>(executedActionsValue, "objectActionsEntry"));

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

        BoxedTablePanel actionTable = new BoxedTablePanel(ID_ACTION_ENTRY, objectActionsEntry, actionEntryColumns) {
            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };

        add(actionTable);

        ListDataProvider<ObjectActionsExecutedEntryType> resultingObjectActionsEntry = new ListDataProvider<>(this, new PropertyModel<>(executedActionsValue, "resultingObjectActionsEntry"));

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
        resultingEntryColumns.add(new PropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType.successCount"), ObjectActionsExecutedEntryType.F_TOTAL_SUCCESS_COUNT.getLocalPart()));
        resultingEntryColumns.add(new PropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType.lastSuccessObject"), ObjectActionsExecutedEntryType.F_LAST_SUCCESS_OBJECT_DISPLAY_NAME.getLocalPart()));
        resultingEntryColumns.add(new AbstractColumn<ObjectActionsExecutedEntryType, String>(createStringResource("ObjectActionsExecutedEntryType.lastSuccessTimestamp")) {
            @Override
            public void populateItem(Item<ICellPopulator<ObjectActionsExecutedEntryType>> item, String id, IModel<ObjectActionsExecutedEntryType> iModel) {
                XMLGregorianCalendar timestamp = iModel.getObject().getLastSuccessTimestamp();
                item.add(new Label(id, WebComponentUtil.formatDate(timestamp)));
            }
        });
        resultingEntryColumns.add(new PropertyColumn<>(createStringResource("ObjectActionsExecutedEntryType.failureCount"), ObjectActionsExecutedEntryType.F_TOTAL_FAILURE_COUNT.getLocalPart()));

        BoxedTablePanel resultingEntry = new BoxedTablePanel(ID_RESULTING_ENTRY, resultingObjectActionsEntry, resultingEntryColumns) {
            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };

        add(resultingEntry);

    }

    class SynchronizationInforationDto implements Serializable {

        public static final String F_ACTION_NAME = "actionName";
        public static final String F_COUNT_BEFORE = "countBefore";
        public static final String F_COUNT_AFTER = "countAfter";

        private String actionName;
        private int countBefore;
        private int countAfter;

        public SynchronizationInforationDto(String actionName, int countBefore, int countAfter) {
            this.actionName = actionName;
            this.countBefore = countBefore;
            this.countAfter = countAfter;
        }
    }
}
