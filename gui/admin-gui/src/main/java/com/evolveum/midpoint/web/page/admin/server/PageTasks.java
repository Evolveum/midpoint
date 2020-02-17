/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.column.ObjectReferenceColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/tasks2", matchUrlForSecurity = "/admin/tasks2")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminTasks.AUTHORIZATION_TASKS_ALL,
                        label = PageAdminTasks.AUTH_TASKS_ALL_LABEL,
                        description = PageAdminTasks.AUTH_TASKS_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASKS_URL,
                        label = "PageTasks.auth.tasks.label",
                        description = "PageTasks.auth.tasks.description")})
public class PageTasks extends PageAdmin {

    private static final String ID_TABLE = "table";

    public static final String SELECTED_CATEGORY = "category";

    public static final long WAIT_FOR_TASK_STOP = 2000L;

    public static final long RUNS_CONTINUALLY = -1L;
    public static final long ALREADY_PASSED = -2L;
    public static final long NOW = 0L;

    public PageTasks() {
        this(null);
    }

    public PageTasks(PageParameters params) {
        super(params);

        TaskTablePanel tablePanel = new TaskTablePanel(ID_TABLE, UserProfileStorage.TableId.TABLE_TASKS, createOperationOptions()) {

            @Override
            protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
                if (query == null) {
                    query = getPrismContext().queryFactory().createQuery();
                }
                query.addFilter(getPrismContext().queryFor(TaskType.class)
                        .item(TaskType.F_PARENT)
                        .isNull()
                        .buildFilter());
                return query;
            }

            @Override
            protected List<IColumn<SelectableBean<TaskType>, String>> createColumns() {
                List<IColumn<SelectableBean<TaskType>, String>> columns = super.createColumns();
                addCustomColumns(columns);
                return columns;
            }
        };
        add(tablePanel);
    }

    private Collection<? extends IColumn<SelectableBean<TaskType>, String>> addCustomColumns(List<IColumn<SelectableBean<TaskType>, String>> columns) {
        columns.add(2, new ObjectReferenceColumn<SelectableBean<TaskType>>(createStringResource("pageTasks.task.objectRef"), SelectableBeanImpl.F_VALUE+"."+TaskType.F_OBJECT_REF.getLocalPart()){
            @Override
            public IModel<ObjectReferenceType> extractDataModel(IModel<SelectableBean<TaskType>> rowModel) {
                SelectableBean<TaskType> bean = rowModel.getObject();
                return Model.of(bean.getValue().getObjectRef());

            }
        });
        columns.add(3, new PropertyColumn<>(createStringResource("pageTasks.task.executingAt"), SelectableBeanImpl.F_VALUE + "." + TaskType.F_NODE_AS_OBSERVED.getLocalPart()));
        columns.add(4, new AbstractExportableColumn<SelectableBean<TaskType>, String>(createStringResource("pageTasks.task.currentRunTime"), TaskType.F_COMPLETION_TIMESTAMP.getLocalPart()) {

            @Override
            public void populateItem(final Item<ICellPopulator<SelectableBean<TaskType>>> item, final String componentId,
                                     final IModel<SelectableBean<TaskType>> rowModel) {

                DateLabelComponent dateLabel = new DateLabelComponent(componentId, new IModel<Date>() {

                    @Override
                    public Date getObject() {
                        Date date = getCurrentRuntime(rowModel);
                        SelectableBean<TaskType> task = rowModel.getObject();
                        if (getRawExecutionStatus(task.getValue()) == TaskExecutionStatus.CLOSED && date != null) {
                            ((DateLabelComponent) item.get(componentId)).setBefore(createStringResource("pageTasks.task.closedAt").getString() + " ");
                        } else if (date != null) {
                            ((DateLabelComponent) item.get(componentId))
                                    .setBefore(WebComponentUtil.formatDurationWordsForLocal(date.getTime(), true, true, PageTasks.this));
                        }
                        return date;
                    }
                }, WebComponentUtil.getShortDateTimeFormat(PageTasks.this));
                item.add(dateLabel);
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<TaskType>> rowModel) {
                SelectableBean<TaskType> task = rowModel.getObject();
                Date date = getCurrentRuntime(rowModel);
                String displayValue = "";
                if (date != null) {
                    if (getRawExecutionStatus(task.getValue()) == TaskExecutionStatus.CLOSED) {
                        displayValue =
                                createStringResource("pageTasks.task.closedAt").getString() +
                                        WebComponentUtil.getShortDateTimeFormattedValue(date, PageTasks.this);
                    } else {
                        displayValue = WebComponentUtil.formatDurationWordsForLocal(date.getTime(), true, true, PageTasks.this);
                    }
                }
                return Model.of(displayValue);
            }
        });
        columns.add(5, new AbstractExportableColumn<SelectableBean<TaskType>, String>(createStringResource("pageTasks.task.scheduledToRunAgain")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<TaskType>>> item, String componentId,
                                     final IModel<SelectableBean<TaskType>> rowModel) {
                item.add(new Label(componentId, () -> createScheduledToRunAgain(rowModel)));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<TaskType>> rowModel) {
                return Model.of(createScheduledToRunAgain(rowModel));
            }
        });
        return columns;
    }


    private Collection<SelectorOptions<GetOperationOptions>> createOperationOptions() {
        List<QName> propertiesToGet = new ArrayList<>();
        propertiesToGet.add(TaskType.F_NODE_AS_OBSERVED);
        propertiesToGet.add(TaskType.F_NEXT_RUN_START_TIMESTAMP);
        propertiesToGet.add(TaskType.F_NEXT_RETRY_TIMESTAMP);

        GetOperationOptionsBuilder getOperationOptionsBuilder = getSchemaHelper().getOperationOptionsBuilder();
        getOperationOptionsBuilder = getOperationOptionsBuilder.resolveNames();
        Collection<SelectorOptions<GetOperationOptions>> searchOptions = getOperationOptionsBuilder
                .items(propertiesToGet.toArray(new Object[0])).retrieve()
                .build();
        return searchOptions;
    }

    private Date getCurrentRuntime(IModel<SelectableBean<TaskType>> taskModel) {
        TaskType task = taskModel.getObject().getValue();

        if (getRawExecutionStatus(task) == TaskExecutionStatus.CLOSED) {

            Long time = getCompletionTimestamp(task);
            if (time == null) {
                return null;
            }
            return new Date(time);

        }
        return null;
    }

    public Long getCompletionTimestamp(TaskType taskType) {
        return xgc2long(taskType.getCompletionTimestamp());
    }

    private String createScheduledToRunAgain(IModel<SelectableBean<TaskType>> taskModel) {
        TaskType task = taskModel.getObject().getValue();
        boolean runnable = getRawExecutionStatus(task) == TaskExecutionStatus.RUNNABLE;
        Long scheduledAfter = getScheduledToStartAgain(taskModel.getObject());
        Long retryAfter = runnable ? getRetryAfter(task) : null;

        if (scheduledAfter == null) {
            if (retryAfter == null || retryAfter <= 0) {
                return "";
            }
        } else if (scheduledAfter == NOW) { // TODO what about retryTime?
            return getString(runnable ? "pageTasks.now" : "pageTasks.nowForNotRunningTasks");
        } else if (scheduledAfter == RUNS_CONTINUALLY) {    // retryTime is probably null here
            return getString("pageTasks.runsContinually");
        } else if (scheduledAfter == ALREADY_PASSED && retryAfter == null) {
            return getString(runnable ? "pageTasks.alreadyPassed" : "pageTasks.alreadyPassedForNotRunningTasks");
        }

        long displayTime;
        boolean displayAsRetry;
        if (retryAfter != null && retryAfter > 0 && (scheduledAfter == null || scheduledAfter < 0
                || retryAfter < scheduledAfter)) {
            displayTime = retryAfter;
            displayAsRetry = true;
        } else {
            displayTime = scheduledAfter;
            displayAsRetry = false;
        }

        String key;
        if (runnable) {
            key = displayAsRetry ? "pageTasks.retryIn" : "pageTasks.in";
        } else {
            key = "pageTasks.inForNotRunningTasks";
        }

        return PageBase.createStringResourceStatic(this, key, DurationFormatUtils.formatDurationWords(displayTime, true, true))
                .getString();
    }

    public Long getScheduledToStartAgain(SelectableBean<TaskType> taskBean) {
        long current = System.currentTimeMillis();

        if (getExecution(taskBean.getValue()) == TaskDtoExecutionStatus.RUNNING) {

            if (TaskRecurrenceType.RECURRING != taskBean.getValue().getRecurrence()) {
                return null;
            } else if (TaskBindingType.TIGHT == taskBean.getValue().getBinding()) {
                return RUNS_CONTINUALLY;             // runs continually; todo provide some information also in this case
            }
        }

        Long nextRunStartTimeLong = getNextRunStartTimeLong(taskBean.getValue());
        if (nextRunStartTimeLong == null || nextRunStartTimeLong == 0) {
            return null;
        }

        if (nextRunStartTimeLong > current + 1000) {
            return nextRunStartTimeLong - System.currentTimeMillis();
        } else if (nextRunStartTimeLong < current - 60000) {
            return ALREADY_PASSED;
        } else {
            return NOW;
        }
    }

    public Long getRetryAfter(TaskType taskType) {
        Long retryAt = getNextRetryTimeLong(taskType);
        return retryAt != null ? retryAt - System.currentTimeMillis() : null;
    }

    public Long getNextRetryTimeLong(TaskType taskType) {
        return xgc2long(taskType.getNextRetryTimestamp());
    }

    public Long getNextRunStartTimeLong(TaskType taskType) {
        return xgc2long(taskType.getNextRunStartTimestamp());
    }

    public TaskDtoExecutionStatus getExecution(TaskType taskType) {
        return TaskDtoExecutionStatus.fromTaskExecutionStatus(taskType.getExecutionStatus(), taskType.getNodeAsObserved() != null);
    }

    //TODO why?
    public TaskExecutionStatus getRawExecutionStatus(TaskType taskType) {
        return TaskExecutionStatus.fromTaskType(taskType.getExecutionStatus());
    }

    private Long xgc2long(XMLGregorianCalendar gc) {
        return gc != null ? XmlTypeConverter.toMillis(gc) : null;
    }

}
