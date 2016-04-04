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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.wf.WorkItemsTablePanel;
import com.evolveum.midpoint.web.component.wf.processes.itemApproval.ItemApprovalHistoryPanel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.PropertyModel;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author semancik
 */
public class TaskApprovalsTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_DELTAS_TO_BE_APPROVED = "deltasToBeApproved";
	private static final String ID_HISTORY = "history";
	private static final String ID_CURRENT_WORK_ITEMS = "currentWorkItems";

	private static final Trace LOGGER = TraceManager.getTrace(TaskApprovalsTabPanel.class);

	public TaskApprovalsTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			LoadableModel<TaskDto> taskDtoModel, PageBase pageBase) {
		super(id, mainForm, taskWrapperModel, pageBase);
		initLayout(taskDtoModel, pageBase);
		setOutputMarkupId(true);
	}
	
	private void initLayout(LoadableModel<TaskDto> taskDtoModel, PageBase pageBase) {

		add(new ScenePanel(ID_DELTAS_TO_BE_APPROVED, new PropertyModel(taskDtoModel, TaskDto.F_WORKFLOW_DELTA_IN)));
		add(new ItemApprovalHistoryPanel(ID_HISTORY, new PropertyModel<WfContextType>(taskDtoModel, TaskDto.F_WORKFLOW_CONTEXT),
				UserProfileStorage.TableId.PAGE_TASK_HISTORY_PANEL, (int) pageBase.getItemsPerPage(UserProfileStorage.TableId.PAGE_TASK_HISTORY_PANEL)));

		ISortableDataProvider<WorkItemDto, String> provider = new ListDataProvider(this, new PropertyModel<List<WorkItemDto>>(taskDtoModel, TaskDto.F_WORK_ITEMS));
		add(new WorkItemsTablePanel(ID_CURRENT_WORK_ITEMS, provider,
				UserProfileStorage.TableId.PAGE_TASK_CURRENT_WORK_ITEMS_PANEL,
				(int) pageBase.getItemsPerPage(UserProfileStorage.TableId.PAGE_TASK_CURRENT_WORK_ITEMS_PANEL),
				WorkItemsTablePanel.View.ITEMS_FOR_PROCESS));

	}

	@Override
	public Collection<Component> getComponentsToUpdate() {
		return Collections.<Component>singleton(this);
	}
}
