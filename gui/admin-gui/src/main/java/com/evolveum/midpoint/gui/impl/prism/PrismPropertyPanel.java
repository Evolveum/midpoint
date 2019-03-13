/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.gui.impl.prism;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.impl.factory.PanelContext;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.ExpressionValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katkav
 */
public class PrismPropertyPanel<T> extends ItemPanel<PrismPropertyValue<T>, PrismProperty<T>, PrismPropertyDefinition<T>, PrismPropertyWrapperImpl<T>> {
	
	private static final long serialVersionUID = 1L;
	private static final Trace LOGGER = TraceManager.getTrace(PrismPropertyPanel.class);
	
	private static final String ID_HEADER = "header";
	
	private static final String ID_FEEDBACK = "feedback";
	private static final String ID_ADD_BUTTON = "addButton";
	private static final String ID_REMOVE_BUTTON = "removeButton";
	private static final String ID_VALUE_CONTAINER = "valueContainer";
	private static final String ID_BUTTON_CONTAINER = "buttonContainer";
	
	private static final String ID_FORM = "form";
	private static final String ID_INPUT = "input";

	
	/**
	 * @param id
	 * @param model
	 */
	public PrismPropertyPanel(String id, IModel<PrismPropertyWrapperImpl<T>> model) {
		super(id, model);
	}


	@Override
	protected Panel createHeaderPanel() {
		return new PrismPropertyHeaderPanel<>(ID_HEADER, getModel());
	}

	
	@Override
	protected void createValuePanel(ListItem item, GuiComponentFactory factory) {
		
		
		WebMarkupContainer panel = createInputPanel(item, factory);
    	

		WebMarkupContainer buttonContainer = new WebMarkupContainer(ID_BUTTON_CONTAINER);
		buttonContainer.add(new AttributeModifier("class", getButtonsCssClass()));
		
		panel.add(buttonContainer);
		// buttons
		AjaxLink<Void> addButton = new AjaxLink<Void>(ID_ADD_BUTTON) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				addValue(target);
			}
		};
		addButton.add(new VisibleBehaviour(() -> isAddButtonVisible()));
		buttonContainer.add(addButton);

		AjaxLink<Void> removeButton = new AjaxLink<Void>(ID_REMOVE_BUTTON) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				removeValue((ValueWrapperOld<T>) item.getModelObject(), target);
			}
		};
		removeButton.add(new VisibleBehaviour(() -> isRemoveButtonVisible()));
		buttonContainer.add(removeButton);
    	

        item.add(AttributeModifier.append("class", createStyleClassModel(item.getModel())));

        item.add(new VisibleBehaviour(() -> isVisibleValue(item.getModel())));
        
	}
	
	 private WebMarkupContainer createInputPanel(ListItem item, GuiComponentFactory factory) {
 		
		 WebMarkupContainer valueContainer = new WebMarkupContainer(ID_VALUE_CONTAINER);
			valueContainer.setOutputMarkupId(true);
			valueContainer.add(new AttributeModifier("class", getValueCssClass()));
			item.add(valueContainer);
		// feedback
			FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
			feedback.setOutputMarkupId(true);
			item.add(feedback);
		 
			PrismPropertyWrapperImpl<T> modelObject = getModelObject();

			LOGGER.trace("createInputComponent: {}", modelObject);

			Panel component = null;
			
			
			Form<?> form = new Form<>(ID_FORM);
			valueContainer.add(form);
			
			if (factory != null) {

				PanelContext<T> panelCtx = new PanelContext(getModel());
				panelCtx.setForm(form);
				panelCtx.setRealValueModel(item.getModel());
				panelCtx.setFeedbackPanel(feedback);
				panelCtx.setComponentId(ID_INPUT);
				panelCtx.setParentComponent(this);

				try {
					component = factory.createPanel(panelCtx);
				} catch (Throwable e) {
					LoggingUtils.logUnexpectedException(LOGGER, "Cannot create panel", e);
					getSession().error("Cannot create panel");
					throw new RuntimeException(e);
				}
			}
			component.add(new AttributeModifier("class", getInputCssClass()));
	    	valueContainer.add(component);

			if (component instanceof InputPanel) {
				InputPanel inputPanel = (InputPanel) component;
				// adding valid from/to date range validator, if necessary
					ExpressionValidator<T> expressionValidator = new ExpressionValidator<T>(LambdaModel.of(modelObject::getFormComponentValidator),
							getPageBase()) {

						private static final long serialVersionUID = 1L;

						@Override
						protected <O extends ObjectType> O getObjectType() {
							return getObject();
						}
					};
					inputPanel.getBaseFormComponent().add(expressionValidator);
					// form.add(expressionValidator);


				final List<FormComponent> formComponents = inputPanel.getFormComponents();
				for (FormComponent<T> formComponent : formComponents) {
					IModel<String> label = LambdaModel.of(modelObject::getDisplayName);
					formComponent.setLabel(label);
					formComponent.setRequired(modelObject.isMandatory());

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
//				throw new RuntimeException(
//						"Cannot create input component for item " + iw + " (" + valueWrapperModel.getObject() + ")");
				WebMarkupContainer cont = new WebMarkupContainer(ID_INPUT);
				cont.setOutputMarkupId(true);
				return cont;
			}
			return valueContainer;

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
	
    private void addValue(AjaxRequestTarget target) {
		PrismPropertyWrapperImpl<T> propertyWrapper = getModel().getObject();
		LOGGER.debug("Adding value of {}", propertyWrapper);
		propertyWrapper.addValue(true);
		target.add(PrismPropertyPanel.this);
	}
	
	private void removeValue(ValueWrapperOld<T> valueToRemove, AjaxRequestTarget target) {
		
		PrismPropertyWrapperImpl itemWrapper = getModel().getObject();
		try {
			itemWrapper.removeValue(valueToRemove);
		} catch (SchemaException e) {
			error("Failed to remove value.");
		}
		
		LOGGER.debug("Removing value of {}", itemWrapper);

		target.add(PrismPropertyPanel.this);
	}
	
	private boolean isAddButtonVisible() {
		return getModelObject().isMultiValue();
	}


	
	private boolean isRemoveButtonVisible() {
		return !getModelObject().isReadOnly();
			
	}
	    
    protected IModel<String> createStyleClassModel(final IModel<ValueWrapperOld<T>> value) {
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

    private int getIndexOfValue(ValueWrapperOld<T> value) {
        ItemWrapperOld property = value.getItem();
        List<ValueWrapperOld<T>> values = property.getValues();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(value)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isVisibleValue(IModel<ValueWrapperOld<T>> model) {
        ValueWrapperOld<T> value = model.getObject();
        return !ValueStatus.DELETED.equals(value.getStatus());
    }
    
    private <O extends ObjectType, C extends Containerable> O getObject() {
		ContainerValueWrapper cValueWrapper = getModelObject().getParent();
		if (cValueWrapper == null) {
			return null;
		}
		ContainerWrapperImpl cWrapper = cValueWrapper.getContainer();
		ObjectWrapperImpl<O> objectWrapper = cWrapper.getObjectWrapper();
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
}
