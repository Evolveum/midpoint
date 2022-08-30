/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MappingColumnPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

public class ResourceAssociationPanel extends ItemRefinedPanel<ResourceObjectAssociationType> {

    public ResourceAssociationPanel(String id, IModel<PrismContainerWrapper<ResourceObjectAssociationType>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ResourceObjectAssociationType>, String>> createAdditionalColumns() {
        List<IColumn<PrismContainerValueWrapper<ResourceObjectAssociationType>, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<>(createStringResource("Outbound")) {

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<ResourceObjectAssociationType>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<ResourceObjectAssociationType>> rowModel) {
                IModel<PrismContainerWrapper<MappingType>> mappingModel = PrismContainerWrapperModel.fromContainerValueWrapper(rowModel, ResourceObjectAssociationType.F_OUTBOUND);
                cellItem.add(new MappingColumnPanel(componentId, mappingModel));
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("Inbound")) {

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<ResourceObjectAssociationType>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<ResourceObjectAssociationType>> rowModel) {
                IModel<PrismContainerWrapper<MappingType>> mappingModel = PrismContainerWrapperModel.fromContainerValueWrapper(rowModel, ResourceObjectAssociationType.F_INBOUND);
                cellItem.add(new MappingColumnPanel(componentId, mappingModel));
            }
        });
        return columns;
    }

    @Override
    protected boolean customEditItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectAssociationType>> rowModel, List<PrismContainerValueWrapper<ResourceObjectAssociationType>> listItems) {
        if (getConfig() == null) {
            return false;
        }

        AbstractPageObjectDetails parent = findParent(AbstractPageObjectDetails.class);
        if (parent == null) {
            return false;
        }

        ContainerPanelConfigurationType detailsPanel = new ContainerPanelConfigurationType();
        detailsPanel.setPanelType("attributeDefinitionDetails");

        PrismContainerValueWrapper<ResourceObjectAssociationType> attrDef;
        if (rowModel != null) {
            attrDef = rowModel.getObject();
        }  else {
            attrDef = listItems.iterator().next();
        }
//                VirtualContainersSpecificationType virtualContainer = new VirtualContainersSpecificationType(getPrismContext());
        detailsPanel.setPath(new ItemPathType(attrDef.getPath()));

        //                  detailsPanel.getContainer().add(virtualContainer);

        detailsPanel.setIdentifier("attributeDefinitionDetails");
        DisplayType displayType = new DisplayType();
        displayType.setLabel(new PolyStringType(attrDef.getDisplayName()));
        IconType icon = new IconType();
        icon.setCssClass("fa fa-navicon");
        displayType.setIcon(icon);
        detailsPanel.setDisplay(displayType);

        getPageBase().getSessionStorage().setObjectDetailsStorage("details" + parent.getType().getSimpleName(), detailsPanel);

        ResourceAssociationPanel.this.getConfig().getPanel().add(detailsPanel);
        target.add(parent);
        parent.replacePanel(detailsPanel, target);
        return true;
    }
}
