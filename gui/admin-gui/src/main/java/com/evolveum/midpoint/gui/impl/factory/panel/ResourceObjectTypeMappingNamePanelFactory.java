/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.validator.MappingNameValidator;
import com.evolveum.midpoint.gui.impl.validator.ObjectTypeMappingNameValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.springframework.stereotype.Component;

@Component
public class ResourceObjectTypeMappingNamePanelFactory extends MappingNamePanelFactory {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!super.match(wrapper, valueWrapper)) {
            return false;
        }

        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parent =
                valueWrapper.getParentContainerValue(ResourceObjectTypeDefinitionType.class);
        return parent != null;
    }

    protected MappingNameValidator createValidator(PrismPropertyPanelContext<String> panelCtx) {
        return new ObjectTypeMappingNameValidator(panelCtx.getItemWrapperModel());
    }

    @Override
    public Integer getOrder() {
        return 999;
    }
}
