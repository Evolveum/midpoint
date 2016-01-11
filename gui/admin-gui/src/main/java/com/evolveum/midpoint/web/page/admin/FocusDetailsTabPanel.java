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
package com.evolveum.midpoint.web.page.admin;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.users.component.*;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusProjectionDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import javax.xml.namespace.QName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author semancik
 */
public class FocusDetailsTabPanel<F extends FocusType> extends FocusTabPanel {
	private static final long serialVersionUID = 1L;
	
	protected static final String ID_FOCUS_FORM = "focusDetails";
	
	protected static final String ID_TASK_TABLE = "taskTable";
	protected static final String ID_TASKS = "tasks";

	private static final Trace LOGGER = TraceManager.getTrace(FocusDetailsTabPanel.class);

	public FocusDetailsTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusModel, PageBase page) {
		super(id, mainForm, focusModel, page);
		initLayout();
	}
	
	private void initLayout() {

		PrismObjectPanel panel = new PrismObjectPanel<F>(ID_FOCUS_FORM, getFocusModel(),
				new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), getMainForm(), getPageBase()) {

			@Override
			protected IModel<String> createDescription(IModel<ObjectWrapper<F>> model) {
				return createStringResource("pageAdminFocus.description");
			}
		};
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
