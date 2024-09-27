/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.variablebindingdefinition;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.gui.impl.component.input.SourceOfFocusMappingProvider;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Component
public class TargetOfFocusMappingPanelFactory extends SourceOrTargetOfMappingPanelFactory implements Serializable {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!QNameUtil.match(VariableBindingDefinitionType.COMPLEX_TYPE, wrapper.getTypeName())) {
            return false;
        }

        if (wrapper.getParentContainerValue(MappingsType.class) == null) {
            return false;
        }

        return wrapper.getItemName().equivalent(MappingType.F_TARGET);
    }

    @Override
    protected List<String> getAvailableVariables(String input, IModel<PrismPropertyWrapper<VariableBindingDefinitionType>> itemWrapperModel, PageBase pageBase) {
        FocusDefinitionsMappingProvider provider = new FocusDefinitionsMappingProvider(itemWrapperModel) {
            @Override
            protected PrismContainerDefinition<? extends Containerable> getFocusTypeDefinition(ResourceObjectTypeDefinitionType resourceObjectType) {
                QName focusType = AssignmentHolderType.COMPLEX_TYPE;
                PrismContainerValueWrapper<AssignmentType> assignmentWrapper =
                        getRowModel().getObject().getParentContainerValue(AssignmentType.class);
                if (assignmentWrapper != null
                        && assignmentWrapper.getRealValue() != null
                        && assignmentWrapper.getRealValue().getFocusType() != null) {
                    focusType = assignmentWrapper.getRealValue().getFocusType();
                }

                return PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(focusType);
            }
        };
        return provider.collectAvailableDefinitions(input);
    }
}
