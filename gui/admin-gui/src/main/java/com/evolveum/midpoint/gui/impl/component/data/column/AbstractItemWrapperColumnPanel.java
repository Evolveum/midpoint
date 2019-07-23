/*
 * Copyright (c) 2018 Evolveum
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

package com.evolveum.midpoint.gui.impl.component.data.column;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author skublik
 */
public abstract class AbstractItemWrapperColumnPanel<IW extends ItemWrapper, VW extends PrismValueWrapper> extends BasePanel<IW> {

	private static final long serialVersionUID = 1L;
	protected PageBase pageBase;
	protected ItemPath itemName;
	
	private ColumnType columnType;
	
	private static final String ID_VALUES = "values";
	private static final String ID_VALUE = "value";
	
	AbstractItemWrapperColumnPanel(String id, IModel<IW> model, ColumnType columnType) {
		super(id, model);
		this.columnType = columnType;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}
	
	private void initLayout() {
		ListView<VW> listView = new ListView<VW>(ID_VALUES, new PropertyModel<>(getModel(), "values")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<VW> item) {
				populate(item);
			}
		};
		listView.setReuseItems(true);
		listView.setOutputMarkupId(true);
		
		add(listView);
	}
	
	protected void populate(ListItem<VW> item) {
		switch (columnType) {
			case STRING:
				Label label = new Label(ID_VALUE, createLabel(item.getModelObject()));
				item.add(label);
				break;
			case LINK:
				item.add(createLink(ID_VALUE, item.getModel()));
				break;
			case VALUE:
				item.add(createValuePanel(ID_VALUE, getModel(), item.getModelObject()));
				break;
				
		}
		
	}
	
	protected abstract String createLabel(VW object);
	protected abstract Panel createLink(String id, IModel<VW> object);
	protected abstract Panel createValuePanel(String id, IModel<IW> model, VW object);
}
