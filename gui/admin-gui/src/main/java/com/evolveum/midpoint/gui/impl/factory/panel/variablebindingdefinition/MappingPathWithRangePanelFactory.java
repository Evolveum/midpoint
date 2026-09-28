/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.variablebindingdefinition;

import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.VariableBindingDefinitionTypePanel;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

/**
 * Renders both a path picker and the range editor for inducement/construction attribute mapping
 * targets.
 */
@Component
public class MappingPathWithRangePanelFactory extends VariableBindingDefinitionTypePanelFactory {

    private static final List<ItemPath> ENABLED_PATHS = List.of(
            ItemPath.create(
                    AbstractRoleType.F_INDUCEMENT,
                    AssignmentType.F_CONSTRUCTION,
                    ConstructionType.F_ATTRIBUTE,
                    ResourceAttributeDefinitionType.F_OUTBOUND,
                    MappingType.F_TARGET),
            ItemPath.create(
                    AbstractRoleType.F_INDUCEMENT,
                    AssignmentType.F_CONSTRUCTION,
                    ConstructionType.F_ATTRIBUTE,
                    ResourceAttributeDefinitionType.F_INBOUND,
                    MappingType.F_TARGET));

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!super.match(wrapper, valueWrapper)) {
            return false;
        }

        ItemPath path = wrapper.getPath().namedSegmentsOnly();
        return ENABLED_PATHS.stream().anyMatch(path::equivalent);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<VariableBindingDefinitionType> panelCtx) {
        return new VariableBindingDefinitionTypePanel(
                panelCtx.getComponentId(), panelCtx.getRealValueModel(), true, mappingValueModel(panelCtx));
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
