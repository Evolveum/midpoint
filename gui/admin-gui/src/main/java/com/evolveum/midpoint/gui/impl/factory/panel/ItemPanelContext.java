/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;

import org.apache.commons.lang3.ClassUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.ExpressionValidator;

public abstract class ItemPanelContext<T, IW extends ItemWrapper<?, ?>> implements Serializable {

    private String componentId;

    private Component parentComponent;

    private final IModel<IW> itemWrapper;
    private IModel<? extends PrismValueWrapper<T>> valueWrapperModel;
//    private ItemRealValueModel<T> realValueModel;

    private Form<?> form;
    private AjaxEventBehavior ajaxEventBehavior;
    private ItemMandatoryHandler mandatoryHandler;
    private VisibleEnableBehaviour visibleEnableBehaviour;
    private ExpressionValidator<T, IW> expressionValidator;
    private FeedbackAlerts feedback;

    public ItemPanelContext(IModel<IW> itemWrapper) {
        this.itemWrapper = itemWrapper;
    }

    public IW unwrapWrapperModel() {
        return itemWrapper.getObject();
    }

    public PageBase getPageBase() {
        return (PageBase) parentComponent.getPage();
    }

    public String getComponentId() {
        return componentId;
    }

    public PrismContext getPrismContext() {
        return unwrapWrapperModel().getPrismContext();
    }

    public ItemName getDefinitionName() {
        return unwrapWrapperModel().getItemName();
    }

    public Component getParentComponent() {
        return parentComponent;
    }

    @SuppressWarnings("unchecked")
    public Class<T> getTypeClass() {
        Class<T> clazz = unwrapWrapperModel().getTypeClass();
        if (clazz == null) {
            clazz = PrismContext.get().getSchemaRegistry().determineClassForType(unwrapWrapperModel().getTypeName());
        }
        if (clazz != null && clazz.isPrimitive()) {
            clazz = (Class<T>) ClassUtils.primitiveToWrapper(clazz);
        }
        return clazz;
    }

    public ItemRealValueModel<T> getRealValueModel() {
        return new ItemRealValueModel<>(valueWrapperModel);
    }

    public <VW extends PrismValueWrapper<T>> void setRealValueModel(IModel<VW> valueWrapper) {
        valueWrapperModel = valueWrapper;
//        this.realValueModel = ;
    }

    IModel<? extends PrismValueWrapper<T>> getValueWrapperModel() {
        return valueWrapperModel;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public void setParentComponent(Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    public void setAjaxEventBehavior(AjaxEventBehavior ajaxEventBehavior) {
        this.ajaxEventBehavior = ajaxEventBehavior;
    }

    public AjaxEventBehavior getAjaxEventBehavior() {
        return ajaxEventBehavior;
    }

    public void setMandatoryHandler(ItemMandatoryHandler mandatoryHandler) {
        this.mandatoryHandler = mandatoryHandler;
    }

    public void setVisibleEnableBehaviour(VisibleEnableBehaviour visibleEnableBehaviour) {
        this.visibleEnableBehaviour = visibleEnableBehaviour;
    }

    public VisibleEnableBehaviour getVisibleEnableBehavior() {
        return visibleEnableBehaviour;
    }

    public boolean isMandatory() {
        if (mandatoryHandler != null) {
            return mandatoryHandler.isMandatory(itemWrapper.getObject());
        }
        return itemWrapper.getObject().isMandatory();
    }

    public void setExpressionValidator(ExpressionValidator<T, IW> expressionValidator) {
        this.expressionValidator = expressionValidator;
    }

    public ExpressionValidator<T, IW> getExpressionValidator() {
        return expressionValidator;
    }

    public void setFeedback(FeedbackAlerts feedback) {
        this.feedback = feedback;
    }

    public FeedbackAlerts getFeedback() {
        return feedback;
    }

    /**
     * @return the form
     */
    public Form<?> getForm() {
        return form;
    }

    /**
     * @param form the form to set
     */
    public void setForm(Form<?> form) {
        this.form = form;
    }

}
