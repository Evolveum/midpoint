/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.stereotype.Component;

/***
 * Panel factory for protected strings in connector configuration.
 * Panel contains only one field for clear password and allow configuration of secret provider.
 */
@Component
public class ProtectedStringInConnectorConfigurationPanelFactory extends ProtectedStringPanelFactory {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!super.match(wrapper, valueWrapper)) {
            return false;
        }

        PrismContainerValueWrapper<ConnectorConfigurationType> configurationParent
                = wrapper.getParentContainerValue(ConnectorConfigurationType.class);
        return configurationParent != null;
    }

    @Override
    protected boolean isShowedOneLinePasswordPanel() {
        return true;
    }

    @Override
    protected boolean showProviderPanel(ItemRealValueModel<ProtectedStringType> realValueModel) {
        return true;
    }

    @Override
    public Integer getOrder() {
        return 799;
    }

}
