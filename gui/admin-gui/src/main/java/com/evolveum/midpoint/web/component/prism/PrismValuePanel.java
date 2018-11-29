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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.input.*;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.login.PageSelfRegistration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
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

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
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
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.model.delta.ModificationsPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.LookupPropertyModel;
import com.evolveum.midpoint.web.util.DateValidator;
import com.evolveum.midpoint.web.util.ExpressionValidator;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

/**
 * @author lazyman
 */
public class PrismValuePanel extends BasePanel<ValueWrapper> {
	private static final long serialVersionUID = 1L;

	private static final String ID_FEEDBACK = "feedback";
	private static final String ID_INPUT = "input";
	private static final String ID_ADD_BUTTON = "addButton";
	private static final String ID_REMOVE_BUTTON = "removeButton";
	private static final String ID_VALUE_CONTAINER = "valueContainer";
	
	private static final String OBJECT_TYPE = "ObjectType";

	private static final Trace LOGGER = TraceManager.getTrace(PrismValuePanel.class);

	private IModel<String> labelModel;
	private Form form;
	private String valueCssClass;
	private String inputCssClass;
	private QName objectTypeValue = null;

	public PrismValuePanel(String id, IModel<ValueWrapper> valueWrapperModel, IModel<String> labelModel, Form form,
			String valueCssClass, String inputCssClass) {
		super(id, valueWrapperModel);
		Validate.notNull(valueWrapperModel, "Property value model must not be null.");
		this.labelModel = labelModel;
		this.form = form;
		this.valueCssClass = valueCssClass;
		this.inputCssClass = inputCssClass;
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
		} else if (input instanceof ExpressionValuePanel || input instanceof QNameEditorPanel){
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
		valueContainer.add(addButton);

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
		valueContainer.add(removeButton);
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

		PropertyOrReferenceWrapper propertyWrapper = (PropertyOrReferenceWrapper) valueWrapper.getItem();
		ItemDefinition definition = propertyWrapper.getItem().getDefinition();
		int min = definition.getMinOccurs();

		int count = countNonDeletedValues(propertyWrapper);
		if (count <= 1 || count <= min) {
			return false;
		}

		if (propertyWrapper.getParent() == null) {
			return true; // TODO
		}

		return isAccessible(definition, getContainerStatus(propertyWrapper));
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

		Panel component = createTypedInputComponent(id);

		if (component instanceof InputPanel) {
			InputPanel inputPanel = (InputPanel) component;
			// adding valid from/to date range validator, if necessary
			ItemPath activation = UserType.F_ACTIVATION;
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

	// normally this method returns an InputPanel;
	// however, for some special readonly types (like ObjectDeltaType) it will
	// return a Panel
	protected Panel createTypedInputComponent(String id) {
		final Item item = getModelObject().getItem().getItem();

		Panel panel = null;
		if (item instanceof PrismProperty) {
			final PrismProperty property = (PrismProperty) item;
			PrismPropertyDefinition definition = property.getDefinition();
			final QName valueType = definition.getTypeName();

			// pointing to prism property real value
			final String baseExpression = "value.value"; 

			// fixing MID-1230, will be improved with some kind of annotation or
			// something like that
			// now it works only in description
			if (ObjectType.F_DESCRIPTION.equals(definition.getName())) {
				return new TextAreaPanel(id, new PropertyModel(getModel(), baseExpression), null);
			}

			if (ActivationType.F_LOCKOUT_STATUS.equals(definition.getName())) {
				return new LockoutStatusPanel(id, getModel().getObject(),
                    new PropertyModel<>(getModel(), baseExpression));
			}
			
			if (SearchFilterType.COMPLEX_TYPE.equals(definition.getTypeName())) {
				return new AceEditorPanel(id, null, new IModel<String>() {
				
					@Override
					public void setObject(String object) {
						
						if (StringUtils.isBlank(object)) {
							return;
						}
						
						try {
							SearchFilterType filter = getPageBase().getPrismContext().parserFor(object).parseRealValue(SearchFilterType.class);
							((PrismPropertyValue<SearchFilterType>) PrismValuePanel.this.getModelObject().getValue()).setValue(filter);
						} catch (SchemaException e) {
							// TODO handle!!!!
							LoggingUtils.logException(LOGGER, "Cannot parse filter", e);
							getSession().error("Cannot parse filter");
						}
						
					}
					
					@Override
					public String getObject() {
						try {
							PrismValue value = getModelObject().getValue();
							if (value == null || value.isEmpty()) {
								return null;
							}
							
							return getPageBase().getPrismContext().xmlSerializer().serialize(value);
						} catch (SchemaException e) {
							// TODO handle!!!!
							LoggingUtils.logException(LOGGER, "Cannot serialize filter", e);
							getSession().error("Cannot serialize filter");
						}
						return null;
					}
					
					@Override
					public void detach() {
						// TODO Auto-generated method stub
						
					}
				
				});
			}
			
			if (AssignmentType.F_FOCUS_TYPE.equals(definition.getName())){
				List<QName> typesList = WebComponentUtil.createFocusTypeList();
				DropDownChoicePanel<QName> typePanel = new DropDownChoicePanel<QName>(id, new PropertyModel(getModel(), baseExpression),
						Model.ofList(typesList), new QNameObjectTypeChoiceRenderer(), true);
				typePanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
				typePanel.setOutputMarkupId(true);
				return typePanel;
			}
			if (DOMUtil.XSD_DATETIME.equals(valueType)) {
				panel = new DatePanel(id, new PropertyModel<>(getModel(), baseExpression));

			} else if (ProtectedStringType.COMPLEX_TYPE.equals(valueType)) {
				
				if (!(getPageBase() instanceof PageUser)) {
					return new PasswordPanel(id, new PropertyModel<>(getModel(), baseExpression),
						getModel().getObject().isReadonly(), true);
				} 
				panel = new PasswordPanel(id, new PropertyModel<>(getModel(), baseExpression),
						getModel().getObject().isReadonly());
				
			} else if (DOMUtil.XSD_BOOLEAN.equals(valueType)) {
				panel = new TriStateComboPanel(id, new PropertyModel<>(getModel(), baseExpression));
			} else if (DOMUtil.XSD_DURATION.equals(valueType)) {
				panel = new TextPanel<>(id, new PropertyModel<String>(getModel(), baseExpression) {
					
					private IModel model =  new PropertyModel<Duration>(getModel(), baseExpression);
					
					@Override
					public void setObject(String object) {
						model.setObject(XmlTypeConverter.createDuration(MiscUtil.nullIfEmpty(object)));
					}
				}, false);
			} else if (DOMUtil.XSD_QNAME.equals(valueType)) {
				DropDownChoicePanel<QName> panelDropDown = new DropDownChoicePanel<QName>(id, new PropertyModel(getModel(), baseExpression),
						Model.ofList(WebComponentUtil.createObjectTypeList()), new QNameIChoiceRenderer(OBJECT_TYPE), true);
				panelDropDown.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
				panelDropDown.setOutputMarkupId(true);
				return panelDropDown;
			} else if (SchemaConstants.T_POLY_STRING_TYPE.equals(valueType)) {
				InputPanel inputPanel;
				PrismPropertyDefinition def = property.getDefinition();

				if (def.getValueEnumerationRef() != null) {
					PrismReferenceValue valueEnumerationRef = def.getValueEnumerationRef();
					String lookupTableUid = valueEnumerationRef.getOid();
					PrismObject<LookupTableType> lookupTable = getLookupTable(lookupTableUid);

					if (lookupTable != null) {

						inputPanel = new AutoCompleteTextPanel<String>(id, new LookupPropertyModel<>(getModel(),
                            baseExpression + ".orig", lookupTable.asObjectable()), String.class) {

							@Override
							public Iterator<String> getIterator(String input) {
								return prepareAutoCompleteList(input, lookupTable).iterator();
							}
						};

					} else {
						inputPanel = new TextPanel<>(id, new PropertyModel<String>(getModel(), baseExpression + ".orig"),
								String.class, false);
					}

				} else {

					inputPanel = new TextPanel<>(id, new PropertyModel<String>(getModel(), baseExpression + ".orig"),
							String.class, false);
				}

				panel = inputPanel;

			} else if (DOMUtil.XSD_BASE64BINARY.equals(valueType)) {
				panel = new UploadDownloadPanel(id, getModel().getObject().isReadonly()) {

					@Override
					public InputStream getStream() {
						Object object = ((PrismPropertyValue) getModel().getObject().getValue()).getValue();
						return object != null ? new ByteArrayInputStream((byte[]) object) : new ByteArrayInputStream(new byte[0]);
					}

					@Override
					public void updateValue(byte[] file) {
						((PrismPropertyValue) getModel().getObject().getValue()).setValue(file);
					}

					@Override
					public void uploadFilePerformed(AjaxRequestTarget target) {
						super.uploadFilePerformed(target);
						target.add(PrismValuePanel.this.get(ID_FEEDBACK));
					}

					@Override
					public void removeFilePerformed(AjaxRequestTarget target) {
						super.removeFilePerformed(target);
						target.add(PrismValuePanel.this.get(ID_FEEDBACK));
					}

					@Override
					public void uploadFileFailed(AjaxRequestTarget target) {
						super.uploadFileFailed(target);
						target.add(PrismValuePanel.this.get(ID_FEEDBACK));
						target.add(((PageBase) getPage()).getFeedbackPanel());
					}
				};

			} else if (ObjectDeltaType.COMPLEX_TYPE.equals(valueType)) {
				panel = new ModificationsPanel(id, new AbstractReadOnlyModel<DeltaDto>() {
					@Override
					public DeltaDto getObject() {
						if (getModel().getObject() == null || getModel().getObject().getValue() == null
								|| ((PrismPropertyValue) getModel().getObject().getValue()).getValue() == null) {
							return null;
						}
						PrismContext prismContext = ((PageBase) getPage()).getPrismContext();
						ObjectDeltaType objectDeltaType = (ObjectDeltaType) ((PrismPropertyValue) getModel().getObject()
								.getValue()).getValue();
						try {
							ObjectDelta delta = DeltaConvertor.createObjectDelta(objectDeltaType, prismContext);
							return new DeltaDto(delta);
						} catch (SchemaException e) {
							throw new IllegalStateException("Couldn't convert object delta: " + objectDeltaType);
						}

					}
				});
			} else if (QueryType.COMPLEX_TYPE.equals(valueType) || CleanupPoliciesType.COMPLEX_TYPE.equals(valueType)) {
				return new TextAreaPanel<>(id, new AbstractReadOnlyModel<String>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        if (getModel().getObject() == null || getModel().getObject().getValue() == null) {
                            return null;
                        }
                        PrismPropertyValue ppv = (PrismPropertyValue) getModel().getObject().getValue();
                        if (ppv == null || ppv.getValue() == null) {
                            return null;
                        }
                        QName name = property.getElementName();
                        if (name == null && property.getDefinition() != null) {
                            name = property.getDefinition().getName();
                        }
                        if (name == null) {
                            name = SchemaConstants.C_VALUE;
                        }
                        PrismContext prismContext = ((PageBase) getPage()).getPrismContext();
                        try {
                            return prismContext.xmlSerializer().serializeAnyData(ppv.getValue(), name);
                        } catch (SchemaException e) {
                            throw new SystemException(
                                "Couldn't serialize property value of type: " + valueType + ": " + e.getMessage(), e);
                        }
                    }
                }, 10);
			} else if (ItemPathType.COMPLEX_TYPE.equals(valueType)) {
				
				if((getModelObject().getItem().getParent() != null && getModelObject().getItem().getParent().getPath() != null) 
						&& (getModelObject().getItem().getParent().getPath().namedSegmentsOnly().equivalent(
						ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE))
						|| getModelObject().getItem().getParent().getPath().namedSegmentsOnly().equivalent(
						ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION)))
						&& (getModelObject().getItem().getParent().getItem() != null
						&& getModelObject().getItem().getParent().getItem().getParent() != null)){
					
					Class type = XsdTypeMapper.getXsdToJavaMapping(valueType);
					if (type != null && type.isPrimitive()) {
						type = ClassUtils.primitiveToWrapper(type);

					}
					
					PrismContainerValue reference = (PrismContainerValue)getModelObject().getItem().getParent().getItem().getParent();
					
					ResourceType resource = ((ConstructionType)reference.getValue()).getResource();
					if(resource == null) {
						ObjectReferenceType resourceRef = ((ConstructionType)reference.getValue()).getResourceRef();
						OperationResult result = new OperationResult("load_resource");
						Task task = getPageBase().createSimpleTask("load_resource");
						resource = (ResourceType)WebModelServiceUtils.resolveReferenceNoFetch(resourceRef, getPageBase(), task, result).getRealValue();
					}
					
		    		RefinedResourceSchema schema = null;
		            try {
		            	schema = RefinedResourceSchemaImpl.getRefinedSchema(resource, getPageBase().getPrismContext());
					} catch (SchemaException ex) {
						LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse resource schema.", ex);
				        getSession().error(getString("SchemaListPanel.message.couldntParseSchema") + " " + ex.getMessage());
					}
		                
		            LookupTableType lookupTable = new LookupTableType();
				    List<LookupTableRowType> list = lookupTable.createRowList();
			        	
		            for(RefinedObjectClassDefinition schemaDefinition: schema.getRefinedDefinitions()){
		            	if(getModelObject().getItem().getParent().getPath().namedSegmentsOnly().equivalent(
					            ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE))) {
		            		for (ResourceAttributeDefinition def : schemaDefinition.getAttributeDefinitions()) {
		            			LookupTableRowType row = new LookupTableRowType();
		            			row.setKey(def.getName().toString());
		            			row.setValue(def.getName().toString());
		            			row.setLabel(new PolyStringType(def.getName().getLocalPart()));
		            			list.add(row);
		            		}
		            	} else {
		            		for (RefinedAssociationDefinition def : schemaDefinition.getAssociationDefinitions()) {
		            			LookupTableRowType row = new LookupTableRowType();
		            			row.setKey(def.getName().toString());
		            			row.setValue(def.getName().toString());
		            			row.setLabel(new PolyStringType(def.getName().getLocalPart()));
		            			list.add(row);
		            		}
		            	}
		            }
		            LookupPropertyModel inputValue = new LookupPropertyModel<>(Model.of(getModel()), baseExpression, lookupTable, true);
					return new AutoCompleteTextPanel<String>(id, inputValue, String.class) {

								private static final long serialVersionUID = 1L;

								@Override
								public Iterator<String> getIterator(String input) {
									return prepareAutoCompleteList(input, lookupTable.asPrismObject()).iterator();
								}
								
								@Override
								public void checkInputValue(AutoCompleteTextField input, AjaxRequestTarget target, LookupPropertyModel model){
									model.setObject(input.getInput());
									for(LookupTableRowType row : lookupTable.getRow()) {
										if(row.getLabel().toString().equalsIgnoreCase(input.getInput())) {
											String namespace = row.getValue().substring(row.getValue().indexOf("{") + 1);
											namespace = namespace.substring(0, namespace.indexOf("}"));
											((PrismPropertyValue<ItemPathType>) PrismValuePanel.this.getModelObject().getValue()).setValue(new ItemPathType(new ItemName(namespace, row.getLabel().toString())));
											break;
										}
									}
									
							    }
								
								
						};
				}
				
				return new ItemPathPanel(id, (ItemPathType) getModelObject().getValue().getRealValue()) {
				
					private static final long serialVersionUID = 1L;

					@Override
					protected void onUpdate(ItemPathDto itemPathDto) {
						((PrismPropertyValue<ItemPathType>) PrismValuePanel.this.getModelObject().getValue()).setValue(new ItemPathType(itemPathDto.toItemPath())); 
						
					}
				};
				
//				return new QNameEditorPanel(id, new PropertyModel<>(getModel(), baseExpression), null, null,
//						false, false) {
//					@Override
//					protected AttributeAppender getSpecificLabelStyleAppender() {
//						return AttributeAppender.append("style", "font-weight: normal !important;");
//					}
//
//				};

			} else {
				Class type = XsdTypeMapper.getXsdToJavaMapping(valueType);
				if (type != null && type.isPrimitive()) {
					type = ClassUtils.primitiveToWrapper(type);

				}

				if (isEnum(property)) {
					Class clazz = getPageBase().getPrismContext().getSchemaRegistry().determineClassForType(definition.getTypeName());

					if (clazz != null) {
						return WebComponentUtil.createEnumPanel(clazz, id, new PropertyModel(getModel(), baseExpression),
								this);
					}

					return WebComponentUtil.createEnumPanel(definition, id,
							new PropertyModel<>(getModel(), baseExpression), this);
				}
				// // default QName validation is a bit weird, so let's treat
				// QNames as strings [TODO finish this - at the parsing side]
				// if (type == QName.class) {
				// type = String.class;
				// }

				PrismPropertyDefinition def = property.getDefinition();
				
				if(getModelObject().getItem() instanceof PropertyWrapper && getModelObject().getItem().getPath().namedSegmentsOnly().equivalent(ItemPath.create(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_AUDITING, AuditingConfigurationType.F_APPENDER))){
					((PropertyWrapper)getModelObject().getItem()).setPredefinedValues(WebComponentUtil.createAppenderChoices(getPageBase()));
				}

				if(getModelObject().getItem() instanceof PropertyWrapper && ((PropertyWrapper)getModelObject().getItem()).getPredefinedValues() != null) {
					LookupTableType lookupTable = ((PropertyWrapper)getModelObject().getItem()).getPredefinedValues();
					
					boolean isStrict = true;
					if(getModelObject().getItem().getName().equals(ClassLoggerConfigurationType.F_PACKAGE)) {
						isStrict = false;
					}
					
					panel = new AutoCompleteTextPanel<String>(id, new LookupPropertyModel<>(getModel(),
                            baseExpression, lookupTable, isStrict), type) {

								private static final long serialVersionUID = 1L;

								@Override
								public Iterator<String> getIterator(String input) {
									return prepareAutoCompleteList(input, lookupTable.asPrismObject()).iterator();
								}
								
								@Override
								public void checkInputValue(AutoCompleteTextField input, AjaxRequestTarget target, LookupPropertyModel model){
									model.setObject(input.getInput());
							    }
								
								
						};
						
				} else if (def.getValueEnumerationRef() != null) {
					PrismReferenceValue valueEnumerationRef = def.getValueEnumerationRef();
					String lookupTableUid = valueEnumerationRef.getOid();
					PrismObject<LookupTableType> lookupTable = getLookupTable(lookupTableUid);
					
					if (lookupTable != null) {

						panel = new AutoCompleteTextPanel<String>(id, new LookupPropertyModel<>(getModel(),
                            baseExpression, lookupTable.asObjectable()), String.class) {

							@Override
							public Iterator<String> getIterator(String input) {
								return prepareAutoCompleteList(input, lookupTable).iterator();
							}

							@Override
							protected void updateFeedbackPanel(AutoCompleteTextField input, boolean isError,
									AjaxRequestTarget target) {
								if (isError) {
									input.error("Entered value doesn't match any of available values and will not be saved.");
								}
								target.add(PrismValuePanel.this.get(ID_FEEDBACK));
							}
						};

					} else {

						panel = new TextPanel<>(id, new PropertyModel<String>(getModel(), baseExpression), type, false);

					}

				} else {
					panel = new TextPanel<>(id, new PropertyModel<String>(getModel(), baseExpression), type, false);
				}
			}
		} else if (item instanceof PrismReference) {
			panel = new ValueChoosePanel<PrismReferenceValue, ObjectType>(id, new PropertyModel<>(getModel(), "value")) {

				private static final long serialVersionUID = 1L;
				
				@Override
				protected ObjectFilter createCustomFilter() {
					ItemWrapper wrapper = PrismValuePanel.this.getModel().getObject().getItem();
					if (!(wrapper instanceof ReferenceWrapper)) {
						return null;
					}
					return ((ReferenceWrapper) wrapper).getFilter();
				}

				@Override
				protected boolean isEditButtonEnabled() {
					return PrismValuePanel.this.getModel().getObject().isEditEnabled();
				}

				@Override
				public List<QName> getSupportedTypes() {
					List<QName> targetTypeList = ((ReferenceWrapper) PrismValuePanel.this.getModel().getObject().getItem()).getTargetTypes();
					if (targetTypeList == null || WebComponentUtil.isAllNulls(targetTypeList)) {
						return Arrays.asList(ObjectType.COMPLEX_TYPE);
					}
					return targetTypeList;
				}

				@Override
				protected Class getDefaultType(List<QName> supportedTypes){
					if (AbstractRoleType.COMPLEX_TYPE.equals(((PrismReference)item).getDefinition().getTargetTypeName())){
						return RoleType.class;
					} else {
						return super.getDefaultType(supportedTypes);
					}
				}

			};

		}

		return panel;
	}
	
	private PrismObject<LookupTableType> getLookupTable(String lookupTableUid) {
		PrismObject<LookupTableType> lookupTable;
		String operation = "loadLookupTable";
		if(getPageBase() instanceof PageSelfRegistration) {
			lookupTable = getPageBase().runPrivileged(
					() -> {
						Task task = getPageBase().createAnonymousTask(operation);
						OperationResult result = task.getResult();
						Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils
								.createLookupTableRetrieveOptions(getSchemaHelper());
						return WebModelServiceUtils.loadObject(LookupTableType.class,
								lookupTableUid, options, getPageBase(), task, result);
					});
		} else {
			Task task = getPageBase().createSimpleTask(operation);
			OperationResult result = task.getResult();

			Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils
					.createLookupTableRetrieveOptions(getSchemaHelper());
			lookupTable = WebModelServiceUtils.loadObject(LookupTableType.class,
					lookupTableUid, options, getPageBase(), task, result);
		}
		
		return lookupTable;
	}

	private List<String> prepareAutoCompleteList(String input, PrismObject<LookupTableType> lookupTable) {
		List<String> values = new ArrayList<>();

		if (lookupTable == null) {
			return values;
		}

		List<LookupTableRowType> rows = lookupTable.asObjectable().getRow();

		if (input == null || input.isEmpty()) {
			for (LookupTableRowType row : rows) {
				values.add(WebComponentUtil.getOrigStringFromPoly(row.getLabel()));
			}
		} else {
			for (LookupTableRowType row : rows) {
				if (WebComponentUtil.getOrigStringFromPoly(row.getLabel()) != null
						&& WebComponentUtil.getOrigStringFromPoly(row.getLabel()).toLowerCase().contains(input.toLowerCase())) {
					values.add(WebComponentUtil.getOrigStringFromPoly(row.getLabel()));
				}
			}
		}

		return values;
	}

	private boolean isEnum(PrismProperty property) {
		PrismPropertyDefinition definition = property.getDefinition();
		if (definition == null) {
			return property.getValueClass().isEnum();
		}
		return (definition.getAllowedValues() != null && definition.getAllowedValues().size() > 0);
	}

	private String getAttributeName(ResourceAttribute<?> attr) {
		if (attr.getDisplayName() != null) {
			return attr.getDisplayName();
		}

		if (attr.getNativeAttributeName() != null) {
			return attr.getNativeAttributeName();
		}

		if (attr.getElementName() != null) {
			return attr.getElementName().getLocalPart();
		}

		return null; // TODO: is this ok?? or better is exception or some
						// default name??
	}

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
				values.add(new ValueWrapper(propertyWrapper, new PrismReferenceValueImpl(null), ValueStatus.ADDED));
			} else {
				values.add(new ValueWrapper(propertyWrapper, new PrismPropertyValueImpl(null), ValueStatus.ADDED));
			}
		}
		ListView parent = findParent(ListView.class);
		target.add(parent.getParent());
	}

}
