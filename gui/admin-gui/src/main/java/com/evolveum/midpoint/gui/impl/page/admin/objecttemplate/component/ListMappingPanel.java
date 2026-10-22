/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.objecttemplate.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.AbstractMappingsTable;

import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.MappingExpressionColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.model.Model;

public class ListMappingPanel extends AbstractMappingsTable<ObjectTemplateType> {

    public ListMappingPanel(String id, IModel<PrismContainerValueWrapper<ObjectTemplateType>> valueModel, ContainerPanelConfigurationType config) {
        super(id, valueModel, config);
    }

    @Override
    protected String getTableContainerAdditionalCssClasses() {
        return super.getTableContainerAdditionalCssClasses() + " table-td-middle";
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return null;
    }

    @Override
    protected Collection<? extends IColumn<PrismContainerValueWrapper<MappingType>, String>> createCustomColumns() {

        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        IModel<PrismContainerDefinition<MappingType>> mappingTypeDef =
                getMappingTypeDefinition();

        columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                mappingTypeDef,
                MappingType.F_SOURCE,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new IconColumn<>(Model.of()) {
            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                return new DisplayType().beginIcon().cssClass("fa fa-minus text-secondary").end();
            }

            @Override
            public String getCssClass() {
                return "px-0";
            }
        });

        columns.add(new MappingExpressionColumn<>(
                mappingTypeDef,
                MappingType.F_EXPRESSION,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new IconColumn<>(Model.of()) {
            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                return new DisplayType().beginIcon().cssClass("fa fa-arrow-right-long text-secondary").end();
            }

            @Override
            public String getCssClass() {
                return "px-0";
            }
        });

        columns.add(createTargetColumn());

        return columns;
    }

    @Override
    protected IModel<PrismContainerWrapper<MappingType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(getValueModel(), ObjectTemplateType.F_MAPPING);
    }

    @Override
    protected MultivalueContainerDetailsPanel<MappingType> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<MappingType>> item) {
        return new MultivalueContainerDetailsPanel<>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true) {

            @Override
            protected DisplayNamePanel<MappingType> createDisplayNamePanel(String displayNamePanelId) {
                ItemRealValueModel<MappingType> displayNameModel =
                        new ItemRealValueModel<>(item.getModel());
                return new DisplayNamePanel<>(displayNamePanelId, displayNameModel) {
                    @Override
                    protected IModel<String> createHeaderModel() {
                        IModel<String> headerModel = super.createHeaderModel();
                        if (StringUtils.isEmpty(headerModel.getObject())) {
                            return getPageBase().createStringResource("feedbackMessagePanel.message.undefined");
                        }
                        return headerModel;
                    }
                };
            }

            @Override
            protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) {
                if (itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, MappingType.F_METADATA_MAPPING))) {
                    return ItemVisibility.HIDDEN;
                }
                return ItemVisibility.AUTO;
            }

            @Override
            protected List<ITab> createTabs() {
                List<ITab> tabs = super.createTabs();
                tabs.add(new PanelTab(createStringResource("MappingRangePanel.range")) {

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new MappingRangePanel(panelId, getModel());
                    }
                });
                return tabs;
            }
        };
    }

    @Override
    public void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<MappingType>> rowModel, List<PrismContainerValueWrapper<MappingType>> listItems) {
        showDetailsPanel(target, rowModel, listItems);
    }
}
