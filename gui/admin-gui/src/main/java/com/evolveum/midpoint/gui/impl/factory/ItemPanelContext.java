/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.io.Serializable;

import org.apache.commons.lang.ClassUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;

public abstract class ItemPanelContext<T, IW extends ItemWrapper> implements Serializable {

    private String componentId;

    private Component parentComponent;

    private IModel<IW> itemWrapper;
    private ItemRealValueModel<T> realValueModel;

    private Form<?> form;

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

    public Class<T> getTypeClass() {
        Class<T> clazz = unwrapWrapperModel().getTypeClass();
        if (clazz == null) {
            clazz = getPrismContext().getSchemaRegistry().determineClassForType(unwrapWrapperModel().getTypeName());
        }
        if (clazz != null && clazz.isPrimitive()) {
            clazz = ClassUtils.primitiveToWrapper(clazz);
        }
        return clazz;
    }

    public ItemRealValueModel<T> getRealValueModel() {
        return realValueModel;
    }

    public <VW extends PrismValueWrapper<T,?>> void setRealValueModel(IModel<VW> valueWrapper) {
        this.realValueModel = new ItemRealValueModel<>(valueWrapper);
    }


    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public void setParentComponent(Component parentComponent) {
        this.parentComponent = parentComponent;
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
