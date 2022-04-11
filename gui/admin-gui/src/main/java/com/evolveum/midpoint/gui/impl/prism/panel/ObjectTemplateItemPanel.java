/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MappingColumnPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

public class ObjectTemplateItemPanel extends ItemRefinedPanel<ObjectTemplateItemDefinitionType> {

    public ObjectTemplateItemPanel(String id, IModel<PrismContainerWrapper<ObjectTemplateItemDefinitionType>> model) {
        super(id, model, null);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ObjectTemplateItemDefinitionType>, String>> createAdditionalColumns() {
        List<IColumn<PrismContainerValueWrapper<ObjectTemplateItemDefinitionType>, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<>(createStringResource("ObjectTemplateItemDefinitionType.mapping")) {

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<ObjectTemplateItemDefinitionType>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<ObjectTemplateItemDefinitionType>> rowModel) {
                IModel<PrismContainerWrapper<MappingType>> mappingModel = PrismContainerWrapperModel.fromContainerValueWrapper(rowModel, ObjectTemplateItemDefinitionType.F_MAPPING);
                cellItem.add(new MappingColumnPanel(componentId, mappingModel));
            }
        });

        return columns;
    }

    @Override
    protected List<InlineMenuItem> createRefinedItemInlineMenu(List<InlineMenuItem> defaultActions) {
        List<InlineMenuItem> items = new ArrayList<>();
        ButtonInlineMenuItem item = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_DELETE_MENU_ITEM);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        getMultivalueContainerListPanel().deleteItemPerformed(target,
                                getMultivalueContainerListPanel().getPerformedSelectedItems(getRowModel()));
                    }
                };
            }
        };
        items.add(item);
        item = new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        getMultivalueContainerListPanel().editItemPerformed(target, getRowModel(),
                                getMultivalueContainerListPanel().getSelectedObjects());
                        target.add(getMultivalueContainerListPanel().getFeedbackPanel());
                    }
                };
            }
        };
        items.add(item);
        return items;
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected boolean allowLinkForFirstColumn() {
        return true;
    }

    @Override
    protected ItemVisibility getItemVisibilityFoeBasicPanel(ItemWrapper<?, ?> itemWrapper) {
        if (itemWrapper.getPath().removeIds().isSubPathOrEquivalent(ItemPath.create(ObjectTemplateType.F_ITEM, ObjectTemplateItemDefinitionType.F_META))
                || itemWrapper.getPath().removeIds().equivalent(
                        ItemPath.create(ObjectTemplateType.F_ITEM, ObjectTemplateItemDefinitionType.F_MAPPING, ObjectTemplateMappingType.F_METADATA_MAPPING))){
            return ItemVisibility.HIDDEN;
        }
        return super.getItemVisibilityFoeBasicPanel(itemWrapper);
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<ObjectTemplateItemDefinitionType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }
}
