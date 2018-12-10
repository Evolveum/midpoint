/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.gui.impl.factory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.LockoutStatusPanel;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.ExpressionValuePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.input.TextAreaPanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.input.TriStateComboPanel;
import com.evolveum.midpoint.web.component.input.UploadDownloadPanel;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.model.delta.ModificationsPanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismValuePanel;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.LookupPropertyModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.util.DateValidator;
import com.evolveum.midpoint.web.util.ExpressionValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuditingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPoliciesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class PrismValuePanel2 extends BasePanel<ValueWrapper> {
	private static final long serialVersionUID = 1L;

	private static final String ID_FEEDBACK = "feedback";
	private static final String ID_INPUT = "input";
	private static final String ID_ADD_BUTTON = "addButton";
	private static final String ID_REMOVE_BUTTON = "removeButton";
	private static final String ID_VALUE_CONTAINER = "valueContainer";
	private static final String ID_BUTTON_CONTAINER = "buttonContainer";
	
	private static final String OBJECT_TYPE = "ObjectType";

	private static final Trace LOGGER = TraceManager.getTrace(PrismValuePanel.class);

	private IModel<String> labelModel;
	private Form form;
	private String valueCssClass;
	private String inputCssClass;
	private String buttonCssClass;
	private QName objectTypeValue = null;

	public PrismValuePanel2(String id, IModel<ValueWrapper> valueWrapperModel, IModel<String> labelModel, Form form,
			String valueCssClass, String inputCssClass, String buttonCssClass) {
		super(id, valueWrapperModel);
		Validate.notNull(valueWrapperModel, "Property value model must not be null.");
		this.labelModel = labelModel;
		this.form = form;
		this.valueCssClass = valueCssClass;
		this.inputCssClass = inputCssClass;
		this.buttonCssClass = buttonCssClass;
	}

	@Override
	protected void onInitialize(){
		super.onInitialize();
		initLayout();
	}

	private void initLayout() {
		// container
		WebMarkupContainer valueContainer = new WebMarkupContainer(ID_VALUE_CONTAINER);
		valueContainer.setOutputMarkupId(true);
		valueContainer.add(new AttributeModifier("class", valueCssClass));
		add(valueContainer);

		// feedback
		FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
		feedback.setOutputMarkupId(true);
		add(feedback);

		// input
		Panel input = createInputComponent(ID_INPUT, labelModel, form);
		input.add(new AttributeModifier("class", inputCssClass));
		//TODO should we set ComponentFeedbackMessageFilter for all types of input field?
		if (input instanceof InputPanel) {
			initAccessBehaviour((InputPanel) input);
			feedback.setFilter(new ComponentFeedbackMessageFilter(((InputPanel) input).getBaseFormComponent()));
		} else if (input instanceof LockoutStatusPanel ||
				input instanceof ValueChoosePanel) {
			feedback.setFilter(new ComponentFeedbackMessageFilter(input));
		} else if (input instanceof ExpressionValuePanel) {
			input.visitChildren(new IVisitor<Component, Object>() {
				@Override
				public void component(Component component, IVisit<Object> objectIVisit) {
					if (component instanceof FormComponent) {
						feedback.setFilter(new ComponentFeedbackMessageFilter(component));
					}
				}
			});

		}
		valueContainer.add(input);

		WebMarkupContainer buttonContainer = new WebMarkupContainer(ID_BUTTON_CONTAINER);
		buttonContainer.add(new AttributeModifier("class", buttonCssClass));
		valueContainer.add(buttonContainer);
		// buttons
		AjaxLink addButton = new AjaxLink(ID_ADD_BUTTON) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				addValue(target);
			}
		};
		addButton.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isAddButtonVisible();
			}
		});
		buttonContainer.add(addButton);

		AjaxLink removeButton = new AjaxLink(ID_REMOVE_BUTTON) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				removeValue(target);
			}
		};
		removeButton.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isRemoveButtonVisible();
			}
		});
		buttonContainer.add(removeButton);
	}

	private IModel<String> createHelpModel() {
		return new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				ItemWrapper wrapper = getModel().getObject().getItem();
				return wrapper.getItem().getHelp();
			}
		};
	}

	private boolean isAccessible(ItemDefinition def, ContainerStatus status) {
		if (def.getName().equals(ConstructionType.F_KIND) || def.getName().equals(ConstructionType.F_INTENT)){
			return false;
		}
		switch (status) {
			case ADDING:
				if (!def.canAdd()) {
					return false;
				}
				break;
			case MODIFYING:
				if (!def.canModify()) {
					return false;
				}
				break;
		}

		return true;
	}

	private void initAccessBehaviour(InputPanel panel) {
		List<FormComponent> components = panel.getFormComponents();
		for (FormComponent component : components) {
			component.add(new VisibleEnableBehaviour() {
				private static final long serialVersionUID = 1L;

				@Override
				public boolean isEnabled() {
					ValueWrapper wrapper = getModel().getObject();
					ItemWrapper itemWrapper = wrapper.getItem();
					if (wrapper.isReadonly()) {
						return false;
					}
					// if (itemWrapper.getParent() == null) {
					// return true; // TODO
					// }
					ContainerWrapper object = itemWrapper.getParent();
					ItemDefinition def = itemWrapper.getItem().getDefinition();

					return object == null || isAccessible(def, object.getStatus());
				}

				@Override
				public void onComponentTag(Component component, ComponentTag tag) {
					if (component instanceof TextField && !isEnabled()) {
						tag.remove("disabled");
						tag.append("class", "input-readonly", " ");
						tag.append("readonly", "readonly", " ");
					}
				}
			});
		}
	}

	private int countUsableValues(PropertyOrReferenceWrapper<? extends Item, ? extends ItemDefinition> property) {
		int count = 0;
		for (ValueWrapper value : property.getValues()) {
			value.normalize(property.getItemDefinition().getPrismContext());

			if (ValueStatus.DELETED.equals(value.getStatus())) {
				continue;
			}

			if (ValueStatus.ADDED.equals(value.getStatus()) && !value.hasValueChanged()) {
				continue;
			}

			count++;
		}
		return count;
	}

	private List<ValueWrapper> getUsableValues(PropertyOrReferenceWrapper<? extends Item, ? extends ItemDefinition> property) {
		List<ValueWrapper> values = new ArrayList<>();
		for (ValueWrapper value : property.getValues()) {
			value.normalize(property.getItemDefinition().getPrismContext());
			if (ValueStatus.DELETED.equals(value.getStatus())) {
				continue;
			}
			values.add(value);
		}

		return values;
	}

	private int countNonDeletedValues(PropertyOrReferenceWrapper<? extends Item, ? extends ItemDefinition> property) {
		int count = 0;
		for (ValueWrapper value : property.getValues()) {
			value.normalize(property.getItemDefinition().getPrismContext());
			if (ValueStatus.DELETED.equals(value.getStatus())) {
				continue;
			}
			count++;
		}
		return count;
	}

	private boolean hasEmptyPlaceholder(PropertyOrReferenceWrapper<? extends Item, ? extends ItemDefinition> property) {
		for (ValueWrapper value : property.getValues()) {
			value.normalize(property.getItemDefinition().getPrismContext());
			if (ValueStatus.ADDED.equals(value.getStatus()) && !value.hasValueChanged()) {
				return true;
			}
		}

		return false;
	}

	private boolean isRemoveButtonVisible() {
		ValueWrapper valueWrapper = getModelObject();

		if (valueWrapper.isReadonly()) {
			return false;
		}
		Component inputPanel = this.get(ID_VALUE_CONTAINER).get(ID_INPUT);
		if (inputPanel instanceof ValueChoosePanel) {
			return true;
		}
		
		return true;

	}

	private ContainerStatus getContainerStatus(ItemWrapper propertyWrapper) {
		final ContainerWrapper<Containerable> objectWrapper = propertyWrapper.getParent();
		return objectWrapper != null ? objectWrapper.getStatus() : ContainerStatus.MODIFYING;
	}

	private boolean isAddButtonVisible() {
		Component inputPanel = this.get(ID_VALUE_CONTAINER).get(ID_INPUT);
		ValueWrapper valueWrapper = getModelObject();

		if (valueWrapper.isReadonly()) {
			return false;
		}

		PropertyOrReferenceWrapper propertyWrapper = (PropertyOrReferenceWrapper) valueWrapper.getItem();
		Item property = propertyWrapper.getItem();

		ItemDefinition definition = property.getDefinition();
		int max = definition.getMaxOccurs();
		List<ValueWrapper> usableValues = getUsableValues(propertyWrapper);
		if (usableValues.indexOf(valueWrapper) != usableValues.size() - 1) {
			return false;
		}

		if (max == -1) {
			return true;
		}

		if (countNonDeletedValues(propertyWrapper) >= max) {
			return false;
		}

		if (propertyWrapper.getParent() == null) {
			return true; // TODO
		}
		return isAccessible(definition, getContainerStatus(propertyWrapper));
	}

	
	
	private <T> Panel createInputComponent(String id, IModel<String> labelModel, Form form) {
		ValueWrapper valueWrapper = getModelObject();
		ContainerWrapper objectWrapper = null;
		if (valueWrapper.getItem().getParent() != null) {
			objectWrapper = valueWrapper.getItem().getParent();
		}
		Item property = valueWrapper.getItem().getItem();
		ItemDefinition definition = valueWrapper.getItem().getItemDefinition();
		boolean required = definition.getMinOccurs() > 0;
		boolean enforceRequiredFields = valueWrapper.getItem().isEnforceRequiredFields();
		LOGGER.trace("createInputComponent: id={}, required={}, enforceRequiredFields={}, definition={}", id, required,
				enforceRequiredFields, definition);

		final String baseExpression = "value.value"; 
		
		Panel component = null;
		GuiComponentFactory componentFactory = getPageBase().getRegistry().findFactory(valueWrapper);
		if (componentFactory != null) {
			
			PanelContext<T> panelCtx = new PanelContext<>();
			panelCtx.setBaseExpression(getBaseExpression(definition));
			panelCtx.setBaseModel((IModel) getModel());
			panelCtx.setPageBase(getPageBase());
			panelCtx.setItemDefinition(definition);
			panelCtx.setComponentId(id);
			panelCtx.setParentComponent(this);
			
			try {
			component = componentFactory.createPanel(panelCtx);
			} catch (Throwable e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Cannot create panel", e);
				getSession().error("Cannot create panel");
				throw new RuntimeException(e);
			}
		
//			component = componentFactory.createPanel();
		} 
		
//		else {
//			component = createTypedInputComponent(id);
//		}

		if (component instanceof InputPanel) {
			InputPanel inputPanel = (InputPanel) component;
			// adding valid from/to date range validator, if necessary
			ItemPath activation = new ItemPath(UserType.F_ACTIVATION);
			if (ActivationType.F_VALID_FROM.equals(property.getElementName())) {
				DateValidator validator = WebComponentUtil.getRangeValidator(form, activation);
				validator.setDateFrom((DateTimeField) inputPanel.getBaseFormComponent());
			} else if (ActivationType.F_VALID_TO.equals(property.getElementName())) {
				DateValidator validator = WebComponentUtil.getRangeValidator(form, activation);
				validator.setDateTo((DateTimeField) inputPanel.getBaseFormComponent());
			} else if (valueWrapper.getItem().getFormItemValidator() != null) {
				ExpressionValidator<T> expressionValidator = new ExpressionValidator<T>(valueWrapper.getItem().getFormItemValidator(), getPageBase()) {
					
					@Override
					protected <O extends ObjectType> O getObjectType() {
						return getObject(valueWrapper);
					}
				};
				inputPanel.getBaseFormComponent().add(expressionValidator);
//				form.add(expressionValidator);
			}

			final List<FormComponent> formComponents = inputPanel.getFormComponents();
			for (FormComponent formComponent : formComponents) {
				formComponent.setLabel(labelModel);
				formComponent.setRequired(required && enforceRequiredFields);

				if (formComponent instanceof TextField) {
					formComponent.add(new AttributeModifier("size", "42"));
				}
				formComponent.add(new AjaxFormComponentUpdatingBehavior("blur") {

					@Override
					protected void onUpdate(AjaxRequestTarget target) {
						target.add(getPageBase().getFeedbackPanel());
						target.add(get(ID_FEEDBACK));
					}
					
					@Override
					protected void onError(AjaxRequestTarget target, RuntimeException e) {
						target.add(getPageBase().getFeedbackPanel());
						target.add(get(ID_FEEDBACK));
					}
					
				});
			}
		}
		if (component == null) {
			throw new RuntimeException(
					"Cannot create input component for item " + property + " (" + valueWrapper + ") in " + objectWrapper);
		}
		return component;
	}
	
	private <O extends ObjectType, C extends Containerable> O getObject(ValueWrapper valueWrapper) {
		ItemWrapper itemWrapper = valueWrapper.getItem();
		if (itemWrapper == null) {
			return null;
		}
		
		ContainerWrapper<C> cWrapper = itemWrapper.getParent();
		if (cWrapper == null) {
			return null;
		}
		
		ObjectWrapper<O> objectWrapper = cWrapper.getObjectWrapper();
		PrismObject<O> newObject = objectWrapper.getObject().clone();
		
		try {
			ObjectDelta<O> objectDelta = objectWrapper.getObjectDelta();
			if (objectDelta.isModify()) {
				objectDelta.applyTo(newObject);
			} else if (objectDelta.isAdd()) {
				newObject = objectDelta.getObjectToAdd().clone();
			} else if (objectDelta.isDelete()) {
				newObject = null;
			}
		} catch (SchemaException e) {
			return null;
		}
		
		return newObject.asObjectable();
		
	}

	private String getBaseExpression(ItemDefinition def) {
		if (def instanceof PrismReferenceDefinition) {
			return "value.referencable";
		}
		
		return "value.value";
	}
	
	// normally this method returns an InputPanel;
	// however, for some special readonly types (like ObjectDeltaType) it will
	// return a Panel
//	protected Panel createTypedInputComponent(String id) {
//		final Item item = getModelObject().getItem().getItem();
//
//		Panel panel = null;
//		if (item instanceof PrismProperty) {
//			final PrismProperty property = (PrismProperty) item;
//			PrismPropertyDefinition definition = property.getDefinition();
//			final QName valueType = definition.getTypeName();
//
//			// pointing to prism property real value
//			final String baseExpression = "value.value"; 
//			
//		return panel;
//	}


	private void addValue(AjaxRequestTarget target) {
		Component inputPanel = this.get(ID_VALUE_CONTAINER).get(ID_INPUT);
		ValueWrapper wrapper = getModel().getObject();
		ItemWrapper propertyWrapper = wrapper.getItem();
		LOGGER.debug("Adding value of {}", propertyWrapper);
		propertyWrapper.addValue(true);
		ListView parent = findParent(ListView.class);
		target.add(parent.getParent());
	}

	private void removeValue(AjaxRequestTarget target) {
		ValueWrapper wrapper = getModel().getObject();
		PropertyOrReferenceWrapper propertyWrapper = (PropertyOrReferenceWrapper) wrapper.getItem();
		LOGGER.debug("Removing value of {}", propertyWrapper);

		List<ValueWrapper> values = propertyWrapper.getValues();
		Component inputPanel = this.get(ID_VALUE_CONTAINER).get(ID_INPUT);

		switch (wrapper.getStatus()) {
			case ADDED:
				values.remove(wrapper);
				break;
			case DELETED:
				error("Couldn't delete already deleted item: " + wrapper.toString());
				target.add(((PageBase) getPage()).getFeedbackPanel());
			case NOT_CHANGED:
				wrapper.setStatus(ValueStatus.DELETED);
				break;
		}

		int count = countUsableValues(propertyWrapper);
		if (count == 0 && !hasEmptyPlaceholder(propertyWrapper)) {
			if (inputPanel instanceof ValueChoosePanel) {
				values.add(new ValueWrapper(propertyWrapper, new PrismReferenceValue(null), ValueStatus.ADDED));
			} else {
				values.add(new ValueWrapper(propertyWrapper, new PrismPropertyValue(null), ValueStatus.ADDED));
			}
		}
		ListView parent = findParent(ListView.class);
		target.add(parent.getParent());
	}


}
