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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.input.ChoiceableChoiceRenderer;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskAddResourcesDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author semancik
 * @author lazyman
 * @author mserbak
 * @author mederly
 */
public class TaskObjectsTabPanel extends AbstractObjectTabPanel<TaskType> {
	private static final long serialVersionUID = 1L;

	private static final String DOT_CLASS = TaskObjectsTabPanel.class.getName() + ".";
	private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "createResourceList";
	private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

	private static final String ID_RESOURCE_REF = "resourceRef";
	private static final String ID_KIND = "kind";
	private static final String ID_INTENT = "intent";
	private static final String ID_OBJECT_CLASS = "objectClass";
	private static final String ID_DRY_RUN = "dryRun";

	private static final Trace LOGGER = TraceManager.getTrace(TaskObjectsTabPanel.class);

	private LoadableModel<TaskDto> taskDtoModel;
	private PageTaskEdit parentPage;

	public TaskObjectsTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			LoadableModel<TaskDto> taskDtoModel, PageTaskEdit parentPage) {
		super(id, mainForm, taskWrapperModel, parentPage);
		this.taskDtoModel = taskDtoModel;
		this.parentPage = parentPage;
		initLayout();
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

		final DropDownChoice<TaskAddResourcesDto> object = new DropDownChoice<>(ID_RESOURCE_REF,
				new PropertyModel<TaskAddResourcesDto>(taskDtoModel, TaskDto.F_RESOURCE_REFERENCE),
				new AbstractReadOnlyModel<List<TaskAddResourcesDto>>() {
					@Override
					public List<TaskAddResourcesDto> getObject() {
						return createResourceList();
					}
				}, new ChoiceableChoiceRenderer<TaskAddResourcesDto>());
		object.setOutputMarkupId(true);
		object.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
				if (!parentPage.isEdit()) {
					return false;
				}
				TaskDto dto = taskDtoModel.getObject();
				boolean sync = TaskCategory.LIVE_SYNCHRONIZATION.equals(dto.getCategory());
				boolean recon = TaskCategory.RECONCILIATION.equals(dto.getCategory());
				boolean importAccounts = TaskCategory.IMPORTING_ACCOUNTS.equals(dto.getCategory());
				return sync || recon || importAccounts;
			}
		});
		object.add(new AjaxFormComponentUpdatingBehavior("change") {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				Task task = parentPage.createSimpleTask(OPERATION_LOAD_RESOURCE);
				OperationResult result = task.getResult();
				List<QName> objectClassList = new ArrayList<>();

				TaskAddResourcesDto resourcesDto = taskDtoModel.getObject().getResource();

				if(resourcesDto != null){
					PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(ResourceType.class,
							resourcesDto.getOid(), parentPage, task, result);

					try {
						ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, getPrismContext());
						schema.getObjectClassDefinitions();

						for(Definition def: schema.getDefinitions()){
							objectClassList.add(def.getTypeName());
						}

						taskDtoModel.getObject().setObjectClassList(objectClassList);
					} catch (Exception e){
						LoggingUtils.logException(LOGGER, "Couldn't load object class list from resource.", e);
						error("Couldn't load object class list from resource.");
					}

				}

				target.add(get(ID_MAIN_FORM + ":" + ID_OBJECT_CLASS));
			}
		});
		add(object);

		final DropDownChoice kind = new DropDownChoice<>(ID_KIND,
				new PropertyModel<ShadowKindType>(taskDtoModel, TaskDto.F_KIND),
				WebComponentUtil.createReadonlyModelFromEnum(ShadowKindType.class), new EnumChoiceRenderer<ShadowKindType>());
		kind.setOutputMarkupId(true);
		kind.setNullValid(true);
		kind.add(new VisibleEnableBehaviour(){

			@Override
			public boolean isEnabled() {
				if(!parentPage.isEdit()){
					return false;
				}

				TaskDto dto = taskDtoModel.getObject();
				boolean sync = TaskCategory.LIVE_SYNCHRONIZATION.equals(dto.getCategory());
				boolean recon = TaskCategory.RECONCILIATION.equals(dto.getCategory());
				boolean importAccounts = TaskCategory.IMPORTING_ACCOUNTS.equals(dto.getCategory());
				return sync || recon || importAccounts;
			}
		});
		add(kind);

		final TextField<String> intent = new TextField<>(ID_INTENT, new PropertyModel<String>(taskDtoModel, TaskDto.F_INTENT));
		add(intent);
		intent.setOutputMarkupId(true);
		intent.add(new VisibleEnableBehaviour(){

			@Override
			public boolean isEnabled() {
				if(!parentPage.isEdit()){
					return false;
				}

				TaskDto dto = taskDtoModel.getObject();
				boolean sync = TaskCategory.LIVE_SYNCHRONIZATION.equals(dto.getCategory());
				boolean recon = TaskCategory.RECONCILIATION.equals(dto.getCategory());
				boolean importAccounts = TaskCategory.IMPORTING_ACCOUNTS.equals(dto.getCategory());
				return sync || recon || importAccounts;
			}
		});

		AutoCompleteSettings autoCompleteSettings = new AutoCompleteSettings();
		autoCompleteSettings.setShowListOnEmptyInput(true);
		final AutoCompleteTextField<String> objectClass = new AutoCompleteTextField<String>(ID_OBJECT_CLASS,
				new PropertyModel<String>(taskDtoModel, TaskDto.F_OBJECT_CLASS), autoCompleteSettings) {

			@Override
			protected Iterator<String> getChoices(String input) {

				return prepareObjectClassChoiceList(input);
			}
		};
		objectClass.add(new VisibleEnableBehaviour(){

			@Override
			public boolean isEnabled() {
				if(!parentPage.isEdit()){
					return false;
				}

				TaskDto dto = taskDtoModel.getObject();
				boolean sync = TaskCategory.LIVE_SYNCHRONIZATION.equals(dto.getCategory());
				boolean recon = TaskCategory.RECONCILIATION.equals(dto.getCategory());
				boolean importAccounts = TaskCategory.IMPORTING_ACCOUNTS.equals(dto.getCategory());
				return sync || recon || importAccounts;
			}
		});
		add(objectClass);

		CheckBox dryRun = new CheckBox(ID_DRY_RUN, new PropertyModel<Boolean>(taskDtoModel, TaskDto.F_DRY_RUN));
		dryRun.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return parentPage.isEdit();
			}
		});
		add(dryRun);

	}

	private List<TaskAddResourcesDto> createResourceList() {
		OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
		Task task = parentPage.createSimpleTask(OPERATION_LOAD_RESOURCES);
		List<PrismObject<ResourceType>> resources = null;
		List<TaskAddResourcesDto> resourceList = new ArrayList<>();

		try {
			resources = parentPage.getModelService().searchObjects(ResourceType.class, new ObjectQuery(), null, task, result);
			result.recomputeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get resource list.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't get resource list", ex);
		}

		if (resources != null) {
			ResourceType item = null;
			for (PrismObject<ResourceType> resource : resources) {
				item = resource.asObjectable();
				resourceList.add(new TaskAddResourcesDto(item.getOid(), WebComponentUtil.getOrigStringFromPoly(item.getName())));
			}
		}
		return resourceList;
	}

	private Iterator<String> prepareObjectClassChoiceList(String input){
		List<String> choices = new ArrayList<>();

		if(taskDtoModel.getObject().getResource() == null){
			return choices.iterator();
		}

		if(Strings.isEmpty(input)){
			for(QName q: taskDtoModel.getObject().getObjectClassList()){
				choices.add(q.getLocalPart());
				Collections.sort(choices);
			}
		} else {
			for(QName q: taskDtoModel.getObject().getObjectClassList()){
				if(q.getLocalPart().startsWith(input)){
					choices.add(q.getLocalPart());
				}
				Collections.sort(choices);
			}
		}

		return choices.iterator();
	}
}
