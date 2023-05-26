/*
 * Copyright (C) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.PolyStringEditorPanel;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Created by honchar
 */
@Component
public class PolyStringEditorPanelFactory extends AbstractInputGuiComponentFactory<PolyString> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return PolyStringType.COMPLEX_TYPE.equals(wrapper.getTypeName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<PolyString> panelCtx) {
        return new PolyStringEditorPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel(),
                panelCtx.getPredefinedValuesOid(), panelCtx.hasValueEnumerationRef());
    }
}
