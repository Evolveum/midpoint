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

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 * <p>
 * todo rewrite
 */
public class MenuMultiButtonPanel<T extends Serializable> extends MultiButtonPanel<T> {
	private static final long serialVersionUID = 1L;

	private static final String ID_INLINE_MENU_PANEL = "inlineMenuPanel";
    private static final String ID_MENU_ITEM_BODY = "menuItemBody";
    private static final String ID_MENU_BUTTON_CONTAINER = "menuButtonContainer";

    public MenuMultiButtonPanel(String id, int buttonsCount, IModel<T> model, IModel<List<InlineMenuItem>> menuItemsModel) {
        super(id, buttonsCount, model, menuItemsModel);
    }

    @Override
    protected void initLayout() {
        super.initLayout();
        
        DropdownButtonPanel inlineMenu = new DropdownButtonPanel(ID_INLINE_MENU_PANEL,
				new DropdownButtonDto(null, null, null, menuItemsModel.getObject())) {
        	private static final long serialVersionUID = 1L;
        	
        	@Override
        	protected String getSpecialButtonClass() {
        		return "btn-xs btn-default";
        	}
        	
        };

        inlineMenu.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isVisible() {
                return !(numberOfButtons < 2);
            }
        });
        add(inlineMenu);

    }
}
