/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.protectedstring;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SmartIntegrationConfigurationType;

import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/***
 * Panel factory for api key.
 * Panel contains only one field for api key and allow configuration of secret provider.
 */
@Component
public class ApiKeyPanelFactory extends ProtectedStringPanelFactory {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!super.match(wrapper, valueWrapper)) {
            return false;
        }

        return wrapper instanceof PrismPropertyWrapper
                && (QNameUtil.match(wrapper.getItemName(), SmartIntegrationConfigurationType.F_CONNECTOR_GENERATOR_API_KEY)
                || QNameUtil.match(wrapper.getItemName(), SmartIntegrationConfigurationType.F_SERVICE_API_KEY));
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
    protected boolean useGlobalValuePolicy(IModel<PrismPropertyWrapper<ProtectedStringType>> wrapperModel) {
        return false;
    }

    @Override
    protected IModel<String> getPasswordPlaceholder(PrismPropertyPanelContext<ProtectedStringType> panelCtx) {
        return panelCtx.getPageBase().createStringResource("ApiKeyPanelFactory.apiKey");
    }

    @Override
    public Integer getOrder() {
        return 798;
    }

}
