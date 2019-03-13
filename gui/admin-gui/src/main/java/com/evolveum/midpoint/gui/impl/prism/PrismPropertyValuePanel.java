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
package com.evolveum.midpoint.gui.impl.prism;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.PanelContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;
import com.evolveum.midpoint.web.util.ExpressionValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katka
 *
 */
public class PrismPropertyValuePanel<T, V extends PrismPropertyValue<T>, VW extends ValueWrapperImpl<V>> extends BasePanel<VW>{

	private static final long serialVersionUID = 1L;
	
	private static final String ID_FEEDBACK = "feedback";
	private static final String ID_ADD_BUTTON = "addButton";
	private static final String ID_REMOVE_BUTTON = "removeButton";
	private static final String ID_VALUE_CONTAINER = "valueContainer";
	private static final String ID_BUTTON_CONTAINER = "buttonContainer";
	
	private static final String ID_FORM = "form";
	private static final String ID_INPUT = "input";

	ComponentFactoryResolver resolver;
	
	public PrismPropertyValuePanel(String id, IModel<VW> model, ComponentFactoryResolver resolver) {
		super(id, model);
		this.resolver = resolver;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}
	
	private void initLayout() {
		WebMarkupContainer valueContainer = new WebMarkupContainer(ID_VALUE_CONTAINER);
		valueContainer.setOutputMarkupId(true);
		valueContainer.add(new AttributeModifier("class", getValueCssClass()));
		add(valueContainer);
//
		// feedback
		FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
		feedback.setOutputMarkupId(true);
		add(feedback);
    	
    	Component panel = createInputPanel(feedback);
    	panel.add(new AttributeModifier("class", getInputCssClass()));
    	valueContainer.add(panel);

		WebMarkupContainer buttonContainer = new WebMarkupContainer(ID_BUTTON_CONTAINER);
		buttonContainer.add(new AttributeModifier("class", getButtonsCssClass()));
		valueContainer.add(buttonContainer);
	}
	
	 protected Component createInputPanel(FeedbackPanel feedback) {
	 		
			Panel component = null;
			
			resolver.resolveFactory();
			GuiComponentFactory componentFactory = getPageBase().getRegistry().findFactory();
			
			Form<?> form = new Form<>(ID_FORM);
			
			if (componentFactory != null) {

				PanelContext<T> panelCtx = new PanelContext<>();
				panelCtx.setItemWrapper(modelObject);
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

				if (modelObject.getFormItemValidator() != null) {
					ExpressionValidator<T> expressionValidator = new ExpressionValidator<T>(modelObject.getFormItemValidator(),
							getPageBase()) {

						private static final long serialVersionUID = 1L;

						@Override
						protected <O extends ObjectType> O getObjectType() {
							return getObject(modelObject);
						}
					};
					inputPanel.getBaseFormComponent().add(expressionValidator);
					// form.add(expressionValidator);
				}

				final List<FormComponent> formComponents = inputPanel.getFormComponents();
				for (FormComponent<T> formComponent : formComponents) {
					IModel<String> label = WebComponentUtil.getDisplayName(getModel(), PrismPropertyPanel.this);
					formComponent.setLabel(label);
					formComponent.setRequired(modelObject.isRequired());

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
			return component;

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

}
