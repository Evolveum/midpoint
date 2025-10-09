/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.impl.validator.AssociationMappingNameValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.validator.MappingNameValidator;
import com.evolveum.midpoint.gui.impl.validator.ObjectTypeMappingNameValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

@Component
public class AssociationTypeMappingNamePanelFactory extends MappingNamePanelFactory {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!super.match(wrapper, valueWrapper)) {
            return false;
        }

        PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> parent =
                valueWrapper.getParentContainerValue(ShadowAssociationTypeDefinitionType.class);
        return parent != null;
    }

    protected MappingNameValidator createValidator(PrismPropertyPanelContext<String> panelCtx) {
        return new AssociationMappingNameValidator(panelCtx.getItemWrapperModel());
    }

    @Override
    public Integer getOrder() {
        return 999;
    }
}
