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

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.progress.StatisticsDtoModel;
import com.evolveum.midpoint.web.component.progress.StatisticsPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.web.page.PageTemplate.createStringResourceStatic;

/**
 * @author mederly
 */
public class TaskStatePanel extends SimplePanel<TaskCurrentStateDto> {

    private static final Trace LOGGER = TraceManager.getTrace(TaskStatePanel.class);

    private static final String ID_UPDATED = "updated";
    private static final String ID_COUNTERS_SOURCE = "countersSource";

    private static final String ID_EXECUTION_STATUS = "executionStatus";
    private static final String ID_EXECUTION_NODE = "executionNode";
    private static final String ID_EXECUTION_TIME = "executionTime";

    private static final String ID_PROGRESS = "progress";

    private static final String ID_OBJECTS_PROCESSED_SUCCESS = "objectsProcessedSuccess";
    private static final String ID_OBJECTS_PROCESSED_SUCCESS_TIME = "objectsProcessedSuccessTime";
    private static final String ID_LAST_OBJECT_PROCESSED_SUCCESS = "lastObjectProcessedSuccess";
    private static final String ID_LAST_OBJECT_PROCESSED_SUCCESS_TIME = "lastObjectProcessedSuccessTime";
    private static final String ID_OBJECTS_PROCESSED_FAILURE = "objectsProcessedFailure";
    private static final String ID_OBJECTS_PROCESSED_FAILURE_TIME = "objectsProcessedFailureTime";
    private static final String ID_LAST_OBJECT_PROCESSED_FAILURE = "lastObjectProcessedFailure";
    private static final String ID_LAST_OBJECT_PROCESSED_FAILURE_TIME = "lastObjectProcessedFailureTime";
    private static final String ID_LAST_ERROR = "lastError";
    private static final String ID_CURRENT_OBJECT_PROCESSED = "currentObjectProcessed";
    private static final String ID_CURRENT_OBJECT_PROCESSED_TIME = "currentObjectProcessedTime";
    private static final String ID_OBJECTS_TOTAL = "objectsTotal";

    private static final String ID_SYNCHRONIZATION_INFORMATION_PANEL = "synchronizationInformationPanel";
    private static final String ID_STATISTICS_PANEL = "statisticsPanel";

    private static final String ID_WORKER_THREADS_TABLE = "workerThreadsTable";
    private static final String ID_WORKER_THREADS_TABLE_LABEL = "workerThreadsTableLabel";

    private static final String ID_OPERATION_RESULT = "operationResult";

    // ugly hack - TODO replace with something serious
    private static final Collection<String> WALL_CLOCK_AVG_CATEGORIES = Arrays.asList(
            TaskCategory.BULK_ACTIONS, TaskCategory.IMPORTING_ACCOUNTS, TaskCategory.RECOMPUTATION, TaskCategory.RECONCILIATION
    );

    private StatisticsDtoModel statisticsDtoModel;

    public TaskStatePanel(String id, IModel<TaskCurrentStateDto> model, final PageBase pageBase) {
        super(id, model);
    }

    @Override
    protected void initLayout() {

        Label updated = new Label(ID_UPDATED, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return formatDate(new Date());
            }
        });
        add(updated);

        Label executionStatus = new Label(ID_EXECUTION_STATUS, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskDtoExecutionStatus executionStatus = getModel().getObject().getTaskDto().getExecution();
                return getString(TaskDtoExecutionStatus.class.getSimpleName() + "." + executionStatus.name());
            }
        });
        add(executionStatus);

        Label executionNode = new Label(ID_EXECUTION_NODE, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskDto dto = getModel().getObject().getTaskDto();
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
                TaskDto dto = getModel().getObject().getTaskDto();
                if (dto == null) {
                    return null;
                }
                Long started = dto.getLastRunStartTimestampLong();
                Long finished = dto.getLastRunFinishTimestampLong();
                if (started == null) {
                    return null;
                }
                if (TaskDtoExecutionStatus.RUNNING.equals(dto.getExecution()) || finished == null || finished < started) {
                    return getString("TaskStatePanel.message.executionTime.notFinished", formatDate(new Date(started)),
                            DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - started));
                } else {
                    return getString("TaskStatePanel.message.executionTime.finished",
                            formatDate(new Date(started)), formatDate(new Date(finished)),
                            DurationFormatUtils.formatDurationHMS(finished - started));
                }
            }
        });
        add(executionTime);

        Label progress = new Label(ID_PROGRESS, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                TaskDto taskDto = dto.getTaskDto();
                if (taskDto == null) {
                    return null;
                } else {
                    return taskDto.getProgressDescription(dto.getCurrentProgress());        // TODO implement stalled since
                }
            }
        });
        add(progress);

        Label processedSuccess = new Label(ID_OBJECTS_PROCESSED_SUCCESS, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getTotalSuccessCount() == 0) {
                    return "0";
                } else {
                    return getString("TaskStatePanel.message.objectsProcessed", info.getTotalSuccessCount());
                }
            }
        });
        add(processedSuccess);

        Label processedSuccessTime = new Label(ID_OBJECTS_PROCESSED_SUCCESS_TIME, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getTotalSuccessCount() == 0) {
                    return null;
                } else {
                    return getString("TaskStatePanel.message.objectsProcessedTime",
                            info.getTotalSuccessDuration()/1000,
                            info.getTotalSuccessDuration()/info.getTotalSuccessCount());
                }
            }
        });
        add(processedSuccessTime);

        Label lastProcessedSuccess = new Label(ID_LAST_OBJECT_PROCESSED_SUCCESS, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getLastSuccessObjectDisplayName() == null) {
                    return null;
                } else {
                    return getString("TaskStatePanel.message.lastObjectProcessed",
                            info.getLastSuccessObjectDisplayName());
                }
            }
        });
        add(lastProcessedSuccess);

        Label lastProcessedSuccessTime = new Label(ID_LAST_OBJECT_PROCESSED_SUCCESS_TIME, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getLastSuccessEndTimestamp() == null) {
                    return null;
                } else {
                    if (showAgo(dto)) {
                        return getString("TaskStatePanel.message.timeInfoWithDurationAndAgo",
                                formatDate(info.getLastSuccessEndTimestamp()),
                                DurationFormatUtils.formatDurationWords(System.currentTimeMillis() -
                                        XmlTypeConverter.toMillis(info.getLastSuccessEndTimestamp()), true, true),
                                info.getLastSuccessDuration());
                    } else {
                        return getString("TaskStatePanel.message.timeInfoWithDuration",
                                formatDate(info.getLastSuccessEndTimestamp()),
                                info.getLastSuccessDuration());
                    }
                }
            }
        });
        add(lastProcessedSuccessTime);

        Label processedFailure = new Label(ID_OBJECTS_PROCESSED_FAILURE, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getTotalFailureCount() == 0) {
                    return "0";
                } else {
                    return getString("TaskStatePanel.message.objectsProcessed",
                            info.getTotalFailureCount());
                }
            }
        });
        add(processedFailure);

        Label processedFailureTime = new Label(ID_OBJECTS_PROCESSED_FAILURE_TIME, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getTotalFailureCount() == 0) {
                    return null;
                } else {
                    return getString("TaskStatePanel.message.objectsProcessedTime",
                            info.getTotalFailureDuration()/1000,
                            info.getTotalFailureDuration()/info.getTotalFailureCount());
                }
            }
        });
        add(processedFailureTime);

        Label lastProcessedFailure = new Label(ID_LAST_OBJECT_PROCESSED_FAILURE, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getLastFailureObjectDisplayName() == null) {
                    return null;
                } else {
                    return getString("TaskStatePanel.message.lastObjectProcessed",
                            info.getLastFailureObjectDisplayName());
                }
            }
        });
        add(lastProcessedFailure);

        Label lastProcessedFailureTime = new Label(ID_LAST_OBJECT_PROCESSED_FAILURE_TIME, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getLastFailureEndTimestamp() == null) {
                    return null;
                } else {
                    if (showAgo(dto)) {
                        return getString("TaskStatePanel.message.timeInfoWithDurationAndAgo",
                                formatDate(info.getLastFailureEndTimestamp()),
                                DurationFormatUtils.formatDurationWords(System.currentTimeMillis() -
                                        XmlTypeConverter.toMillis(info.getLastFailureEndTimestamp()), true, true),
                                info.getLastFailureDuration());
                    } else {
                        return getString("TaskStatePanel.message.timeInfoWithDuration",
                                formatDate(info.getLastFailureEndTimestamp()),
                                info.getLastFailureDuration());
                    }
                }
            }
        });
        add(lastProcessedFailureTime);

        Label lastError = new Label(ID_LAST_ERROR, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                return info.getLastFailureExceptionMessage();
            }
        });
        add(lastError);

        Label currentObjectProcessed = new Label(ID_CURRENT_OBJECT_PROCESSED, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                return info.getCurrentObjectDisplayName();
            }
        });
        add(currentObjectProcessed);

        Label currentObjectProcessedTime = new Label(ID_CURRENT_OBJECT_PROCESSED_TIME, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (info.getCurrentObjectStartTimestamp() == null) {
                    return null;
                } else {
                    return getString("TaskStatePanel.message.timeInfoWithAgo",
                            formatDate(info.getCurrentObjectStartTimestamp()),
                            DurationFormatUtils.formatDurationWords(System.currentTimeMillis() -
                                    XmlTypeConverter.toMillis(info.getCurrentObjectStartTimestamp()), true, true));
                }
            }
        });
        add(currentObjectProcessedTime);

        Label objectsTotal = new Label(ID_OBJECTS_TOTAL, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                int objectsTotal = info.getTotalSuccessCount() + info.getTotalFailureCount();
                if (WALL_CLOCK_AVG_CATEGORIES.contains(dto.getTaskDto().getCategory())) {
                    Long avg = getWallClockAverage(dto, objectsTotal);
                    if (avg != null) {
                        return getString("TaskStatePanel.message.objectsTotal",
                                objectsTotal, avg);
                    }
                }
                return String.valueOf(objectsTotal);
            }
        });
        add(objectsTotal);

        SynchronizationInformationPanel synchronizationInformationPanel = new SynchronizationInformationPanel(
                ID_SYNCHRONIZATION_INFORMATION_PANEL,
                new AbstractReadOnlyModel<SynchronizationInformationDto>() {
                    @Override
                    public SynchronizationInformationDto getObject() {
                        TaskCurrentStateDto dto = getModelObject();
                        if (dto == null) {
                            return null;
                        }
                        if (dto.getSynchronizationInformationType() == null) {
                            return null;
                        }
                        return new SynchronizationInformationDto(dto.getSynchronizationInformationType());
                    }
                });
        synchronizationInformationPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                TaskCurrentStateDto dto = getModelObject();
                return dto != null && dto.getSynchronizationInformationType() != null;
            }
        });
        add(synchronizationInformationPanel);

        Label countersSource = new Label(ID_COUNTERS_SOURCE, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                TaskCurrentStateDto dto = getModelObject();
                if (dto == null) {
                    return null;
                }
                IterativeTaskInformationType info = dto.getIterativeTaskInformationType();
                if (info == null) {
                    return null;
                }
                if (Boolean.TRUE.equals(info.isFromMemory())) {
                    return getString("TaskStatePanel.message.countersSourceMemory",
                            formatDate(info.getTimestamp()));
                } else {
                    return getString("TaskStatePanel.message.countersSourceRepository",
                            formatDate(info.getTimestamp()));
                }
            }
        });
        add(countersSource);

        statisticsDtoModel = new StatisticsDtoModel(new AbstractReadOnlyModel<TaskDto>() {
            @Override
            public TaskDto getObject() {
                return getModelObject().getTaskDto();
            }
        });
        StatisticsPanel statisticsPanel = new StatisticsPanel(ID_STATISTICS_PANEL, statisticsDtoModel);
        add(statisticsPanel);

        VisibleEnableBehaviour hiddenWhenNoSubtasks = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                TaskDto taskDto = getModelObject().getTaskDto();
                return taskDto != null && !taskDto.getTransientSubtasks().isEmpty();
            }
        };

        Label workerThreadsTableLabel = new Label(ID_WORKER_THREADS_TABLE_LABEL, new ResourceModel("TaskStatePanel.workerThreads"));
        workerThreadsTableLabel.add(hiddenWhenNoSubtasks);
        add(workerThreadsTableLabel);
        List<IColumn<WorkerThreadDto, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn(createStringResourceStatic(this, "TaskStatePanel.subtaskName"), WorkerThreadDto.F_NAME));
        columns.add(new EnumPropertyColumn<WorkerThreadDto>(createStringResourceStatic(this, "TaskStatePanel.subtaskState"), WorkerThreadDto.F_EXECUTION_STATUS));
        columns.add(new PropertyColumn(createStringResourceStatic(this, "TaskStatePanel.subtaskObjectsProcessed"), WorkerThreadDto.F_PROGRESS));
        ISortableDataProvider<WorkerThreadDto, String> threadsProvider = new ListDataProvider<>(this,
                new AbstractReadOnlyModel<List<WorkerThreadDto>>() {
                    @Override
                    public List<WorkerThreadDto> getObject() {
                        List<WorkerThreadDto> rv = new ArrayList<>();
                        TaskDto taskDto = getModelObject().getTaskDto();
                        if (taskDto != null) {
                            for (TaskDto subtaskDto : taskDto.getTransientSubtasks()) {
                                rv.add(new WorkerThreadDto(subtaskDto));
                            }
                        }
                        return rv;
                    }
                });
        TablePanel<WorkerThreadDto> workerThreadsTablePanel = new TablePanel<>(ID_WORKER_THREADS_TABLE, threadsProvider , columns);
        workerThreadsTablePanel.add(hiddenWhenNoSubtasks);
        add(workerThreadsTablePanel);

        SortableDataProvider<OperationResult, String> provider = new ListDataProvider<>(this,
                new PropertyModel<List<OperationResult>>(getModel(), "taskDto.opResult"));
        TablePanel result = new TablePanel<>(ID_OPERATION_RESULT, provider, initResultColumns());
        result.setStyle("padding-top: 0px;");
        result.setShowPaging(false);
        result.setOutputMarkupId(true);
        add(result);
    }

    private String formatDate(XMLGregorianCalendar date) {
        return formatDate(XmlTypeConverter.toDate(date));
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toLocaleString();
    }

    protected boolean showAgo(TaskCurrentStateDto dto) {
        boolean showAgo = false;
        TaskDto taskDto = dto.getTaskDto();
        if (taskDto != null) {
            Long started = taskDto.getLastRunStartTimestampLong();
            Long finished = taskDto.getLastRunFinishTimestampLong();
            if (started != null && (finished == null || finished < started)) {
                showAgo = true;     // for all running tasks
            } else if (finished != null && (System.currentTimeMillis()-finished < 60000L)) {
                showAgo = true;     // for tasks finished just a while ago
            }
        }
        return showAgo;
    }

    private Long getWallClockAverage(TaskCurrentStateDto dto, int objectsTotal) {
        if (objectsTotal == 0) {
            return null;
        }
        if (dto == null || dto.getTaskDto() == null) {
            return null;
        }
        Long started = dto.getTaskDto().getLastRunStartTimestampLong();
        if (started == null) {
            return null;
        }
        Long finished = dto.getTaskDto().getLastRunFinishTimestampLong();
        if (finished == null || finished < started) {
            finished = System.currentTimeMillis();
        }
        return (finished - started) / objectsTotal;
    }

    private List<IColumn<OperationResult, String>> initResultColumns() {
        List<IColumn<OperationResult, String>> columns = new ArrayList<IColumn<OperationResult, String>>();

        columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.token"), "token"));
        columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.operation"), "operation"));
        columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.status"), "status"));
        columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.message"), "message"));
        return columns;
    }


    public void refreshModel(PageTaskEdit page) {
        IModel<TaskCurrentStateDto> model = getModel();
        if (!(model instanceof TaskCurrentStateDtoModel)) {
            LOGGER.warn("Unexpected or null model for TaskCurrentStateDto object: {}", model);
            return;
        }
        ((TaskCurrentStateDtoModel) model).refresh(page);
        if (statisticsDtoModel != null) {
            statisticsDtoModel.invalidateCache();
        }
    }
}
