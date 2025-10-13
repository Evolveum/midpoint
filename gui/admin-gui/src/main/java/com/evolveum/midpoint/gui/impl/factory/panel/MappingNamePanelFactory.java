/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.validator.IntentValidator;
import com.evolveum.midpoint.gui.impl.validator.MappingNameValidator;
import com.evolveum.midpoint.gui.impl.validator.ObjectTypeMappingNameValidator;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

@Component
public class MappingNamePanelFactory extends TextPanelFactory<String> {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (wrapper == null || wrapper.getPath().isEmpty() || wrapper.getPath().lastName() == null) {
            return false;
        }

        if (!AbstractMappingType.F_NAME.equivalent(wrapper.getItemName())) {
            return false;
        }

        if (wrapper.getParent() == null) {
            return false;
        }

        return AbstractMappingType.class.isAssignableFrom(wrapper.getParent().getParent().getTypeClass());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<String> panelCtx) {
        panelCtx.setMandatoryHandler(itemWrapper -> true);
        return super.getPanel(panelCtx);
    }

    @Override
    public void configure(PrismPropertyPanelContext<String> panelCtx, org.apache.wicket.Component component) {
        super.configure(panelCtx, component);
        InputPanel panel = (InputPanel) component;
        panel.getValidatableComponent().add(createValidator(panelCtx));
    }

    protected MappingNameValidator createValidator(PrismPropertyPanelContext<String> panelCtx) {
        return new MappingNameValidator(panelCtx.getItemWrapperModel());
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }
}
