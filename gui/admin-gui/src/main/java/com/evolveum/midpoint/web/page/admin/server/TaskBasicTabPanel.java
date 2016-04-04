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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * @author semancik
 */
public class TaskBasicTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_NAME = "name";
	private static final String ID_NAME_LABEL = "nameLabel";
	private static final String ID_DESCRIPTION = "description";
	private static final String ID_DESCRIPTION_LABEL = "descriptionLabel";
	private static final String ID_OID = "oid";
	private static final String ID_IDENTIFIER = "identifier";
	private static final String ID_CATEGORY = "category";
	private static final String ID_PARENT = "parent";
	private static final String ID_HANDLER_URI_LIST = "handlerUriList";
	private static final String ID_HANDLER_URI = "handlerUri";
	private static final String ID_EXECUTION = "execution";
	private static final String ID_NODE = "node";

	private static final Trace LOGGER = TraceManager.getTrace(TaskBasicTabPanel.class);

	private LoadableModel<TaskDto> taskDtoModel;
	private PageTaskEdit parentPage;

	public TaskBasicTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			LoadableModel<TaskDto> taskDtoModel, PageTaskEdit parentPage) {
		super(id, mainForm, taskWrapperModel, parentPage);
		this.taskDtoModel = taskDtoModel;
		this.parentPage = parentPage;
		initLayout();
		setOutputMarkupId(true);
	}
	
	private void initLayout() {

		final VisibleEnableBehaviour visibleIfEdit = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return parentPage.isEdit();
			}
		};
		final VisibleEnableBehaviour visibleIfView = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !parentPage.isEdit();
			}
		};

		RequiredTextField<String> name = new RequiredTextField<>(ID_NAME, new PropertyModel<String>(taskDtoModel, TaskDto.F_NAME));
		name.add(visibleIfEdit);
		name.add(new AttributeModifier("style", "width: 100%"));
		name.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		add(name);

		Label nameLabel = new Label(ID_NAME_LABEL, new PropertyModel(taskDtoModel, TaskDto.F_NAME));
		nameLabel.add(visibleIfView);
		add(nameLabel);

		TextArea<String> description = new TextArea<>(ID_DESCRIPTION, new PropertyModel<String>(taskDtoModel, TaskDto.F_DESCRIPTION));
		description.add(visibleIfEdit);
		//        description.add(new AttributeModifier("style", "width: 100%"));
		//        description.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		add(description);

		Label descriptionLabel = new Label(ID_DESCRIPTION_LABEL, new PropertyModel(taskDtoModel, TaskDto.F_DESCRIPTION));
		descriptionLabel.add(visibleIfView);
		add(descriptionLabel);

		Label oid = new Label(ID_OID, new PropertyModel(getObjectWrapperModel(), ID_OID));
		add(oid);

		add(new Label(ID_IDENTIFIER, new PropertyModel(taskDtoModel, TaskDto.F_IDENTIFIER)));
		add(new Label(ID_CATEGORY, new PropertyModel(taskDtoModel, TaskDto.F_CATEGORY)));

		final LinkPanel parent = new LinkPanel(ID_PARENT, new PropertyModel<>(taskDtoModel, TaskDto.F_PARENT_TASK_NAME)) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				String oid = taskDtoModel.getObject().getParentTaskOid();
				if (oid != null) {
					PageParameters parameters = new PageParameters();
					parameters.add(OnePageParameterEncoder.PARAMETER, oid);
					setResponsePage(new PageTaskEdit(parameters));
				}
			}
		};
		add(parent);

		ListView<String> handlerUriList = new ListView<String>(ID_HANDLER_URI_LIST, new PropertyModel(taskDtoModel, TaskDto.F_HANDLER_URI_LIST)) {
			@Override
			protected void populateItem(ListItem<String> item) {
				item.add(new Label(ID_HANDLER_URI, item.getModelObject()));
			}
		};
		add(handlerUriList);

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
		add(execution);

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
		add(node);

	}

	@Override
	public Collection<Component> getComponentsToUpdate() {
		return Collections.<Component>singleton(this);
	}

}
