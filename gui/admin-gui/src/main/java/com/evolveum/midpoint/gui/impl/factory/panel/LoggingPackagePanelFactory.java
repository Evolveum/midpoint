/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.component.autocomplete.LoggingPackageAutocompletePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;

import jakarta.annotation.PostConstruct;

@Component
public class LoggingPackagePanelFactory extends AbstractInputGuiComponentFactory<String> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }


    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<String> panelCtx) {
        return new LoggingPackageAutocompletePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return wrapper instanceof PrismPropertyWrapper
                && QNameUtil.match(wrapper.getItemName(), ClassLoggerConfigurationType.F_PACKAGE);
    }


    @Override
    public Integer getOrder() {
        return 1;
    }
}
