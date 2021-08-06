/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MappingColumnPanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

public class ResourceAttributePanel extends ItemRefinedPanel<ResourceAttributeDefinitionType> {

    public ResourceAttributePanel(String id, IModel<PrismContainerWrapper<ResourceAttributeDefinitionType>> model) {
        super(id, model);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ResourceAttributeDefinitionType>, String>> createAdditionalColumns() {
        List<IColumn<PrismContainerValueWrapper<ResourceAttributeDefinitionType>, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<>(createStringResource("Outbound")) {

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<ResourceAttributeDefinitionType>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> rowModel) {
                IModel<PrismContainerWrapper<MappingType>> mappingModel = PrismContainerWrapperModel.fromContainerValueWrapper(rowModel, ResourceAttributeDefinitionType.F_OUTBOUND);
                cellItem.add(new MappingColumnPanel(componentId, mappingModel));
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("Inbound")) {

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<ResourceAttributeDefinitionType>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> rowModel) {
                IModel<PrismContainerWrapper<MappingType>> mappingModel = PrismContainerWrapperModel.fromContainerValueWrapper(rowModel, ResourceAttributeDefinitionType.F_INBOUND);
                cellItem.add(new MappingColumnPanel(componentId, mappingModel));
            }
        });
        return columns;
    }
}
