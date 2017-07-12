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
package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.ChoiceableChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskAddResourcesDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.ResourceRelatedHandlerDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
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
public class ResourceRelatedHandlerPanel<D extends ResourceRelatedHandlerDto> extends BasePanel<D> {
	private static final long serialVersionUID = 1L;

	private static final String DOT_CLASS = ResourceRelatedHandlerPanel.class.getName() + ".";
	private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "createResourceList";
	private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

	private static final Trace LOGGER = TraceManager.getTrace(ResourceRelatedHandlerPanel.class);

	private static final String ID_RESOURCE_REF_CONTAINER = "resourceRefContainer";
	private static final String ID_RESOURCE_REF = "resourceRef";
	private static final String ID_KIND_CONTAINER = "kindContainer";
	private static final String ID_KIND = "kind";
	private static final String ID_INTENT_CONTAINER = "intentContainer";
	private static final String ID_INTENT = "intent";
	private static final String ID_OBJECT_CLASS_CONTAINER = "objectClassContainer";
	private static final String ID_OBJECT_CLASS = "objectClass";
	private static final String ID_OPTIONS_CONTAINER = "optionsContainer";
	private static final String ID_DRY_RUN_CONTAINER = "dryRunContainer";
	private static final String ID_DRY_RUN = "dryRun";

	private PageTaskEdit parentPage;
        protected VisibleEnableBehaviour enabledIfEdit;

	public ResourceRelatedHandlerPanel(String id, IModel<D> handlerDtoModel, PageTaskEdit parentPage) {
		super(id, handlerDtoModel);
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
		enabledIfEdit = new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
				return parentPage.isEdit();
			}
		};
		final VisibleEnableBehaviour visibleForResourceCoordinates = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getTaskDto().configuresResourceCoordinates();
			}
		};

		final WebMarkupContainer resourceRefContainer = new WebMarkupContainer(ID_RESOURCE_REF_CONTAINER);
		resourceRefContainer.add(visibleForResourceCoordinates);
		resourceRefContainer.setOutputMarkupId(true);
		add(resourceRefContainer);

		final DropDownChoice<TaskAddResourcesDto> resourceRef = new DropDownChoice<>(ID_RESOURCE_REF,
				new PropertyModel<TaskAddResourcesDto>(getModel(), ResourceRelatedHandlerDto.F_RESOURCE_REFERENCE),
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

				TaskAddResourcesDto resourcesDto = getModelObject().getResourceRef();

				if(resourcesDto != null){
					PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(ResourceType.class,
							resourcesDto.getOid(), parentPage, task, result);

					try {
						ResourceSchema schema = RefinedResourceSchemaImpl.getResourceSchema(resource, parentPage.getPrismContext());
						schema.getObjectClassDefinitions();

						for(Definition def: schema.getDefinitions()){
							objectClassList.add(def.getTypeName());
						}

						getModelObject().setObjectClassList(objectClassList);
					} catch (Exception e){
						LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object class list from resource.", e);
						error("Couldn't load object class list from resource.");
					}

				}
				target.add(resourceRefContainer);
			}
		});
		resourceRefContainer.add(resourceRef);

		WebMarkupContainer kindContainer = new WebMarkupContainer(ID_KIND_CONTAINER);
		kindContainer.add(visibleForResourceCoordinates);
		add(kindContainer);

		final DropDownChoice kind = new DropDownChoice<>(ID_KIND,
				new PropertyModel<ShadowKindType>(getModel(), ResourceRelatedHandlerDto.F_KIND),
				WebComponentUtil.createReadonlyModelFromEnum(ShadowKindType.class), new EnumChoiceRenderer<ShadowKindType>());
		kind.setOutputMarkupId(true);
		kind.setNullValid(true);
		kindContainer.add(kind);

		WebMarkupContainer intentContainer = new WebMarkupContainer(ID_INTENT_CONTAINER);
		intentContainer.add(visibleForResourceCoordinates);
		add(intentContainer);

		final TextField<String> intent = new TextField<>(ID_INTENT, new PropertyModel<String>(getModel(), ResourceRelatedHandlerDto.F_INTENT));
		intentContainer.add(intent);
		intent.setOutputMarkupId(true);
		intent.add(enabledIfEdit);

		WebMarkupContainer objectClassContainer = new WebMarkupContainer(ID_OBJECT_CLASS_CONTAINER);
		objectClassContainer.add(visibleForResourceCoordinates);
		add(objectClassContainer);

		AutoCompleteSettings autoCompleteSettings = new AutoCompleteSettings();
		autoCompleteSettings.setShowListOnEmptyInput(true);
		final AutoCompleteTextField<String> objectClass = new AutoCompleteTextField<String>(ID_OBJECT_CLASS,
				new PropertyModel<String>(getModel(), ResourceRelatedHandlerDto.F_OBJECT_CLASS), autoCompleteSettings) {

			@Override
			protected Iterator<String> getChoices(String input) {

				return prepareObjectClassChoiceList(input);
			}
		};
		objectClass.add(enabledIfEdit);
		objectClassContainer.add(objectClass);

		WebMarkupContainer optionsContainer = new WebMarkupContainer(ID_OPTIONS_CONTAINER);
		optionsContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getTaskDto().configuresDryRun();
			}
		});
		add(optionsContainer);

		WebMarkupContainer dryRunContainer = new WebMarkupContainer(ID_DRY_RUN_CONTAINER);
		dryRunContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getTaskDto().configuresDryRun();
			}
		});
		optionsContainer.add(dryRunContainer);
		CheckBox dryRun = new CheckBox(ID_DRY_RUN, new PropertyModel<Boolean>(getModel(), ResourceRelatedHandlerDto.F_DRY_RUN));
		dryRun.add(enabledIfEdit);
		dryRunContainer.add(dryRun);
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
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get resource list", ex);
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

		if(getModelObject().getResourceRef() == null){
			return choices.iterator();
		}

		if(Strings.isEmpty(input)){
			for(QName q: getModelObject().getObjectClassList()){
				choices.add(q.getLocalPart());
				Collections.sort(choices);
			}
		} else {
			for(QName q: getModelObject().getObjectClassList()){
				if(q.getLocalPart().startsWith(input)){
					choices.add(q.getLocalPart());
				}
				Collections.sort(choices);
			}
		}

		return choices.iterator();
	}

}
