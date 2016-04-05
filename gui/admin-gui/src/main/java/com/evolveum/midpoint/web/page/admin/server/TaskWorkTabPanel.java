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
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author semancik
 * @author lazyman
 * @author mserbak
 * @author mederly
 */
public class TaskWorkTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
	private static final long serialVersionUID = 1L;

	private static final String DOT_CLASS = TaskWorkTabPanel.class.getName() + ".";
	private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "createResourceList";
	private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

	private static final Trace LOGGER = TraceManager.getTrace(TaskWorkTabPanel.class);

	private static final String ID_OBJECT_REF_CONTAINER = "objectRefContainer";
	private static final String ID_OBJECT_REF = "objectRef";
	private static final String ID_OBJECT_TYPE_CONTAINER = "objectTypeContainer";
	private static final String ID_OBJECT_TYPE = "objectType";
	private static final String ID_OBJECT_QUERY_CONTAINER = "objectQueryContainer";
	private static final String ID_OBJECT_QUERY = "objectQuery";
	private static final String ID_RESOURCE_REF_CONTAINER = "resourceRefContainer";
	private static final String ID_RESOURCE_REF = "resourceRef";
	private static final String ID_KIND_CONTAINER = "kindContainer";
	private static final String ID_KIND = "kind";
	private static final String ID_INTENT_CONTAINER = "intentContainer";
	private static final String ID_INTENT = "intent";
	private static final String ID_OBJECT_CLASS_CONTAINER = "objectClassContainer";
	private static final String ID_OBJECT_CLASS = "objectClass";
	private static final String ID_OBJECT_DELTA_CONTAINER = "objectDeltaContainer";
	private static final String ID_OBJECT_DELTA = "objectDelta";
	private static final String ID_SCRIPT_CONTAINER = "scriptContainer";
	private static final String ID_SCRIPT = "script";
	private static final String ID_OPTIONS_CONTAINER = "optionsContainer";
	private static final String ID_DRY_RUN_CONTAINER = "dryRunContainer";
	private static final String ID_DRY_RUN = "dryRun";
	private static final String ID_EXECUTE_IN_RAW_MODE_CONTAINER = "executeInRawModeContainer";
	private static final String ID_EXECUTE_IN_RAW_MODE = "executeInRawMode";

	private IModel<TaskDto> taskDtoModel;
	private PageTaskEdit parentPage;

	public TaskWorkTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			IModel<TaskDto> taskDtoModel, PageTaskEdit parentPage) {
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
		final VisibleEnableBehaviour enabledIfEdit = new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
				return parentPage.isEdit();
			}
		};
		final VisibleEnableBehaviour visibleForResourceCoordinates = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return parentPage.configuresResourceCoordinates();
			}
		};

		// components
		WebMarkupContainer objectRefContainer = new WebMarkupContainer(ID_OBJECT_REF_CONTAINER);
		objectRefContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getTaskDto().getObjectRef() != null && !parentPage.configuresResourceCoordinates();
			}
		});
		add(objectRefContainer);
		objectRefContainer.add(new Label(ID_OBJECT_REF, new PropertyModel<>(taskDtoModel, TaskDto.F_OBJECT_REF_NAME)));

		WebMarkupContainer objectTypeContainer = new WebMarkupContainer(ID_OBJECT_TYPE_CONTAINER);
		objectTypeContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return parentPage.configuresObjectType();
			}
		});
		add(objectTypeContainer);
		objectTypeContainer.add(new Label(ID_OBJECT_TYPE, new PropertyModel<>(taskDtoModel, TaskDto.F_OBJECT_TYPE)));

		WebMarkupContainer objectQueryContainer = new WebMarkupContainer(ID_OBJECT_QUERY_CONTAINER);
		objectQueryContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return parentPage.configuresObjectQuery();
			}
		});
		add(objectQueryContainer);
		objectQueryContainer.add(new Label(ID_OBJECT_QUERY, new PropertyModel<>(taskDtoModel, TaskDto.F_OBJECT_QUERY)));

		WebMarkupContainer resourceRefContainer = new WebMarkupContainer(ID_RESOURCE_REF_CONTAINER);
		resourceRefContainer.add(visibleForResourceCoordinates);
		add(resourceRefContainer);

		final DropDownChoice<TaskAddResourcesDto> resourceRef = new DropDownChoice<>(ID_RESOURCE_REF,
				new PropertyModel<TaskAddResourcesDto>(taskDtoModel, TaskDto.F_RESOURCE_REFERENCE),
				new AbstractReadOnlyModel<List<TaskAddResourcesDto>>() {
					@Override
					public List<TaskAddResourcesDto> getObject() {
						return createResourceList();
					}
				}, new ChoiceableChoiceRenderer<TaskAddResourcesDto>());
		resourceRef.setOutputMarkupId(true);
		resourceRef.add(enabledIfEdit);
		resourceRef.add(new AjaxFormComponentUpdatingBehavior("change") {

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
		resourceRefContainer.add(resourceRef);

		WebMarkupContainer kindContainer = new WebMarkupContainer(ID_KIND_CONTAINER);
		kindContainer.add(visibleForResourceCoordinates);
		add(kindContainer);

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
		kindContainer.add(kind);

		WebMarkupContainer intentContainer = new WebMarkupContainer(ID_INTENT_CONTAINER);
		intentContainer.add(visibleForResourceCoordinates);
		add(intentContainer);

		final TextField<String> intent = new TextField<>(ID_INTENT, new PropertyModel<String>(taskDtoModel, TaskDto.F_INTENT));
		intentContainer.add(intent);
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

		WebMarkupContainer objectClassContainer = new WebMarkupContainer(ID_OBJECT_CLASS_CONTAINER);
		objectClassContainer.add(visibleForResourceCoordinates);
		add(objectClassContainer);

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
		objectClassContainer.add(objectClass);

		WebMarkupContainer objectDeltaContainer = new WebMarkupContainer(ID_OBJECT_DELTA_CONTAINER);
		objectDeltaContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return parentPage.configuresObjectDelta();
			}
		});
		add(objectDeltaContainer);
		objectDeltaContainer.add(new Label(ID_OBJECT_DELTA, new PropertyModel<>(taskDtoModel, TaskDto.F_OBJECT_DELTA)));

		WebMarkupContainer scriptContainer = new WebMarkupContainer(ID_SCRIPT_CONTAINER);
		scriptContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return parentPage.configuresScript();
			}
		});
		add(scriptContainer);
		scriptContainer.add(new Label(ID_SCRIPT, new PropertyModel<>(taskDtoModel, TaskDto.F_SCRIPT)));

		WebMarkupContainer optionsContainer = new WebMarkupContainer(ID_OPTIONS_CONTAINER);
		optionsContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return parentPage.configuresDryRun() || parentPage.configuresExecuteInRawMode();
			}
		});
		add(optionsContainer);

		WebMarkupContainer dryRunContainer = new WebMarkupContainer(ID_DRY_RUN_CONTAINER);
		dryRunContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return parentPage.configuresDryRun();
			}
		});
		optionsContainer.add(dryRunContainer);
		CheckBox dryRun = new CheckBox(ID_DRY_RUN, new PropertyModel<Boolean>(taskDtoModel, TaskDto.F_DRY_RUN));
		dryRun.add(enabledIfEdit);
		dryRunContainer.add(dryRun);

		WebMarkupContainer executeInRawModeContainer = new WebMarkupContainer(ID_EXECUTE_IN_RAW_MODE_CONTAINER);
		executeInRawModeContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return parentPage.configuresExecuteInRawMode();
			}
		});
		optionsContainer.add(executeInRawModeContainer);
		CheckBox executeInRawMode = new CheckBox(ID_EXECUTE_IN_RAW_MODE, new PropertyModel<Boolean>(taskDtoModel, TaskDto.F_EXECUTE_IN_RAW_MODE));
		executeInRawMode.add(enabledIfEdit);
		executeInRawModeContainer.add(executeInRawMode);
	}

	private TaskDto getTaskDto() {
		return parentPage.getTaskDto();
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

	@Override
	public Collection<Component> getComponentsToUpdate() {
		return Collections.<Component>singleton(this);
	}

}
