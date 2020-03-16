/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.prism;

import com.evolveum.midpoint.gui.api.Validatable;
import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.factory.PrismPropertyPanelContext;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.util.ExpressionValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;

import java.util.List;

/**
 * @author katkav
 */
public class PrismPropertyPanel<T> extends ItemPanel<PrismPropertyValueWrapper<T>, PrismPropertyWrapper<T>> {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PrismPropertyPanel.class);

    private static final String ID_HEADER = "header";

    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_VALUE_CONTAINER = "valueContainer";

    private static final String ID_FORM = "form";
    private static final String ID_INPUT = "input";


    /**
     * @param id
     * @param model
     */
    public PrismPropertyPanel(String id, IModel<PrismPropertyWrapper<T>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }


    @Override
    protected Panel createHeaderPanel() {
        return new PrismPropertyHeaderPanel<>(ID_HEADER, getModel());
    }


    @Override
    protected Component createValuePanel(ListItem<PrismPropertyValueWrapper<T>> item, GuiComponentFactory factory,
            ItemVisibilityHandler visibilityHandler, ItemEditabilityHandler editabilityHandler) {

        return createInputPanel(item, factory);



    }

     private WebMarkupContainer createInputPanel(ListItem<PrismPropertyValueWrapper<T>> item, GuiComponentFactory factory) {

        WebMarkupContainer valueContainer = new WebMarkupContainer(ID_VALUE_CONTAINER);
        valueContainer.setOutputMarkupId(true);
        item.add(valueContainer);
        // feedback
        FeedbackAlerts feedback = new FeedbackAlerts(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        item.add(feedback);

        PrismPropertyWrapper<T> modelObject = getModelObject();

        LOGGER.trace("create input component for: {}", modelObject.debugDump());

        Panel component = null;

        Form<?> form = new Form<>(ID_FORM);
        valueContainer.add(form);

        if (factory == null) {
            if (getPageBase().getApplication().usesDevelopmentConfig()) {
                form.add(new ErrorPanel(ID_INPUT, createStringResource("Cannot create component for: " + modelObject.getItem())));
            } else {
                Label noComponent = new Label(ID_INPUT);
                noComponent.setVisible(false);
                form.add(noComponent);
            }
            return valueContainer;
        }

        if (factory != null) {

            PrismPropertyPanelContext<T> panelCtx = new PrismPropertyPanelContext<T>(getModel());
            panelCtx.setForm(form);
            panelCtx.setRealValueModel(item.getModel());
            panelCtx.setComponentId(ID_INPUT);
            panelCtx.setParentComponent(this);

            try {
                component = factory.createPanel(panelCtx);
                form.add(component);
            } catch (Throwable e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot create panel", e);
                getSession().error("Cannot create panel");
                throw new RuntimeException(e);
            }
        }

        if (component instanceof Validatable) {
            Validatable inputPanel = (Validatable) component;
            // adding valid from/to date range validator, if necessary
            ExpressionValidator<T> expressionValidator = new ExpressionValidator<T>(
                    LambdaModel.of(modelObject::getFormComponentValidator), getPageBase()) {

                private static final long serialVersionUID = 1L;

                @Override
                protected <O extends ObjectType> O getObjectType() {
                    return getObject();
                }
            };
            inputPanel.getValidatableComponent().add(expressionValidator);

            inputPanel.getValidatableComponent().add(new AjaxFormComponentUpdatingBehavior("change") {

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
            feedback.setFilter(new ComponentFeedbackMessageFilter(inputPanel.getValidatableComponent()));
        } else {
            feedback.setFilter(new ComponentFeedbackMessageFilter(component));
        }

        if (component instanceof InputPanel) {
            InputPanel inputPanel = (InputPanel) component;

            final List<FormComponent> formComponents = inputPanel.getFormComponents();
            for (FormComponent<T> formComponent : formComponents) {
                IModel<String> label = LambdaModel.of(modelObject::getDisplayName);
                formComponent.setLabel(label);
                formComponent.setRequired(modelObject.isMandatory());

                if (formComponent instanceof TextField) {
                    formComponent.add(new AttributeModifier("size", "42"));
                }
                formComponent.add(new AjaxFormComponentUpdatingBehavior("change") {

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
                formComponent.add(new EnableBehaviour(() -> itemPanelSettings == null || itemPanelSettings.getEditabilityHandler() == null ||
                        itemPanelSettings.getEditabilityHandler().isEditable(getModelObject())));
            }


        }
        if (component == null) {
            WebMarkupContainer cont = new WebMarkupContainer(ID_INPUT);
            cont.setOutputMarkupId(true);
            return cont;
        }
        return valueContainer;

    }

    private <OW extends PrismObjectWrapper<O>, O extends ObjectType, C extends Containerable> O getObject() {

        OW objectWrapper = getModelObject().findObjectWrapper();
        if (objectWrapper == null) {
            return null;
        }

        try {
            PrismObject<O> objectNew = objectWrapper.getObjectApplyDelta();
            return objectNew.asObjectable();
        } catch (SchemaException e) {
            LOGGER.error("Cannot apply deltas to object for validation: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected <PV extends PrismValue> PV createNewValue(PrismPropertyWrapper<T> itemWrapper) {
        return (PV) getPrismContext().itemFactory().createPropertyValue();
    }
}
