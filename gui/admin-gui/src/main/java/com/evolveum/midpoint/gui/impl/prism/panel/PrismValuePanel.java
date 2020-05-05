/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
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
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
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
import org.apache.wicket.model.PropertyModel;

import java.util.List;

public abstract class PrismValuePanel<T, IW extends ItemWrapper, VW extends PrismValueWrapper<T, ?>> extends BasePanel<VW> {

    private static final transient Trace LOGGER = TraceManager.getTrace(PrismValuePanel.class);

    private static final String ID_VALUE_FORM = "valueForm";
    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_REMOVE_BUTTON = "removeButton";

    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_VALUE_CONTAINER = "valueContainer";

    private static final String ID_HEADER_CONTAINER = "header";

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
        setOutputMarkupId(true);
    }

    private void initLayout() {
        Form<VW> form = new Form<>(ID_VALUE_FORM);
        add(form);
        form.add(createHeaderPanel());

        createValuePanel(form);
    }

    private WebMarkupContainer createHeaderPanel() {

        WebMarkupContainer buttonContainer = new WebMarkupContainer(ID_HEADER_CONTAINER);
//        buttonContainer.add(new AttributeModifier("class", getButtonsCssClass()));
//        add(buttonContainer);

//        // buttons
        AjaxLink<Void> addButton = new AjaxLink<Void>(ID_ADD_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    addValue(target);
                } catch (SchemaException e) {
                    LOGGER.error("Cannot add value for {}, {}", PrismValuePanel.this.getModelObject().getParent(), e.getMessage(), e);
                    getSession().error(PrismValuePanel.this.getString("PrismValuePanel.addValue.failed", e.getMessage()));
                    target.add(getFeedback());
                }
            }
        };
        addButton.add(new VisibleBehaviour(() -> isAddButtonVisible()));
        buttonContainer.add(addButton);

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

        addToHeader(buttonContainer);
        return buttonContainer;
    }

    protected void addToHeader(WebMarkupContainer headerContainer) {

    }

    private WebMarkupContainer createValuePanel(Form form) {

        GuiComponentFactory factory = getPageBase().getRegistry().findValuePanelFactory(getModelObject().getParent());
            WebMarkupContainer valueContainer = new WebMarkupContainer(ID_VALUE_CONTAINER);
            valueContainer.setOutputMarkupId(true);
            form.add(valueContainer);
            // feedback
            FeedbackAlerts feedback = new FeedbackAlerts(ID_FEEDBACK);
            feedback.setOutputMarkupId(true);
            valueContainer.add(feedback);

            IW modelObject = getModelObject().getParent();

            LOGGER.trace("create input component for: {}", modelObject.debugDump());



        if (factory == null) {
            valueContainer.add(createDefaultPanel(ID_INPUT));
            return valueContainer;
        }



                ItemPanelContext<T, ItemWrapper<?,?>> panelCtx = createPanelCtx(new PropertyModel<>(getModel(), "parent"));
        panelCtx.setComponentId(ID_INPUT);
                panelCtx.setForm(getForm());
                panelCtx.setRealValueModel(getModel());
                panelCtx.setParentComponent(this);
                panelCtx.setAjaxEventBehavior(createEventBehavior());
                panelCtx.setMandatoryHandler(getMandatoryHandler());
                panelCtx.setVisibleEnableBehaviour(createVisibleEnableBehavior());
                panelCtx.setExpressionValidator(createExpressionValidator());
                panelCtx.setFeedback(feedback);

                try {
                    Panel component = factory.createPanel(panelCtx);
                    valueContainer.add(component);
                    factory.configure(panelCtx, component);
                } catch (Throwable e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot create panel", e);
                    getSession().error("Cannot create panel");
                    throw new RuntimeException(e);
                }

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
                    return getEditabilityHandler().isEditable(getModelObject().getParent());
                }
                return super.isEnabled();
            }
        };
    }

    protected ItemPanelSettings getSettings() {
        return settings;
    }

    protected ItemMandatoryHandler getMandatoryHandler() {
        if (settings == null) {
            return null;
        }

        return settings.getMandatoryHandler();
    }

    protected ItemEditabilityHandler getEditabilityHandler() {
        if (settings == null) {
             return null;
        }

        return settings.getEditabilityHandler();
    }

    protected ItemVisibilityHandler getVisibilityHandler() {
        if (settings == null) {
            return null;
        }

        return settings.getVisibilityHandler();
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

    protected abstract <PC extends ItemPanelContext> PC createPanelCtx(IModel<IW> wrapper);

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
    }

    protected abstract <PV extends PrismValue> PV createNewValue(IW itemWrapper);

    //TODO move to the ItemPanel, exception handling
    protected void removeValue(VW valueToRemove, AjaxRequestTarget target) throws SchemaException {
        LOGGER.debug("Removing value of {}", valueToRemove);

        getModelObject().getParent().remove(valueToRemove, getPageBase());
    }

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
        return (FeedbackAlerts) get(createComponentPath(ID_VALUE_FORM, ID_VALUE_CONTAINER, ID_FEEDBACK));
    }
}
