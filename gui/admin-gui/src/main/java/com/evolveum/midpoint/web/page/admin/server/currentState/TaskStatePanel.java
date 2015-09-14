/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.server.currentState;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskManagerException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.progress.StatisticsPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wf.WfDeltasPanel;
import com.evolveum.midpoint.web.component.wf.WfHistoryEventDto;
import com.evolveum.midpoint.web.component.wf.WfHistoryPanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.web.page.admin.workflow.PageProcessInstance;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author mederly
 */
public class TaskStatePanel extends SimplePanel<TaskDto> {

    private static final String ID_EXECUTION_STATUS = "executionStatus";
    private static final String ID_EXECUTION_NODE = "executionNode";
    private static final String ID_EXECUTION_TIME = "executionTime";

    private static final String ID_PROGRESS = "progress";

    private static final String ID_LAST_OBJECT_PROCESSED_OK = "lastObjectProcessedOk";
    private static final String ID_LAST_OBJECT_PROCESSED_NOT_OK = "lastObjectProcessedNotOk";
    private static final String ID_LAST_ERROR = "lastError";
    private static final String ID_CURRENT_OBJECT_PROCESSED = "currentObjectProcessed";

    private static final String ID_STATISTICS_PANEL = "statisticsPanel";

    private static final String ID_OPERATION_RESULT = "operationResult";

    IModel<Task> taskModel;

    public TaskStatePanel(String id, IModel<TaskDto> model, final PageBase pageBase) {
        super(id, model);
        taskModel = new AbstractReadOnlyModel<Task>() {
            @Override
            public Task getObject() {
                TaskDto taskDto = getModel().getObject();
                if (taskDto == null || taskDto.getIdentifier() == null) {
                    return null;
                }
                String id = taskDto.getIdentifier();
                TaskManager taskManager = pageBase.getTaskManager();
                Task task = taskManager.getLocallyRunningTaskByIdentifier(id);
                return task;
            }
        };
        realInitLayout();
    }

    @Override
    protected void initLayout() {
        // taskModel is not ready here...
    }

    private void realInitLayout() {
        Label executionStatus = new Label(ID_EXECUTION_STATUS, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskDtoExecutionStatus executionStatus = getModel().getObject().getExecution();
                return getString(TaskDtoExecutionStatus.class.getSimpleName() + "." + executionStatus.name());
            }
        });
        add(executionStatus);

        Label executionNode = new Label(ID_EXECUTION_NODE, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskDto dto = getModel().getObject();
                if (!TaskDtoExecutionStatus.RUNNING.equals(dto.getExecution())) {
                    return null;
                }
                return getString("TaskStatePanel.message.node", dto.getExecutingAt());
            }
        });
        add(executionNode);

        Label executionTime = new Label(ID_EXECUTION_TIME, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskDto dto = getModel().getObject();
                Date started = new Date(dto.getLastRunFinishTimestampLong());
                if (TaskDtoExecutionStatus.RUNNING.equals(dto.getExecution())) {
                    return getString("TaskStatePanel.message.executionTime.notFinished", WebMiscUtil.formatDate(started));
                } else {
                    Date finished = new Date(dto.getLastRunFinishTimestampLong());
                    return getString("TaskStatePanel.message.executionTime.finished",
                            WebMiscUtil.formatDate(started), WebMiscUtil.formatDate(finished));
                }
            }
        });
        add(executionTime);

        Label progress = new Label(ID_PROGRESS, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskDto dto = getModel().getObject();
                return dto.getProgressDescription();        // TODO implement using live task + implement stalled since
            }
        });
        add(progress);

        Label lastProcessedOk = new Label(ID_LAST_OBJECT_PROCESSED_OK, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return "N/A";
            }
        });
        add(lastProcessedOk);

        Label lastProcessedNotOk = new Label(ID_LAST_OBJECT_PROCESSED_NOT_OK, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return "N/A";
            }
        });
        add(lastProcessedNotOk);

        Label lastError = new Label(ID_LAST_ERROR, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return "N/A";
            }
        });
        add(lastError);

        Label currentObjectProcessed = new Label(ID_CURRENT_OBJECT_PROCESSED, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return "N/A";
            }
        });
        add(currentObjectProcessed);

        StatisticsPanel statisticsPanel = new StatisticsPanel(ID_STATISTICS_PANEL, taskModel.getObject());
        add(statisticsPanel);

        SortableDataProvider<OperationResult, String> provider = new ListDataProvider<>(this,
                new PropertyModel<List<OperationResult>>(getModel(), "opResult"));
        TablePanel result = new TablePanel<>(ID_OPERATION_RESULT, provider, initResultColumns());
        result.setStyle("padding-top: 0px;");
        result.setShowPaging(false);
        result.setOutputMarkupId(true);
        add(result);
    }

    private List<IColumn<OperationResult, String>> initResultColumns() {
        List<IColumn<OperationResult, String>> columns = new ArrayList<IColumn<OperationResult, String>>();

        columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.token"), "token"));
        columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.operation"), "operation"));
        columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.status"), "status"));
        columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.message"), "message"));
        return columns;
    }


}
