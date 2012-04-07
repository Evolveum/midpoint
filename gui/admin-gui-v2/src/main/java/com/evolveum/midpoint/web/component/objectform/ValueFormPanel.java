/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.objectform;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.web.component.objectform.input.DatePanel;
import com.evolveum.midpoint.web.component.objectform.input.TextPanel;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.Date;
import java.util.List;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

/**
 * @author lazyman
 */
public class ValueFormPanel extends Panel {
    private IModel<PropertyValueWrapper> model;
    private Integer count = 0;

    public ValueFormPanel(String id, IModel<PropertyValueWrapper> model) {
        super(id);
        Validate.notNull(model, "Property value model must not be null.");
        this.model = model;

        initLayout();
    }

    private void initLayout() {
        //feedback
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        //helpButton
        StaticImage helpButtonImage = new StaticImage("helpButton", new Model<String>("../../img/formIcon/InfoSmall.png"));
        helpButtonImage.setOutputMarkupId(true);
        add(helpButtonImage);
        
        //helpContent
        Label labelHelpContent = new Label("helpContent", "This is help text sample...");
        labelHelpContent.setMarkupId("content_"+helpButtonImage.getMarkupId());
        add(labelHelpContent);
        
        //input
        InputPanel component = createInputComponent("input", feedback);
        add(component);

        feedback.setFilter(new ComponentFeedbackMessageFilter(component.getComponent()));

        //buttons
        add(new AjaxLink("addButton") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValue(target);
            }
        });
        add(new AjaxLink("removeButton") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeValue(target);
            }
        });
    }

    private InputPanel createInputComponent(String id, final FeedbackPanel feedback) {
        //todo create input components
        InputPanel component = createTypedInputComponent(id);

        final FormComponent formComponent = component.getComponent();
        formComponent.add(new SimpleAttributeModifier("size", "42"));
        formComponent.add(new AjaxFormComponentUpdatingBehavior("onBlur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(formComponent);
                target.add(feedback);
            }

            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                target.add(formComponent);
                target.add(feedback);

                super.onError(target, e);
            }
        });
        
        count++;
        return component;
    }

    private InputPanel createTypedInputComponent(String id) {
        PrismProperty property = model.getObject().getProperty().getProperty();
        QName valueType = property.getDefinition().getTypeName();

        InputPanel panel = null;
        if (new QName(W3C_XML_SCHEMA_NS_URI, "dateTime").equals(valueType)) {
            panel = new DatePanel(id, new PropertyModel<Date>(model, "value.value"));
        } else {
            panel = new TextPanel<String>(id, new PropertyModel<String>(model, "value.value"));
        }
        
        return panel;
    }

    private void addValue(AjaxRequestTarget target) {
        PropertyValueWrapper wrapper = model.getObject();
        PropertyWrapper propertyWrapper = wrapper.getProperty();

        List<PropertyValueWrapper> values = propertyWrapper.getPropertyValueWrappers();
        values.add(new PropertyValueWrapper(propertyWrapper, new PrismPropertyValue(null), ValueStatus.ADDED));

        ListView parent = findParent(ListView.class);
        target.add(parent.getParent());
    }

    private void removeValue(AjaxRequestTarget target) {
        PropertyValueWrapper wrapper = model.getObject();
        PropertyWrapper propertyWrapper = wrapper.getProperty();

        List<PropertyValueWrapper> values = propertyWrapper.getPropertyValueWrappers();

        switch (wrapper.getStatus()) {
            case ADDED:
                values.remove(wrapper);
                break;
            case DELETED:
                throw new IllegalStateException("Couldn't delete already deleted item: " + toString());
            case NOT_CHANGED:
                wrapper.setStatus(ValueStatus.DELETED);
                break;
        }

        ListView parent = findParent(ListView.class);
        target.add(parent.getParent());
    }
    
    public class StaticImage extends WebComponent {
		private static final long serialVersionUID = 1L;

		public StaticImage(String id, IModel<String> model) {
            super(id, model);
        }

        protected void onComponentTag(ComponentTag tag) {
            super.onComponentTag(tag);
            checkComponentTag(tag, "img");
            tag.put("src", getDefaultModelObjectAsString());
            tag.put("alt", "");
        }

    }
    
}
