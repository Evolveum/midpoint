/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class InlineMenuColumn<T extends InlineMenuable> extends AbstractColumn<T, String> {

    public InlineMenuColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
                             IModel<T> rowModel) {

//        InlineMenu inlineMenu = new com.evolveum.midpoint.web.component.menu.cog.InlineMenu(componentId,
//                createMenuModel(rowModel));
        DropdownButtonDto model = new DropdownButtonDto(null, null, null, createMenuModel(rowModel).getObject());
        DropdownButtonPanel inlineMenu = new DropdownButtonPanel(componentId, model) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getSpecialButtonClass() {
                return "btn-xs btn-default";
            }
        };
        inlineMenu.setOutputMarkupId(true);
        inlineMenu.setOutputMarkupPlaceholderTag(true);
        cellItem.add(inlineMenu);
    }

    private IModel<List<InlineMenuItem>> createMenuModel(final IModel<T> rowModel) {
        return new LoadableModel<List<InlineMenuItem>>(false) {

            @Override
            public List<InlineMenuItem> load() {
                if (!(rowModel.getObject() instanceof InlineMenuable)) {
                    return new ArrayList<>();
                }

                T row = rowModel.getObject();
                if (row.getMenuItems() == null) {
                    return new ArrayList<>();
                }

                for (InlineMenuItem item : row.getMenuItems()) {
                    if (!(item.getAction() instanceof ColumnMenuAction)) {
                        continue;
                    }

                    ColumnMenuAction action = (ColumnMenuAction) item.getAction();
                    action.setRowModel(rowModel);
                }

                return row.getMenuItems();
            }
        };
    }

    @Override
    public String getCssClass() {
        return "cog-xs";
    }
}
