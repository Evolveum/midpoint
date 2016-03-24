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
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractFocusTabPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.prism.PrismPropertyPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismPropertyRealValueFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.PropertyWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.certification.dto.AccessCertificationReviewerDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.StageDefinitionDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusProjectionDto;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import java.util.ArrayList;
import java.util.List;

/**
 * @author semancik
 */
public class TaskBasicTabPanel extends AbstractObjectTabPanel<TaskType> {
	private static final long serialVersionUID = 1L;
	
	protected static final String ID_NAME = "name";
	protected static final String ID_DESCRIPTION = "description";
	private static final String ID_OID = "oid";
	private static final String ID_IDENTIFIER = "identifier";
	private static final String ID_CATEGORY = "category";
	private static final String ID_PARENT = "parent";
	private static final String ID_HANDLER_URI_LIST = "handlerUriList";
	private static final String ID_HANDLER_URI = "handlerUri";

	private static final Trace LOGGER = TraceManager.getTrace(TaskBasicTabPanel.class);

	private LoadableModel<TaskDto> taskDtoModel;

	public TaskBasicTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			LoadableModel<TaskDto> taskDtoModel, PageBase pageBase) {
		super(id, mainForm, taskWrapperModel, pageBase);
		this.taskDtoModel = taskDtoModel;
		initLayout(pageBase);
	}
	
	private void initLayout(final PageBase pageBase) {

		PrismPropertyPanel taskNamePanel = new PrismPropertyPanel<>(ID_NAME,
				new PropertyWrapperFromObjectWrapperModel(getObjectWrapperModel(), TaskType.F_NAME),
				null, pageBase);
		taskNamePanel.setLabelContainerVisible(false);
		add(taskNamePanel);

		PrismPropertyPanel taskDescriptionPanel = new PrismPropertyPanel<>(ID_DESCRIPTION,
				new PropertyWrapperFromObjectWrapperModel(getObjectWrapperModel(), TaskType.F_DESCRIPTION),
				null, pageBase);
		taskDescriptionPanel.setLabelContainerVisible(false);
		add(taskDescriptionPanel);

		Label oid = new Label(ID_OID, new PropertyModel(getObjectWrapperModel(), "oid"));
		add(oid);

		add(new Label(ID_IDENTIFIER, new PrismPropertyRealValueFromObjectWrapperModel<>(getObjectWrapperModel(), TaskType.F_TASK_IDENTIFIER)));
		add(new Label(ID_CATEGORY, new PrismPropertyRealValueFromObjectWrapperModel<>(getObjectWrapperModel(), TaskType.F_CATEGORY)));

		LinkPanel parent = new LinkPanel(ID_PARENT, new PropertyModel<>(taskDtoModel, TaskDto.F_PARENT_TASK_NAME)) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				String oid = taskDtoModel.getObject().getParentTaskOid();
				if (oid != null) {
					PageParameters parameters = new PageParameters();
					parameters.add(OnePageParameterEncoder.PARAMETER, oid);
					setResponsePage(new PageTaskEdit(parameters, pageBase));
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
	}

}
