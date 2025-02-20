/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormRoleAnalysisAttributeSettingPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAnalysisSessionOptionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleAnalysisAttributeSettingPropertyFactory extends PrismPropertyWrapperFactoryImpl<List<ItemPathType>> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (!super.match(def)) {
            return false;
        }
        if (parent == null || parent.getDefinition() == null) {
            return false;
        }
        return AbstractAnalysisSessionOptionType.F_USER_ANALYSIS_ATTRIBUTE_SETTING.equivalent(parent.getDefinition().getItemName());
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }

    @Override
    public void registerWrapperPanel(PrismPropertyWrapper<List<ItemPathType>> wrapper) {
        getRegistry().registerWrapperPanel(ItemPathType.COMPLEX_TYPE, VerticalFormRoleAnalysisAttributeSettingPanel.class);
    }
}
