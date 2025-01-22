/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.objecttemplate.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;

import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoiceColumnPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.AbstractMappingsTable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.model.Model;

public class ListMappingPanel extends AbstractMappingsTable<ObjectTemplateType> {

    public ListMappingPanel(String id, IModel<PrismContainerValueWrapper<ObjectTemplateType>> valueModel, ContainerPanelConfigurationType config) {
        super(id, valueModel, config);
    }

//    @Override
//    protected void onInitialize() {
//        super.onInitialize();
//        initLayout();
//    }
//
//    private void initLayout() {
//        MultivalueContainerListPanelWithDetailsPanel<MappingType> table = new MultivalueContainerListPanelWithDetailsPanel<MappingType>(ID_TABLE, MappingType.class) {
//
//            @Override
//            protected MultivalueContainerDetailsPanel<MappingType> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<MappingType>> item) {
//                return new MultivalueContainerDetailsPanel<>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true) {
//
//                    @Override
//                    protected DisplayNamePanel<MappingType> createDisplayNamePanel(String displayNamePanelId) {
//                        ItemRealValueModel<MappingType> displayNameModel =
//                                new ItemRealValueModel<>(item.getModel());
//                        return new DisplayNamePanel<>(displayNamePanelId, displayNameModel) {
//                            @Override
//                            protected IModel<String> createHeaderModel() {
//                                IModel<String> headerModel = super.createHeaderModel();
//                                if (StringUtils.isEmpty(headerModel.getObject())) {
//                                    return getPageBase().createStringResource("feedbackMessagePanel.message.undefined");
//                                }
//                                return headerModel;
//                            }
//                        };
//                    }
//
//                    @Override
//                    protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) {
//                        if (itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, MappingType.F_METADATA_MAPPING))) {
//                            return ItemVisibility.HIDDEN;
//                        }
//                        return ItemVisibility.AUTO;
//                    }
//                };
//            }
//
//            @Override
//            protected boolean isCreateNewObjectVisible() {
//                return true;
//            }
//
//            @Override
//            protected IModel<PrismContainerWrapper<MappingType>> getContainerModel() {
//                return ListMappingPanel.this.getModel();
//            }
//
//            @Override
//            protected UserProfileStorage.TableId getTableId() {
//                return null;
//            }
//
//            @Override
//            protected List<IColumn<PrismContainerValueWrapper<MappingType>, String>> createDefaultColumns() {
//                List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();
//
//                columns.add(new PrismPropertyWrapperColumn<>(ListMappingPanel.this.getModel(), MappingType.F_DESCRIPTION, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
//                columns.add(new AbstractColumn<>(createStringResource("ListMappingPanel.mappingDescription")) {
//
//                    @Override
//                    public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<MappingType>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
//                        cellItem.add(new Label(componentId, WebComponentUtil.createMappingDescription(rowModel)));
//                    }
//                });
//
//                columns.add(new LifecycleStateColumn<>(getContainerModel(), getPageBase()));
//
//                List<InlineMenuItem> items = new ArrayList<>();
//                InlineMenuItem item = new InlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
//                    private static final long serialVersionUID = 1L;
//
//                    @Override
//                    public InlineMenuItemAction initAction() {
//                        return new ColumnMenuAction() {
//                            private static final long serialVersionUID = 1L;
//
//                            @Override
//                            public void onClick(AjaxRequestTarget target) {
//                                deleteItemPerformed(target, getPerformedSelectedItems(getRowModel()));
//                            }
//                        };
//                    }
//                };
//                items.add(item);
//                item = new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
//                    private static final long serialVersionUID = 1L;
//
//                    @Override
//                    public CompositedIconBuilder getIconCompositedBuilder() {
//                        return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
//                    }
//
//                    @Override
//                    public InlineMenuItemAction initAction() {
//                        return new ColumnMenuAction() {
//                            private static final long serialVersionUID = 1L;
//
//                            @Override
//                            public void onClick(AjaxRequestTarget target) {
//                                editItemPerformed(target, getRowModel(), getSelectedObjects());
//                                target.add(getFeedbackPanel());
//                            }
//                        };
//                    }
//                };
//                items.add(item);
//
//                columns.add(new InlineMenuButtonColumn(items, getPageBase()) {
//                    @Override
//                    public String getCssClass() {
//                        return "col-xs-1";
//                    }
//                });
//
//                return columns;
//            }
//
//            @Override
//            protected IColumn<PrismContainerValueWrapper<MappingType>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
//                return new PrismPropertyWrapperColumn<>(ListMappingPanel.this.getModel(), MappingType.F_NAME, AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {
//                    @Override
//                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<MappingType>> model) {
//                        itemDetailsPerformed(target, model);
//                    }
//                };
//            }
//
//            @Override
//            protected IColumn<PrismContainerValueWrapper<MappingType>, String> createCheckboxColumn() {
//                return new CheckBoxHeaderColumn<>();
//            }
//
//            @Override
//            protected PrismContainerDefinition<MappingType> getTypeDefinitionForSearch() {
//                return getPrismContext().getSchemaRegistry().findContainerDefinitionByType(MappingType.COMPLEX_TYPE);
//            }
//        };
//        add(table);
//    }

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

        columns.add(new PrismPropertyWrapperColumn<>(
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

        columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                mappingTypeDef,
                MappingType.F_TARGET,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

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
        };
    }

    @Override
    public void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<MappingType>> rowModel, List<PrismContainerValueWrapper<MappingType>> listItems) {
        showDetailsPanel(target, rowModel, listItems);
    }
}
