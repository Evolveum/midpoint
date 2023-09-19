/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;

import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefinedDefinitionType;

public class ItemRefinedPanel<C extends ItemRefinedDefinitionType> extends BasePanel<PrismContainerWrapper<C>> {

    private static final String ID_TABLE = "table";

    private ContainerPanelConfigurationType config;

    public ItemRefinedPanel(String id, IModel<PrismContainerWrapper<C>> model, ContainerPanelConfigurationType config) {
        super(id, model);
        this.config = config;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MultivalueContainerListPanelWithDetailsPanel<C> table = new MultivalueContainerListPanelWithDetailsPanel<C>(ID_TABLE, getClassType()) {

            @Override
            protected MultivalueContainerDetailsPanel<C> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<C>> item) {
                return new MultivalueContainerDetailsPanel<>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true) {

                    @Override
                    protected DisplayNamePanel<C> createDisplayNamePanel(String displayNamePanelId) {
                        ItemRealValueModel<C> displayNameModel =
                                new ItemRealValueModel<>(item.getModel());
                        return new DisplayNamePanel<>(displayNamePanelId, displayNameModel) {
                            @Override
                            protected IModel<String> createHeaderModel() {
                                return createDisplayNameForRefinedItem(getModelObject());
                            }

                            @Override
                            protected IModel<String> getDescriptionLabelModel() {
                                return new ReadOnlyModel<>(() -> getModelObject().getDescription());
                            }
                        };
                    }

                    @Override
                    protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) {
                        return getItemVisibilityFoeBasicPanel(itemWrapper);
                    }
                };
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return ItemRefinedPanel.this.isCreateNewObjectVisible();
            }

            @Override
            protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSepc) {
                if (customNewItemPerformed(target, relationSepc)) {
                    return;
                }
                super.newItemPerformed(target, relationSepc);
            }

            @Override
            protected IModel<PrismContainerWrapper<C>> getContainerModel() {
                return ItemRefinedPanel.this.getModel();
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<C>, String>> createDefaultColumns() {
                List<IColumn<PrismContainerValueWrapper<C>, String>> columns = new ArrayList<>();

                columns.add(new PrismPropertyWrapperColumn<>(ItemRefinedPanel.this.getModel(), ItemRefinedDefinitionType.F_DISPLAY_NAME, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
                columns.add(new PrismPropertyWrapperColumn<>(ItemRefinedPanel.this.getModel(), ItemRefinedDefinitionType.F_DESCRIPTION, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
                columns.addAll(createAdditionalColumns());
                return columns;
            }

            @Override
            protected IColumn<PrismContainerValueWrapper<C>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
                return new PrismPropertyWrapperColumn<>(getContainerModel(), ItemRefinedDefinitionType.F_REF,
                        allowLinkForFirstColumn() ? AbstractItemWrapperColumn.ColumnType.LINK : AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()) {
                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> model) {
                        itemDetailsPerformed(target, model);
                    }
                };
            }

            @Override
            protected IColumn<PrismContainerValueWrapper<C>, String> createCheckboxColumn() {
                return ItemRefinedPanel.this.createCheckboxColumn();
            }

            @Override
            public void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> rowModel, List<PrismContainerValueWrapper<C>> listItems) {
                if (customEditItemPerformed(target, rowModel, listItems)) {
                    return;
                }
                super.editItemPerformed(target, rowModel, listItems);
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return createRefinedItemInlineMenu(getDefaultMenuActions());
            }
        };
        add(table);
    }

    protected boolean customNewItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSepc) {
        return false;
    }

    private IModel<String> createDisplayNameForRefinedItem(C refinedItem) {
        return new ReadOnlyModel<>(() -> {
            if (refinedItem.getDisplayName() != null) {
                return refinedItem.getDisplayName();
            }

            if (refinedItem.getRef() != null) {
                return refinedItem.getRef().toString();
            }

            return getPageBase().createStringResource("feedbackMessagePanel.message.undefined").getString();
        });

    }

    protected boolean isCreateNewObjectVisible() {
        return false;
    }

    protected List<InlineMenuItem> getMenuActions() {
        return null;
    }

    protected List<InlineMenuItem> createRefinedItemInlineMenu(List<InlineMenuItem> defaultActions) {
        return defaultActions;
    }

    public ContainerPanelConfigurationType getConfig() {
        return config;
    }

    protected boolean customEditItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> rowModel, List<PrismContainerValueWrapper<C>> listItmes) {
        return false;
    }

    protected IColumn<PrismContainerValueWrapper<C>, String> createCheckboxColumn() {
        return null;
    }

    protected ItemVisibility getItemVisibilityFoeBasicPanel(ItemWrapper<?, ?> itemWrapper) {
        return ItemVisibility.AUTO;
    }

    protected boolean allowLinkForFirstColumn() {
        return true;
    }

    protected List<IColumn<PrismContainerValueWrapper<C>, String>> createAdditionalColumns() {
        return Collections.emptyList();
    }

    private Class<C> getClassType() {
        return (Class<C>) ItemRefinedDefinitionType.class;
    }

    protected MultivalueContainerListPanelWithDetailsPanel getMultivalueContainerListPanel() {
        return (MultivalueContainerListPanelWithDetailsPanel) get(ID_TABLE);
    }
}
