package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ActionsExecutedInformation;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/task", matchUrlForSecurity = "/admin/task")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                        label = "PageAdminUsers.auth.usersAll.label",
                        description = "PageAdminUsers.auth.usersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_URL,
                        label = "PageUser.auth.user.label",
                        description = "PageUser.auth.user.description")
        })
public class PageTask extends PageAdminObjectDetails<TaskType> {
    private static final long serialVersionUID = 1L;

    private static final transient Trace LOGGER = TraceManager.getTrace(PageTask.class);

    private static final String DOT_CLASS = PageTask.class.getName() + ".";
    private static final String OPERATION_LOAD_SUBTASKS = DOT_CLASS + "loadSubtasks";
    private static final String OPERATION_CREATE_STATS_WRAPPER = DOT_CLASS + "createStatsWrapper";


    private LoadableModel<List<TaskType>> subTasksModel;

    public PageTask() {
        initialize(null);
    }

    public PageTask(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> buildGetOptions() {
        return getOperationOptionsBuilder()
                // retrieve
                .item(TaskType.F_SUBTASK).retrieve()
                .item(TaskType.F_NODE_AS_OBSERVED).retrieve()
                .item(TaskType.F_NEXT_RUN_START_TIMESTAMP).retrieve()
                .item(TaskType.F_NEXT_RETRY_TIMESTAMP).retrieve()
                .item(TaskType.F_RESULT).retrieve()         // todo maybe only when it is to be displayed
                .build();
    }

    public PageTask(final PrismObject<TaskType> taskToEdit) {
        initialize(taskToEdit);
    }

    public PageTask(final PrismObject<TaskType> taskToEdit, boolean isNewObject)  {
        initialize(taskToEdit, isNewObject);
    }

    @Override
    public Class<TaskType> getCompileTimeClass() {
        return TaskType.class;
    }

    @Override
    protected TaskType createNewObject() {
        return new TaskType();
    }

    @Override
    protected ObjectSummaryPanel<TaskType> createSummaryPanel() {
        return new TaskSummaryPanelNew(ID_SUMMARY_PANEL, isEditingFocus() ?
                Model.of(getObjectModel().getObject().getObject().asObjectable()) : Model.of(), this);
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

    private void resolveSubTasks(TaskType subTask, boolean alreadyLoaded, List<TaskType> allTasks, Task task, OperationResult result) {
        List<TaskType> subTasks = new ArrayList<>();

        PrismObject<TaskType> subTaskWithLoadedSubtasks;
        if (alreadyLoaded) {
            subTaskWithLoadedSubtasks = subTask.asPrismObject();
        } else {
            subTaskWithLoadedSubtasks = WebModelServiceUtils.loadObject(TaskType.class, subTask.getOid(), getOperationOptionsBuilder().item(TaskType.F_SUBTASK).retrieve().build(), PageTask.this, task, result);
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

    @Override
    protected AbstractObjectMainPanel<TaskType> createMainPanel(String id) {

        subTasksModel = new LoadableModel<List<TaskType>>(true) {

            @Override
            protected List<TaskType> load() {
//                try {

                    List<TaskType> loadedSubtasks =new ArrayList<>();
                    PrismObject<TaskType> taskType = getObjectWrapper().getObject();
                    if (taskType == null) {
                        return null; //TODO throw exception?
                    }

                    Task task = createSimpleTask(OPERATION_LOAD_SUBTASKS);
                    OperationResult result = task.getResult();
                    resolveSubTasks(taskType.asObjectable(), true, loadedSubtasks, task, result);
//                    PrismReferenceWrapper ref = PageTask.this.getObjectWrapper().findReference(TaskType.F_SUBTASK_REF);
//
//                    if (ref == null || CollectionUtils.isEmpty(ref.getValues())) {
//                        LOGGER.trace("No subtasks, nothing will be loaded.");
//                        return null;
//                    }
//
//                    Task task = createSimpleTask(OPERATION_LOAD_SUBTASKS);
//                    OperationResult result = task.getResult();
//                    List<PrismReferenceValueWrapperImpl> refValues = ref.getValues();
//
//
//                    for (PrismReferenceValueWrapperImpl<Referencable> refValue : refValues) {
//                        PrismReferenceValue subTaskRef = refValue.getOldValue();
//                        addSubTasks(subTaskRef, loadedSubtasks);
//                    }
//
//                    for (TaskType subTask : loadedSubtasks) {
//                        resolveSubTasks(subTask, loadedSubtasks, task, result);
//                    }
//
//
//
//                    result.recomputeStatus();
//                    showResult(result, false);

                    return loadedSubtasks;
//                } catch (SchemaException e) {
//                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot load subtasks of task {}", e, getObjectWrapper().getObjectOld());
//                    return null;
//                }
            }
        };

        return new AbstractObjectMainPanel<TaskType>(id, getObjectModel(), this) {

            @Override
            protected List<ITab> createTabs(PageAdminObjectDetails<TaskType> parentPage) {
                List<ITab> tabs = new ArrayList<>();
                tabs.add(new AbstractTab(createStringResource("pageTask.basic.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return createContainerPanel(panelId, TaskType.COMPLEX_TYPE, getObjectModel());
                    }
                });

                tabs.add(new AbstractTab(createStringResource("pageTask.basic.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return createContainerPanel(panelId, TaskType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_SCHEDULE));
                    }
                });

                tabs.add(new AbstractTab(createStringResource("pageTask.operationStats.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return createContainerPanel(panelId, TaskType.COMPLEX_TYPE, createOperationStatsModel());
                    }
                });
                return tabs;
            }
        };
    }

    private IModel<PrismContainerWrapper<OperationStatsType>> createOperationStatsModel() {

        PrismObject<TaskType> task = getObjectWrapper().getObject();

        IterativeTaskInformationType iterativeTaskInformation = new IterativeTaskInformationType();
        SynchronizationInformationType synchronizationInformation = new SynchronizationInformationType();
        ActionsExecutedInformationType actionsExecutedInformation = new ActionsExecutedInformationType();

        if (TaskTypeUtil.isPartitionedMaster(task.asObjectable())) {
            List<TaskType> subTasks = subTasksModel.getObject();
            for (TaskType taskDto : subTasks) {
                OperationStatsType operationStats = taskDto.getOperationStats();
                if (operationStats != null) {
                    IterativeTaskInformation.addTo(iterativeTaskInformation, operationStats.getIterativeTaskInformation(), true);
                    SynchronizationInformation.addTo(synchronizationInformation, operationStats.getSynchronizationInformation());
                    ActionsExecutedInformation.addTo(actionsExecutedInformation, operationStats.getActionsExecutedInformation());
                }
            }

            OperationStatsType aggregatedStats = new OperationStatsType(getPrismContext())
                    .iterativeTaskInformation(iterativeTaskInformation)
                    .synchronizationInformation(synchronizationInformation)
                    .actionsExecutedInformation(actionsExecutedInformation);

            PrismContainer<OperationStatsType> statsContainer = getPrismContext().itemFactory().createContainer(TaskType.F_OPERATION_STATS, task.getDefinition().findContainerDefinition(TaskType.F_OPERATION_STATS));
            try {
                statsContainer.setValue(aggregatedStats.asPrismContainerValue());
                Task operationTask = createSimpleTask(OPERATION_CREATE_STATS_WRAPPER);
                WrapperContext ctx = new WrapperContext(operationTask, operationTask.getResult());
                ctx.setCreateIfEmpty(true);
                ctx.setReadOnly(Boolean.TRUE);

                PrismContainerWrapper<OperationStatsType> statsContainerWrapper = createItemWrapper(statsContainer, ItemStatus.NOT_CHANGED, ctx);
                return Model.of(statsContainerWrapper);
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot create wrapper for operation stats: {}", e);
                return null;
            }

        }
        return PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_OPERATION_STATS);
    }

    private <C extends Containerable, T extends Containerable> Panel createContainerPanel(String id, QName typeName, IModel<? extends PrismContainerWrapper<C>> model) {
            try {
                ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder()
                        .visibilityHandler(wrapper -> getVisibility(wrapper.getPath()))
                        .showOnTopLevel(true);
                Panel panel = initItemPanel(id, typeName, model, builder.build());
                return panel;
            } catch (SchemaException e) {
                LOGGER.error("Cannot create panel for {}, {}", typeName, e.getMessage(), e);
                getSession().error("Cannot create panel for " + typeName); // TODO opertion result? localization?
            }

            return null;
    }

    private ItemVisibility getVisibility(ItemPath path) {
        return ItemVisibility.AUTO;
    }

    @Override
    protected Class<? extends Page> getRestartResponsePage() {
        return PageTasks.class;
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, OperationResult result, boolean returningFromAsync) {

    }

    @Override
    public void continueEditing(AjaxRequestTarget target) {

    }
}
