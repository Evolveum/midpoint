/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshDto;
import com.evolveum.midpoint.web.component.refresh.Refreshable;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author mederly
 *
 */
public class TaskSummaryPanel extends ObjectSummaryPanel<TaskType> {
    private static final long serialVersionUID = -5077637168906420769L;

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskSummaryPanel.class);

    private static final String ID_TAG_REFRESH = "refreshTag";

    private Refreshable refreshable;

    public TaskSummaryPanel(String id, IModel<TaskType> model, Refreshable refreshable, final PageBase parentPage) {
        super(id, TaskType.class, model, parentPage);
        this.refreshable = refreshable;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();


//        AutoRefreshPanel refreshTag = new AutoRefreshPanel(ID_TAG_REFRESH, createRefreshModel()) {
//
//            @Override
//            protected void refreshPerformed(AjaxRequestTarget target) {
//                refreshable.refresh(target);
//            }
//
//        };
//        AbstractAjaxTimerBehavior refreshingBehavior = new AbstractAjaxTimerBehavior(Duration.milliseconds(refreshable.getRefreshInterval())) {
//            @Override
//            protected void onTimer(AjaxRequestTarget ajaxRequestTarget) {
//                LOGGER.trace("onTimer called for {}; enabled = {}", this, refreshTag.getModelObject().isEnabled());
//                refreshable.refresh(ajaxRequestTarget);
//
//            }
//
//            @Override
//            protected boolean shouldTrigger() {
//                return refreshTag.getModelObject().isEnabled();
//            }
//        };
//
//        refreshTag.add(new AttributeModifier("class", "summary-tag"));
//
//        refreshTag.add(refreshingBehavior);
//        refreshTag.setOutputMarkupId(true);
//
//        getSummaryBoxPanel().add(refreshTag);
    }

    private IModel<AutoRefreshDto> createRefreshModel() {
        AutoRefreshDto dto = new AutoRefreshDto();
        dto.setEnabled(true);
        dto.setInterval(refreshable.getRefreshInterval());
        return Model.of(dto);
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
                return getTaskExecutionIcon(getModelObject());
            }

            @Override
            public String getLabel() {
                return getTaskExecutionLabel(getModelObject());
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
                return getTaskResultIcon(getModelObject());
            }

            @Override
            public String getLabel() {
                return getTaskResultLabel(getModelObject());
            }
        };
        summaryTagList.add(tagResult);
        return summaryTagList;
    }

    private String getIconForExecutionStatus(TaskDtoExecutionStatus status) {
        if (status == null) {
            return "fa fa-fw fa-question-circle text-warning";
        }
        switch (status) {
            //TODO move to the GUI style constants?
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

    @Override
    protected IModel<String> getDisplayNameModel() {
        //TODO temporary
        return new PropertyModel<>(getModel(), "name.orig");
    }

    @Override
    protected IModel<String> getTitleModel() {
        return new IModel<String>() {
            @Override
            public String getObject() {
//                TaskDto taskDto = getModelObject();
                //TODO what to do with WF?
//                if (taskDto.isWorkflow()) {
//                    return getString("TaskSummaryPanel.requestedBy", taskDto.getRequestedBy());
//                } else {
                    TaskType taskType = getModelObject();

                    String rv;
                    if (taskType.getExpectedTotal() != null) {
                        rv = createStringResource("TaskSummaryPanel.progressWithTotalKnown", taskType.getProgress(), taskType.getExpectedTotal())
                                .getString();
                    } else {
                        rv = createStringResource("TaskSummaryPanel.progressWithTotalUnknown", taskType.getProgress()).getString();
                    }
                    if (taskType.getExecutionStatus() != null) {
                        switch (taskType.getExecutionStatus()) {
                            case SUSPENDED:
                                rv += " " + getString("TaskSummaryPanel.progressIfSuspended");
                                break;
                            case CLOSED:
                                rv += " " + getString("TaskSummaryPanel.progressIfClosed");
                                break;
                            case WAITING:
                                rv += " " + getString("TaskSummaryPanel.progressIfWaiting");
                                break;
                        }
                    }
                    Long stalledSince = WebComponentUtil.xgc2long(taskType.getStalledSince());
                    if (stalledSince != null) {
                        rv += " " + getString("TaskSummaryPanel.progressIfStalled", WebComponentUtil.formatDate(new Date(stalledSince)));
                    }
                    return rv;
//                }
            }
        };
    }

    @Override
    protected IModel<String> getTitle2Model() {
        return new IModel<String>() {
            @Override
            public String getObject() {
                //TODO what to do with WF?
//                if (parentPage.getTaskDto().isWorkflow()) {
//                    return getString("TaskSummaryPanel.requestedOn", getRequestedOn());
//                } else {
                    TaskType taskType = getModelObject();
                    if (taskType.getOperationStats() != null && taskType.getOperationStats().getIterativeTaskInformation() != null &&
                            taskType.getOperationStats().getIterativeTaskInformation().getLastSuccessObjectName() != null) {
                        return createStringResource("TaskSummaryPanel.lastProcessed",
                                taskType.getOperationStats().getIterativeTaskInformation().getLastSuccessObjectName()).getString();
                    } else {
                        return "";
                    }
//                }
            }
        };
    }

    @Override
    protected IModel<String> getTitle3Model() {
        return new IModel<String>() {
            @Override
            public String getObject() {
                //TODO what to do with WF?
//                if (parentPage.getTaskDto().isWorkflow()) {
//                    String stageInfo = getStageInfo();
//                    if (stageInfo != null) {
//                        return getString("TaskSummaryPanel.stage", stageInfo);
//                    } else {
//                        return null;
//                    }
//                }

                TaskType taskType = getModelObject();
                if (taskType == null) {
                    return null;
                }
                long started = XmlTypeConverter.toMillis(taskType.getLastRunStartTimestamp());
                long finished = XmlTypeConverter.toMillis(taskType.getLastRunFinishTimestamp());
                if (started == 0) {
                    return null;
                }
                TaskDtoExecutionStatus status = TaskDtoExecutionStatus.fromTaskExecutionStatus(
                        taskType.getExecutionStatus(), taskType.getNodeAsObserved() != null);
                if (status.equals(TaskDtoExecutionStatus.RUNNING)
                        || finished == 0 || finished < started) {

                    return getString("TaskStatePanel.message.executionTime.notFinished",
                            WebComponentUtil.getShortDateTimeFormattedValue(new Date(started), getPageBase()),
                            DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - started));
                } else {
                    return getString("TaskStatePanel.message.executionTime.finished",
                            WebComponentUtil.getShortDateTimeFormattedValue(new Date(started), getPageBase()),
                            WebComponentUtil.getShortDateTimeFormattedValue(new Date(finished), getPageBase()),
                            DurationFormatUtils.formatDurationHMS(finished - started));
                }
            }
        };
    }

    private String getTaskExecutionLabel(TaskType task) {
        TaskDtoExecutionStatus status = TaskDtoExecutionStatus.fromTaskExecutionStatus(task.getExecutionStatus(), task.getNodeAsObserved() != null);
        if (status != null){
            return PageBase.createStringResourceStatic(TaskSummaryPanel.this, status).getString();
        }
        return "";
    }

    private String getTaskExecutionIcon(TaskType task) {
        TaskDtoExecutionStatus status = TaskDtoExecutionStatus.fromTaskExecutionStatus(task.getExecutionStatus(), task.getNodeAsObserved() != null);
        return getIconForExecutionStatus(status);
    }

    private String getTaskResultLabel(TaskType task) {
        OperationResultStatusType resultStatus = task.getResultStatus();
        if (resultStatus != null){
            return PageBase.createStringResourceStatic(TaskSummaryPanel.this, resultStatus).getString();
        }
        return "";
    }

    private String getTaskResultIcon(TaskType task) {
        OperationResultStatusType resultStatus = task.getResultStatus();
        return OperationResultStatusPresentationProperties.parseOperationalResultStatus(resultStatus).getIcon();
    }
}
