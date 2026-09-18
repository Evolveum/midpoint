/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.variablebindingdefinition;

import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.VariableBindingDefinitionTypePanel;
import com.evolveum.midpoint.gui.impl.component.input.range.AutoFormMappingRangePanel;
import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangeUtils;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

/**
 * Renders the range sub-panel for mapping targets rendered through the generic (auto-generated)
 * container panels.
 *
 * For an autoassign mapping's target specifically, the target is always implicitly the role's own
 * assignment - there is nothing meaningful for the user to pick there, only the range - so that one path
 * gets the {@link AutoFormMappingRangePanel} instead of the generic panel used for the rest.
 */
@Component
public class MappingPathWithRangePanelFactory extends VariableBindingDefinitionTypePanelFactory {

    private static final List<ItemPath> ENABLED_PATHS = List.of(
            MappingRangeUtils.AUTOASSIGN_MAPPING_TARGET_PATH,
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
        if (isAutoassignTarget(panelCtx)) {
            return new AutoFormMappingRangePanel(panelCtx.getComponentId(), createMappingValueModel(panelCtx));
        }
        return new VariableBindingDefinitionTypePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel(), true);
    }

    private boolean isAutoassignTarget(PrismPropertyPanelContext<VariableBindingDefinitionType> panelCtx) {
        ItemPath path = panelCtx.getItemWrapperModel().getObject().getPath().namedSegmentsOnly();
        return path.equivalent(MappingRangeUtils.AUTOASSIGN_MAPPING_TARGET_PATH);
    }

    private IModel<PrismContainerValueWrapper<MappingType>> createMappingValueModel(
            PrismPropertyPanelContext<VariableBindingDefinitionType> panelCtx) {
        return () -> {
            PrismValueWrapper<VariableBindingDefinitionType> targetValue = panelCtx.getValueWrapperModel().getObject();
            ItemWrapper<?, ?> targetProperty = targetValue != null ? targetValue.getParent() : null;
            PrismContainerValueWrapper<?> mappingValue = targetProperty != null ? targetProperty.getParent() : null;

            return (PrismContainerValueWrapper<MappingType>) mappingValue;
        };
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
