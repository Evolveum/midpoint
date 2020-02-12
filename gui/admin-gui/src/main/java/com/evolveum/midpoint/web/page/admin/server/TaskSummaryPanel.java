/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshDto;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshPanel;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wf.WfGuiUtil;
import com.evolveum.midpoint.web.page.admin.server.dto.ApprovalOutcomeIcon;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author mederly
 *
 */
public class TaskSummaryPanel extends ObjectSummaryPanel<TaskType> {
    private static final long serialVersionUID = -5077637168906420769L;

    private static final String ID_TAG_REFRESH = "refreshTag";

    private PageTaskEdit parentPage;
    private IModel<AutoRefreshDto> refreshModel;

    public TaskSummaryPanel(String id, IModel<TaskType> model, IModel<AutoRefreshDto> refreshModel, final PageTaskEdit parentPage) {
        super(id, TaskType.class, model, parentPage);
        this.parentPage = parentPage;
        this.refreshModel = refreshModel;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();

        final AutoRefreshPanel refreshTag = new AutoRefreshPanel(ID_TAG_REFRESH, refreshModel);
        refreshTag.setOutputMarkupId(true);
        refreshTag.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return parentPage.getTaskDto().getWorkflowOutcome() == null;        // because otherwise there are too many tags to fit into window
            }
        } );
        getSummaryBoxPanel().add(refreshTag);
    }

    @Override
    protected List<SummaryTag<TaskType>> getSummaryTagComponentList(){
        List<SummaryTag<TaskType>> summaryTagList = new ArrayList<>();
        SummaryTag<TaskType> tagExecutionStatus = new SummaryTag<TaskType>(ID_SUMMARY_TAG, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void initialize(TaskType taskType) {
                setIconCssClass(getTaskExecutionIcon(taskType));
                setLabel(getTaskExecutionLabel(taskType));
                // TODO setColor
            }

            @Override
            public String getIconCssClass() {
                return getTaskExecutionIcon(parentPage.getTaskDto().getTaskType());
            }

            @Override
            public String getLabel() {
                return getTaskExecutionLabel(parentPage.getTaskDto().getTaskType());
            }
        };
        summaryTagList.add(tagExecutionStatus);

        SummaryTag<TaskType> tagResult = new SummaryTag<TaskType>(ID_SUMMARY_TAG, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void initialize(TaskType taskType) {
                setIconCssClass(getTaskResultIcon(taskType));
                setLabel(getTaskResultLabel(taskType));
                // TODO setColor
            }

            @Override
            public String getIconCssClass() {
                return getTaskResultIcon(parentPage.getTaskDto().getTaskType());
            }

            @Override
            public String getLabel() {
                return getTaskResultLabel(parentPage.getTaskDto().getTaskType());
            }
        };
        summaryTagList.add(tagResult);

        SummaryTag<TaskType> tagOutcome = new SummaryTag<TaskType>(ID_SUMMARY_TAG, getModel()) {
            @Override
            protected void initialize(TaskType taskType) {
                String icon, name;
                if (parentPage.getTaskDto().getWorkflowOutcome() == null) {
                    // shouldn't occur!
                    return;
                }

                if (parentPage.getTaskDto().getWorkflowOutcome()) {
                    icon = ApprovalOutcomeIcon.APPROVED.getIcon();
                    name = "approved";
                } else {
                    icon = ApprovalOutcomeIcon.REJECTED.getIcon();
                    name = "rejected";
                }
                setIconCssClass(icon);
                setLabel(PageBase.createStringResourceStatic(TaskSummaryPanel.this, "TaskSummaryPanel." + name).getString());
            }
        };
        tagOutcome.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return parentPage.getTaskDto().getWorkflowOutcome() != null;
            }
        });
        summaryTagList.add(tagOutcome);

        return summaryTagList;
    }

    private String getIconForExecutionStatus(TaskDtoExecutionStatus status) {
        if (status == null) {
            return "fa fa-fw fa-question-circle text-warning";
        }
        switch (status) {
            case RUNNING: return "fa fa-fw fa-spinner";
            case RUNNABLE: return "fa fa-fw fa-hand-o-up";
            case SUSPENDED: return "fa fa-fw fa-bed";
            case SUSPENDING: return "fa fa-fw fa-bed";
            case WAITING: return "fa fa-fw fa-clock-o";
            case CLOSED: return "fa fa-fw fa-power-off";
            default: return "";
        }
    }

    @Override
    protected String getIconCssClass() {
        return GuiStyleConstants.CLASS_OBJECT_TASK_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {        // TODO
        return "summary-panel-task";
    }

    @Override
    protected String getBoxAdditionalCssClass() {            // TODO
        return "summary-panel-task";
    }

    @Override
    protected boolean isIdentifierVisible() {
        return false;
    }

    @Override
    protected String getTagBoxCssClass() {
        return "summary-tag-box-wide";
    }

    private String getStageInfo() {
//        return WfContextUtil.getStageInfo(parentPage.getTaskDto().getApprovalContext());
        // TODO determine from Case
        return null;
    }

    public String getRequestedOn() {
        return WebComponentUtil.getLongDateTimeFormattedValue(parentPage.getTaskDto().getRequestedOn(), parentPage);
    }

    @Override
    protected IModel<String> getDisplayNameModel() {
        return new ReadOnlyModel<>(() -> {
            // temporary code
            TaskDto taskDto = parentPage.getTaskDto();
            String name = WfGuiUtil.getLocalizedProcessName(taskDto.getApprovalContext(), TaskSummaryPanel.this);
            if (name == null) {
                name = WfGuiUtil.getLocalizedTaskName(taskDto.getApprovalContext(), TaskSummaryPanel.this);
            }
            if (name == null) {
                name = taskDto.getName();
            }
            return name;
        });
    }

    @Override
    protected IModel<String> getTitleModel() {
        return new IModel<String>() {
            @Override
            public String getObject() {
                TaskDto taskDto = parentPage.getTaskDto();
                if (taskDto.isWorkflow()) {
                    return getString("TaskSummaryPanel.requestedBy", taskDto.getRequestedBy());
                } else {
                    TaskType taskType = parentPage.getTaskDto().getTaskType();

                    String rv;
                    if (taskType.getExpectedTotal() != null) {
                        rv = createStringResource("TaskSummaryPanel.progressWithTotalKnown", taskType.getProgress(), taskType.getExpectedTotal())
                                .getString();
                    } else {
                        rv = createStringResource("TaskSummaryPanel.progressWithTotalUnknown", taskType.getProgress()).getString();
                    }
                    if (taskDto.isSuspended()) {
                        rv += " " + getString("TaskSummaryPanel.progressIfSuspended");
                    } else if (taskDto.isClosed()) {
                        rv += " " + getString("TaskSummaryPanel.progressIfClosed");
                    } else if (taskDto.isWaiting()) {
                        rv += " " + getString("TaskSummaryPanel.progressIfWaiting");
                    } else if (taskDto.getStalledSince() != null) {
                        rv += " " + getString("TaskSummaryPanel.progressIfStalled", WebComponentUtil.formatDate(new Date(parentPage.getTaskDto().getStalledSince())));
                    }
                    return rv;
                }
            }
        };
    }

    @Override
    protected IModel<String> getTitle2Model() {
        return new IModel<String>() {
            @Override
            public String getObject() {
                if (parentPage.getTaskDto().isWorkflow()) {
                    return getString("TaskSummaryPanel.requestedOn", getRequestedOn());
                } else {
                    TaskType taskType = parentPage.getTaskDto().getTaskType();
                    if (taskType.getOperationStats() != null && taskType.getOperationStats().getIterativeTaskInformation() != null &&
                            taskType.getOperationStats().getIterativeTaskInformation().getLastSuccessObjectName() != null) {
                        return createStringResource("TaskSummaryPanel.lastProcessed",
                                taskType.getOperationStats().getIterativeTaskInformation().getLastSuccessObjectName()).getString();
                    } else {
                        return "";
                    }
                }
            }
        };
    }

    @Override
    protected IModel<String> getTitle3Model() {
        return new IModel<String>() {
            @Override
            public String getObject() {
                if (parentPage.getTaskDto().isWorkflow()) {
                    String stageInfo = getStageInfo();
                    if (stageInfo != null) {
                        return getString("TaskSummaryPanel.stage", stageInfo);
                    } else {
                        return null;
                    }
                }

                TaskType taskType = parentPage.getTaskDto().getTaskType();
                if (taskType == null) {
                    return null;
                }
                long started = XmlTypeConverter.toMillis(taskType.getLastRunStartTimestamp());
                long finished = XmlTypeConverter.toMillis(taskType.getLastRunFinishTimestamp());
                if (started == 0) {
                    return null;
                }
                if (taskType.getExecutionStatus() == TaskExecutionStatusType.RUNNABLE && taskType.getNodeAsObserved() != null
                        || finished == 0 || finished < started) {

                    return getString("TaskStatePanel.message.executionTime.notFinished",
                            WebComponentUtil.getShortDateTimeFormattedValue(new Date(started), parentPage),
                            DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - started));
                } else {
                    return getString("TaskStatePanel.message.executionTime.finished",
                            WebComponentUtil.getShortDateTimeFormattedValue(new Date(started), parentPage),
                            WebComponentUtil.getShortDateTimeFormattedValue(new Date(finished), parentPage),
                            DurationFormatUtils.formatDurationHMS(finished - started));
                }
            }
        };
    }

    public AutoRefreshPanel getRefreshPanel() {
        return (AutoRefreshPanel) getSummaryBoxPanel().get(ID_TAG_REFRESH);
    }

    private String getTaskExecutionLabel(TaskType task){
        TaskDtoExecutionStatus status = TaskDtoExecutionStatus.fromTaskExecutionStatus(task.getExecutionStatus(), task.getNodeAsObserved() != null);
        if (status != null){
            return PageBase.createStringResourceStatic(TaskSummaryPanel.this, status).getString();
        }
        return "";
    }

    private String getTaskExecutionIcon(TaskType task){
        TaskDtoExecutionStatus status = TaskDtoExecutionStatus.fromTaskExecutionStatus(task.getExecutionStatus(), task.getNodeAsObserved() != null);
        return getIconForExecutionStatus(status);
    }

    private String getTaskResultLabel(TaskType task){
        OperationResultStatusType resultStatus = task.getResultStatus();
        if (resultStatus != null){
            return PageBase.createStringResourceStatic(TaskSummaryPanel.this, resultStatus).getString();
        }
        return "";
    }

    private String getTaskResultIcon(TaskType task){
        OperationResultStatusType resultStatus = task.getResultStatus();
        return OperationResultStatusPresentationProperties.parseOperationalResultStatus(resultStatus).getIcon();
    }
}
