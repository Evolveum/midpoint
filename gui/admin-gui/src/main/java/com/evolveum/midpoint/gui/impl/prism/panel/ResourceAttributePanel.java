/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MappingColumnPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

public class ResourceAttributePanel extends BasePanel<PrismContainerWrapper<ResourceAttributeDefinitionType>> {

    private static final String ID_TABLE = "table";

    public ResourceAttributePanel(String id, IModel<PrismContainerWrapper<ResourceAttributeDefinitionType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MultivalueContainerListPanelWithDetailsPanel<ResourceAttributeDefinitionType> table = new MultivalueContainerListPanelWithDetailsPanel<ResourceAttributeDefinitionType>(ID_TABLE, ResourceAttributeDefinitionType.class) {

            @Override
            protected MultivalueContainerDetailsPanel<ResourceAttributeDefinitionType> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> item) {
                return new MultivalueContainerDetailsPanel<>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true) {

                    @Override
                    protected DisplayNamePanel<ResourceAttributeDefinitionType> createDisplayNamePanel(String displayNamePanelId) {
                        ItemRealValueModel<ResourceAttributeDefinitionType> displayNameModel =
                                new ItemRealValueModel<>(item.getModel());
                        return new DisplayNamePanel<>(displayNamePanelId, displayNameModel);
                    }
                };
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return true;
            }

            @Override
            protected IModel<PrismContainerWrapper<ResourceAttributeDefinitionType>> getContainerModel() {
                return ResourceAttributePanel.this.getModel();
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<ResourceAttributeDefinitionType>, String>> createDefaultColumns() {
                List<IColumn<PrismContainerValueWrapper<ResourceAttributeDefinitionType>, String>> columns = new ArrayList<>();

                columns.add(new PrismPropertyWrapperColumn<>(ResourceAttributePanel.this.getModel(), ResourceAttributeDefinitionType.F_REF, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
                columns.add(new PrismPropertyWrapperColumn<>(ResourceAttributePanel.this.getModel(), ResourceAttributeDefinitionType.F_DISPLAY_NAME, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
                columns.add(new PrismPropertyWrapperColumn<>(ResourceAttributePanel.this.getModel(), ResourceAttributeDefinitionType.F_DESCRIPTION, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
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
        };
        add(table);
    }
}
