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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FormTypeUtil;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

public class DynamicFormPanel<O extends ObjectType> extends BasePanel<ObjectWrapper<O>> {

	private static final long serialVersionUID = 1L;

	private static final transient Trace LOGGER = TraceManager.getTrace(DynamicFormPanel.class);

	private static final String DOT_CLASS = DynamicFormPanel.class.getName() + ".";

	private static final String ID_FORM_FIELDS = "formFields";

	private LoadableModel<ObjectWrapper<O>> wrapperModel;
	private FormType form;

	public DynamicFormPanel(String id, final IModel<O> model, String formOid, Form<?> mainForm,
			Task task, final PageBase parentPage, boolean enforceRequiredFields) {
		this(id, (PrismObject<O>) model.getObject().asPrismObject(), formOid, mainForm, task, parentPage, enforceRequiredFields);
	}

	public DynamicFormPanel(String id, final PrismObject<O> prismObject, String formOid, Form<?> mainForm,
			Task task, final PageBase parentPage, boolean enforceRequiredFields) {
		super(id);
		initialize(prismObject, formOid, mainForm, task, parentPage, enforceRequiredFields);
	}

	public DynamicFormPanel(String id, final QName objectType, String formOid, Form<?> mainForm,
			Task task, final PageBase parentPage, boolean enforceRequiredFields) {
		super(id);
		PrismObject<O> prismObject = instantiateObject(objectType, parentPage);
		initialize(prismObject, formOid, mainForm, task, parentPage, enforceRequiredFields);
	}

	private PrismObject<O> instantiateObject(QName objectType, PageBase parentPage) {
		PrismObjectDefinition<O> objectDef = parentPage.getPrismContext().getSchemaRegistry()
				.findObjectDefinitionByType(objectType);
		PrismObject<O> prismObject;
		try {
			prismObject = objectDef.instantiate();
		} catch (SchemaException e) {
			LoggingUtils.logException(LOGGER, "Could not initialize model for forgot password", e);
			throw new RestartResponseException(parentPage);
		}
		return prismObject;
	}

	private void initialize(final PrismObject<O> prismObject, String formOid, Form<?> mainForm,
			final Task task, final PageBase parentPage, boolean enforceRequiredFields) {

		if (prismObject == null) {
			getSession().error(getString("DynamicFormPanel.object.must.not.be.null"));
			throw new RestartResponseException(parentPage);
		}

//		setParent(parentPage);
		form = loadForm(formOid, task, parentPage);
		if (form == null || form.getFormDefinition() == null) {
			LOGGER.debug("No form or form definition; form OID = {}", formOid);
			add(new Label(ID_FORM_FIELDS));			// to avoid wicket exceptions
			return;
		}

		final ObjectWrapperFactory owf = new ObjectWrapperFactory(parentPage);
		ObjectWrapper<O> objectWrapper = createObjectWrapper(owf, task, prismObject, enforceRequiredFields);
		wrapperModel = LoadableModel.create(() -> objectWrapper, true);
		initLayout(mainForm, parentPage);
	}

	private ObjectWrapper<O> createObjectWrapper(ObjectWrapperFactory owf, Task task, PrismObject<O> prismObject,
			boolean enforceRequiredFields) {
		FormAuthorizationType formAuthorization = form.getFormDefinition().getAuthorization();
		AuthorizationPhaseType authorizationPhase = formAuthorization != null && formAuthorization.getPhase() != null
				? formAuthorization.getPhase()
				: AuthorizationPhaseType.REQUEST;
		ObjectWrapper<O> objectWrapper = owf.createObjectWrapper("DisplayName", "description",
				prismObject, prismObject.getOid() == null ? ContainerStatus.ADDING : ContainerStatus.MODIFYING,
				authorizationPhase, task);
		objectWrapper.setShowEmpty(true);
		objectWrapper.setEnforceRequiredFields(enforceRequiredFields);
		return objectWrapper;
	}

	@Override
	public IModel<ObjectWrapper<O>> getModel() {
		return wrapperModel;
	}

	private void initLayout(Form<?> mainForm, PageBase parenPage) {
		DynamicFieldGroupPanel<O> formFields = new DynamicFieldGroupPanel<>(ID_FORM_FIELDS, getModel(),
				form.getFormDefinition(), mainForm, parenPage);
		formFields.setOutputMarkupId(true);
		add(formFields);
	}

	private FormType loadForm(String formOid, Task task, PageBase parentPage) {
		OperationResult result = new OperationResult("some some operation");
		return asObjectable(WebModelServiceUtils.loadObject(FormType.class, formOid, null, false,
				parentPage, task, result));
	}

	public ObjectDelta<O> getObjectDelta() throws SchemaException {
		return wrapperModel.getObject().getObjectDelta();
	}

	public boolean checkRequiredFields(PageBase pageBase) {
		return getFormFields().checkRequiredFields(pageBase);
	}

	@SuppressWarnings("unchecked")
	public DynamicFieldGroupPanel<O> getFormFields() {
		return (DynamicFieldGroupPanel<O>) get(ID_FORM_FIELDS);
	}

	public PrismObject<O> getObject() throws SchemaException {
		ObjectDelta<O> delta = wrapperModel.getObject().getObjectDelta();
		if (delta != null && delta.isAdd()) {
			return delta.getObjectToAdd();
		}
		return wrapperModel.getObject().getObject();
	}

	public List<ItemPath> getChangedItems() {
		DynamicFieldGroupPanel<O> formFields = (DynamicFieldGroupPanel<O>) get(ID_FORM_FIELDS);
		List<AbstractFormItemType> items = formFields.getFormItems();
		List<ItemPath> paths = new ArrayList<>();
		collectItemPaths(items, paths);
		return paths;
	}

	private void collectItemPaths(List<AbstractFormItemType> items, List<ItemPath> paths) {
		for (AbstractFormItemType aItem : items) {
			ItemPath itemPath = GuiImplUtil.getItemPath(aItem);
			if (itemPath != null) {
				paths.add(itemPath);
			}
			if (aItem instanceof FormFieldGroupType) {
				collectItemPaths(FormTypeUtil.getFormItems(((FormFieldGroupType) aItem).getFormItems()), paths);
			}
		}
	}
}
