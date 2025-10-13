/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemDetailsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * Created by honchar
 */
@Component
public class CaseWorkItemWrapperFactoryImpl extends NoEmptyValueContainerWrapperFactoryImpl<CaseWorkItemType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return CaseWorkItemType.COMPLEX_TYPE.equals(def.getTypeName());
    }

    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }

    @Override
    public void registerWrapperPanel(PrismContainerWrapper<CaseWorkItemType> wrapper) {
        getRegistry().registerWrapperPanel(wrapper.getTypeName(), WorkItemDetailsPanel.class);
    }
}
