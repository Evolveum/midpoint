/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;

import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemDetailsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * Created by honchar
 */
@Component
public class WorkItemDetailsPanelFactory implements GuiComponentFactory<PrismContainerPanelContext<CaseWorkItemType>> {

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return CaseWorkItemType.COMPLEX_TYPE.equals(wrapper.getTypeName());
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismContainerPanelContext<CaseWorkItemType> panelCtx) {
        WorkItemDetailsPanel panel = new WorkItemDetailsPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
        panel.setOutputMarkupId(true);
        return panel;
    }

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE - 10;
    }
}
