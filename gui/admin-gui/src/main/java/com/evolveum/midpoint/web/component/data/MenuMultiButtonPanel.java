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
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.List;

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
                new DropdownButtonDto(null, null, null, menuItemsModel.getObject())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected String getSpecialButtonClass() {
                return "btn-xs btn-default";
            }

        };
        add(inlineMenu);

        inlineMenu.add(new VisibleBehaviour(() -> !(getNumberOfButtons() <= 2) || menuItemsModel.getObject().size() > 2));
    }

}
