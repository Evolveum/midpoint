/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import javax.annotation.PostConstruct;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapper;

/**
 * @author katka
 *
 */
@Component
public class EnumPanelFactory<T extends Enum<?>> extends AbstractGuiComponentFactory<T> {

    private static final long serialVersionUID = 1L;

    @Autowired GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    private boolean isEnum(ItemWrapper<?, ?> property) {

        if (!(property instanceof PrismPropertyWrapper)) {
            return false;
        }

        Class<T> valueType = property.getTypeClass();
        if (valueType == null) {
            valueType = property.getPrismContext() != null ?
                    property.getPrismContext().getSchemaRegistry().getCompileTimeClass(property.getTypeName()) : null;
        }

        if (valueType != null) {
            return valueType.isEnum();
        }

        return (((PrismPropertyWrapper)property).getAllowedValues() != null && ((PrismPropertyWrapper)property).getAllowedValues().size() > 0);
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return (isEnum(wrapper));
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<T> panelCtx) {
        Class<T> clazz = panelCtx.getTypeClass();

        if (clazz != null) {
            return WebComponentUtil.createEnumPanel(clazz, panelCtx.getComponentId(), panelCtx.getRealValueModel(),
                    panelCtx.getParentComponent());
        }

        return WebComponentUtil.createEnumPanel(panelCtx.unwrapWrapperModel(), panelCtx.getComponentId(),
                panelCtx.getRealValueModel());

    }

}
