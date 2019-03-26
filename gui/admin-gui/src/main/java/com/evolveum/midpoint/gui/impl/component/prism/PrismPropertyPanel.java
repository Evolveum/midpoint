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

package com.evolveum.midpoint.gui.impl.component.prism;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.ObjectWrapperOld;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandlerOld;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author lazyman
 */
public class PrismPropertyPanel<IW extends ItemWrapperOld> extends AbstractPrismPropertyPanel<IW> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PrismPropertyPanel.class);

	
	private static final String ID_FEEDBACK = "feedback";
	private static final String ID_INPUT = "input";
	private static final String ID_ADD_BUTTON = "addButton";
	private static final String ID_REMOVE_BUTTON = "removeButton";
	private static final String ID_VALUE_CONTAINER = "valueContainer";
	private static final String ID_BUTTON_CONTAINER = "buttonContainer";
	
	
    private boolean labelContainerVisible = true;
    
    public PrismPropertyPanel(String id, IW model, Form form, ItemVisibilityHandlerOld visibilityHandler) {
		super(id, Model.of(model), form, visibilityHandler);
	}
    
    public PrismPropertyPanel(String id, IModel<IW> model, Form form, ItemVisibilityHandlerOld visibilityHandler) {
		super(id, model, form, visibilityHandler);
	}
    
    protected WebMarkupContainer getHeader(String idComponent) {
    	
    	PrismPropertyHeaderPanel<IW> headerLabel = new PrismPropertyHeaderPanel<IW>(idComponent, getModel(), getPageBase()) {
			private static final long serialVersionUID = 1L;

			@Override
    		public String getContainerLabelCssClass() {
				return " col-md-2 col-xs-12 prism-property-label ";
    		}
    	};
    	headerLabel.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override public boolean isVisible() {
                return labelContainerVisible;
            }
        });
    	
    	return headerLabel;
    }
    
    protected <T> WebMarkupContainer getValues(String idComponent, final IModel<IW> model, final Form form) {
    	
        ListView<ValueWrapperOld<T>> values = new ListView<ValueWrapperOld<T>>(idComponent,
            new PropertyModel<>(model, "values")) {
        	private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<ValueWrapperOld<T>> item) {
            	WebMarkupContainer valueContainer = new WebMarkupContainer(ID_VALUE_CONTAINER);
        		valueContainer.setOutputMarkupId(true);
        		valueContainer.add(new AttributeModifier("class", getValueCssClass()));
        		item.add(valueContainer);
        //
        		// feedback
        		FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        		feedback.setOutputMarkupId(true);
        		item.add(feedback);
            	
            	Component panel = createValuePanel(item.getModel(), feedback, form);
            	panel.add(new AttributeModifier("class", getInputCssClass()));
            	valueContainer.add(panel);

        		WebMarkupContainer buttonContainer = new WebMarkupContainer(ID_BUTTON_CONTAINER);
        		buttonContainer.add(new AttributeModifier("class", getButtonsCssClass()));
        		valueContainer.add(buttonContainer);
        		// buttons
        		AjaxLink<Void> addButton = new AjaxLink<Void>(ID_ADD_BUTTON) {
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

        		AjaxLink<Void> removeButton = new AjaxLink<Void>(ID_REMOVE_BUTTON) {
        			private static final long serialVersionUID = 1L;

        			@Override
        			public void onClick(AjaxRequestTarget target) {
        				removeValue(item.getModelObject(), target);
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
            	

                item.add(AttributeModifier.append("class", createStyleClassModel(item.getModel())));

                item.add(new VisibleEnableBehaviour() {
                	private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        return isVisibleValue(item.getModel());
                    }
                });
            }
        };
        values.add(new AttributeModifier("class", getValuesClass()));
        values.setReuseItems(true);
        return values;
    }
    
    //VALUE REGION
    
    protected <T> Component createValuePanel(IModel<ValueWrapperOld<T>> valueWrapperModel, FeedbackPanel feedback, Form form) {
    		
		IW iw = getModel().getObject();

		LOGGER.trace("createInputComponent: {}", iw);

		Panel component = null;
		GuiComponentFactory componentFactory = getPageBase().getRegistry().findValuePanelFactory(null);
		if (componentFactory != null) {

//			ItemPanelContext<T> panelCtx = new ItemPanelContext<>();
//			panelCtx.setItemWrapper(iw);
//			panelCtx.setForm(form);
//			panelCtx.setRealValueModel(valueWrapperModel.getObject());
//			panelCtx.setFeedbackPanel(feedback);
//			panelCtx.setComponentId(ID_INPUT);
//			panelCtx.setParentComponent(this);
//
//			try {
//				component = componentFactory.createPanel(panelCtx);
//			} catch (Throwable e) {
//				LoggingUtils.logUnexpectedException(LOGGER, "Cannot create panel", e);
//				getSession().error("Cannot create panel");
//				throw new RuntimeException(e);
//			}
		}

		if (component instanceof InputPanel) {
			InputPanel inputPanel = (InputPanel) component;
			// adding valid from/to date range validator, if necessary

//			if (iw.getFormItemValidator() != null) {
//				ExpressionValidator<T> expressionValidator = new ExpressionValidator<T>(iw.getFormItemValidator(),
//						getPageBase()) {
//
//					private static final long serialVersionUID = 1L;
//
//					@Override
//					protected <O extends ObjectType> O getObjectType() {
//						return getObject(iw);
//					}
//				};
//				inputPanel.getBaseFormComponent().add(expressionValidator);
//				// form.add(expressionValidator);
//			}

			final List<FormComponent> formComponents = inputPanel.getFormComponents();
			for (FormComponent<T> formComponent : formComponents) {
//				IModel<String> label = WebComponentUtil.getDisplayName(getModel(), PrismPropertyPanel.this);
//				formComponent.setLabel(label);
				formComponent.setRequired(iw.isRequired());

				if (formComponent instanceof TextField) {
					formComponent.add(new AttributeModifier("size", "42"));
				}
				formComponent.add(new AjaxFormComponentUpdatingBehavior("blur") {
					
					private static final long serialVersionUID = 1L;

					@Override
					protected void onUpdate(AjaxRequestTarget target) {
						target.add(getPageBase().getFeedbackPanel());
						target.add(feedback);
					}

					@Override
					protected void onError(AjaxRequestTarget target, RuntimeException e) {
						target.add(getPageBase().getFeedbackPanel());
						target.add(feedback);
					}

				});
			}
		}
		if (component == null) {
//			throw new RuntimeException(
//					"Cannot create input component for item " + iw + " (" + valueWrapperModel.getObject() + ")");
			WebMarkupContainer cont = new WebMarkupContainer(ID_INPUT);
			cont.setOutputMarkupId(true);
			return cont;
		}
		return component;

    }

    
//    private FeedbackPanel getFeedbackPanel() {
//		return (FeedbackPanel) get(getPageBase().createComponentPath("values", ID_FEEDBACK));
//	}
    
    private void addValue(AjaxRequestTarget target) {
		ItemWrapperOld propertyWrapper = getModel().getObject();
		LOGGER.debug("Adding value of {}", propertyWrapper);
		propertyWrapper.addValue(true);
		target.add(PrismPropertyPanel.this);
	}
	
	private <T> void removeValue(ValueWrapperOld<T> valueToRemove, AjaxRequestTarget target) {
		
		ItemWrapperOld itemWrapper = getModel().getObject();
		try {
			itemWrapper.removeValue(valueToRemove);
		} catch (SchemaException e) {
			error("Failed to remove value.");
		}
		
		LOGGER.debug("Removing value of {}", itemWrapper);

		target.add(PrismPropertyPanel.this);
	}
	
	private boolean isAddButtonVisible() {
		PropertyOrReferenceWrapper propertyWrapper = (PropertyOrReferenceWrapper) getModel().getObject();
		Item property = propertyWrapper.getItem();

		ItemDefinition definition = property.getDefinition();
		int max = definition.getMaxOccurs();
//		List<ValueWrapper> usableValues = getUsableValues(propertyWrapper);
//		if (usableValues.indexOf(valueWrapper) != usableValues.size() - 1) {
//			return false;
//		}

		if (max == -1) {
			return true;
		}
		return false;

//		if (countNonDeletedValues(propertyWrapper) >= max) {
//			return false;
//		}
//
//		if (propertyWrapper.getParent() == null) {
//			return true; // TODO
//		}
//		return isAccessible(definition, getContainerStatus(propertyWrapper));
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

	
	private boolean isRemoveButtonVisible() {
		if (getModel().getObject().isReadonly()) {
			return false;
		}
		
		return true;

	}
	
	private ContainerStatus getContainerStatus(ItemWrapperOld propertyWrapper) {
		final ContainerWrapperImpl<Containerable> objectWrapper = null;// propertyWrapper.getParent();
		return objectWrapper != null ? objectWrapper.getStatus() : ContainerStatus.MODIFYING;
	}
	
//	private int countUsableValues(PropertyOrReferenceWrapper<? extends Item, ? extends ItemDefinition> property) {
//		int count = 0;
//		for (ValueWrapper value : property.getValues()) {
//			value.normalize(property.getItemDefinition().getPrismContext());
//
//			if (ValueStatus.DELETED.equals(value.getStatus())) {
//				continue;
//			}
//
//			if (ValueStatus.ADDED.equals(value.getStatus()) && !value.hasValueChanged()) {
//				continue;
//			}
//
//			count++;
//		}
//		return count;
//	}


	private List<ValueWrapperOld> getUsableValues(PropertyOrReferenceWrapper<? extends Item, ? extends ItemDefinition> property) {
		List<ValueWrapperOld> values = new ArrayList<>();
		for (ValueWrapperOld value : property.getValues()) {
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
		for (ValueWrapperOld value : property.getValues()) {
			value.normalize(property.getItemDefinition().getPrismContext());
			if (ValueStatus.DELETED.equals(value.getStatus())) {
				continue;
			}
			count++;
		}
		return count;
	}

//	private boolean hasEmptyPlaceholder(PropertyOrReferenceWrapper<? extends Item, ? extends ItemDefinition> property) {
//		for (ValueWrapper value : property.getValues()) {
//			value.normalize(property.getItemDefinition().getPrismContext());
//			if (ValueStatus.ADDED.equals(value.getStatus()) && !value.hasValueChanged()) {
//				return true;
//			}
//		}
//
//		return false;
//	}
    
    private <O extends ObjectType, C extends Containerable> O getObject(ItemWrapperOld itemWrapper) {
		ContainerWrapperImpl<C> cWrapper = null;//itemWrapper.getParent();
		if (cWrapper == null) {
			return null;
		}
		
		ObjectWrapperOld<O> objectWrapper = cWrapper.getObjectWrapper();
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
    
    
    protected String getInputCssClass() {
        return"col-xs-10";
    }
    
    protected String getButtonsCssClass() {
        return"col-xs-2";
    }

    protected String getValuesClass() {
        return "col-md-6";
    }

    protected String getValueCssClass() {
        return "row";
    }

    protected <T> IModel<String> createStyleClassModel(final IModel<ValueWrapperOld<T>> value) {
        return new IModel<String>() {
        	private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (getIndexOfValue(value.getObject()) > 0) {
                    return getItemCssClass();
                }

                return null;
            }
        };
    }
    
    protected String getItemCssClass() {
    	return " col-md-offset-2 prism-value ";
    }

    private <T> int getIndexOfValue(ValueWrapperOld<T> value) {
        ItemWrapperOld property = value.getItem();
        List<ValueWrapperOld<T>> values = property.getValues();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(value)) {
                return i;
            }
        }

        return -1;
    }

    private <T> boolean isVisibleValue(IModel<ValueWrapperOld<T>> model) {
        ValueWrapperOld<T> value = model.getObject();
        return !ValueStatus.DELETED.equals(value.getStatus());
    }

    public boolean isLabelContainerVisible() {
        return labelContainerVisible;
    }

    public void setLabelContainerVisible(boolean labelContainerVisible) {
        this.labelContainerVisible = labelContainerVisible;
    }
}
