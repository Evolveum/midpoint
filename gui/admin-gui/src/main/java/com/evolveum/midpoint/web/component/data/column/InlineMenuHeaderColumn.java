/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.List;

/**
 * @author lazyman
 */
public class InlineMenuHeaderColumn<T extends InlineMenuable> extends InlineMenuColumn<T> {

    private IModel<List<InlineMenuItem>> menuItems;

    public InlineMenuHeaderColumn(List<InlineMenuItem> menuItems) {
        super(null);
        this.menuItems = new Model((Serializable) menuItems);
    }

    @Override
    public Component getHeader(String componentId) {
//        InlineMenu inlineMenu = new com.evolveum.midpoint.web.component.menu.cog.InlineMenu(componentId, menuItems);
        DropdownButtonDto model = new DropdownButtonDto(null, null, null, menuItems.getObject());
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
    public String getCssClass() {
        return "cog-xs";
    }
}
