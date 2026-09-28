/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.variablebindingdefinition;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangePanel;
import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangeUtils;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

/**
 * Renders just the range editor for autoassign's {@code target} - no path picker, since the target
 * there is always the role's own assignment.
 */
@Component
public class AutoassignMappingTargetPanelFactory extends VariableBindingDefinitionTypePanelFactory {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return super.match(wrapper, valueWrapper)
                && wrapper.getPath().namedSegmentsOnly().equivalent(MappingRangeUtils.AUTOASSIGN_MAPPING_TARGET_PATH);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<VariableBindingDefinitionType> panelCtx) {
        return new MappingRangePanel(panelCtx.getComponentId(), mappingValueModel(panelCtx)) {

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }
        };
    }

    @Override
    public Integer getOrder() {
        return 99;
    }
}
