/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.variablebindingdefinition;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.input.SourceOfFocusMappingProvider;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

@Component
public class SourceOfFocusMappingPanelFactory extends SourceOrTargetOfMappingPanelFactory implements Serializable {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!QNameUtil.match(VariableBindingDefinitionType.COMPLEX_TYPE, wrapper.getTypeName())) {
            return false;
        }

        if (wrapper.getParentContainerValue(MappingsType.class) == null) {
            return false;
        }

        return wrapper.getItemName().equivalent(MappingType.F_SOURCE);
    }

    @Override
    protected List<String> getAvailableVariables(String input, IModel<PrismPropertyWrapper<VariableBindingDefinitionType>> itemWrapperModel, PageBase pageBase) {
        SourceOfFocusMappingProvider provider = new SourceOfFocusMappingProvider(itemWrapperModel);
        return provider.collectAvailableDefinitions(input);
    }

    @Override
    protected boolean stripVariableSegment() {
        return false;
    }
}
