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
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusIcon;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 *
 */
public class TaskSummaryPanel extends FocusSummaryPanel<TaskType> {
	private static final long serialVersionUID = -5077637168906420769L;

	private static final String ID_TAG_EXECUTION_STATUS = "summaryTagExecutionStatus";
	private static final String ID_TAG_RESULT = "summaryTagResult";

	public TaskSummaryPanel(String id, IModel<ObjectWrapper<TaskType>> model) {
		super(id, model);
		
		SummaryTag<TaskType> tagResult = new SummaryTag<TaskType>(ID_TAG_RESULT, model) {
			@Override
			protected void initialize(ObjectWrapper<TaskType> wrapper) {
				OperationResultStatusType resultStatus = wrapper.getObject().asObjectable().getResultStatus();
				String icon = OperationResultStatusIcon.parseOperationalResultStatus(resultStatus).getIcon();
				setIconCssClass(icon);
				setLabel(PageBase.createStringResourceStatic(TaskSummaryPanel.this, resultStatus).getString());
				// TODO setColor
			}
		};
		addTag(tagResult);

		SummaryTag<TaskType> tagExecutionStatus = new SummaryTag<TaskType>(ID_TAG_EXECUTION_STATUS, model) {
			@Override
			protected void initialize(ObjectWrapper<TaskType> wrapper) {
				TaskType taskType = wrapper.getObject().asObjectable();
				TaskDtoExecutionStatus status = TaskDtoExecutionStatus.fromTaskExecutionStatus(taskType.getExecutionStatus(), taskType.getNodeAsObserved() != null);
				setLabel(PageBase.createStringResourceStatic(TaskSummaryPanel.this, status).getString());
				// TODO setColor
			}
		};
		addTag(tagExecutionStatus);
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
	protected boolean isActivationVisible() {
		return false;
	}

	@Override
	protected IModel<String> getTitleModel() {
		return new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskType taskType = getModelObject().getObject().asObjectable();
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
				TaskType taskType = getModelObject().getObject().asObjectable();
				if (taskType.getOperationStats() != null && taskType.getOperationStats().getIterativeTaskInformation() != null &&
						taskType.getOperationStats().getIterativeTaskInformation().getLastSuccessObjectName() != null) {
					return createStringResource("TaskSummaryPanel.lastProcessed", taskType.getOperationStats().getIterativeTaskInformation().getLastSuccessObjectName()).getString();
				} else {
					return "";
				}
			}
		};
	}

}
