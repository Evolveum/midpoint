/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ResourceAttributePanel;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ResourceSchemaHandlingTabPanel extends BasePanel<PrismContainerWrapper<SchemaHandlingType>> {

    private static final String ID_TABLE = "table";
    private static final String ID_FORM = "form";

    public ResourceSchemaHandlingTabPanel(String id, IModel<PrismContainerWrapper<SchemaHandlingType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        add(form);

        MultivalueContainerListPanelWithDetailsPanel<ResourceObjectTypeDefinitionType> objectTypesPanel = new MultivalueContainerListPanelWithDetailsPanel<>(ID_TABLE, ResourceObjectTypeDefinitionType.class) {

            @Override
            protected MultivalueContainerDetailsPanel<ResourceObjectTypeDefinitionType> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> item) {
                return createMultivalueContainerDetailsPanel(ID_ITEM_DETAILS, item.getModel());
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> getContainerModel() {
                return PrismContainerWrapperModel.fromContainerWrapper(ResourceSchemaHandlingTabPanel.this.getModel(), SchemaHandlingType.F_OBJECT_TYPE);
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, String>> createDefaultColumns() {
                List<IColumn<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, String>> columns = new ArrayList<>();
                columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), ResourceObjectTypeDefinitionType.F_DISPLAY_NAME, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
                columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), ResourceObjectTypeDefinitionType.F_KIND, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
                columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), ResourceObjectTypeDefinitionType.F_INTENT, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
                columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), ResourceObjectTypeDefinitionType.F_DEFAULT, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
                columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), ResourceObjectTypeDefinitionType.F_DESCRIPTION, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
                List<InlineMenuItem> menuActionsList = getMultivalueContainerListPanel().getDefaultMenuActions();
                columns.add(new InlineMenuButtonColumn(menuActionsList, getPageBase()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getCssClass() {
                        return " col-md-1 ";
                    }

                });
                return columns;
            }
        };
        form.add(objectTypesPanel);
    }

    private MultivalueContainerListPanelWithDetailsPanel<ResourceObjectTypeDefinitionType> getMultivalueContainerListPanel(){
        return ((MultivalueContainerListPanelWithDetailsPanel<ResourceObjectTypeDefinitionType>)get(createComponentPath(ID_FORM, ID_TABLE)));
    }

    private MultivalueContainerDetailsPanel<ResourceObjectTypeDefinitionType> createMultivalueContainerDetailsPanel(String panelId, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model) {
        return new MultivalueContainerDetailsPanel<>(panelId, model, true) {

            @Override
            protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) {
                if (itemWrapper instanceof PrismContainerWrapper) {
                    return ItemVisibility.HIDDEN;
                }
                return ItemVisibility.AUTO;
            }

            @Override
            protected @NotNull List<ITab> createTabs() {
                List<ITab> tabs = new ArrayList<>();
                tabs.add(new PanelTab(createStringResource("Attributes")) {

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new ResourceAttributePanel(panelId, PrismContainerWrapperModel.fromContainerValueWrapper(getModel(), ResourceObjectTypeDefinitionType.F_ATTRIBUTE));
                    }
                });
                return tabs;
            }

            @Override
            protected DisplayNamePanel<ResourceObjectTypeDefinitionType> createDisplayNamePanel(String displayNamePanelId) {
                return new DisplayNamePanel<>(displayNamePanelId, new ItemRealValueModel<>(getModel()));
            }
        };
    }
}
