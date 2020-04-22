/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemDetailsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by honchar
 */
@Component
public class WorkItemDetailsPanelFactory extends AbstractGuiComponentFactory<CaseWorkItemType> {

    private static final long serialVersionUID = 1L;

    @Autowired
    GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return CaseWorkItemType.COMPLEX_TYPE.equals(wrapper.getTypeName());
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<CaseWorkItemType> panelCtx) {
        WorkItemDetailsPanel panel = new WorkItemDetailsPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
        panel.setOutputMarkupId(true);
        return panel;
    }

}
