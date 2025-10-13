/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.input.LifecycleStatePanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

public class LifecycleStateColumn<C extends Containerable> extends PrismPropertyWrapperColumn<C, String> {

    public LifecycleStateColumn(IModel<? extends PrismContainerDefinition<C>> mainModel, PageBase pageBase) {
        super(mainModel, ObjectType.F_LIFECYCLE_STATE, ColumnType.VALUE, pageBase);
    }

    @Override
    protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
        return new LifecycleStatePanel(componentId, (IModel<PrismPropertyWrapper<String>>) rowModel);
    }
}
