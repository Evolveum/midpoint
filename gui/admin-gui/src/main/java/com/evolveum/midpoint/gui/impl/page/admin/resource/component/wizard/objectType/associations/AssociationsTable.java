/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.associations;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@Experimental
public abstract class AssociationsTable extends MultivalueContainerListPanel<ResourceObjectAssociationType> {

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public AssociationsTable(
            String id,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, ResourceObjectAssociationType.class);
        this.valueModel = valueModel;
    }

    @Override
    protected boolean isHeaderVisible() {
        return false;
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttons = super.createToolbarButtonsList(idButton);
        buttons.forEach(button -> {
            if (button instanceof AjaxIconButton) {
                ((AjaxIconButton) button).showTitleAsLabel(true);
            }
        });
        return buttons;
    }

    @Override
    protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec) {
        createNewAssociation(target);
        refreshTable(target);
    }

    protected PrismContainerValueWrapper createNewAssociation(AjaxRequestTarget target) {
        PrismContainerWrapper<ResourceObjectAssociationType> container = getContainerModel().getObject();
        PrismContainerValue<ResourceObjectAssociationType> newReaction = container.getItem().createNewValue();
        PrismContainerValueWrapper<ResourceObjectAssociationType> newReactionWrapper =
                createNewItemContainerValueWrapper(newReaction, container, target);
        return newReactionWrapper;
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();

        InlineMenuItem item = new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createEditColumnAction();
            }
        };
        items.add(item);

        items.add(new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createDeleteColumnAction();
            }
        });
        return items;
    }

    @Override
    protected IModel<PrismContainerWrapper<ResourceObjectAssociationType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(
                valueModel,
                ResourceObjectTypeDefinitionType.F_ASSOCIATION);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ResourceObjectAssociationType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<ResourceObjectAssociationType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<ResourceObjectAssociationType>> associationsDef = getAssociationsDefinition();
        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_REF,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_DISPLAY_NAME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_KIND,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_INTENT,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_DIRECTION,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_ASSOCIATION_ATTRIBUTE,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceObjectAssociationType, String>(
                associationsDef,
                ResourceObjectAssociationType.F_VALUE_ATTRIBUTE,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        return columns;
    }

    protected LoadableModel<PrismContainerDefinition<ResourceObjectAssociationType>> getAssociationsDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<ResourceObjectAssociationType> load() {
                return valueModel.getObject().getDefinition().findContainerDefinition(
                        ResourceObjectTypeDefinitionType.F_ASSOCIATION);
            }
        };
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_CORRELATION_ITEMS_WIZARD;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "AssociationsTable.newObject";
    }
}
