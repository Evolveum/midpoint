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

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.PanelContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.input.ExpressionValuePanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ExpressionWrapper;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismHeaderPanel;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.ExpressionValidator;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PrismPropertyPanel<IW extends ItemWrapper> extends AbstractPrismPropertyPanel<IW> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PrismPropertyPanel.class);

	
	private static final String ID_FEEDBACK = "feedback";
	private static final String ID_INPUT = "input";
	private static final String ID_ADD_BUTTON = "addButton";
	private static final String ID_REMOVE_BUTTON = "removeButton";
	private static final String ID_VALUE_CONTAINER = "valueContainer";
	private static final String ID_BUTTON_CONTAINER = "buttonContainer";
	
	
    private boolean labelContainerVisible = true;
    
    public PrismPropertyPanel(String id, IModel<IW> model, Form form, ItemVisibilityHandler visibilityHandler,
			PageBase pageBase) {
		super(id, model, form, visibilityHandler, pageBase);
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
    	
        ListView<ValueWrapper<T>> values = new ListView<ValueWrapper<T>>(idComponent,
            new PropertyModel<>(model, "values")) {
        	private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<ValueWrapper<T>> item) {
            	WebMarkupContainer valueContainer = new WebMarkupContainer(ID_VALUE_CONTAINER);
        		valueContainer.setOutputMarkupId(true);
        		valueContainer.add(new AttributeModifier("class", getValueCssClass()));
        		item.add(valueContainer);
        //
        		// feedback
        		FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        		feedback.setOutputMarkupId(true);
        		item.add(feedback);
            	
            	Component panel = createPropertyPanel("value", item.getModel(), feedback, form);
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
            	
//                ItemWrapper itemWrapper = item.getModelObject().getItem();
//                if ((itemWrapper.getPath().containsNameExactly(ConstructionType.F_ASSOCIATION) ||
//                                itemWrapper.getPath().containsNameExactly(ConstructionType.F_ATTRIBUTE))&&
//                        itemWrapper.getPath().containsNameExactly(ResourceObjectAssociationType.F_OUTBOUND) &&
//                        itemWrapper.getPath().containsNameExactly(MappingType.F_EXPRESSION)){
//                    ExpressionWrapper expressionWrapper = (ExpressionWrapper)item.getModelObject().getItem();
//                    panel = new ExpressionValuePanel("value", new PropertyModel(item.getModel(), "value.value"),
//                            expressionWrapper.getConstruction(), getPageBase()){
//                        private static final long serialVersionUID = 1L;
//
//                        @Override
//                        protected boolean isAssociationExpression(){
//                            return itemWrapper.getPath().containsNameExactly(ConstructionType.F_ASSOCIATION);
//                        }
//                    };
//                } else {
////                    panel = new PrismValuePanel2("value", item.getModel(), label, form, getValueCssClass(), getInputCssClass(), getButtonsCssClass());
//                }
            	
            	
            	
            	
//                item.add(panel);
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
    
    private <T> Component createPropertyPanel(String id, IModel<ValueWrapper<T>> valueWrapperModel, FeedbackPanel feedback, Form form) {
    		
		ItemWrapper iw = getModel().getObject();

		ItemDefinition definition = iw.getItemDefinition();
		boolean required = definition.getMinOccurs() > 0;
		boolean enforceRequiredFields = iw.isEnforceRequiredFields();
		LOGGER.trace("createInputComponent: {}", iw);

		Panel component = null;
		GuiComponentFactory componentFactory = getPageBase().getRegistry().findFactory(iw);
		if (componentFactory != null) {

			PanelContext<T> panelCtx = new PanelContext<>();
			panelCtx.setItemWrapper(iw);
			panelCtx.setForm(form);
			panelCtx.setRealValueModel(valueWrapperModel.getObject());
			panelCtx.setFeedbackPanel(feedback);
			panelCtx.setComponentId(ID_INPUT);
			panelCtx.setParentComponent(this);

			try {
				component = componentFactory.createPanel(panelCtx);
			} catch (Throwable e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Cannot create panel", e);
				getSession().error("Cannot create panel");
				throw new RuntimeException(e);
			}
		}

		if (component instanceof InputPanel) {
			InputPanel inputPanel = (InputPanel) component;
			// adding valid from/to date range validator, if necessary

			if (iw.getFormItemValidator() != null) {
				ExpressionValidator<T> expressionValidator = new ExpressionValidator<T>(iw.getFormItemValidator(),
						getPageBase()) {

					private static final long serialVersionUID = 1L;

					@Override
					protected <O extends ObjectType> O getObjectType() {
						return getObject(iw);
					}
				};
				inputPanel.getBaseFormComponent().add(expressionValidator);
				// form.add(expressionValidator);
			}

			final List<FormComponent> formComponents = inputPanel.getFormComponents();
			for (FormComponent formComponent : formComponents) {
				IModel<String> label = WebComponentUtil.getDisplayName((IModel<ItemWrapper>) getModel(), PrismPropertyPanel.this);
				formComponent.setLabel(label);
				formComponent.setRequired(required && enforceRequiredFields);

				if (formComponent instanceof TextField) {
					formComponent.add(new AttributeModifier("size", "42"));
				}
				formComponent.add(new AjaxFormComponentUpdatingBehavior("blur") {

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
			throw new RuntimeException(
					"Cannot create input component for item " + iw + " (" + valueWrapperModel.getObject() + ")");
		}
		return component;

    }

    
//    private FeedbackPanel getFeedbackPanel() {
//		return (FeedbackPanel) get(getPageBase().createComponentPath("values", ID_FEEDBACK));
//	}
    
    private void addValue(AjaxRequestTarget target) {
		ItemWrapper propertyWrapper = getModel().getObject();
		LOGGER.debug("Adding value of {}", propertyWrapper);
		propertyWrapper.addValue(true);
		target.add(PrismPropertyPanel.this);
	}
	
	private <T> void removeValue(ValueWrapper<T> valueToRemove, AjaxRequestTarget target) {
		
		ItemWrapper itemWrapper = getModel().getObject();
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
	
	private ContainerStatus getContainerStatus(ItemWrapper propertyWrapper) {
		final ContainerWrapper<Containerable> objectWrapper = propertyWrapper.getParent();
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
    
    private <O extends ObjectType, C extends Containerable> O getObject(ItemWrapper itemWrapper) {
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

    protected <T> IModel<String> createStyleClassModel(final IModel<ValueWrapper<T>> value) {
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

    private <T> int getIndexOfValue(ValueWrapper<T> value) {
        ItemWrapper property = value.getItem();
        List<ValueWrapper<T>> values = property.getValues();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(value)) {
                return i;
            }
        }

        return -1;
    }

    private <T> boolean isVisibleValue(IModel<ValueWrapper<T>> model) {
        ValueWrapper<T> value = model.getObject();
        return !ValueStatus.DELETED.equals(value.getStatus());
    }

    public boolean isLabelContainerVisible() {
        return labelContainerVisible;
    }

    public void setLabelContainerVisible(boolean labelContainerVisible) {
        this.labelContainerVisible = labelContainerVisible;
    }
}
