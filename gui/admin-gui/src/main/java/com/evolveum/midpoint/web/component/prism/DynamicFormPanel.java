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
package com.evolveum.midpoint.web.component.prism;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class DynamicFormPanel<O extends ObjectType> extends BasePanel<ObjectWrapper<O>> {

	private static final long serialVersionUID = 1L;

	private static final transient Trace LOGGER = TraceManager.getTrace(DynamicFormPanel.class);

	private static final String DOT_CLASS = DynamicFormPanel.class.getName() + ".";
	private static final String OPERATION_LOAD_FORM = DOT_CLASS + "loadForm";

	private static final String ID_FORM_FIELDS = "formFields";

	LoadableModel<ObjectWrapper<O>> wrapperModel = null;
	
	public DynamicFormPanel(String id, final IModel<O> model, String formOid, Form<?> mainForm, boolean runPrivileged,
			final PageBase parentPage) {
		
		super(id);
		
		wrapperModel = new LoadableModel<ObjectWrapper<O>>() {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected ObjectWrapper<O> load() {
				ObjectWrapperFactory owf = new ObjectWrapperFactory(parentPage);
				ObjectWrapper<O> objectWrapper = owf.createObjectWrapper("DisplayName", "description",
						model.getObject().asPrismObject(), (model.getObject().getOid() == null) ? ContainerStatus.ADDING : ContainerStatus.MODIFYING);
				objectWrapper.setShowEmpty(true);
				return objectWrapper;
			}
		};
		
		
		setParent(parentPage);

		initLayout(formOid, runPrivileged, mainForm);
	}
	
	@Override
	public IModel<ObjectWrapper<O>> getModel() {
		return wrapperModel;
	}

	private void initLayout(String formOid, boolean runPrivileged, Form<?> mainForm) {

		Task task = null;
		if (runPrivileged) {
			task = getPageBase().createAnonymousTask(OPERATION_LOAD_FORM);
		} else {
			task = getPageBase().createSimpleTask(OPERATION_LOAD_FORM);
		}
		OperationResult result = new OperationResult(OPERATION_LOAD_FORM);
		PrismObject<FormType> prismForm = WebModelServiceUtils.loadObject(FormType.class, formOid,
				getPageBase(), task, result);

		if (prismForm == null) {
			LOGGER.trace("No form defined, skipping denerating GUI form");
			return;
		}

		FormType formType = prismForm.asObjectable();

		FormDefinitionType formDefinitionType = formType.getFormDefinition();
		if (formDefinitionType == null) {
			LOGGER.trace("No form definition defined for dynamic form");
		}

		DynamicFieldGroupPanel<O> formFields = new DynamicFieldGroupPanel<O>(ID_FORM_FIELDS, getModel(), formDefinitionType, mainForm, getPageBase());
		formFields.setOutputMarkupId(true);
		add(formFields);

	}
	
	public ObjectDelta<O> getObjectDelta() throws SchemaException {
		return wrapperModel.getObject().getObjectDelta();
	}
	
	public PrismObject<O> getObject() throws SchemaException {
		ObjectDelta<O> delta = wrapperModel.getObject().getObjectDelta();
		if (delta != null && delta.isAdd()) {
			return delta.getObjectToAdd();
		}
		return wrapperModel.getObject().getObject();
	}


}
