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

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.web.component.LockoutStatusPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;

@Component
public class LockoutStatusPanelFactory implements GuiComponentFactory<PrismPropertyPanelContext<LockoutStatusType>> {

    private static final long serialVersionUID = 1L;

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
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return ActivationType.F_LOCKOUT_STATUS.equals(wrapper.getItemName());
    }

    @Override
    public Panel createPanel(PrismPropertyPanelContext<LockoutStatusType> panelCtx) {
        LockoutStatusPanel panel = new LockoutStatusPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
        return panel;
    }

}
