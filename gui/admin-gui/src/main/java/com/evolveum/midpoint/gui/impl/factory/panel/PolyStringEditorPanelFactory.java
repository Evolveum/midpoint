/*
 * Copyright (C) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

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
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return PolyStringType.COMPLEX_TYPE.equals(wrapper.getTypeName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<PolyString> panelCtx) {
        return new PolyStringEditorPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel(),
                panelCtx.getPredefinedValuesOid(), panelCtx.hasValueEnumerationRef());
    }
}
