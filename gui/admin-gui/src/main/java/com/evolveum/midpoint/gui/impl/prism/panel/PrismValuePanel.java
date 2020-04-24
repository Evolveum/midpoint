/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemPanelContext;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;

import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.ExpressionValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.IEventSource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;

import java.util.List;

public abstract class PrismValuePanel<T, PV extends PrismValue, VW extends PrismValueWrapper<T, PV>> extends BasePanel<VW> {

    private static final transient Trace LOGGER = TraceManager.getTrace(PrismValuePanel.class);

    private static final String ID_VALUE_FORM = "valueForm";
    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_REMOVE_BUTTON = "removeButton";
    private static final String ID_BUTTON_CONTAINER = "buttonContainer";

    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_VALUE_CONTAINER = "valueContainer";

    private static final String ID_HEADER_CONTAINER = "header";


    private static final String ID_FORM = "form";
    private static final String ID_INPUT = "input";

    private ItemPanelSettings settings;

    public PrismValuePanel(String id, IModel<VW> model, ItemPanelSettings settings) {
        super(id, model);
        this.settings = settings;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Form<VW> form = new Form<>(ID_VALUE_FORM);
        add(form);
        WebMarkupContainer buttonContainer = createHeaderPanel();
        GuiComponentFactory factory = getPageBase().getRegistry().findValuePanelFactory(getModelObject().getParent());
        createValuePanel(factory);
    }

    private WebMarkupContainer createHeaderPanel() {

        WebMarkupContainer buttonContainer = new WebMarkupContainer(ID_HEADER_CONTAINER);
//        buttonContainer.add(new AttributeModifier("class", getButtonsCssClass()));
        add(buttonContainer);

//        // buttons
//        AjaxLink<Void> addButton = new AjaxLink<Void>(ID_ADD_BUTTON) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                addValue(target);
//            }
//        };
//        addButton.add(new VisibleBehaviour(() -> isAddButtonVisible()));
//        buttonContainer.add(addButton);

        AjaxLink<Void> removeButton = new AjaxLink<Void>(ID_REMOVE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    removeValue(PrismValuePanel.this.getModelObject(), target);
                } catch (SchemaException e) {
                    LOGGER.error("Cannot remove value: {}", getModelObject());
                    getSession().error("Cannot remove value "+ getModelObject());
                    target.add(getPageBase().getFeedbackPanel());
                    target.add(PrismValuePanel.this);
                }
            }
        };
        removeButton.add(new VisibleBehaviour(() -> isRemoveButtonVisible()));
        buttonContainer.add(removeButton);


        add(AttributeModifier.append("class", createStyleClassModel(getModel())));

//        add(new VisibleBehaviour(() -> isVisibleValue(getModel())));

        return buttonContainer;
    }

    protected void addToHeader(WebMarkupContainer headerContainer) {

    }

    protected WebMarkupContainer createValuePanel(GuiComponentFactory factory) {

            WebMarkupContainer valueContainer = new WebMarkupContainer(ID_VALUE_CONTAINER);
            valueContainer.setOutputMarkupId(true);
            add(valueContainer);
            // feedback
            FeedbackAlerts feedback = new FeedbackAlerts(ID_FEEDBACK);
            feedback.setOutputMarkupId(true);
            add(feedback);

            PrismPropertyWrapper<T> modelObject = getModelObject().getParent();

            LOGGER.trace("create input component for: {}", modelObject.debugDump());

            Panel component = null;

            org.apache.wicket.markup.html.form.Form<?> form = new org.apache.wicket.markup.html.form.Form<>(ID_FORM);
            valueContainer.add(form);

        if (factory == null) {
            form.add(createDefaultPanel(ID_INPUT));
            return valueContainer;
        }



                ItemPanelContext<T, ItemWrapper<?,?>> panelCtx = createPanelCtx();
        panelCtx.setComponentId(ID_INPUT);
                panelCtx.setForm(form);
                panelCtx.setRealValueModel(getModel());
                panelCtx.setParentComponent(this);
                panelCtx.setAjaxEventBehavior(createEventBehavior());
                panelCtx.setMandatoryHandler(getMandatoryHandler());
                panelCtx.setVisibleEnableBehaviour(createVisibleEnableBehavior());
                panelCtx.setExpressionValidator(createExpressionValidator());
                panelCtx.setFeedback(feedback);

                try {
                    component = factory.createPanel(panelCtx);
                    form.add(component);
                } catch (Throwable e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot create panel", e);
                    getSession().error("Cannot create panel");
                    throw new RuntimeException(e);
                }


//            if (component instanceof Validatable) {
//                Validatable inputPanel = (Validatable) component;
//                // adding valid from/to date range validator, if necessary
//
//                inputPanel.getValidatableComponent().add(expressionValidator);
//
//                inputPanel.getValidatableComponent().add(createEventBehavior());
//                feedback.setFilter(new ComponentFeedbackMessageFilter(inputPanel.getValidatableComponent()));
//            } else {
//                feedback.setFilter(new ComponentFeedbackMessageFilter(component));
//            }

//            if (component instanceof InputPanel) {
//                InputPanel inputPanel = (InputPanel) component;
//
//                final List<FormComponent> formComponents = inputPanel.getFormComponents();
//                for (FormComponent<T> formComponent : formComponents) {
//                    IModel<String> label = LambdaModel.of(modelObject::getDisplayName);
//                    formComponent.setLabel(label);
//                    formComponent.setRequired(getMandatoryHandler() == null ? modelObject.isMandatory() : getMandatoryHandler().isMandatory(modelObject));
//
//                    if (formComponent instanceof TextField) {
//                        formComponent.add(new AttributeModifier("size", "42"));
//                    }
//                    formComponent.add(createEventBehavior());
//                    formComponent.add(new EnableBehaviour(() -> getEditabilityHandler() == null ||
//                            getEditabilityHandler().isEditable(getModelObject())));
//                }
//
////            }
//            if (component == null) {
//                WebMarkupContainer cont = new WebMarkupContainer(ID_INPUT);
//                cont.setOutputMarkupId(true);
//                return cont;
//            }
            return valueContainer;

    }

    private AjaxEventBehavior createEventBehavior() {
        return new AjaxFormComponentUpdatingBehavior("change") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
                target.add(getFeedback());
            }

            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                target.add(getPageBase().getFeedbackPanel());
                target.add(getFeedback());
            }

        };
    }

    private VisibleEnableBehaviour createVisibleEnableBehavior() {
        return new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isVisibleValue();
            }

            @Override
            public boolean isEnabled() {
                if (getEditabilityHandler() != null) {
                    getEditabilityHandler().isEditable(getModelObject().getParent());
                }
                return super.isEnabled();
            }
        };
    }

    private ItemMandatoryHandler getMandatoryHandler() {
        if (settings == null) {
            return null;
        }

        return settings.getMandatoryHandler();
    }

    private ItemEditabilityHandler getEditabilityHandler() {
        if (settings == null) {
             return null;
        }

        return settings.getEditabilityHandler();
    }

    protected ExpressionValidator<T> createExpressionValidator() {
        return new ExpressionValidator<T>(
                LambdaModel.of(getModelObject().getParent()::getFormComponentValidator), getPageBase()) {

            @Override
            protected <O extends ObjectType> O getObjectType() {
                return getObject();
            }
        };
    }

    protected String getButtonsCssClass() {
        return"col-xs-2";
    }

    protected abstract <PC extends ItemPanelContext> PC createPanelCtx();

    protected IModel<String> createStyleClassModel(final IModel<VW> value) {
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

    private int getIndexOfValue(VW value) {
        ItemWrapper<?, VW> property = value.getParent();
        List<VW> values = property.getValues();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(value)) {
                return i;
            }
        }

        return -1;
    }

    protected String getItemCssClass() {
        return " col-sm-offset-0 col-md-offset-4 col-lg-offset-2 prism-value ";
    }

    private <O extends ObjectType> O getObject() {

        PrismObjectWrapper<O> objectWrapper = getModelObject().getParent().findObjectWrapper();
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

    protected abstract Component createDefaultPanel(String id);

    //TODO move to the ItemPanel, exceptionhandling
    protected void addValue(AjaxRequestTarget target) throws SchemaException {
        getModelObject().getParent().add(createNewValue(getModelObject().getParent()), getPageBase());
//        getModelObject().getParent().add(createNewValue(getModelObject().getParent()), getPageBase());
//
//        ItemWrapper<?,?> propertyWrapper = getModel().getObject().getParent();
//
//        WebPrismUtil.createNewValueWrapper(propertyWrapper, createNewValue(propertyWrapper), getPageBase(), target);
//
//        target.add(ItemPanel.this);
    }

    protected abstract <IW extends ItemWrapper<?,?>> PV createNewValue(IW itemWrapper);

    //TODO move to the ItemPanel, exception handling
    protected void removeValue(VW valueToRemove, AjaxRequestTarget target) throws SchemaException {
        LOGGER.debug("Removing value of {}", valueToRemove);

        getModelObject().getParent().remove(valueToRemove, getPageBase());
//        List<VW> values = getModelObject().getValues();
//
//        switch (valueToRemove.getStatus()) {
//            case ADDED:
//                values.remove(valueToRemove);
//                getModelObject().getParent().remove(valueToRemove.getOldValue());
//                getModelObject().getParent().remove(valueToRemove.getNewValue());
//                break;
//            case DELETED:
//                valueToRemove.setStatus(ValueStatus.NOT_CHANGED);
//                getModelObject().getItem().add(valueToRemove.getNewValue());
//                break;
//            case NOT_CHANGED:
//                getModelObject().getItem().remove(valueToRemove.getNewValue());
//                valueToRemove.setStatus(ValueStatus.DELETED);
//                break;
//        }

//        int count = countUsableValues(values);
//
//        if (count == 0 && !hasEmptyPlaceholder(values)) {
//            addValue(target);
//        }

//        target.add(ItemPanel.this);
    }

//    private int countUsableValues(List<VW> values) {
//        int count = 0;
//        for (VW value : values) {
//            if (ValueStatus.DELETED.equals(value.getStatus())) {
//                continue;
//            }
//            if (ValueStatus.ADDED.equals(value.getStatus())) {
//                continue;
//            }
//            count++;
//        }
//        return count;
//    }
//
//    private boolean hasEmptyPlaceholder(List<VW> values) {
//        for (VW value : values) {
//            if (ValueStatus.ADDED.equals(value.getStatus()) ) {//&& !value.hasValueChanged()) {
//                return true;
//            }
//        }
//
//        return false;
//    }

    private boolean isAddButtonVisible() {
        return getModelObject().getParent().isMultiValue();
    }



    protected boolean isRemoveButtonVisible() {
        return !getModelObject().getParent().isReadOnly();

    }


    protected boolean isVisibleValue() {
        VW value = getModelObject();
        return !ValueStatus.DELETED.equals(value.getStatus());
    }

    private Form<VW> getForm() {
        return (Form) get(ID_VALUE_FORM);
    }

    protected FeedbackAlerts getFeedback() {
        return (FeedbackAlerts) get(ID_FEEDBACK);
    }
}
