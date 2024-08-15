/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BehaviorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import jakarta.annotation.PostConstruct;

import org.apache.wicket.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.web.component.LockoutStatusPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;

@Component
public class LockoutStatusPanelFactory implements GuiComponentFactory<PrismPropertyPanelContext<LockoutStatusType>> {

    @Autowired GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return wrapper instanceof PrismPropertyWrapper && ActivationType.F_LOCKOUT_STATUS.equals(wrapper.getItemName());
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismPropertyPanelContext<LockoutStatusType> panelCtx) {
        PrismObjectWrapper<FocusType> focus = panelCtx.getItemWrapperModel().getObject().findObjectWrapper();
        PrismContainerWrapper<BehaviorType> behavior = null;
        PrismContainerWrapper<ActivationType> activation = null;
        if (focus != null) {
            try {
                activation = focus.findContainer(FocusType.F_ACTIVATION);
                behavior = focus.findContainer(FocusType.F_BEHAVIOR);
            } catch (SchemaException e) {
                //nothing to do
            }
        }
        PrismPropertyWrapper<LockoutStatusType> lockoutStatus = panelCtx.unwrapWrapperModel();
        return new LockoutStatusPanel(panelCtx.getComponentId(), Model.of(lockoutStatus), Model.of(behavior), Model.of(activation));
    }
}
