/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import java.util.List;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.input.EnumCardChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevConnectorIntentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevConnectorType;

/**
 * Renders {@code connector/intent} as a card-based single selection (see
 * {@link EnumCardChoicePanel}) instead of the default enum drop-down.
 */
@Component
public class ConnDevConnectorIntentGuiComponentFactory extends AbstractGuiComponentFactory<ConnDevConnectorIntentType> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (wrapper.getParentContainerValue(ConnDevConnectorType.class) == null) {
            return false;
        }
        return QNameUtil.match(wrapper.getItemName(), ConnDevConnectorType.F_INTENT);
    }

    @Override
    protected EnumCardChoicePanel<ConnDevConnectorIntentType> getPanel(PrismPropertyPanelContext<ConnDevConnectorIntentType> panelCtx) {
        List<ConnDevConnectorIntentType> choices = List.of(ConnDevConnectorIntentType.values());

        List<EnumCardChoicePanel.CardOption<ConnDevConnectorIntentType>> options = choices.stream()
                .map(intent -> EnumCardChoicePanel.createLocalizedOption(intent, panelCtx.getParentComponent(), ""))
                .toList();

        return new EnumCardChoicePanel<>(panelCtx.getComponentId(), panelCtx.getRealValueModel(),
                options, panelCtx.isMandatory(), false);
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
