/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author honchar
 * @author Viliam Repan (lazyman)
 */
public class MenuMultiButtonPanel<T extends Serializable> extends MultiButtonPanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_INLINE_MENU_PANEL = "inlineMenuPanel";

    private IModel<List<InlineMenuItem>> menuItemsModel;

    public MenuMultiButtonPanel(String id, IModel<T> model, int numberOfButtons, IModel<List<InlineMenuItem>> menuItemsModel) {
        super(id, model, numberOfButtons);

        this.menuItemsModel = menuItemsModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        DropdownButtonPanel inlineMenu = new DropdownButtonPanel(ID_INLINE_MENU_PANEL,
                new DropdownButtonDto(null, getDropDownButtonIcon(), null, menuItemsModel.getObject())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected String getSpecialButtonClass() {
                return MenuMultiButtonPanel.this.getSpecialButtonClass();
            }

            @Override
            protected boolean hasToggleIcon() {
                if(getDropDownButtonIcon() != null){
                    return false;
                }
                return super.hasToggleIcon();
            }

            @Override
            protected void onBeforeClickMenuItem(AjaxRequestTarget target, InlineMenuItemAction action, IModel<? extends InlineMenuItem> item) {
                MenuMultiButtonPanel.this.onBeforeClickMenuItem(target, action, item);
            }
        };
        add(inlineMenu);

        inlineMenu.add(new VisibleBehaviour(() -> {

            List<InlineMenuItem> menuItems = menuItemsModel.getObject();
            for (InlineMenuItem menuItem : menuItems) {
                if (!(menuItem instanceof ButtonInlineMenuItem)){
                    return true;
                }
            }
            return false;
        }));
    }

    protected void onBeforeClickMenuItem(AjaxRequestTarget target, InlineMenuItemAction action, IModel<? extends InlineMenuItem> item) {
    }

    protected String getDropDownButtonIcon() {
        return null;
    }

    protected String getSpecialButtonClass() {
        return "btn-xs btn-default";
    }


}
