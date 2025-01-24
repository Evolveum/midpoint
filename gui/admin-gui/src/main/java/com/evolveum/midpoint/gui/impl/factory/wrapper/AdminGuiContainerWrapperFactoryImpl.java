/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.springframework.stereotype.Component;

@Component
public class AdminGuiContainerWrapperFactoryImpl<C extends Containerable> extends NoEmptyValueContainerWrapperFactoryImpl<C> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (!super.match(def, parent)) {
            return false;
        }

        return (QNameUtil.match(AdminGuiConfigurationType.F_USER_DASHBOARD_LINK, def.getItemName()));

    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public boolean skipCreateWrapper(ItemDefinition<?> def, ItemStatus status, WrapperContext context, boolean isEmptyValue) {
        if (QNameUtil.match(AdminGuiConfigurationType.F_USER_DASHBOARD_LINK, def.getItemName())) {
            return false;
        }
        return super.skipCreateWrapper(def, status, context, isEmptyValue);
    }
}
