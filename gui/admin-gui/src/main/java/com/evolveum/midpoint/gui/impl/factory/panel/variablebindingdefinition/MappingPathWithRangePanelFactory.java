/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.variablebindingdefinition;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.VariableBindingDefinitionTypePanel;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

@Component
public class MappingPathWithRangePanelFactory extends VariableBindingDefinitionTypePanelFactory {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!super.match(wrapper, valueWrapper)) {
            return false;
        }

        if (wrapper.getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                RoleType.F_AUTOASSIGN,
                AutoassignSpecificationType.F_FOCUS,
                FocalAutoassignSpecificationType.F_MAPPING,
                AutoassignMappingType.F_TARGET))) {
            return true;
        }

        return false;

    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<VariableBindingDefinitionType> panelCtx) {
        return new VariableBindingDefinitionTypePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel(), true);
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
