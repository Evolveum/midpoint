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
package com.evolveum.midpoint.gui.api.component.button;

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.MenuLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Universal button to display drop-down menus. The button itself can have numerous decorations: icon, label and tag with count (info)
 * 
 * @author katkav
 *
 */
public class DropdownButtonPanel extends BasePanel<DropdownButtonDto> {

	private static final long serialVersionUID = 1L;
	private static final String ID_BUTTON_CONTAINER = "buttonContainer";
	private static final String ID_INFO = "info";
	private static final String ID_ICON = "icon";
	private static final String ID_LABEL = "label";

	private static String ID_MENU_ITEM = "menuItem";
    private static String ID_MENU_ITEM_BODY = "menuItemBody";


	public DropdownButtonPanel(String id, DropdownButtonDto model) {
		super(id);
		initLayout(model);
	}

	private void initLayout(DropdownButtonDto model) {
		WebMarkupContainer buttonContainer = new WebMarkupContainer(ID_BUTTON_CONTAINER);
		buttonContainer.setOutputMarkupId(true);
		buttonContainer.add(AttributeAppender.append("class", getSpecialButtonClass()));
		add(buttonContainer);

		Label info = new Label(ID_INFO, model.getInfo());
		info.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;
			@Override
            public boolean isVisible() {
                return model.getInfo() != null;
            }
        });
		buttonContainer.add(info);

		Label label = new Label(ID_LABEL, model.getLabel());
		label.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;
			@Override
            public boolean isVisible() {
                return model.getLabel() != null;
            }
        });
		buttonContainer.add(label);

		WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
		icon.add(AttributeModifier.append("class", model.getIcon()));
		icon.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;
			@Override
            public boolean isVisible() {
                return model.getIcon() != null;
            }
        });
		buttonContainer.add(icon);

		ListView<InlineMenuItem> li = new ListView<InlineMenuItem>(ID_MENU_ITEM, new Model((Serializable) model.getMenuItems())) {
			private static final long serialVersionUID = 1L;

			@Override
            protected void populateItem(ListItem<InlineMenuItem> item) {
                initMenuItem(item);
            }
        };

        add(li);
	}

	public WebMarkupContainer getButtonContainer() {
		return (WebMarkupContainer)get(ID_BUTTON_CONTAINER);
	}

	 private void initMenuItem(ListItem<InlineMenuItem> menuItem) {
	        final InlineMenuItem item = menuItem.getModelObject();

	        WebMarkupContainer menuItemBody = new MenuLinkPanel(ID_MENU_ITEM_BODY, menuItem.getModel());
	        menuItemBody.setRenderBodyOnly(true);
	        menuItem.add(menuItemBody);
    }

    protected String getSpecialButtonClass() {
		return "btn-app";
	}

}
