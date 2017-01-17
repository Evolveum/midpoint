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

import org.apache.wicket.RestartResponseException;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class DynamicFormPanel extends BasePanel {

	private static final long serialVersionUID = 1L;

	private static final transient Trace LOGGER = TraceManager.getTrace(DynamicFormPanel.class);

	private static final String DOT_CLASS = DynamicFormPanel.class.getName() + ".";
	private static final String OPERATION_LOAD_FORM = DOT_CLASS + "loadForm";

	private static final String ID_FORM_FIELDS = "formFields";

	public DynamicFormPanel(String id, String formOid, Form mainForm, boolean runPrivileged,
			PageBase parentPage) {
		super(id);
		setParent(parentPage);

		initLayout(formOid, runPrivileged, mainForm);
	}

	private void initLayout(String formOid, boolean runPrivileged, Form mainForm) {

		// String formOid = "";
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

//		List<AbstractFormItemType> formItems = getFormItems(formDefinitionType);

		PrismObjectDefinition<UserType> objectDefinition = getPageBase().getPrismContext().getSchemaRegistry()
				.findObjectDefinitionByType(UserType.COMPLEX_TYPE);
		PrismObject<UserType> prismObject;
		try {
			prismObject = objectDefinition.instantiate();
		} catch (SchemaException e) {
			throw new RestartResponseException(getPageBase());
		}

		ObjectWrapperFactory owf = new ObjectWrapperFactory(getPageBase());
		ObjectWrapper<UserType> objectWrapper = owf.createObjectWrapper("DisplayName", "description",
				prismObject, ContainerStatus.ADDING);
		objectWrapper.setShowEmpty(true);
		
		DynamicFieldGroupPanel formFields = new DynamicFieldGroupPanel(ID_FORM_FIELDS, objectWrapper, formDefinitionType, mainForm, getPageBase());
		formFields.setOutputMarkupId(true);
		add(formFields);
		
//		RepeatingView itemView = new RepeatingView(ID_PROPERTY);
//		add(itemView);
//		for (AbstractFormItemType formItem : formItems) {
//
//			if (!(formItem instanceof FormFieldType)) {
//				continue;
//			}
//
//			FormFieldType formField = (FormFieldType) formItem;
//
//			ItemWrapper itemWrapper = createItemWrapper(formItem, objectWrapper);
//
//			applyFormDefinition(itemWrapper, formItem);
//
//			if (itemWrapper instanceof ContainerWrapper) {
//				PrismContainerPanel containerPanel = new PrismContainerPanel(itemView.newChildId(),
//						Model.of((ContainerWrapper)itemWrapper), true, mainForm, getPageBase());
//				itemView.add(containerPanel);
//
//			} else {
//			
//				PrismPropertyPanel propertyPanel = new PrismPropertyPanel(itemView.newChildId(),
//						Model.of(itemWrapper), mainForm, getPageBase());
//				itemView.add(propertyPanel);
//			}
//
//		}

	}

//	private ItemWrapper createItemWrapper(AbstractFormItemType formField, ObjectWrapper objectWrapper) {
//		ItemPathType itemPathType = formField.getRef();
//
//		if (itemPathType == null) {
//			getSession().error("Bad form item definition. It has to contain reference to the real attribute");
//			LOGGER.error("Bad form item definition. It has to contain reference to the real attribute");
//			throw new RestartResponseException(getPageBase());
//		}
//
//		ItemPath path = itemPathType.getItemPath();
//
//		ItemWrapper itemWrapper = null;
//		Item item = objectWrapper.getObject().findItem(path);
//		if (item instanceof PrismContainer) {
//			itemWrapper = objectWrapper.findContainerWrapper(path);
//		} else {
//			itemWrapper = objectWrapper.findPropertyWrapper(path);
//		}
//
//		if (itemWrapper == null) {
//			getSession().error("Bad form item definition. No attribute with path: " + path + " was found");
//			LOGGER.error("Bad form item definition. No attribute with path: " + path + " was found");
//			throw new RestartResponseException(getPageBase());
//		}
//
//		return itemWrapper;
//
//	}
//
//	private void applyFormDefinition(ItemWrapper itemWrapper, AbstractFormItemType formField) {
//
//		FormItemDisplayType displayType = formField.getDisplay();
//
//		if (displayType == null) {
//			return;
//		}
//
//		ItemDefinitionImpl itemDef = (ItemDefinitionImpl) itemWrapper.getItemDefinition();
//		if (StringUtils.isNotEmpty(displayType.getLabel())) {
//			itemDef.setDisplayName(displayType.getLabel());
//		}
//
//		if (StringUtils.isNotEmpty(displayType.getHelp())) {
//			itemDef.setHelp(displayType.getHelp());
//		}
//
//		if (StringUtils.isNotEmpty(displayType.getMaxOccurs())) {
//			itemDef.setMaxOccurs(XsdTypeMapper.multiplicityToInteger(displayType.getMaxOccurs()));
//		}
//
//		if (StringUtils.isNotEmpty(displayType.getMinOccurs())) {
//			itemDef.setMinOccurs(XsdTypeMapper.multiplicityToInteger(displayType.getMinOccurs()));
//		}
//
//	}

//	private List<AbstractFormItemType> getFormItems(FormDefinitionType formDefinitionType) {
//		List<AbstractFormItemType> items = new ArrayList<>();
//		List<JAXBElement<? extends AbstractFormItemType>> formItems = formDefinitionType.getFormItem();
//		for (JAXBElement<? extends AbstractFormItemType> formItem : formItems) {
//			AbstractFormItemType item = formItem.getValue();
//			items.add(item);
//		}
//		return items;
//	}

}
