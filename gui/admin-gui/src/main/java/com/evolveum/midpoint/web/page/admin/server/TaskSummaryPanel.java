/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil;
import com.evolveum.midpoint.schema.util.task.LegacyTaskInformation;
import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class TaskSummaryPanel extends ObjectSummaryPanel<TaskType> {
    private static final long serialVersionUID = -5077637168906420769L;

    /** Keeps the pre-processed task information. */
    @NotNull private final NonEmptyLoadableModel<TaskInformation> taskInformationModel;

    public TaskSummaryPanel(String id, IModel<TaskType> model, @NotNull IModel<TaskType> rootTaskModel, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, model, summaryPanelSpecificationType);
        this.taskInformationModel = createTaskInformationModel(rootTaskModel, model);
    }

    TaskSummaryPanel(String id, IModel<TaskType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, model, summaryPanelSpecificationType);
        this.taskInformationModel = createFallbackTaskInformationModel(model);
    }

    private NonEmptyLoadableModel<TaskInformation> createTaskInformationModel(@NotNull IModel<TaskType> taskModel,
            @NotNull IModel<TaskType> rootTaskModel) {
        return NonEmptyLoadableModel.create(
                () -> {
                    TaskType task = taskModel.getObject().clone();
                    WebPrismUtil.cleanupEmptyContainers(task.asPrismContainer());
                    return TaskInformation.createForTask(task, rootTaskModel.getObject());
                    }, false);
    }

    private NonEmptyLoadableModel<TaskInformation> createFallbackTaskInformationModel(@NotNull IModel<TaskType> model) {
        return NonEmptyLoadableModel.create(
                () -> LegacyTaskInformation.fromLegacyTaskOrNoTask(model.getObject()), false);
    }

    @Override
    protected IModel<List<Badge>> createBadgesModel() {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<Badge> load() {
                TaskType task = getModelObject();
                ActivityDefinitionType def = task.getActivity();

                if (def == null || def.getExecution() == null) {
                    return Collections.emptyList();
                }

                ActivityExecutionModeDefinitionType executionDef = def.getExecution();
                ExecutionModeType mode = executionDef.getMode();
                if (mode == null) {
                    return Collections.emptyList();
                }

                return List.of(new Badge(Badge.State.INFO.getCss(), LocalizationUtil.translateEnum(mode)));
            }
        };
    }

    @Override
    protected List<SummaryTag<TaskType>> getSummaryTagComponentList() {
        List<SummaryTag<TaskType>> summaryTagList = new ArrayList<>();
        SummaryTag<TaskType> tagExecutionState = new SummaryTag<>(ID_SUMMARY_TAG, getModel()) {
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
        summaryTagList.add(tagExecutionState);

        SummaryTag<TaskType> tagResult = new SummaryTag<>(ID_SUMMARY_TAG, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void initialize(TaskType taskType) {
                setIconCssClass(getTaskResultIcon());
                setLabel(getTaskResultLabel());
                // TODO setColor
            }

            @Override
            public String getIconCssClass() {
                return getTaskResultIcon();
            }

            @Override
            public String getLabel() {
                return getTaskResultLabel();
            }
        };
        summaryTagList.add(tagResult);

        SummaryTag<TaskType> tagLiveSyncToken = new SummaryTag<>(ID_SUMMARY_TAG, getModel()) {
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
            return task != null && ObjectTypeUtil.hasArchetypeRef(task, SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value());
        }));
        summaryTagList.add(tagLiveSyncToken);
        return summaryTagList;
    }

    private String getIconForExecutionState(TaskDtoExecutionState status) {
        if (status == null) {
            return "fa fa-fw fa-question-circle text-warning";
        }

        String css;
        switch (status) {
            case RUNNING:
                css = GuiStyleConstants.ICON_FA_SPINNER;
                break;
            case RUNNABLE:
                css = GuiStyleConstants.CLASS_SELECTION_HAND + " fa-fw";
                break;
            case SUSPENDED:
            case SUSPENDING:
                css = GuiStyleConstants.ICON_FA_BED;
                break;
            case WAITING:
                css = GuiStyleConstants.ICON_FAR_CLOCK;
                break;
            case CLOSED:
                css = GuiStyleConstants.ICON_FA_POWER_OFF;
                break;
            default:
                css = "";
        }

        return StringUtils.isNotEmpty(css) ? css + " fa-fw" : "";
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
        return () -> {
            TaskType taskType = getModelObject();
            TaskInformation taskInformation = taskInformationModel.getObject();

            String rv = WebComponentUtil.getTaskProgressDescription(taskInformation, true, getPageBase());
            if (taskType.getExecutionState() != null) {
                switch (taskType.getExecutionState()) {
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
        };
    }

    @Override
    protected IModel<String> getTitle2Model() {
        return () -> {
            TaskType task = getModelObject();
            String lastSuccess = ActivityItemProcessingStatisticsUtil.getLastSuccessObjectName(task);
            if (lastSuccess != null) {
                return createStringResource("TaskSummaryPanel.lastProcessed", lastSuccess).getString();
            } else {
                return "";
            }
        };
    }

    @Override
    protected IModel<String> getTitle3Model() {
        return () -> {
            TaskInformation taskInformation = taskInformationModel.getObject();
            long started = XmlTypeConverter.toMillis(taskInformation.getStartTimestamp());
            long finished = XmlTypeConverter.toMillis(taskInformation.getEndTimestamp());
            if (started == 0) {
                return null;
            }
            if (finished == 0) {
                return getString("TaskStatePanel.message.executionTime.notFinished",
                        WebComponentUtil.getShortDateTimeFormattedValue(new Date(started), getPageBase()),
                        DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - started));
            } else {
                return getString("TaskStatePanel.message.executionTime.finished",
                        WebComponentUtil.getShortDateTimeFormattedValue(new Date(started), getPageBase()),
                        WebComponentUtil.getShortDateTimeFormattedValue(new Date(finished), getPageBase()),
                        DurationFormatUtils.formatDurationHMS(finished - started));
            }
        };
    }

    private String getTaskExecutionLabel(TaskType task) {
        TaskDtoExecutionState status = TaskDtoExecutionState.fromTaskExecutionState(task.getExecutionState(), task.getNodeAsObserved() != null);
        if (status != null) {
            return PageBase.createStringResourceStatic(TaskSummaryPanel.this, status).getString();
        }
        return "";
    }

    private String getTaskExecutionIcon(TaskType task) {
        TaskDtoExecutionState status = TaskDtoExecutionState.fromTaskExecutionState(task.getExecutionState(), task.getNodeAsObserved() != null);
        return getIconForExecutionState(status);
    }

    private String getTaskResultLabel() {
        TaskInformation info = taskInformationModel.getObject();
        GuiTaskResultStatus status = GuiTaskResultStatus.fromTaskResultStatus(info.getTaskUserFriendlyStatus());

        if (status == null) {
            return GuiTaskResultStatus.UNKNOWN.icon;
        }

        return LocalizationUtil.translateEnum(status);
    }

    private String getTaskResultIcon() {
        OperationResultStatusType resultStatus = taskInformationModel.getObject().getResultStatus();
        return OperationResultStatusPresentationProperties.parseOperationalResultStatus(resultStatus).getIcon();
    }

    private String getLiveSyncTokenIcon() {
        return "fa-regular fa-hand-point-right fa-fw";
    }

    private String getLiveSyncToken(TaskType taskType) {
        if (!ObjectTypeUtil.hasArchetypeRef(taskType, SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value())) {
            return null;
        }
        Object token = taskInformationModel.getObject().getLiveSyncToken();
        return token != null ? token.toString() : null;
    }

    public NonEmptyLoadableModel<TaskInformation> getTaskInfoModel() {
        return taskInformationModel;
    }
}
