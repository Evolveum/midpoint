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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.web.page.admin.server.handlers.HandlerPanelFactory;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.HandlerDto;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * @author semancik
 * @author lazyman
 * @author mserbak
 * @author mederly
 */
public class TaskBasicTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_NAME_CONTAINER = "nameContainer";
	private static final String ID_NAME = "name";
	private static final String ID_DESCRIPTION_CONTAINER = "descriptionContainer";
	private static final String ID_DESCRIPTION = "description";
	private static final String ID_OID = "oid";
	private static final String ID_IDENTIFIER_CONTAINER = "identifierContainer";
	private static final String ID_IDENTIFIER = "identifier";
	private static final String ID_CATEGORY_CONTAINER = "categoryContainer";
	private static final String ID_CATEGORY = "category";
	private static final String ID_PARENT_CONTAINER = "parentContainer";
	private static final String ID_PARENT = "parent";
	private static final String ID_OWNER_CONTAINER = "ownerContainer";
	private static final String ID_OWNER = "owner";
	private static final String ID_HANDLER_URI_CONTAINER = "handlerUriContainer";
	private static final String ID_HANDLER_URI = "handlerUri";
	private static final String ID_EXECUTION_CONTAINER = "executionContainer";
	private static final String ID_EXECUTION = "execution";

	private static final String ID_NODE = "node";

	private static final String ID_HANDLER_PANEL = "handlerPanel";

	private static final Trace LOGGER = TraceManager.getTrace(TaskBasicTabPanel.class);

	private IModel<TaskDto> taskDtoModel;
	private PageTaskEdit parentPage;

	public TaskBasicTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			IModel<TaskDto> taskDtoModel, PageTaskEdit parentPage) {
		super(id, mainForm, taskWrapperModel, parentPage);
		this.taskDtoModel = taskDtoModel;
		this.parentPage = parentPage;
		initLayoutBasic();
		initLayoutHandler();
		setOutputMarkupId(true);
	}

	private void initLayoutBasic() {

		// Name
		WebMarkupContainer nameContainer = new WebMarkupContainer(ID_NAME_CONTAINER);
		RequiredTextField<String> name = new RequiredTextField<>(ID_NAME, new PropertyModel<String>(taskDtoModel, TaskDto.F_NAME));
		name.add(parentPage.createEnabledIfEdit(new ItemPath(TaskType.F_NAME)));
		name.add(new AttributeModifier("style", "width: 100%"));
		name.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		nameContainer.add(name);
		nameContainer.add(parentPage.createVisibleIfAccessible(new ItemPath(TaskType.F_NAME)));
		add(nameContainer);

		// Description
		WebMarkupContainer descriptionContainer = new WebMarkupContainer(ID_DESCRIPTION_CONTAINER);
		TextArea<String> description = new TextArea<>(ID_DESCRIPTION, new PropertyModel<String>(taskDtoModel, TaskDto.F_DESCRIPTION));
		description.add(parentPage.createEnabledIfEdit(new ItemPath(TaskType.F_DESCRIPTION)));
		//        description.add(new AttributeModifier("style", "width: 100%"));
		//        description.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		descriptionContainer.add(description);
		descriptionContainer.add(parentPage.createVisibleIfAccessible(new ItemPath(TaskType.F_DESCRIPTION)));
		add(descriptionContainer);

		// OID
		Label oid = new Label(ID_OID, new PropertyModel(getObjectWrapperModel(), ID_OID));
		add(oid);

		// Identifier
		WebMarkupContainer identifierContainer = new WebMarkupContainer(ID_IDENTIFIER_CONTAINER);
		identifierContainer.add(new Label(ID_IDENTIFIER, new PropertyModel(taskDtoModel, TaskDto.F_IDENTIFIER)));
		identifierContainer.add(parentPage.createVisibleIfAccessible(new ItemPath(TaskType.F_TASK_IDENTIFIER)));
		add(identifierContainer);

		// Category
		WebMarkupContainer categoryContainer = new WebMarkupContainer(ID_CATEGORY_CONTAINER);
		categoryContainer.add(new Label(ID_CATEGORY,
				WebComponentUtil.createCategoryNameModel(this, new PropertyModel(taskDtoModel, TaskDto.F_CATEGORY))));
		categoryContainer.add(parentPage.createVisibleIfAccessible(new ItemPath(TaskType.F_CATEGORY)));
		add(categoryContainer);

		// Parent
		WebMarkupContainer parentContainer = new WebMarkupContainer(ID_PARENT_CONTAINER);
		final LinkPanel parent = new LinkPanel(ID_PARENT, new PropertyModel<>(taskDtoModel, TaskDto.F_PARENT_TASK_NAME)) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				String oid = taskDtoModel.getObject().getParentTaskOid();
				if (oid != null) {
					PageParameters parameters = new PageParameters();
					parameters.add(OnePageParameterEncoder.PARAMETER, oid);
					getPageBase().navigateToNext(PageTaskEdit.class, parameters);
				}
			}
		};
		parentContainer.add(parent);
		parentContainer.add(parentPage.createVisibleIfAccessible(new ItemPath(TaskType.F_PARENT)));
		add(parentContainer);

		// Owner
		WebMarkupContainer ownerContainer = new WebMarkupContainer(ID_OWNER_CONTAINER);
		final LinkPanel owner = new LinkPanel(ID_OWNER, new PropertyModel<>(taskDtoModel, TaskDto.F_OWNER_NAME)) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				String oid = taskDtoModel.getObject().getOwnerOid();
				if (oid != null) {
					PageParameters parameters = new PageParameters();
					parameters.add(OnePageParameterEncoder.PARAMETER, oid);
					getPageBase().navigateToNext(PageUser.class, parameters);
				}
			}
		};
		ownerContainer.add(owner);
		ownerContainer.add(parentPage.createVisibleIfAccessible(new ItemPath(TaskType.F_OWNER_REF)));
		add(ownerContainer);

		// Handler URI
		ListView<String> handlerUriContainer = new ListView<String>(ID_HANDLER_URI_CONTAINER, new PropertyModel(taskDtoModel, TaskDto.F_HANDLER_URI_LIST)) {
			@Override
			protected void populateItem(ListItem<String> item) {
				item.add(new Label(ID_HANDLER_URI, item.getModelObject()));
			}
		};
		handlerUriContainer.add(parentPage.createVisibleIfAccessible(new ItemPath(TaskType.F_HANDLER_URI), new ItemPath(TaskType.F_OTHER_HANDLERS_URI_STACK)));
		add(handlerUriContainer);

		// Execution
		WebMarkupContainer executionContainer = new WebMarkupContainer(ID_EXECUTION_CONTAINER);
		Label execution = new Label(ID_EXECUTION, new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				TaskDtoExecutionStatus executionStatus = taskDtoModel.getObject().getExecution();
				if (executionStatus != TaskDtoExecutionStatus.CLOSED) {
					return getString(TaskDtoExecutionStatus.class.getSimpleName() + "." + executionStatus.name());
				} else {
					return getString(TaskDtoExecutionStatus.class.getSimpleName() + "." + executionStatus.name() + ".withTimestamp",
							new AbstractReadOnlyModel<String>() {
								@Override
								public String getObject() {
									if (taskDtoModel.getObject().getCompletionTimestamp() != null) {
										return new Date(taskDtoModel.getObject().getCompletionTimestamp()).toLocaleString();   // todo correct formatting
									} else {
										return "?";
									}
								}
							});
				}
			}
		});
		executionContainer.add(execution);
		Label node = new Label(ID_NODE, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskDto dto = taskDtoModel.getObject();
				if (!TaskDtoExecutionStatus.RUNNING.equals(dto.getExecution())) {
					return null;
				}
				return parentPage.getString("pageTaskEdit.message.node", dto.getExecutingAt());
			}
		});
		executionContainer.add(node);
		executionContainer.add(parentPage.createVisibleIfAccessible(new ItemPath(TaskType.F_EXECUTION_STATUS), new ItemPath(TaskType.F_NODE_AS_OBSERVED)));
		add(executionContainer);

	}

	private void initLayoutHandler() {
		Panel handlerPanel = HandlerPanelFactory.instance().createPanelForTask(ID_HANDLER_PANEL, new PropertyModel<>(taskDtoModel, TaskDto.F_HANDLER_DTO), parentPage);
		add(handlerPanel);
	}

	@Override
	public Collection<Component> getComponentsToUpdate() {
		return Collections.<Component>singleton(this);
	}

}
