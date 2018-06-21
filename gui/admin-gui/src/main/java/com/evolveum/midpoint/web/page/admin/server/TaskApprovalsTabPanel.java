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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.util.Collection;
import java.util.Collections;

/**
 * @author semancik
 */
public class TaskApprovalsTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_WORKFLOW_PARENT_PANEL = "workflowParentPanel";
	private static final String ID_WORKFLOW_CHILD_PANEL = "workflowChildPanel";

	private PageTaskEdit parentPage;

	private TaskWfChildPanel childPanel;
	private TaskWfParentPanel parentPanel;

	private static final Trace LOGGER = TraceManager.getTrace(TaskApprovalsTabPanel.class);

	public TaskApprovalsTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			IModel<TaskDto> taskDtoModel, PageTaskEdit parentPage) {
		super(id, mainForm, taskWrapperModel, parentPage);
		this.parentPage = parentPage;
		initLayout(taskDtoModel);
		setOutputMarkupId(true);
	}

	private void initLayout(final IModel<TaskDto> taskDtoModel) {

		childPanel = new TaskWfChildPanel(ID_WORKFLOW_CHILD_PANEL, taskDtoModel, parentPage);
		childPanel.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return taskDtoModel.getObject().isWorkflowChild();
			}
		});
		add(childPanel);

		parentPanel = new TaskWfParentPanel(ID_WORKFLOW_PARENT_PANEL, taskDtoModel, parentPage);
		parentPanel.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return taskDtoModel.getObject().isWorkflowParent();
			}
		});
		add(parentPanel);
	}

	@Override
	public Collection<Component> getComponentsToUpdate() {
		if (parentPage.getTaskDto().isWorkflowChild()) {
			return childPanel.getComponentsToUpdate();
		} else if (parentPage.getTaskDto().isWorkflowParent()) {
			return parentPanel.getComponentsToUpdate();
		} else {
			// shouldn't occur
			return Collections.<Component>singleton(this);
		}
	}
}
