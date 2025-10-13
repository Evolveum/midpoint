/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

/**
 * @author lazyman
 */
public class InlineMenuHeaderColumn<T> extends AbstractColumn<T, String> {

    private List<InlineMenuItem> menuItems;

    public InlineMenuHeaderColumn(List<InlineMenuItem> menuItems) {
        super(null);
        this.menuItems = menuItems;
    }

    @Override
    public Component getHeader(String componentId) {
        DropdownButtonDto model = new DropdownButtonDto(null, null, null, menuItems);
        DropdownButtonPanel inlineMenu = new DropdownButtonPanel(componentId, model) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getSpecialButtonClass() {
                return "btn-xs btn-default";
            }
        };
        inlineMenu.setOutputMarkupPlaceholderTag(true);
        inlineMenu.setOutputMarkupId(true);
        return inlineMenu;
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, IModel<T> rowModel) {
        cellItem.add(new Label(componentId)); //this is hack, TODO: implement header vs. row inline menu visibility correctly
    }

    @Override
    public String getCssClass() {
        return "cog-xs";
    }
}
