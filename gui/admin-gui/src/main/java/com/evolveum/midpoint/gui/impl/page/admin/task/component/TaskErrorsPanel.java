/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import java.text.DateFormat;
import java.util.*;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.search.panel.NamedIntervalPreset;
import com.evolveum.midpoint.gui.impl.component.search.panel.SearchPanel;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.DateSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.OperationResultUtil;
import com.evolveum.midpoint.schema.util.task.ActivityStateOverviewUtil;
import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskErrorSelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskErrorSelectableBeanImplOld;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by honchar.
 */
@SuppressWarnings("unused")
@PanelType(name = "taskErrors")
@PanelInstance(identifier = "taskErrors", applicableForType = TaskType.class, applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageTask.errors.title", icon = GuiStyleConstants.CLASS_TASK_ERRORS_ICON, order = 85))
public class TaskErrorsPanel extends AbstractObjectMainPanel<TaskType, TaskDetailsModel> implements RefreshableTabPanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_TASK_ERRORS = "taskErrors";

    private static final String ID_SUBTASKS_ERRORS = "subtasksErrors";

    private IModel<Search<OperationExecutionType>> searchModel;

    public TaskErrorsPanel(String id, TaskDetailsModel taskWrapperModel, ContainerPanelConfigurationType config) {
        super(id, taskWrapperModel, config);
    }

    protected void initLayout() {
        searchModel = createSearchModel();

        if (getPageBase().isNativeRepo()) {
            initLayoutNew(); // New repo, searchContainers, see MID-7235
        } else {
            initLayoutOld(); // Old repo, searchObjects
        }
    }

    private void initLayoutNew() {
        var provider = new SelectableBeanContainerDataProvider<>(this, searchModel, null, true) {

            @Override
            protected String getDefaultSortParam() {
                return TaskErrorSelectableBeanImpl.F_ERROR_TIMESTAMP;
            }

            @Override
            protected SortOrder getDefaultSortOrder() {
                return SortOrder.DESCENDING;
            }

            @Override
            public SelectableBean<OperationExecutionType> createDataObjectWrapper(OperationExecutionType obj) {
                return new TaskErrorSelectableBeanImpl(obj);
            }

            @Override
            protected SelectableBean<OperationExecutionType> createDataObjectWrapperForError() {
                return new TaskErrorSelectableBeanImpl();
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return createContentQuery(getObjectWrapper().getOid(), getPageBase());
            }

            @NotNull
            @Override
            protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
                if (sortParam != null && sortParam.getProperty() != null) {
                    OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
                    ItemPath ordering;
                    if (sortParam.getProperty().equals(TaskErrorSelectableBeanImpl.F_ERROR_TIMESTAMP)) {
                        ordering = ItemPath.create(OperationExecutionType.F_TIMESTAMP);
                    } else if (sortParam.getProperty().equals("name")) {
                        // TODO why is this "name" and not TaskErrorSelectableBeanImplNew.F_OBJECT_REF_NAME?
                        ordering = ItemPath.create(PrismConstants.T_PARENT, ObjectType.F_NAME);
                    } else {
                        // TODO this is actually not used
                        ordering = ItemPath.create(new QName(SchemaConstantsGenerated.NS_COMMON, sortParam.getProperty()));
                    }
                    return Collections.singletonList(
                            getPrismContext().queryFactory().createOrdering(ordering, order));
                } else {
                    return Collections.emptyList();
                }
            }

            @Override
            public Class<OperationExecutionType> getType() {
                return OperationExecutionType.class;
            }
        };

        BoxedTablePanel<?> table = new BoxedTablePanel<>(
                ID_TASK_ERRORS, provider, initColumnsNew(), UserProfileStorage.TableId.PANEL_TASK_ERRORS) {

            @Override
            protected Component createHeader(String headerId) {
                return createSearch(headerId);
            }
        };
        table.setOutputMarkupId(true);
        add(table);

        initSubtasksErrorsTable();
    }

    private void initSubtasksErrorsTable() {
        IModel<List<ActivityTaskStateOverviewType>> overviewModel = new LoadableDetachableModel<>() {

            @Override
            protected List<ActivityTaskStateOverviewType> load() {
                PrismObject<TaskType> object = getObjectWrapperObject();
                if (object == null) {
                    return List.of();
                }

                ActivityStateOverviewType rootOverview = ActivityStateOverviewUtil.getStateOverview(object.asObjectable());
                if (rootOverview == null) {
                    return List.of();
                }

                List<ActivityTaskStateOverviewType> result = new ArrayList<>();

                ActivityStateOverviewUtil.StateOverviewVisitor visitor = state -> state.getTask().stream()
                        .filter(o -> OperationResultUtil.isError(o.getResultStatus()))
                        .filter(o -> o.getTaskRef() == null || !Objects.equals(object.getOid(), o.getTaskRef().getOid()))
                        .forEach(o -> result.add(o));

                ActivityStateOverviewUtil.acceptStateOverviewVisitor(rootOverview, visitor);

                result.sort(Comparator.comparing(o -> getTaskName(o)));

                return result;
            }
        };
        var provider = new ListDataProvider(this, overviewModel);

        BoxedTablePanel<?> table = new BoxedTablePanel<>(ID_SUBTASKS_ERRORS, provider, initTaskErrorsColumns());
        table.setOutputMarkupId(true);
        table.add(new VisibleBehaviour(() -> !overviewModel.getObject().isEmpty()));
        add(table);
    }

    private String getTaskName(ActivityTaskStateOverviewType overview) {
        ObjectReferenceType taskRef = overview.getTaskRef();
        if (taskRef == null || taskRef.getOid() == null) {
            return "";
        }

        return taskRef.getTargetName() != null ? taskRef.getTargetName().getOrig() : taskRef.getOid();
    }

    private List<IColumn<ActivityTaskStateOverviewType, String>> initTaskErrorsColumns() {
        List<IColumn<ActivityTaskStateOverviewType, String>> columns = new ArrayList<>();

        columns.add(new AjaxLinkColumn<>(createStringResource("TaskErrorsPanel.taskName")) {

            @Override
            protected IModel createLinkModel(IModel<ActivityTaskStateOverviewType> rowModel) {
                return new LoadableDetachableModel() {

                    @Override
                    protected String load() {
                        ActivityTaskStateOverviewType state = rowModel.getObject();
                        if (state.getTaskRef() == null) {
                            return null;
                        }

                        ObjectReferenceType ref = state.getTaskRef();
                        if (ref.getTargetName() != null) {
                            return ref.getTargetName().getOrig();
                        }

                        return ref.getOid();
                    }
                };
            }

            @Override
            public boolean isEnabled(IModel<ActivityTaskStateOverviewType> rowModel) {
                ObjectReferenceType ref = rowModel.getObject().getTaskRef();
                return ref != null && ref.getOid() != null;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ActivityTaskStateOverviewType> rowModel) {
                ObjectReferenceType ref = rowModel.getObject().getTaskRef();
                if (ref == null || ref.getOid() == null) {
                    return;
                }

                DetailsPageUtil.dispatchToObjectDetailsPage(
                        TaskType.class, ref.getOid(), TaskErrorsPanel.this, true);
            }
        });

        columns.add(new PropertyColumn<>(
                createStringResource("TaskErrorsPanel.node"),
                ActivityTaskStateOverviewType.F_NODE.getLocalPart()));

        columns.add(new EnumPropertyColumn<>(
                createStringResource("TaskErrorsPanel.executionState"),
                ActivityTaskStateOverviewType.F_EXECUTION_STATE.getLocalPart()));

        columns.add(new IconColumn<ActivityTaskStateOverviewType>(createStringResource("TaskErrorsPanel.resultStatus")) {

            @Override
            protected DisplayType getIconDisplayType(IModel<ActivityTaskStateOverviewType> rowModel) {
                OperationResultStatusType status = rowModel.getObject().getResultStatus();
                if (status == null) {
                    status = OperationResultStatusType.UNKNOWN;
                }

                String icon = OperationResultStatusPresentationProperties.parseOperationalResultStatus(status).getIcon();
                String title = createStringResource(status).getString();

                return GuiDisplayTypeUtil.createDisplayType(icon, "", title);
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("TaskErrorsPanel.message")) {

            @Override
            public void populateItem(
                    Item<ICellPopulator<ActivityTaskStateOverviewType>> cellItem,
                    String componentId,
                    IModel<ActivityTaskStateOverviewType> rowModel) {

                IModel<String> model = new LoadableDetachableModel<>() {

                    @Override
                    protected String load() {
                        ActivityTaskStateOverviewType overview = rowModel.getObject();
                        LocalizableMessageType msg = overview.getUserFriendlyMessage();
                        if (msg == null) {
                            return overview.getMessage();
                        }

                        return LocalizationUtil.translateMessage(msg);
                    }
                };

                cellItem.add(new Label(componentId, model));
            }
        });

        return columns;
    }

    private Component createSearch(String headerId) {
        return new SearchPanel<>(headerId, searchModel) {

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                refreshTable(target);
            }
        };
    }

    private IModel<Search<OperationExecutionType>> createSearchModel() {
        return new LoadableDetachableModel<>() {

            @Override
            protected Search<OperationExecutionType> load() {
                PageStorage storage = getSessionStorage().getOrCreatePageStorage(SessionStorage.KEY_TASK_ERRORS_PANEL);
                Search<OperationExecutionType> search = storage != null ? storage.getSearch() : null;
                if (search == null) {
                    SearchBuilder<OperationExecutionType> searchBuilder =
                            new SearchBuilder<>(OperationExecutionType.class)
                                    .additionalSearchContext(createAdditionalSearchContext())
                                    .modelServiceLocator(getPageBase());

                    search = searchBuilder.build();

                    if (storage != null) {
                        storage.setSearch(search);
                        // todo paging?
                    }
                } else {
                    // we have to make sure named time intervals are up to date as the task might be running
                    PropertySearchItemWrapper wrapper = search.findPropertySearchItem(OperationExecutionType.F_TIMESTAMP);
                    if (wrapper instanceof DateSearchItemWrapper dateItem) {
                        List<NamedIntervalPreset> history = createTaskRunNamedIntervals();
                        List<NamedIntervalPreset> presets = createTimestampNamedIntervals(history);

                        dateItem.setIntervalPresets(presets);
                    }
                }

                return search;
            }
        };
    }

    private List<NamedIntervalPreset> createTimestampNamedIntervals(List<NamedIntervalPreset> runHistoryPresets) {
        List<NamedIntervalPreset> presets = new ArrayList<>();
        presets.addAll(runHistoryPresets);
        presets.addAll(NamedIntervalPreset.DEFAULT_PRESETS);

        return presets;
    }

    private List<NamedIntervalPreset> createTaskRunNamedIntervals() {
        TaskType task = getObjectWrapperObject().asObjectable();
        return task.getTaskRunRecord().stream()
                .map(r -> {
                    Long start = r.getRunStartTimestamp() != null ?
                            r.getRunStartTimestamp().toGregorianCalendar().getTimeInMillis() : null;
                    Long end = r.getRunEndTimestamp() != null ?
                            r.getRunEndTimestamp().toGregorianCalendar().getTimeInMillis() : null;

                    return Pair.of(start, end);
                })
                .map(p -> {
                    SingleLocalizableMessage msg = p.getRight() != null ?
                            // previous run
                            new SingleLocalizableMessage(
                                    "TaskErrorsPanel.runHistoryPreset",
                                    new Object[] {
                                            p.getLeft() != null ? WebComponentUtil.formatDate(DateFormat.SHORT, new Date(p.getLeft())) : "",
                                            p.getRight() != null ? WebComponentUtil.formatDate(DateFormat.SHORT, new Date(p.getRight())) : ""
                                    })
                            :
                            // current run
                            new SingleLocalizableMessage("TaskErrorsPanel.showCurrentRun");

                    return new NamedIntervalPreset(() -> p.getLeft(), () -> p.getRight(), null, msg);
                })
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    private SearchContext createAdditionalSearchContext() {
        SearchContext ctx = new SearchContext();

        List<NamedIntervalPreset> runHistoryPresets = createTaskRunNamedIntervals();

        List<NamedIntervalPreset> presets = createTimestampNamedIntervals(runHistoryPresets);

        ctx.setIntervalPresets(OperationExecutionType.F_TIMESTAMP, presets);

        NamedIntervalPreset selected = !runHistoryPresets.isEmpty() ? runHistoryPresets.get(0) : NamedIntervalPreset.LAST_1_DAY;
        ctx.setSelectedIntervalPreset(OperationExecutionType.F_TIMESTAMP, selected);

        return ctx;
    }

    private boolean hasSingleActivityStatePersistence() {
        TaskType task = getObjectWrapperObject().asObjectable();
        TaskInformation info = TaskInformation.createForTask(task, task);
        return info.getRootActivityStatePersistence() == ActivityStatePersistenceType.SINGLE_REALIZATION;
    }

    private void refreshTable(AjaxRequestTarget target) {
        target.add(get(ID_TASK_ERRORS));
        target.add(getPageBase().getFeedbackPanel());
    }

    private List<IColumn<TaskErrorSelectableBeanImpl, String>> initColumnsNew() {
        List<IColumn<TaskErrorSelectableBeanImpl, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.taskErros.objectName"), TaskErrorSelectableBeanImpl.F_OBJECT_REF_NAME) {
            @Override
            public String getSortProperty() {
                return "name";
            }
        });
        columns.add(new AbstractColumn<>(createStringResource("pageTaskEdit.taskErros.timestamp"), TaskErrorSelectableBeanImpl.F_ERROR_TIMESTAMP) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<TaskErrorSelectableBeanImpl>> cellItem, String componentId,
                    IModel<TaskErrorSelectableBeanImpl> rowModel) {
                Label label = new Label(componentId, (IModel<String>) () ->
                        WebComponentUtil.getShortDateTimeFormattedValue(rowModel.getObject().getErrorTimestamp(), getPageBase()));
                cellItem.add(label);
            }
        });
        columns.add(new EnumPropertyColumn<>(createStringResource("pageTaskEdit.taskErros.status"), TaskErrorSelectableBeanImpl.F_STATUS));
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.taskErros.message"), TaskErrorSelectableBeanImpl.F_MESSAGE));
        columns.add(new EnumPropertyColumn<>(createStringResource("pageTaskEdit.taskErros.recordType"), TaskErrorSelectableBeanImpl.F_RECORD_TYPE));
        columns.add(new AjaxLinkColumn<>(createStringResource("pageTaskEdit.taskErros.realOwner"), TaskErrorSelectableBeanImpl.F_REAL_OWNER_DESCRIPTION) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<TaskErrorSelectableBeanImpl> rowModel) {
                TaskErrorSelectableBeanImpl object = rowModel.getObject();
                PrismObject<ObjectType> realOwner = object.getRealOwner();
                DetailsPageUtil.dispatchToObjectDetailsPage(
                        realOwner.getCompileTimeClass(), realOwner.getOid(), TaskErrorsPanel.this, false);
            }
        });

        return columns;
    }

    private void initLayoutOld() {
        SelectableBeanObjectDataProvider<? extends ObjectType> provider = new SelectableBeanObjectDataProvider<>(this, null) {

            @Override
            protected String getDefaultSortParam() {
                return TaskErrorSelectableBeanImplOld.F_ERROR_TIMESTAMP;
            }

            @Override
            protected SortOrder getDefaultSortOrder() {
                return SortOrder.DESCENDING;
            }

            @Override
            public SelectableBean<ObjectType> createDataObjectWrapper(ObjectType obj) {
                return new TaskErrorSelectableBeanImplOld<>(obj, getObjectWrapper().getOid());
            }

            @Override
            protected SelectableBean<ObjectType> createDataObjectWrapperForError() {
                return new TaskErrorSelectableBeanImplOld<>();
            }

            @Override
            public ObjectQuery getQuery() {
                return createContentQuery(getObjectWrapper().getOid(), getPageBase());
            }

            @NotNull
            @Override
            protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
                if (sortParam != null && sortParam.getProperty() != null) {
                    OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
                    ItemPath ordering;
                    if (sortParam.getProperty().equals(TaskErrorSelectableBeanImplOld.F_ERROR_TIMESTAMP)) {
                        ordering = ItemPath.create("operationExecution", "timestamp");
                    } else {
                        ordering = ItemPath.create(new QName(SchemaConstantsGenerated.NS_COMMON, sortParam.getProperty()));
                    }
                    return Collections.singletonList(
                            getPrismContext().queryFactory().createOrdering(
                                    ordering, order));
                } else {
                    return Collections.emptyList();
                }
            }

            @Override
            public Class<ObjectType> getType() {
                return ObjectType.class;
            }
        };

        BoxedTablePanel<TaskErrorSelectableBeanImplOld<ObjectType>> table = new BoxedTablePanel<>(ID_TASK_ERRORS, provider, initColumnsOld());
        table.setOutputMarkupId(true);
        add(table);
    }

    private List<IColumn<TaskErrorSelectableBeanImplOld<ObjectType>, String>> initColumnsOld() {
        List<IColumn<TaskErrorSelectableBeanImplOld<ObjectType>, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.taskErros.objectName"), TaskErrorSelectableBeanImplOld.F_OBJECT_REF_NAME) {
            @Override
            public String getSortProperty() {
                return "name";
            }
        });
        columns.add(new AbstractColumn<>(createStringResource("pageTaskEdit.taskErros.timestamp"), TaskErrorSelectableBeanImplOld.F_ERROR_TIMESTAMP) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<TaskErrorSelectableBeanImplOld<ObjectType>>> cellItem, String componentId,
                    IModel<TaskErrorSelectableBeanImplOld<ObjectType>> rowModel) {
                Label label = new Label(componentId, (IModel<String>) () ->
                        WebComponentUtil.getShortDateTimeFormattedValue(rowModel.getObject().getErrorTimestamp(), getPageBase()));
                cellItem.add(label);
            }
        });
        columns.add(new EnumPropertyColumn<>(createStringResource("pageTaskEdit.taskErros.status"), TaskErrorSelectableBeanImplOld.F_STATUS));
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.taskErros.message"), TaskErrorSelectableBeanImplOld.F_MESSAGE));
        columns.add(new EnumPropertyColumn<>(createStringResource("pageTaskEdit.taskErros.recordType"), TaskErrorSelectableBeanImplOld.F_RECORD_TYPE));
        columns.add(new AjaxLinkColumn<>(createStringResource("pageTaskEdit.taskErros.realOwner"), TaskErrorSelectableBeanImplOld.F_REAL_OWNER_DESCRIPTION) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<TaskErrorSelectableBeanImplOld<ObjectType>> rowModel) {
                TaskErrorSelectableBeanImplOld<ObjectType> object = rowModel.getObject();
                PrismObject<ObjectType> realOwner = object.getRealOwner();
                if (realOwner != null) {
                    DetailsPageUtil.dispatchToObjectDetailsPage(
                            realOwner.getCompileTimeClass(), realOwner.getOid(), TaskErrorsPanel.this, false);
                }
            }
        });

        return columns;
    }

    private ObjectQuery createContentQuery(String taskOid, PageBase pageBase) {
        if (getPageBase().isNativeRepo()) {
            return getPrismContext().queryFor(OperationExecutionType.class)
                    .item(OperationExecutionType.F_TASK_REF).ref(taskOid)
                    .and()
                    // new repo allows EQ with multiple values meaning IN
                    .item(OperationExecutionType.F_STATUS).eq(OperationResultStatusType.FATAL_ERROR,
                            OperationResultStatusType.PARTIAL_ERROR, OperationResultStatusType.WARNING)
                    .build();
        } else {
            return getPrismContext().queryFor(ObjectType.class)
                    .exists(ObjectType.F_OPERATION_EXECUTION)
                    .block()
                    .item(OperationExecutionType.F_TASK_REF).ref(taskOid)
                    .and()
                    .block().item(OperationExecutionType.F_STATUS)
                    .eq(OperationResultStatusType.FATAL_ERROR)
                    .or().item(OperationExecutionType.F_STATUS)
                    .eq(OperationResultStatusType.PARTIAL_ERROR)
                    .or().item(OperationExecutionType.F_STATUS)
                    .eq(OperationResultStatusType.WARNING)
                    .endBlock()
                    .endBlock()
                    .build();
        }
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.singleton(get(ID_TASK_ERRORS));
    }

    @Override
    protected void detachModel() {
        super.detachModel();
        getObjectWrapperModel().reset();
    }
}
