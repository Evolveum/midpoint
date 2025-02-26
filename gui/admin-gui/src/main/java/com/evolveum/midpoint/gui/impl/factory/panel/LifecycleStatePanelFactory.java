/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.input.LifecycleStateFormPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.web.component.prism.InputPanel;

@Component
public class LifecycleStatePanelFactory extends AbstractInputGuiComponentFactory<String> {

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return ObjectType.F_LIFECYCLE_STATE.equals(wrapper.getItemName())
                && wrapper instanceof PrismPropertyWrapper<?> propertyWrapper
                && SystemObjectsType.LOOKUP_LIFECYCLE_STATES.value().equals(propertyWrapper.getPredefinedValuesOid());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<String> panelCtx) {
        return new LifecycleStateFormPanel(panelCtx.getComponentId(), panelCtx.getItemWrapperModel());
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }
}
