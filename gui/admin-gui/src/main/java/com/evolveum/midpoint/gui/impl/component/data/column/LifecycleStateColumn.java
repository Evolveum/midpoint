/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.SimulationModePanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

public class LifecycleStateColumn<C extends Containerable> extends PrismPropertyWrapperColumn<C, String> {

    public LifecycleStateColumn(IModel<? extends PrismContainerDefinition<C>> mainModel, PageBase pageBase) {
        super(mainModel, ObjectType.F_LIFECYCLE_STATE, ColumnType.VALUE, pageBase);
    }

    @Override
    protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
        SimulationModePanel panel =  new SimulationModePanel(componentId, (IModel<PrismPropertyWrapper<String>>) rowModel);
        panel.add(AttributeAppender.append("class", "d-inline"));
        return panel;
    }

//    @Override
//    public Component getHeader(String componentId) {
//        return new Label(componentId, getPageBase().createStringResource("PrismPropertyWrapperColumn.column.mode"));
//    }
}
