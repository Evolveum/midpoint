/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.MenuDividerPanel;
import com.evolveum.midpoint.web.component.menu.cog.MenuLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class MenuMultiButtonPanel<T extends Serializable> extends MultiButtonPanel<T> {
    private static final String ID_INLINE_MENU_PANEL = "inlineMenuPanel";
    private static final String ID_MENU_ITEM_BODY = "menuItemBody";
    private static final String ID_MENU_BUTTON_CONTAINER = "menuButtonContainer";

    public MenuMultiButtonPanel(String id, int buttonsCount, IModel<T> model, IModel<List<InlineMenuItem>> menuItemsModel) {
        super(id, buttonsCount, model, menuItemsModel);
    }

    @Override
    protected void initLayout() {
        super.initLayout();

        InlineMenu inlineMenu = new InlineMenu(ID_INLINE_MENU_PANEL, menuItemsModel){
            @Override
            protected String getIconClass(){
                return "fa fa-ellipsis-h";
            }
           @Override
            protected String getAdditionalButtonClass(){
                return "btn btn-default btn-xs";
            }

           @Override
           protected String getMenuItemContainerClass(){
               return "none";
            }

           @Override
           protected String getMenuItemButtonStyle(){
               return "";
            }

           @Override
           protected String getMenuItemContainerStyle(){
               return "margin-left: -37px; margin-bottom: -3px; list-style: none;";
            }

        };
        inlineMenu.add(new VisibleEnableBehaviour(){
           @Override
            public boolean isVisible(){
               return !(numberOfButtons < 2);
           }
        });
        add(inlineMenu);

    }
}
