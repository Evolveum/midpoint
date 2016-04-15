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
package com.evolveum.midpoint.web.component.objectdetails;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author semancik
 */
public class FocusDetailsTabPanel<F extends FocusType> extends AbstractFocusTabPanel<F> {
	private static final long serialVersionUID = 1L;
	
	protected static final String ID_FOCUS_FORM = "focusDetails";
	
	protected static final String ID_TASK_TABLE = "taskTable";
	protected static final String ID_TASKS = "tasks";

	private static final Trace LOGGER = TraceManager.getTrace(FocusDetailsTabPanel.class);

	public FocusDetailsTabPanel(String id, Form mainForm, 
			LoadableModel<ObjectWrapper<F>> focusWrapperModel, 
			LoadableModel<List<AssignmentEditorDto>> assignmentsModel, 
			LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel,
			PageBase pageBase) {
		super(id, mainForm, focusWrapperModel, assignmentsModel, projectionModel, pageBase);
		initLayout();
	}
	
	private void initLayout() {

		PrismObjectPanel<F> panel = new PrismObjectPanel<F>(ID_FOCUS_FORM, getObjectWrapperModel(),
				new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), getMainForm(), getPageBase());
		add(panel);

		WebMarkupContainer tasks = new WebMarkupContainer(ID_TASKS);
		tasks.setOutputMarkupId(true);
		add(tasks);
		initTasks(tasks);

	}

	private void initTasks(WebMarkupContainer tasks) {
		List<IColumn<TaskDto, String>> taskColumns = initTaskColumns();
		final TaskDtoProvider taskDtoProvider = new TaskDtoProvider(getPageBase(), TaskDtoProviderOptions.minimalOptions());
		taskDtoProvider.setQuery(createTaskQuery(null));
		TablePanel taskTable = new TablePanel<TaskDto>(ID_TASK_TABLE, taskDtoProvider, taskColumns) {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				StringValue oidValue = getPageParameters().get(OnePageParameterEncoder.PARAMETER);

				taskDtoProvider.setQuery(createTaskQuery(oidValue != null ? oidValue.toString() : null));
			}
		};
		tasks.add(taskTable);

		tasks.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return taskDtoProvider.size() > 0;
			}
		});
	}

	private ObjectQuery createTaskQuery(String oid) {
		List<ObjectFilter> filters = new ArrayList<ObjectFilter>();

		if (oid == null) {
			oid = "non-existent"; // TODO !!!!!!!!!!!!!!!!!!!!
		}
		try {
			filters.add(RefFilter.createReferenceEqual(TaskType.F_OBJECT_REF, TaskType.class,
					getPrismContext(), oid));
			filters.add(NotFilter.createNot(EqualFilter.createEqual(TaskType.F_EXECUTION_STATUS,
					TaskType.class, getPrismContext(), null, TaskExecutionStatusType.CLOSED)));
			filters.add(EqualFilter.createEqual(TaskType.F_PARENT, TaskType.class, getPrismContext(), null));
		} catch (SchemaException e) {
			throw new SystemException("Unexpected SchemaException when creating task filter", e);
		}

		return new ObjectQuery().createObjectQuery(AndFilter.createAnd(filters));
	}

	private List<IColumn<TaskDto, String>> initTaskColumns() {
		List<IColumn<TaskDto, String>> columns = new ArrayList<IColumn<TaskDto, String>>();

		columns.add(PageTasks.createTaskNameColumn(this, "pageAdminFocus.task.name"));
		columns.add(PageTasks.createTaskCategoryColumn(this, "pageAdminFocus.task.category"));
		columns.add(PageTasks.createTaskExecutionStatusColumn(this, "pageAdminFocus.task.execution"));
		columns.add(PageTasks.createTaskResultStatusColumn(this, "pageAdminFocus.task.status"));
		return columns;
	}

}
