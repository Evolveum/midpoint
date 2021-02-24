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
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.refresh.Refreshable;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.wicket.model.IModel;
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

    private static final Trace LOGGER = TraceManager.getTrace(TaskSummaryPanel.class);

    private static final String ID_TAG_REFRESH = "refreshTag";

    private Refreshable refreshable;

    public TaskSummaryPanel(String id, IModel<TaskType> model, Refreshable refreshable, final PageBase parentPage) {
        super(id, TaskType.class, model, parentPage);
        this.refreshable = refreshable;
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

        SummaryTag<TaskType> tagLiveSyncToken = new SummaryTag<TaskType>(ID_SUMMARY_TAG, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void initialize(TaskType taskType) {
                setIconCssClass(getLiveSyncTokenIcon());
                setLabel(getLiveSyncToken(taskType));
                // TODO setColor
            }

        };
        tagLiveSyncToken.add(new VisibleBehaviour(() -> {
            TaskType task = getModelObject();
            return task != null && ObjectTypeUtil.hasArchetype(task, SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value());
        }));
        summaryTagList.add(tagLiveSyncToken);
        return summaryTagList;
    }

    private String getIconForExecutionStatus(TaskDtoExecutionState status) {
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
    protected String getDefaultIconCssClass() {
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
            }
        };
    }

    @Override
    protected IModel<String> getTitle2Model() {
        return new IModel<String>() {
            @Override
            public String getObject() {
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

                TaskType taskType = getModelObject();
                if (taskType == null) {
                    return null;
                }
                long started = XmlTypeConverter.toMillis(taskType.getLastRunStartTimestamp());
                long finished = XmlTypeConverter.toMillis(taskType.getLastRunFinishTimestamp());
                if (started == 0) {
                    return null;
                }
                TaskDtoExecutionState status = TaskDtoExecutionState.fromTaskExecutionStatus(
                        taskType.getExecutionStatus(), taskType.getNodeAsObserved() != null);
                if (status.equals(TaskDtoExecutionState.RUNNING)
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
        TaskDtoExecutionState status = TaskDtoExecutionState.fromTaskExecutionStatus(task.getExecutionStatus(), task.getNodeAsObserved() != null);
        if (status != null){
            return PageBase.createStringResourceStatic(TaskSummaryPanel.this, status).getString();
        }
        return "";
    }

    private String getTaskExecutionIcon(TaskType task) {
        TaskDtoExecutionState status = TaskDtoExecutionState.fromTaskExecutionStatus(task.getExecutionStatus(), task.getNodeAsObserved() != null);
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

    private String getLiveSyncTokenIcon() {
        return "fa fa-hand-o-right";
    }

    private <T> String getLiveSyncToken(TaskType taskType) {
        if (!ObjectTypeUtil.hasArchetype(taskType, SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value())) {
            return null;
        }
        PrismProperty<T> tokenProperty = taskType.asPrismObject().findProperty(LivesyncTokenEditorPanel.PATH_TOKEN);
        if (tokenProperty == null) {
            return null;
        }
        T realValue = tokenProperty.getRealValue();
        if (realValue == null) {
            return null;
        }

        return realValue.toString();
    }
}
