/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshDto;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshPanel;
import com.evolveum.midpoint.web.component.util.SummaryTagSimple;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wf.WfGuiUtil;
import com.evolveum.midpoint.web.model.ContainerableFromPrismObjectModel;
import com.evolveum.midpoint.web.page.admin.server.dto.ApprovalOutcomeIcon;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.wicket.datetime.PatternDateConverter;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.Date;

/**
 * @author mederly
 *
 */
public class TaskSummaryPanel extends ObjectSummaryPanel<TaskType> {
	private static final long serialVersionUID = -5077637168906420769L;

	private static final String ID_TAG_EXECUTION_STATUS = "summaryTagExecutionStatus";
	private static final String ID_TAG_RESULT = "summaryTagResult";
	private static final String ID_TAG_WF_OUTCOME = "wfOutcomeTag";
	private static final String ID_TAG_EMPTY = "emptyTag";
	private static final String ID_TAG_REFRESH = "refreshTag";

	private PageTaskEdit parentPage;

	public TaskSummaryPanel(String id, IModel<PrismObject<TaskType>> model, IModel<AutoRefreshDto> refreshModel, final PageTaskEdit parentPage) {
		super(id, TaskType.class, model, parentPage);
		initLayoutCommon(parentPage);
		this.parentPage = parentPage;
		IModel<TaskType> containerModel = new ContainerableFromPrismObjectModel<>(model);

		SummaryTagSimple<TaskType> tagExecutionStatus = new SummaryTagSimple<TaskType>(ID_TAG_EXECUTION_STATUS, containerModel) {
			@Override
			protected void initialize(TaskType taskType) {
				TaskDtoExecutionStatus status = TaskDtoExecutionStatus.fromTaskExecutionStatus(taskType.getExecutionStatus(), taskType.getNodeAsObserved() != null);
				String icon = getIconForExecutionStatus(status);
				setIconCssClass(icon);
				if (status != null) {
					setLabel(PageBase.createStringResourceStatic(TaskSummaryPanel.this, status).getString());
				}
				// TODO setColor
			}
		};
		addTag(tagExecutionStatus);

		SummaryTagSimple<TaskType> tagResult = new SummaryTagSimple<TaskType>(ID_TAG_RESULT, containerModel) {
			@Override
			protected void initialize(TaskType taskType) {
				OperationResultStatusType resultStatus = taskType.getResultStatus();
				String icon = OperationResultStatusPresentationProperties.parseOperationalResultStatus(resultStatus).getIcon();
				setIconCssClass(icon);
				if (resultStatus != null) {
					setLabel(PageBase.createStringResourceStatic(TaskSummaryPanel.this, resultStatus).getString());
				}
				// TODO setColor
			}
		};
		addTag(tagResult);

		SummaryTagSimple<TaskType> tagOutcome = new SummaryTagSimple<TaskType>(ID_TAG_WF_OUTCOME, containerModel) {
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
		addTag(tagOutcome);

		final AutoRefreshPanel refreshTag = new AutoRefreshPanel(ID_TAG_REFRESH, refreshModel, parentPage, true);
		refreshTag.setOutputMarkupId(true);
		refreshTag.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return parentPage.getTaskDto().getWorkflowOutcome() == null;		// because otherwise there are too many tags to fit into window
			}
		} );
		addTag(refreshTag);
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
	protected String getIconBoxAdditionalCssClass() {		// TODO
		return "summary-panel-task";
	}

	@Override
	protected String getBoxAdditionalCssClass() {			// TODO
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
		return WfContextUtil.getStageInfo(parentPage.getTaskDto().getWorkflowContext());
	}

	public String getRequestedOn() {
		return WebComponentUtil.getLocalizedDate(parentPage.getTaskDto().getRequestedOn(), DateLabelComponent.MEDIUM_MEDIUM_STYLE);
	}

	@Override
	protected IModel<String> getDisplayNameModel() {
		return new ReadOnlyModel<>(() -> {
			// temporary code
			TaskDto taskDto = parentPage.getTaskDto();
			String name = WfGuiUtil.getLocalizedProcessName(taskDto.getWorkflowContext(), TaskSummaryPanel.this);
			if (name == null) {
				name = WfGuiUtil.getLocalizedTaskName(taskDto.getWorkflowContext(), TaskSummaryPanel.this);
			}
			if (name == null) {
				name = taskDto.getName();
			}
			return name;
		});
	}

	@Override
	protected IModel<String> getTitleModel() {
		return new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskDto taskDto = parentPage.getTaskDto();
				if (taskDto.isWorkflow()) {
					return getString("TaskSummaryPanel.requestedBy", taskDto.getRequestedBy());
				} else {
					TaskType taskType = getModelObject();
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
		return new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				if (parentPage.getTaskDto().isWorkflow()) {
					return getString("TaskSummaryPanel.requestedOn", getRequestedOn());
				} else {
					TaskType taskType = getModelObject();
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
		return new AbstractReadOnlyModel<String>() {
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

				TaskType taskType = getModel().getObject();
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

                    PatternDateConverter pdc = new PatternDateConverter
                            (WebComponentUtil.getLocalizedDatePattern(DateLabelComponent.SHORT_MEDIUM_STYLE), true );
                    String date = pdc.convertToString(new Date(started), WebComponentUtil.getCurrentLocale());
                    return getString("TaskStatePanel.message.executionTime.notFinished", date,
							DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - started));
				} else {
                    PatternDateConverter pdc = new PatternDateConverter
                            (WebComponentUtil.getLocalizedDatePattern(DateLabelComponent.SHORT_MEDIUM_STYLE), true );
                    String startedDate = pdc.convertToString(new Date(started), WebComponentUtil.getCurrentLocale());
                    String finishedDate = pdc.convertToString(new Date(finished), WebComponentUtil.getCurrentLocale());

					return getString("TaskStatePanel.message.executionTime.finished",
                            startedDate, finishedDate,
							DurationFormatUtils.formatDurationHMS(finished - started));
				}
			}
		};
	}

	public AutoRefreshPanel getRefreshPanel() {
		return (AutoRefreshPanel) getTag(ID_TAG_REFRESH);
	}
}
