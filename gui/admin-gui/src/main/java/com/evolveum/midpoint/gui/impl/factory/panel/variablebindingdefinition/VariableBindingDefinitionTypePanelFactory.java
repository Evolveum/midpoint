/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.variablebindingdefinition;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;

import jakarta.annotation.PostConstruct;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.component.VariableBindingDefinitionTypePanel;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

@Component
public class VariableBindingDefinitionTypePanelFactory extends AbstractGuiComponentFactory<VariableBindingDefinitionType> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<VariableBindingDefinitionType> panelCtx) {
        return new VariableBindingDefinitionTypePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return QNameUtil.match(VariableBindingDefinitionType.COMPLEX_TYPE, wrapper.getTypeName());
    }

    /**
     * Walks up from {@code target} to the owning mapping's value wrapper - the range panels need
     * the whole mapping, not just its target.
     */
    @SuppressWarnings("unchecked")
    protected static IModel<PrismContainerValueWrapper<MappingType>> mappingValueModel(
            PrismPropertyPanelContext<VariableBindingDefinitionType> panelCtx) {
        return () -> {
            ItemWrapper<?, ?> targetWrapper = panelCtx.getItemWrapperModel().getObject();
            PrismContainerValueWrapper<?> mappingValue = targetWrapper != null ? targetWrapper.getParent() : null;
            return mappingValue != null && mappingValue.getRealValue() instanceof MappingType
                    ? (PrismContainerValueWrapper<MappingType>) mappingValue
                    : null;
        };
    }
}
