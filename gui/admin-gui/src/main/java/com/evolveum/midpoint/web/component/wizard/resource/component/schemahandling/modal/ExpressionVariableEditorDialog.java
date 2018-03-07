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

package com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.util.exception.CommonException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.input.ObjectReferenceChoiceRenderer;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ExpressionVariableDefinitionTypeDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionVariableDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;


/**
 * @author shood
 */
public class ExpressionVariableEditorDialog extends ModalWindow {

	private static final Trace LOGGER = TraceManager.getTrace(ExpressionVariableEditorDialog.class);

	private static final String DOT_CLASS = ExpressionVariableEditorDialog.class.getName() + ".";
	private static final String OPERATION_LOAD_REPOSITORY_OBJECTS = DOT_CLASS + "loadRepositoryObjects";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_NAME = "name";
	private static final String ID_DESCRIPTION = "description";
	private static final String ID_PATH = "path";
	private static final String ID_OBJECT_REFERENCE = "objectReference";
	private static final String ID_VALUE = "value";
	private static final String ID_BUTTON_SAVE = "saveButton";
	private static final String ID_BUTTON_CANCEL = "cancelButton";

	private static final String ID_LABEL_SIZE = "col-md-4";
	private static final String ID_INPUT_SIZE = "col-md-8";

	private boolean initialized;
	private IModel<ExpressionVariableDefinitionTypeDto> model;
	private IModel<ExpressionVariableDefinitionType> inputModel;
	private Map<String, String> objectMap = new HashMap<>();

	public ExpressionVariableEditorDialog(String id, final IModel<ExpressionVariableDefinitionType> variable) {
		super(id);

		inputModel = variable;
		model = new LoadableModel<ExpressionVariableDefinitionTypeDto>(false) {

			@Override
			protected ExpressionVariableDefinitionTypeDto load() {
				if (variable != null) {
					return new ExpressionVariableDefinitionTypeDto(variable.getObject());
				} else {
					return new ExpressionVariableDefinitionTypeDto(new ExpressionVariableDefinitionType());
				}
			}
		};

		setOutputMarkupId(true);
		setTitle(createStringResource("ExpressionVariableEditor.label"));
		showUnloadConfirmation(false);
		setCssClassName(ModalWindow.CSS_CLASS_GRAY);
		setCookieName(MappingEditorDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
		setInitialWidth(450);
		setInitialHeight(550);
		setWidthUnit("px");

		WebMarkupContainer content = new WebMarkupContainer(getContentId());
		content.setOutputMarkupId(true);
		setContent(content);
	}

	public void updateModel(AjaxRequestTarget target, ExpressionVariableDefinitionType variable) {
		model.setObject(new ExpressionVariableDefinitionTypeDto(variable));

		if (inputModel != null) {
			inputModel.setObject(variable);
		} else {
			inputModel = new Model<>(variable);
		}

		target.add(getContent());
	}

	public StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return PageBase.createStringResourceStatic(this, resourceKey, objects);
		// return new StringResourceModel(resourceKey, this, null, resourceKey,
		// objects);
	}

	@Override
	protected void onBeforeRender() {
		super.onBeforeRender();

		if (initialized) {
			return;
		}

		initLayout((WebMarkupContainer) get(getContentId()));
		initialized = true;
	}

	public void initLayout(WebMarkupContainer content) {
		Form form = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
		form.setOutputMarkupId(true);
		content.add(form);

		// TODO - shouldn't this be some AutoCompleteField? If yer, where do we
		// get value?
		TextFormGroup name = new TextFormGroup(ID_NAME,
            new PropertyModel<>(model, ExpressionVariableDefinitionTypeDto.F_VARIABLE + ".name.localPart"),
				createStringResource("ExpressionVariableEditor.label.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
		form.add(name);

		TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION,
            new PropertyModel<>(model, ExpressionVariableDefinitionTypeDto.F_VARIABLE + ".description"),
				createStringResource("ExpressionVariableEditor.label.description"), ID_LABEL_SIZE, ID_INPUT_SIZE,
				false);
		form.add(description);

		TextFormGroup path = new TextFormGroup(ID_PATH,
            new PropertyModel<>(model, ExpressionVariableDefinitionTypeDto.F_PATH),
				createStringResource("ExpressionVariableEditor.label.path"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
		form.add(path);

		DropDownFormGroup objectReference = new DropDownFormGroup<>(ID_OBJECT_REFERENCE,
            new PropertyModel<>(model,
                ExpressionVariableDefinitionTypeDto.F_VARIABLE + ".objectRef"),
				new AbstractReadOnlyModel<List<ObjectReferenceType>>() {

					@Override
					public List<ObjectReferenceType> getObject() {
						return WebModelServiceUtils.createObjectReferenceList(ObjectType.class, getPageBase(), objectMap);
					}
				}, new ObjectReferenceChoiceRenderer(objectMap),
				createStringResource("ExpressionVariableEditor.label.objectRef"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
		form.add(objectReference);

		TextAreaFormGroup value = new TextAreaFormGroup(ID_VALUE,
            new PropertyModel<>(model, ExpressionVariableDefinitionTypeDto.F_VALUE),
				createStringResource("ExpressionVariableEditor.label.value"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
		form.add(value);

		AjaxSubmitButton cancel = new AjaxSubmitButton(ID_BUTTON_CANCEL,
				createStringResource("ExpressionVariableEditor.button.cancel")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				cancelPerformed(target);
			}
		};
		form.add(cancel);

		AjaxSubmitButton save = new AjaxSubmitButton(ID_BUTTON_SAVE,
				createStringResource("ExpressionVariableEditor.button.apply")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(target);
			}
		};
		form.add(save);
	}

	private PageBase getPageBase() {
		return (PageBase) getPage();
	}

	private List<ObjectReferenceType> createObjectReferenceListDeprecated() {
		objectMap.clear();
		OperationResult result = new OperationResult(OPERATION_LOAD_REPOSITORY_OBJECTS);
		Task task = getPageBase().createSimpleTask(OPERATION_LOAD_REPOSITORY_OBJECTS);
		List<PrismObject<ObjectType>> objects = null;
		List<ObjectReferenceType> references = new ArrayList<>();

		try {
			objects = getPageBase().getModelService().searchObjects(ObjectType.class, new ObjectQuery(), null, task,
					result);
			result.recomputeStatus();
		} catch (CommonException|RuntimeException e) {
			result.recordFatalError("Couldn't load objects from repository.", e);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load objects from repository", e);
		}

		// TODO - show error somehow
		// if(!result.isSuccess()){
		// getPageBase().showResult(result);
		// }

		if (objects != null) {
			ObjectReferenceType ref;

			for (PrismObject<ObjectType> obj : objects) {
				objectMap.put(obj.getOid(), WebComponentUtil.getName(obj));
				ref = new ObjectReferenceType();
				ref.setOid(obj.getOid());
				references.add(ref);
			}
		}

		return references;
	}

	private String createReferenceDisplayString(String oid) {
		return objectMap.get(oid);
	}

	private void cancelPerformed(AjaxRequestTarget target) {
		close(target);
	}

	private void savePerformed(AjaxRequestTarget target) {
		if (model != null && model.getObject() != null) {
			model.getObject().prepareDtoToSave();
			inputModel.setObject(model.getObject().getVariableObject());
		}

		updateComponents(target);
		close(target);
	}

	public void updateComponents(AjaxRequestTarget target) {
		// Override this if update of component(s) holding this modal window is
		// needed
	}
}
