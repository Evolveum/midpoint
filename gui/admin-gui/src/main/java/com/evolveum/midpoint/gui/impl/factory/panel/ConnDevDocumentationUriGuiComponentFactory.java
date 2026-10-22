/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.validation.DocumentationUriValidator;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDocumentationSourceType;

/**
 * Renders documentationSource/uri with validation of the URL.
 */
@org.springframework.stereotype.Component
public class ConnDevDocumentationUriGuiComponentFactory extends AbstractInputGuiComponentFactory<String> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (wrapper.getParentContainerValue(ConnDevDocumentationSourceType.class) == null) {
            return false;
        }
        return QNameUtil.match(wrapper.getItemName(), ConnDevDocumentationSourceType.F_URI);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<String> panelCtx) {
        return new TextPanel<>(panelCtx.getComponentId(), panelCtx.getRealValueModel());
    }

    @Override
    public void configure(PrismPropertyPanelContext<String> panelCtx, Component component) {
        super.configure(panelCtx, component);
        if (component instanceof InputPanel panel) {
            panel.getBaseFormComponent().add(new DocumentationUriValidator());
        }
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
