/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.CaseWorkItemTypeWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemDetailsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by honchar
 */
@Component
public class CaseWorkItemWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<CaseWorkItemType> {

    @Autowired
    private GuiComponentRegistry registry;

    @Override
    public boolean match(ItemDefinition<?> def) {
        return CaseWorkItemType.COMPLEX_TYPE.equals(def.getTypeName());
    }

    @Override
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    protected PrismContainerValue<CaseWorkItemType> createNewValue(PrismContainer<CaseWorkItemType> item) {
        throw new UnsupportedOperationException("New case work item value should not be created while creating wrappers.");
    }


    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<CaseWorkItemType> item, WrapperContext context) {
        return false;
    }

    @Override
    protected PrismContainerWrapper<CaseWorkItemType> createWrapper(PrismContainerValueWrapper<?> parent, PrismContainer<CaseWorkItemType> item,
                                                                 ItemStatus status) {
        getRegistry().registerWrapperPanel(item.getDefinition().getTypeName(), WorkItemDetailsPanel.class);
        CaseWorkItemTypeWrapper containerWrapper = new CaseWorkItemTypeWrapper(parent, item, status);
        return containerWrapper;
    }

    @Override
    public PrismContainerValueWrapper<CaseWorkItemType> createValueWrapper(PrismContainerWrapper<CaseWorkItemType> parent,
                                                                         PrismContainerValue<CaseWorkItemType> value, ValueStatus status, WrapperContext context) throws SchemaException {
        context.setCreateIfEmpty(false);
        return super.createValueWrapper(parent, value, status, context);
    }


}
