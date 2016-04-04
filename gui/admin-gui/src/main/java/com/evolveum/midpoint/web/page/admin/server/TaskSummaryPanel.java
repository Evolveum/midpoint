/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshDto;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshPanel;
import com.evolveum.midpoint.web.component.util.SummaryTagSimple;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusIcon;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.Date;

/**
 * @author mederly
 *
 */
public class TaskSummaryPanel extends ObjectSummaryPanel<TaskType> {
	private static final long serialVersionUID = -5077637168906420769L;

	//private static final String ID_TAG_EXECUTION_STATUS = "summaryTagExecutionStatus";
	private static final String ID_TAG_RESULT = "summaryTagResult";
	private static final String ID_TAG_EMPTY = "emptyTag";
	private static final String ID_TAG_REFRESH = "refreshTag";

	public TaskSummaryPanel(String id, IModel<PrismObject<TaskType>> model, IModel<AutoRefreshDto> refreshModel, PageTaskEdit parentPage) {
		super(id, model);

		SummaryTagSimple<TaskType> tagExecutionStatus = new SummaryTagSimple<TaskType>(ID_FIRST_SUMMARY_TAG, model) {
			@Override
			protected void initialize(PrismObject<TaskType> taskObject) {
				TaskType taskType = taskObject.asObjectable();
				TaskDtoExecutionStatus status = TaskDtoExecutionStatus.fromTaskExecutionStatus(taskType.getExecutionStatus(), taskType.getNodeAsObserved() != null);
				String icon = getIconForExecutionStatus(status);
				setIconCssClass(icon);
				setLabel(PageBase.createStringResourceStatic(TaskSummaryPanel.this, status).getString());
				// TODO setColor
			}
		};
		addTag(tagExecutionStatus);

		SummaryTagSimple<TaskType> tagResult = new SummaryTagSimple<TaskType>(ID_TAG_RESULT, model) {
			@Override
			protected void initialize(PrismObject<TaskType> taskObject) {
				OperationResultStatusType resultStatus = taskObject.asObjectable().getResultStatus();
				String icon = OperationResultStatusIcon.parseOperationalResultStatus(resultStatus).getIcon();
				setIconCssClass(icon);
				setLabel(PageBase.createStringResourceStatic(TaskSummaryPanel.this, resultStatus).getString());
				// TODO setColor
			}
		};
		addTag(tagResult);

		addTag(new Label(ID_TAG_EMPTY, new Model("<br/>")).setEscapeModelStrings(false));

		final AutoRefreshPanel refreshTag = new AutoRefreshPanel(ID_TAG_REFRESH, refreshModel, parentPage);
		refreshTag.setOutputMarkupId(true);
		addTag(refreshTag);
	}

	private String getIconForExecutionStatus(TaskDtoExecutionStatus status) {
		switch (status) {
			case RUNNING: return "fa fa-fw fa-lg fa-spinner";
			case RUNNABLE: return "fa fa-fw fa-lg fa-hand-o-up";
			case SUSPENDED: return "fa fa-fw fa-lg fa-bed";
			case SUSPENDING: return "fa fa-fw fa-lg fa-bed";
			case WAITING: return "fa fa-fw fa-lg fa-clock-o";
			case CLOSED: return "fa fa-fw fa-lg fa-power-off";
			default: return "";
		}
	}

	@Override
	protected QName getDisplayNamePropertyName() {
		return TaskType.F_NAME;
	}

	@Override
	protected String getIconCssClass() {
		return "fa fa-tasks";
	}

	@Override
	protected String getIconBoxAdditionalCssClass() {		// TODO
		return "summary-panel-resource";
	}

	@Override
	protected String getBoxAdditionalCssClass() {			// TODO
		return "summary-panel-resource";
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
	protected IModel<String> getTitleModel() {
		return new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskType taskType = getModelObject().asObjectable();
				if (taskType.getExpectedTotal() != null) {
					return createStringResource("TaskSummaryPanel.progressWithTotalKnown", taskType.getProgress(), taskType.getExpectedTotal()).getString();
				} else {
					return createStringResource("TaskSummaryPanel.progressWithTotalUnknown", taskType.getProgress()).getString();
				}
			}
		};
	}

	@Override
	protected IModel<String> getTitle2Model() {
		return new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskType taskType = getModelObject().asObjectable();
				if (taskType.getOperationStats() != null && taskType.getOperationStats().getIterativeTaskInformation() != null &&
						taskType.getOperationStats().getIterativeTaskInformation().getLastSuccessObjectName() != null) {
					return createStringResource("TaskSummaryPanel.lastProcessed", taskType.getOperationStats().getIterativeTaskInformation().getLastSuccessObjectName()).getString();
				} else {
					return "";
				}
			}
		};
	}

	@Override
	protected IModel<String> getTitle3Model() {
		return new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskType taskType = getModel().getObject().asObjectable();
				if (taskType == null) {
					return null;
				}
				long started = XmlTypeConverter.toMillis(taskType.getLastRunStartTimestamp());
				long finished = XmlTypeConverter.toMillis(taskType.getLastRunFinishTimestamp());
				if (started == 0) {
					return null;
				}
				if ((TaskExecutionStatus.RUNNABLE.equals(taskType.getExecutionStatus()) && taskType.getNodeAsObserved() != null)
						|| finished == 0 || finished < started) {
					return getString("TaskStatePanel.message.executionTime.notFinished", formatDate(new Date(started)),
							DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - started));
				} else {
					return getString("TaskStatePanel.message.executionTime.finished",
							formatDate(new Date(started)), formatDate(new Date(finished)),
							DurationFormatUtils.formatDurationHMS(finished - started));
				}
			}
		};
	}

	private String formatDate(Date date) {
		if (date == null) {
			return null;
		}
		return date.toLocaleString();
	}

	public AutoRefreshPanel getRefreshPanel() {
		return (AutoRefreshPanel) getTag(ID_TAG_REFRESH);
	}
}
