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
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.input.EnumCardChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevApplicationInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevIntegrationType;

/**
 * Renders {@code application/integrationType} as a card-based single selection
 * (see {@link EnumCardChoicePanel}) and restricts it so that a connector development
 * can never be switched between {@link ConnDevIntegrationType#SQL} and REST/SCIM once created —
 * the SQL backend is a structurally different template/backend family, so such a switch would
 * leave the object in an inconsistent state. New (not yet persisted) objects still offer all values.
 */
@Component
public class ConnDevIntegrationTypeGuiComponentFactory extends AbstractGuiComponentFactory<ConnDevIntegrationType> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (wrapper.getParentContainerValue(ConnDevApplicationInfoType.class) == null) {
            return false;
        }
        return QNameUtil.match(wrapper.getItemName(), ConnDevApplicationInfoType.F_INTEGRATION_TYPE);
    }

    @Override
    protected EnumCardChoicePanel<ConnDevIntegrationType> getPanel(PrismPropertyPanelContext<ConnDevIntegrationType> panelCtx) {
        PrismPropertyWrapper<ConnDevIntegrationType> wrapper = panelCtx.unwrapWrapperModel();

        ConnDevIntegrationType oldType = null;
        if (wrapper.findObjectStatus() != ItemStatus.ADDED) {
            try {
                var oldValue = wrapper.getValue().getOldValue();
                oldType = oldValue != null ? oldValue.getRealValue() : null;
            } catch (SchemaException e) {
                throw new SystemException("Couldn't determine current value of " + wrapper.getItemName(), e);
            }
        }

        List<ConnDevIntegrationType> choices;
        if (oldType == null) {
            // new connector development, nothing to restrict yet
            choices = List.of(ConnDevIntegrationType.values());
        } else if (oldType == ConnDevIntegrationType.SQL) {
            choices = List.of(ConnDevIntegrationType.SQL);
        } else {
            choices = List.of(ConnDevIntegrationType.SCIM, ConnDevIntegrationType.REST);
        }

        List<EnumCardChoicePanel.CardOption<ConnDevIntegrationType>> options = choices.stream()
                .map(type -> EnumCardChoicePanel.createLocalizedOption(type, panelCtx.getParentComponent(), ""))
                .toList();

        // A persisted SQL connector development is locked to the SQL card, so the selection is read-only.
        return new EnumCardChoicePanel<>(panelCtx.getComponentId(), panelCtx.getRealValueModel(),
                options, panelCtx.isMandatory(), oldType == ConnDevIntegrationType.SQL);
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
