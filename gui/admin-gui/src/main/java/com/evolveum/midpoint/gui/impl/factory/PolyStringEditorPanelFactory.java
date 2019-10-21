/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.gui.impl.prism.component.PolyStringEditorPanel;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by honchar
 */
@Component
public class PolyStringEditorPanelFactory extends AbstractGuiComponentFactory<PolyString> {

    private static final long serialVersionUID = 1L;

    @Autowired
    GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }
    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return PolyStringType.COMPLEX_TYPE.equals(wrapper.getTypeName());
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<PolyString> panelCtx) {
        PolyStringEditorPanel panel = new PolyStringEditorPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());

        return panel;
    }

}
